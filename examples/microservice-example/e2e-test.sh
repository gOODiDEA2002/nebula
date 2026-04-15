#!/usr/bin/env bash
# microservice-example E2E 测试
# 验证微服务架构: user-service(1001) + order-service(1002) 通过 Nacos 通信
# 外部依赖: Nacos（服务发现）
source "$(dirname "$0")/../e2e-common.sh"

USER_PORT=1001
ORDER_PORT=1002
log_info "========== microservice-example E2E =========="

skip_if_no_service "Nacos" "localhost" 8848 "microservice-example"

# 先安装共享的 api 模块
log_info "安装 user-api 和 order-api 模块..."
cd "$PROJECT_ROOT"
mvn -q -f examples/microservice-example/user-api install -DskipTests || {
    log_fail "user-api 安装失败"; exit 1
}
mvn -q -f examples/microservice-example/order-api install -DskipTests || {
    log_fail "order-api 安装失败"; exit 1
}

# 启动 user-service
start_app "examples/microservice-example/user-service" "$USER_PORT"

# 启动 order-service
start_app "examples/microservice-example/order-service" "$ORDER_PORT"

# user-service 健康检查
assert_contains "user-service /health/ping" \
    "http://localhost:$USER_PORT/health/ping" '"status":"pong"'

# user-service RPC 接口 -- 获取用户列表（RPC 接口直接返回数据，不经 Result 封装）
assert_contains "user-service GET /rpc/users 用户列表" \
    "http://localhost:$USER_PORT/rpc/users" '"users"'

# user-service RPC 接口 -- 获取单个用户
assert_contains "user-service GET /rpc/users/1 获取用户" \
    "http://localhost:$USER_PORT/rpc/users/1" '"username"'

# order-service 健康检查
assert_contains "order-service /health/ping" \
    "http://localhost:$ORDER_PORT/health/ping" '"status":"pong"'

print_summary "microservice-example"
