<template>
  <div class="table-data-tab">
    <div class="data-toolbar">
      <div class="toolbar-left">
        <span class="summary">
          共 <b>{{ tab.total }}</b> 条 · 第 {{ tab.page }} / {{ pageCount }} 页
        </span>
        <span v-if="editHint" class="edit-hint">{{ editHint }}</span>
      </div>
      <div class="actions">
        <el-button size="small" type="primary" @click="$emit('add-row')" :disabled="tab.loading || !tab.columnDefs.length">
          <el-icon><Plus /></el-icon>
          <span>新增行</span>
        </el-button>
      </div>
    </div>

    <div v-if="readOnlyNotice" class="read-only-notice">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ readOnlyNotice }}</span>
    </div>

    <div class="data-grid" v-loading="tab.loading">
      <el-table
        v-if="tab.rows.length"
        :data="tab.rows"
        style="width: 100%; height: 100%"
        border
        stripe
        size="small"
        @cell-dblclick="onCellDblClick"
      >
        <el-table-column type="index" label="#" width="62" align="center" :index="globalIndex" />
        <el-table-column
          v-for="column in dataColumnNames"
          :key="column"
          :prop="column"
          :label="column"
          min-width="140"
          :show-overflow-tooltip="!isEditingColumn(column)"
        >
          <template #default="{ row }">
            <template v-if="isEditingCell(row, column)">
              <el-select
                v-if="isBooleanColumn(column)"
                v-model="editState.value"
                size="small"
                style="width: 100%"
                @change="commitEdit"
              >
                <el-option label="true" value="true" />
                <el-option label="false" value="false" />
              </el-select>
              <el-date-picker
                v-else-if="isDateTimeColumn(column)"
                v-model="editState.value"
                type="datetime"
                size="small"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择日期时间"
                style="width: 100%"
                @change="commitEdit"
              />
              <el-date-picker
                v-else-if="isDateColumn(column)"
                v-model="editState.value"
                type="date"
                size="small"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
                @change="commitEdit"
              />
              <el-input
                v-else
                :ref="focusEditInput"
                v-model="editState.value"
                size="small"
                :placeholder="columnPlaceholder(column)"
                @keydown.enter.prevent.stop="commitEdit"
                @keydown.esc.prevent.stop="cancelEdit"
                @blur="onEditBlur"
              />
            </template>
            <span
              v-else
              class="cell-value"
              :class="{
                'cell-null': row[column] === null || row[column] === undefined,
                'cell-editable': isColumnEditable(column)
              }"
              :title="isColumnEditable(column) ? '双击编辑' : cellReadonlyReason(column)"
            >{{ displayCell(row[column]) }}</span>
          </template>
        </el-table-column>
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
        <el-button size="small" type="primary" @click="$emit('add-row')" :disabled="!tab.columnDefs.length">新增行</el-button>
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
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, Plus } from '@element-plus/icons-vue'
import { updateTableCell } from '@/api/database'
import { useWorkspaceStore } from '@/stores/workspace'

const props = defineProps({
  tab: { type: Object, required: true }
})

const emit = defineEmits(['add-row', 'delete-row', 'page-change', 'size-change', 'refresh'])

const store = useWorkspaceStore()

/** 暂不支持单元格编辑的列类型，与后端 SqlValueNormalizer 保持一致 */
const NON_EDITABLE_TYPES = [
  'BLOB', 'TINYBLOB', 'MEDIUMBLOB', 'LONGBLOB',
  'BINARY', 'VARBINARY', 'BIT',
  'JSON', 'XML',
  'GEOMETRY', 'POINT', 'LINESTRING', 'POLYGON',
  'MULTIPOINT', 'MULTILINESTRING', 'MULTIPOLYGON', 'GEOMETRYCOLLECTION'
]

const editState = ref(null)

const pageCount = computed(() => {
  const total = Number(props.tab.total) || 0
  const size = Number(props.tab.pageSize) || 20
  return Math.max(1, Math.ceil(total / size))
})

