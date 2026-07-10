#!/usr/bin/env bash
# rpc-async-example E2E 测试
# 验证 Nacos 服务发现、异步状态持久化、取消、重启恢复和资源清理。
source "$(dirname "$0")/../e2e-common.sh"

SERVICE_PORT=8081
CLIENT_PORT=8082
NACOS_HOST="${NEBULA_E2E_NACOS_HOST:-localhost}"
NACOS_PORT="${NEBULA_E2E_NACOS_PORT:-8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
NACOS_BASE_URL="http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1"
RESOURCE_SUFFIX="${E2E_RUN_ID//-/_}"
CLIENT_APP_NAME="nebula-e2e-async-rpc-client-${E2E_RUN_ID}"
ASYNC_CONFIG_GROUP="ASYNC_RPC_${CLIENT_APP_NAME}"
NACOS_TOKEN=""
NACOS_RECORDS_CLEANED=0
EXECUTION_IDS=()
LAST_EXECUTION_ID=""

export CLIENT_APP_NAME
export NACOS_SERVER_ADDR="${NACOS_HOST}:${NACOS_PORT}"
export NACOS_USERNAME
export NACOS_PASSWORD
export NACOS_GROUP
export ASYNC_CORE_POOL_SIZE=1
export ASYNC_MAX_POOL_SIZE=1
export ASYNC_QUEUE_CAPACITY=20

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
    local port=$2
    local response
    response=$(curl --silent --show-error --noproxy '*' -G "$NACOS_BASE_URL/ns/instance/list" \
        --data-urlencode "serviceName=$service_name" \
        --data-urlencode "groupName=$NACOS_GROUP" \
        --data-urlencode "accessToken=$NACOS_TOKEN") || return 1
    printf '%s' "$response" | jq -e --argjson port "$port" \
        'any(.hosts[]?; .port == $port and .healthy == true and .enabled == true)' >/dev/null
}

nacos_instance_absent() {
    local service_name=$1
    local port=$2
    local response
    response=$(curl --silent --show-error --noproxy '*' -G "$NACOS_BASE_URL/ns/instance/list" \
        --data-urlencode "serviceName=$service_name" \
        --data-urlencode "groupName=$NACOS_GROUP" \
        --data-urlencode "accessToken=$NACOS_TOKEN") || return 1
    printf '%s' "$response" | jq -e --argjson port "$port" \
        'all(.hosts[]?; .port != $port)' >/dev/null
}

nacos_execution_status_is() {
    local execution_id=$1
    local expected_status=$2
    local content
    content=$(curl --silent --show-error --noproxy '*' -G "$NACOS_BASE_URL/cs/configs" \
        --data-urlencode "dataId=execution_${execution_id}" \
        --data-urlencode "group=$ASYNC_CONFIG_GROUP" \
        --data-urlencode "accessToken=$NACOS_TOKEN") || return 1
    printf '%s' "$content" | jq -e --arg status "$expected_status" \
        '.status == $status and .executionId != null' >/dev/null
}

nacos_execution_absent() {
    local execution_id=$1
    local status
    status=$(curl --silent --show-error --noproxy '*' --connect-timeout 3 --max-time 10 \
        -o /dev/null -w '%{http_code}' -G "$NACOS_BASE_URL/cs/configs" \
        --data-urlencode "dataId=execution_${execution_id}" \
        --data-urlencode "group=$ASYNC_CONFIG_GROUP" \
        --data-urlencode "accessToken=$NACOS_TOKEN") || status=000
    [ "$status" = "404" ]
}

execution_status_is() {
    local execution_id=$1
    local expected_status=$2
    perform_request GET "http://localhost:$CLIENT_PORT/api/tasks/status/$execution_id" "" || true
    [ "$HTTP_STATUS" = "200" ] &&
        jq -e --arg status "$expected_status" '.status == $status' "$HTTP_BODY_FILE" >/dev/null 2>&1
}

submit_task() {
    local desc=$1
    local url=$2
    local data=$3
    LAST_EXECUTION_ID=""
    perform_request POST "$url" "$data" || true
    if [ "$HTTP_STATUS" = "202" ] &&
        jq -e '.executionId | type == "string" and length > 0' "$HTTP_BODY_FILE" >/dev/null 2>&1; then
        LAST_EXECUTION_ID=$(jq -r '.executionId' "$HTTP_BODY_FILE")
        EXECUTION_IDS+=("$LAST_EXECUTION_ID")
        record_pass "${desc}，HTTP 202，返回 executionId"
        return 0
    fi
    record_fail "${desc}，期望 HTTP 202 和 executionId，实际 HTTP $HTTP_STATUS"
    print_summary "rpc-async-example"
    exit 1
}

