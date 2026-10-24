<template>
  <header class="top-bar">
    <div class="brand" @click="goWorkspace">
      <span class="brand-logo">🐱</span>
      <span class="brand-name">MiniCat</span>
      <span class="brand-sub">轻量级数据库管理工具</span>
    </div>

    <div class="top-actions">
      <el-button text @click="goSettings">
        <el-icon><Setting /></el-icon>
        <span>设置</span>
      </el-button>
      <el-button text @click="goWelcome">
        <el-icon><QuestionFilled /></el-icon>
        <span>帮助</span>
      </el-button>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { QuestionFilled, Setting } from '@element-plus/icons-vue'

const router = useRouter()

function goWorkspace() {
  router.push({ name: 'Workspace' })
}

function goSettings() {
  router.push({ name: 'Settings' })
}

function goWelcome() {
  router.push({ name: 'Welcome' })
}
</script>

<style scoped lang="scss">
.top-bar {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;

  .brand-logo {
    font-size: 20px;
    line-height: 1;
  }

  .brand-name {
    font-size: 17px;
    font-weight: 700;
    color: #303133;
  }

  .brand-sub {
    font-size: 12px;
    color: #909399;
    margin-left: 4px;
  }
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 4px;

  .el-button {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}
</style>
