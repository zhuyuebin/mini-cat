# 🐱 MiniCat — 轻量级 Web 数据库管理工具

<div align="center">

<img src="MiniCat-Frontend/public/logo.png" alt="MiniCat" width="120" />

**一个现代、轻量的 Web 数据库管理工具 —— 像用 Navicat 一样在浏览器里管理数据库。**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883.svg)](https://vuejs.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

GitHub（主仓库）：<https://github.com/zhuyuebin/mini-cat> ·
Gitee（国内备份）：<https://gitee.com/zhuyuebin/mini-cat>

</div>

---

## 📖 项目简介

MiniCat 是**单文件即可运行的 Web 数据库管理工具**：一个约 80MB 的 JAR 内置前端，启动后浏览器打开
`http://localhost:8888/#/workspace` 就能连接、浏览、编辑、查询你的 MySQL 数据库（PostgreSQL 实验性支持）。

- 面向**个人开发者/本机使用**：无需安装桌面客户端、无需额外数据库；
- 连接配置保存在本地 H2 文件库（`./data/`），密码 AES-256-GCM 加密；
- 前后端分离开发，但打包成单个可执行 JAR 发布。

> [!WARNING]
> **本项目定位为「本机单用户工具」**：默认只监听 `127.0.0.1`，且没有登录认证。
> 请勿直接部署到公网或不受信任的网络；确需远程访问请叠加反向代理 + HTTPS + Basic Auth / VPN。
> 完整安全模型见 [SECURITY.md](SECURITY.md)。

---

## ✨ 功能特性

### 🗄️ 数据库支持

- **MySQL 5.7+ / 8.0+**：完整支持连接、库表浏览、数据分页、单元格编辑、SQL 查询、建表/删表、CSV/Excel 导入；
- **PostgreSQL 12+**：实验性，当前仅支持连接测试（浏览/编辑能力见 [路线图](docs/ROADMAP.md)）；
- 多连接并行管理，连接类型能力矩阵区分可浏览/可查询能力。

### 🖥️ 资源树 + 多 Tab 工作区

- 左侧**资源树**：连接 → 数据库 → 数据表，懒加载、可刷新，单击表打开工作区、双击直接看数据；
- 右侧**多 Tab**：同时打开多张表 / 多个 SQL 查询，切换不丢上下文、页码与 SQL 草稿；
- 底部状态栏显示当前连接、类型、连通状态与路径；
- 工作区状态自动持久化（Tab / 激活 Tab / 展开节点 / 上次选择 / 每页条数 / SQL 草稿）；
- 快捷键：`Alt+T` 新建查询、`Alt+W` 关闭 Tab、`Ctrl+Enter` 执行 SQL。

### 📊 表工作区

- **数据**：服务端分页（每页 20/50/100/200，固定底部页码）、全局行号、刷新、新增行、行内删除；
- **双击单元格内联编辑**：文本/数字/日期/布尔按类型渲染编辑器，Enter/失焦保存、Esc 取消；
- 行定位优先主键，无主键时回退 `UNIQUE NOT NULL` 唯一索引，都没有则只读提示；
- 保存走服务端**参数化 UPDATE + 旧值 CAS**，别人/别的 Tab 改过会提示 409 并自动刷新，不静默覆盖；
- **结构**：列名/类型/可空/PK/UK/默认值/自增/注释；**信息**：连接/库/表/列数/行数；
- 低频/危险操作收进「更多 ▾」：新建表、导入数据、自定义 SQL、删除表。

### 🛠️ SQL 工作台

- 等宽字体编辑器、`sql-formatter` 一键格式化；
- `SELECT` 走查询接口，`INSERT/UPDATE/DELETE/DROP TABLE` 走更新接口；
- 危险语句二次确认；查询结果表格 + 行数/耗时；SQL 草稿随 Tab 保留。

### ⚙️ 设置与连接管理

- 连接管理从一级导航迁入「设置」：新增/编辑/测试/删除连接，支持 MySQL / PostgreSQL；
- 欢迎/帮助页提供四步上手引导与最近连接入口；
- 一键清空本地工作区状态（不影响数据库连接）。

### 🔒 安全与性能

- 连接密码 **AES-256-GCM 加密存储**，密钥支持环境变量或本地密钥文件；
- SQL 黑名单式危险操作提醒 + `WHERE` 强制校验（**注意：不是安全边界**）；
- 单元格编辑全参数化、标识符白名单、行标识列/自增列/BLOB/JSON 禁止修改；
- 每个「连接 + 数据库」独立 HikariCP 池（单池上限 5、最小空闲 1），修改/删除连接自动回收；
- 默认只监听 `127.0.0.1`，同源访问、无通配 CORS。

---

## 🖥️ 界面一览

```
┌───────────────────────────────────────────────────────────────────┐
│ 🐱 MiniCat   轻量级数据库管理工具                        设置 帮助 │
├──────────────────┬────────────────────────────────────────────────┤
│ 资源             │  user ×  +                                     │
│ ▼ MySQL test1    ├────────────────────────────────────────────────┤
│   information_.. │ test1 › usertest › user   MySQL  3列 · 100行     │
│   mysql          │ ┌ 数据 ┬ 结构 ┬ 信息 ┐                 刷新 更多▾│
│   usertest       │ │ 共100条 · 第1/5页              + 新增行     │ │
│     ▸ user       │ │ # id username beizhu              操作     │ │
│   world          │ │ 1 1  zhangsan1 无                 删除     │ │
│                  │ │ 2 2  zhangsan2 无                 删除     │ │
│                  │ │ …                                           │ │
├──────────────────┤ │ 共100条  20条/页  ◀ 1 2 3 4 5 ▶  前往 1 页 │ │
│ test1 MySQL 连接正常 usertest / user          数据保存在 data/ 下 │
└──────────────────┴────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 前置要求

- **JDK 17+**（必须；`java -version` 可验证）
- 运行：无需 Node/Maven（已打包进 JAR）
- 从源码构建：Maven 3.6+（或使用仓库自带 `mvnw`），首次构建会由插件下载 Node 20
- 浏览器：Chrome / Edge / Firefox / Safari 现代版本

### 方式一：一键脚本（推荐给本机使用）

```bash
# Linux / macOS
git clone https://github.com/zhuyuebin/mini-cat.git
cd mini-cat
chmod +x start.sh
./start.sh          # 构建 + 启动，访问 http://localhost:8888
```

```cmd
:: Windows（双击 start_win.bat 或命令行执行）
git clone https://github.com/zhuyuebin/mini-cat.git
cd mini-cat
start_win.bat
```

> 首次构建需要下载依赖和 Node，耗时较长；之后可直接 `java -jar target\minicat-server-*.jar` 秒启。

### 方式二：手动构建运行（JAR）

```bash
git clone https://github.com/zhuyuebin/mini-cat.git
cd mini-cat
./mvnw clean package -DskipTests
java -jar target/minicat-server-1.2.0.jar
```

**启动成功标志**：控制台出现 `Tomcat started on port 8888` 与 `Started MiniCatApplication`。
浏览器打开 **<http://localhost:8888/#/workspace>**（Hash 路由，`/tables` 等旧地址会自动跳转）。

### 方式三：Docker

```bash
git clone https://github.com/zhuyuebin/mini-cat.git
cd mini-cat
docker compose up -d
# 访问 http://localhost:8888
```

### 方式四：开发模式（前后端分离热更新）

```bash
# 终端 1：后端
./mvnw spring-boot:run

# 终端 2：前端
cd MiniCat-Frontend
npm install --registry=https://registry.npmmirror.com
npm run dev        # http://localhost:5173，/api 自动代理到 8888
```

### 首次使用（4 步）

1. 右上角 **设置** → 新建连接（MySQL）：填名称、主机、端口、用户名、密码 → 保存；
2. 回到 **工作区**，左侧资源树展开连接 → 数据库 → 数据表（单击打开 Tab，双击直达数据）；
3. 在「数据」页翻页浏览；**双击单元格**可直接修改，Enter 保存、Esc 取消；
4. 按 `Alt+T` 打开 SQL 查询 Tab，写 SQL 后 `Ctrl+Enter` 执行。

更细的说明见 [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md) 与 [docs/USER_GUIDE.md](docs/USER_GUIDE.md)。

---

## 💾 数据与持久化

- 连接配置、加密密钥都在**启动目录**下的 `data/`（`minicat_db.mv.db` + `secret.key`）；
- 同目录重启 → 配置保留；换目录启动 → 会创建新的空 `data/`（看起来像“连接丢了”）；
- **备份 `data/` = 备份全部连接配置与密钥**；删除 `data/` 等于清空连接；
- `data/` 已在 `.gitignore` 中忽略，请勿提交；`MINICAT_SECRET_KEY` 环境变量可替代密钥文件。

详见 [docs/CONFIGURATION.md](docs/CONFIGURATION.md)。

---

## ⚙️ 常用配置

```properties
server.port=8888                              # HTTP 端口
server.address=127.0.0.1                      # 默认只监听本机
minicat.security.key-file=./data/secret.key   # 密码加密密钥文件
minicat.ui.inline-edit-enabled=true           # 单元格内联编辑开关（false=只读）
spring.servlet.multipart.max-file-size=50MB   # CSV/Excel 导入大小上限
```

完整配置/环境变量见 [docs/CONFIGURATION.md](docs/CONFIGURATION.md)。

---

## 🔌 API 一览

基础路径 `http://localhost:8888/api/database`，统一响应 `{code, message, data, success}`，
共 **19 个接口**：连接管理（6）、元数据（3）、表数据分页与行编辑（2）、SQL 执行（2）、
表操作（3）、监控（2）、前端设置（1）。完整参数与示例见 **[docs/API.md](docs/API.md)**。

---

## 🧪 测试与 CI

```bash
# 后端测试（H2 内存库隔离，不污染 ./data）
./mvnw test

# 只跑后端测试、跳过前端构建
./mvnw -Dskip.installnodenpm -Dskip.npm test
```

- 当前 **51 个后端用例**：纯单测（SQL 校验、加密、连接池、行更新 SQL 构建、值归一化）、
  H2 集成、MockMvc 契约测试；
- GitHub Actions 三个 job：`backend-test`（真跑测试）、`frontend-build`、`package`（产出 JAR 工件）。

详见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

---

## 🏗️ 技术栈

| 层级 | 技术 |
|---|---|
| 后端 | Spring Boot 3.5.14、Java 17、Spring Data JPA、HikariCP |
| 元数据 | H2 文件数据库（连接配置） |
| 目标库 | MySQL（完整）、PostgreSQL（实验性） |
| 前端 | Vue 3.5、Vite 8、Element Plus 2.13、Pinia 3、Vue Router 5（Hash）、Axios、sql-formatter |
| 安全 | AES-256-GCM、参数化 SQL、标识符白名单、默认回环监听 |
| 构建/部署 | Maven Wrapper + frontend-maven-plugin、Docker 多阶段构建、GitHub Actions |

---

## 📁 项目结构

```
mini-cat/
├── src/main/java/...        # Spring Boot 后端（Controller/Service/Utils/异常体系）
├── src/test/java/...        # 51 个后端测试
├── MiniCat-Frontend/        # Vue 3 前端源码
├── docs/                    # 用户/开发/架构/API 文档
├── .github/workflows/       # GitHub Actions CI
├── Dockerfile / docker-compose.yml
├── pom.xml / mvnw / start.sh / start_win.bat / restart.sh
└── data/                    # 运行时生成（已忽略）
```

完整目录与模块说明见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

---

## 📚 文档导航

| 文档 | 内容 |
|---|---|
| [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md) | 安装、启动、首次使用、常见问题 |
| [docs/USER_GUIDE.md](docs/USER_GUIDE.md) | 界面与功能使用（多 Tab、表工作区、内联编辑、SQL、导入） |
| [docs/API.md](docs/API.md) | REST API 全量参考 + 错误码 + curl 示例 |
| [docs/CONFIGURATION.md](docs/CONFIGURATION.md) | 配置项、环境变量、数据目录与持久化 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 架构、模块、连接池、行更新与状态设计 |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | JAR / Docker / 反向代理部署 |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | 本地开发、测试分层、CI、调试 |
| [docs/RELEASING.md](docs/RELEASING.md) | 版本发布与 GitHub/Gitee 双远程同步 |
| [docs/ROADMAP.md](docs/ROADMAP.md) | 已完成里程碑、下一步规划、已知限制 |
| [docs/design/UI_REFACTOR.md](docs/design/UI_REFACTOR.md) | 前端信息架构设计记录（P0/P1） |
| [SECURITY.md](SECURITY.md) | 安全模型、威胁边界、部署加固清单 |
| [CHANGELOG.md](CHANGELOG.md) | 版本更新日志 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 贡献指南 |

---

## 🗺️ 路线图（摘要）

- ✅ M0 安全/工程化加固、M1 正确性修复与测试护栏；
- ✅ P0 前端信息架构重构（资源树 + 多 Tab + 设置/欢迎页 + Hash 路由 + 状态持久化）；
- ✅ P1-1 表格单元格内联编辑（参数化更新 + 乐观冲突 + 行标识回退）；
- ⏭️ P0-4 PostgreSQL 只读浏览 + 数据库级管理；
- ⏭️ M2 查询治理（结果集上限/超时/取消）、认证与审计。

详见 [docs/ROADMAP.md](docs/ROADMAP.md)。

---

## 🤝 参与贡献

欢迎 Issue / PR。开发流程、分支与提交规范、测试要求见 [CONTRIBUTING.md](CONTRIBUTING.md) 与
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

## 📄 开源协议

MIT License — 详见 [LICENSE](LICENSE)。

## 🙏 致谢

[Spring Boot](https://spring.io/projects/spring-boot) ·
[Vue.js](https://vuejs.org/) ·
[Element Plus](https://element-plus.org/) ·
[HikariCP](https://github.com/brettwooldridge/HikariCP) ·
[Apache POI](https://poi.apache.org/) ·
[OpenCSV](https://opencsv.sourceforge.net/)

<div align="center">
  <b>Made with ❤️ by MiniCat Team</b>
</div>
