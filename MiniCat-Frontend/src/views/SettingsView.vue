<template>
  <div class="settings-view">
    <div class="page-header">
      <div class="header-left">
        <h2>设置 · 连接管理</h2>
        <span class="sub">管理 MySQL / PostgreSQL 连接，测试连通性</span>
      </div>
      <div class="header-actions">
        <el-button @click="backToWorkspace">返回工作区</el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新建连接</span>
        </el-button>
      </div>
    </div>

    <el-alert
      class="pg-alert"
      type="info"
      :closable="false"
      title="PostgreSQL 连接说明"
      description="当前版本 PostgreSQL 仅支持「新增 / 编辑 / 测试连接」，库表浏览与查询能力开发中（P0-4）；不影响同时存在的 MySQL 连接。"
    />

    <el-card shadow="never">
      <el-table :data="store.connections" v-loading="store.connectionsLoading" style="width: 100%">
        <el-table-column label="连接名称" prop="name" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :color="capabilitiesOf(row).color">
              {{ capabilitiesOf(row).badge }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主机" prop="host" min-width="140" show-overflow-tooltip />
        <el-table-column label="端口" prop="port" width="80" align="center" />
        <el-table-column label="用户名" prop="username" min-width="100" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row)" size="small">{{ statusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="testConnectionRow(row)" :loading="testingId === row.id">测试</el-button>
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="danger-zone">
      <div>
        <h3>本地工作区状态</h3>
        <p>清空多 Tab、展开状态、上次选择与每页条数（不影响数据库连接）。</p>
      </div>
      <el-button @click="clearWorkspaceState">清空本地工作区状态</el-button>
    </div>

    <ConnectionDialog
      v-model="dialogVisible"
      :connection="editingConnection"
      :edit="editMode"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { deleteConnection, testConnection } from '@/api/database'
import { useWorkspaceStore } from '@/stores/workspace'
import ConnectionDialog from '@/components/ConnectionDialog.vue'

const router = useRouter()
const store = useWorkspaceStore()

const dialogVisible = ref(false)
const editMode = ref(false)
const editingConnection = ref(null)
const testingId = ref(null)

function capabilitiesOf(connection) {
  return store.capabilitiesOf(connection)
}

function statusText(connection) {
  if (connection.connectionStatus === 'success') return '连接成功'
  if (connection.connectionStatus === 'failed') return '连接失败'
  return '未测试'
}

function statusTagType(connection) {
  if (connection.connectionStatus === 'success') return 'success'
  if (connection.connectionStatus === 'failed') return 'danger'
  return 'info'
}

function backToWorkspace() {
  router.push({ name: 'Workspace' })
}

function openCreate() {
  editMode.value = false
  editingConnection.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  editMode.value = true
  editingConnection.value = { ...row }
  dialogVisible.value = true
}

async function onSaved() {
  await store.refreshConnections()
}

async function testConnectionRow(row) {
  testingId.value = row.id
  try {
    await testConnection(row.id)
    ElMessage.success(`连接「${row.name}」测试成功`)
  } catch (error) {
    console.error('连接测试失败:', error)
  } finally {
    testingId.value = null
    await store.refreshConnections()
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除连接「${row.name}」吗？该连接下已打开的工作区 Tab 也会关闭。`, '删除连接', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }
  try {
    await deleteConnection(row.id)
    store.closeTabsForConnection(row.id)
    await store.refreshConnections()
    ElMessage.success('连接已删除')
  } catch (error) {
    console.error('删除连接失败:', error)
  }
}

async function clearWorkspaceState() {
  try {
    await ElMessageBox.confirm('将清空本地保存的 Tab、展开状态与上次选择，确定继续吗？', '清空工作区状态', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }
  store.clearPersistedState()
  ElMessage.success('本地工作区状态已清空')
}

onMounted(() => {
  if (!store.connections.length) store.loadConnections()
})
</script>

<style scoped lang="scss">
.settings-view {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 20px 24px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;

  .header-left {
    display: flex;
    flex-direction: column;
    gap: 4px;

    h2 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }

    .sub {
      font-size: 12px;
      color: #909399;
    }
  }

  .header-actions {
    display: flex;
    gap: 8px;

    .el-button {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.pg-alert {
  margin-bottom: 16px;
}

.danger-zone {
  margin-top: 20px;
  padding: 16px 18px;
  border: 1px dashed #f3d19e;
  border-radius: 8px;
  background: #fdf6ec;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    margin: 0 0 4px;
    font-size: 14px;
    color: #b88230;
  }

  p {
    margin: 0;
    font-size: 12px;
    color: #909399;
  }
}
</style>
