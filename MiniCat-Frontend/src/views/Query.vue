<template>
  <div class="query-page">
    <!-- 顶部工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-content">
        <div class="toolbar-left">
          <el-select
            v-model="selectedConnectionId"
            placeholder="选择数据库连接"
            style="width: 200px; margin-right: 12px"
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
            style="width: 180px; margin-right: 12px"
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
        
        <div class="toolbar-right">
          <el-button-group>
            <el-button type="primary" @click="executeQueryHandler" :loading="executing">
              <el-icon><VideoPlay /></el-icon>
              <span>执行查询</span>
            </el-button>
            <el-button type="success" @click="executeUpdateHandler" :loading="executing">
              <el-icon><Edit /></el-icon>
              <span>执行更新</span>
            </el-button>
          </el-button-group>
          <el-divider direction="vertical" />
          <el-button-group>
            <el-button @click="formatSQL">
              <el-icon><MagicStick /></el-icon>
              <span>格式化</span>
            </el-button>
            <el-button @click="clearEditor">
              <el-icon><Delete /></el-icon>
              <span>清空</span>
            </el-button>
          </el-button-group>
        </div>
      </div>
    </el-card>

    <!-- 主体内容区 -->
    <el-row :gutter="16" class="main-content">
      <!-- 左侧：数据表导航 -->
      <el-col :span="5">
        <el-card class="tables-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>数据表</span>
              <el-tag size="small" type="info">{{ tableTree.length }}</el-tag>
            </div>
          </template>
          <el-tree
            :data="tableTree"
            :props="treeProps"
            node-key="name"
            @node-click="handleTableClick"
            v-loading="tablesLoading"
            highlight-current
            :expand-on-click-node="false"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <el-icon><Files /></el-icon>
                <span class="node-label" :title="data.comment || data.name">{{ data.name }}</span>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：SQL编辑器和结果 -->
      <el-col :span="19">
        <div class="editor-result-wrapper">
          <!-- SQL编辑器 -->
          <el-card class="editor-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>SQL 编辑器</span>
                <el-tag size="small" type="success">就绪</el-tag>
              </div>
            </template>
            <el-input
              v-model="sqlQuery"
              type="textarea"
              :rows="10"
              placeholder="请输入 SQL 语句...

查询示例：SELECT * FROM users WHERE id = 1
插入示例：INSERT INTO users (name, email) VALUES ('test', 'test@example.com')
更新示例：UPDATE users SET name = 'new_name' WHERE id = 1
删除示例：DELETE FROM users WHERE id = 1"
              class="sql-editor"
            />
          </el-card>

          <!-- 查询结果 -->
          <el-card class="result-card" shadow="hover" v-if="queryResult">
            <template #header>
              <div class="card-header">
                <span>查询结果</span>
                <div class="result-info">
                  <el-tag size="small" type="primary">{{ queryResult.totalRows }} 行</el-tag>
                  <el-tag size="small" type="warning">{{ queryResult.executionTime }} ms</el-tag>
                </div>
              </div>
            </template>
            <el-table
              :data="queryResult.rows"
              style="width: 100%"
              max-height="400"
              border
              stripe
              size="small"
            >
              <el-table-column
                v-for="col in queryResult.columns"
                :key="col"
                :prop="col"
                :label="col"
                min-width="120"
                show-overflow-tooltip
              />
            </el-table>
          </el-card>

          <!-- 空状态提示 -->
          <el-card class="result-card result-empty" shadow="hover" v-else>
            <el-empty description="执行查询后查看结果" :image-size="120" />
          </el-card>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VideoPlay, MagicStick, Delete, Files, Edit } from '@element-plus/icons-vue'
import { getAllConnections, getDatabases, getTables, executeQuery, executeUpdate } from '@/api/database'
import { format } from 'sql-formatter'

const connections = ref([])
const databases = ref([])
const tableTree = ref([])
const selectedConnectionId = ref('')
const selectedDatabase = ref('')
const sqlQuery = ref('SELECT * FROM ')
const queryResult = ref(null)
const executing = ref(false)
const tablesLoading = ref(false)

const treeProps = {
  children: 'children',
  label: 'name'
}

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
  tableTree.value = []
  queryResult.value = null
  
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
  tableTree.value = []
  queryResult.value = null
  
  if (!selectedConnectionId.value || !database) return
  
  tablesLoading.value = true
  try {
    const res = await getTables(selectedConnectionId.value, database)
    const tables = res.data || []
    tableTree.value = tables.map(table => ({
      name: table.tableName,
      comment: table.tableComment
    }))
  } catch (error) {
    ElMessage.error('获取表列表失败')
  } finally {
    tablesLoading.value = false
  }
}

