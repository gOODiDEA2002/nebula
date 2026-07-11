#!/usr/bin/env bash
# fullstack-example E2E 测试
# Task 9：验证默认、读写分离、分片和组合数据模式，以及多级缓存。
source "$(dirname "$0")/../e2e-common.sh"

PORT=${FULLSTACK_PORT:-1000}
MYSQL_HOST=127.0.0.1
MYSQL_PORT=${NEBULA_E2E_MYSQL_PORT:-13306}
REDIS_HOST=${NEBULA_E2E_REDIS_HOST:-127.0.0.1}
REDIS_PORT=${NEBULA_E2E_REDIS_PORT:-6379}
REDIS_DB=14
COMPOSE_FILE="$PROJECT_ROOT/docker/verification/docker-compose.yml"
COMPOSE_PROJECT="nebula-e2e-fullstack-$$"
CACHE_PREFIX="nebula:e2e:${E2E_RUN_ID}:"
MYSQL_STARTED=0
REDIS_CLEANED=0

export FULLSTACK_PORT="$PORT" MYSQL_HOST MYSQL_PORT MYSQL_USERNAME=root MYSQL_PASSWORD=''
export REDIS_HOST REDIS_PORT REDIS_PASSWORD='' FULLSTACK_REDIS_DATABASE="$REDIS_DB"
export FULLSTACK_CACHE_PREFIX="$CACHE_PREFIX"
export NEBULA_DISCOVERY_NACOS_ENABLED=false
export NEBULA_RPC_HTTP_ENABLED=false NEBULA_RPC_GRPC_ENABLED=false NEBULA_RPC_DISCOVERY_ENABLED=false
export NEBULA_MESSAGING_RABBITMQ_ENABLED=false NEBULA_AI_ENABLED=false
export NEBULA_SEARCH_ELASTICSEARCH_ENABLED=false NEBULA_STORAGE_MINIO_ENABLED=false
export NEBULA_TASK_ENABLED=false NEBULA_LOCK_ENABLED=false NEBULA_WEBSOCKET_ENABLED=false

mysql_exec() {
    docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" exec -T mysql mysql -uroot "$@"
}

redis_keys() {
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB" --scan --pattern "${CACHE_PREFIX}*"
}

cleanup_redis() {
    local key
    while IFS= read -r key; do
        [ -n "$key" ] || continue
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB" DEL "$key" >/dev/null 2>&1 || true
    done < <(redis_keys 2>/dev/null || true)
}

cleanup_fullstack() {
    local exit_code=$?
    trap - EXIT
    set +e
    stop_all_apps false
    if [ "$REDIS_CLEANED" -eq 0 ]; then
        cleanup_redis
    fi
    if [ "$MYSQL_STARTED" -eq 1 ]; then
        docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" down -v --remove-orphans >/dev/null 2>&1 || true
    fi
    cleanup "$exit_code"
    exit "$exit_code"
}

trap cleanup_fullstack EXIT

start_profile() {
    local profile=$1
    export SPRING_PROFILES_ACTIVE="$profile"
    start_app "examples/fullstack-example" "$PORT"
    PROFILE_LOG=$CURRENT_APP_LOG
}

stop_profile() {
    stop_all_apps true
}

log_info "========== fullstack-example Task 9 E2E =========="
skip_if_no_service Redis "$REDIS_HOST" "$REDIS_PORT" fullstack-example
require_command docker || exit 1
require_command redis-cli || exit 1

if [ "$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB" DBSIZE)" != "0" ]; then
    record_fail "Redis 测试库 $REDIS_DB 非空，拒绝覆盖"
    print_summary fullstack-example
    exit $?
fi
record_pass "Redis 测试库 $REDIS_DB 为空"

NEBULA_E2E_MYSQL_PORT="$MYSQL_PORT" docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" up -d mysql
MYSQL_STARTED=1
if wait_until "隔离 MySQL 8.3 健康" 60 1 \
    docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" exec -T mysql mysqladmin ping -h 127.0.0.1 --silent; then
    record_pass "隔离 MySQL 8.3 健康"
else
    record_fail "隔离 MySQL 8.3 未就绪"
    print_summary fullstack-example
    exit $?
fi

mysql_exec < examples/fullstack-example/sql/data-demo-tables.sql
mysql_exec < examples/fullstack-example/sql/sharding-tables.sql
if [ "$(mysql_exec -Nse 'SELECT COUNT(*) FROM nebula_example.t_product')" = "24" ]; then
    record_pass "产品表初始化 24 条基线数据"
