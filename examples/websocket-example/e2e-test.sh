#!/usr/bin/env bash
# websocket-example E2E：验证 REST、双客户端协议、前端构建和浏览器流程。
source "$(dirname "$0")/../e2e-common.sh"

PORT="${WEBSOCKET_BACKEND_PORT:-8086}"
FRONTEND_PORT="${WEBSOCKET_FRONTEND_PORT:-3000}"
FRONTEND_DIR="$PROJECT_ROOT/examples/websocket-example/frontend"

log_info "========== websocket-example E2E =========="
require_command node
require_command npm
require_command jq
ensure_port_available "$PORT"
ensure_port_available "$FRONTEND_PORT"

run_checked "前端 npm ci 成功" npm --prefix "$FRONTEND_DIR" ci
run_checked "前端生产构建成功" npm --prefix "$FRONTEND_DIR" run build

start_app "examples/websocket-example/backend" "$PORT"

assert_json "初始 WebSocket 状态为空" \
    "http://localhost:$PORT/ws-api/status" \
    '.success == true and .data.onlineSessions == 0 and .data.onlineUsers == 0'

PROTOCOL_EVIDENCE="$E2E_CASE_DIR/websocket-protocol.json"
run_checked "双客户端 WebSocket 协议流程成功" \
    sh -c "node '$FRONTEND_DIR/e2e-websocket.mjs' 'http://localhost:$PORT' > '$PROTOCOL_EVIDENCE'"
run_checked "两个用户和会话均完成连接" \
    jq -e '.connected == true and .initialSessions == 2 and .initialUsers == 2' "$PROTOCOL_EVIDENCE"
run_checked "聊天和 REST 广播送达两个客户端" \
    jq -e '.chatDelivered == true and .broadcastSentTo == 2' "$PROTOCOL_EVIDENCE"
run_checked "按用户发送仅命中目标用户" \
    jq -e '.userSentTo == 1 and .userAOnline == true' "$PROTOCOL_EVIDENCE"
run_checked "按会话发送仅命中目标会话" \
    jq -e '.sessionSent == true' "$PROTOCOL_EVIDENCE"
run_checked "应用层心跳返回 pong" \
    jq -e '.heartbeat == "pong"' "$PROTOCOL_EVIDENCE"
run_checked "客户端断开后在线状态正确" \
    jq -e '.userBOnlineAfterClose == false and .finalSessions == 1 and .finalUsers == 1' "$PROTOCOL_EVIDENCE"

FRONTEND_LOG="$E2E_CASE_DIR/websocket-frontend.log"
log_info "启动 WebSocket 前端，端口 ${FRONTEND_PORT}，日志 ${FRONTEND_LOG}"
(
    cd "$FRONTEND_DIR"
    exec npm run dev -- --host 127.0.0.1 --port "$FRONTEND_PORT"
) >"$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!
APP_PIDS+=("$FRONTEND_PID")
APP_PORTS+=("$FRONTEND_PORT")
APP_LOGS+=("$FRONTEND_LOG")
if wait_for_port "$FRONTEND_PORT" 30 "$FRONTEND_PID"; then
    record_pass "WebSocket 前端开发服务器已启动"
else
    record_fail "WebSocket 前端开发服务器启动失败"
fi

assert_contains "前端页面可访问" \
    "http://localhost:$FRONTEND_PORT" '<title>Nebula WebSocket Demo</title>'

UI_EVIDENCE="$E2E_CASE_DIR/websocket-ui.json"
UI_SCREENSHOT="$E2E_CASE_DIR/websocket-ui.png"
run_checked "浏览器完成连接和发送消息" \
    sh -c "node '$FRONTEND_DIR/e2e-ui.mjs' 'http://localhost:$FRONTEND_PORT' '$UI_SCREENSHOT' > '$UI_EVIDENCE'"
run_checked "浏览器流程结果完整" \
    jq -e '.connected == true and .messageDelivered == true' "$UI_EVIDENCE"
assert_file_exists "浏览器截图证据已保存" "$UI_SCREENSHOT"

print_summary "websocket-example"