// 点击表名
const handleTableClick = (data) => {
  if (selectedDatabase.value) {
    // 移除反引号和分号,避免触发SQL注入检测
    sqlQuery.value = `SELECT * FROM ${data.name} LIMIT 100`
  }
}

// 执行查询
const executeQueryHandler = async () => {
  if (!selectedConnectionId.value) {
    ElMessage.warning('请先选择数据库连接')
    return
  }
  
  if (!selectedDatabase.value) {
    ElMessage.warning('请先选择数据库')
    return
  }
  
  if (!sqlQuery.value.trim()) {
    ElMessage.warning('请输入 SQL 查询语句')
    return
  }
  
  executing.value = true
  try {
    const startTime = Date.now()
    const res = await executeQuery(
      selectedConnectionId.value,
      selectedDatabase.value,
      sqlQuery.value
    )
    queryResult.value = res.data
    ElMessage.success(`查询成功，共 ${res.data.totalRows} 行`)
  } catch (error) {
    ElMessage.error('查询执行失败')
    queryResult.value = null
  } finally {
    executing.value = false
  }
}

// 执行更新（INSERT/UPDATE/DELETE）
const executeUpdateHandler = async () => {
  if (!selectedConnectionId.value) {
    ElMessage.warning('请先选择数据库连接')
    return
  }
  
  if (!selectedDatabase.value) {
    ElMessage.warning('请先选择数据库')
    return
  }
  
  if (!sqlQuery.value.trim()) {
    ElMessage.warning('请输入 SQL 更新语句')
    return
  }
  
  // 检测是否为危险操作
  const sql = sqlQuery.value.trim().toUpperCase()
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
  
  executing.value = true
  try {
    const res = await executeUpdate(
      selectedConnectionId.value,
      selectedDatabase.value,
      sqlQuery.value
    )
    const affectedRows = res.data
    ElMessage.success(`执行成功，影响 ${affectedRows} 行`)
    
    // 如果是INSERT/UPDATE/DELETE，显示结果
    queryResult.value = {
      columns: ['affected_rows'],
      rows: [{ affected_rows: affectedRows }],
      totalRows: 1,
      executionTime: 0
    }
  } catch (error) {
    ElMessage.error('更新执行失败')
    queryResult.value = null
  } finally {
    executing.value = false
  }
}

// 格式化 SQL
const formatSQL = () => {
  if (!sqlQuery.value.trim()) return
  
  try {
    sqlQuery.value = format(sqlQuery.value, { language: 'mysql' })
  } catch (error) {
    ElMessage.warning('SQL 格式化失败')
  }
}

// 清空编辑器
const clearEditor = () => {
  sqlQuery.value = ''
  queryResult.value = null
}

onMounted(() => {
  loadConnections()
})
</script>

<style scoped lang="scss">
.query-page {
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
      
      .toolbar-right {
        .el-divider--vertical {
          margin: 0 4px;
          height: 24px;
        }
      }
    }
  }
  
  // 主体内容区
  .main-content {
    flex: 1;
    overflow: hidden;
    
    .tables-card {
      height: 100%;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      
      :deep(.el-card__body) {
        flex: 1;
        overflow-y: auto;
        padding: 12px;
      }
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: 600;
      }
      
      .tree-node {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 2px 0;
        
        .el-icon {
          color: #409eff;
          font-size: 14px;
        }
        
        .node-label {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
      
      :deep(.el-tree-node__content) {
        height: 32px;
        border-radius: 4px;
        
        &:hover {
          background-color: #f5f7fa;
        }
      }
      
      :deep(.el-tree-node.is-current > .el-tree-node__content) {
        background-color: #ecf5ff;
        color: #409eff;
      }
    }
    
    .editor-result-wrapper {
      height: 100%;
      display: flex;
      flex-direction: column;
      gap: 16px;
      overflow-y: auto;
      
      .editor-card,
      .result-card {
        border-radius: 8px;
        flex-shrink: 0;
        
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-weight: 600;
          
          .result-info {
            display: flex;
            gap: 8px;
          }
        }
      }
      
      .editor-card {
        .sql-editor {
          font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
          font-size: 14px;
          
          :deep(textarea) {
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            line-height: 1.6;
          }
        }
      }
      
      .result-card {
        :deep(.el-card__body) {
          padding: 0;
        }
        
        &.result-empty {
          :deep(.el-card__body) {
            padding: 20px;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 200px;
          }
        }
      }
    }
  }
}
</style>
