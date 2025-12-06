#!/bin/bash

# 停止所有RPC服务实例的脚本

echo "Stopping all RPC server instances..."

# 杀死所有相关的Java进程
pkill -f "site.hexaarch.rpc.example.ServerTest"

# 等待进程结束
sleep 2

# 检查是否还有相关进程在运行
if pgrep -f "site.hexaarch.rpc.example.ServerTest" > /dev/null; then
    echo "Some processes are still running, force killing..."
    pkill -9 -f "site.hexaarch.rpc.example.ServerTest"
else
    echo "All RPC server instances stopped."
fi