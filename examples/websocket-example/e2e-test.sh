#!/usr/bin/env bash
# websocket-example E2E 测试
# 验证 WebSocket 后端 REST API 和 WebSocket 连接
# 无外部依赖
source "$(dirname "$0")/../e2e-common.sh"

PORT=8086
log_info "========== websocket-example E2E =========="

start_app "examples/websocket-example/backend" "$PORT"

# REST API 验证（控制器路径为 /ws-api）
assert_contains "GET /ws-api/status WebSocket 状态" \
    "http://localhost:$PORT/ws-api/status" '"success":true'

assert_contains "GET /ws-api/status 返回在线数" \
    "http://localhost:$PORT/ws-api/status" '"onlineSessions"'

# 广播消息（无在线用户时也应正常返回）
assert_contains "POST /ws-api/broadcast 广播消息" \
    "http://localhost:$PORT/ws-api/broadcast" '"success":true' \
    "POST" '{"content":"e2e-test-broadcast","type":"SYSTEM"}'

# WebSocket 连接测试（通过 python3）
TOTAL=$((TOTAL + 1))
if command -v python3 >/dev/null 2>&1; then
    ws_result=$(NO_PROXY='*' no_proxy='*' python3 -c "
import asyncio, sys
try:
    import websockets
except ImportError:
    print('NO_WEBSOCKETS_MODULE')
    sys.exit(0)

async def test():
    try:
        async with websockets.connect('ws://localhost:$PORT/ws?userId=e2e-test-user') as ws:
            await ws.send('{\"type\":\"CHAT\",\"content\":\"hello\"}')
            msg = await asyncio.wait_for(ws.recv(), timeout=5)
            print('WS_OK:' + msg[:80])
    except Exception as e:
        print('WS_FAIL:' + str(e)[:80])

asyncio.run(test())
" 2>/dev/null || echo "PYTHON_FAILED")

    if echo "$ws_result" | grep -q "WS_OK\|NO_WEBSOCKETS_MODULE"; then
        if echo "$ws_result" | grep -q "NO_WEBSOCKETS_MODULE"; then
            log_skip "WebSocket 连接测试 (缺少 websockets 模块)"
            SKIP=$((SKIP + 1))
            TOTAL=$((TOTAL - 1))
        else
            log_pass "WebSocket 连接并收发消息"
            PASS=$((PASS + 1))
        fi
    else
        log_fail "WebSocket 连接失败: $ws_result"
        FAIL=$((FAIL + 1))
    fi
else
    log_skip "WebSocket 连接测试 (缺少 python3)"
    SKIP=$((SKIP + 1))
    TOTAL=$((TOTAL - 1))
fi

# 前端构建验证
TOTAL=$((TOTAL + 1))
FRONTEND_DIR="$PROJECT_ROOT/examples/websocket-example/frontend"
if [ -f "$FRONTEND_DIR/package.json" ]; then
    log_info "验证前端 npm install..."
    cd "$FRONTEND_DIR"
    if npm install --silent 2>/dev/null; then
        log_pass "前端 npm install 成功"
        PASS=$((PASS + 1))
    else
        log_fail "前端 npm install 失败"
        FAIL=$((FAIL + 1))
    fi
else
    log_skip "前端 package.json 不存在"
    SKIP=$((SKIP + 1))
fi

print_summary "websocket-example"
