<template>
  <div class="table-workspace">
    <div class="context-bar">
      <div class="context-left">
        <span class="crumb">{{ connectionName }} › {{ tab.database }} ›</span>
        <span class="table-name">{{ tab.table }}</span>
        <el-tag size="small" effect="plain" class="type-tag">{{ capabilities.label }}</el-tag>
        <span class="meta">{{ tab.columnDefs.length }} 列 · {{ tab.total }} 行</span>
      </div>
      <div class="context-actions">
        <el-button size="small" :icon="Refresh" :loading="tab.loading || columnsLoading" @click="refresh">
          刷新
        </el-button>
        <el-dropdown trigger="click" @command="onMoreCommand">
          <el-button size="small">
            更多
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="create-table">
                <el-icon><Plus /></el-icon>新建表
              </el-dropdown-item>
              <el-dropdown-item command="import" :disabled="!capabilities.ddl">
                <el-icon><Upload /></el-icon>导入数据
              </el-dropdown-item>
              <el-dropdown-item command="custom-sql">
                <el-icon><Edit /></el-icon>自定义 SQL
              </el-dropdown-item>
              <el-dropdown-item command="drop-table" divided class="danger-item">
                <el-icon><Delete /></el-icon>删除表
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-tabs v-model="activeSubTab" class="table-tabs">
      <el-tab-pane label="数据" name="data">
        <TableDataTab
          :tab="tab"
          @add-row="openAddRow"
          @delete-row="deleteRow"
          @page-change="onPageChange"
          @size-change="onSizeChange"
          @refresh="refreshData"
        />
      </el-tab-pane>
      <el-tab-pane label="结构" name="structure">
        <TableStructureTab
          :tab="tab"
          :loading="columnsLoading"
          :error="columnsError"
          @refresh="loadColumns(true)"
        />
      </el-tab-pane>
      <el-tab-pane label="信息" name="info">
        <TableInfoTab :tab="tab" :connection="connection" />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增行 -->
    <el-dialog v-model="addRowVisible" title="新增数据行" width="600px" :close-on-click-modal="false">
      <el-form label-width="140px">
        <el-form-item v-for="column in editableColumns" :key="column.columnName" :label="column.columnName">
          <el-input v-model="newRow[column.columnName]" :placeholder="columnPlaceholder(column)" clearable />
          <div class="field-tags">
            <el-tag v-if="column.columnKey === 'PRI'" size="small" type="danger">主键</el-tag>
            <el-tag v-if="column.nullable === false" size="small" type="warning">必填</el-tag>
            <span class="field-type">{{ column.dataType }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addRowVisible = false">取消</el-button>
        <el-button type="primary" :loading="addRowSubmitting" @click="submitAddRow">确定</el-button>
      </template>
    </el-dialog>

    <CreateTableDialog
      v-model="createTableVisible"
      :connection-id="tab.connectionId"
      :database="tab.database"
      @saved="onTableCreated"
    />
    <ImportDataDialog
      v-model="importVisible"
      :connection-id="tab.connectionId"
      :database="tab.database"
      :table="tab.table"
      @imported="onImported"
    />
    <CustomSqlDialog
      v-model="customSqlVisible"
      :connection-id="tab.connectionId"
      :database="tab.database"
      :table="tab.table"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Delete,
  Edit,
  Plus,
  Refresh,
  Upload
} from '@element-plus/icons-vue'
import { dropTable, executeUpdate, getColumns, getTableData } from '@/api/database'
import { useWorkspaceStore } from '@/stores/workspace'
import TableDataTab from '@/views/workspace/TableDataTab.vue'
import TableStructureTab from '@/views/workspace/TableStructureTab.vue'
import TableInfoTab from '@/views/workspace/TableInfoTab.vue'
import CreateTableDialog from '@/components/CreateTableDialog.vue'
import ImportDataDialog from '@/components/ImportDataDialog.vue'
import CustomSqlDialog from '@/components/CustomSqlDialog.vue'

const props = defineProps({
  tab: { type: Object, required: true }
})

