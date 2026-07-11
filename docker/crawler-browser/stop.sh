#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "正在停止 Playwright Browser Server"
docker compose down
echo "Playwright Browser Server 已停止"
