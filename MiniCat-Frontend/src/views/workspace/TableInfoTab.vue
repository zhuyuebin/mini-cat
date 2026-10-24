<template>
  <div class="table-info-tab">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="连接">
        {{ connection?.name || '未知连接' }}
      </el-descriptions-item>
      <el-descriptions-item label="数据库类型">
        {{ capabilities.label }}
      </el-descriptions-item>
      <el-descriptions-item label="数据库">
        {{ tab.database }}
      </el-descriptions-item>
      <el-descriptions-item label="数据表">
        <el-tag size="small">{{ tab.table }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="列数">
        <el-tag type="primary" size="small">{{ tab.columnDefs.length }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="行数">
        <el-tag type="success" size="small">{{ tab.total }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="当前页">
        {{ tab.page }} / {{ pageCount }}
      </el-descriptions-item>
      <el-descriptions-item label="每页条数">
        {{ tab.pageSize }}
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useWorkspaceStore } from '@/stores/workspace'

const props = defineProps({
  tab: { type: Object, required: true },
  connection: { type: Object, default: null }
})

const store = useWorkspaceStore()
const capabilities = computed(() => store.capabilitiesOf(props.connection))
const pageCount = computed(() => {
  const total = Number(props.tab.total) || 0
  const size = Number(props.tab.pageSize) || 20
  return Math.max(1, Math.ceil(total / size))
})
</script>

<style scoped>
.table-info-tab {
  height: 100%;
  overflow: auto;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
}
</style>
