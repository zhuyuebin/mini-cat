# MiniCat - 轻量级Web数据库管理工具

<div align="center">

**一个简单、轻量的Web版数据库管理工具，类似Navicat的在线版本**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-42b883.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 🚀 快速开始

### 方式一：一键启动（推荐）

#### Linux/Mac
```bash
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd MiniCat
chmod +x start.sh
./start.sh
```

#### Windows
```bash
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd MiniCat
start.bat
```

访问: **http://localhost:8888**

### 方式二：Docker部署

```bash
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd MiniCat
docker-compose up -d
```

访问: **http://localhost:8888**

---

## ✨ 主要特性

- 🎯 **多数据库支持**: MySQL, PostgreSQL, H2
- 🖥️ **现代化界面**: Vue3 + Element Plus
- 🔒 **安全可靠**: SQL注入防护、密码加密
- ⚡ **高性能**: HikariCP连接池
- 📊 **数据可视化**: 直观的表格展示
- 🔍 **智能查询**: SQL编辑器和格式化
- 🚀 **快速部署**: Docker/JAR包多种方式

---

## 📸 项目截图

![首页](src/main/resources/static/2026-04-24_19-15.png)

---

## 🛠️ 技术栈

**后端**: Spring Boot 3.5 | Java 17 | Spring Security | HikariCP  
**前端**: Vue 3.5 | Vite | Element Plus | Pinia  
**数据库**: H2 | MySQL | PostgreSQL

---

## 📖 文档

- [README.md](README.md) - 完整文档
- [DEPLOYMENT.md](DEPLOYMENT.md) - 部署指南
- [USAGE.md](USAGE.md) - 使用手册
- [CONTRIBUTING.md](CONTRIBUTING.md) - 贡献指南
- [CHANGELOG.md](CHANGELOG.md) - 更新日志

---

## 🤝 参与贡献

欢迎提交 Issue 和 Pull Request！详见 [CONTRIBUTING.md](CONTRIBUTING.md)

---

## 📄 开源协议

本项目采用 MIT 协议 - 查看 [LICENSE](LICENSE) 文件

---

<div align="center">

**Made with ❤️ by MiniCat Team**

</div>