const store = useWorkspaceStore()

const columnsLoading = ref(false)
const columnsError = ref('')
const addRowVisible = ref(false)
const addRowSubmitting = ref(false)
const newRow = ref({})
const createTableVisible = ref(false)
const importVisible = ref(false)
const customSqlVisible = ref(false)

const connection = computed(() => store.connectionById(props.tab.connectionId))
const connectionName = computed(() => connection.value?.name || '未知连接')
const capabilities = computed(() => store.capabilitiesOf(connection.value))

const activeSubTab = computed({
  get: () => props.tab.subTab || 'data',
  set: value => {
    updateTab({ subTab: value })
    if (value === 'data') loadData()
    if (value === 'structure' && !props.tab.columnDefs.length) loadColumns()
  }
})

const editableColumns = computed(() => (props.tab.columnDefs || []).filter(column => !column.autoIncrement))

function updateTab(patch) {
  store.updateTab(props.tab.id, patch)
}

function columnPlaceholder(column) {
  if (column.columnKey === 'PRI') return '请输入主键值'
  if (column.nullable === false) return '必填'
  return '可留空'
}

async function loadColumns(force = false) {
  if (columnsLoading.value) return
  if (props.tab.columnDefs.length && !force) return
  columnsLoading.value = true
  columnsError.value = ''
  try {
    const res = await getColumns(props.tab.connectionId, props.tab.database, props.tab.table)
    updateTab({ columnDefs: res.data || [] })
  } catch (error) {
    columnsError.value = '表结构加载失败'
    console.error('[table] 加载表结构失败', error)
  } finally {
    columnsLoading.value = false
  }
}

async function loadData(force = false) {
  if (props.tab.loading) return
  if (props.tab.loaded && !force) return
  updateTab({ loading: true, error: '' })
  try {
    const res = await getTableData(
      props.tab.connectionId,
      props.tab.database,
      props.tab.table,
      props.tab.page,
      props.tab.pageSize
    )
    const data = res.data || {}
    updateTab({
      columns: data.columns || [],
      rows: data.rows || [],
      total: Number(data.total) || 0,
      loaded: true,
      loading: false
    })
  } catch (error) {
    updateTab({ loading: false, error: '表数据加载失败' })
    console.error('[table] 加载表数据失败', error)
  }
}

function refreshData() {
  loadData(true)
}

function refresh() {
  loadColumns(true)
  loadData(true)
}

async function onPageChange(page) {
  updateTab({ page, loaded: false })
  await loadData(true)
}

async function onSizeChange(size) {
  updateTab({ pageSize: size, page: 1, loaded: false })
  await loadData(true)
}

function openAddRow() {
  if (!props.tab.columnDefs.length) {
    ElMessage.warning('表结构尚未加载完成，请稍后再试')
    return
  }
  newRow.value = {}
  for (const column of editableColumns.value) {
    newRow.value[column.columnName] = ''
  }
  addRowVisible.value = true
}

