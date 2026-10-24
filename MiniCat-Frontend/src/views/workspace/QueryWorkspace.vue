<template>
  <div class="query-workspace">
    <div class="query-toolbar">
      <el-select
        :model-value="tab.connectionId"
        placeholder="选择连接"
        style="width: 200px"
        @change="onConnectionChange"
      >
        <el-option
          v-for="connection in store.connections"
          :key="connection.id"
          :value="connection.id"
          :label="connection.name"
        />
      </el-select>

      <el-select
        :model-value="tab.database"
        placeholder="选择数据库"
        style="width: 180px"
        :disabled="!tab.connectionId || !capabilities.browse"
        :loading="databasesLoading"
        @change="onDatabaseChange"
      >
        <el-option v-for="database in databases" :key="database" :value="database" :label="database" />
      </el-select>

      <el-tag size="small" effect="plain">{{ capabilities.label }}</el-tag>

      <div class="toolbar-spacer" />

      <el-button type="primary" :loading="tab.executing" :disabled="!capabilities.query" @click="execute">
        <el-icon><VideoPlay /></el-icon>
        <span>执行</span>
      </el-button>
      <el-button :disabled="!capabilities.query" @click="formatSql">
        <el-icon><MagicStick /></el-icon>
        <span>格式化</span>
      </el-button>
      <el-button :disabled="!capabilities.query" @click="clearEditor">
        <el-icon><Delete /></el-icon>
        <span>清空</span>
      </el-button>
    </div>

    <el-alert
      v-if="!capabilities.query"
      class="capability-alert"
      type="warning"
      :closable="false"
      :title="capabilities.hint"
      description="该连接当前仅支持连接测试；MySQL 连接不受影响。"
    />

    <div class="editor-panel">
      <el-input
        :model-value="tab.sqlText"
        type="textarea"
        :rows="10"
        class="sql-editor"
        :disabled="!capabilities.query"
        placeholder="SELECT * FROM `table` LIMIT 100"
        @update:model-value="onSqlInput"
        @keydown.ctrl.enter.prevent="execute"
        @keydown.meta.enter.prevent="execute"
      />
      <div class="editor-hint">
        <span>Ctrl+Enter 执行</span>
        <span class="separator">·</span>
        <span>草稿自动保留在 Tab 中</span>
      </div>
    </div>

    <QueryResultPanel :result="tab.result" />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, MagicStick, VideoPlay } from '@element-plus/icons-vue'
import { format } from 'sql-formatter'
import { executeQuery, executeUpdate } from '@/api/database'
import { useWorkspaceStore } from '@/stores/workspace'
import QueryResultPanel from '@/views/workspace/QueryResultPanel.vue'

const props = defineProps({
  tab: { type: Object, required: true }
})

const store = useWorkspaceStore()

const connection = computed(() => store.connectionById(props.tab.connectionId))
const capabilities = computed(() => store.capabilitiesOf(connection.value))
const databases = computed(() => store.dbCache[props.tab.connectionId]?.databases || [])
const databasesLoading = computed(() => store.dbCache[props.tab.connectionId]?.loading || false)

function updateTab(patch) {
  store.updateTab(props.tab.id, patch)
}

function onSqlInput(value) {
  updateTab({ sqlText: value })
}

function onConnectionChange(connectionId) {
  updateTab({ connectionId, database: null, result: null })
  const target = store.connectionById(connectionId)
  if (target && store.capabilitiesOf(target).browse) {
    store.loadDatabases(connectionId)
  }
}

function onDatabaseChange(database) {
  updateTab({ database, result: null })
}

async function ensureDatabaseSelected() {
  if (props.tab.database) return true
  if (databases.value.length) {
    updateTab({ database: databases.value[0] })
    return true
  }
  return false
}

async function confirmDangerous(statement) {
  if (!/delete|drop|truncate/i.test(statement)) return true
  try {
    await ElMessageBox.confirm('该语句可能删除或修改大量数据，确定继续吗？', '危险操作确认', {
      confirmButtonText: '继续执行',
      cancelButtonText: '取消',
      type: 'warning'
    })
    return true
  } catch (error) {
    return false
  }
}

async function execute() {
  const statement = String(props.tab.sqlText || '').trim()
  if (!statement) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }
  if (!props.tab.connectionId) {
    ElMessage.warning('请先选择数据库连接')
    return
  }
  if (!capabilities.value.query) {
    ElMessage.warning(capabilities.value.hint)
    return
  }
  const hasDatabase = await ensureDatabaseSelected()
  if (!hasDatabase) {
    ElMessage.warning('请先选择数据库')
    return
  }

  const isSelect = /^select/i.test(statement)
  if (!isSelect) {
    const ok = await confirmDangerous(statement)
    if (!ok) return
  }

  updateTab({ executing: true, result: null })
  try {
    if (isSelect) {
      const res = await executeQuery(props.tab.connectionId, props.tab.database, statement)
      updateTab({ executing: false, result: { type: 'query', data: res.data || {} } })
      ElMessage.success(`查询成功，共 ${res.data?.totalRows ?? 0} 行`)
    } else {
      const res = await executeUpdate(props.tab.connectionId, props.tab.database, statement)
      updateTab({ executing: false, result: { type: 'update', affectedRows: res.data ?? 0 } })
      ElMessage.success(`执行成功，影响 ${res.data ?? 0} 行`)
    }
  } catch (error) {
    updateTab({ executing: false, result: { type: 'error' } })
    console.error('[query] 执行失败', error)
  }
}

function formatSql() {
  const statement = String(props.tab.sqlText || '')
  if (!statement.trim()) return
  try {
    updateTab({ sqlText: format(statement, { language: 'mysql' }) })
  } catch (error) {
    ElMessage.warning('SQL 格式化失败')
  }
}

function clearEditor() {
  updateTab({ sqlText: '', result: null })
}

onMounted(async () => {
  if (props.tab.connectionId && capabilities.value.browse) {
    await store.loadDatabases(props.tab.connectionId)
    if (!props.tab.database && databases.value.length) {
      updateTab({ database: databases.value[0] })
    }
  }
})
</script>

<style scoped lang="scss">
.query-workspace {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 16px;
  overflow: hidden;
}

.query-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;

  .toolbar-spacer {
    flex: 1;
  }

  .el-button {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.capability-alert {
  flex-shrink: 0;
}

.editor-panel {
  flex-shrink: 0;

  .sql-editor {
    :deep(textarea) {
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.6;
    }
  }

  .editor-hint {
    margin-top: 6px;
    font-size: 12px;
    color: #a8abb2;

    .separator {
      margin: 0 6px;
    }
  }
}
</style>
