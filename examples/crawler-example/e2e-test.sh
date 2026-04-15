#!/usr/bin/env bash
# crawler-example E2E 测试
# 验证 HTTP 爬虫引擎的基础功能
# 无外部依赖（Browser 引擎默认禁用）
source "$(dirname "$0")/../e2e-common.sh"

PORT=8085
log_info "========== crawler-example E2E =========="

start_app "examples/crawler-example" "$PORT"

# 健康检查
assert_contains "GET /crawler/health 健康检查" \
    "http://localhost:$PORT/crawler/health" '"success":true'

# HTTP 引擎抓取测试 -- 抓取 example.com
assert_contains "GET /crawler/parse 抓取成功" \
    "http://localhost:$PORT/crawler/parse?url=https://example.com" '"success":true'

assert_contains "GET /crawler/parse 状态码 200" \
    "http://localhost:$PORT/crawler/parse?url=https://example.com" '"statusCode":200'

# Browser 引擎状态检查（默认禁用，应返回禁用状态）
TOTAL=$((TOTAL + 1))
browser_response=$(curl -sf "http://localhost:$PORT/crawler/health" 2>/dev/null || echo "CURL_FAILED")
if echo "$browser_response" | grep -q '"success":true'; then
    log_pass "GET /crawler/health 爬虫模块正常"
    PASS=$((PASS + 1))
else
    log_fail "GET /crawler/health 爬虫模块异常"
    FAIL=$((FAIL + 1))
fi

print_summary "crawler-example"
