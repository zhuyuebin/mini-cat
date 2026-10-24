<template>
  <div class="welcome-view" :class="{ embedded }">
    <div class="welcome-hero">
      <img src="/logo.png" alt="MiniCat" class="logo" />
      <h1 class="title">MiniCat</h1>
      <p class="subtitle">轻量级 Web 数据库管理工具 — 连接、浏览、查询、导入，一个 JAR 打开即用</p>
      <div class="hero-actions">
        <el-button type="primary" @click="openSettings">新建 / 管理连接</el-button>
        <el-button :disabled="!store.hasConnections" @click="newQuery">新建 SQL 查询</el-button>
      </div>
    </div>

    <div class="guide-grid">
      <div v-for="step in steps" :key="step.index" class="guide-card">
        <span class="guide-index">{{ step.index }}</span>
        <h3>{{ step.title }}</h3>
        <p>{{ step.desc }}</p>
      </div>
    </div>

    <div v-if="store.hasConnections" class="recent-section">
      <h3 class="section-title">连接</h3>
      <div class="recent-list">
        <div
          v-for="connection in store.connections"
          :key="connection.id"
          class="recent-item"
          @click="openQueryFor(connection)"
        >
          <el-tag size="small" effect="dark" :color="capabilitiesOf(connection).color">
            {{ capabilitiesOf(connection).badge }}
          </el-tag>
          <span class="recent-name">{{ connection.name }}</span>
          <span class="recent-meta">{{ connection.host }}:{{ connection.port }}</span>
          <span class="status-dot" :class="statusClass(connection)" />
        </div>
      </div>
    </div>

    <p class="hint">提示：左侧资源树是唯一导航入口；数据默认保存在启动目录的 <code>data/</code> 下。</p>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useWorkspaceStore } from '@/stores/workspace'

defineProps({
  embedded: { type: Boolean, default: false }
})

const router = useRouter()
const store = useWorkspaceStore()

const steps = [
  { index: 1, title: '新建连接', desc: '打开「设置」添加 MySQL / PostgreSQL 连接，并测试连通性' },
  { index: 2, title: '展开资源树', desc: '在左侧依次展开连接 → 数据库 → 数据表' },
  { index: 3, title: '浏览与编辑', desc: '单击表打开工作区，双击直接查看表数据、结构与分页' },
  { index: 4, title: 'SQL 查询', desc: '按 Alt+T 新建查询 Tab，写 SQL 并执行，草稿自动保留' }
]

function capabilitiesOf(connection) {
  return store.capabilitiesOf(connection)
}

function statusClass(connection) {
  if (connection.connectionStatus === 'success') return 'success'
  if (connection.connectionStatus === 'failed') return 'failed'
  return 'unknown'
}

function openSettings() {
  router.push({ name: 'Settings' })
}

function openQueryFor(connection) {
  if (!capabilitiesOf(connection).query) {
    ElMessage.info(capabilitiesOf(connection).hint || '该连接暂不支持查询')
    return
  }
  const result = store.openQueryTab({ connectionId: connection.id })
  if (result.ok) router.push({ name: 'Workspace' })
}

function newQuery() {
  const result = store.openQueryTab()
  if (result.ok) {
    router.push({ name: 'Workspace' })
  }
}
</script>

<style scoped lang="scss">
.welcome-view {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 40px 32px;
  background: linear-gradient(180deg, #f5f8ff 0%, #f0f2f5 260px, #f0f2f5 100%);

  &.embedded {
    padding: 32px;
  }
}

.welcome-hero {
  text-align: center;
  max-width: 720px;
  margin: 0 auto;

  .logo {
    width: 84px;
    height: 84px;
    border-radius: 20px;
    background: #ffffff;
    padding: 10px;
    box-shadow: 0 8px 24px rgba(31, 111, 235, 0.12);
  }

  .title {
    margin: 14px 0 6px;
    font-size: 30px;
    color: #303133;
  }

  .subtitle {
    margin: 0;
    color: #606266;
    font-size: 14px;
  }

  .hero-actions {
    margin-top: 18px;
    display: flex;
    justify-content: center;
    gap: 12px;
  }
}

.guide-grid {
  max-width: 900px;
  margin: 32px auto 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.guide-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);

  .guide-index {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #ecf5ff;
    color: #409eff;
    font-size: 12px;
    font-weight: 600;
  }

  h3 {
    margin: 10px 0 6px;
    font-size: 14px;
    color: #303133;
  }

  p {
    margin: 0;
    font-size: 12px;
    line-height: 1.6;
    color: #909399;
  }
}

.recent-section {
  max-width: 900px;
  margin: 28px auto 0;

  .section-title {
    font-size: 14px;
    color: #606266;
    margin: 0 0 10px;
  }
}

.recent-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #ebeef5;
  font-size: 13px;
  cursor: pointer;

  &:hover {
    border-color: #b3d8ff;
    background: #f5faff;
  }

  .recent-name {
    font-weight: 500;
    color: #303133;
  }

  .recent-meta {
    color: #909399;
    font-size: 12px;
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #c0c4cc;

    &.success {
      background: #67c23a;
    }

    &.failed {
      background: #f56c6c;
    }
  }
}

.hint {
  max-width: 900px;
  margin: 28px auto 0;
  text-align: center;
  font-size: 12px;
  color: #a8abb2;

  code {
    background: #eef1f6;
    padding: 1px 5px;
    border-radius: 4px;
  }
}
</style>
