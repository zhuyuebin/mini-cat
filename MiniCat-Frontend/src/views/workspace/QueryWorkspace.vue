<template>
  <div class="query-workspace">
    <el-card shadow="never" class="placeholder-card">
      <template #header>
        <div class="header">
          <el-icon><Document /></el-icon>
          <span class="title">{{ tab.title }}</span>
        </div>
      </template>
      <p>连接：{{ connectionName }}</p>
      <p v-if="tab.database">数据库：{{ tab.database }}</p>
      <el-alert
        type="info"
        :closable="false"
        title="P0-1 阶段说明"
        description="SQL 编辑器、执行结果与草稿持久化将在 P0-3 接入本窗口。"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { useWorkspaceStore } from '@/stores/workspace'

const props = defineProps({
  tab: { type: Object, required: true }
})

const store = useWorkspaceStore()
const connectionName = computed(() => store.connectionById(props.tab.connectionId)?.name || '未知连接')
</script>

<style scoped lang="scss">
.query-workspace {
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

  .title {
    font-size: 16px;
    font-weight: 600;
  }
}
</style>
