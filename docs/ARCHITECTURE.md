# MiniCat 架构说明

> 面向想了解/二次开发 MiniCat 的读者。接口细节见 [API.md](API.md)，配置项见 [CONFIGURATION.md](CONFIGURATION.md)，
> UI 信息架构与设计决策见 [design/UI_REFACTOR.md](design/UI_REFACTOR.md)。

---

## 1. 总览

MiniCat 是**单体应用**：Spring Boot 后端 + Vue 3 前端，构建产物合并进同一个可执行 JAR，
默认监听 `127.0.0.1:8888`，浏览器打开即用。

```
┌──────────────────────────── Browser ────────────────────────────┐
│  Vue 3 + Element Plus SPA（Hash 路由 /#/workspace、/#/settings） │
│  ┌──────────┐  ┌───────────────────────────────┐  ┌──────────┐  │
│  │ 资源树    │  │ 多 Tab 工作区（表 / SQL 查询）  │  │ 设置/帮助 │  │
│  └──────────┘  └───────────────────────────────┘  └──────────┘  │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTP /api/database/**（同源，无 CORS）
┌───────────────────────────────▼─────────────────────────────────┐
│ Spring Boot 3.5（Java 17）                                       │
│  Controller ── Service ── ConnectionPoolManager ── 目标数据库     │
│       │            │                                             │
│       │            └── JPA ── H2 文件库（连接配置元数据）          │
│       └── GlobalExceptionHandler（ErrorCode → HTTP + 业务码）     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.5.14、Java 17 |
| Web / 安全 | Spring MVC、Spring Security（仅用于密码编码能力，当前全部放行 + 关闭 CSRF） |
| 元数据存储 | Spring Data JPA + H2 文件数据库（`./data/minicat_db`） |
| 目标库连接 | JDBC + HikariCP（每个「连接 + 数据库」一个池，单池上限 5） |
| 目标库支持 | MySQL 5.7+/8.0+（完整）；PostgreSQL 12+（实验性，仅测试连接） |
| 密码加密 | AES-256-GCM（`v2:` 前缀），密钥来自环境变量或 `./data/secret.key` |
| 文件导入 | Apache POI（Excel）、OpenCSV（CSV） |
| 前端 | Vue 3.5、Vite 8、Element Plus 2.13、Pinia 3、Vue Router 5（Hash 模式）、Axios、sql-formatter |
| 构建 | Maven Wrapper + frontend-maven-plugin（Node 20.19 / npm 10.2 内置安装） |
| CI | GitHub Actions（后端测试 / 前端构建 / 打包）；Gitee Go 参考配置 |

---

## 3. 代码结构

```
mini-cat/
├── src/main/java/com/minicat/minicatserver/
│   ├── MiniCatApplication.java              # 启动类
│   ├── config/
│   │   ├── H2ConfigConfiguration.java       # H2 元数据库配置
│   │   ├── HikariConfigProperties.java      # 连接池参数
│   │   └── SecurityConfig.java              # 关闭 CSRF、放行请求
│   ├── controller/DatabaseController.java   # 全部 REST 接口（19 个）
│   ├── Services/DatabaseService.java        # 服务接口
│   ├── ServiceImpls/DatabaseServiceImpl.java# 连接/元数据/分页/导入/行更新等核心逻辑
│   ├── dto/                                 # 请求/响应 DTO（含 API 封装、分页、行更新）
│   ├── entity/                              # DatabaseConnection DTO + JPA 实体
│   ├── exception/                           # ErrorCode、DatabaseException 家族、全局异常处理
│   ├── repository/                          # Spring Data JPA 仓库
│   └── Utils/
│       ├── ConnectionPoolManager.java       # 池生命周期（按 connectionId + 数据库）
│       ├── PasswordEncryptionService.java   # AES-256-GCM 加解密
│       ├── SqlInjectionValidator.java       # SQL 黑名单式风险校验（非安全边界）
│       ├── RowUpdateSqlBuilder.java         # 行更新 SQL 构建（标识符白名单 + 占位符）
│       └── SqlValueNormalizer.java          # JSON 值 → JDBC 绑定值（类型归一化）
├── src/main/resources/
│   ├── application.properties               # 默认配置
│   └── application-prod.properties          # 生产 profile
├── src/test/java/                           # 51 个用例：纯单测 + H2 集成 + MockMvc 契约
├── MiniCat-Frontend/
│   ├── src/
│   │   ├── api/database.js                  # Axios API 封装
│   │   ├── layout/                          # AppShell / TopBar / DataExplorer / WorkspaceTabs
│   │   ├── router/index.js                  # Hash 路由 + 旧路由重定向
│   │   ├── stores/workspace.js              # 资源树缓存、多 Tab、工作区持久化、UI 设置
│   │   ├── utils/                           # request.js（Axios 拦截器）、capabilities.js
│   │   ├── views/                           # Workspace / Welcome / Settings + workspace 子组件
│   │   └── components/                      # 连接/建表/导入/自定义 SQL 对话框
│   ├── index.html、vite.config.js、package.json
├── docs/                                    # 本文档集
├── .github/workflows/build.yml               # GitHub Actions（唯一 CI）
├── .gitee/                                   # Gitee Issue/PR 模板（代码镜像，不跑 CI）
├── Dockerfile、docker-compose.yml            # 容器化部署
├── pom.xml、mvnw、mvnw.cmd                   # 构建
├── start.sh、start_win.bat、restart.sh       # 启动/重启脚本
└── data/                                    # 运行时生成（H2 元数据 + 密钥），已 gitignore
```

---

## 4. 后端关键设计

### 4.1 请求链路

```
Browser → Axios → DatabaseController
          → DatabaseService（接口）
          → DatabaseServiceImpl
              ├─ 连接元数据：JPA + H2
              ├─ 目标库访问：ConnectionPoolManager（HikariCP）
              ├─ SQL 安全校验：SqlInjectionValidator（黑名单提醒）
              └─ 异常：抛 DatabaseException 子类
          → GlobalExceptionHandler：ErrorCode → HTTP 状态码 + {code,message,data,success}
