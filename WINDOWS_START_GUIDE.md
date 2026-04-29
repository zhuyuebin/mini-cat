# MiniCat Windows 启动指南

## 快速开始

### 方法一：使用启动脚本（推荐）

直接双击运行 `start_win.bat` 文件即可。

### 方法二：手动命令行启动

打开命令提示符（CMD）或 PowerShell，在项目根目录执行：

```bash
# 使用 Maven Wrapper 构建项目（正确写法）
mvnw.cmd clean package -DskipTests

# 或者如果你的系统安装了 Maven
mvn clean package -DskipTests
```

## ⚠️ 常见错误

### ❌ 错误的命令格式

```bash
# 错误！不要在 mvnw.cmd 后面再加 mvn
mvnw.cmd mvn clean package
```

这会报错：
```
[ERROR] Unknown lifecycle phase "mvn"
```

### ✅ 正确的命令格式

```bash
# 正确！直接使用 mvnw.cmd + Maven 参数
mvnw.cmd clean package -DskipTests
```

## 启动流程

1. **检查 Java 环境**
   - 需要 JDK 17 或更高版本
   - 运行 `java -version` 确认

2. **构建项目**
   ```bash
   mvnw.cmd clean package -DskipTests
   ```

3. **运行应用**
   ```bash
   java -jar target/minicat-server-0.0.1-SNAPSHOT.jar
   ```

4. **访问应用**
   - 浏览器打开：http://localhost:8888

## 故障排查

### 问题 1：'mvnw.cmd' 不是内部或外部命令

**解决方案：**
- 确保你在项目根目录下执行命令
- 使用完整路径：`.\mvnw.cmd clean package`

### 问题 2：首次构建很慢

**原因：** Maven Wrapper 需要下载 Maven 和依赖包

**解决方案：**
- 等待下载完成（只需一次）
- 可以配置国内镜像加速

### 问题 3：端口被占用

**解决方案：**
修改 `src/main/resources/application.properties` 中的端口配置：
```properties
server.port=8889
```

## 开发模式

如果需要前后端同时开发：

### 后端（Spring Boot）
```bash
mvnw.cmd spring-boot:run
```

### 前端（Vue）
```bash
cd MiniCat-Frontend
npm install
npm run dev
```

## 需要帮助？

- 查看主 README.md 获取更多文档
- 检查 MD-Files 目录下的详细文档
