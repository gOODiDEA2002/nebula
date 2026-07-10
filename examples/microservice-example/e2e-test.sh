#!/usr/bin/env bash
# microservice-example E2E 测试
# 验证 User、Order 的 REST、HTTP RPC、gRPC、Nacos 发现和跨服务调用。
source "$(dirname "$0")/../e2e-common.sh"

USER_HTTP_PORT=${USER_HTTP_PORT:-1001}
ORDER_HTTP_PORT=${ORDER_HTTP_PORT:-1002}
USER_GRPC_PORT=${USER_GRPC_PORT:-2001}
ORDER_GRPC_PORT=${ORDER_GRPC_PORT:-2002}
NACOS_HOST="${NEBULA_E2E_NACOS_HOST:-localhost}"
NACOS_PORT="${NEBULA_E2E_NACOS_PORT:-8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
NACOS_BASE_URL="http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1"
USER_SERVICE_NAME=nebula-example-user-service
ORDER_SERVICE_NAME=nebula-example-order-service
RESOURCE_SUFFIX="${E2E_RUN_ID//-/_}"
SHORT_SUFFIX=$(printf '%s' "$E2E_RUN_ID" | shasum | cut -c1-8)
NACOS_TOKEN=""
GRPC_REQUEST_COUNT=0

export USER_HTTP_PORT ORDER_HTTP_PORT USER_GRPC_PORT ORDER_GRPC_PORT
export NACOS_SERVER_ADDR="${NACOS_HOST}:${NACOS_PORT}"
export NACOS_USERNAME NACOS_PASSWORD NACOS_GROUP
export APP_ENV=e2e

nacos_login() {
    local response
    response=$(curl --silent --show-error --noproxy '*' --connect-timeout 3 --max-time 10 \
        -X POST "$NACOS_BASE_URL/auth/login" \
        --data-urlencode "username=$NACOS_USERNAME" \
        --data-urlencode "password=$NACOS_PASSWORD") || return 1
    NACOS_TOKEN=$(printf '%s' "$response" | jq -r '.accessToken // empty')
    [ -n "$NACOS_TOKEN" ]
}

nacos_instance_matches() {
    local service_name=$1
    local http_port=$2
    local grpc_port=$3
    local response
    response=$(curl --silent --show-error --noproxy '*' -G "$NACOS_BASE_URL/ns/instance/list" \
        --data-urlencode "serviceName=$service_name" \
        --data-urlencode "groupName=$NACOS_GROUP" \
        --data-urlencode "accessToken=$NACOS_TOKEN") || return 1
    printf '%s' "$response" | jq -e --argjson httpPort "$http_port" --arg grpcPort "$grpc_port" \
        'any(.hosts[]?; .port == $httpPort and .healthy == true and .enabled == true
            and .metadata.grpcPort == $grpcPort and .metadata.env == "e2e")' >/dev/null
}

nacos_instance_absent() {
    local service_name=$1
    local http_port=$2
    local response
    response=$(curl --silent --show-error --noproxy '*' -G "$NACOS_BASE_URL/ns/instance/list" \
        --data-urlencode "serviceName=$service_name" \
        --data-urlencode "groupName=$NACOS_GROUP" \
        --data-urlencode "accessToken=$NACOS_TOKEN") || return 1
    printf '%s' "$response" | jq -e --argjson port "$http_port" \
        'all(.hosts[]?; .port != $port)' >/dev/null
}

assert_grpc_json() {
    local desc=$1
    local port=$2
    local payload=$3
    local jq_filter=$4
    local body_file
    GRPC_REQUEST_COUNT=$((GRPC_REQUEST_COUNT + 1))
    body_file="$E2E_CASE_DIR/grpc-$(printf '%04d' "$GRPC_REQUEST_COUNT").json"
    TOTAL=$((TOTAL + 1))

    if grpcurl -plaintext -d "$payload" "localhost:$port" \
        io.nebula.rpc.grpc.GenericRpcService/Call >"$body_file" 2>&1 &&
        jq -e "$jq_filter" "$body_file" >/dev/null 2>&1; then
        PASS=$((PASS + 1))
        log_pass "${desc}，gRPC JSON 断言通过"
        return 0
    fi

    FAIL=$((FAIL + 1))
    log_fail "${desc}，gRPC 响应不满足「${jq_filter}」"
    log_fail "响应证据：$body_file"
    return 0
}

log_info "========== microservice-example E2E =========="