wait_for_execution_status() {
    local desc=$1
    local execution_id=$2
    local expected_status=$3
    local timeout=${4:-30}
    if wait_until "$desc" "$timeout" 1 execution_status_is "$execution_id" "$expected_status"; then
        record_pass "$desc"
    else
        record_fail "$desc"
    fi
}

delete_nacos_records_best_effort() {
    local execution_id
    for execution_id in "${EXECUTION_IDS[@]}"; do
        curl --silent --show-error --noproxy '*' --connect-timeout 3 --max-time 10 \
            -X DELETE "$NACOS_BASE_URL/cs/configs" \
            --data-urlencode "dataId=execution_${execution_id}" \
            --data-urlencode "group=$ASYNC_CONFIG_GROUP" \
            --data-urlencode "accessToken=$NACOS_TOKEN" >/dev/null 2>&1 || true
    done
}

delete_nacos_records_and_verify() {
    local execution_id
    local ok=true
    local status
    local body_file
    for execution_id in "${EXECUTION_IDS[@]}"; do
        body_file="$E2E_CASE_DIR/nacos-delete-${execution_id}.body"
        status=$(curl --silent --show-error --noproxy '*' --connect-timeout 3 --max-time 10 \
            -o "$body_file" -w '%{http_code}' -X DELETE "$NACOS_BASE_URL/cs/configs" \
            --data-urlencode "dataId=execution_${execution_id}" \
            --data-urlencode "group=$ASYNC_CONFIG_GROUP" \
            --data-urlencode "accessToken=$NACOS_TOKEN") || status=000
        if [ "$status" != "200" ] || [ "$(cat "$body_file")" != "true" ]; then
            ok=false
        fi
        if ! wait_until "Nacos 删除 execution_${execution_id}" 10 1 \
            nacos_execution_absent "$execution_id"; then
            ok=false
        fi
    done
    if [ "$ok" = true ]; then
        record_pass "Nacos 异步执行记录全部删除并复核不存在"
        NACOS_RECORDS_CLEANED=1
    else
        record_fail "Nacos 异步执行记录清理不完整"
    fi
}

cleanup_rpc_async() {
    local exit_code=$?
    trap - EXIT
    set +e
    stop_all_apps false
    if [ "$NACOS_RECORDS_CLEANED" -eq 0 ] && [ -n "$NACOS_TOKEN" ]; then
        delete_nacos_records_best_effort
    fi
    cleanup "$exit_code"
    exit "$exit_code"
}

trap cleanup_rpc_async EXIT

log_info "========== rpc-async-example E2E =========="

skip_if_no_service Nacos "$NACOS_HOST" "$NACOS_PORT" "rpc-async-example"
if nacos_login; then
    record_pass "Nacos 认证 API 登录成功"
else
    if [ "$E2E_MODE" = "full" ]; then
        BLOCKED=$((BLOCKED + 1))
        TOTAL=$((TOTAL + 1))
        log_fail "Nacos 认证 API 登录失败"
    else
        skip_test "Nacos 协议验证" "认证 API 登录失败"
    fi
    print_summary "rpc-async-example"
    exit $?
fi

TOTAL=$((TOTAL + 1))
if mvn -q -f examples/rpc-async-example/api install -DskipTests; then
    PASS=$((PASS + 1))
    log_pass "rpc-async-example API 模块安装成功"
else
    FAIL=$((FAIL + 1))
    log_fail "rpc-async-example API 模块安装失败"
    print_summary "rpc-async-example"
    exit $?
fi

start_app "examples/rpc-async-example/service" "$SERVICE_PORT"
SERVICE_PID=$APP_PID
SERVICE_LOG=$CURRENT_APP_LOG
start_app "examples/rpc-async-example/client" "$CLIENT_PORT"
CLIENT_PID=$APP_PID
FIRST_CLIENT_LOG=$CURRENT_APP_LOG

assert_json "Service 健康检查" \
    "http://localhost:$SERVICE_PORT/health/ping" \
    '.success == true and .data.status == "pong"'
