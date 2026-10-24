# 更新日志

所有重要变更记录在此文件。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

- 主仓库：<https://github.com/zhuyuebin/mini-cat>
- 国内镜像：<https://gitee.com/zhuyuebin/mini-cat>

---

## [Unreleased] — 目标 v1.2.0

### 新增

**P0 前端信息架构重构（2026-10）**

- 资源树（连接 → 数据库 → 表，懒加载）+ 多 Tab 工作区（表 / SQL 查询，上限 12、去重、草稿关闭确认）
- Hash 路由：`/#/workspace`、`/#/settings`、`/#/welcome`；旧路由 `/app`、`/connections`、`/query`、`/tables` 重定向
- 设置页（连接管理从一级导航迁入）、欢迎/帮助页与四步引导
- 工作区状态持久化：Tab、激活 Tab、展开节点、上次选择、每页条数、SQL 草稿；失效连接的 Tab 启动时自动清理
- 表工作区：固定底部服务端分页、全局行号、刷新、新增行、按主键删除行；
  导入成功后自动切到数据页并跳最后一页；删除当前页最后一行时自动回退上一页
- 结构页展示 PK/UK（唯一非空索引=行标识）、可空、默认值、自增、注释；信息页展示连接/库/表/列数/行数
- 连接类型能力矩阵（`utils/capabilities.js`）：MySQL 完整可用；PostgreSQL 在 P0 阶段显示“浏览能力开发中”占位
- 快捷键：`Alt+T` 新建查询、`Alt+W` 关闭 Tab（`Ctrl+T/W` 为浏览器保留键，无法拦截）

**P1-1 表格单元格内联编辑（2026-10）**

- 双击单元格编辑：文本/数字用输入框、日期/日期时间用日期选择器、布尔用下拉；
  Enter/失焦保存、Esc 取消、未修改静默退出
- 后端 `PUT /connections/{id}/table-row`：表/列元数据校验 + 标识符白名单 + `PreparedStatement` 参数绑定；
  `keys` 定位行、`changes` 更新列、`expected` 可选旧值 CAS
- 行标识三层回退：主键 → 无主键时 `UNIQUE NOT NULL` 唯一索引（`getColumns` 新增 `rowIdentityPart`，`columnKey` 标记 `UNI`）→ 都没有则整表只读提示
- 冲突语义：更新 0 行且行存在 → `409/40902`；行不存在 → `404/40402`；
  旧值 NULL 的 CAS 使用 `IS NULL`（避免 `col = NULL` 恒假）
- 行标识列、自增列、BLOB/BINARY/BIT/JSON/空间类型禁止修改；必填列清空被拦截
- `GET /settings` 下发 `minicat.ui.inline-edit-enabled` 开关（默认 `true`，可降级只读）
- 测试：`RowUpdateSqlBuilderTest`、`SqlValueNormalizerTest` + MockMvc `table-row`/`settings` 契约（后端用例 36 → 51）

**M1 工程质量（2026-09 / 10）**

- 语义化业务错误码（`ErrorCode`：4xxxx 客户端 / 5xxxx 服务端 + HTTP 状态映射）与全局异常处理
- 表数据服务端分页接口：`GET /connections/{id}/table-data`（返回 rows + total，page/size 服务端 clamp）
- H2 内存库完全隔离的测试护栏、纯单测 / 集成测试 / MockMvc 契约测试
- CI 拆分为 `backend-test`（真跑 `mvn test`）/ `frontend-build` / `package` 三个 job
- 密码加密密钥外置：`MINICAT_SECRET_KEY` 或 `./data/secret.key`（首次启动生成）；密文增加 `v2:` 版本前缀
- 连接池生命周期：按 `connectionId` 统一回收，修改/删除连接后重建；单池上限 5、最小空闲 1

**M0 安全与工程加固（2026-09）**

- 安全模型与威胁边界说明（[SECURITY.md](SECURITY.md)）、`.dockerignore`
- 导入上传限制：`spring.servlet.multipart.max-file-size=50MB`
- 默认只监听 `127.0.0.1`（Docker 内由 `SERVER_ADDRESS=0.0.0.0` 覆盖）
- 移除 Controller 上的 `@CrossOrigin(origins = "*")`，改为同源访问
- 新增 `docs/design/UI_REFACTOR.md` 设计契约，作为前端重构依据

