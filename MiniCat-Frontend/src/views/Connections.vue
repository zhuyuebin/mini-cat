<template>
  <div class="connections-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据库连接管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增连接
          </el-button>
        </div>
      </template>

      <div class="table-container">
        <el-table :data="connections" style="width: 100%" v-loading="loading" class="centered-table">
          <el-table-column prop="name" label="连接名称" align="center" show-overflow-tooltip />
          <el-table-column prop="host" label="主机" align="center" show-overflow-tooltip />
          <el-table-column prop="port" label="端口" align="center" />
          <el-table-column prop="databaseType" label="类型" align="center" />
          <el-table-column prop="username" label="用户名" align="center" show-overflow-tooltip />
          <el-table-column label="连接状态" align="center">
            <template #default="{ row }">
              <el-tag :type="getConnectionStatusType(row)" effect="dark" size="small">
                <el-icon style="margin-right: 4px;">
                  <SuccessFilled v-if="row.connectionStatus === 'success'" />
                  <CircleCloseFilled v-else-if="row.connectionStatus === 'failed'" />
                  <QuestionFilled v-else />
                </el-icon>
                {{ getConnectionStatusText(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最后测试时间" align="center">
            <template #default="{ row }">
              <div v-if="row.lastTestTime" class="test-time-cell" :title="formatTestTime(row.lastTestTime)">
                {{ formatRelativeTime(row.lastTestTime) }}
              </div>
              <span v-else class="no-test-time">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" min-width="280">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button size="small" @click="handleTest(row)">测试</el-button>
                <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
                <el-button size="small" type="success" @click="handleSelect(row)">选择</el-button>
                <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑连接' : '新增连接'"
      width="600px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="连接名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入连接名称" />
        </el-form-item>
        <el-form-item label="数据库类型" prop="databaseType">
          <el-select v-model="form.databaseType" placeholder="请选择数据库类型" style="width: 100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机" prop="host">
          <el-input v-model="form.host" placeholder="例如: localhost" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit && !form.password ? '' : 'password'" :rules="passwordRules">
          <el-input v-model="form.password" type="password" placeholder="请输入密码（编辑时留空表示不修改）" show-password />
        </el-form-item>
        <el-form-item label="字符集" prop="charset">
          <el-input v-model="form.charset" placeholder="例如: utf8mb4" />
        </el-form-item>
        <el-form-item label="是否活跃">
          <el-switch v-model="form.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, SuccessFilled, CircleCloseFilled, QuestionFilled } from '@element-plus/icons-vue'
import { getAllConnections, addConnection, updateConnection, deleteConnection, testConnection } from '@/api/database'
import { useConnectionStore } from '@/stores/connection'

const connectionStore = useConnectionStore()
const connections = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = ref({
  id: '',
  name: '',
  host: 'localhost',
  port: 3306,
  username: '',
  password: '',
  databaseType: 'mysql',
  charset: 'utf8mb4',
  active: true
})

const rules = {
  name: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口号', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  databaseType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }]
}

// 动态密码验证规则
const passwordRules = computed(() => {
  // 编辑模式下且密码为空时,不要求必填
  if (isEdit.value && !form.value.password) {
    return []
  }
  return [{ required: true, message: '请输入密码', trigger: 'blur' }]
})

// 加载连接列表
const loadConnections = async () => {
  loading.value = true
  try {
    const res = await getAllConnections()
    connections.value = res.data || []
  } catch (error) {
    console.error('加载连接失败:', error)
  } finally {
    loading.value = false
  }
}

// 新增连接
const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: '',
    name: '',
    host: 'localhost',
    port: 3306,
    username: '',
    password: '',
    databaseType: 'mysql',
    charset: 'utf8mb4',
    active: true
  }
  dialogVisible.value = true
}

// 编辑连接
const handleEdit = (row) => {
  isEdit.value = true
  form.value = { ...row }
  // 后端不返回密码,编辑时需要清空密码字段,让用户选择是否修改
  form.value.password = ''
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateConnection(form.value.id, form.value)
        ElMessage.success('更新成功')
      } else {
        await addConnection(form.value)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      await loadConnections()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

// 删除连接
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该连接吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteConnection(row.id)
      ElMessage.success('删除成功')
      await loadConnections()
    } catch (error) {
      console.error('删除失败:', error)
    }
  })
}

// 测试连接
const handleTest = async (row) => {
  try {
    const res = await testConnection(row.id)
    if (res.data) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error('连接测试失败')
    }
    // 重新加载连接列表以获取最新状态
    await loadConnections()
  } catch (error) {
    ElMessage.error('连接测试失败')
    // 重新加载连接列表以获取最新状态
    await loadConnections()
  }
}

// 获取连接状态文本
const getConnectionStatusText = (row) => {
  if (!row.connectionStatus || row.connectionStatus === 'unknown') {
    return '未测试'
  }
  return row.connectionStatus === 'success' ? '连接成功' : '连接失败'
}

// 获取连接状态标签类型
const getConnectionStatusType = (row) => {
  if (!row.connectionStatus || row.connectionStatus === 'unknown') {
    return 'info'
  }
  return row.connectionStatus === 'success' ? 'success' : 'danger'
}

// 格式化测试时间为相对时间
const formatRelativeTime = (testTime) => {
  if (!testTime) return ''
  
  const now = new Date()
  const testDate = new Date(testTime)
  const diff = now - testDate
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  return `${days}天前`
}

// 格式化完整测试时间
const formatTestTime = (testTime) => {
  if (!testTime) return ''
  const date = new Date(testTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 选择连接
const handleSelect = (row) => {
  connectionStore.setCurrentConnection(row)
  ElMessage.success(`已选择连接: ${row.name}`)
}

onMounted(() => {
  loadConnections()
})
</script>

<style scoped lang="scss">
.connections-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .table-container {
    width: 100%;
    overflow-x: auto;
    
    :deep(.el-table) {
      width: 100% !important;
    }
  }

  .centered-table {
    width: 100%;

    :deep(.el-table__header th) {
      text-align: center;
      background-color: #f5f7fa;
      font-weight: 600;
      height: 50px;
      padding: 12px 0;
    }

    :deep(.el-table__body td) {
      text-align: center;
      height: 60px;
      padding: 8px 0;
    }
    
    // 固定列样式优化
    :deep(.el-table__fixed),
    :deep(.el-table__fixed-right) {
      .el-table__row {
        height: 60px;
        
        td {
          height: 60px;
          padding: 8px 0;
        }
      }
    }

    :deep(.el-table__row) {
      transition: all 0.3s ease;
      height: 60px;
      
      &:hover {
        background-color: #ecf5ff;
      }
    }

    :deep(.el-table) {
      width: 100% !important;
    }
  }
  
  .test-time-cell {
    font-size: 12px;
    color: #606266;
    white-space: nowrap;
  }
  
  .no-test-time {
    font-size: 12px;
    color: #c0c4cc;
  }
  
  .action-buttons {
    display: flex;
    gap: 6px;
    justify-content: center;
    flex-wrap: nowrap;
    
    .el-button {
      padding: 5px 10px;
      font-size: 12px;
    }
  }
}
</style>
