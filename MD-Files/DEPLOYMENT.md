# MiniCat 快速部署指南

## 📦 部署方式总览

本指南提供多种部署方式,选择最适合你的一种即可。

---

## 方式一: 使用启动脚本 (最简单)

### Linux/Mac

```bash
# 1. 克隆项目
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd MiniCat

# 2. 赋予执行权限
chmod +x start.sh

# 3. 启动
./start.sh
```

### Windows

```bash
# 1. 克隆项目
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd MiniCat

# 2. 双击运行
start.bat
```

**访问**: http://localhost:8888

---

## 方式二: 使用 Docker (推荐生产环境)

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+

### 部署步骤

```bash
# 1. 克隆项目
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd MiniCat

# 2. 一键启动
docker-compose up -d

# 3. 查看日志
docker-compose logs -f
```

### 常用命令

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看容器状态
docker-compose ps

# 更新镜像并重新部署
docker-compose pull
docker-compose up -d --build
```

**访问**: http://localhost:8888

---

## 方式三: 手动构建 JAR 包

### 前置要求

- JDK 17+
- Maven 3.6+

### 构建步骤

```bash
# 1. 克隆项目
git clone https://gitee.com/zhuyuebin/mini-cat.git
cd MiniCat

# 2. 构建项目
mvn clean package -DskipTests

# 3. 运行应用
java -jar target/minicat-server-0.0.1-SNAPSHOT.jar
```

### 后台运行 (Linux)

```bash
# 使用 nohup 后台运行
nohup java -jar target/minicat-server-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 查看进程
ps aux | grep minicat

# 停止应用
kill $(ps aux | grep 'minicat-server' | awk '{print $2}')
```

### 使用 systemd 管理 (推荐)

创建服务文件 `/etc/systemd/system/minicat.service`:

```ini
[Unit]
Description=MiniCat Database Management Tool
After=network.target

[Service]
Type=simple
User=minicat
WorkingDirectory=/opt/minicat
ExecStart=/usr/bin/java -jar /opt/minicat/minicat-server.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务:

```bash
sudo systemctl daemon-reload
sudo systemctl enable minicat
sudo systemctl start minicat
sudo systemctl status minicat
```

---

## 方式四: Kubernetes 部署

### 创建 deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: minicat
spec:
  replicas: 1
  selector:
    matchLabels:
      app: minicat
  template:
    metadata:
      labels:
        app: minicat
    spec:
      containers:
      - name: minicat
        image: your-registry/minicat:latest
        ports:
        - containerPort: 8080
        volumeMounts:
        - name: data
          mountPath: /app/data
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
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: minicat
```

部署:

```bash
kubectl apply -f deployment.yaml
```

---

## 🔧 配置说明

### 修改端口

#### 方法1: 命令行参数
```bash
java -jar minicat-server.jar --server.port=9090
```

#### 方法2: 环境变量
```bash
export SERVER_PORT=9090
java -jar minicat-server.jar
```

#### 方法3: 配置文件
编辑 `application.properties`:
```properties
server.port=9090
```

### 数据库持久化

H2数据库文件默认存储在 `data/` 目录。

**Docker部署时**:
```yaml
volumes:
  - ./data:/app/data
```

**JAR部署时**:
确保 `data/` 目录有写权限。

### JVM 参数调优

```bash
java -Xms512m -Xmx1024m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar minicat-server.jar
```

---

## 🌐 反向代理配置

### Nginx 配置

```nginx
server {
    listen 80;
    server_name minicat.example.com;

    location / {
        proxy_pass http://localhost:8888;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### Apache 配置

```apache
<VirtualHost *:80>
    ServerName minicat.example.com
    
    ProxyPreserveHost On
    ProxyPass / http://localhost:8888/
    ProxyPassReverse / http://localhost:8888/
</VirtualHost>
```

---

## 🔒 安全建议

### 生产环境检查清单

- [ ] 修改默认端口（可选）
- [ ] 配置HTTPS
- [ ] 设置防火墙规则
- [ ] 定期备份数据
- [ ] 启用日志轮转
- [ ] 限制访问IP（可选）

### 配置 HTTPS

使用 Let's Encrypt:

```bash
# 安装 certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d minicat.example.com
```

---

## 📊 监控和日志

### 查看日志

```bash
# Docker
docker-compose logs -f minicat

# JAR
tail -f app.log

# systemd
journalctl -u minicat -f
```

### 健康检查

```bash
curl http://localhost:8888/actuator/health
```

---

## ❓ 故障排查

### 问题1: 端口被占用

```bash
# 查找占用端口的进程
lsof -i :8888
netstat -tlnp | grep 8888

# 杀死进程
kill -9 <PID>
```

### 问题2: 内存不足

```bash
# 增加JVM堆内存
java -Xms1g -Xmx2g -jar minicat-server.jar
```

### 问题3: 数据库连接失败

检查目标数据库是否可访问:
```bash
telnet your-db-host 3306
```

---

## 📞 获取帮助

- 查看文档: [README.md](README.md)
- 提交问题: [Issues](https://gitee.com/zhuyuebin/mini-cat/issues)

---

祝部署顺利! 🎉
