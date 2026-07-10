#!/usr/bin/env bash
# gateway-example E2E 测试
# 验证真实代理、JWT 认证、Redis 限流、令牌恢复和资源清理。
source "$(dirname "$0")/../e2e-common.sh"

GATEWAY_PORT=${GATEWAY_PORT:-8000}
USER_PORT=${USER_HTTP_PORT:-1001}
ORDER_PORT=${ORDER_HTTP_PORT:-1002}
USER_GRPC_PORT=${USER_GRPC_PORT:-2001}
ORDER_GRPC_PORT=${ORDER_GRPC_PORT:-2002}
NACOS_HOST="${NEBULA_E2E_NACOS_HOST:-localhost}"
NACOS_PORT="${NEBULA_E2E_NACOS_PORT:-8848}"
REDIS_HOST="${NEBULA_E2E_REDIS_HOST:-localhost}"
REDIS_PORT="${NEBULA_E2E_REDIS_PORT:-6379}"
REDIS_DB=15
JWT_SECRET="nebula-e2e-gateway-jwt-secret-at-least-32-bytes"
RATE_PATTERN='request_rate_limiter.*'
RATE_KEYS_CLEANED=0

export GATEWAY_PORT USER_HTTP_PORT="$USER_PORT" ORDER_HTTP_PORT="$ORDER_PORT"
export USER_GRPC_PORT ORDER_GRPC_PORT
export NACOS_SERVER_ADDR="${NACOS_HOST}:${NACOS_PORT}"
export NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
export NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"
export NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
export REDIS_HOST REDIS_PORT
export GATEWAY_REDIS_DATABASE="$REDIS_DB"
export GATEWAY_JWT_SECRET="$JWT_SECRET"
export GATEWAY_RATE_REPLENISH=1
export GATEWAY_RATE_BURST=5
export GATEWAY_RATE_TOKENS=1

base64url() {
    openssl base64 -A | tr '+/' '-_' | tr -d '='
}

create_jwt() {
    local now expires header payload signing_input signature
    now=$(date +%s)
    expires=$((now + 600))
    header=$(printf '%s' '{"alg":"HS256","typ":"JWT"}' | base64url)
    payload=$(jq -cn --argjson iat "$now" --argjson exp "$expires" \
        '{sub:"nebula_e2e_user",username:"nebula_e2e",iat:$iat,exp:$exp}' | base64url)
    signing_input="$header.$payload"
    signature=$(printf '%s' "$signing_input" | \
        openssl dgst -sha256 -mac HMAC -macopt "key:$JWT_SECRET" -binary | base64url)
    printf '%s.%s' "$signing_input" "$signature"
}

rate_keys() {
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB" --scan --pattern "$RATE_PATTERN"
}

cleanup_rate_keys() {
    local key
    while IFS= read -r key; do
        [ -n "$key" ] || continue
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB" DEL "$key" >/dev/null 2>&1 || true
    done < <(rate_keys 2>/dev/null || true)
}

cleanup_gateway() {
    local exit_code=$?
    trap - EXIT
    set +e
    stop_all_apps false
    if [ "$RATE_KEYS_CLEANED" -eq 0 ]; then
        cleanup_rate_keys
    fi
    cleanup "$exit_code"
    exit "$exit_code"
}

trap cleanup_gateway EXIT

log_info "========== gateway-example E2E =========="
skip_if_no_service Nacos "$NACOS_HOST" "$NACOS_PORT" gateway-example
skip_if_no_service Redis "$REDIS_HOST" "$REDIS_PORT" gateway-example
require_command redis-cli || exit 1
require_command openssl || exit 1

if [ -n "$(rate_keys)" ]; then
    record_fail "Redis 测试库 $REDIS_DB 已有同名限流键，拒绝覆盖"
    print_summary gateway-example
    exit $?
fi
record_pass "Redis 测试库 $REDIS_DB 无既有限流键"

mvn -q -f examples/microservice-example/user-api install -DskipTests
mvn -q -f examples/microservice-example/order-api install -DskipTests

