#!/bin/bash
# OpenCV 验证码识别服务启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================="
echo "  启动 OpenCV 验证码识别服务"
echo "=================================="

# 检查网络是否存在
if ! docker network ls | grep -q crawler-network; then
    echo "创建 Docker 网络: crawler-network"
    docker network create crawler-network
fi

# 启动服务
echo "启动服务..."
docker-compose up -d

# 等待服务就绪
echo "等待服务就绪..."
OK_COUNT=0
for i in {1..30}; do
    OK1=$(curl -sf http://localhost:8867/ping > /dev/null 2>&1 && echo 1 || echo 0)
    OK2=$(curl -sf http://localhost:8868/ping > /dev/null 2>&1 && echo 1 || echo 0)
    
    if [ "$OK1" = "1" ] && [ "$OK2" = "1" ]; then
        echo ""
        echo "=================================="
        echo "  服务启动成功！"
        echo "  实例1: http://localhost:8867"
        echo "  实例2: http://localhost:8868"
        echo "=================================="
        exit 0
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "警告: 服务可能未正常启动，请检查日志"
docker-compose logs
