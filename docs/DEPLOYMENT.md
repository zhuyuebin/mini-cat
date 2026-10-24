# 部署指南

> 适用版本：v1.2.0。部署前请务必阅读 [SECURITY.md](../SECURITY.md)：
> **MiniCat 默认无认证且只监听本机，直接暴露到公网/内网是不安全的。**

---

## 1. 部署方式总览

| 方式 | 适用场景 | 章节 |
|---|---|---|
| JAR 前台运行 | 本机个人使用、临时试用 | [2](#2-jar-方式本机) |
| systemd 常驻（Linux） | 长期在本机/服务器上运行 | [3](#3-systemd-常驻-linux) |
| Docker Compose | 隔离环境、统一运维 | [4](#4-docker-部署) |
| 反向代理 + 认证 | 需要远程访问时（必须叠加认证） | [5](#5-反向代理与远程访问加固) |
| Kubernetes | 已有 K8s 基础设施 | [7](#7-kubernetes-示例) |

---

## 2. JAR 方式（本机）

### 2.1 构建

```bash
git clone https://github.com/zhuyuebin/mini-cat.git
cd mini-cat
./mvnw clean package -DskipTests        # Windows: mvnw.cmd clean package -DskipTests
```

产物：`target/minicat-server-0.0.1-SNAPSHOT.jar`（约 80MB，已内置前端）。

### 2.2 运行

```bash
# 前台运行（推荐本机使用）
java -jar target/minicat-server-0.0.1-SNAPSHOT.jar

# 生产 profile（Gzip、文件日志、关闭 DevTools/H2 控制台）
java -jar target/minicat-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 后台运行
nohup java -jar target/minicat-server-0.0.1-SNAPSHOT.jar > minicat.log 2>&1 &
```

JVM 参数建议：

```bash
java -Xms512m -Xmx1024m -XX:+UseG1GC -jar target/minicat-server-0.0.1-SNAPSHOT.jar
```

> 运行目录决定 `data/` 位置：请在项目根目录（或你希望保存 `data/` 的目录）启动。
> 详见 [CONFIGURATION.md](CONFIGURATION.md#5-数据目录与持久化)。

访问：**<http://localhost:8888/#/workspace>**

---

## 3. systemd 常驻（Linux）

创建 `/etc/systemd/system/minicat.service`：

```ini
[Unit]
Description=MiniCat Database Management Tool
After=network.target

[Service]
Type=simple
User=minicat
WorkingDirectory=/opt/minicat
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/minicat/minicat-server.jar
Restart=on-failure
RestartSec=10
Environment=SERVER_ADDRESS=127.0.0.1

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now minicat
sudo systemctl status minicat
journalctl -u minicat -f          # 查看日志
```

注意：`WorkingDirectory` 必须是希望存放 `data/` 的目录；`/opt/minicat/data` 需要运行用户可写权限。

---

## 4. Docker 部署

### 4.1 一键启动

```bash
git clone https://github.com/zhuyuebin/mini-cat.git
cd mini-cat
docker compose up -d
docker compose logs -f
```

访问 <http://localhost:8888>。

### 4.2 compose 配置要点

```yaml
services:
  minicat:
    build: .
    ports:
      - "8888:8888"
    volumes:
      - ./data:/app/data          # 持久化 H2 元数据 + 加密密钥
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SERVER_ADDRESS=0.0.0.0    # 容器内监听；对外暴露仍由 ports 控制
      - JAVA_TOOL_OPTIONS=-Xms512m -Xmx1024m
    restart: unless-stopped
```

- 镜像为多阶段构建：Node 20 构建前端 → Maven/JDK 17 打包 → JRE 17 alpine 运行（非 root 用户）；
- 健康检查探测 `http://localhost:8888/`（项目未引入 actuator，**不要**配置 `/actuator/health`）；
- 更新镜像：`docker compose up -d --build`；
- 停止：`docker compose down`（不会删除挂载的 `./data`）。

### 4.3 常用命令

```bash
docker compose ps
docker compose logs -f minicat
docker compose down
docker compose up -d --build
docker compose build --no-cache     # 网络/缓存异常时
```

---

## 5. 反向代理与远程访问加固

> **没有认证的 MiniCat 不能直接暴露。** 远程访问至少满足：HTTPS + 认证 + 访问来源限制。

### 5.1 Nginx（HTTPS + Basic Auth 示例）

```nginx
server {
    listen 443 ssl;
    server_name minicat.example.com;

    ssl_certificate     /etc/letsencrypt/live/minicat.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/minicat.example.com/privkey.pem;

    auth_basic           "MiniCat";
    auth_basic_user_file /etc/nginx/.htpasswd;

    location / {
        proxy_pass http://127.0.0.1:8888;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 60m;    # 需大于 50MB 导入上限
    }
}
```

生成密码文件：`sudo htpasswd -c /etc/nginx/.htpasswd your-user`。

### 5.2 Apache（等价配置）

```apache
<VirtualHost *:443>
    ServerName minicat.example.com
    ProxyPreserveHost On
    ProxyPass / http://127.0.0.1:8888/
    ProxyPassReverse / http://127.0.0.1:8888/
    <Location />
        AuthType Basic
        AuthName "MiniCat"
        AuthUserFile /etc/apache2/.htpasswd
        Require valid-user
    </Location>
</VirtualHost>
```

### 5.3 加固检查清单

- [ ] 不直接映射 8888 到公网，只经反代；
- [ ] HTTPS + Basic Auth / OAuth / VPN / IP 白名单；
- [ ] `MINICAT_SECRET_KEY` 用 Secret 注入，避免密钥文件随镜像分发；
- [ ] 目标库使用最小权限账号（只读账号可把内联编辑关闭）；
- [ ] `minicat.ui.inline-edit-enabled=false`（共享/生产库建议）；
- [ ] 定期备份 `data/` 并保护权限（600）；
- [ ] 在反代/数据库侧开启访问审计（应用自身无审计）。

---

## 6. 配置覆盖与日志

```bash
# 端口
java -jar minicat-server.jar --server.port=9000
SERVER_PORT=9000 java -jar minicat-server.jar

# Profile
java -jar minicat-server.jar --spring.profiles.active=prod

# JVM 参数（JAVA_TOOL_OPTIONS 会被 JVM 自动读取；JAVA_OPTS 不会）
export JAVA_TOOL_OPTIONS='-Xms512m -Xmx1024m'
```

日志位置：

| 部署方式 | 查看方式 |
|---|---|
| JAR 前台 | 控制台 |
| JAR + prod profile | `logs/minicat.log` |
| systemd | `journalctl -u minicat -f` |
| Docker | `docker compose logs -f` |

健康检查（无 actuator）：

```bash
curl -fsS http://127.0.0.1:8888/ >/dev/null && echo OK
curl -fsS http://127.0.0.1:8888/api/database/connections >/dev/null && echo API-OK
```

---

## 7. Kubernetes 示例

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: minicat
spec:
  replicas: 1
  selector:
    matchLabels: { app: minicat }
  template:
    metadata:
      labels: { app: minicat }
    spec:
      containers:
        - name: minicat
          image: your-registry/minicat:latest
          ports:
            - containerPort: 8888
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
            - name: SERVER_ADDRESS
              value: 0.0.0.0
            - name: MINICAT_SECRET_KEY
              valueFrom:
                secretKeyRef: { name: minicat-secret, key: secret-key }
          volumeMounts:
            - name: data
              mountPath: /app/data
          readinessProbe:
            httpGet: { path: /, port: 8888 }
          livenessProbe:
            httpGet: { path: /, port: 8888 }
      volumes:
        - name: data
          persistentVolumeClaim:
            claimName: minicat-data
---
apiVersion: v1
kind: Service
metadata:
  name: minicat
spec:
  ports:
    - port: 80
      targetPort: 8888
  selector: { app: minicat }
```

> 仍建议在前面加 Ingress + 认证，不要直接 `type: LoadBalancer` 暴露。
> 注意：单副本部署；H2 文件库不支持多实例并发写同一 `data/`。

---

## 8. 备份、升级与回滚

**备份**：停止写入后复制整个 `data/` 目录（H2 元数据 + `secret.key`）。
**升级**：

```bash
# 1. 备份 data/
cp -r data data.bak.$(date +%F)
# 2. 停止旧进程
# 3. 替换 JAR / 重新 docker compose up -d --build
# 4. 启动后验证首页与连接列表
```

**回滚**：恢复旧 JAR + 对应版本的 `data/` 备份（元数据表结构由 JPA `ddl-auto=update` 自动兼容，跨大版本回滚前建议先备份）。

---

## 9. 故障排查

| 现象 | 处理 |
|---|---|
| 端口被占用 | `lsof -i :8888` / `netstat -ano \| findstr :8888`；结束进程或改端口 |
| 启动报 H2 锁 | 确认无残留 Java 进程，删除 `data/minicat_db.lock.db` 后重启 |
| 容器内可访问、宿主不可 | 确认 `SERVER_ADDRESS=0.0.0.0` 与端口映射 |
| 上传大文件失败 | 反代 `client_max_body_size` 小于 50MB 或应用配置被覆盖 |
| 页面空白/404 | 访问 `/#/workspace`；强刷；确认静态资源随 JAR 构建 |
| 升级后连接密码失效 | `data/secret.key` / `MINICAT_SECRET_KEY` 变化，重新填写密码 |

---

## 10. 相关文档

- [GETTING_STARTED.md](GETTING_STARTED.md) — 安装与首次使用
- [CONFIGURATION.md](CONFIGURATION.md) — 配置/环境变量/数据目录
- [SECURITY.md](../SECURITY.md) — 安全模型与加固清单
- [RELEASING.md](RELEASING.md) — 发布与双远程同步