/**
 * 数据列名：后端 /table-data 返回的是字符串数组。
 * 这里做一次防御：如果历史状态里混入了列对象，也能取到列名而不是渲染成 [object Object]。
 */
const dataColumnNames = computed(() => (props.tab.columns || [])
  .map(column => (typeof column === 'string' ? column : (column && column.columnName) || ''))
  .filter(Boolean))

const columnDefsByName = computed(() => {
  const map = {}
  for (const columnDef of props.tab.columnDefs || []) {
    map[columnDef.columnName] = columnDef
  }
  return map
})

const identityColumns = computed(() => (props.tab.columnDefs || [])
  .filter(columnDef => columnDef.rowIdentityPart)
  .map(columnDef => columnDef.columnName))

const inlineEditEnabled = computed(() => store.uiSettings.inlineEditEnabled !== false)
const canEditTable = computed(() => inlineEditEnabled.value && identityColumns.value.length > 0)

const editHint = computed(() => (canEditTable.value
  ? '双击单元格编辑 · Enter/失焦保存 · Esc 取消'
  : ''))

const readOnlyNotice = computed(() => {
  if (!inlineEditEnabled.value) {
    return '单元格编辑已关闭（配置项 minicat.ui.inline-edit-enabled=false），当前表格为只读模式'
  }
  if (!identityColumns.value.length) {
    return '该表没有主键或唯一非空索引，无法安全定位数据行，当前表格为只读；建议先为该表添加主键'
  }
  return ''
})

function columnDefOf(column) {
  return columnDefsByName.value[column] || null
}

function dataTypeOf(column) {
  const columnDef = columnDefOf(column)
  return String(columnDef?.dataType || '').toUpperCase()
}

function isBooleanColumn(column) {
  return ['BOOLEAN', 'BOOL'].includes(dataTypeOf(column))
}

function isDateColumn(column) {
  return dataTypeOf(column) === 'DATE'
}

function isDateTimeColumn(column) {
  return ['DATETIME', 'TIMESTAMP'].includes(dataTypeOf(column))
}

function isColumnEditable(column) {
  if (!canEditTable.value) return false
  const columnDef = columnDefOf(column)
  if (!columnDef) return false
  if (columnDef.rowIdentityPart || columnDef.autoIncrement) return false
  return !NON_EDITABLE_TYPES.includes(dataTypeOf(column))
}

function cellReadonlyReason(column) {
  if (!inlineEditEnabled.value) return '单元格编辑已关闭'
  if (!identityColumns.value.length) return '该表没有主键或唯一非空索引，无法定位行'
  const columnDef = columnDefOf(column)
  if (!columnDef) return '列元数据缺失'
  if (columnDef.rowIdentityPart) return '行标识列不允许直接修改'
  if (columnDef.autoIncrement) return '自增列不允许直接修改'
  return '该列类型暂不支持编辑'
}

function columnPlaceholder(column) {
  const columnDef = columnDefOf(column)
  if (columnDef?.nullable === false) return '必填'
  return '输入新值，清空表示 NULL'
}

function rowKeyOf(row) {
  return JSON.stringify(identityColumns.value.map(column => row[column] ?? null))
}

function isEditingCell(row, column) {
  return !!editState.value
    && editState.value.column === column
    && editState.value.rowKey === rowKeyOf(row)
}

function isEditingColumn(column) {
  return !!editState.value && editState.value.column === column
}

function displayCell(value) {
  if (value === null || value === undefined) return 'NULL'
  if (typeof value === 'object') return JSON.stringify(value)
  const text = String(value)
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(text)) {
    return text.replace('T', ' ').replace(/\.\d+$/, '')
  }
  return text
}

function displayForInput(value, column) {
  if (value === null || value === undefined) return ''
  if (isBooleanColumn(column)) {
    const text = String(value).toLowerCase()
    return (text === '1' || text === 'true') ? 'true' : 'false'
  }
  const text = String(value)
  if (isDateTimeColumn(column)) {
    return text.replace('T', ' ').replace(/\.\d+$/, '')
  }
  if (isDateColumn(column)) {
    return text.split('T')[0].split(' ')[0]
  }
  return text
}

