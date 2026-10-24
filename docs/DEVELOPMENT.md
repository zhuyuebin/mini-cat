# 开发指南

> 面向二次开发/提交 PR 的读者。贡献流程与规范见 [CONTRIBUTING.md](../CONTRIBUTING.md)。

---

## 1. 环境要求

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | 后端编译与运行 |
| Maven | 3.6+ 或 `./mvnw` | 推荐用仓库自带 Wrapper |
| Node.js | 20.x | 仅前端开发/构建时需要；Maven 打包时插件会自动下载 |
| Git | 任意较新版本 | Windows 建议 Git Bash 或 Windows Terminal |

Windows 下：

```cmd
mvnw.cmd -v
java -version
node -v
```

---

## 2. 分支与提交模型

- `main`：稳定分支，可发布状态；
- 功能/修复分支从 `main` 拉出：
  - `feat/<scope>`（新功能，如 `feat/ui-refactor`）
  - `fix/<scope>`（缺陷修复）
  - `docs/<scope>`（文档）
  - `refactor/<scope>`、`test/<scope>`
- **一个里程碑 = 一次或多次小提交**，提交信息使用 Conventional Commits：

```
feat(api): 行标识元数据 + 参数化单行更新
fix(ui): 修复表头 [object Object]
docs: 重构 README 与 docs/
test(api): 覆盖 CAS 冲突分支
```

每个提交应保证：`./mvnw test` 通过、前端 `npm run build` 通过（涉及前端时）。

---

## 3. 本地运行

### 3.1 后端

```bash
./mvnw spring-boot:run
# 或构建后运行
./mvnw clean package -DskipTests -Dskip.installnodenpm -Dskip.npm
java -jar target/minicat-server-0.0.1-SNAPSHOT.jar
```

### 3.2 前端

```bash
cd MiniCat-Frontend
npm install --registry=https://registry.npmmirror.com
npm run dev        # http://localhost:5173，/api 代理到 8888
```

### 3.3 完整打包（含前端）

```bash
./mvnw clean package -DskipTests
```

frontend-maven-plugin 会安装 Node 20 → `npm install` → `npm run build`，
然后 maven-resources-plugin 把 `MiniCat-Frontend/dist` 复制进 `target/classes/static`。

---

## 4. 代码结构约定

### 4.1 后端

```
controller/ → Services/（接口） → ServiceImpls/（实现） → Utils / repository
```

- 新接口：`DatabaseController` 加映射方法 → `DatabaseService` 接口 → `DatabaseServiceImpl`；
- 业务异常：抛 `DatabaseException` 子类（`ConnectionNotFoundException`、`ValidationException`、
  `SqlInjectionException`、`RowNotFoundException`、`RowConflictException` 等），
  由 `GlobalExceptionHandler` 统一映射 HTTP 状态 + `ErrorCode`；
- 涉及用户输入的 SQL：必须走 `RowUpdateSqlBuilder` 式**标识符白名单 + PreparedStatement 参数绑定**，
  不要拼接值；
- 新增错误码：在 `ErrorCode` 枚举中定义业务码/HTTP 状态/默认文案，并补充 MockMvc 契约测试；
- 日志用 slf4j，**禁止**打印密码、密钥、完整连接串。

### 4.2 前端

```
api/ → views/ → components/ → stores/workspace.js；布局在 layout/
```

- 路由为 **Hash 模式**（`/#/workspace`），新增页面在 `router/index.js` 注册；
- 资源树/多 Tab 状态统一放 `stores/workspace.js`，不要散落在组件里；
- 表数据页列模型：`tab.columnDefs`（列结构对象，含 `rowIdentityPart/dataType`）
  与 `tab.columns`（`/table-data` 返回的字符串列名）**不要混用**；
- 数据库类型能力判断走 `utils/capabilities.js`；
- 请求统一走 `api/database.js`，错误提示由 `utils/request.js` 拦截器处理，页面不要重复弹错。

---

## 5. 测试