start_app "examples/microservice-example/user-service" "$USER_PORT"
USER_LOG=$CURRENT_APP_LOG
start_app "examples/microservice-example/order-service" "$ORDER_PORT"
ORDER_LOG=$CURRENT_APP_LOG
start_app "examples/gateway-example" "$GATEWAY_PORT"
GATEWAY_LOG=$CURRENT_APP_LOG

assert_json "Gateway 健康检查" "http://localhost:$GATEWAY_PORT/health/ping" \
    '.status == "pong" and .message == "Gateway is running"'
assert_file_contains "Gateway 路由指向真实 User 服务名" "$GATEWAY_LOG" \
    'nebula-example-user-service -> lb://nebula-example-user-service'
assert_file_contains "Gateway 路由指向真实 Order 服务名" "$GATEWAY_LOG" \
    'nebula-example-order-service -> lb://nebula-example-order-service'

assert_json "白名单请求代理到真实 User 后端" \
    "http://localhost:$GATEWAY_PORT/api/users?page=1&size=2" \
    '.total == 10 and (.users | length) == 2 and .users[0].username != null'
assert_file_contains "User 后端收到 Gateway 列表请求" "$USER_LOG" \
    'REST API: getUsers, page=1, size=2'

assert_json "受保护 User 路径无 Token 返回 401" \
    "http://localhost:$GATEWAY_PORT/api/users/1" \
    '.code == "UNAUTHORIZED" and .message == "Missing authentication token"' GET '' 401
assert_json "受保护 Order 路径无 Token 返回 401" \
    "http://localhost:$GATEWAY_PORT/api/orders/1" \
    '.code == "UNAUTHORIZED" and .message == "Missing authentication token"' GET '' 401

TOKEN=$(create_jwt)
assert_json "有效 JWT 代理查询 User" \
    "http://localhost:$GATEWAY_PORT/api/users/1" \
    '.user.id == 1 and .user.username == "user1"' GET '' 200 "Authorization: Bearer $TOKEN"

ORDER_JSON='{"userId":1,"productName":"nebula_e2e_gateway_product","quantity":2,"price":12.50}'
assert_json "有效 JWT 代理创建 Order" \
    "http://localhost:$GATEWAY_PORT/api/orders" \
    '.orderId > 0 and .totalAmount == 25.00' POST "$ORDER_JSON" 200 "Authorization: Bearer $TOKEN"
assert_file_contains "Order 后端收到 Gateway 创建请求" "$ORDER_LOG" \
    'RPC服务端: createOrder, userId=1, productName=nebula_e2e_gateway_product'
assert_file_contains "Order 通过 Nacos 和 gRPC 调用 User" "$ORDER_LOG" \
    'gRPC 目标地址: .*:2001'

RATE_200=0
RATE_429=0
for _ in 1 2 3 4 5 6 7 8; do
    perform_request GET "http://localhost:$GATEWAY_PORT/api/users" "" || true
    case "$HTTP_STATUS" in
        200) RATE_200=$((RATE_200 + 1)) ;;
        429) RATE_429=$((RATE_429 + 1)) ;;
        *) record_fail "限流压测出现意外 HTTP $HTTP_STATUS" ;;
    esac
done
if [ "$RATE_429" -gt 0 ]; then
    record_pass "连续请求触发 429 限流，次数=$RATE_429"
else
    record_fail "连续请求未触发 429 限流"
fi

sleep 2
assert_json "等待补充令牌后恢复 200" "http://localhost:$GATEWAY_PORT/api/users" \
    '.total == 10 and (.users | length) > 0'

stop_all_apps true
for port in "$USER_GRPC_PORT" "$ORDER_GRPC_PORT"; do
    if wait_for_port_release "$port" 15; then
        record_pass "gRPC 端口 $port 已释放"
    else
        record_fail "gRPC 端口 $port 未释放"
    fi
done

cleanup_rate_keys
if [ -z "$(rate_keys)" ]; then
    record_pass "Redis 限流键已清理"
    RATE_KEYS_CLEANED=1
else
    record_fail "Redis 限流键清理不完整"
fi

print_summary gateway-example
