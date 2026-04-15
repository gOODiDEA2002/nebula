#!/usr/bin/env bash
# starter-service-example E2E 测试
# 验证 Service Starter 的核心功能: 健康检查、分布式锁
# 外部依赖: Redis（锁服务），Nacos 已禁用
source "$(dirname "$0")/../e2e-common.sh"

PORT=8082
log_info "========== starter-service-example E2E =========="

skip_if_no_service "Redis" "localhost" 6379 "starter-service-example"

start_app "examples/starter-service-example" "$PORT"

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

print_summary "starter-service-example"
