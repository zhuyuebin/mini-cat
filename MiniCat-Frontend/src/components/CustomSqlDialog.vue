<template>
  <el-dialog v-model="visible" title="执行自定义 SQL" width="760px" :close-on-click-modal="false" append-to-body>
    <el-input
      v-model="sql"
      type="textarea"
      :rows="8"
      class="sql-editor"
      placeholder="SELECT * FROM `table` LIMIT 100"
    />

    <div class="run-bar">
      <el-button type="primary" :loading="running" @click="run">执行</el-button>
      <span class="hint">SELECT 走查询接口；INSERT / UPDATE / DELETE / DROP TABLE 走更新接口</span>
    </div>

    <template v-if="lastResult">
      <el-alert
        :closable="false"
        :title="lastResult.summary"
        :type="lastResult.error ? 'error' : 'success'"
        class="result-alert"
      />
      <el-table
        v-if="lastResult.columns && lastResult.rows"
        :data="lastResult.rows"
        border
        stripe
        size="small"
        max-height="320"
        style="width: 100%"
      >
        <el-table-column v-for="column in lastResult.columns" :key="column" :prop="column" :label="column" min-width="120" show-overflow-tooltip />
      </el-table>
    </template>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { executeQuery, executeUpdate } from '@/api/database'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  connectionId: { type: String, required: true },
  database: { type: String, required: true },
  table: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const sql = ref('')
const running = ref(false)
const lastResult = ref(null)

watch(() => props.modelValue, value => {
  if (!value) return
  sql.value = `SELECT * FROM \`${props.table}\` LIMIT 100`
  lastResult.value = null
})

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

async function run() {
  const statement = sql.value.trim()
  if (!statement) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }
  const isSelect = /^select/i.test(statement)
  if (!isSelect) {
    const ok = await confirmDangerous(statement)
    if (!ok) return
  }

  running.value = true
  lastResult.value = null
  try {
    if (isSelect) {
      const res = await executeQuery(props.connectionId, props.database, statement)
      const data = res.data || {}
      lastResult.value = {
        summary: `查询成功，共 ${data.totalRows ?? data.rows?.length ?? 0} 行，耗时 ${data.executionTime ?? 0}ms`,
        columns: data.columns || [],
        rows: data.rows || []
      }
    } else {
      const res = await executeUpdate(props.connectionId, props.database, statement)
      lastResult.value = {
        summary: `执行成功，影响 ${res.data ?? 0} 行`,
        affectedRows: res.data ?? 0
      }
    }
  } catch (error) {
    console.error('[custom-sql] 执行失败', error)
  } finally {
    running.value = false
  }
}
</script>

<style scoped lang="scss">
.sql-editor {
  :deep(textarea) {
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.6;
  }
}

.run-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;

  .hint {
    font-size: 12px;
    color: #909399;
  }
}

.result-alert {
  margin-top: 12px;
}
</style>
