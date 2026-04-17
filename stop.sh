#!/bin/bash

# 关闭前后端服务

# 查找并关闭后端服务 (Java Spring Boot)
java_pid=$(lsof -ti:8080)
if [ -n "$java_pid" ]; then
    echo "关闭后端服务 (PID: $java_pid)..."
    kill $java_pid
    echo "后端服务已关闭"
else
    echo "后端服务未运行"
fi

# 查找并关闭前端服务 (Node/Vite)
# 常见前端端口: 3000, 5173, 5174
for port in 5173 5174 3000; do
    node_pid=$(lsof -ti:$port)
    if [ -n "$node_pid" ]; then
        echo "关闭前端服务 (PID: $node_pid, 端口: $port)..."
        kill $node_pid
        echo "前端服务已关闭"
        break
    fi
done

# 如果上述方法没找到，尝试通过项目目录查找
if ! lsof -ti:8080 > /dev/null && ! lsof -ti:5173 > /dev/null 2>&1; then
    project_dir=$(cd "$(dirname "$0")" && pwd)

    # 关闭包含项目路径的java进程
    for pid in $(ps aux | grep java | grep "$project_dir" | awk '{print $2}'); do
        echo "关闭后端服务 (PID: $pid)..."
        kill $pid 2>/dev/null
    done

    # 关闭包含项目路径的node进程
    for pid in $(ps aux | grep node | grep "$project_dir" | awk '{print $2}'); do
        echo "关闭前端服务 (PID: $pid)..."
        kill $pid 2>/dev/null
    done
fi

echo "完成"
