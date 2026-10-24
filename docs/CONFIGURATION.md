# MiniCat 配置说明

> 覆盖：应用配置项、环境变量、Profile、数据目录与持久化、Docker 配置。
> 版本：v1.2.0

---

## 1. 配置文件位置

| 文件 | 作用 |
|---|---|
| `src/main/resources/application.properties` | 默认配置（开发/本机使用） |
| `src/main/resources/application-prod.properties` | `prod` profile（Gzip、文件日志、关闭 DevTools/H2 控制台） |

Spring Boot 支持用命令行参数/环境变量覆盖任意配置，例如：

```bash
java -jar minicat-server.jar --server.port=9000
java -jar minicat-server.jar --minicat.ui.inline-edit-enabled=false
java -jar minicat-server.jar --spring.profiles.active=prod
```

---

## 2. 默认配置项（application.properties）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | `8888` | HTTP 端口 |
| `server.address` | `127.0.0.1` | 监听地址；**默认只允许本机访问**，容器内用 `SERVER_ADDRESS=0.0.0.0` 覆盖 |
| `spring.application.name` | `MiniCat` | 应用名 |
| `spring.servlet.multipart.max-file-size` | `50MB` | 单个导入文件上限 |
| `spring.servlet.multipart.max-request-size` | `50MB` | 请求体上限 |
| `minicat.security.key-file` | `./data/secret.key` | 连接密码加密密钥文件路径 |
| `minicat.ui.inline-edit-enabled` | `true` | 表格单元格内联编辑开关；`false` 时前端只读 |
| `spring.datasource.hikari.maximum-pool-size` | `5` | 每个目标库连接池上限 |
| `spring.datasource.hikari.minimum-idle` | `1` | 最小空闲连接 |
| `spring.datasource.hikari.connection-timeout` | `30000` | 获取连接超时（ms） |
| `spring.datasource.hikari.idle-timeout` | `600000` | 空闲回收（ms） |
| `spring.datasource.hikari.max-lifetime` | `1800000` | 连接最大存活（ms） |
| `spring.datasource.hikari.leak-detection-threshold` | `60000` | 连接泄漏检测（ms） |
| `logging.level.root` | `INFO` | 根日志级别 |
| `logging.level.com.minicat` | `DEBUG` | 业务包日志级别 |
| `spring.devtools.restart.enabled` | `true`（默认 profile） | DevTools 热重启 |

> H2 元数据库连接串与 JPA 配置在 `H2ConfigConfiguration` / 应用默认中维护，
> 数据文件位于 `./data/minicat_db.mv.db`。

---

## 3. 生产 Profile（application-prod.properties）

| 配置项 | 值 | 说明 |
|---|---|---|
| `server.compression.enabled` | `true` | Gzip 压缩 HTML/JSON/CSS/JS |
| `logging.level.root` | `WARN` | 降低日志量 |
| `logging.file.name` | `logs/minicat.log` | 输出到文件 |
| `spring.devtools.restart.enabled` | `false` | 关闭热重启 |
| `spring.h2.console.enabled` | `false` | 关闭 H2 控制台 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 元数据表自动迁移 |
| `spring.jpa.show-sql` | `false` | 不打印 SQL |

启动方式：

```bash
java -jar target/minicat-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 4. 环境变量

| 变量 | 作用 | 备注 |
|---|---|---|
| `MINICAT_SECRET_KEY` | 提供密码加密密钥 | 优先级高于 `./data/secret.key`；容器部署推荐用 Secret 注入 |
| `SERVER_ADDRESS` | 覆盖 `server.address` | Docker 内需设为 `0.0.0.0` 才能被端口映射访问 |
| `JAVA_TOOL_OPTIONS` | JVM 参数 | 如 `-Xms512m -Xmx1024m`（`JAVA_OPTS` 不会被自动读取） |
| `SPRING_PROFILES_ACTIVE` | 激活 profile | compose 中设为 `prod` |

---

## 5. 数据目录与持久化

运行时生成的 `./data/`（相对**启动时的工作目录**）：

| 文件 | 内容 |
|---|---|
| `data/minicat_db.mv.db` | H2 元数据库：保存全部连接配置 |
| `data/minicat_db.trace.db` 等 | H2 辅助文件（如产生） |
| `data/secret.key` | 密码加密密钥（首次启动生成，权限应仅本人可读） |
| `data/minicat_db.lock.db` | H2 运行锁文件，异常退出可能残留；正常停止后一般自动清理 |

持久化规则：

- 在**同一目录**、同一套 `data/` 下重启 → 连接配置保留；
- 换目录启动 → 会创建新的空 `data/`（看起来像“连接丢了”），不是 Bug；
- 删除 `data/` → 等于清空所有已保存连接（无法恢复）；
- 丢失/更换 `secret.key` 且未设置 `MINICAT_SECRET_KEY` → 历史密码无法解密，需要重新填写密码；
- **备份 `data/` = 备份全部连接配置和密钥**；`data/` 已加入 `.gitignore`，禁止提交；
- 若进程异常退出后启动报 H2 锁相关错误，确认没有残留 Java 进程占用后删除 `data/minicat_db.lock.db` 再启动。

---

## 6. Docker 配置

`docker-compose.yml` 关键点：

```yaml
services:
  minicat:
    build: .
    ports:
      - "8888:8888"
    volumes:
      - ./data:/app/data          # 持久化 H2 元数据 + 密钥
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SERVER_ADDRESS=0.0.0.0    # 容器内监听所有网卡，对外仍由 ports 控制
      - JAVA_TOOL_OPTIONS=-Xms512m -Xmx1024m
    restart: unless-stopped
```

- 健康检查探测 `http://localhost:8888/`（项目未引入 actuator）；
- Dockerfile 多阶段构建：Node 20 构建前端 → Maven 3.9/JDK 17 打包 → JRE 17 alpine 运行，非 root 用户；
- **容器默认无认证**，对外暴露必须叠加反向代理 + HTTPS + Basic Auth/VPN，详见 [SECURITY.md](../SECURITY.md)。

---

## 7. 前端开发配置

`MiniCat-Frontend/vite.config.js`：

- 开发服务器默认 `5173`，`/api` 代理到 `http://localhost:8888`；
- 路径别名 `@` → `MiniCat-Frontend/src`；
- Hash 路由：生产访问 `http://localhost:8888/#/workspace`。

---

## 8. 常用组合示例

```bash
# 本机默认启动（数据目录 = 当前目录/data）
java -jar target/minicat-server-0.0.1-SNAPSHOT.jar

# 指定端口 + 关闭内联编辑
java -jar target/minicat-server-0.0.1-SNAPSHOT.jar \
  --server.port=9000 --minicat.ui.inline-edit-enabled=false

# 生产 profile + 环境变量密钥
MINICAT_SECRET_KEY='<32+ 位随机串>' \
  java -jar target/minicat-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 前端开发模式
cd MiniCat-Frontend && npm install && npm run dev
```

---

## 9. 相关文档

- [GETTING_STARTED.md](GETTING_STARTED.md) — 安装与首次使用
- [DEPLOYMENT.md](DEPLOYMENT.md) — 部署方式
- [ARCHITECTURE.md](ARCHITECTURE.md) — 架构与连接池设计
- [SECURITY.md](../SECURITY.md) — 安全模型
