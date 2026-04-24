<template>
  <div class="home-page">
    <div class="hero-section">
      <div class="logo-container">
        <img src="/logo.png" alt="MiniCat Logo" class="logo" />
      </div>
      <h1 class="title">MiniCat</h1>
      <p class="subtitle">轻量级数据库管理工具</p>
    </div>

    <div class="content-section">
      <el-card class="intro-card">
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            <span>项目介绍</span>
          </div>
        </template>
        <div class="intro-content">
          <p>MiniCat 是一款现代化的轻量级数据库管理工具，旨在为开发者提供简洁、高效的数据库操作体验。</p>
          <div class="features">
            <div class="feature-item">
              <el-icon color="#409EFF"><Connection /></el-icon>
              <span>多数据库连接管理</span>
            </div>
            <div class="feature-item">
              <el-icon color="#67C23A"><Document /></el-icon>
              <span>SQL查询执行</span>
            </div>
            <div class="feature-item">
              <el-icon color="#E6A23C"><Grid /></el-icon>
              <span>数据表浏览与管理</span>
            </div>
            <div class="feature-item">
              <el-icon color="#F56C6C"><Lock /></el-icon>
              <span>安全可靠的数据访问</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="author-card">
        <template #header>
          <div class="card-header">
            <el-icon><User /></el-icon>
            <span>关于作者</span>
          </div>
        </template>
        <div class="author-content">
          <p><strong>作者：</strong>zhuyuebin</p>
          <p class="gitee-link">
            <strong>Gitee：</strong>
            <a href="https://gitee.com/zhuyuebin/mini-cat" target="_blank" class="link">
              https://gitee.com/zhuyuebin/mini-cat
              <el-icon><Link /></el-icon>
            </a>
          </p>
        </div>
      </el-card>

      <div class="action-section">
        <el-button type="primary" size="large" @click="goToConnections" class="enter-btn">
          <el-icon><Connection /></el-icon>
          <span>进入连接管理</span>
        </el-button>
      </div>
    </div>

    <footer class="footer">
      <p>© 2026 MiniCat. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { InfoFilled, User, Link, Lock } from '@element-plus/icons-vue'

const router = useRouter()

const goToConnections = () => {
  router.push('/connections')
}
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}

.hero-section {
  text-align: center;
  margin-bottom: 40px;
  animation: fadeInDown 0.8s ease-out;

  .logo-container {
    margin-bottom: 20px;

    .logo {
      width: 120px;
      height: 120px;
      border-radius: 50%;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
      background: white;
      padding: 10px;
      transition: transform 0.3s ease;

      &:hover {
        transform: scale(1.05);
      }
    }
  }

  .title {
    font-size: 48px;
    color: white;
    font-weight: 700;
    margin: 0 0 10px 0;
    text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
  }

  .subtitle {
    font-size: 20px;
    color: rgba(255, 255, 255, 0.9);
    margin: 0;
  }
}

.content-section {
  width: 100%;
  max-width: 800px;
  animation: fadeInUp 0.8s ease-out 0.2s both;

  .intro-card,
  .author-card {
    margin-bottom: 20px;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

    :deep(.el-card__header) {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-bottom: none;
      padding: 15px 20px;
    }

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      color: white;
      font-size: 18px;
      font-weight: 600;

      .el-icon {
        font-size: 20px;
      }
    }

    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .intro-content {
    p {
      font-size: 16px;
      line-height: 1.8;
      color: #606266;
      margin-bottom: 20px;
    }

    .features {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 15px;

      .feature-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 12px;
        background: #f5f7fa;
        border-radius: 8px;
        transition: all 0.3s ease;

        &:hover {
          background: #ecf5ff;
          transform: translateX(5px);
        }

        .el-icon {
          font-size: 24px;
        }

        span {
          font-size: 15px;
          color: #303133;
        }
      }
    }
  }

  .author-content {
    p {
      font-size: 16px;
      line-height: 2;
      color: #606266;
      margin: 10px 0;
    }

    .gitee-link {
      .link {
        color: #409EFF;
        text-decoration: none;
        display: inline-flex;
        align-items: center;
        gap: 5px;
        transition: all 0.3s ease;

        &:hover {
          color: #66b1ff;
          text-decoration: underline;
        }

        .el-icon {
          font-size: 16px;
        }
      }
    }
  }

  .action-section {
    text-align: center;
    margin-top: 30px;

    .enter-btn {
      padding: 15px 40px;
      font-size: 18px;
      border-radius: 30px;
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
      }

      .el-icon {
        font-size: 20px;
        margin-right: 8px;
      }
    }
  }
}

.footer {
  margin-top: auto;
  padding-top: 40px;
  text-align: center;

  p {
    color: rgba(255, 255, 255, 0.8);
    font-size: 14px;
  }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .hero-section {
    .title {
      font-size: 36px;
    }

    .subtitle {
      font-size: 16px;
    }
  }

  .content-section {
    .intro-content {
      .features {
        grid-template-columns: 1fr;
      }
    }
  }
}
</style>
