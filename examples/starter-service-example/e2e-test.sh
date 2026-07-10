#!/usr/bin/env bash
# starter-service-example E2E 测试
# 验证 Service Starter 的核心功能：健康检查、HTTP RPC、分布式锁
# 外部依赖: Redis（锁服务），Nacos 已禁用
source "$(dirname "$0")/../e2e-common.sh"

PORT=8082
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-}
log_info "========== starter-service-example E2E =========="

skip_if_no_service "Redis" "$REDIS_HOST" "$REDIS_PORT" "starter-service-example"

start_app "examples/starter-service-example" "$PORT"

assert_json "GET /health/ping 健康检查" \
    "http://localhost:$PORT/health/ping" \
    '.success == true and .data.status == "pong"'

assert_json "GET /performance/status 性能状态" \
    "http://localhost:$PORT/performance/status" \
    '.success == true and (.data.application.status | IN("HEALTHY", "WARNING"))'

assert_json "GET /rpc/hello 返回问候" \
    "http://localhost:$PORT/rpc/hello" \
    '.success == true and .data == "Hello, Nebula Service!"'

assert_json "GET /rpc/hello/greet 返回带参数问候" \
    "http://localhost:$PORT/rpc/hello/greet?name=Nebula" \
    '.success == true and .data == "Hello, Nebula! Welcome to Nebula Framework."'

assert_json "GET /rpc/hello/info 返回服务信息" \
    "http://localhost:$PORT/rpc/hello/info" \
    '.success == true and .data.serviceName == "starter-service-example" and .data.version == "2.1.0-SNAPSHOT"'

RPC_REQUEST='{"requestId":"nebula-e2e-http-rpc","serviceName":"io.nebula.examples.service.api.HelloRpcClient","methodName":"hello","parameterTypes":[],"parameters":[],"headers":{},"timestamp":1,"timeout":3000,"version":"1.0"}'
assert_json "POST /rpc 真实调用 HTTP RPC 服务" \
    "http://localhost:$PORT/rpc" \
    '.requestId == "nebula-e2e-http-rpc" and .statusCode == 200 and .result == "Hello, Nebula Service!"' \
    POST "$RPC_REQUEST"

LOCK_KEY="nebula_e2e_starter_service_${E2E_RUN_ID}"
assert_json "Redis Lock Bean 执行受锁回调" \
    "http://localhost:$PORT/lock/execute?key=$LOCK_KEY" \
    ".success == true and .data.executed == true and .data.key == \"$LOCK_KEY\""

assert_file_contains "HTTP RPC 服务已注册" "$CURRENT_APP_LOG" \
    '自动注册RPC服务:.*HelloRpcClient'
assert_file_contains "HTTP RPC Server 跟随应用端口" "$CURRENT_APP_LOG" \
    '配置HTTP RPC服务器: port=8082,'
assert_file_contains "Redis Lock 管理器已初始化" "$CURRENT_APP_LOG" \
    'RedisLockManager initialized with Redisson client'
assert_file_not_contains "未启用数据源、Nacos 和 RabbitMQ" "$CURRENT_APP_LOG" \
    'DataSourceManager 开始初始化|NacosServiceAutoRegistrar|RabbitMQAutoConfiguration'

if command -v redis-cli >/dev/null 2>&1; then
    REDIS_CLI=(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT")
    if [ -n "$REDIS_PASSWORD" ]; then
        REDIS_CLI+=(--no-auth-warning -a "$REDIS_PASSWORD")
    fi
    if [ -z "$("${REDIS_CLI[@]}" --scan --pattern "$LOCK_KEY")" ]; then
        record_pass "Redis 临时锁键已释放"
    else
        record_fail "Redis 临时锁键未释放：$LOCK_KEY"
    fi
else
    skip_test "Redis 临时锁键清理复核" "缺少 redis-cli"
fi

stop_app "$APP_PID"

FAILURE_PORT=18082
SERVER_PORT="$FAILURE_PORT" \
REDIS_PORT=1 \
REDIS_TIMEOUT=500 \
assert_app_startup_failure "Redis 不可用时应用快速失败" \
    "examples/starter-service-example" "$FAILURE_PORT" \
    'RedisConnectionException|Unable to connect to Redis server|Connection refused' 30

print_summary "starter-service-example"
