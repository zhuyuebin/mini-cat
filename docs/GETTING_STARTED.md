# 安装与首次使用

> 适用版本：v1.2.0。想先了解项目？见 [README](../README.md)；接口与架构见 [API.md](API.md) / [ARCHITECTURE.md](ARCHITECTURE.md)。

---

## 1. 前置要求

| 场景 | 需要 |
|---|---|
| 直接运行已构建 JAR | **JDK 17+**，无需 Node/Maven |
| 从源码构建 | JDK 17+、Maven 3.6+（或仓库自带 `mvnw`）；首次构建会由 Maven 插件下载 Node 20 与前端依赖 |
| Docker 部署 | Docker 20.10+、Docker Compose 2.0+ |
| 浏览器 | Chrome / Edge / Firefox / Safari 现代版本 |

检查 Java：

```bash
java -version    # 期望 17 或更高
```

---

## 2. 获取代码

```bash
# GitHub（主仓库）
git clone https://github.com/zhuyuebin/mini-cat.git
cd mini-cat

# Gitee（国内备份，访问 GitHub 慢时用）
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd mini-cat
```

---

## 3. 启动方式

### 3.1 一键脚本（本机使用推荐）

**Windows**

```cmd
start_win.bat
```

- 双击或命令行执行均可；
- 脚本会检查 Java、构建项目、然后启动 JAR；
- 若系统没有 Maven，会自动使用 `mvnw.cmd`；
- 首次构建会下载依赖，耗时较长（之后可跳过构建直接 `java -jar`）。

**Linux / macOS**

```bash
chmod +x start.sh
./start.sh
```

### 3.2 手动构建 + 运行（JAR）

```bash
# 构建（自动构建前端并打包进 JAR）
./mvnw clean package -DskipTests      # Windows: mvnw.cmd clean package -DskipTests

# 运行
java -jar target/minicat-server-1.2.0.jar
```

> Windows 上注意：不要写成 `mvnw.cmd mvn ...`，正确是 `mvnw.cmd clean package -DskipTests`。

启动成功标志（控制台）：

```
Tomcat started on port 8888 (http) with context path ''
Started MiniCatApplication in x.xxx seconds
```

### 3.3 Docker

```bash
docker compose up -d
docker compose logs -f
```

见 [DEPLOYMENT.md](DEPLOYMENT.md)。

### 3.4 开发模式（前后端分离）

```bash
# 终端 1：后端（8888）
./mvnw spring-boot:run

# 终端 2：前端（5173，/api 代理到 8888）
cd MiniCat-Frontend
npm install --registry=https://registry.npmmirror.com
npm run dev
```

浏览器访问 `http://localhost:5173`（开发模式也使用 Hash 路由）。

---

## 4. 访问应用

打开：**<http://localhost:8888/#/workspace>**

- 项目使用 **Hash 路由**（后端静态资源无 SPA fallback），地址中的 `#/workspace` 不能省略；
- 旧地址 `/tables`、`/connections`、`/query` 会自动重定向到新工作区；
- 首次打开建议 `Ctrl+Shift+R` 强刷，避免浏览器缓存旧前端资源。

---

## 5. 首次使用（4 步）

1. 右上角 **设置** → **新建连接**：填名称、类型（MySQL）、主机、端口、用户名、密码 → 保存；
2. 回到 **工作区**，左侧资源树展开：连接 → 数据库 → 数据表；
   - **单击**表名：打开/聚焦该表的工作区 Tab；
   - **双击**表名：直接进入「数据」子页；
3. 「数据」页：服务端分页浏览，**双击单元格**可直接修改（Enter/失焦保存，Esc 取消）；
4. `Alt+T` 新建 SQL 查询 Tab，输入 SQL 后 `Ctrl+Enter` 执行。

更多界面说明见 [USER_GUIDE.md](USER_GUIDE.md)。

---

## 6. 停止与重启

```bash
# 前台运行：Ctrl+C 停止

# 后台运行（Linux/macOS，项目自带脚本）
./restart.sh

# Docker
docker compose down
```

Windows 若关闭了窗口但进程仍在，可结束占用 8888 的 Java 进程：

```powershell
netstat -ano | findstr :8888
Stop-Process -Id <PID> -Force
```

---

## 7. 数据目录（重要）

连接配置与加密密钥保存在**启动时工作目录**下的 `data/`：

| 文件 | 内容 |
|---|---|
| `data/minicat_db.mv.db` | H2 元数据库（所有连接配置） |
| `data/secret.key` | 密码加密密钥（首次启动生成） |
| `data/minicat_db.lock.db` | 运行锁（异常退出可能残留） |

- **同一目录重启** → 连接保留；
- **换目录启动** → 会创建新的空 `data/`（看起来像连接丢了）；
- **删除 `data/`** → 清空所有连接（不可恢复）；
- **备份 `data/`** = 备份全部连接配置与密钥；
- 用 `MINICAT_SECRET_KEY` 环境变量可替代密钥文件（远程部署推荐）。

详见 [CONFIGURATION.md](CONFIGURATION.md)。

---

## 8. 常见问题

| 现象 | 原因/解决 |
|---|---|
| `'java' 不是内部或外部命令` | 未安装 JDK 17 或未配置 PATH；安装 [Temurin](https://adoptium.net/) |
| `'mvnw.cmd' 不是内部或外部命令` | 不在项目根目录；用 `.\mvnw.cmd ...` 或在根目录执行 |
| 首次构建很慢/卡在下载 | Maven/Node 首次下载依赖；可配置国内镜像，耐心等待一次即可 |
| 端口 8888 被占用 | 找到占用进程结束，或 `--server.port=9000` 换端口 |
| 启动成功但页面空白/旧界面 | 强刷 `Ctrl+Shift+R`；确认访问的是 `/#/workspace`；看 F12 Console |
| 重启后“连接丢了” | 启动目录变了，`data/` 不在当前目录；回到原目录启动或迁移 `data/` |
| 启动报 H2 锁错误 | 有残留 Java 进程占用 `data/`；结束进程后删除 `data/minicat_db.lock.db` |
| 内存不足 / 频繁 Full GC | 加 JVM 参数：`java -Xmx1024m -jar ...`（低配可用 `-Xms256m -Xmx512m`） |
| Windows 控制台中文乱码 | `chcp 65001` 切 UTF-8，或使用 Windows Terminal |
| 防火墙/安全软件拦截 | 放行 Java 进程的本地端口（默认仅回环，一般无需入站放行） |
| PostgreSQL 连上了但不能浏览 | 当前 PG 仅支持连接测试，浏览能力在路线图中（见 [ROADMAP.md](ROADMAP.md)） |
| Docker 构建失败 | 检查网络/代理、`docker compose build --no-cache` 重试；首次构建会下载 Node/Maven 依赖 |

---

## 9. 下一步

- [USER_GUIDE.md](USER_GUIDE.md) — 界面与功能使用
- [CONFIGURATION.md](CONFIGURATION.md) — 配置与环境变量
- [DEPLOYMENT.md](DEPLOYMENT.md) — 生产部署
- [SECURITY.md](../SECURITY.md) — 使用前必读的安全边界