assert_json "Client 健康检查" \
    "http://localhost:$CLIENT_PORT/health/ping" \
    '.success == true and .data.status == "pong"'

if wait_until "Nacos 注册 data-process-service:8081" 20 1 \
    nacos_instance_matches data-process-service "$SERVICE_PORT"; then
    record_pass "Nacos 注册 data-process-service:8081"
else
    record_fail "Nacos 未注册 data-process-service:8081"
fi
if wait_until "Nacos 注册 ${CLIENT_APP_NAME}:8082" 20 1 \
    nacos_instance_matches "$CLIENT_APP_NAME" "$CLIENT_PORT"; then
    record_pass "Nacos 注册 ${CLIENT_APP_NAME}:8082"
else
    record_fail "Nacos 未注册 ${CLIENT_APP_NAME}:8082"
fi

PRIMARY_TASK_ID="nebula_e2e_async_primary_${RESOURCE_SUFFIX}"
PRIMARY_JSON=$(jq -cn --arg taskId "$PRIMARY_TASK_ID" \
    '{taskId:$taskId,dataSource:"nebula_e2e",type:"DATA_IMPORT",delaySeconds:2,params:{scope:"e2e"}}')
submit_task "提交单条异步任务" \
    "http://localhost:$CLIENT_PORT/api/tasks/async" "$PRIMARY_JSON"
PRIMARY_EXECUTION_ID=$LAST_EXECUTION_ID

assert_json "异步任务首先处于非终态" \
    "http://localhost:$CLIENT_PORT/api/tasks/status/$PRIMARY_EXECUTION_ID" \
    '.status == "PENDING" or .status == "RUNNING"'
wait_for_execution_status "异步任务最终进入 SUCCESS" "$PRIMARY_EXECUTION_ID" SUCCESS 30
assert_json "异步任务返回真实服务端结果" \
    "http://localhost:$CLIENT_PORT/api/tasks/result/$PRIMARY_EXECUTION_ID" \
    ".taskId == \"$PRIMARY_TASK_ID\" and .success == true and .data.processType == \"DATA_IMPORT\""
if nacos_execution_status_is "$PRIMARY_EXECUTION_ID" SUCCESS; then
    record_pass "Nacos 配置中心保存 SUCCESS 执行记录"
else
    record_fail "Nacos 配置中心没有 SUCCESS 执行记录"
fi

BATCH_TASK_1="nebula_e2e_async_batch_1_${RESOURCE_SUFFIX}"
BATCH_TASK_2="nebula_e2e_async_batch_2_${RESOURCE_SUFFIX}"
BATCH_JSON=$(jq -cn --arg first "$BATCH_TASK_1" --arg second "$BATCH_TASK_2" \
    '{requests:[
        {taskId:$first,dataSource:"nebula_e2e",type:"DATA_EXPORT",delaySeconds:0},
        {taskId:$second,dataSource:"nebula_e2e",type:"DATA_CLEANING",delaySeconds:0}
    ]}')
submit_task "提交批量异步任务" \
    "http://localhost:$CLIENT_PORT/api/tasks/batch" "$BATCH_JSON"
BATCH_EXECUTION_ID=$LAST_EXECUTION_ID
wait_for_execution_status "批量异步任务最终进入 SUCCESS" "$BATCH_EXECUTION_ID" SUCCESS 30
assert_json "批量结果包含两条真实结果" \
    "http://localhost:$CLIENT_PORT/api/tasks/result/batch/$BATCH_EXECUTION_ID" \
    "length == 2 and .[0].taskId == \"$BATCH_TASK_1\" and .[1].taskId == \"$BATCH_TASK_2\" and all(.[]; .success == true)"

SYNC_TASK_ID="nebula_e2e_async_sync_${RESOURCE_SUFFIX}"
SYNC_JSON=$(jq -cn --arg taskId "$SYNC_TASK_ID" \
    '{taskId:$taskId,dataSource:"nebula_e2e",type:"DATA_TRANSFORM",delaySeconds:0}')
assert_json "同步 RPC 返回服务端结果" \
    "http://localhost:$CLIENT_PORT/api/tasks/sync" \
    ".taskId == \"$SYNC_TASK_ID\" and .success == true and .data.processType == \"DATA_TRANSFORM\"" \
    POST "$SYNC_JSON"

