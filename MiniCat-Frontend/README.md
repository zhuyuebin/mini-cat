# MiniCat Frontend

MiniCat 的 Web 前端（Vue 3 + Vite + Element Plus），构建产物由 Maven 复制进后端 JAR 的 `static/` 目录，
最终以 `http://localhost:8888/#/workspace` 访问。

---

## 技术栈

| 能力 | 技术 |
|---|---|
| 框架 | Vue 3.5（`<script setup>`） |
| 构建 | Vite 8 |
| UI | Element Plus 2.13 + `@element-plus/icons-vue` |
| 状态 | Pinia 3（`stores/workspace.js`） |
| 路由 | Vue Router 5（**Hash 模式**） |
| 请求 | Axios（`utils/request.js` 统一拦截） |
| SQL | sql-formatter |

---

## 开发

```bash
npm install --registry=https://registry.npmmirror.com
npm run dev        # http://localhost:5173，/api 代理到 http://localhost:8888
npm run build      # 产物 dist/（已 gitignore）
npm run preview    # 本地预览构建产物
```

开发前请先启动后端（项目根目录 `./mvnw spring-boot:run` 或 `java -jar target/...`）。
后端地址在 `vite.config.js` 中配置：

```js
server: {
  proxy: {
    '/api': { target: 'http://localhost:8888', changeOrigin: true }
  }
}
```

---

## 目录结构

```
src/
├── api/database.js          # 全部后端接口封装
├── layout/
│   ├── AppShell.vue         # 应用外壳：顶栏 + 资源树 + 工作区 + 状态栏
│   ├── TopBar.vue           # 品牌 / 设置 / 帮助
│   ├── DataExplorer.vue     # 资源树（连接 → 数据库 → 表）
│   └── WorkspaceTabs.vue    # 多 Tab 栏
├── router/index.js          # Hash 路由 + 旧路由重定向
├── stores/workspace.js      # 连接/库/表缓存、多 Tab、持久化、UI 设置
├── utils/
│   ├── request.js           # Axios 实例 + 错误拦截
│   └── capabilities.js      # 数据库类型能力矩阵（MySQL/PG）
├── views/
│   ├── WorkspaceView.vue    # Tab 内容容器
│   ├── WelcomeView.vue      # 欢迎/帮助页
│   ├── SettingsView.vue     # 设置（连接管理）
│   └── workspace/
│       ├── TableWorkspace.vue    # 表工作区（上下文栏 + 更多菜单）
│       ├── TableDataTab.vue      # 数据（分页/行号/内联编辑）
│       ├── TableStructureTab.vue # 结构（PK/UK/自增等）
│       ├── TableInfoTab.vue      # 信息
│       ├── QueryWorkspace.vue    # SQL 工作区
│       └── QueryResultPanel.vue  # 查询结果
├── components/              # ConnectionDialog / CreateTableDialog / ImportDataDialog / CustomSqlDialog
├── App.vue、main.js
```

---

## 关键约定

- **Hash 路由**：后端没有 SPA fallback，`/#/workspace` 才能刷新不 404；
- **表数据列模型**：`tab.columnDefs` 是列结构对象（`dataType`/`rowIdentityPart` 等），
  `tab.columns` 是 `/table-data` 返回的字符串列名，**不要混用**；
- **状态持久化**：`stores/workspace.js` 把 Tab 外壳/展开节点/每页条数/SQL 草稿写入
  `localStorage['minicat.workspace.v1']`，恢复时不预加载行数据；
- **能力矩阵**：涉及新数据库类型时先改 `utils/capabilities.js`，不要在各组件里散落判断；
- **错误提示**：请求错误由 `utils/request.js` 统一 toast，页面里不要重复弹；
- **内联编辑**：新增可编辑类型需同时改前端 `TableDataTab.vue` 与后端 `SqlValueNormalizer`。

更多文档：

- [docs/USER_GUIDE.md](../docs/USER_GUIDE.md) — 界面与功能说明
- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) — 前端/后端架构
- [docs/design/UI_REFACTOR.md](../docs/design/UI_REFACTOR.md) — UI 信息架构设计记录
- [docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md) — 开发与测试
