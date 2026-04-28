# 使用多阶段构建优化镜像大小
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# 复制前端项目文件
COPY MiniCat-Frontend/package*.json ./

# 安装依赖
RUN npm install

# 复制前端源代码
COPY MiniCat-Frontend/ ./

# 构建前端
RUN npm run build

# 第二阶段：构建后端
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app

# 复制pom.xml并下载依赖（利用Docker缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src
COPY MiniCat-Frontend ./MiniCat-Frontend

# 从第一阶段复制构建好的前端文件
COPY --from=frontend-builder /app/frontend/dist ./MiniCat-Frontend/dist

# 构建Spring Boot应用
RUN mvn clean package -DskipTests -B

# 第三阶段：运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 添加元数据标签
LABEL maintainer="MiniCat Team"
LABEL description="A lightweight web-based database management tool"
LABEL version="1.0.0"

# 创建非root用户
RUN addgroup -S minicat && adduser -S minicat -G minicat

# 复制构建好的JAR文件
COPY --from=backend-builder /app/target/*.jar app.jar

# 设置文件权限
RUN chown -R minicat:minicat /app

# 切换到非root用户
USER minicat

# 暴露端口
EXPOSE 8888

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8888/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
