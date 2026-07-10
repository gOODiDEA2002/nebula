#!/usr/bin/env bash
# starter-web-example E2E 测试
# 验证 Web Starter 的核心功能：Hello、健康检查、性能监控和 OpenAPI。
source "$(dirname "$0")/../e2e-common.sh"

PORT=8080
log_info "========== starter-web-example E2E =========="

start_app "examples/starter-web-example" "$PORT"

assert_json "GET /hello 返回统一成功响应" \
    "http://localhost:$PORT/hello" \
    '.success == true and .code == "SUCCESS" and .data == "Hello, Nebula Web"'

assert_json "GET /health/ping 健康检查" \
    "http://localhost:$PORT/health/ping" \
    '.success == true and .data.status == "pong"'

assert_json "GET /performance/status 性能状态" \
    "http://localhost:$PORT/performance/status" \
    '.success == true and .data.application.status == "HEALTHY"'

assert_json "GET /v3/api-docs 包含 Hello 接口" \
    "http://localhost:$PORT/v3/api-docs" \
    '(.openapi | startswith("3.")) and .paths["/hello"].get != null'

print_summary "starter-web-example"
