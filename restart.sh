#!/bin/bash

echo "正在重启 MiniCat 后端服务..."

# 查找并杀死现有的 Java 进程
PID=$(ps aux | grep "com.minicat.minicatserver.MiniCatApplication" | grep -v grep | awk '{print $2}')

if [ ! -z "$PID" ]; then
    echo "发现运行中的进程: $PID"
    kill $PID
    echo "等待进程停止..."
    sleep 3
    
    # 如果还在运行，强制杀死
    if ps -p $PID > /dev/null; then
        echo "强制停止进程..."
        kill -9 $PID
        sleep 1
    fi
    echo "旧进程已停止"
else
    echo "没有发现运行中的进程"
fi

# 启动新的后端服务
echo "启动新的后端服务..."
cd /home/zhuyuebin/mini-cat
nohup java -jar target/minicat-server-0.0.1-SNAPSHOT.jar > minicat.log 2>&1 &

NEW_PID=$!
echo "新进程已启动，PID: $NEW_PID"
echo "等待服务启动..."
sleep 5

# 检查服务是否启动成功
if curl -s http://localhost:8888/api/database/connections > /dev/null; then
    echo "✅ 后端服务启动成功！"
    echo "访问地址："
    echo "  - 生产模式: http://localhost:8888"
    echo "  - 开发模式: http://localhost:5173 (需要先启动 Vite)"
else
    echo "❌ 后端服务启动失败，请查看日志: minicat.log"
fi
