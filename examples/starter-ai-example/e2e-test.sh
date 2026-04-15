#!/usr/bin/env bash
# starter-ai-example E2E 测试
# 验证 AI Starter 在 AI 禁用模式下能正常启动
# AI 功能需要有效的 API Key，此脚本仅验证框架启动
source "$(dirname "$0")/../e2e-common.sh"

PORT=8083
log_info "========== starter-ai-example E2E =========="

start_app "examples/starter-ai-example" "$PORT"

# starter-ai 不含 nebula-web，使用 /ai/echo 验证 Web 可用
assert_contains "GET /ai/echo 接口可用" \
    "http://localhost:$PORT/ai/echo?q=test" '"success"'

# AI 禁用时应返回 AI 不可用或降级信息，但不应 500 崩溃
TOTAL=$((TOTAL + 1))
ai_response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/ai/echo?q=test" 2>/dev/null || echo "000")
if [ "$ai_response" != "500" ]; then
    log_pass "GET /ai/echo AI 禁用时不崩溃 (HTTP $ai_response)"
    PASS=$((PASS + 1))
else
    log_fail "GET /ai/echo AI 禁用时返回 500"
    FAIL=$((FAIL + 1))
fi

print_summary "starter-ai-example"
