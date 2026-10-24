<template>
  <aside class="data-explorer">
    <div class="explorer-header">
      <span class="explorer-title">资源</span>
      <div class="explorer-actions">
        <el-tooltip content="刷新连接与已展开节点" placement="bottom">
          <el-button text circle :icon="Refresh" :loading="store.connectionsLoading" @click="refreshAll" />
        </el-tooltip>
        <el-tooltip content="连接管理" placement="bottom">
          <el-button text circle :icon="Setting" @click="openSettings" />
        </el-tooltip>
      </div>
    </div>

    <el-scrollbar class="explorer-body">
      <div v-if="store.connectionsLoading && !store.connections.length" class="explorer-hint">连接加载中…</div>
      <div v-else-if="store.connectionsError" class="explorer-hint error" @click="refreshAll">
        {{ store.connectionsError }}（点击重试）
      </div>
      <div v-else-if="!store.connections.length" class="explorer-empty">
        <p>还没有数据库连接</p>
        <el-button type="primary" size="small" @click="openSettings">新建连接</el-button>
      </div>

      <template v-else>
        <div v-for="connection in store.connections" :key="connection.id" class="tree-connection">
          <div
            class="tree-row connection-row"
            :class="{ selected: isConnectionSelected(connection.id) }"
            @click="store.toggleConnection(connection)"
          >
            <el-icon class="expand-icon">
              <ArrowDown v-if="isConnectionExpanded(connection.id)" />
              <ArrowRight v-else />
            </el-icon>
            <el-tag
              class="type-badge"
              size="small"
              effect="dark"
              :color="capabilitiesOf(connection).color"
            >{{ capabilitiesOf(connection).badge }}</el-tag>
            <span class="node-name" :title="connection.name">{{ connection.name }}</span>
            <span class="status-dot" :class="statusClass(connection)" :title="statusText(connection)" />
          </div>

          <template v-if="isConnectionExpanded(connection.id)">
            <div v-if="!capabilitiesOf(connection).browse" class="node-hint">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ capabilitiesOf(connection).hint }}</span>
            </div>

            <template v-else>
              <div v-if="dbEntry(connection.id).loading" class="node-hint">数据库加载中…</div>
              <div
                v-else-if="dbEntry(connection.id).error"
                class="node-hint error"
                @click.stop="store.loadDatabases(connection.id, true)"
              >{{ dbEntry(connection.id).error }}（点击重试）</div>
              <div v-else-if="!dbEntry(connection.id).databases.length" class="node-hint">没有可见数据库</div>

              <div v-for="database in dbEntry(connection.id).databases" :key="database" class="tree-database">
                <div
                  class="tree-row database-row"
                  :class="{ selected: isDatabaseSelected(connection.id, database) }"
                  @click="store.toggleDatabase(connection, database)"
                >
                  <el-icon class="expand-icon">
                    <ArrowDown v-if="isDatabaseExpanded(connection.id, database)" />
                    <ArrowRight v-else />
                  </el-icon>
                  <el-icon class="node-icon"><Coin /></el-icon>
                  <span class="node-name" :title="database">{{ database }}</span>
                </div>

                <template v-if="isDatabaseExpanded(connection.id, database)">
                  <div v-if="tableEntry(connection.id, database).loading" class="node-hint">表加载中…</div>
                  <div
                    v-else-if="tableEntry(connection.id, database).error"
                    class="node-hint error"
                    @click.stop="store.loadTables(connection.id, database, true)"
                  >{{ tableEntry(connection.id, database).error }}（点击重试）</div>
                  <div v-else-if="!tableEntry(connection.id, database).tables.length" class="node-hint">没有数据表</div>

                  <div
                    v-for="table in tableEntry(connection.id, database).tables"
                    :key="table.tableName"
                    class="tree-row table-row"
                    :class="{ selected: isTableActive(connection.id, database, table.tableName) }"
                    @click="openTable(connection, database, table.tableName)"
                    @dblclick="openTable(connection, database, table.tableName, 'data')"
                  >
                    <el-icon class="node-icon"><Grid /></el-icon>
                    <span class="node-name" :title="table.tableName">{{ table.tableName }}</span>
                    <span v-if="table.tableComment" class="table-comment">{{ table.tableComment }}</span>
                  </div>
                </template>
              </div>
            </template>
          </template>
        </div>
      </template>
    </el-scrollbar>
  </aside>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  Coin,
  Grid,
  Refresh,
  Setting,
  WarningFilled
} from '@element-plus/icons-vue'
import { useWorkspaceStore } from '@/stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()

const MAX_TABS = 12

