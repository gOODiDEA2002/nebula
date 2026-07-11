#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORT="${CRAWLER_BROWSER_PORT:-9222}"
cd "$SCRIPT_DIR"

if ! docker info >/dev/null 2>&1; then
    echo "错误: Docker 未运行，请先启动 Docker" >&2
    exit 1
fi

echo "正在启动 Playwright Browser Server，宿主端口: $PORT"
CRAWLER_BROWSER_PORT="$PORT" docker compose up -d crawler-browser-01

for _ in $(seq 1 30); do
    if [ "$(curl -fsS --noproxy '*' "http://localhost:$PORT/" 2>/dev/null || true)" = "Running" ]; then
        echo "Playwright Browser Server 已就绪"
        echo "WebSocket: ws://localhost:$PORT"
        docker exec crawler-browser-01 npx playwright --version
        echo "查看日志: docker compose logs -f crawler-browser-01"
        echo "停止服务: ./stop.sh"
        exit 0
    fi
    sleep 1
done

echo "错误: 服务未在 30 秒内就绪" >&2
docker compose logs crawler-browser-01 >&2
exit 1