### 修复

- 表数据表头显示 `[object Object]`、单元格全空（P0-2 将列结构对象与数据列名复用同一字段；
  拆分为 `columnDefs` 与 `columns` 并加防御归一化）
- CAS 旧值为 NULL 时误判冲突（SQL `col = NULL` 恒不成立，改为 `IS NULL` 且不占参数位）
- 表结构页“可空 / 默认值”字段名与后端 DTO 不匹配导致显示错误
- 自增列识别失效（JDBC 的 `IS_AUTOINCREMENT` 归一化为 `autoIncrement` 布尔字段）
- 表数据分页总数错误（此前把当前页行数当总数，永远只有一页）
- 后端业务异常被全局异常处理器吞掉，前端只能看到 “An unexpected error occurred”
- `SecurityTests` 中 `--` 注释断言与实现相反导致 `mvn test` 失败；补充 `isEncrypted` 误判回归用例
- Docker 健康检查探测不存在的 `/actuator/health`；后端镜像构建阶段重复构建前端
- `JAVA_OPTS` 环境变量未被消费（改为 `JAVA_TOOL_OPTIONS`）
- 默认监听所有网卡（改为 `127.0.0.1`）
- `restart.sh` 硬编码的部署路径与 JAR 文件名
- 连接池生命周期：修改连接配置/删除连接不回收旧池、改密码后仍使用旧凭据
- 单池连接上限 20 → 5、最小空闲 → 1，避免多连接多库场景目标库连接数爆炸
- 表数据分页从前端 `COUNT(*)` 补丁改为服务端正式分页接口
- 分页条被表格撑出可视区、按钮随选中态跳动、表列表行数不刷新等 UI 问题（P0 重构修复）

### 变更

- 密码加密密钥不再硬编码在源码中；日志统一 slf4j（移除 `System.out/err` 与 `printStackTrace`，密码/密钥永不记录）
- Service 层异常语义化：连接不存在 404、参数校验 400、SQL 拦截 403、执行失败 500
- 前端 Axios 拦截器适配语义化 HTTP 状态码，并保留对旧「HTTP 200 + success=false」的兼容
- 前端路由改为 Hash 模式；连接管理入口从一级导航迁入「设置」；默认打开工作区而非营销首页
- 删除旧 `Home.vue` / `Connections.vue` / `Query.vue` / `Tables.vue` / `layout/index.vue` / `stores/connection.js`
- 表数据页列模型拆分：`columnDefs`（列结构）与 `columns`（数据列名）分离
- 文档全量重构：`MD-Files/` 与根目录 Windows 历史文档合并为 `docs/` 文档集，`CHANGELOG.md` / `CONTRIBUTING.md` 移至根目录

### 安全

- 连接密码 AES-256-GCM 加密落库（`v2:` 前缀），密钥支持环境变量或本地密钥文件
- 默认仅回环监听、同源访问、导入大小限制、生产 profile 关闭 H2 控制台与 DevTools
- 单元格编辑全参数化 + 标识符白名单；`/update` 强制 UPDATE/DELETE 带 WHERE、DROP TABLE 禁多语句

---

## [1.0.0] - 2026-04-25

### 新增

- 首个正式版本
- MySQL / PostgreSQL 连接管理（PostgreSQL 为实验性）
- 基于 Vue 3 + Element Plus 的 Web 界面
- SQL 查询与格式化、表浏览、CSV/Excel 数据导入
- HikariCP 连接池、密码加密存储、SQL 危险操作校验
- Docker 与 docker-compose 部署、一键启动脚本

### 技术栈

- 后端：Spring Boot 3.5、Java 17
- 前端：Vue 3.5、Vite、Element Plus
- 元数据：H2（元数据存储）

---

## 版本说明

- `MAJOR`：不兼容的 API 变更；`MINOR`：向后兼容的功能新增；`PATCH`：向后兼容的问题修复；
- 分类：`新增` / `修复` / `变更` / `废弃` / `移除` / `安全`；
- 发布流程（版本号、Tag、GitHub/Gitee 双远程同步）见 [docs/RELEASING.md](docs/RELEASING.md)。

[Unreleased]: https://github.com/zhuyuebin/mini-cat/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/zhuyuebin/mini-cat/releases/tag/v1.0.0
