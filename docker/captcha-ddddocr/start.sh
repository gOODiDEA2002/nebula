#!/bin/bash
# 启动 ddddocr 服务

# 确保脚本抛出遇到的错误
set -e

# 获取脚本所在目录
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# 检查是否安装了 Docker
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed."
    exit 1
fi

# 停止旧容器
if [ "$(docker ps -q -f name=captcha-ddddocr)" ]; then
    echo "Stopping existing container..."
    docker stop captcha-ddddocr
    docker rm captcha-ddddocr
fi
# 清理可能存在的已停止容器
if [ "$(docker ps -aq -f name=captcha-ddddocr)" ]; then
    docker rm captcha-ddddocr
fi

# 构建镜像
echo "Building captcha-ddddocr image..."
docker build -t captcha-ddddocr:latest "$DIR"

# 启动容器
echo "Starting captcha-ddddocr container..."
docker run -d \
    --name captcha-ddddocr \
    -p 8866:8866 \
    --restart unless-stopped \
    captcha-ddddocr:latest

echo "Service started on port 8866"