function buildInsertSql() {
  const tab = props.tab
  const columnNames = Object.keys(newRow.value).filter(key => {
    const value = newRow.value[key]
    return value !== null && value !== undefined && String(value) !== ''
  })
  if (!columnNames.length) return null

  const requiredColumns = tab.columnDefs.filter(column => column.nullable === false && !column.autoIncrement)
  const missing = requiredColumns.filter(column => {
    const value = newRow.value[column.columnName]
    return value === null || value === undefined || String(value) === ''
  })
  if (missing.length) {
    ElMessage.warning(`以下必填字段未填写：${missing.map(column => column.columnName).join(', ')}`)
    return null
  }

  const values = columnNames.map(name => {
    const value = newRow.value[name]
    const column = tab.columnDefs.find(item => item.columnName === name)
    const dataType = String(column?.dataType || '').toUpperCase()
    const numeric = ['INT', 'BIGINT', 'TINYINT', 'SMALLINT', 'MEDIUMINT', 'DECIMAL', 'DOUBLE', 'FLOAT', 'NUMERIC'].some(type => dataType.includes(type))
      || dataType.includes('INT')
    if (numeric && /^-?\d+(\.\d+)?$/.test(String(value).trim())) {
      return String(value).trim()
    }
    return `'${String(value).replace(/'/g, "''")}'`
  })

  return `INSERT INTO \`${tab.table}\` (${columnNames.map(name => `\`${name}\``).join(', ')}) VALUES (${values.join(', ')})`
}

async function submitAddRow() {
  const sql = buildInsertSql()
  if (!sql) return
  addRowSubmitting.value = true
  try {
    await executeUpdate(props.tab.connectionId, props.tab.database, sql)
    ElMessage.success('新增成功')
    addRowVisible.value = false
    updateTab({ page: 1, loaded: false })
    await loadData(true)
  } catch (error) {
    console.error('[table] 新增行失败', error)
  } finally {
    addRowSubmitting.value = false
  }
}

async function deleteRow(row) {
  const tab = props.tab
  const primaryKeys = (tab.columnDefs || []).filter(column => column.columnKey === 'PRI')
  if (!primaryKeys.length) {
    ElMessage.error('该表没有主键，无法安全删除单行')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该行数据吗？此操作不可恢复。', '删除数据行', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }

  const where = primaryKeys.map(column => {
    const value = row[column.columnName]
    if (value === null || value === undefined) return `${column.columnName} IS NULL`
    return `\`${column.columnName}\` = '${String(value).replace(/'/g, "''")}'`
  }).join(' AND ')
  const sql = `DELETE FROM \`${tab.table}\` WHERE ${where}`

  try {
    await executeUpdate(tab.connectionId, tab.database, sql)
    ElMessage.success('已删除')
    if (tab.rows.length === 1 && tab.page > 1) {
      updateTab({ page: tab.page - 1, loaded: false })
    } else {
      updateTab({ loaded: false })
    }
    await loadData(true)
  } catch (error) {
    console.error('[table] 删除行失败', error)
  }
}

async function onImported(result) {
  ElMessage.success(result?.message || '导入完成')
  updateTab({ subTab: 'data', loaded: false })
  await loadData(true)
  const lastPage = Math.max(1, Math.ceil((props.tab.total || 0) / props.tab.pageSize))
  if (lastPage !== props.tab.page) {
    updateTab({ page: lastPage, loaded: false })
    await loadData(true)
  }
}

async function onTableCreated() {
  await store.loadTables(props.tab.connectionId, props.tab.database, true)
  ElMessage.success('数据表已创建，可在左侧资源树中打开')
}

async function dropCurrentTable() {
  try {
    await ElMessageBox.confirm(`确定删除数据表「${props.tab.table}」吗？此操作不可恢复。`, '删除数据表', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }
  try {
    await dropTable(props.tab.connectionId, props.tab.database, props.tab.table)
    store.closeTab(props.tab.id)
    await store.loadTables(props.tab.connectionId, props.tab.database, true)
    ElMessage.success('数据表已删除')
  } catch (error) {
    console.error('[table] 删除表失败', error)
  }
}

function onMoreCommand(command) {
  if (command === 'create-table') createTableVisible.value = true
  if (command === 'import') importVisible.value = true
  if (command === 'custom-sql') customSqlVisible.value = true
  if (command === 'drop-table') dropCurrentTable()
}

onMounted(() => {
  if (!props.tab.columnDefs.length) loadColumns()
  if (props.tab.subTab === 'data' && !props.tab.loaded) loadData()
})
</script>

<style scoped lang="scss">
.table-workspace {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f0f2f5;
}

.context-bar {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.context-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  .crumb {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
  }

  .table-name {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    max-width: 240px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .type-tag {
    font-size: 11px;
  }

  .meta {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
  }
}

.context-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.table-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0 16px 12px;

  :deep(.el-tabs__header) {
    margin: 0 0 8px;
    flex-shrink: 0;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }
}

.field-tags {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 6px;

  .field-type {
    font-size: 12px;
    color: #909399;
  }
}

:deep(.danger-item) {
  color: #f56c6c;
}
</style>
