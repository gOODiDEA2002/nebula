#!/bin/bash

# 停止 Chrome 浏览器服务
# 用法: ./stop.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "正在停止浏览器服务..."
docker-compose down

echo "浏览器服务已停止"
