# 更新日志

所有重要的项目更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/),
项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 新增
- 安全模型与威胁边界说明（[SECURITY.md](../SECURITY.md)）
- `.dockerignore`，构建上下文瘦身
- 导入上传大小限制：`spring.servlet.multipart.max-file-size=50MB`
- 连接密码加密密钥改为外部提供：`MINICAT_SECRET_KEY` 环境变量或 `./data/secret.key`（首次启动自动生成）
- 密文增加版本前缀 `v2:`，为后续密钥轮换做准备
- 语义化业务错误码（`ErrorCode`：4xxxx 客户端 / 5xxxx 服务端 + HTTP 状态映射）
- 表数据服务端分页接口：`GET /connections/{id}/table-data`（返回 rows + total，page/size 服务端 clamp）
- M1 测试护栏：H2 内存库完全隔离 + 纯单测 / 集成测试 / MockMvc 契约测试（36 个用例）
- CI 拆分为 `backend-test`（真跑 `mvn test`）/ `frontend-build` / `package` 三个 job

### 修复
- 表结构页"可空 / 默认值"字段名与后端 DTO 不匹配导致显示错误
- 自增列识别失效（JDBC 的 `IS_AUTOINCREMENT` 归一化为 `autoIncrement` 布尔字段）
- 表数据分页总数错误（此前把当前页行数当总数，永远只有一页）
- 后端业务异常被全局异常处理器吞掉，前端只能看到 "An unexpected error occurred"
- `SecurityTests` 中 `--` 注释断言与实现相反导致 `mvn test` 失败；补充 `isEncrypted` 误判回归用例
- Docker 健康检查探测不存在的 `/actuator/health`；后端镜像构建阶段重复构建前端
- `JAVA_OPTS` 环境变量未被消费（改为 `JAVA_TOOL_OPTIONS`）
- 默认监听所有网卡（改为 `127.0.0.1`，Docker 内由 `SERVER_ADDRESS=0.0.0.0` 覆盖）
- `restart.sh` 硬编码的部署路径与 JAR 文件名
- 连接池生命周期：修改连接配置/删除连接不回收旧池、改密码后仍使用旧凭据；
  改为按 `connectionId` 统一回收，且对"取连接/建池/关池"加锁避免并发重建旧池
- 单池连接上限 20 → 5、最小空闲 → 1，避免多连接多库场景目标库连接数爆炸
- 表数据分页从前端 `COUNT(*)` 补丁改为服务端正式分页接口

### 变更
- 移除 Controller 上的 `@CrossOrigin(origins = "*")`，改为同源访问
- 密码加密密钥不再硬编码在源码中
- 移除重复的失败提示（统一由 Axios 响应拦截器弹出）
- 清理由 Vite 模板遗留的组件/样式/图标，以及重复提交的 logo 图片
- 文档与代码对齐：PostgreSQL 标记为实验性、SQL 校验定位修正、API 接口数量修正
- 日志统一到 slf4j（移除 34 处 `System.out/err` 与 6 处 `printStackTrace`，密码/密钥永不记录）
- Service 层异常语义化：连接不存在 404、参数校验 400、SQL 拦截 403、执行失败 500
- 前端 Axios 拦截器适配语义化 HTTP 状态码，并保留对旧「HTTP 200 + success=false」的兼容

---

## [1.0.0] - 2026-04-25

### 新增
- ✨ 初始版本发布
- 🎯 支持 MySQL、PostgreSQL、H2 数据库连接管理
- 🖥️ 基于 Vue3 + Element Plus 的现代化Web界面
- 🔒 SQL注入防护和密码加密存储
- ⚡ HikariCP 连接池优化性能
- 📊 数据表格展示和SQL查询执行
- 🔍 SQL编辑器支持格式化
- 🐳 Docker 和 docker-compose 部署支持
- 📝 完善的文档和快速启动脚本

### 技术栈
- **后端**: Spring Boot 3.5, Java 17
- **前端**: Vue 3.5, Vite, Element Plus
- **数据库**: H2 (元数据存储)
- **安全**: Spring Security

---

## 版本说明

### 版本号格式: MAJOR.MINOR.PATCH

- **MAJOR**: 不兼容的 API 变更
- **MINOR**: 向后兼容的功能新增
- **PATCH**: 向后兼容的问题修正

### 前置符号含义

- `新增` - 新功能
- `修复` - Bug修复
- `变更` - 现有功能的变更
- `废弃` - 即将移除的功能
- `移除` - 已移除的功能
- `安全` - 安全相关的修复

---

## 发布流程

1. 更新此更新日志文件
2. 更新 `pom.xml` 中的版本号
3. 创建 Git Tag: `git tag -a v1.0.0 -m "Release version 1.0.0"`
4. 推送 Tag: `git push origin v1.0.0`
5. 在 Gitee 创建 Release
6. 构建并发布 Docker 镜像

---

[未发布]: https://gitee.com/your-username/MiniCat/compare/v1.0.0...HEAD
[1.0.0]: https://gitee.com/your-username/MiniCat/releases/tag/v1.0.0