```

### 4.2 连接池生命周期

- `PoolKey = (connectionId, databaseName)`：同一连接的不同库各自一个池；
- 首次访问某库时创建池，后续复用；单池 `maximumPoolSize=5`、`minimumIdle=1`；
- 修改连接配置 / 删除连接 → 关闭该 connectionId 下**所有**池，下次访问重建（避免用旧凭据/旧配置）；
- 「取连接 / 建池 / 关池」加锁，避免并发下重新建出旧池；
- 目标数据库凭据只在内存与加密后的 H2 元数据中出现，密码永不写日志。

### 4.3 密码加密流程

```
POST/PUT 连接（明文密码）
  → PasswordEncryptionService.encrypt()
  → "v2:Base64(IV + 密文)"  →  H2 元数据库
连接目标库时
  → 解密 → 组装 JDBC URL → HikariCP
```

密钥优先级：`MINICAT_SECRET_KEY` 环境变量 → `./data/secret.key`（首次启动自动生成）。
密钥丢失/更换会导致历史密码无法解密。

### 4.4 表数据分页与行定位

- `GET /table-data`：`COUNT(*)` 取总数 + `LIMIT/OFFSET` 取当前页（`size` clamp 10~200）；
- 行标识（用于按行删除 / 单元格编辑）：`getColumns` 计算 `rowIdentityPart`：
  1. 主键列；
  2. 无主键时取第一个「UNIQUE 且所有列 NOT NULL」索引（InnoDB 同样用它做聚簇索引）；
  3. 都没有 → 前端只读并提示。
- 行更新（`PUT /table-row`）：`RowUpdateSqlBuilder` 生成
  `UPDATE \`t\` SET \`c\`=? WHERE \`k\`=? [AND \`c\` = ? | AND \`c\` IS NULL]`，
  `SqlValueNormalizer` 按列类型把 JSON 值归一化为 `BigDecimal/Date/Timestamp/Boolean/String`；
  更新 0 行时区分 `40402`（行不存在）与 `40902`（乐观冲突）。

### 4.5 错误码体系

`ErrorCode` 枚举同时定义业务码、HTTP 状态与默认文案；所有业务异常继承 `DatabaseException`，
由 `GlobalExceptionHandler` 统一转换。前端 `utils/request.js` 拦截器读取 HTTP 状态 + `code`，
统一弹出后端 `message`，避免页面各自重复提示。

---

## 5. 前端关键设计