function capabilitiesOf(connection) {
  return store.capabilitiesOf(connection)
}

function dbEntry(connectionId) {
  return store.dbCache[connectionId] || { databases: [], loading: false, error: '', loaded: false }
}

function tableEntry(connectionId, database) {
  const key = `${connectionId}::${database}`
  return store.tableCache[key] || { tables: [], loading: false, error: '', loaded: false }
}

function isConnectionExpanded(connectionId) {
  return store.isExpanded(store.connectionKey(connectionId))
}

function isDatabaseExpanded(connectionId, database) {
  return store.isExpanded(store.databaseKey(connectionId, database))
}

function isConnectionSelected(connectionId) {
  const selection = store.treeSelection
  return selection && selection.connectionId === connectionId && !selection.database
}

function isDatabaseSelected(connectionId, database) {
  const selection = store.treeSelection
  return selection && selection.connectionId === connectionId && selection.database === database
}

function isTableActive(connectionId, database, tableName) {
  const tab = store.activeTab
  return tab && tab.type === 'table' && tab.connectionId === connectionId
    && tab.database === database && tab.table === tableName
}

function statusClass(connection) {
  if (connection.connectionStatus === 'success') return 'success'
  if (connection.connectionStatus === 'failed') return 'failed'
  return 'unknown'
}

function statusText(connection) {
  if (connection.connectionStatus === 'success') return '连接正常'
  if (connection.connectionStatus === 'failed') return '连接失败'
  return '未测试'
}

function openSettings() {
  router.push({ name: 'Settings' })
}

async function refreshAll() {
  await store.refreshConnections()
  await preloadExpanded()
}

async function preloadExpanded() {
  const connectionKeys = store.expandedKeys.filter(key => key.startsWith('conn:')).map(key => key.slice(5))
  for (const connectionId of connectionKeys) {
    await store.loadDatabases(connectionId)
  }
  const databaseKeys = store.expandedKeys
    .filter(key => key.startsWith('db:'))
    .map(key => key.slice(3).split('::'))
  for (const [connectionId, database] of databaseKeys) {
    if (!connectionId || !database) continue
    await store.loadDatabases(connectionId)
    await store.loadTables(connectionId, database)
  }
}

function openTable(connection, database, tableName, subTab = 'data') {
  const result = store.openTableTab({
    connectionId: connection.id,
    database,
    table: tableName,
    subTab: subTab || 'data'
  })
  if (!result.ok) {
    if (result.reason === 'limit') {
      ElMessage.warning(`Tab 数量已达上限（${MAX_TABS} 个），请先关闭一些 Tab`)
    }
    return
  }
  router.push({ name: 'Workspace' })
}

onMounted(() => {
  preloadExpanded()
})
</script>

<style scoped lang="scss">
.data-explorer {
  width: 268px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background-color: #f7f8fa;
  border-right: 1px solid #e4e7ed;
  overflow: hidden;
}

.explorer-header {
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 0 14px;
  border-bottom: 1px solid #ebeef5;

  .explorer-title {
    font-size: 13px;
    font-weight: 600;
    color: #606266;
    letter-spacing: 1px;
  }

  .explorer-actions {
    display: flex;
    align-items: center;
  }
}

.explorer-body {
  flex: 1;
  min-height: 0;
  padding: 6px 0;
}

.explorer-hint,
.explorer-empty {
  padding: 12px 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.explorer-hint.error {
  color: #f56c6c;
  cursor: pointer;
}

.explorer-empty {
  p {
    margin: 0 0 10px;
  }
}

.tree-row {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px 0 6px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  user-select: none;

  &:hover {
    background-color: #ecf0f5;
  }

  &.selected {
    background-color: #e6f0ff;
    color: #1f6feb;
  }

  .expand-icon {
    width: 16px;
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
  }

  .node-icon {
    margin: 0 2px;
    font-size: 13px;
    color: #909399;
    flex-shrink: 0;
  }

  .node-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .table-comment {
    max-width: 90px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12px;
    color: #c0c4cc;
  }
}

.connection-row {
  font-weight: 500;
  padding-left: 8px;
}

.type-badge {
  flex-shrink: 0;
  border: none;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
  font-size: 10px;
  letter-spacing: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background-color: #c0c4cc;

  &.success {
    background-color: #67c23a;
  }

  &.failed {
    background-color: #f56c6c;
  }
}

.database-row {
  padding-left: 22px;
}

.table-row {
  padding-left: 44px;
}

.node-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px 6px 36px;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;

  &.error {
    color: #f56c6c;
    cursor: pointer;
  }
}
</style>
