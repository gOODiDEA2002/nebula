#!/usr/bin/env bash
# starter-all-example E2E 测试
# 验证全量 Starter 在外部服务最小依赖下能正常启动
# 大部分外部服务已禁用，仅测试基础功能
source "$(dirname "$0")/../e2e-common.sh"

PORT=8084
log_info "========== starter-all-example E2E =========="

skip_if_no_service "Redis" "localhost" 6379 "starter-all-example"

start_app "examples/starter-all-example" "$PORT"

assert_contains "GET /hello 返回成功" \
    "http://localhost:$PORT/hello" '"success":true'

assert_contains "GET /hello 返回数据" \
    "http://localhost:$PORT/hello" 'Hello, Nebula'

assert_contains "GET /health/ping 健康检查" \
    "http://localhost:$PORT/health/ping" '"status":"pong"'

# 性能监控可能未启用，仅验证端点存在且不崩溃
TOTAL=$((TOTAL + 1))
perf_code=$(curl -s --noproxy '*' -o /dev/null -w "%{http_code}" "http://localhost:$PORT/performance/status" 2>/dev/null || echo "000")
if [ "$perf_code" = "200" ] || [ "$perf_code" = "503" ]; then
    log_pass "GET /performance/status 端点可访问 (HTTP $perf_code)"
    PASS=$((PASS + 1))
else
    log_fail "GET /performance/status 意外状态码 (HTTP $perf_code)"
    FAIL=$((FAIL + 1))
fi

print_summary "starter-all-example"