skip_if_no_service Nacos "$NACOS_HOST" "$NACOS_PORT" microservice-example
if ! require_command grpcurl; then
    if [ "$E2E_MODE" = "full" ]; then
        BLOCKED=$((BLOCKED + 1))
        TOTAL=$((TOTAL + 1))
        print_summary microservice-example
        exit $?
    fi
    skip_test "microservice-example gRPC" "缺少 grpcurl"
    print_summary microservice-example
    exit $?
fi
if nacos_login; then
    record_pass "Nacos 认证 API 登录成功"
else
    BLOCKED=$((BLOCKED + 1))
    TOTAL=$((TOTAL + 1))
    log_fail "Nacos 认证 API 登录失败"
    print_summary microservice-example
    exit $?
fi

for port in "$USER_GRPC_PORT" "$ORDER_GRPC_PORT"; do
    if ! ensure_port_available "$port"; then
        record_fail "gRPC 端口 $port 已被占用"
        print_summary microservice-example
        exit $?
    fi
done

if mvn -q -f examples/microservice-example/user-api install -DskipTests; then
    record_pass "user-api 契约模块安装成功"
else
    record_fail "user-api 契约模块安装失败"
    print_summary microservice-example
    exit $?
fi
if mvn -q -f examples/microservice-example/order-api install -DskipTests; then
    record_pass "order-api 契约模块安装成功"
else
    record_fail "order-api 契约模块安装失败"
    print_summary microservice-example
    exit $?
fi

assert_archive_contains "user-api JAR 包含 UserRpcClient" \
    "examples/microservice-example/user-api/target/nebula-example-user-api-2.1.0-SNAPSHOT.jar" \
    "io/nebula/example/user/api/rpc/UserRpcClient.class"
assert_archive_contains "order-api JAR 包含 OrderRpcClient" \
    "examples/microservice-example/order-api/target/nebula-example-order-api-2.1.0-SNAPSHOT.jar" \
    "io/nebula/example/order/api/rpc/OrderRpcClient.class"

start_app "examples/microservice-example/user-service" "$USER_HTTP_PORT"
USER_PID=$APP_PID
USER_LOG=$CURRENT_APP_LOG
start_app "examples/microservice-example/order-service" "$ORDER_HTTP_PORT"
ORDER_PID=$APP_PID
ORDER_LOG=$CURRENT_APP_LOG

assert_json "User 健康检查" "http://localhost:$USER_HTTP_PORT/health/ping" \
    '.success == true and .data.status == "pong"'
assert_json "Order 健康检查" "http://localhost:$ORDER_HTTP_PORT/health/ping" \
    '.success == true and .data.status == "pong"'

if wait_until "Nacos 注册 User HTTP/gRPC metadata" 20 1 \
    nacos_instance_matches "$USER_SERVICE_NAME" "$USER_HTTP_PORT" "$USER_GRPC_PORT"; then
    record_pass "Nacos 注册 User HTTP/gRPC metadata"
else
    record_fail "Nacos 未注册 User HTTP/gRPC metadata"
fi
if wait_until "Nacos 注册 Order HTTP/gRPC metadata" 20 1 \
    nacos_instance_matches "$ORDER_SERVICE_NAME" "$ORDER_HTTP_PORT" "$ORDER_GRPC_PORT"; then
    record_pass "Nacos 注册 Order HTTP/gRPC metadata"
else
    record_fail "Nacos 未注册 Order HTTP/gRPC metadata"
fi

USERNAME="neb_e2e_${SHORT_SUFFIX}"
CREATE_USER_JSON=$(jq -cn --arg username "$USERNAME" \
    '{username:$username,name:"Nebula E2E User",email:"nebula_e2e@example.com",phone:"13900000001",status:"ACTIVE"}')
assert_json "REST 创建用户" "http://localhost:$USER_HTTP_PORT/rpc/users" \
    '.id | type == "number" and . > 10' POST "$CREATE_USER_JSON"
USER_ID=$(jq -r '.id // 0' "$HTTP_BODY_FILE")

assert_json "REST 查询新用户" "http://localhost:$USER_HTTP_PORT/rpc/users/$USER_ID" \
    ".user.id == $USER_ID and .user.username == \"$USERNAME\" and .user.status == \"ACTIVE\""
assert_json "REST 列表筛选新用户" \
    "http://localhost:$USER_HTTP_PORT/rpc/users?username=$USERNAME&page=1&size=10" \
    ".total == 1 and (.users | length) == 1 and .users[0].id == $USER_ID"

