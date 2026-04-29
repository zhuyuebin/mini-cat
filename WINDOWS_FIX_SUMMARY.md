# Windows 启动问题解决方案

## 问题分析

你在 Windows 上遇到的错误信息：

```
[ERROR] Unknown lifecycle phase "mvn"
```

### 根本原因

你执行的命令是：
```bash
mvnw.cmd mvn clean package
```

这是**错误的**！Maven Wrapper (`mvnw.cmd`) 本身就是 Maven 的包装器，不需要再加 `mvn`。

## 正确的命令

### ❌ 错误写法
```bash
mvnw.cmd mvn clean package
```

### ✅ 正确写法
```bash
mvnw.cmd clean package -DskipTests
```

## 快速解决步骤

### 方法一：使用启动脚本（最简单）

直接双击运行项目根目录下的：
```
start_win.bat
```

这个脚本会自动：
1. 检测 Java 环境
2. 使用 Maven Wrapper 构建项目
3. 启动应用

### 方法二：手动命令行

打开 CMD 或 PowerShell，在项目根目录执行：

```bash
# 1. 构建项目
mvnw.cmd clean package -DskipTests

# 2. 运行应用
java -jar target/minicat-server-0.0.1-SNAPSHOT.jar
```

## 常见问题

### Q1: 提示 "'mvnw.cmd' 不是内部或外部命令"

**解决方法：**
- 确保你在项目根目录下
- 使用完整路径：`.\mvnw.cmd clean package`
- 或者检查文件是否存在

### Q2: 首次构建很慢

**原因：** Maven Wrapper 需要下载 Maven 和依赖包

**解决方法：**
- 耐心等待（只需一次）
- 后续构建会快很多

### Q3: 端口被占用

**解决方法：**
修改 `src/main/resources/application.properties`：
```properties
server.port=8889
```

## 项目已优化

我已经为你做了以下优化：

1. ✅ 删除了旧的 `start.bat`，统一使用 `start_win.bat`
2. ✅ 优化了 `start_win.bat`，增加了更清晰的错误提示
3. ✅ 创建了详细的 Windows 启动指南：`WINDOWS_START_GUIDE.md`
4. ✅ 更新了主 README，明确说明 Windows 用户的正确启动方式

## 下一步

现在你可以：

1. **直接使用启动脚本**：双击 `start_win.bat`
2. **查看详细文档**：打开 `WINDOWS_START_GUIDE.md`
3. **开始使用 MiniCat**：访问 http://localhost:8888

---

如果还有问题，请查看：
- `WINDOWS_START_GUIDE.md` - 详细的 Windows 启动指南
- `README.md` - 项目主文档
- `MD-Files/` 目录 - 更多技术文档