function focusEditInput(component) {
  if (component && typeof component.focus === 'function') {
    nextTick(() => component.focus())
  }
}

function globalIndex(index) {
  return (Number(props.tab.page) - 1) * Number(props.tab.pageSize) + index + 1
}

async function onCellDblClick(row, column) {
  if (editState.value?.saving) return
  if (props.tab.loading) return
  const columnName = column?.property
  if (!columnName || !isColumnEditable(columnName)) return

  if (editState.value) {
    await commitEdit()
    if (editState.value) return
  }

  editState.value = {
    rowKey: rowKeyOf(row),
    rowRef: row,
    column: columnName,
    original: row[columnName],
    value: displayForInput(row[columnName], columnName),
    saving: false
  }
}

function cancelEdit() {
  editState.value = null
}

function onEditBlur() {
  if (!editState.value || editState.value.saving) return
  commitEdit()
}

function buildUpdatePayload(state, nextValue, emptyValue) {
  const keys = {}
  for (const identityColumn of identityColumns.value) {
    keys[identityColumn] = state.rowRef[identityColumn]
  }
  const changes = {}
  if (emptyValue) {
    changes[state.column] = null
  } else if (isBooleanColumn(state.column)) {
    changes[state.column] = String(nextValue) === 'true'
  } else {
    changes[state.column] = nextValue
  }
  const expected = {}
  expected[state.column] = state.original
  return {
    table: props.tab.table,
    keys,
    changes,
    expected
  }
}

async function commitEdit() {
  const state = editState.value
  if (!state || state.saving) return

  const columnDef = columnDefOf(state.column)
  const nextValue = state.value
  const emptyValue = nextValue === '' || nextValue === null || nextValue === undefined

  if (emptyValue && columnDef?.nullable === false && !columnDef?.autoIncrement) {
    ElMessage.warning(`「${state.column}」为必填列，不能为空`)
    return
  }

  const originalForCompare = displayForInput(state.original, state.column)
  if (String(originalForCompare) === String(nextValue ?? '')) {
    cancelEdit()
    return
  }

  state.saving = true
  try {
    const payload = buildUpdatePayload(state, nextValue, emptyValue)
    const res = await updateTableCell(props.tab.connectionId, props.tab.database, payload)
    const updatedRow = res.data?.row
    if (updatedRow) {
      Object.assign(state.rowRef, updatedRow)
    } else {
      state.rowRef[state.column] = emptyValue ? null : nextValue
    }
    editState.value = null
    ElMessage.success('已保存')
  } catch (error) {
    if (error.code === 40902) {
      ElMessage.warning('该行已被其他操作修改，已为你刷新最新数据')
      emit('refresh')
    } else if (error.code === 40402) {
      ElMessage.warning('数据行不存在或已被删除，已刷新')
      emit('refresh')
    }
    editState.value = null
  }
}

watch(
  () => [props.tab.page, props.tab.pageSize],
  () => {
    if (editState.value) cancelEdit()
  }
)
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
  min-height: 42px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #ebeef5;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }

  .summary {
    font-size: 12px;
    color: #606266;
    white-space: nowrap;

    b {
      color: #303133;
    }
  }

  .edit-hint {
    font-size: 12px;
    color: #a8abb2;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;

    .el-button {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.read-only-notice {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 12px;
  color: #b88230;
  background: #fdf6ec;
  border-bottom: 1px solid #faecd8;
}

.data-grid {
  flex: 1;
  min-height: 0;
  padding: 0 8px;

  :deep(.el-table) {
    height: 100%;
  }

  :deep(.cell) {
    padding: 0 8px;
  }
}

.cell-value {
  display: block;
  width: 100%;
  padding: 6px 4px;
  margin: -6px -4px;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.cell-null {
    color: #c0c4cc;
    font-style: italic;
  }

  &.cell-editable {
    cursor: text;

    &:hover {
      background: #ecf5ff;
      box-shadow: inset 0 0 0 1px #b3d8ff;
    }
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