MISSING_ID="nebula_e2e_missing_${RESOURCE_SUFFIX}"
assert_status "不存在的执行状态返回 404" \
    "http://localhost:$CLIENT_PORT/api/tasks/status/$MISSING_ID" 404
assert_status "不存在的执行结果返回 404" \
    "http://localhost:$CLIENT_PORT/api/tasks/result/$MISSING_ID" 404

BLOCKER_TASK_ID="nebula_e2e_async_blocker_${RESOURCE_SUFFIX}"
BLOCKER_JSON=$(jq -cn --arg taskId "$BLOCKER_TASK_ID" \
    '{taskId:$taskId,dataSource:"nebula_e2e",type:"REPORT_GENERATION",delaySeconds:6}')
submit_task "提交占用单线程执行器的任务" \
    "http://localhost:$CLIENT_PORT/api/tasks/async" "$BLOCKER_JSON"
BLOCKER_EXECUTION_ID=$LAST_EXECUTION_ID

CANCEL_TASK_ID="nebula_e2e_async_cancel_${RESOURCE_SUFFIX}"
CANCEL_JSON=$(jq -cn --arg taskId "$CANCEL_TASK_ID" \
    '{taskId:$taskId,dataSource:"nebula_e2e",type:"DATA_EXPORT",delaySeconds:0}')
submit_task "提交排队等待取消的任务" \
    "http://localhost:$CLIENT_PORT/api/tasks/async" "$CANCEL_JSON"
CANCEL_EXECUTION_ID=$LAST_EXECUTION_ID
wait_for_execution_status "排队任务状态可读取为 PENDING" "$CANCEL_EXECUTION_ID" PENDING 10
assert_json "PENDING 任务可以取消" \
    "http://localhost:$CLIENT_PORT/api/tasks/$CANCEL_EXECUTION_ID" \
    '.cancelled == true' DELETE
wait_for_execution_status "取消后状态为 CANCELLED" "$CANCEL_EXECUTION_ID" CANCELLED 10
wait_for_execution_status "占位任务最终进入 SUCCESS" "$BLOCKER_EXECUTION_ID" SUCCESS 20
sleep 1
assert_json "排队任务不会在取消后继续执行" \
    "http://localhost:$CLIENT_PORT/api/tasks/status/$CANCEL_EXECUTION_ID" \
    '.status == "CANCELLED"'
assert_file_contains "客户端明确跳过已取消执行" "$FIRST_CLIENT_LOG" \
    "跳过已取消执行: executionId=$CANCEL_EXECUTION_ID"
assert_file_not_contains "服务端没有收到已取消任务" "$SERVICE_LOG" \
    "$CANCEL_TASK_ID"

stop_app "$CLIENT_PID"
if wait_for_port_release "$CLIENT_PORT" 15; then
    record_pass "首次 Client 停止后 8082 端口释放"
else
    record_fail "首次 Client 停止后 8082 端口未释放"
fi
start_app "examples/rpc-async-example/client" "$CLIENT_PORT"

assert_json "Client 重启后仍能读取 SUCCESS 状态" \
    "http://localhost:$CLIENT_PORT/api/tasks/status/$PRIMARY_EXECUTION_ID" \
    '.status == "SUCCESS"'
assert_json "Client 重启后仍能读取持久化结果" \
    "http://localhost:$CLIENT_PORT/api/tasks/result/$PRIMARY_EXECUTION_ID" \
    ".taskId == \"$PRIMARY_TASK_ID\" and .success == true"

stop_all_apps true
delete_nacos_records_and_verify

if wait_until "data-process-service:8081 从 Nacos 注销" 20 1 \
    nacos_instance_absent data-process-service "$SERVICE_PORT"; then
    record_pass "data-process-service:8081 已从 Nacos 注销"
else
    record_fail "data-process-service:8081 未从 Nacos 注销"
fi
if wait_until "${CLIENT_APP_NAME}:8082 从 Nacos 注销" 20 1 \
    nacos_instance_absent "$CLIENT_APP_NAME" "$CLIENT_PORT"; then
    record_pass "${CLIENT_APP_NAME}:8082 已从 Nacos 注销"
else
    record_fail "${CLIENT_APP_NAME}:8082 未从 Nacos 注销"
fi

print_summary "rpc-async-example"
