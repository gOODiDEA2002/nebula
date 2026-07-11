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
ELASTICSEARCH_PORT=${NEBULA_E2E_ES_PORT:-19200}
RABBITMQ_MANAGEMENT_PORT=${RABBITMQ_MANAGEMENT_PORT:-15672}
COMPOSE_FILE="$PROJECT_ROOT/docker/verification/docker-compose.yml"
COMPOSE_PROJECT="nebula-e2e-fullstack-$$"
CACHE_PREFIX="nebula:e2e:${E2E_RUN_ID}:"
RABBITMQ_VHOST="nebula_e2e_${E2E_RUN_ID//-/_}"
MINIO_BUCKET="nebula-e2e-${E2E_RUN_ID}"
SEARCH_INDEX="nebula_e2e_${E2E_RUN_ID//-/_}"
SEARCH_PHYSICAL_INDEX="nebula_example_${SEARCH_INDEX}"
MYSQL_STARTED=0
REDIS_CLEANED=0
RABBITMQ_CLEANED=0
MINIO_CLEANED=0

export FULLSTACK_PORT="$PORT" MYSQL_HOST MYSQL_PORT MYSQL_USERNAME=root MYSQL_PASSWORD=''
export REDIS_HOST REDIS_PORT REDIS_PASSWORD='' FULLSTACK_REDIS_DATABASE="$REDIS_DB"
export FULLSTACK_CACHE_PREFIX="$CACHE_PREFIX"
export ELASTICSEARCH_HOST=127.0.0.1 ELASTICSEARCH_PORT FULLSTACK_SEARCH_INDEX="$SEARCH_INDEX"
export RABBITMQ_HOST=127.0.0.1 RABBITMQ_PORT=5672 RABBITMQ_USERNAME=guest RABBITMQ_PASSWORD=guest
export RABBITMQ_VHOST
export MINIO_HOST=127.0.0.1 MINIO_PORT=9000 MINIO_ACCESS_KEY=minioadmin MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET
export NEBULA_DISCOVERY_NACOS_ENABLED=false
export NEBULA_RPC_HTTP_ENABLED=false NEBULA_RPC_GRPC_ENABLED=false NEBULA_RPC_DISCOVERY_ENABLED=false
export NEBULA_MESSAGING_RABBITMQ_ENABLED=true NEBULA_AI_ENABLED=false
export NEBULA_SEARCH_ENABLED=true NEBULA_SEARCH_ELASTICSEARCH_ENABLED=true NEBULA_MINIO_ENABLED=true
export NEBULA_STORAGE_MINIO_ENABLED=true NEBULA_TASK_ENABLED=true
export NEBULA_LOCK_ENABLED=false NEBULA_WEBSOCKET_ENABLED=false
export NEBULA_WEB_RATE_LIMIT_REQUESTS_PER_SECOND=1000
export NEBULA_WEB_RATE_LIMIT_KEY_STRATEGY=IP_API

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

cleanup_rabbitmq() {
    curl -fsS -u guest:guest -X DELETE \
        "http://127.0.0.1:$RABBITMQ_MANAGEMENT_PORT/api/vhosts/$RABBITMQ_VHOST" >/dev/null 2>&1 || true
}

cleanup_minio() {
    docker exec nebula-minio mc alias set nebula-e2e http://127.0.0.1:9000 \
        minioadmin minioadmin >/dev/null 2>&1 || true
    docker exec nebula-minio mc rb --force "nebula-e2e/$MINIO_BUCKET" >/dev/null 2>&1 || true
}

