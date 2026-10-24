<template>
  <div class="app-shell">
    <TopBar />
    <div class="app-body">
      <DataExplorer />
      <main class="app-main">
        <router-view />
        <footer class="status-bar">
          <div class="status-left">
            <template v-if="activeConnection">
              <span class="status-connection">{{ activeConnection.name }}</span>
              <el-tag
                size="small"
                effect="plain"
                class="status-type"
              >{{ capabilities.label }}</el-tag>
              <span class="status-item" :class="statusClass">{{ statusText }}</span>
              <span v-if="contextText" class="status-item muted">{{ contextText }}</span>
            </template>
            <template v-else-if="store.hasConnections">
              <span class="status-item muted">{{ store.connections.length }} 个连接 · 从左侧资源树选择数据库或表</span>
            </template>
            <template v-else>
              <span class="status-item muted">还没有数据库连接 · 打开「设置」新建</span>
            </template>
          </div>
          <div class="status-right">
            <span class="status-item muted">数据默认保存在启动目录的 data/ 下</span>
          </div>
        </footer>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TopBar from '@/layout/TopBar.vue'
import DataExplorer from '@/layout/DataExplorer.vue'
import { useWorkspaceStore } from '@/stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()

const activeConnection = computed(() => store.activeConnection)
const capabilities = computed(() => store.capabilitiesOf(activeConnection.value))
const statusText = computed(() => {
  const status = activeConnection.value?.connectionStatus
  if (status === 'success') return '连接正常'
  if (status === 'failed') return '连接失败'
  return '未测试'
})
const statusClass = computed(() => {
  const status = activeConnection.value?.connectionStatus
  if (status === 'success') return 'success'
  if (status === 'failed') return 'failed'
  return 'muted'
})
const contextText = computed(() => {
  const tab = store.activeTab
  if (!tab) return ''
  if (tab.type === 'table') return `${tab.database} / ${tab.table}`
  return tab.database ? `${tab.database} · SQL 查询` : 'SQL 查询'
})

function newQueryTab() {
  const result = store.openQueryTab()
  if (!result.ok) {
    if (result.reason === 'limit') {
      ElMessage.warning('Tab 数量已达上限（12 个），请先关闭一些 Tab')
    } else if (result.reason === 'no-connection') {
      ElMessage.warning('请先在设置中新建数据库连接')
    }
    return
  }
  router.push({ name: 'Workspace' })
}

function closeActiveTab() {
  if (store.activeTabId) store.closeTab(store.activeTabId)
}

function onKeydown(event) {
  if (!event.altKey || event.ctrlKey || event.metaKey) return
  const key = String(event.key || '').toLowerCase()
  if (key === 't') {
    event.preventDefault()
    newQueryTab()
  } else if (key === 'w') {
    event.preventDefault()
    closeActiveTab()
  }
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  const result = await store.init()
  if (result && result.droppedTabs > 0) {
    ElMessage.warning(`${result.droppedTabs} 个失效的 Tab 已移除（对应连接不存在）`)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped lang="scss">
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f0f2f5;
}

.app-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.app-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.status-bar {
  height: 28px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  background-color: #ffffff;
  border-top: 1px solid #e4e7ed;
  font-size: 12px;
  color: #606266;
  overflow: hidden;
}

.status-left,
.status-right {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  white-space: nowrap;
}

.status-connection {
  font-weight: 600;
  color: #303133;
}

.status-type {
  font-size: 10px;
  padding: 0 5px;
  height: 18px;
  line-height: 18px;
}

.status-item {
  overflow: hidden;
  text-overflow: ellipsis;

  &.muted {
    color: #909399;
  }

  &.success {
    color: #67c23a;
  }

  &.failed {
    color: #f56c6c;
  }
}
</style>
