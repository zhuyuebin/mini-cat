<template>
  <div class="table-data-tab">
    <div class="data-toolbar">
      <span class="summary">
        共 <b>{{ tab.total }}</b> 条 · 第 {{ tab.page }} / {{ pageCount }} 页
      </span>
      <div class="actions">
        <el-button size="small" type="primary" @click="$emit('add-row')" :disabled="tab.loading || !tab.columns.length">
          <el-icon><Plus /></el-icon>
          <span>新增行</span>
        </el-button>
      </div>
    </div>

    <div class="data-grid" v-loading="tab.loading">
      <el-table
        v-if="tab.rows.length"
        :data="tab.rows"
        style="width: 100%; height: 100%"
        border
        stripe
        size="small"
      >
        <el-table-column type="index" label="#" width="62" align="center" :index="globalIndex" />
        <el-table-column
          v-for="column in tab.columns"
          :key="column"
          :prop="column"
          :label="column"
          min-width="120"
          show-overflow-tooltip
        />
        <el-table-column label="操作" width="86" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="$emit('delete-row', row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else-if="tab.error" :description="tab.error">
        <el-button size="small" @click="$emit('refresh')">重试</el-button>
      </el-empty>
      <el-empty v-else-if="!tab.loading" description="暂无数据">
        <el-button size="small" type="primary" @click="$emit('add-row')" :disabled="!tab.columns.length">新增行</el-button>
        <el-button size="small" @click="$emit('refresh')">刷新</el-button>
      </el-empty>
    </div>

    <div class="pagination-bar">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="tab.total"
        :page-size="tab.pageSize"
        :current-page="tab.page"
        :page-sizes="[20, 50, 100, 200]"
        @current-change="$emit('page-change', $event)"
        @size-change="$emit('size-change', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'

const props = defineProps({
  tab: { type: Object, required: true }
})

defineEmits(['add-row', 'delete-row', 'page-change', 'size-change', 'refresh'])

const pageCount = computed(() => {
  const total = Number(props.tab.total) || 0
  const size = Number(props.tab.pageSize) || 20
  return Math.max(1, Math.ceil(total / size))
})

function globalIndex(index) {
  return (Number(props.tab.page) - 1) * Number(props.tab.pageSize) + index + 1
}
</script>

<style scoped lang="scss">
.table-data-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.data-toolbar {
  flex-shrink: 0;
  height: 42px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;

  .summary {
    font-size: 12px;
    color: #606266;

    b {
      color: #303133;
    }
  }

  .actions {
    display: flex;
    gap: 8px;

    .el-button {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.data-grid {
  flex: 1;
  min-height: 0;
  padding: 0 8px;

  :deep(.el-table) {
    height: 100%;
  }
}

.pagination-bar {
  flex-shrink: 0;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #ebeef5;
  background: #ffffff;
}
</style>