else
    record_fail "产品表基线数据不正确"
fi
if [ "$(mysql_exec -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema IN ('nebula_sharding_0','nebula_sharding_1') AND table_name LIKE 't_order_%'")" = "4" ]; then
    record_pass "四张分片订单表初始化完成"
else
    record_fail "分片订单表初始化不完整"
fi

# 默认模式：产品 CRUD、分页、逻辑删除和缓存。
start_profile dev
assert_json "默认模式健康检查" "http://localhost:$PORT/health/ping" \
    '.success == true and .data.status == "pong"'

PRODUCT_NAME="nebula_e2e_default_${E2E_RUN_ID}"
PRODUCT_JSON=$(jq -cn --arg name "$PRODUCT_NAME" \
    '{name:$name,description:"task9 default",price:88.50,category:"E2E",stockQuantity:9,status:"ACTIVE"}')
assert_json "默认模式创建产品" "http://localhost:$PORT/data/products" \
    '.success == true and .data.id > 0' POST "$PRODUCT_JSON"
PRODUCT_ID=$(jq -r '.data.id // 0' "$HTTP_BODY_FILE")
assert_json "默认模式查询产品" "http://localhost:$PORT/data/products?id=$PRODUCT_ID" \
    ".success == true and .data.product.id == $PRODUCT_ID and .data.product.name == \"$PRODUCT_NAME\""

UPDATE_JSON=$(jq -cn --argjson id "$PRODUCT_ID" \
    '{id:$id,name:"nebula_e2e_default_updated",price:99.50,category:"E2E",stock:7,description:"updated"}')
assert_json "默认模式更新产品" "http://localhost:$PORT/data/products" \
    ".success == true and .data.product.id == $PRODUCT_ID and .data.product.name == \"nebula_e2e_default_updated\"" \
    PUT "$UPDATE_JSON"
assert_json "默认模式分页筛选产品" \
    "http://localhost:$PORT/data/products/list?keyword=nebula_e2e_default_updated&page=1&size=10" \
    ".success == true and .data.products.total == 1 and .data.products.records[0].id == $PRODUCT_ID" POST

CACHE_KEY="task9-cache-consistency"
assert_json "多级缓存设置初始值" "http://localhost:$PORT/cache/set" \
    '.success == true and .data.success == true' POST \
    "{\"key\":\"$CACHE_KEY\",\"value\":\"nebula_e2e_cache_v1\",\"ttlSeconds\":60}"
assert_json "多级缓存 L1 读取初始值" "http://localhost:$PORT/cache/get" \
    '.success == true and .data.exists == true and .data.value == "nebula_e2e_cache_v1" and .data.source == "L1"' POST \
    "{\"key\":\"$CACHE_KEY\",\"valueType\":\"string\"}"
assert_json "多级缓存更新值" "http://localhost:$PORT/cache/set" \
    '.success == true and .data.success == true' POST \
    "{\"key\":\"$CACHE_KEY\",\"value\":\"nebula_e2e_cache_v2\",\"ttlSeconds\":60}"
assert_json "多级缓存 L1/L2 更新一致" "http://localhost:$PORT/cache/get" \
    '.success == true and .data.exists == true and .data.value == "nebula_e2e_cache_v2" and .data.source == "L1"' POST \
    "{\"key\":\"$CACHE_KEY\",\"valueType\":\"string\"}"
if [ "$(redis_keys | wc -l | tr -d ' ')" -ge 1 ]; then
    record_pass "Redis L2 存在命名空间缓存键"
else
    record_fail "Redis L2 未写入命名空间缓存键"
fi
assert_json "多级缓存删除" "http://localhost:$PORT/cache/delete" \
    '.success == true and .data.deletedCount == 1' DELETE "{\"keys\":[\"$CACHE_KEY\"]}"
assert_json "多级缓存删除后未命中" "http://localhost:$PORT/cache/get" \
    '.success == true and .data.exists == false and .data.source == "MISS"' POST \
    "{\"key\":\"$CACHE_KEY\",\"valueType\":\"string\"}"

TTL_CACHE_KEY="task9-cache-ttl"
assert_json "多级缓存设置短 TTL" "http://localhost:$PORT/cache/set" \
    '.success == true and .data.success == true and .data.ttlSeconds == 2' POST \
    "{\"key\":\"$TTL_CACHE_KEY\",\"value\":\"nebula_e2e_cache_ttl\",\"ttlSeconds\":2}"