- **Hash 路由**：后端静态资源没有 SPA fallback，使用 `/#/workspace` 避免刷新 404；
- **信息架构**：左侧资源树（连接 → 库 → 表，懒加载）+ 顶部多 Tab 工作区 + 底部状态栏，
  连接管理收进「设置」，详见 [design/UI_REFACTOR.md](design/UI_REFACTOR.md)；
- **状态管理**：`stores/workspace.js` 统一管理连接/库/表缓存、Tab 列表、树展开状态、
  UI 设置与持久化；
- **工作区持久化**：Tab（只存外壳）、激活 Tab、展开节点、上次选择、每页条数、SQL 草稿写入
  `localStorage['minicat.workspace.v1']`，失效连接对应的 Tab 在启动时自动清理；
- **能力矩阵**：`utils/capabilities.js` 按数据库类型声明 `browse/query/ddl` 能力，
  PostgreSQL 在 P0 阶段显示“开发中”占位；
- **Axios 拦截器**：统一超时、错误提示、`code` 提取；`HTTP 4xx/5xx` 与业务失败信息不重复弹窗。

---

## 6. 构建与部署

- `mvnw clean package`：frontend-maven-plugin 安装 Node → `npm install` → `npm run build`；
  maven-resources-plugin 把 `MiniCat-Frontend/dist` 复制到 `target/classes/static`；
  spring-boot-maven-plugin 打出可执行 JAR（≈80MB）；
- 跳过前端构建：`-Dskip.installnodenpm -Dskip.npm`（CI 中先单独构建前端再用）；
- 生产运行：`java -jar target/minicat-server-*.jar --spring.profiles.active=prod`；
- Docker：多阶段构建（Node 构建前端 → Maven 打包 → JRE alpine 运行），
  compose 挂载 `./data:/app/data`、`SERVER_ADDRESS=0.0.0.0`。

更多见 [GETTING_STARTED.md](GETTING_STARTED.md) 与 [DEPLOYMENT.md](DEPLOYMENT.md)。

---

## 7. 设计决策记录

| 决策 | 原因 |
|---|---|
| 单体 JAR 内置前端 | 本机工具追求“一个文件打开即用” |
| 默认仅监听 127.0.0.1 | 无认证，防止误暴露到局域网/公网 |
| Hash 路由 | 后端无 SPA fallback，刷新深链不 404 |
| 连接管理迁入设置 + 资源树主导航 | 符合“数据库工具”用户心智，减少跨页跳转 |
| 多 Tab + localStorage 持久化 | 贴近 Navicat/DataGrip 使用习惯 |
| 行更新走服务端参数化接口 | 前端拼 SQL 无法正确处理转义/类型，且无法做 CAS |
| `expected` 可选 CAS | 本地工具成本极低，可防静默覆盖外部修改；不需要可关闭 |
| 行标识三层回退 | 现实中存在无主键表，优先保证“能安全定位才允许编辑” |

---

## 8. 扩展点

- **新增目标数据库类型**：`getConnectionCapabilities`（前端）+ JDBC URL/驱动（后端）；
  PG 浏览需补方言（标识符引用、`COUNT`/分页 SQL 相同但反引号需替换为双引号）；
- **新增 API**：Controller → Service 接口 → Impl，异常统一抛 `DatabaseException` 子类；
- **新增 UI 页面**：`views/` + 路由 `router/index.js`；需要跨 Tab 状态时放 `stores/workspace.js`；
- **新增测试**：纯逻辑放 `Utils/*Test`，Web 契约放 `DatabaseControllerTest`（MockMvc + H2 内存库）。

---

## 9. 相关文档

- [GETTING_STARTED.md](GETTING_STARTED.md) — 安装与首次使用
- [USER_GUIDE.md](USER_GUIDE.md) — 界面与功能使用
- [API.md](API.md) — REST API 全量参考
- [CONFIGURATION.md](CONFIGURATION.md) — 配置、环境变量、数据目录
- [DEPLOYMENT.md](DEPLOYMENT.md) — Docker/生产部署
- [DEVELOPMENT.md](DEVELOPMENT.md) — 开发与测试
- [SECURITY.md](../SECURITY.md) — 安全模型与威胁边界
- [design/UI_REFACTOR.md](design/UI_REFACTOR.md) — UI 设计与 P0/P1 记录
