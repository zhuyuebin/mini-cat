<template>
  <div class="table-workspace">
    <el-card shadow="never" class="placeholder-card">
      <template #header>
        <div class="header">
          <el-icon><Grid /></el-icon>
          <span class="table-name">{{ tab.table }}</span>
          <span class="path">{{ tab.database }}</span>
        </div>
      </template>
      <p>连接：{{ connectionName }}</p>
      <p>数据库：{{ tab.database }}</p>
      <p>数据表：{{ tab.table }}</p>
      <el-alert
        type="info"
        :closable="false"
        title="P0-1 阶段说明"
        description="当前里程碑完成了资源树、多 Tab 与上下文持久化；表数据 / 表结构 / 分页将在 P0-2 接入本窗口。"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Grid } from '@element-plus/icons-vue'
import { useWorkspaceStore } from '@/stores/workspace'

const props = defineProps({
  tab: { type: Object, required: true }
})

const store = useWorkspaceStore()
const connectionName = computed(() => store.connectionById(props.tab.connectionId)?.name || '未知连接')
</script>

<style scoped lang="scss">
.table-workspace {
  padding: 16px;
  overflow: auto;
}

.placeholder-card {
  max-width: 720px;
}

.header {
  display: flex;
  align-items: center;
  gap: 8px;

  .table-name {
    font-size: 16px;
    font-weight: 600;
  }

  .path {
    font-size: 12px;
    color: #909399;
  }
}
</style>
