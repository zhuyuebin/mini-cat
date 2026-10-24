<template>
  <div class="table-structure-tab" v-loading="loading">
    <el-table
      v-if="tab.columnDefs.length"
      :data="tab.columnDefs"
      style="width: 100%; height: 100%"
      border
      stripe
      size="small"
    >
      <el-table-column prop="columnName" label="列名" width="200" fixed show-overflow-tooltip>
        <template #default="{ row }">
          <span class="column-name">
            <el-icon v-if="row.columnKey === 'PRI'" color="#f56c6c"><Key /></el-icon>
            <el-icon v-else color="#909399"><Collection /></el-icon>
            <span>{{ row.columnName }}</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="dataType" label="数据类型" width="140">
        <template #default="{ row }">
          <el-tag size="small" type="primary">{{ row.dataType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="可空" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.nullable === false ? 'warning' : 'success'" size="small">
            {{ row.nullable === false ? '否' : '是' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="键" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.columnKey === 'PRI'" type="danger" size="small">PK</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="defaultValue" label="默认值" width="130" show-overflow-tooltip />
      <el-table-column label="自增" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.autoIncrement" type="success" size="small">自增</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="columnComment" label="注释" min-width="180" show-overflow-tooltip />
    </el-table>

    <el-empty v-else-if="error" :description="error">
      <el-button size="small" @click="$emit('refresh')">重试</el-button>
    </el-empty>
    <el-empty v-else description="暂无结构信息" />
  </div>
</template>

<script setup>
import { Collection, Key } from '@element-plus/icons-vue'

defineProps({
  tab: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

defineEmits(['refresh'])
</script>

<style scoped lang="scss">
.table-structure-tab {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 8px;
  overflow: hidden;

  :deep(.el-table) {
    height: 100%;
  }

  .column-name {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
  }
}
</style>
