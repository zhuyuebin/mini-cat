# 更新日志

所有重要的项目更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/),
项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 新增
- 待添加新功能

### 修复
- 待修复的Bug

### 变更
- 待更新的变更

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