assert_json "多级缓存短 TTL 立即命中" "http://localhost:$PORT/cache/get" \
    '.success == true and .data.exists == true and .data.value == "nebula_e2e_cache_ttl"' POST \
    "{\"key\":\"$TTL_CACHE_KEY\",\"valueType\":\"string\"}"
sleep 3
assert_json "多级缓存 TTL 到期" "http://localhost:$PORT/cache/get" \
    '.success == true and .data.exists == false and .data.source == "MISS"' POST \
    "{\"key\":\"$TTL_CACHE_KEY\",\"valueType\":\"string\"}"

assert_json "Spring Cache 创建用户" "http://localhost:$PORT/cache/users/create" \
    '.success == true and .data.userId > 0 and .data.username == "nebula_e2e_cache_user"' POST \
    '{"username":"nebula_e2e_cache_user","email":"cache@example.com","age":25}'
CACHE_USER_ID=$(jq -r '.data.userId // 0' "$HTTP_BODY_FILE")
assert_json "Spring Cache 更新用户" "http://localhost:$PORT/cache/users/update" \
    ".success == true and .data.userId == $CACHE_USER_ID and .data.username == \"nebula_e2e_cache_updated\"" PUT \
    "{\"userId\":$CACHE_USER_ID,\"username\":\"nebula_e2e_cache_updated\",\"email\":\"updated@example.com\",\"age\":26}"
assert_json "Spring Cache 读取更新值" "http://localhost:$PORT/cache/users/get" \
    ".success == true and .data.userId == $CACHE_USER_ID and .data.username == \"nebula_e2e_cache_updated\"" POST \
    "{\"userId\":$CACHE_USER_ID}"
assert_json "Spring Cache 删除用户" "http://localhost:$PORT/cache/users/delete" \
    '.success == true and .data.success == true' DELETE "{\"userId\":$CACHE_USER_ID}"

assert_json "默认模式逻辑删除产品" "http://localhost:$PORT/data/products" \
    '.success == true and .data.deletedCount == 1' DELETE "{\"ids\":[$PRODUCT_ID]}"
stop_profile
if [ "$(mysql_exec -Nse "SELECT deleted FROM nebula_example.t_product WHERE id=$PRODUCT_ID")" = "1" ]; then
    record_pass "数据库确认产品为逻辑删除"
else
    record_fail "数据库未记录产品逻辑删除"
fi
mysql_exec -e "DELETE FROM nebula_example.t_product WHERE id=$PRODUCT_ID"

# 读写分离模式。
start_profile dev,readwrite
assert_json "读写分离状态" "http://localhost:$PORT/readwrite/status" \
    '.success == true and .data.readWriteSeparationEnabled == true and (.data.availableDataSources | length) == 2'
RW_NAME="nebula_e2e_readwrite_${E2E_RUN_ID}"
RW_JSON=$(jq -cn --arg name "$RW_NAME" \
    '{name:$name,description:"task9 readwrite",price:66.00,category:"E2E",stockQuantity:6,status:"ACTIVE"}')
assert_json "读写分离写入主库" "http://localhost:$PORT/readwrite/products" \
    '.success == true and .data.id > 0' POST "$RW_JSON"
RW_ID=$(jq -r '.data.id // 0' "$HTTP_BODY_FILE")
assert_json "读写分离从库读取" "http://localhost:$PORT/readwrite/products/?id=$RW_ID" \
    ".success == true and .data.product.id == $RW_ID and .data.product.name == \"$RW_NAME\""
assert_file_contains "读写分离日志包含写路由" "$PROFILE_LOG" '写数据源|WRITE|master|primary'
stop_profile
mysql_exec -e "DELETE FROM nebula_example.t_product WHERE id=$RW_ID"

# 分片模式。
SHARD_ORDER_ID=900000000000000002
SHARD_USER_ID=200
start_profile dev,sharding
SHARD_JSON=$(jq -cn --argjson orderId "$SHARD_ORDER_ID" --argjson userId "$SHARD_USER_ID" \
    '{orderId:$orderId,userId:$userId,productName:"nebula_e2e_sharding",amount:123.45,status:"PENDING"}')
assert_json "分片模式创建订单" "http://localhost:$PORT/sharding/orders" \
    ".success == true and .data.orderId == $SHARD_ORDER_ID" POST "$SHARD_JSON"