UPDATE_USER_JSON='{"name":"Nebula E2E Updated","email":"nebula_e2e_updated@example.com","status":"LOCKED"}'
assert_json "REST 更新用户" "http://localhost:$USER_HTTP_PORT/rpc/users/$USER_ID" \
    ".user.id == $USER_ID and .user.name == \"Nebula E2E Updated\" and .user.status == \"LOCKED\"" \
    PUT "$UPDATE_USER_JSON"
assert_status "REST 非法用户输入返回 400" "http://localhost:$USER_HTTP_PORT/rpc/users" 400 POST \
    '{"username":"x","name":"","email":"invalid"}'

HTTP_RPC_REQUEST_ID="nebula_e2e_http_${RESOURCE_SUFFIX}"
HTTP_RPC_PAYLOAD=$(jq -cn --arg requestId "$HTTP_RPC_REQUEST_ID" --argjson userId "$USER_ID" \
    '{requestId:$requestId,serviceName:"io.nebula.example.user.api.rpc.UserRpcClient",
      methodName:"getUserById",parameterTypes:["java.lang.Long"],parameters:[$userId],headers:{},
      timestamp:(now|floor),timeout:30000,version:"2.1"}')
assert_json "HTTP RPC 查询用户成功" "http://localhost:$USER_HTTP_PORT/rpc" \
    ".requestId == \"$HTTP_RPC_REQUEST_ID\" and .statusCode == 200 and .result.user.id == $USER_ID" \
    POST "$HTTP_RPC_PAYLOAD"

HTTP_RPC_ERROR_ID="nebula_e2e_http_error_${RESOURCE_SUFFIX}"
HTTP_RPC_ERROR=$(jq -cn --arg requestId "$HTTP_RPC_ERROR_ID" \
    '{requestId:$requestId,serviceName:"io.nebula.e2e.MissingService",methodName:"missing",
      parameterTypes:[],parameters:[],headers:{},timestamp:(now|floor),timeout:30000,version:"2.1"}')
assert_json "HTTP RPC 未知服务返回 404" "http://localhost:$USER_HTTP_PORT/rpc" \
    ".requestId == \"$HTTP_RPC_ERROR_ID\" and .statusCode == 500 and (.message | contains(\"服务未找到\"))" \
    POST "$HTTP_RPC_ERROR" 404

GRPC_USER_ID="nebula_e2e_grpc_user_${RESOURCE_SUFFIX}"
GRPC_USER_PAYLOAD=$(jq -cn --arg requestId "$GRPC_USER_ID" --arg userId "$USER_ID" \
    '{requestId:$requestId,serviceName:"io.nebula.example.user.api.rpc.UserRpcClient",
      methodName:"getUserById",parameterTypes:["java.lang.Long"],parameters:[$userId],timestamp:now|floor}')
assert_grpc_json "gRPC 查询用户成功" "$USER_GRPC_PORT" "$GRPC_USER_PAYLOAD" \
    ".request_id == \"$GRPC_USER_ID\" and .success == true and ((.result | fromjson).user.id == $USER_ID)"

GRPC_ERROR_ID="nebula_e2e_grpc_error_${RESOURCE_SUFFIX}"
GRPC_ERROR_PAYLOAD=$(jq -cn --arg requestId "$GRPC_ERROR_ID" \
    '{requestId:$requestId,serviceName:"io.nebula.example.user.api.rpc.UserRpcClient",
      methodName:"missingMethod",parameterTypes:[],parameters:[],timestamp:now|floor}')
assert_grpc_json "gRPC 错误方法返回业务错误" "$USER_GRPC_PORT" "$GRPC_ERROR_PAYLOAD" \
    ".request_id == \"$GRPC_ERROR_ID\" and (.success // false) == false
      and .error_code == \"RPC_CALL_ERROR\" and (.error_message | contains(\"方法未找到\"))"

PRODUCT_NAME="nebula_e2e_product_${RESOURCE_SUFFIX}"
ORDER_PARAM=$(jq -cn --argjson userId "$USER_ID" --arg product "$PRODUCT_NAME" \
    '{userId:$userId,productName:$product,quantity:2,price:19.95}')
ORDER_HTTP_ID="nebula_e2e_order_http_${RESOURCE_SUFFIX}"
ORDER_HTTP_PAYLOAD=$(jq -cn --arg requestId "$ORDER_HTTP_ID" --arg parameter "$ORDER_PARAM" \
    '{requestId:$requestId,serviceName:"io.nebula.example.order.api.rpc.OrderRpcClient",
      methodName:"createOrder",parameterTypes:["io.nebula.example.order.api.dto.CreateOrderDto$Request"],
      parameters:[($parameter | fromjson)],headers:{},timestamp:(now|floor),timeout:30000,version:"2.1"}')
