<template>
  <div class="query-result-panel">
    <template v-if="result && result.type === 'query'">
      <div class="result-header">
        <span class="title">查询结果</span>
        <div class="stats">
          <el-tag size="small" type="primary">{{ result.data.totalRows ?? 0 }} 行</el-tag>
          <el-tag size="small" type="warning">{{ result.data.executionTime ?? 0 }} ms</el-tag>
        </div>
      </div>
      <div class="result-body">
        <el-table
          v-if="result.data.rows && result.data.rows.length"
          :data="result.data.rows"
          border
          stripe
          size="small"
          height="100%"
          style="width: 100%"
        >
          <el-table-column
            v-for="column in result.data.columns"
            :key="column"
            :prop="column"
            :label="column"
            min-width="120"
            show-overflow-tooltip
          />
        </el-table>
        <el-empty v-else description="查询没有返回数据" />
      </div>
    </template>

    <template v-else-if="result && result.type === 'update'">
      <el-result icon="success" title="执行成功" :sub-title="`影响 ${result.affectedRows ?? 0} 行`" />
    </template>

    <template v-else-if="result && result.type === 'error'">
      <el-empty description="执行失败，错误详情已由全局提示展示，可查看后端日志" />
    </template>

    <template v-else>
      <el-empty description="执行 SQL 后在这里查看结果" :image-size="110" />
    </template>
  </div>
</template>

<script setup>
defineProps({
  result: { type: Object, default: null }
})
</script>

<style scoped lang="scss">
.query-result-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.result-header {
  flex-shrink: 0;
  height: 40px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;

  .title {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
  }

  .stats {
    display: flex;
    gap: 6px;
  }
}

.result-body {
  flex: 1;
  min-height: 0;
  padding: 0 6px;
}
</style>