assert_json "分片模式精确查询订单" \
    "http://localhost:$PORT/sharding/orders/?orderId=$SHARD_ORDER_ID&userId=$SHARD_USER_ID" \
    ".success == true and .data.order.id == $SHARD_ORDER_ID and .data.order.userId == $SHARD_USER_ID"
if [ "$(mysql_exec -Nse "SELECT COUNT(*) FROM nebula_sharding_0.t_order_0 WHERE id=$SHARD_ORDER_ID AND user_id=$SHARD_USER_ID")" = "1" ]; then
    record_pass "数据库确认订单路由到 ds0.t_order_0"
else
    record_fail "订单未路由到预期分片"
fi
stop_profile
mysql_exec -e "DELETE FROM nebula_sharding_0.t_order_0 WHERE id=$SHARD_ORDER_ID"

# 组合模式：同一进程同时使用读写分离和分片。
COMBINED_ORDER_ID=900000000000000013
COMBINED_USER_ID=201
start_profile dev,combined
assert_json "组合模式读写分离状态" "http://localhost:$PORT/readwrite/status" \
    '.success == true and .data.readWriteSeparationEnabled == true'
COMBINED_PRODUCT="nebula_e2e_combined_${E2E_RUN_ID}"
COMBINED_PRODUCT_JSON=$(jq -cn --arg name "$COMBINED_PRODUCT" \
    '{name:$name,description:"task9 combined",price:77.00,category:"E2E",stockQuantity:7,status:"ACTIVE"}')
assert_json "组合模式写入产品" "http://localhost:$PORT/readwrite/products" \
    '.success == true and .data.id > 0' POST "$COMBINED_PRODUCT_JSON"
COMBINED_PRODUCT_ID=$(jq -r '.data.id // 0' "$HTTP_BODY_FILE")
assert_json "组合模式从读库读取产品" "http://localhost:$PORT/readwrite/products/?id=$COMBINED_PRODUCT_ID" \
    ".success == true and .data.product.id == $COMBINED_PRODUCT_ID and .data.product.name == \"$COMBINED_PRODUCT\""
COMBINED_ORDER_JSON=$(jq -cn --argjson orderId "$COMBINED_ORDER_ID" --argjson userId "$COMBINED_USER_ID" \
    '{orderId:$orderId,userId:$userId,productName:"nebula_e2e_combined_order",amount:55.00,status:"PENDING"}')
assert_json "组合模式写入分片订单" "http://localhost:$PORT/sharding/orders" \
    ".success == true and .data.orderId == $COMBINED_ORDER_ID" POST "$COMBINED_ORDER_JSON"
if [ "$(mysql_exec -Nse "SELECT COUNT(*) FROM nebula_sharding_1.t_order_1 WHERE id=$COMBINED_ORDER_ID AND user_id=$COMBINED_USER_ID")" = "1" ]; then
    record_pass "组合模式订单路由到 ds1.t_order_1"
else
    record_fail "组合模式订单未路由到预期分片"
fi
assert_file_contains "组合模式产品写路由到 master" "$PROFILE_LOG" \
    'Actual SQL: master ::: INSERT INTO t_product'
assert_file_contains "组合模式产品读路由到 slave01" "$PROFILE_LOG" \
    'Actual SQL: slave01 ::: SELECT.*FROM t_product'
stop_profile
mysql_exec -e "DELETE FROM nebula_example.t_product WHERE id=$COMBINED_PRODUCT_ID; DELETE FROM nebula_sharding_1.t_order_1 WHERE id=$COMBINED_ORDER_ID"

cleanup_redis
if [ -z "$(redis_keys)" ] && [ "$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB" DBSIZE)" = "0" ]; then
    record_pass "Redis 测试库缓存键已清理"
    REDIS_CLEANED=1
else
    record_fail "Redis 测试库仍有缓存键"
fi

docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" down -v --remove-orphans
MYSQL_STARTED=0
if ! nc -z "$MYSQL_HOST" "$MYSQL_PORT" >/dev/null 2>&1 &&
    [ -z "$(docker volume ls -q --filter "name=${COMPOSE_PROJECT}_mysql-data")" ]; then
    record_pass "隔离 MySQL 容器、端口和卷已清理"
else
    record_fail "隔离 MySQL 资源清理不完整"
fi

print_summary fullstack-example
