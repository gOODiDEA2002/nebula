#!/usr/bin/env bash
# rpc-async-example E2E 测试
# 验证异步 RPC 服务端和客户端的通信
# 外部依赖: Nacos（服务发现）
# 流程: 先启动 service(8081)，再启动 client(8082)，通过 client 调用 service
source "$(dirname "$0")/../e2e-common.sh"

SERVICE_PORT=8081
CLIENT_PORT=8082
log_info "========== rpc-async-example E2E =========="

skip_if_no_service "Nacos" "localhost" 8848 "rpc-async-example"

# 先安装共享的 api 模块
log_info "安装 rpc-async-example/api 模块..."
cd "$PROJECT_ROOT"
mvn -q -f examples/rpc-async-example/api install -DskipTests || {
    log_fail "api 模块安装失败"
    exit 1
}

# 启动 service 端
start_app "examples/rpc-async-example/service" "$SERVICE_PORT"

# 启动 client 端
start_app "examples/rpc-async-example/client" "$CLIENT_PORT"

assert_contains "Service /health/ping 健康检查" \
    "http://localhost:$SERVICE_PORT/health/ping" '"status":"pong"'

assert_contains "Client /health/ping 健康检查" \
    "http://localhost:$CLIENT_PORT/health/ping" '"status":"pong"'

# 通过 client 发起快速测试（同步 RPC 调用）
assert_contains "GET /api/tasks/test 异步 RPC 快速测试" \
    "http://localhost:$CLIENT_PORT/api/tasks/test?delay=1" '"executionId"'

print_summary "rpc-async-example"
