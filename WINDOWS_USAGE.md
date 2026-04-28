# 🪟 MiniCat Windows 使用指南

## 📋 前置要求

在 Windows 上运行 MiniCat，您需要：

### ✅ 必需软件

1. **Java JDK 17 或更高版本**
   - 下载地址: https://adoptium.net/
   - 推荐: Eclipse Temurin 17 LTS
   - 安装后验证: `java -version`

2. **Git** (用于克隆仓库)
   - 下载地址: https://git-scm.com/download/win
   - 安装后验证: `git --version`

### ❌ 不需要预装

- ~~Maven~~ - 项目包含 Maven Wrapper，会自动下载
- ~~Node.js~~ - Maven 插件会自动处理前端构建

---

## 🚀 快速开始（三种方式）

### 方式一：使用启动脚本（最简单）⭐

#### 步骤 1: 克隆项目

打开命令提示符（CMD）或 PowerShell：

```bash
# 克隆仓库
git clone https://gitee.com/zhuyuebin/mini-cat.git

# 进入目录
cd mini-cat
```

#### 步骤 2: 运行启动脚本

**方法 A - 双击运行：**
- 在文件资源管理器中找到 `start.bat`
- 双击运行

**方法 B - 命令行运行：**
```bash
start.bat
```

#### 步骤 3: 等待构建完成

首次运行会：
1. 下载 Maven（约 10MB）
2. 下载 Java 依赖（约 100-200MB）
3. 下载 Node.js 和 npm 依赖
4. 构建前端
5. 编译后端
6. 启动应用

⏱️ **预计时间**: 5-15 分钟（取决于网络速度）

#### 步骤 4: 访问应用

看到以下提示表示启动成功：
```
=========================================
  启动MiniCat...
=========================================

访问地址: http://localhost:8888

提示: 首次启动需要下载依赖,可能需要几分钟

按 Ctrl+C 停止服务
```

打开浏览器访问: **http://localhost:8888**

---

### 方式二：使用 Docker（推荐）

如果您已安装 Docker Desktop：

```bash
# 克隆项目
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd mini-cat

# 一键启动
docker-compose up -d

# 查看日志
docker-compose logs -f
```

访问: **http://localhost:8888**

---

### 方式三：手动构建（开发者）

适合需要调试或自定义构建的场景。

#### 步骤 1: 克隆项目
```bash
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd mini-cat
```

#### 步骤 2: 构建项目
```bash
# 使用 Maven Wrapper
mvnw.cmd clean package -DskipTests

# 或者如果已安装 Maven
mvn clean package -DskipTests
```

#### 步骤 3: 运行应用
```bash
java -Xms512m -Xmx1024m -jar target\minicat-server-0.0.1-SNAPSHOT.jar
```

---

## 🔧 常见问题与解决方案

### 问题 1: 'java' 不是内部或外部命令

**原因**: Java 未安装或未添加到系统 PATH

**解决方案**:
1. 下载并安装 JDK 17: https://adoptium.net/
2. 安装时勾选 "Add to PATH"
3. 重新打开命令行窗口
4. 验证: `java -version`

---

### 问题 2: 端口 8888 已被占用

**症状**: 启动失败，提示端口被占用

**解决方案**:

**方法 A - 查找并关闭占用进程:**
```bash
# 查找占用端口的进程
netstat -ano | findstr :8888

# 记下 PID，然后结束进程
taskkill /F /PID <PID>
```

**方法 B - 修改端口:**

编辑 `src\main\resources\application.properties`:
```properties
server.port=9090
```

重新启动应用。

---

### 问题 3: 内存不足错误

**症状**: 出现 `OutOfMemoryError` 或构建失败

**解决方案**:

增加 JVM 内存参数，编辑 `start.bat` 最后一行：
```batch
java -Xms1024m -Xmx2048m -jar "%JAR_FILE%"
```