```bash
# 全部后端测试（H2 内存库隔离；不触碰 ./data）
./mvnw test

# 跳过前端构建，快速跑后端测试
./mvnw -Dskip.installnodenpm -Dskip.npm test

# 前端构建验证
cd MiniCat-Frontend && npm run build
```

分层（当前 **51 个用例**）：

| 层 | 内容 | 位置 |
|---|---|---|
| L1 纯单测 | SQL 校验、密码加解密、连接池、分页 clamp、行更新 SQL 构建、值归一化 | `src/test/java/.../Utils/` |
| L2 集成 | H2 内存元数据库：加密落库、改密码、删除、连接池回收、语义异常 | `DatabaseConnectionServiceTest` |
| L3 Web 契约 | MockMvc：状态码/业务码/密码屏蔽/分页参数/行更新校验/设置开关 | `DatabaseControllerTest` |

测试隔离：`src/test/resources/application-test.properties` 使用 H2 内存库 + `create-drop`，
密钥与数据都写在 `target/` 下，不污染 `./data/`。

新增测试建议：

- 纯逻辑抽到 `Utils/` 并写单测（参考 `RowUpdateSqlBuilderTest`、`SqlValueNormalizerTest`）；
- 新接口补 MockMvc 契约（成功 + 至少一个失败分支）；
- 依赖真实目标库的场景（如真实 UPDATE）留给本地手工验证，后续可引入 Testcontainers。

---

## 6. CI

`.github/workflows/build.yml` 三个 job（push/PR 到 `main` 触发）：

| job | 内容 |
|---|---|
| `backend-test` | `mvn -B -Dskip.installnodenpm -Dskip.npm test` + 上传 surefire 报告 |
| `frontend-build` | Node 20 → `npm ci` → `npm run build` + 上传 dist |
| `package` | 下载 frontend dist → `mvn package -DskipTests` + 上传 JAR |

`.gitee/workflows/build.yml` 为 Gitee Go 参考配置（未持续验证）。

---

## 7. 常见开发问题

| 问题 | 处理 |
|---|---|
| 本地运行后 `./data` 出现测试连接 | 正常：本地运行会生成真实 `data/`；测试不影响它（测试用内存库） |
| `mvn clean package` 报文件被占用/无法 rename JAR | 有 Java 进程正在运行旧 JAR，先停止再打包 |
| 前端改了但页面没变 | Maven 打包场景需重新 `npm run build`；浏览器强刷；确认看的是 `/#/workspace` |
| 页面空白/路由 404 | 使用 Hash 路由；后端静态资源无 SPA fallback |
| H2 锁文件 `minicat_db.lock.db` 残留 | 确认无残留进程后删除再启动 |
| 表格表头显示 `[object Object]` | `columnDefs`/`columns` 混用，参考 `TableDataTab.vue` |
| 打包很慢 | 首次会下载 Node/npm 依赖；`-Dskip.installnodenpm -Dskip.npm` 跳过前端构建 |
| Windows 端口占用 | `netstat -ano \| findstr :8888` + `taskkill /F /PID <PID>` |

---

## 8. 文档规范

- 用户文档放 `docs/`，入口是根 [README.md](../README.md) 的「文档导航」；
- 新增/修改功能时同步：README 特性表（如涉及）、`docs/` 对应文档、`CHANGELOG.md`；
- 文档中的命令/端口/路径必须可在当前代码上跑通；
- 禁止写入真实密码、密钥、私有内网地址；
- 设计类记录放 `docs/design/`（如 [design/UI_REFACTOR.md](design/UI_REFACTOR.md)）。

---

## 9. 相关文档

- [CONTRIBUTING.md](../CONTRIBUTING.md) — 贡献指南与 PR 要求
- [ARCHITECTURE.md](ARCHITECTURE.md) — 架构与模块
- [API.md](API.md) — 接口参考
- [CONFIGURATION.md](CONFIGURATION.md) — 配置
- [RELEASING.md](RELEASING.md) — 发布流程
