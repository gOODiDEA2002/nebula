#!/usr/bin/env bash
# starter-web-example E2E 测试
# 验证 Web Starter 的核心功能: Hello/健康检查/性能监控
source "$(dirname "$0")/../e2e-common.sh"

PORT=8080
log_info "========== starter-web-example E2E =========="

start_app "examples/starter-web-example" "$PORT"

assert_contains "GET /hello 返回成功" \
    "http://localhost:$PORT/hello" '"success":true'

assert_contains "GET /hello 返回数据" \
    "http://localhost:$PORT/hello" 'Hello, Nebula Web'

assert_contains "GET /health/ping 健康检查" \
    "http://localhost:$PORT/health/ping" '"status":"pong"'

assert_contains "GET /performance/status 性能状态" \
    "http://localhost:$PORT/performance/status" '"status":"HEALTHY"'

print_summary "starter-web-example"
