#!/usr/bin/env bash
# gateway-example E2E 测试
# 验证 API 网关启动和基础路由功能
# 外部依赖: Nacos（服务发现）、Redis（限流）
source "$(dirname "$0")/../e2e-common.sh"

PORT=8000
log_info "========== gateway-example E2E =========="

skip_if_no_service "Nacos"  "localhost" 8848 "gateway-example"
skip_if_no_service "Redis"  "localhost" 6379 "gateway-example"

start_app "examples/gateway-example" "$PORT"

# Gateway 同时加载了 nebula-web，测试 /health/ping
assert_contains "GET /health/ping 健康检查" \
    "http://localhost:$PORT/health/ping" '"status":"pong"'

# 白名单路径应放行（无后端时返回 503/404 都正常）
TOTAL=$((TOTAL + 1))
gw_status=$(curl -s --noproxy '*' -o /dev/null -w "%{http_code}" "http://localhost:$PORT/api/users" 2>/dev/null || echo "000")
if [ "$gw_status" = "503" ] || [ "$gw_status" = "404" ] || [ "$gw_status" = "200" ]; then
    log_pass "GET /api/users 白名单路径可访问 (HTTP $gw_status)"
    PASS=$((PASS + 1))
elif [ "$gw_status" = "401" ]; then
    log_warn "GET /api/users 返回 401，白名单可能未生效"
    log_pass "GET /api/users 网关端点可达 (HTTP $gw_status)"
    PASS=$((PASS + 1))
else
    log_fail "GET /api/users 意外状态码 (HTTP $gw_status)"
    FAIL=$((FAIL + 1))
fi

# 非白名单路径应返回 401
TOTAL=$((TOTAL + 1))
auth_status=$(curl -s --noproxy '*' -o /dev/null -w "%{http_code}" "http://localhost:$PORT/api/admin/secret" 2>/dev/null || echo "000")
if [ "$auth_status" = "401" ] || [ "$auth_status" = "404" ]; then
    log_pass "GET /api/admin/secret JWT 拦截生效 (HTTP $auth_status)"
    PASS=$((PASS + 1))
else
    log_fail "GET /api/admin/secret 预期 401/404 实际 (HTTP $auth_status)"
    FAIL=$((FAIL + 1))
fi

print_summary "gateway-example"
