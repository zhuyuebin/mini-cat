<template>
  <div class="tables-page">
    <!-- 顶部工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-content">
        <!-- 左侧：数据库选择 -->
        <div class="toolbar-left">
          <el-select
            v-model="selectedConnectionId"
            placeholder="选择数据库连接"
            style="width: 200px"
            @change="handleConnectionChange"
          >
            <el-option
              v-for="conn in connections"
              :key="conn.id"
              :label="conn.name"
              :value="conn.id"
            />
          </el-select>

          <el-select
            v-model="selectedDatabase"
            placeholder="选择数据库"
            style="width: 180px"
            :disabled="!selectedConnectionId"
            @change="handleDatabaseChange"
          >
            <el-option
              v-for="db in databases"
              :key="db"
              :label="db"
              :value="db"
            />
          </el-select>
        </div>
        
        <!-- 右侧：操作按钮区 -->
        <div class="toolbar-right">
          <!-- 表级操作按钮 -->
          <div class="button-group" v-if="selectedDatabase">
            <el-button type="primary" @click="showCreateTableDialog">
              <el-icon><Plus /></el-icon>
              <span>新建表</span>
            </el-button>
            <el-button 
              type="success" 
              @click="showImportDialog" 
              :disabled="!selectedTable"
              :style="{ marginLeft: '8px' }"
            >
              <el-icon><Upload /></el-icon>
              <span>批量导入</span>
            </el-button>
            <el-button 
              type="danger" 
              @click="handleDeleteTable" 
              :disabled="!selectedTable"
              :style="{ marginLeft: '8px' }"
            >
              <el-icon><Delete /></el-icon>
              <span>删除表</span>
            </el-button>
          </div>
          
          <!-- 数据操作按钮组 -->
          <template v-if="selectedTable">
            <el-divider direction="vertical" style="height: 24px; margin: 0 12px" />
            <el-tag size="small" type="info" class="table-tag">
              <el-icon><Files /></el-icon>
              {{ selectedTable.tableName }}
            </el-tag>
            <el-button-group style="margin-left: 12px">
              <el-button type="primary" @click="loadTableData" :loading="dataLoading">
                <el-icon><Download /></el-icon>
                <span>加载数据</span>
              </el-button>
              <el-button type="success" @click="showAddRowDialog">
                <el-icon><Plus /></el-icon>
                <span>新增行</span>
              </el-button>
              <el-button type="warning" @click="executeCustomSQL">
                <el-icon><Edit /></el-icon>
                <span>自定义SQL</span>
              </el-button>
            </el-button-group>
          </template>
        </div>
      </div>
    </el-card>

    <!-- 主体内容区 -->
    <el-row :gutter="16" class="main-content">
      <!-- 左侧：表列表 -->
      <el-col :span="6">
        <el-card class="tables-list-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>数据表</span>
              <el-tag size="small" type="info">{{ tables.length }}</el-tag>
            </div>
          </template>
          <el-table
            :data="tables"
            style="width: 100%"
            height="calc(100vh - 240px)"
            highlight-current-row
            @current-change="handleTableSelect"
            v-loading="tablesLoading"
            size="small"
          >
            <el-table-column prop="tableName" label="表名" min-width="100" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="table-name-cell">
                  <el-icon><Grid /></el-icon>
                  <span>{{ row.tableName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="rowCount" label="行数" width="70" align="right">
              <template #default="{ row }">
                <el-tag size="small" type="success">{{ row.rowCount || 0 }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：表详情和数据 -->
      <el-col :span="18">
        <el-card class="detail-card" shadow="hover" v-if="selectedTable">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon size="18"><Grid /></el-icon>
                <span class="table-title">{{ selectedTable.tableName }}</span>
                <el-tag v-if="selectedTable.tableComment" size="small" type="info">
                  {{ selectedTable.tableComment }}
                </el-tag>
              </div>
              <div class="header-stats">
                <el-tag size="small">
                  <el-icon><Document /></el-icon>
                  {{ columns.length }} 列
                </el-tag>
                <el-tag size="small" type="success">
                  <el-icon><Coin /></el-icon>
                  {{ selectedTable.rowCount || 0 }} 行
                </el-tag>
              </div>
            </div>
          </template>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <!-- 表结构 -->
            <el-tab-pane name="structure">
              <template #label>
                <span>
                  <el-icon><List /></el-icon>
                  表结构
                </span>
              </template>
              <el-table
                :data="columns"
                style="width: 100%"
                max-height="calc(100vh - 320px)"
                border
                stripe
                v-loading="columnsLoading"
                size="small"
              >
                <el-table-column prop="columnName" label="列名" width="150" fixed>
                  <template #default="{ row }">
                    <div class="column-name">
                      <el-icon v-if="row.columnKey === 'PRI'" color="#f56c6c"><Key /></el-icon>
                      <el-icon v-else-if="row.columnKey === 'UNI'" color="#e6a23c"><Lock /></el-icon>
                      <el-icon v-else color="#909399"><Collection /></el-icon>
                      <span>{{ row.columnName }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="dataType" label="数据类型" width="120">
                  <template #default="{ row }">
                    <el-tag size="small" type="primary">{{ row.dataType }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="isNullable" label="可空" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.isNullable === 'YES' ? 'warning' : 'success'" size="small">
                      {{ row.isNullable === 'YES' ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="columnKey" label="键" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.columnKey === 'PRI'" type="danger" size="small">PK</el-tag>
                    <el-tag v-else-if="row.columnKey === 'UNI'" type="warning" size="small">UK</el-tag>
                    <el-tag v-else-if="row.columnKey === 'MUL'" type="primary" size="small">IDX</el-tag>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="columnDefault" label="默认值" width="100" show-overflow-tooltip />
                <el-table-column prop="extra" label="额外" width="100" show-overflow-tooltip />
                <el-table-column prop="columnComment" label="注释" min-width="150" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <!-- 表数据 -->
            <el-tab-pane name="data">
              <template #label>
                <span>
                  <el-icon><DataLine /></el-icon>
                  表数据
                </span>
              </template>
              <div class="table-data-container">
                <el-table
                  :data="tableData"
                  style="width: 100%"
                  max-height="calc(100vh - 320px)"
                  border
                  stripe
                  v-loading="dataLoading"
                  size="small"
                >
                  <el-table-column type="index" label="#" width="50" align="center" />
                  <el-table-column
                    v-for="col in dataColumns"
                    :key="col"
                    :prop="col"
                    :label="col"
                    min-width="120"
                    show-overflow-tooltip
                  />
                  <el-table-column label="操作" width="150" fixed="right" align="center">
                    <template #default="{ row, $index }">
                      <el-button
                        type="danger"
                        size="small"
                        @click="deleteRow(row, $index)"
                        :loading="deletingRows.has($index)"
                      >
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="pagination-container" v-if="tableData.length > 0">
                  <el-pagination
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="totalRows"
                    :page-size="pageSize"
                    :current-page="currentPage"
                    :page-sizes="[20, 50, 100, 200]"
                    @current-change="handlePageChange"
                    @size-change="handleSizeChange"
                  />
                </div>
              </div>
            </el-tab-pane>

            <!-- 表信息 -->
            <el-tab-pane name="info">
              <template #label>
                <span>
                  <el-icon><InfoFilled /></el-icon>
                  表信息
                </span>
              </template>
              <el-descriptions :column="2" border class="table-info-desc">
                <el-descriptions-item label="表名">
                  <el-tag>{{ selectedTable.tableName }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="注释">
                  {{ selectedTable.tableComment || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="行数">
                  <el-tag type="success">{{ selectedTable.rowCount || 0 }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="列数">
                  <el-tag type="primary">{{ columns.length }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">
                  {{ selectedTable.createTime || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="更新时间">
                  {{ selectedTable.updateTime || '-' }}
                </el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 空状态提示 -->
        <el-card class="detail-card detail-empty" shadow="hover" v-else>
          <el-empty description="请从左侧选择一个数据表查看详情" :image-size="160">
            <template #image>
              <el-icon :size="80" color="#dcdfe6"><Files /></el-icon>
            </template>
          </el-empty>
        </el-card>
      </el-col>
    </el-row>
    <!-- 新增行对话框 -->
    <el-dialog
      v-model="addRowDialogVisible"
      title="新增数据行"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form label-width="120px">
        <el-form-item
          v-for="col in columns"
          :key="col.columnName"
          :label="col.columnName"
        >
          <el-input
            v-model="newRowData[col.columnName]"
            :placeholder="getPlaceholder(col)"
            :disabled="isAutoIncrement(col)"
            clearable
          />
          <div class="field-info">
            <el-tag size="small" v-if="col.columnKey === 'PRI'" type="danger">主键</el-tag>
            <el-tag size="small" v-if="col.isNullable === 'NO'" type="warning">必填</el-tag>
            <el-tag size="small" v-if="isAutoIncrement(col)" type="success">自增</el-tag>
            <span class="type-tag">{{ col.dataType }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addRowDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddRow">确定</el-button>
      </template>
    </el-dialog>

    <!-- 自定义SQL对话框 -->
    <el-dialog
      v-model="customSQLDialogVisible"
      title="执行自定义 SQL"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-input
        v-model="customSQL"
        type="textarea"
        :rows="8"
        placeholder="请输入 SQL 语句..."
        class="custom-sql-editor"
      />
      <template #footer>
        <el-button @click="customSQLDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCustomSQL">执行</el-button>
      </template>
    </el-dialog>

    <!-- 创建表对话框 -->
    <el-dialog
      v-model="createTableDialogVisible"
      title="创建新表"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="表名">
          <el-input
            v-model="newTableForm.tableName"
            placeholder="请输入表名，例如: users"
            clearable
          />
        </el-form-item>
        <el-form-item label="表注释">
          <el-input
            v-model="newTableForm.tableComment"
            placeholder="请输入表注释（可选）"
            clearable
          />
        </el-form-item>
        
        <el-divider>列定义</el-divider>
        
        <el-table :data="newTableForm.columns" border size="small" max-height="400">
          <el-table-column label="列名" width="120">
            <template #default="{ row }">
              <el-input v-model="row.columnName" placeholder="列名" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="数据类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.dataType" size="small" style="width: 100%">
                <el-option
                  v-for="type in dataTypes"
                  :key="type"
                  :label="type"
                  :value="type"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="长度" width="80">
            <template #default="{ row }">
              <el-input
                v-model="row.length"
                size="small"
                placeholder="长度"
                :disabled="!['VARCHAR', 'CHAR', 'DECIMAL'].includes(row.dataType)"
              />
            </template>
          </el-table-column>
          <el-table-column label="可空" width="70" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.nullable" />
            </template>
          </el-table-column>
          <el-table-column label="主键" width="70" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.primaryKey" />
            </template>
          </el-table-column>
          <el-table-column label="自增" width="70" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.autoIncrement" :disabled="row.dataType !== 'INT' && row.dataType !== 'BIGINT'" />
            </template>
          </el-table-column>
          <el-table-column label="默认值" width="100">
            <template #default="{ row }">
              <el-input v-model="row.defaultValue" placeholder="默认值" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="注释" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.comment" placeholder="注释" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button
                type="danger"
                size="small"
                link
                @click="removeColumn($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div style="margin-top: 12px; text-align: center;">
          <el-button type="primary" size="small" @click="addColumn">
            <el-icon><Plus /></el-icon>
            添加列
          </el-button>
        </div>
      </el-form>
      
      <template #footer>
        <el-button @click="createTableDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateTable">创建表</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入数据"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-alert
        title="导入说明"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <p>1. 支持 Excel (.xlsx, .xls) 和 CSV (.csv) 格式文件</p>
        <p>2. 文件第一行必须是表头，且必须与数据库表字段名严格匹配</p>
        <p>3. 如果有任何一列不匹配，导入将会失败并提示具体错误</p>
      </el-alert>
      
      <el-upload
        class="upload-demo"
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".xlsx,.xls,.csv"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .xlsx / .xls / .csv 文件
          </div>
        </template>
      </el-upload>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImport" :loading="importing">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Files, Download, Grid, Document, Coin, List, Key, Lock, Collection, DataLine, InfoFilled, Plus, Edit, Upload, UploadFilled, Delete } from '@element-plus/icons-vue'
import { getAllConnections, getDatabases, getTables, getColumns, executeQuery, executeUpdate, createTable, importData, dropTable } from '@/api/database'

const connections = ref([])
const databases = ref([])
const tables = ref([])
const columns = ref([])
const tableData = ref([])
const dataColumns = ref([])
const selectedConnectionId = ref('')
const selectedDatabase = ref('')
const selectedTable = ref(null)
const activeTab = ref('structure')
const tablesLoading = ref(false)
const columnsLoading = ref(false)
const dataLoading = ref(false)
const totalRows = ref(0)
const currentPage = ref(1)
const pageSize = ref(50)
const tableInfo = ref(null)
const deletingRows = ref(new Set())
const customSQLDialogVisible = ref(false)
const customSQL = ref('')
const addRowDialogVisible = ref(false)
const newRowData = ref({})
const createTableDialogVisible = ref(false)
const importDialogVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const newTableForm = ref({
  tableName: '',
  tableComment: '',
  columns: [
    {
      columnName: 'id',
      dataType: 'INT',
      nullable: false,
      primaryKey: true,
      autoIncrement: true,
      length: '',
      defaultValue: '',
      comment: '主键ID'
    }
  ]
})

// 常用的数据类型
const dataTypes = [
  'INT', 'BIGINT', 'VARCHAR', 'TEXT', 'DECIMAL', 'DOUBLE', 'FLOAT',
  'BOOLEAN', 'DATE', 'DATETIME', 'TIMESTAMP', 'CHAR', 'LONGTEXT'
]

// 加载连接列表
const loadConnections = async () => {
  try {
    const res = await getAllConnections()
    connections.value = res.data || []
  } catch (error) {
    console.error('加载连接失败:', error)
  }
}

// 连接改变
const handleConnectionChange = async (connectionId) => {
  selectedDatabase.value = ''
  databases.value = []
  tables.value = []
  selectedTable.value = null
  
  if (!connectionId) return
  
  try {
    const res = await getDatabases(connectionId)
    databases.value = res.data || []
  } catch (error) {
    ElMessage.error('获取数据库列表失败')
  }
}

// 数据库改变
const handleDatabaseChange = async (database) => {
  tables.value = []
  selectedTable.value = null
  columns.value = []
  tableData.value = []
  
  if (!selectedConnectionId.value || !database) return
  
  tablesLoading.value = true
  try {
    const res = await getTables(selectedConnectionId.value, database)
    tables.value = res.data || []
  } catch (error) {
    ElMessage.error('获取表列表失败')
  } finally {
    tablesLoading.value = false
  }
}

// 选择表
const handleTableSelect = async (table) => {
  if (!table) return
  
  selectedTable.value = table
  activeTab.value = 'structure'
  columns.value = []
  tableData.value = []
  currentPage.value = 1
  
  // 加载列信息
  columnsLoading.value = true
  try {
    const res = await getColumns(
      selectedConnectionId.value,
      selectedDatabase.value,
      table.tableName
    )
    columns.value = res.data || []
  } catch (error) {
    ElMessage.error('获取列信息失败')
  } finally {
    columnsLoading.value = false
  }
}

// 加载表数据
const loadTableData = async () => {
  if (!selectedTable.value) return
  
  dataLoading.value = true
  activeTab.value = 'data'
  
  try {
    const offset = (currentPage.value - 1) * pageSize.value
    // 移除反引号,避免触发SQL注入检测
    const sql = `SELECT * FROM ${selectedTable.value.tableName} LIMIT ${pageSize.value} OFFSET ${offset}`
    
    const res = await executeQuery(
      selectedConnectionId.value,
      selectedDatabase.value,
      sql
    )
    
    if (res.data && res.data.rows) {
      tableData.value = res.data.rows
      dataColumns.value = res.data.columns || []
      totalRows.value = res.data.totalRows || res.data.rows.length
    }
  } catch (error) {
    ElMessage.error('加载表数据失败: ' + (error.response?.data?.message || error.message))
    console.error('Load table data error:', error)
  } finally {
    dataLoading.value = false
  }
}

// 分页改变
const handlePageChange = (page) => {
  currentPage.value = page
  loadTableData()
}

// 每页条数改变
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadTableData()
}

// 显示新增行对话框
const showAddRowDialog = () => {
  if (!columns.value || columns.value.length === 0) {
    ElMessage.warning('请先加载表结构信息')
    return
  }
  
  // 初始化新行数据
  newRowData.value = {}
  columns.value.forEach(col => {
    // 如果是自增字段，跳过
    if (!isAutoIncrement(col)) {
      newRowData.value[col.columnName] = null
    }
  })
  
  addRowDialogVisible.value = true
}

// 判断是否为自增字段
const isAutoIncrement = (col) => {
  return col.extra && col.extra.toLowerCase().includes('auto_increment')
}

// 获取占位符文本
const getPlaceholder = (col) => {
  if (isAutoIncrement(col)) {
    return '自增字段，无需填写'
  }
  if (col.columnKey === 'PRI') {
    return col.isNullable === 'NO' ? '请输入主键值' : '请输入主键值（可选）'
  }
  return `输入${col.columnName}`
}

// 提交新增行
const submitAddRow = async () => {
  try {
    // 构建 INSERT SQL
    const columnNames = Object.keys(newRowData.value).filter(key => newRowData.value[key] !== null && newRowData.value[key] !== '')
    if (columnNames.length === 0) {
      ElMessage.warning('请至少填写一个字段')
      return
    }
    
    // 验证必填字段
    const requiredColumns = columns.value.filter(col => col.isNullable === 'NO' && !isAutoIncrement(col))
    for (const col of requiredColumns) {
      if (!newRowData.value[col.columnName] && newRowData.value[col.columnName] !== 0) {
        ElMessage.warning(`字段 ${col.columnName} 为必填项`)
        return
      }
    }
    
    const values = columnNames.map(col => {
      const val = newRowData.value[col]
      // 如果是数字类型，不加引号
      if (typeof val === 'number') {
        return val
      }
      // 字符串类型加引号并转义
      return `'${String(val).replace(/'/g, "''")}'`
    })
    
    const sql = `INSERT INTO ${selectedTable.value.tableName} (${columnNames.join(', ')}) VALUES (${values.join(', ')})`
    
    const res = await executeUpdate(
      selectedConnectionId.value,
      selectedDatabase.value,
      sql
    )
    
    ElMessage.success(`新增成功，影响 ${res.data} 行`)
    addRowDialogVisible.value = false
    
    // 刷新数据
    if (activeTab.value === 'data') {
      loadTableData()
    }
  } catch (error) {
    ElMessage.error('新增失败: ' + (error.response?.data?.message || error.message))
  }
}

// 删除行
const deleteRow = async (row, index) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这行数据吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    deletingRows.value.add(index)
    
    // 找到主键列
    const pkColumns = columns.value.filter(col => col.columnKey === 'PRI')
    if (pkColumns.length === 0) {
      ElMessage.error('无法删除：该表没有主键')
      return
    }
    
    // 构建 WHERE 条件
    const whereConditions = pkColumns.map(pk => {
      const val = row[pk.columnName]
      if (typeof val === 'number') {
        return `${pk.columnName} = ${val}`
      }
      return `${pk.columnName} = '${String(val).replace(/'/g, "''")}'`
    })
    
    const sql = `DELETE FROM ${selectedTable.value.tableName} WHERE ${whereConditions.join(' AND ')}`
    
    const res = await executeUpdate(
      selectedConnectionId.value,
      selectedDatabase.value,
      sql
    )
    
    ElMessage.success(`删除成功，影响 ${res.data} 行`)
    
    // 从表格中移除该行
    tableData.value.splice(index, 1)
    totalRows.value--
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
    }
  } finally {
    deletingRows.value.delete(index)
  }
}

// 执行自定义SQL
const executeCustomSQL = () => {
  customSQL.value = `SELECT * FROM ${selectedTable.value.tableName} LIMIT 100`
  customSQLDialogVisible.value = true
}

// 提交自定义SQL
const submitCustomSQL = async () => {
  if (!customSQL.value.trim()) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }
  
  // 检测是否为危险操作
  const sql = customSQL.value.trim().toUpperCase()
  const isDangerous = sql.includes('DELETE') || sql.includes('DROP') || sql.includes('TRUNCATE')
  
  if (isDangerous) {
    try {
      await ElMessageBox.confirm(
        '此操作可能会删除或修改大量数据，确定要继续吗？',
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return // 用户取消
    }
  }
  
  try {
    let res
    // 判断是查询还是更新
    if (sql.startsWith('SELECT')) {
      res = await executeQuery(
        selectedConnectionId.value,
        selectedDatabase.value,
        customSQL.value
      )
      ElMessage.success(`查询成功，共 ${res.data.totalRows} 行`)
    } else {
      res = await executeUpdate(
        selectedConnectionId.value,
        selectedDatabase.value,
        customSQL.value
      )
      ElMessage.success(`执行成功，影响 ${res.data} 行`)
    }
    
    customSQLDialogVisible.value = false
    
    // 如果在数据标签页，刷新数据
    if (activeTab.value === 'data' && sql.startsWith('SELECT')) {
      tableData.value = res.data.rows
      dataColumns.value = res.data.columns || []
      totalRows.value = res.data.totalRows || res.data.rows.length
    }
  } catch (error) {
    ElMessage.error('执行失败: ' + (error.response?.data?.message || error.message))
  }
}

// 显示创建表对话框
const showCreateTableDialog = () => {
  newTableForm.value = {
    tableName: '',
    tableComment: '',
    columns: [
      {
        columnName: 'id',
        dataType: 'INT',
        nullable: false,
        primaryKey: true,
        autoIncrement: true,
        length: null,
        defaultValue: '',
        comment: '主键ID'
      }
    ]
  }
  createTableDialogVisible.value = true
}

// 添加列
const addColumn = () => {
  newTableForm.value.columns.push({
    columnName: '',
    dataType: 'VARCHAR',
    nullable: true,
    primaryKey: false,
    autoIncrement: false,
    length: '255',
    defaultValue: '',
    comment: ''
  })
}

// 删除列
const removeColumn = (index) => {
  if (newTableForm.value.columns.length <= 1) {
    ElMessage.warning('至少需要保留一个列')
    return
  }
  newTableForm.value.columns.splice(index, 1)
}

// 提交创建表
const submitCreateTable = async () => {
  // 验证表单
  if (!newTableForm.value.tableName.trim()) {
    ElMessage.warning('请输入表名')
    return
  }

  // 验证列名
  for (let i = 0; i < newTableForm.value.columns.length; i++) {
    const col = newTableForm.value.columns[i]
    if (!col.columnName.trim()) {
      ElMessage.warning(`第 ${i + 1} 行的列名不能为空`)
      return
    }
  }

  try {
    await createTable(
      selectedConnectionId.value,
      selectedDatabase.value,
      newTableForm.value
    )
    
    ElMessage.success('表创建成功')
    createTableDialogVisible.value = false
    
    // 刷新表列表
    handleDatabaseChange(selectedDatabase.value)
  } catch (error) {
    ElMessage.error('创建表失败: ' + (error.response?.data?.message || error.message))
  }
}

// 显示导入对话框
const showImportDialog = () => {
  if (!selectedTable.value) {
    ElMessage.warning('请先选择一个数据表')
    return
  }
  importFile.value = null
  importDialogVisible.value = true
}

// 处理文件选择
const handleFileChange = (file) => {
  importFile.value = file.raw
}

// 提交导入
const submitImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  importing.value = true
  try {
    const res = await importData(
      selectedConnectionId.value,
      selectedDatabase.value,
      selectedTable.value.tableName,
      importFile.value
    )
    
    const result = res.data
    if (result.success) {
      ElMessage.success(result.message)
      importDialogVisible.value = false
      // 刷新数据
      if (activeTab.value === 'data') {
        loadTableData()
      }
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('导入失败: ' + (error.response?.data?.message || error.message))
  } finally {
    importing.value = false
  }
}

// 删除表
const handleDeleteTable = async () => {
  if (!selectedTable.value) {
    ElMessage.warning('请先选择一个数据表')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除表 "${selectedTable.value.tableName}" 吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return // 用户取消
  }
  
  try {
    await dropTable(
      selectedConnectionId.value,
      selectedDatabase.value,
      selectedTable.value.tableName
    )
    
    ElMessage.success('表删除成功')
    
    // 刷新表列表
    handleDatabaseChange(selectedDatabase.value)
    
    // 清空选中的表
    selectedTable.value = null
  } catch (error) {
    ElMessage.error('删除表失败: ' + (error.response?.data?.message || error.message))
  }
}

onMounted(() => {
  loadConnections()
})
</script>

<style scoped lang="scss">
.tables-page {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  padding: 16px;
  background-color: #f5f7fa;
  gap: 16px;
  
  // 顶部工具栏
  .toolbar-card {
    flex-shrink: 0;
    border-radius: 8px;
    
    :deep(.el-card__body) {
      padding: 12px 16px;
    }
    
    .toolbar-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .toolbar-left,
      .toolbar-right {
        display: flex;
        align-items: center;
        gap: 12px;
      }
      
      .toolbar-left {
        // 左侧选择器样式
      }
      
      .toolbar-right {
        display: flex;
        align-items: center;
        gap: 12px;
        
        .button-group {
          display: flex;
          align-items: center;
        }
        
        .table-tag {
          padding: 6px 12px;
          font-weight: 500;
          display: flex;
          align-items: center;
          gap: 4px;
          
          .el-icon {
            font-size: 14px;
          }
        }
        
        .el-divider--vertical {
          height: 24px;
          margin: 0;
        }
        
        :deep(.el-button-group) {
          .el-button {
            display: flex;
            align-items: center;
            gap: 4px;
          }
        }
      }
    }
  }
  
  // 主体内容区
  .main-content {
    flex: 1;
    overflow: hidden;
    
    .tables-list-card {
      height: 100%;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      
      :deep(.el-card__body) {
        flex: 1;
        overflow: hidden;
        padding: 0;
      }
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: 600;
      }
      
      .table-name-cell {
        display: flex;
        align-items: center;
        gap: 6px;
        
        .el-icon {
          color: #409eff;
          font-size: 14px;
        }
      }
      
      :deep(.el-table__row) {
        cursor: pointer;
        
        &:hover {
          background-color: #f5f7fa;
        }
      }
      
      :deep(.el-table__row.current-row) {
        background-color: #ecf5ff;
      }
    }
    
    .detail-card {
      height: 100%;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      
      :deep(.el-card__body) {
        flex: 1;
        overflow-y: auto;
        padding: 16px;
      }
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .header-left {
          display: flex;
          align-items: center;
          gap: 12px;
          
          .table-title {
            font-size: 16px;
            font-weight: 600;
            color: #303133;
          }
        }
        
        .header-stats {
          display: flex;
          gap: 8px;
        }
      }
      
      .detail-tabs {
        :deep(.el-tabs__item) {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
      
      .column-name {
        display: flex;
        align-items: center;
        gap: 6px;
        font-weight: 500;
      }
      
      .table-data-container {
        .pagination-container {
          margin-top: 16px;
          display: flex;
          justify-content: center;
        }
      }
      
      .table-info-desc {
        :deep(.el-descriptions__label) {
          font-weight: 600;
          width: 120px;
        }
      }
      
      &.detail-empty {
        :deep(.el-card__body) {
          display: flex;
          justify-content: center;
          align-items: center;
          min-height: 400px;
        }
      }
    }
  }
}

// 自定义SQL编辑器样式
.custom-sql-editor {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  
  :deep(textarea) {
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    line-height: 1.6;
  }
}

// 字段信息样式
.field-info {
  margin-top: 4px;
  display: flex;
  gap: 6px;
  align-items: center;
  
  .type-tag {
    font-size: 12px;
    color: #909399;
  }
}
</style>
