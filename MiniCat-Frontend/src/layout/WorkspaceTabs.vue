<template>
  <div class="workspace-tabs">
    <el-scrollbar class="tabs-scroll" :wrap-style="'display:flex'">
      <div class="tabs-inner">
        <div
          v-for="tab in store.tabs"
          :key="tab.id"
          class="workspace-tab"
          :class="{ active: tab.id === store.activeTabId }"
          @click="activate(tab)"
        >
          <el-icon class="tab-icon">
            <Grid v-if="tab.type === 'table'" />
            <Document v-else />
          </el-icon>
          <span class="tab-title" :title="tabTooltip(tab)">{{ tab.title }}</span>
          <el-icon class="tab-close" @click.stop="close(tab)"><Close /></el-icon>
        </div>
        <button class="tab-add" title="新建查询" @click="newQuery">
          <el-icon><Plus /></el-icon>
        </button>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Document, Grid, Plus } from '@element-plus/icons-vue'
import { useWorkspaceStore } from '@/stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()

function tabTooltip(tab) {
  const connection = store.connectionById(tab.connectionId)
  const connectionName = connection ? connection.name : '未知连接'
  if (tab.type === 'table') {
    return `${connectionName} / ${tab.database} / ${tab.table}`
  }
  return tab.database ? `${connectionName} / ${tab.database}` : connectionName
}

function activate(tab) {
  store.activateTab(tab.id)
  if (router.currentRoute.value.name !== 'Workspace') {
    router.push({ name: 'Workspace' })
  }
}

async function close(tab) {
  if (tab.type === 'query' && String(tab.sqlText || '').trim()) {
    try {
      await ElMessageBox.confirm('该查询有未执行的 SQL 草稿，关闭后草稿会丢失，确定关闭吗？', '关闭查询', {
        confirmButtonText: '关闭',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (error) {
      return
    }
  }
  store.closeTab(tab.id)
}

function newQuery() {
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
</script>

<style scoped lang="scss">
.workspace-tabs {
  flex-shrink: 0;
  height: 40px;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.tabs-scroll {
  height: 100%;

  :deep(.el-scrollbar__view) {
    height: 100%;
  }
}

.tabs-inner {
  display: flex;
  align-items: flex-end;
  height: 100%;
  padding: 0 6px;
  gap: 2px;
}

.workspace-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 200px;
  height: 32px;
  padding: 0 8px 0 12px;
  margin-top: 8px;
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: 6px 6px 0 0;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  background-color: #f5f7fa;

  &:hover {
    background-color: #ecf0f5;
  }

  &.active {
    background-color: #ffffff;
    border-color: #e4e7ed;
    color: #1f6feb;
    font-weight: 500;
  }

  .tab-icon {
    font-size: 13px;
    flex-shrink: 0;
  }

  .tab-title {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tab-close {
    font-size: 12px;
    border-radius: 50%;
    padding: 2px;

    &:hover {
      background-color: #d9dde3;
      color: #303133;
    }
  }
}

.tab-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin-bottom: 1px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #909399;
  cursor: pointer;

  &:hover {
    background-color: #ecf0f5;
    color: #1f6feb;
  }
}
</style>