或在命令行运行时：
```bash
java -Xms1024m -Xmx2048m -jar target\minicat-server-0.0.1-SNAPSHOT.jar
```

---

### 问题 4: Maven 下载依赖很慢

**原因**: 默认使用中央仓库，国内访问较慢

**解决方案**: 配置阿里云镜像

创建或编辑 `%USERPROFILE%\.m2\settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <mirrors>
        <mirror>
            <id>aliyun-maven</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Maven Mirror</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
```

---

### 问题 5: 防火墙阻止连接

**症状**: 无法访问 http://localhost:8888

**解决方案**:

1. 打开 Windows 防火墙设置
2. 允许 Java 通过防火墙
3. 或者临时关闭防火墙测试

---

### 问题 6: 中文乱码

**症状**: 控制台显示中文为乱码

**解决方案**:

start.bat 已包含编码设置：
```batch
chcp 65001 >nul
```

如果仍有问题，确保：
1. 使用 UTF-8 编码保存文件
2. 命令行窗口支持 UTF-8

---

## 📁 项目结构说明

```
mini-cat/
├── start.bat                 # Windows 启动脚本 ⭐
├── mvnw.cmd                  # Maven Wrapper
├── pom.xml                   # Maven 配置
├── src/                      # 后端源代码
├── MiniCat-Frontend/         # 前端项目
├── data/                     # H2 数据库文件（自动生成）
└── target/                   # 构建输出（自动生成）
```

---

## 🛠️ 开发模式

如果需要前后端分离开发：

### 启动后端
```bash
# 在 mini-cat 目录
mvnw.cmd spring-boot:run
```

### 启动前端
```bash
# 在新命令行窗口
cd MiniCat-Frontend
npm install
npm run dev
```

前端访问: **http://localhost:5173**  
后端 API: **http://localhost:8888**

---

## 📊 性能优化建议

### 1. 调整 JVM 参数

根据机器配置调整内存：

```batch
# 低配机器 (4GB RAM)
java -Xms256m -Xmx512m -jar ...

# 中等配置 (8GB RAM)
java -Xms512m -Xmx1024m -jar ...

# 高配机器 (16GB+ RAM)
java -Xms1024m -Xmx2048m -jar ...
```

### 2. 使用 SSD 硬盘

将项目放在 SSD 上可以显著提升构建速度。

### 3. 关闭不必要的应用

构建期间关闭浏览器、IDE 等占用内存的应用。

---

## 🔄 更新项目

当项目有新版本时：

```bash
# 拉取最新代码
git pull origin main

# 重新构建
start.bat
```

或者手动：
```bash
mvnw.cmd clean package -DskipTests
java -jar target\minicat-server-0.0.1-SNAPSHOT.jar
```

---

## 🗑️ 卸载/清理

如需清理构建产物：

```bash
# 清理 Maven 构建
mvnw.cmd clean

# 或删除整个 target 目录
rmdir /s /q target

# 清理前端依赖
rmdir /s /q MiniCat-Frontend\node_modules
```

---

## 📞 获取帮助

如果遇到问题：

1. **查看日志**: 检查控制台输出的错误信息
2. **查阅文档**: [README.md](README.md)
3. **提交 Issue**: https://gitee.com/zhuyuebin/mini-cat/issues

---

## ✅ 验证清单

启动成功后，您应该能够：

- [ ] 访问 http://localhost:8888
- [ ] 看到 MiniCat 登录/主界面
- [ ] 添加数据库连接
- [ ] 执行 SQL 查询
- [ ] 浏览数据表

---

## 💡 小贴士

1. **首次启动最慢**: 后续启动会快很多（缓存了依赖）
2. **保持网络畅通**: 构建过程需要下载大量依赖
3. **使用 PowerShell**: 比 CMD 有更好的 Unicode 支持
4. **管理员权限**: 某些情况下可能需要以管理员身份运行

---

**祝您使用愉快！🎉**

如有任何问题，欢迎提交 Issue 反馈。