cleanup_fullstack() {
    local exit_code=$?
    trap - EXIT
    set +e
    stop_all_apps false
    if [ "$REDIS_CLEANED" -eq 0 ]; then
        cleanup_redis
    fi
    if [ "$RABBITMQ_CLEANED" -eq 0 ]; then
        cleanup_rabbitmq
    fi
    if [ "$MINIO_CLEANED" -eq 0 ]; then
        cleanup_minio
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
skip_if_no_service RabbitMQ 127.0.0.1 5672 fullstack-example
skip_if_no_service RabbitMQ-Management 127.0.0.1 "$RABBITMQ_MANAGEMENT_PORT" fullstack-example
skip_if_no_service MinIO 127.0.0.1 9000 fullstack-example
require_command docker || exit 1
require_command redis-cli || exit 1

if [ "$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB" DBSIZE)" != "0" ]; then
    record_fail "Redis 测试库 $REDIS_DB 非空，拒绝覆盖"
    print_summary fullstack-example
    exit $?
fi
record_pass "Redis 测试库 $REDIS_DB 为空"

NEBULA_E2E_MYSQL_PORT="$MYSQL_PORT" NEBULA_E2E_ES_PORT="$ELASTICSEARCH_PORT" \
    docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" up -d mysql elasticsearch
MYSQL_STARTED=1
if wait_until "隔离 MySQL 8.3 健康" 60 1 \
    docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" exec -T mysql mysqladmin ping -h 127.0.0.1 --silent; then
    record_pass "隔离 MySQL 8.3 健康"
else
    record_fail "隔离 MySQL 8.3 未就绪"
    print_summary fullstack-example
    exit $?
fi
if wait_until "隔离 Elasticsearch 9.4.2 健康" 90 2 \
    curl -fsS "http://127.0.0.1:$ELASTICSEARCH_PORT/_cluster/health"; then
    record_pass "隔离 Elasticsearch 9.4.2 健康"
else
    record_fail "隔离 Elasticsearch 9.4.2 未就绪"
    print_summary fullstack-example
    exit $?
fi

curl -fsS -u guest:guest -X PUT \
    "http://127.0.0.1:$RABBITMQ_MANAGEMENT_PORT/api/vhosts/$RABBITMQ_VHOST" >/dev/null
curl -fsS -u guest:guest -H 'Content-Type: application/json' -X PUT \
    "http://127.0.0.1:$RABBITMQ_MANAGEMENT_PORT/api/permissions/$RABBITMQ_VHOST/guest" \
    -d '{"configure":".*","write":".*","read":".*"}' >/dev/null
if curl -fsS -u guest:guest \
    "http://127.0.0.1:$RABBITMQ_MANAGEMENT_PORT/api/vhosts/$RABBITMQ_VHOST" | jq -e \
    --arg name "$RABBITMQ_VHOST" '.name == $name' >/dev/null; then
    record_pass "RabbitMQ 临时 vhost 已创建"
else
    record_fail "RabbitMQ 临时 vhost 创建失败"
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

# 通用模块：RabbitMQ、Elasticsearch、MinIO、Task、Payment、Notification 和 Web。
export NEBULA_WEB_RATE_LIMIT_REQUESTS_PER_SECOND=3
start_profile dev
MODULE_LOG=$PROFILE_LOG

MESSAGE_ORDER_ID=910000001
assert_json "RabbitMQ 发送订单通知" "http://localhost:$PORT/messaging/order/notification" \
    '.success == true and .data.success == true and (.data.messageId | length) > 0' POST \
    "{\"orderId\":$MESSAGE_ORDER_ID,\"orderNo\":\"nebula_e2e_order\",\"userId\":101,\"productName\":\"E2E Product\",\"amount\":19.99,\"status\":\"CREATED\",\"notificationType\":\"ORDER_CREATED\"}"
if wait_until "RabbitMQ 消费订单通知" 15 1 grep -q \
    "订单通知处理完成: orderId=$MESSAGE_ORDER_ID" "$MODULE_LOG"; then
    record_pass "RabbitMQ 订单通知已消费"
else
    record_fail "RabbitMQ 订单通知未消费"
fi

RETRY_ORDER_ID=910000002
assert_json "RabbitMQ 发送失败重投消息" "http://localhost:$PORT/messaging/order/status-update" \
    '.success == true and .data.success == true' POST \
    "{\"orderId\":$RETRY_ORDER_ID,\"orderNo\":\"nebula_e2e_retry\",\"oldStatus\":\"CREATED\",\"newStatus\":\"E2E_RETRY\",\"async\":false}"
if wait_until "RabbitMQ 失败消息重投成功" 15 1 grep -q \
    "订单状态重试验证: orderId=$RETRY_ORDER_ID, attempt=2" "$MODULE_LOG"; then
    record_pass "RabbitMQ 首次失败后重投成功"
else
    record_fail "RabbitMQ 失败消息没有完成重投"
fi

assert_json "RabbitMQ 发送延迟消息" "http://localhost:$PORT/messaging/delay/custom" \
    '.success == true and .data.delaySeconds == 1 and (.data.messageId | length) > 0' POST \
    '{"title":"nebula_e2e_delay","content":"delayed","delaySeconds":1}'
if wait_until "RabbitMQ 延迟消息到期消费" 15 1 grep -q \
    '延迟通知处理完成: title=nebula_e2e_delay' "$MODULE_LOG"; then
    record_pass "RabbitMQ 延迟消息已消费"
else
    record_fail "RabbitMQ 延迟消息未消费"
fi
assert_json "RabbitMQ 生产和消费统计" "http://localhost:$PORT/messaging/stats?statsType=ALL" \
    '.success == true and .data.producerStats.sentCount == 2 and .data.producerStats.successCount == 2 and .data.consumerStats.consumedCount >= 3 and .data.consumerStats.failedCount == 1'

assert_json "Elasticsearch 创建临时索引" "http://localhost:$PORT/search/index/create" \
    ".success == true and .data.success == true and .data.indexName == \"$SEARCH_PHYSICAL_INDEX\"" POST \
    "{\"indexName\":\"$SEARCH_INDEX\",\"shards\":1,\"replicas\":0}"
assert_json "Elasticsearch 索引单个产品" "http://localhost:$PORT/search/products/index" \
    '.success == true and .data.success == true and .data.id == "1"' POST '{"productId":1}'
assert_json "Elasticsearch 批量索引产品" "http://localhost:$PORT/search/products/bulk-index" \
    '.success == true and .data.success == true and .data.totalCount == 2 and .data.failureCount == 0' POST \
    '{"productIds":[2,3]}'
if curl -fsS -X POST "http://127.0.0.1:$ELASTICSEARCH_PORT/$SEARCH_PHYSICAL_INDEX/_refresh" >/dev/null; then
    record_pass "Elasticsearch 索引刷新成功"
else
    record_fail "Elasticsearch 索引刷新失败"
fi
assert_json "Elasticsearch 查询产品" "http://localhost:$PORT/search/products/search" \
    '.success == true and .data.success == true and .data.totalHits >= 1 and (.data.products | length) >= 1' POST \
    '{"keyword":"iPhone","page":1,"size":10,"sortFields":["_score"],"sortOrder":"desc","highlight":true}'
assert_json "Elasticsearch 搜索建议" "http://localhost:$PORT/search/products/suggest" \
    '.success == true and .data.success == true and (.data.suggestions | type) == "array"' POST \
    '{"text":"Phne","size":5}'
assert_json "Elasticsearch 删除单个文档" "http://localhost:$PORT/search/products/index" \
    '.success == true and .data.success == true and .data.id == "1"' DELETE '{"productId":1}'
assert_json "Elasticsearch 删除临时索引" "http://localhost:$PORT/search/index/delete" \
    ".success == true and .data.success == true and .data.indexName == \"$SEARCH_INDEX\"" DELETE \
    "{\"indexName\":\"$SEARCH_INDEX\"}"

assert_json "MinIO 临时 Bucket 已创建" \
    "http://localhost:$PORT/storage/bucket/exists?bucket=$MINIO_BUCKET" '.success == true and .data == true'
UPLOAD_SOURCE="$E2E_CASE_DIR/minio-upload.txt"
UPLOAD_BODY="$E2E_CASE_DIR/minio-upload-response.json"
DOWNLOAD_BODY="$E2E_CASE_DIR/minio-download.txt"
printf 'nebula_e2e_minio_payload_%s\n' "$E2E_RUN_ID" > "$UPLOAD_SOURCE"
UPLOAD_STATUS=$(curl --silent --show-error --noproxy '*' -o "$UPLOAD_BODY" -w '%{http_code}' \
    -F "bucket=$MINIO_BUCKET" -F 'category=nebula_e2e' -F "file=@$UPLOAD_SOURCE;type=text/plain" \
    "http://localhost:$PORT/storage/upload")
if [ "$UPLOAD_STATUS" = 200 ] && jq -e '.success == true and (.data.key | length) > 0' "$UPLOAD_BODY" >/dev/null; then
    record_pass "MinIO 上传对象"
else
    record_fail "MinIO 上传对象失败，HTTP $UPLOAD_STATUS"
fi
MINIO_KEY=$(jq -r '.data.key // empty' "$UPLOAD_BODY")
assert_json "MinIO 列出对象" "http://localhost:$PORT/storage/list" \
    ".success == true and .data.total == 1 and .data.files[0].key == \"$MINIO_KEY\"" POST \
    "{\"bucket\":\"$MINIO_BUCKET\",\"prefix\":\"nebula_e2e/\",\"maxKeys\":10}"
if curl -fsS --noproxy '*' -G --data-urlencode "bucket=$MINIO_BUCKET" --data-urlencode "key=$MINIO_KEY" \
    "http://localhost:$PORT/storage/download" -o "$DOWNLOAD_BODY" && cmp -s "$UPLOAD_SOURCE" "$DOWNLOAD_BODY"; then
    record_pass "MinIO 下载字节与上传内容一致"
else
    record_fail "MinIO 下载字节不一致"
fi
assert_json "MinIO 生成预签名 URL" "http://localhost:$PORT/storage/presigned-url" \
    '.success == true and .data.expirySeconds == 60 and (.data.url | startswith("http"))' POST \
    "{\"bucket\":\"$MINIO_BUCKET\",\"key\":\"$MINIO_KEY\",\"expirySeconds\":60}"
assert_json "MinIO 删除对象" "http://localhost:$PORT/storage/delete" \
    '.success == true and .data.deletedCount == 1 and (.data.failedKeys == null or (.data.failedKeys | length) == 0)' DELETE \
    "{\"bucket\":\"$MINIO_BUCKET\",\"keys\":[\"$MINIO_KEY\"]}"

assert_json "Task 执行器已注册" "http://localhost:$PORT/task/executors" \
    '.success == true and ([.data[].name] | index("dataCleanupTask")) != null and ([.data[].name] | index("reportGeneratorTask")) != null'
assert_json "Task 同步执行成功" "http://localhost:$PORT/task/execute/dataCleanupTask" \
    '.success == true and .data.success == true and .data.data.retentionDays == 7 and .data.data.targetTable == "orders"' POST \
    '{"retentionDays":7,"targetTable":"orders"}'
assert_json "Task 未知执行器返回业务失败" "http://localhost:$PORT/task/execute/doesNotExist" \
    '.success == true and .data.success == false and (.data.message | contains("找不到任务执行器"))' POST '{}'

PAYMENT_NO="nebula_e2e_payment_${E2E_RUN_ID}"
assert_json "Mock Payment 创建支付" "http://localhost:$PORT/payment/create" \
    '.success == true and .data.success == true and (.data.tradeNo | length) > 0' POST \
    "{\"outTradeNo\":\"$PAYMENT_NO\",\"amount\":12.34,\"subject\":\"E2E Payment\",\"paymentType\":\"WEB\"}"
assert_json "Mock Payment 查询支付" "http://localhost:$PORT/payment/query" \
    '.success == true and .data.success == true' POST "{\"outTradeNo\":\"$PAYMENT_NO\"}"
assert_json "Mock Payment 未支付订单拒绝退款" "http://localhost:$PORT/payment/refund" \
    '.success == true and .data.success == false and .data.errorCode == "ORDER_NOT_FOUND_OR_NOT_PAID"' POST \
    "{\"outTradeNo\":\"$PAYMENT_NO\",\"outRefundNo\":\"refund_$E2E_RUN_ID\",\"refundAmount\":1.23,\"reason\":\"E2E\"}"
assert_status "Mock Payment 非法金额返回 400" "http://localhost:$PORT/payment/create" 400 POST \
    '{"outTradeNo":"invalid","amount":0,"subject":"invalid","paymentType":"WEB"}'

assert_json "Mock Notification 发送短信" "http://localhost:$PORT/notification/sms/send" \
    '.success == true and .data.success == true and .data.phone == "13800138000"' POST \
    '{"phone":"13800138000","template":"nebula_e2e_sms","params":["ok"]}'
assert_json "Mock Notification 发送验证码" "http://localhost:$PORT/notification/sms/verification-code" \
    '.success == true and .data.success == true and .data.phone == "13800138000"' POST \
    '{"phone":"13800138000","code":"123456"}'
assert_status "Mock Notification 非法手机号返回 400" \
    "http://localhost:$PORT/notification/sms/send" 400 POST \
    '{"phone":"123","template":"nebula_e2e_sms","params":[]}'

assert_json "Web 健康端点" "http://localhost:$PORT/health/ping" \
    '.success == true and .data.status == "pong"'
assert_json "Web 性能端点" "http://localhost:$PORT/performance/status" \
    '.success == true'
assert_json "Web 敏感数据脱敏" "http://localhost:$PORT/hello/sensitive-data" \
    '.success == true and (.data.email | contains("***")) and (.data.mobile | contains("****")) and .data.password == "******"'
assert_json "Web 响应缓存首次请求" "http://localhost:$PORT/hello/cached-data/42" \
    '.success == true and .data.id == "42" and (.data.generatedAt | length) > 0'
CACHED_TIMESTAMP=$(jq -r '.data.generatedAt' "$HTTP_BODY_FILE")
CACHE_HIT_BODY="$E2E_CASE_DIR/cache-hit.body"
CACHE_HIT_HEADERS="$E2E_CASE_DIR/cache-hit.headers"
CACHE_HIT_STATUS=$(curl --silent --show-error --noproxy '*' -D "$CACHE_HIT_HEADERS" \
    -o "$CACHE_HIT_BODY" -w '%{http_code}' "http://localhost:$PORT/hello/cached-data/42")
if [ "$CACHE_HIT_STATUS" = 200 ] &&
    jq -e ".success == true and .data.generatedAt == \"$CACHED_TIMESTAMP\"" "$CACHE_HIT_BODY" >/dev/null &&
    grep -qi '^x-cache: HIT' "$CACHE_HIT_HEADERS"; then
    record_pass "Web 响应缓存正文和命中头一致"
else
    record_fail "Web 响应缓存未命中，证据：$CACHE_HIT_BODY、$CACHE_HIT_HEADERS"
fi

RATE_LIMIT_429=0
for _ in 1 2 3 4 5 6; do
    perform_request GET "http://localhost:$PORT/hello/rate-limit-test" "" || true
    if [ "$HTTP_STATUS" = 429 ]; then
        RATE_LIMIT_429=$((RATE_LIMIT_429 + 1))
    fi
done
if [ "$RATE_LIMIT_429" -ge 1 ]; then
    record_pass "Web 限流连续请求出现 429"
else
    record_fail "Web 限流连续请求未出现 429"
fi

stop_profile

if curl -fsS "http://127.0.0.1:$ELASTICSEARCH_PORT/$SEARCH_PHYSICAL_INDEX" >/dev/null 2>&1; then
    record_fail "Elasticsearch 临时索引仍存在"
else
    record_pass "Elasticsearch 临时索引已删除"
fi
cleanup_rabbitmq
if curl -fsS -u guest:guest \
    "http://127.0.0.1:$RABBITMQ_MANAGEMENT_PORT/api/vhosts/$RABBITMQ_VHOST" >/dev/null 2>&1; then
    record_fail "RabbitMQ 临时 vhost 仍存在"
else
    record_pass "RabbitMQ 临时 vhost 及其中资源已删除"
    RABBITMQ_CLEANED=1
fi
cleanup_minio
if docker exec nebula-minio mc stat "nebula-e2e/$MINIO_BUCKET" >/dev/null 2>&1; then
    record_fail "MinIO 临时 Bucket 仍存在"
else
    record_pass "MinIO 临时 Bucket 及对象已删除"
    MINIO_CLEANED=1
fi

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
    ! nc -z 127.0.0.1 "$ELASTICSEARCH_PORT" >/dev/null 2>&1 &&
    [ -z "$(docker volume ls -q --filter "name=${COMPOSE_PROJECT}_mysql-data")" ] &&
    [ -z "$(docker volume ls -q --filter "name=${COMPOSE_PROJECT}_elasticsearch-data")" ]; then
    record_pass "隔离 MySQL、Elasticsearch 容器、端口和卷已清理"
else
    record_fail "隔离 MySQL 或 Elasticsearch 资源清理不完整"
fi

print_summary fullstack-example
