#!/bin/bash

# 启动多个RPC服务提供方实例的脚本

# 检查是否安装了Maven
if ! command -v mvn &> /dev/null
then
    echo "Maven is not installed or not in PATH"
    exit 1
fi

# 编译项目
echo "Compiling the project..."
mvn compile test-compile

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# 定义端口列表
PORTS=("8081" "8082" "8083")

# 杀死之前可能存在的实例
pkill -f "site.hexaarch.rpc.example.ServerTest" >/dev/null 2>&1

# 等待旧实例完全停止
sleep 2

echo "Starting multiple server instances..."

# 启动多个服务实例
for PORT in "${PORTS[@]}"; do
    echo "Starting server instance on port $PORT..."
    # 使用不同的日志配置来区分不同实例的日志
    mvn test -Dtest=site.hexaarch.rpc.example.ServerTest -Dserver.port=$PORT > server_$PORT.log 2>&1 &
    
    # 等待一点时间确保服务启动
    sleep 3
    
    if ps -p $! > /dev/null; then
        echo "Server instance on port $PORT started successfully (PID: $!)"
    else
        echo "Failed to start server instance on port $PORT"
    fi
done

echo ""
echo "All server instances started."
echo "Logs are available in server_<port>.log files"
echo "To stop the servers, run: ./stop-servers.sh"