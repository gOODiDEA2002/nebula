#!/bin/bash
# proxy-pool 停止脚本
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================="
echo "  停止 proxy-pool 服务"
echo "=================================="

docker compose down

echo "服务已停止"