assert_json "Order HTTP RPC 跨服务创建订单" "http://localhost:$ORDER_HTTP_PORT/rpc" \
    ".requestId == \"$ORDER_HTTP_ID\" and .statusCode == 200 and .result.orderId > 0 and .result.totalAmount == 39.90" \
    POST "$ORDER_HTTP_PAYLOAD"
ORDER_ID=$(jq -r '.result.orderId // 0' "$HTTP_BODY_FILE")

ORDER_GRPC_ID="nebula_e2e_order_grpc_${RESOURCE_SUFFIX}"
ORDER_GRPC_PAYLOAD=$(jq -cn --arg requestId "$ORDER_GRPC_ID" --arg orderId "$ORDER_ID" \
    '{requestId:$requestId,serviceName:"io.nebula.example.order.api.rpc.OrderRpcClient",
      methodName:"getOrderById",parameterTypes:["java.lang.Long"],parameters:[$orderId],timestamp:now|floor}')
assert_grpc_json "Order gRPC 查询订单成功" "$ORDER_GRPC_PORT" "$ORDER_GRPC_PAYLOAD" \
    ".request_id == \"$ORDER_GRPC_ID\" and .success == true
      and ((.result | fromjson).order.id == $ORDER_ID)
      and ((.result | fromjson).order.productName == \"$PRODUCT_NAME\")"

MISSING_ORDER_PARAM=$(jq -cn --arg product "nebula_e2e_missing_${RESOURCE_SUFFIX}" \
    '{userId:999999,productName:$product,quantity:1,price:1.00}')
ORDER_ERROR_ID="nebula_e2e_order_error_${RESOURCE_SUFFIX}"
ORDER_ERROR_PAYLOAD=$(jq -cn --arg requestId "$ORDER_ERROR_ID" --arg parameter "$MISSING_ORDER_PARAM" \
    '{requestId:$requestId,serviceName:"io.nebula.example.order.api.rpc.OrderRpcClient",
      methodName:"createOrder",parameterTypes:["io.nebula.example.order.api.dto.CreateOrderDto$Request"],
      parameters:[$parameter],timestamp:now|floor}')
assert_grpc_json "Order gRPC 错误用户返回业务错误" "$ORDER_GRPC_PORT" "$ORDER_ERROR_PAYLOAD" \
    ".request_id == \"$ORDER_ERROR_ID\" and (.success // false) == false
      and .error_code == \"RPC_CALL_ERROR\" and (.error_message | contains(\"用户不存在\"))"

assert_file_contains "Order 通过发现客户端选择 User gRPC metadata" "$ORDER_LOG" \
    "gRPC 目标地址: .*:$USER_GRPC_PORT"
assert_file_contains "Order 日志记录跨服务用户验证" "$ORDER_LOG" \
    "调用 UserService 验证用户: userId=$USER_ID"
assert_file_contains "User 收到 Order 发起的 gRPC 请求" "$USER_LOG" \
    "收到 gRPC RPC 请求: .*service=io.nebula.example.user.api.rpc.UserRpcClient, method=getUserById"

assert_json "REST 删除测试用户" "http://localhost:$USER_HTTP_PORT/rpc/users/$USER_ID" \
    '.success == true' DELETE
assert_json "REST 删除后用户不存在" "http://localhost:$USER_HTTP_PORT/rpc/users/$USER_ID" \
    '.user == null'

stop_all_apps true
for port in "$USER_GRPC_PORT" "$ORDER_GRPC_PORT"; do
    if wait_for_port_release "$port" 15; then
        record_pass "gRPC 端口 $port 已释放"
    else
        record_fail "gRPC 端口 $port 未释放"
    fi
done
if wait_until "User 实例从 Nacos 注销" 20 1 \
    nacos_instance_absent "$USER_SERVICE_NAME" "$USER_HTTP_PORT"; then
    record_pass "User 实例已从 Nacos 注销"
else
    record_fail "User 实例未从 Nacos 注销"
fi
if wait_until "Order 实例从 Nacos 注销" 20 1 \
    nacos_instance_absent "$ORDER_SERVICE_NAME" "$ORDER_HTTP_PORT"; then
    record_pass "Order 实例已从 Nacos 注销"
else
    record_fail "Order 实例未从 Nacos 注销"
fi

print_summary microservice-example
