# MiniCat 快速参考

## 🚀 一键启动

```bash
# Linux/Mac
./start.sh

# Windows
start.bat

# Docker
docker-compose up -d
```

访问: http://localhost:8080

---

## 📦 常用命令

### Maven 构建

```bash
# 清理并构建
mvn clean package -DskipTests

# 运行测试
mvn test

# 启动开发模式
mvn spring-boot:run
```

### Docker 操作

```bash
# 启动
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止
docker-compose down

# 重启
docker-compose restart

# 重新构建
docker-compose build --no-cache
```

### 前端开发

```bash
cd MiniCat-Frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

---

## 🔧 配置文件

| 文件 | 说明 |
|------|------|
| `application.properties` | 主配置文件 |
| `application-dev.properties` | 开发环境配置 |
| `application-prod.properties` | 生产环境配置 |

---

## 📁 重要目录

```
MiniCat/
├── src/main/java/          # 后端源码
├── MiniCat-Frontend/       # 前端源码
├── data/                   # H2数据库文件
├── target/                 # 构建输出
├── Dockerfile              # Docker配置
└── docker-compose.yml      # Docker编排
```

---

## 🌐 默认端口

- 后端API: 8080
- 前端开发: 5173
- MySQL: 3306
- PostgreSQL: 5432

---

## 📝 环境变量

```bash
# 修改端口
SERVER_PORT=9090

# 激活配置文件
SPRING_PROFILES_ACTIVE=prod

# JVM参数
JAVA_OPTS=-Xms512m -Xmx1024m
```

---

## 🔍 故障排查

### 检查Java版本
```bash
java -version
```

### 检查端口占用
```bash
# Linux/Mac
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

### 查看日志
```bash
# Docker
docker-compose logs -f

# JAR
tail -f logs/minicat.log

# systemd
journalctl -u minicat -f
```

---

## 📖 文档链接

- [完整文档](README.md)
- [部署指南](DEPLOYMENT.md)
- [使用手册](USAGE.md)
- [贡献指南](CONTRIBUTING.md)
- [更新日志](CHANGELOG.md)

---

## 💡 提示

1. **首次启动**: 需要下载依赖,可能需要几分钟
2. **端口冲突**: 修改 `application.properties` 中的端口
3. **数据持久化**: 备份 `data/` 目录
4. **生产环境**: 使用 `application-prod.properties`

---

需要帮助? 查看 [README.md](README.md) 或提交 Issue
