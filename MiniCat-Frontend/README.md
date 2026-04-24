# MiniCat 前端项目

这是 MiniCat 数据库管理工具的前端项目，基于 Vue 3 + Vite + Element Plus 构建。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 组件库
- **Vue Router** - 官方路由管理器
- **Pinia** - Vue 状态管理库
- **Axios** - HTTP 客户端
- **SQL Formatter** - SQL 格式化工具

## 功能模块

### 1. 连接管理 (/connections)
- 创建、编辑、删除数据库连接
- 测试数据库连接
- 支持 MySQL 和 PostgreSQL
- 选择活跃连接

### 2. SQL 查询 (/query)
- SQL 查询编辑器
- SQL 格式化功能
- 执行查询并查看结果
- 表结构树形浏览
- 点击表名快速生成查询语句

### 3. 数据表浏览 (/tables)
- 浏览所有数据表
- 查看表结构（列名、类型、约束等）
- 查看表数据（支持分页）
- 查看表信息（注释、行数、创建时间等）

## 安装依赖

```bash
npm install
```

## 开发环境运行

```bash
npm run dev
```

访问 http://localhost:5173/ 或 http://localhost:5174/

## 生产环境构建

```bash
npm run build
```

## 预览构建结果

```bash
npm run preview
```

## 项目结构

```
MiniCat-Frontend/
├── src/
│   ├── api/              # API 接口
│   │   └── database.js
│   ├── layout/           # 布局组件
│   │   └── index.vue
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── stores/           # Pinia 状态管理
│   │   └── connection.js
│   ├── utils/            # 工具函数
│   │   └── request.js
│   ├── views/            # 页面组件
│   │   ├── Connections.vue
│   │   ├── Query.vue
│   │   └── Tables.vue
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── package.json
├── vite.config.js        # Vite 配置
└── README.md
```

## 后端 API 代理配置

在 `vite.config.js` 中配置了 API 代理：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true
    }
  }
}
```

确保后端服务运行在 http://localhost:8088

## 注意事项

1. 使用前需要先启动后端服务
2. 在"连接管理"页面添加数据库连接
3. 选择连接后才能在"SQL查询"和"数据表浏览"页面使用
4. SQL 查询仅支持 SELECT 查询语句
5. 数据表浏览支持分页查看数据

## 浏览器支持

推荐使用现代浏览器：
- Chrome (最新版)
- Firefox (最新版)
- Edge (最新版)
- Safari (最新版)
