#!/usr/bin/env bash
# starter-all-example E2E 测试
# 验证全量 Starter 在零外部依赖配置下正常启动
source "$(dirname "$0")/../e2e-common.sh"

PORT=8084
log_info "========== starter-all-example E2E =========="

start_app "examples/starter-all-example" "$PORT"

assert_json "GET /hello 返回统一成功响应" \
    "http://localhost:$PORT/hello" \
    '.success == true and .code == "SUCCESS" and .data == "Hello, Nebula All"'

assert_json "GET /health/ping 健康检查" \
    "http://localhost:$PORT/health/ping" \
    '.success == true and .data.status == "pong"'

assert_json "GET /performance/status 性能状态" \
    "http://localhost:$PORT/performance/status" \
    '.success == true and (.data.application.status | IN("HEALTHY", "WARNING"))'

assert_json "GET /v3/api-docs 包含 Hello 接口" \
    "http://localhost:$PORT/v3/api-docs" \
    '(.openapi | startswith("3.")) and .paths["/hello"].get != null'

assert_file_not_contains "外部模块未建立连接或启动运行组件" "$CURRENT_APP_LOG" \
    'ConnectionsHolder.*connections initialized|NacosServiceAutoRegistrar|RabbitMQAutoConfiguration|WebSocketAutoConfiguration|TaskEngine|AIAutoConfiguration|配置HTTP RPC服务器'

print_summary "starter-all-example"
