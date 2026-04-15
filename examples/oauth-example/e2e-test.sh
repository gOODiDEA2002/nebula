#!/usr/bin/env bash
# oauth-example E2E 测试
# 验证 OAuth 后端能正常启动，前端可构建
# 外部依赖: MySQL（用户数据存储）
source "$(dirname "$0")/../e2e-common.sh"

PORT=8081
log_info "========== oauth-example E2E =========="

skip_if_no_service "MySQL" "192.168.2.130" 3306 "oauth-example"

# 启动后端
start_app "examples/oauth-example/backend" "$PORT"

# 健康检查
assert_contains "GET /health/ping 健康检查" \
    "http://localhost:$PORT/health/ping" '"status":"pong"'

assert_contains "GET /performance/status 性能状态" \
    "http://localhost:$PORT/performance/status" '"status"'

# OAuth 授权入口应返回重定向 URL
TOTAL=$((TOTAL + 1))
auth_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/api/oauth/authorize" 2>/dev/null || echo "000")
if [ "$auth_status" = "302" ] || [ "$auth_status" = "200" ] || [ "$auth_status" = "400" ]; then
    log_pass "GET /api/oauth/authorize 接口可访问 (HTTP $auth_status)"
    PASS=$((PASS + 1))
else
    log_fail "GET /api/oauth/authorize 意外状态码 (HTTP $auth_status)"
    FAIL=$((FAIL + 1))
fi

# 前端构建验证
TOTAL=$((TOTAL + 1))
FRONTEND_DIR="$PROJECT_ROOT/examples/oauth-example/frontend"
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

print_summary "oauth-example"
