# 贡献指南

感谢关注 MiniCat！欢迎 Issue、PR、文档改进与使用反馈。

---

## 1. 行为准则

- 尊重、友善、就事论事；
- 不提交他人的私有信息、数据库凭据或内网地址；
- 安全漏洞请按 [SECURITY.md](SECURITY.md) 的方式私下报告，不要在公开 Issue 贴利用细节。

---

## 2. 你可以怎么贡献

| 方式 | 说明 |
|---|---|
| 报告 Bug | 用 [Issue 模板](.gitee/ISSUE_TEMPLATE.zh-CN.md)（GitHub 可直接开 Issue），附版本、复现步骤、日志/截图 |
| 提功能建议 | 说明使用场景与期望行为；可先看 [docs/ROADMAP.md](docs/ROADMAP.md) 是否已规划 |
| 提交代码 | 从 `main` 拉分支，按第 4、5 节规范提交 PR |
| 改进文档 | 文档在 `README.md` 与 `docs/`，修正错漏同样欢迎 |
| 测试反馈 | 在不同 MySQL/PostgreSQL 版本、Windows/macOS/Linux 上试用并反馈 |

---

## 3. 开发环境

```bash
git clone https://github.com/zhuyuebin/mini-cat.git
cd mini-cat

# 后端测试（H2 内存库隔离，不污染 ./data）
./mvnw -Dskip.installnodenpm -Dskip.npm test

# 前端
cd MiniCat-Frontend
npm install --registry=https://registry.npmmirror.com
npm run dev          # http://localhost:5173，/api 代理到 8888
```

Windows 使用 `mvnw.cmd`；完整说明见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

---

## 4. 分支与提交

分支命名：`feat/<scope>`、`fix/<scope>`、`docs/<scope>`、`refactor/<scope>`、`test/<scope>`。

提交信息使用 Conventional Commits：

```
feat(api): 新增 xxx 接口
fix(ui): 修复 xxx
docs: 重构 README
test(api): 覆盖 xxx 分支
```

- 一个 PR 聚焦一件事；
- 保持提交可独立构建、可回滚；
- 不要提交 `target/`、`data/`、`node_modules/`、任何密钥/密码/`.env`。

---

## 5. PR 要求

提交前自检：

- [ ] `./mvnw -Dskip.installnodenpm -Dskip.npm test` 全绿（当前 51 个用例）；
- [ ] 涉及前端时 `cd MiniCat-Frontend && npm run build` 通过；
- [ ] 新增/修改行为有对应测试（纯逻辑放 `src/test/.../Utils/`，接口契约放 `DatabaseControllerTest`）；
- [ ] 涉及接口/配置/行为变化时，同步更新 `docs/` 与 `CHANGELOG.md`；
- [ ] 提交信息清晰，PR 描述包含：背景、改动点、验证方式、影响范围；
- [ ] 不包含敏感信息与无关格式化改动。

PR 描述模板见 [.gitee/PULL_REQUEST_TEMPLATE.zh-CN.md](.gitee/PULL_REQUEST_TEMPLATE.zh-CN.md)。

---

## 6. 代码风格与约定

### 后端（Java）

- 4 空格缩进；命名清晰；注释写“为什么”而不是复述代码；
- 新接口：`DatabaseController` → `DatabaseService` 接口 → `DatabaseServiceImpl`；
- 异常统一抛 `DatabaseException` 子类，配合 `ErrorCode`；错误信息面向用户可读；
- 涉及用户输入的 SQL 必须参数化 + 标识符白名单（参考 `RowUpdateSqlBuilder`）；
- 日志用 slf4j，禁止记录密码/密钥；新增测试参考现有分层。

### 前端（Vue 3）

- `<script setup>` 组合式写法；组件按 `layout/`、`views/`、`components/` 划分；
- 跨页/跨 Tab 状态放 `stores/workspace.js`；
- 请求统一走 `api/database.js`，错误提示由 `utils/request.js` 拦截器负责；
- 表数据页注意 `tab.columnDefs`（列结构）与 `tab.columns`（数据列名）的区别；
- 路由使用 Hash 模式；新增页面在 `router/index.js` 注册。

详细架构约定见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)、[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

---

## 7. 文档规范

- 用户/开发文档放 `docs/`，入口是 [README.md](README.md) 的「文档导航」；
- 不新增 `MD-Files/` 之类历史目录；过时文档直接删除/合并（Git 历史可查）；
- 命令、端口、路径、接口数量必须与代码一致；
- 不写真实密码、密钥、私有地址；示例数据用 `demo` / `zhangsan1` 这类占位。

---

## 8. 需要帮助

- 开发/运行问题：先看 [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md) 与 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)；
- 安全问题：见 [SECURITY.md](SECURITY.md)；
- 其他：开 Issue 或在 PR 中说明。

## 9. 开源协议

贡献即表示同意以 [MIT License](LICENSE) 授权。
