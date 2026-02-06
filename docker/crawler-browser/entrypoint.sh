#!/bin/bash
# 使用 CDP 模式启动 Chromium
# Chrome DevTools Protocol 支持多客户端连接
# 注意：新版 Chrome --headless=new 模式会忽略 --remote-debugging-address=0.0.0.0
# 需要使用 socat 进行端口转发

set -e

# 查找 Playwright 安装的 Chromium 路径
CHROMIUM_DIR="/root/.cache/ms-playwright"
CHROMIUM_PATH=$(find "$CHROMIUM_DIR" -name "chrome" -type f 2>/dev/null | head -1)

if [ -z "$CHROMIUM_PATH" ]; then
    echo "Error: Chromium executable not found in $CHROMIUM_DIR"
    exit 1
fi

echo "Starting Chromium with CDP mode..."
echo "Chromium path: $CHROMIUM_PATH"

# 后台启动 Chromium（绑定到 127.0.0.1:9223 内部端口）
"$CHROMIUM_PATH" \
    --remote-debugging-port=9223 \
    --no-sandbox \
    --disable-setuid-sandbox \
    --disable-dev-shm-usage \
    --disable-gpu \
    --headless=new \
    --disable-blink-features=AutomationControlled \
    --no-first-run \
    --no-default-browser-check \
    --disable-background-networking \
    --disable-sync \
    --disable-translate \
    --mute-audio \
    --disable-extensions \
    --disable-component-extensions-with-background-pages \
    --disable-background-timer-throttling \
    --disable-backgrounding-occluded-windows \
    --disable-renderer-backgrounding \
    --disable-ipc-flooding-protection &

CHROME_PID=$!
echo "Chrome started with PID: $CHROME_PID"

# 等待 Chrome 启动
sleep 2

# 检查 Chrome 是否还在运行
if ! kill -0 $CHROME_PID 2>/dev/null; then
    echo "Error: Chrome process terminated unexpectedly"
    exit 1
fi

echo "Starting socat to forward 0.0.0.0:9222 -> 127.0.0.1:9223..."

# 使用 socat 将外部请求转发到内部 Chrome 端口
exec socat TCP-LISTEN:9222,fork,reuseaddr TCP:127.0.0.1:9223
