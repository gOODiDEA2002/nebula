#!/usr/bin/env bash
# fullstack-example E2E 测试
# 验证全功能示例的核心模块: Web/Cache/Task/Payment/Notification/Messaging/Search/Storage
# 外部依赖: MySQL, Redis, Nacos, RabbitMQ, Elasticsearch, MinIO
source "$(dirname "$0")/../e2e-common.sh"

PORT=1000
DEV_SERVER="${NEBULA_DEV_SERVER:-127.0.0.1}"
export NEBULA_DEV_SERVER="$DEV_SERVER"
log_info "========== fullstack-example E2E =========="
log_info "NEBULA_DEV_SERVER=$DEV_SERVER"

skip_if_no_service "MySQL"    "$DEV_SERVER" 3306 "fullstack-example"
skip_if_no_service "Redis"    "$DEV_SERVER" 6379 "fullstack-example"
skip_if_no_service "Nacos"    "$DEV_SERVER" 8848 "fullstack-example"

# 先安装 microservice user-api 依赖
log_info "安装 microservice-example/user-api 依赖..."
cd "$PROJECT_ROOT"
mvn -q -f examples/microservice-example/user-api install -DskipTests 2>/dev/null || true

# 根据实际可用服务动态启用/禁用可选模块
export NEBULA_MESSAGING_RABBITMQ_ENABLED=false
export NEBULA_AI_ENABLED=false

HAS_MINIO=false
HAS_ES=false
if require_service "MinIO" "$DEV_SERVER" 9000; then
    HAS_MINIO=true
    export NEBULA_MINIO_ENABLED=true
else
    export NEBULA_MINIO_ENABLED=false
fi
if require_service "Elasticsearch" "$DEV_SERVER" 9200; then
    HAS_ES=true
    export NEBULA_SEARCH_ENABLED=true
else
    export NEBULA_SEARCH_ENABLED=false
fi

start_app "examples/fullstack-example" "$PORT"

# ===== Web 基础 =====
assert_contains "GET /hello 返回成功" \
    "http://localhost:$PORT/hello" '"success":true'

assert_contains "GET /health/ping 健康检查" \
    "http://localhost:$PORT/health/ping" '"status":"pong"'

assert_contains "GET /performance/status 性能状态" \
    "http://localhost:$PORT/performance/status" '"success"'

# ===== 任务调度 =====
assert_contains "GET /task/executors 任务执行器列表" \
    "http://localhost:$PORT/task/executors" '"success":true'

# ===== 支付模块 (Mock) =====
assert_contains "POST /payment/create 创建支付单" \
    "http://localhost:$PORT/payment/create" '"success":true' \
    "POST" '{"outTradeNo":"E2E-001","amount":100.00,"paymentType":"web","subject":"E2E Test"}'

assert_contains "POST /payment/query 查询支付单" \
    "http://localhost:$PORT/payment/query" '"success":true' \
    "POST" '{"outTradeNo":"E2E-001"}'

assert_contains "GET /payment/status 支付状态" \
    "http://localhost:$PORT/payment/status" '"success":true'

# ===== 通知模块 (Mock) =====
assert_contains "POST /notification/sms/send 发送短信" \
    "http://localhost:$PORT/notification/sms/send" '"success":true' \
    "POST" '{"phone":"13800138000","template":"SMS_E2E_TEST","params":["e2e"]}'

assert_contains "POST /notification/sms/verification-code 发送验证码" \
    "http://localhost:$PORT/notification/sms/verification-code" '"success":true' \
    "POST" '{"phone":"13800138000"}'

# ===== 缓存模块 =====
assert_contains "POST /cache/set 设置缓存" \
    "http://localhost:$PORT/cache/set" '"success":true' \
    "POST" '{"key":"e2e-test-key","value":"e2e-test-value","ttl":60}'

assert_contains "POST /cache/get 获取缓存" \
    "http://localhost:$PORT/cache/get" '"success":true' \
    "POST" '{"key":"e2e-test-key"}'

# ===== 消息队列 (E2E 中已禁用 RabbitMQ，跳过) =====
skip_test "消息队列测试" "E2E 中 RabbitMQ 已禁用"

# ===== 搜索模块 (Elasticsearch 可选) =====
if [ "$HAS_ES" = "true" ]; then
    assert_contains "POST /search/index/create 创建索引" \
        "http://localhost:$PORT/search/index/create" '"success"' \
        "POST" '{"indexName":"e2e_test_index"}'
else
    skip_test "搜索模块测试" "Elasticsearch 不可达"
fi

# ===== 存储模块 (MinIO 可选) =====
if [ "$HAS_MINIO" = "true" ]; then
    assert_contains "GET /storage/buckets 存储桶列表" \
        "http://localhost:$PORT/storage/buckets" '"success"'
else
    skip_test "存储模块测试" "MinIO 不可达"
fi

print_summary "fullstack-example"
