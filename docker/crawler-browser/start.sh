#!/bin/bash

# 启动 Chrome 浏览器服务
# 用法: ./start.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  Chrome 浏览器服务"
echo "=========================================="

# 检查 Docker
if ! docker info >/dev/null 2>&1; then
    echo "错误: Docker 未运行，请先启动 Docker"
    exit 1
fi

# 构建并启动
echo "正在启动浏览器服务..."
docker-compose up -d

# 等待服务就绪
echo "等待服务就绪..."
for i in {1..30}; do
    if curl -s http://localhost:9222/json/version >/dev/null 2>&1; then
        echo ""
        echo "=========================================="
        echo "  浏览器服务已就绪！"
        echo "=========================================="
        echo ""
        echo "CDP 端点: http://localhost:9222"
        echo "WebSocket: ws://localhost:9222"
        echo ""
        echo "查看状态: docker-compose logs -f"
        echo "停止服务: docker-compose down"
        echo ""
        
        # 显示版本信息
        echo "浏览器信息:"
        curl -s http://localhost:9222/json/version | python3 -m json.tool 2>/dev/null || \
            curl -s http://localhost:9222/json/version
        exit 0
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "错误: 服务启动超时"
docker-compose logs
exit 1
