#!/usr/bin/env bash
# 对示例依赖执行协议级读写，并准备隔离的 MySQL 和 Elasticsearch。

E2E_TEST_NAME="${E2E_TEST_NAME:-middleware-preflight}"
source "$(dirname "$0")/e2e-common.sh"

COMPOSE_FILE="$PROJECT_ROOT/docker/verification/docker-compose.yml"
VERIFICATION_PROJECT="${E2E_VERIFICATION_PROJECT:-nebula-e2e-$(safe_name "$E2E_RUN_ID")}"
KEEP_CONTAINERS="${E2E_KEEP_VERIFICATION_CONTAINERS:-false}"
MYSQL_PORT="${NEBULA_E2E_MYSQL_PORT:-13306}"
ES_PORT="${NEBULA_E2E_ES_PORT:-19200}"
REDIS_HOST="${NEBULA_E2E_REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${NEBULA_E2E_REDIS_PORT:-6379}"
RABBITMQ_HOST="${NEBULA_E2E_RABBITMQ_HOST:-127.0.0.1}"
RABBITMQ_API_PORT="${NEBULA_E2E_RABBITMQ_API_PORT:-15672}"
NACOS_HOST="${NEBULA_E2E_NACOS_HOST:-127.0.0.1}"
NACOS_PORT="${NEBULA_E2E_NACOS_PORT:-8848}"
MINIO_PORT="${NEBULA_E2E_MINIO_PORT:-9000}"
CHROMA_HOST="${NEBULA_E2E_CHROMA_HOST:-127.0.0.1}"
CHROMA_PORT="${NEBULA_E2E_CHROMA_PORT:-9002}"
RABBITMQ_CONTAINER="${NEBULA_E2E_RABBITMQ_CONTAINER:-nebula-rabbitmq}"
NACOS_CONTAINER="${NEBULA_E2E_NACOS_CONTAINER:-nebula-nacos}"
MINIO_CONTAINER="${NEBULA_E2E_MINIO_CONTAINER:-nebula-minio}"
RESOURCE_SUFFIX="$(date '+%s')-$$"
VERSIONS_FILE="$E2E_CASE_DIR/middleware-versions.txt"
STACK_STARTED=false

export NEBULA_E2E_MYSQL_PORT="$MYSQL_PORT"
export NEBULA_E2E_ES_PORT="$ES_PORT"

compose() {
    docker compose -f "$COMPOSE_FILE" -p "$VERIFICATION_PROJECT" "$@"
}

container_env_value() {
    local container=$1
    local key=$2
    docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$container" 2>/dev/null |
        sed -n "s/^${key}=//p" | head -n 1
}

record_version() {
    local service=$1
    local version=$2
    printf '%s=%s\n' "$service" "$version" >>"$VERSIONS_FILE"
}

wait_for_container_health() {
    local service=$1
    local timeout=${2:-180}
    local elapsed=0
    local container_id
    local status

    container_id=$(compose ps -q "$service")
    [ -n "$container_id" ] || return 1
    while [ "$elapsed" -lt "$timeout" ]; do
        status=$(docker inspect --format '{{.State.Health.Status}}' "$container_id" 2>/dev/null || true)
        case "$status" in
            healthy) return 0 ;;
            unhealthy) return 1 ;;
        esac
        sleep 2
        elapsed=$((elapsed + 2))
    done
    return 1
}

start_verification_stack() {
    if ! ensure_port_available "$MYSQL_PORT" || ! ensure_port_available "$ES_PORT"; then
        return 1
    fi
    if ! compose up -d mysql elasticsearch; then
        return 1
    fi
    STACK_STARTED=true
    wait_for_container_health mysql 180 && wait_for_container_health elasticsearch 240
}

stop_verification_stack() {
    if [ "$STACK_STARTED" = false ]; then
        return 0
    fi
    compose down --volumes --remove-orphans
    STACK_STARTED=false
}

check_redis() {
    local key="nebula_e2e_preflight_$RESOURCE_SUFFIX"
    local -a redis_args=(-h "$REDIS_HOST" -p "$REDIS_PORT" --no-auth-warning)
    local version
    local ok=true

    if [ -n "${REDIS_PASSWORD:-}" ]; then
        redis_args+=(-a "$REDIS_PASSWORD")
    fi
    version=$(redis-cli "${redis_args[@]}" INFO server 2>/dev/null |
        sed -n 's/^redis_version://p' | tr -d '\r')
    [ -n "$version" ] || ok=false
    [ "$(redis-cli "${redis_args[@]}" SET "$key" nebula_e2e_value EX 60 2>/dev/null)" = OK ] || ok=false
    [ "$(redis-cli "${redis_args[@]}" GET "$key" 2>/dev/null)" = nebula_e2e_value ] || ok=false
    [ "$(redis-cli "${redis_args[@]}" DEL "$key" 2>/dev/null)" = 1 ] || ok=false
    [ "$(redis-cli "${redis_args[@]}" EXISTS "$key" 2>/dev/null)" = 0 ] || ok=false
    redis-cli "${redis_args[@]}" DEL "$key" >/dev/null 2>&1 || true
    [ "$ok" = true ] || return 1
    record_version redis "$version"
}

check_rabbitmq() {
    local envs
    local user
    local password
    local base="http://$RABBITMQ_HOST:$RABBITMQ_API_PORT/api"
    local queue="nebula_e2e_preflight_$RESOURCE_SUFFIX"
    local create_code
    local publish_response
    local consume_response
    local delete_code
    local post_delete_code
    local version
    local ok=true

    envs=$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$RABBITMQ_CONTAINER" 2>/dev/null) || return 1
    user=$(printf '%s\n' "$envs" | sed -n 's/^RABBITMQ_DEFAULT_USER=//p' | head -n 1)
    password=$(printf '%s\n' "$envs" | sed -n 's/^RABBITMQ_DEFAULT_PASS=//p' | head -n 1)
    [ -n "$user" ] && [ -n "$password" ] || return 1

    version=$(curl -sS --noproxy '*' -u "$user:$password" "$base/overview" | jq -r '.rabbitmq_version // empty')
    create_code=$(curl -sS --noproxy '*' -u "$user:$password" -o /dev/null -w '%{http_code}' \
        -X PUT "$base/queues/%2F/$queue" -H 'Content-Type: application/json' \
        -d '{"auto_delete":false,"durable":false,"arguments":{}}')
    publish_response=$(curl -sS --noproxy '*' -u "$user:$password" \
        -X POST "$base/exchanges/%2F/amq.default/publish" -H 'Content-Type: application/json' \
        -d "{\"properties\":{},\"routing_key\":\"$queue\",\"payload\":\"nebula_e2e_message\",\"payload_encoding\":\"string\"}")
    consume_response=$(curl -sS --noproxy '*' -u "$user:$password" \
        -X POST "$base/queues/%2F/$queue/get" -H 'Content-Type: application/json' \
        -d '{"count":1,"ackmode":"ack_requeue_false","encoding":"auto","truncate":50000}')

    [ "$create_code" = 201 ] || ok=false
    [ "$(printf '%s' "$publish_response" | jq -r '.routed // false')" = true ] || ok=false
    [ "$(printf '%s' "$consume_response" | jq -r '.[0].payload // empty')" = nebula_e2e_message ] || ok=false
    delete_code=$(curl -sS --noproxy '*' -u "$user:$password" -o /dev/null -w '%{http_code}' \
        -X DELETE "$base/queues/%2F/$queue")
    post_delete_code=$(curl -sS --noproxy '*' -u "$user:$password" -o /dev/null -w '%{http_code}' \
        "$base/queues/%2F/$queue")
    [ "$delete_code" = 204 ] || ok=false
    [ "$post_delete_code" = 404 ] || ok=false
    [ "$ok" = true ] && [ -n "$version" ] || return 1
    record_version rabbitmq "$version"
}

check_nacos() {
    local user="${NACOS_USERNAME:-}"
    local password="${NACOS_PASSWORD:-}"
    local login_response
    local token
    local service="nebula_e2e_preflight_$RESOURCE_SUFFIX"
    local base="http://$NACOS_HOST:$NACOS_PORT/nacos/v1"
    local register_response
    local list_response
    local state_response
    local delete_response
    local post_delete_response
    local remaining_hosts=-1
    local service_list_response
    local remaining_services=-1
    local attempt
    local version
    local ok=true

    if [ -z "$user" ]; then
        user=$(container_env_value "$NACOS_CONTAINER" NACOS_USER)
    fi
    [ -n "$user" ] || return 1
    if [ -z "$password" ]; then
        password=$user
    fi

    login_response=$(curl -sS --noproxy '*' -X POST "$base/auth/login" \
        --data-urlencode "username=$user" --data-urlencode "password=$password")
    token=$(printf '%s' "$login_response" | jq -r '.accessToken // empty')
    [ -n "$token" ] || return 1
    register_response=$(curl -sS --noproxy '*' -X POST "$base/ns/instance" \
        --data-urlencode "serviceName=$service" --data-urlencode 'ip=127.0.0.1' \
        --data-urlencode 'port=19091' --data-urlencode 'metadata={"e2e":"true"}' \
        --data-urlencode 'ephemeral=true' --data-urlencode "accessToken=$token")
    list_response=$(curl -sS --noproxy '*' -G "$base/ns/instance/list" \
        --data-urlencode "serviceName=$service" --data-urlencode "accessToken=$token")
    state_response=$(curl -sS --noproxy '*' "$base/console/server/state")
    version=$(printf '%s' "$state_response" | jq -r '.version // empty')

    [ "$register_response" = ok ] || ok=false
    [ "$(printf '%s' "$list_response" | jq -r '.hosts | length')" = 1 ] || ok=false
    [ "$(printf '%s' "$list_response" | jq -r '.hosts[0].metadata.e2e // empty')" = true ] || ok=false
    delete_response=$(curl -sS --noproxy '*' -X DELETE "$base/ns/instance" \
        --data-urlencode "serviceName=$service" --data-urlencode 'ip=127.0.0.1' \
        --data-urlencode 'port=19091' --data-urlencode 'ephemeral=true' \
        --data-urlencode "accessToken=$token")
    [ "$delete_response" = ok ] || ok=false
    for attempt in 1 2 3 4 5 6 7 8 9 10; do
        post_delete_response=$(curl -sS --noproxy '*' -G "$base/ns/instance/list" \
            --data-urlencode "serviceName=$service" --data-urlencode "accessToken=$token")
        remaining_hosts=$(printf '%s' "$post_delete_response" | jq -r '.hosts | length')
        [ "$remaining_hosts" = 0 ] && break
        sleep 1
    done
    [ "$remaining_hosts" = 0 ] || ok=false
    # 临时客户端记录会延迟断开，空服务随后才按 60 秒周期回收。
    for attempt in {1..30}; do
        service_list_response=$(curl -sS --noproxy '*' -G "$base/ns/service/list" \
            --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=1000' \
            --data-urlencode "accessToken=$token")
        remaining_services=$(printf '%s' "$service_list_response" |
            jq --arg service "$service" '[.doms[]? | select(. == $service)] | length')
        [ "$remaining_services" = 0 ] && break
        sleep 5
    done
    [ "$remaining_services" = 0 ] || ok=false
    [ "$ok" = true ] && [ -n "$version" ] || return 1
    record_version nacos "$version"
}

check_minio() {
    local user
    local password
    local image
    local bucket="nebula-e2e-preflight-$RESOURCE_SUFFIX"

    user=$(container_env_value "$MINIO_CONTAINER" MINIO_ROOT_USER)
    password=$(container_env_value "$MINIO_CONTAINER" MINIO_ROOT_PASSWORD)
    image=$(docker inspect --format '{{.Config.Image}}' "$MINIO_CONTAINER" 2>/dev/null)
    [ -n "$user" ] && [ -n "$password" ] && [ -n "$image" ] || return 1

    docker run --rm --entrypoint /bin/sh \
        -e MC_USER="$user" -e MC_PASS="$password" -e MC_BUCKET="$bucket" \
        minio/mc:latest -c '
            set -eu
            mc alias set local http://host.docker.internal:'"$MINIO_PORT"' "$MC_USER" "$MC_PASS" >/dev/null
            cleanup() {
                mc rm --force "local/$MC_BUCKET/nebula_e2e_object.txt" >/dev/null 2>&1 || true
                mc rb --force "local/$MC_BUCKET" >/dev/null 2>&1 || true
            }
            trap cleanup EXIT
            mc mb "local/$MC_BUCKET" >/dev/null
            printf nebula_e2e_payload | mc pipe "local/$MC_BUCKET/nebula_e2e_object.txt" >/dev/null
            test "$(mc cat "local/$MC_BUCKET/nebula_e2e_object.txt")" = nebula_e2e_payload
            mc rm --force "local/$MC_BUCKET/nebula_e2e_object.txt" >/dev/null
            mc rb "local/$MC_BUCKET" >/dev/null
            if mc stat "local/$MC_BUCKET" >/dev/null 2>&1; then
                exit 1
            fi
            trap - EXIT
        ' >/dev/null
    record_version minio "$image"
}

check_chroma() {
    local root="http://$CHROMA_HOST:$CHROMA_PORT/api/v2"
    local base="$root/tenants/default_tenant/databases/default_database/collections"
    local name="nebula_e2e_preflight_$RESOURCE_SUFFIX"
    local created
    local collection_id
    local get_response
    local query_response
    local delete_code
    local remaining
    local version
    local ok=true

    curl -sS --noproxy '*' "$root/heartbeat" | jq -e '.["nanosecond heartbeat"] > 0' >/dev/null || return 1
    version=$(curl -sS --noproxy '*' "$root/version" | tr -d '"\r\n')
    created=$(curl -sS --noproxy '*' -X POST "$base" -H 'Content-Type: application/json' \
        -d "{\"name\":\"$name\",\"get_or_create\":false}")
    collection_id=$(printf '%s' "$created" | jq -r '.id // empty')
    [ -n "$collection_id" ] || return 1

    curl -sS --noproxy '*' -X POST "$base/$collection_id/add" -H 'Content-Type: application/json' \
        -d '{"ids":["nebula_e2e_record"],"embeddings":[[0.1,0.2,0.3]],"documents":["nebula e2e document"],"metadatas":[{"source":"e2e"}]}' >/dev/null || ok=false
    get_response=$(curl -sS --noproxy '*' -X POST "$base/$collection_id/get" -H 'Content-Type: application/json' \
        -d '{"ids":["nebula_e2e_record"],"include":["documents","metadatas"]}')
    query_response=$(curl -sS --noproxy '*' -X POST "$base/$collection_id/query" -H 'Content-Type: application/json' \
        -d '{"query_embeddings":[[0.1,0.2,0.3]],"n_results":1,"include":["documents","distances"]}')
    [ "$(printf '%s' "$get_response" | jq -r '.ids[0] // empty')" = nebula_e2e_record ] || ok=false
    [ "$(printf '%s' "$query_response" | jq -r '.ids[0][0] // empty')" = nebula_e2e_record ] || ok=false
    delete_code=$(curl -sS --noproxy '*' -o /dev/null -w '%{http_code}' -X DELETE "$base/$name")
    remaining=$(curl -sS --noproxy '*' "$base" |
        jq --arg name "$name" '[.[] | select(.name == $name)] | length')
    [ "$delete_code" = 200 ] || ok=false
    [ "$remaining" = 0 ] || ok=false
    [ "$ok" = true ] && [ -n "$version" ] || return 1
    record_version chroma "$version"
}

mysql_exec() {
    compose exec -T mysql mysql --protocol=TCP -h 127.0.0.1 -u root "$@"
}

check_mysql() {
    local version
    local product_count
    local ok=true

    version=$(mysql_exec -Nse 'SELECT VERSION();')
    [ "$(mysql_exec -Nse 'SELECT 1;')" = 1 ] || ok=false
    mysql_exec <"$PROJECT_ROOT/examples/fullstack-example/sql/data-demo-tables.sql" || ok=false
    mysql_exec <"$PROJECT_ROOT/examples/fullstack-example/sql/sharding-tables.sql" || ok=false
    mysql_exec -e 'CREATE DATABASE IF NOT EXISTS oauth_client_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;' || ok=false
    mysql_exec <"$PROJECT_ROOT/examples/oauth-example/backend/sql/init.sql" || ok=false
    mysql_exec -e "INSERT INTO nebula_example.t_product (name, description, price, category, stock_quantity, status) VALUES ('nebula_e2e_preflight', 'protocol check', 1.00, 'e2e', 1, 1);" || ok=false
    product_count=$(mysql_exec -Nse "SELECT COUNT(*) FROM nebula_example.t_product WHERE name='nebula_e2e_preflight';")
    [ "$product_count" = 1 ] || ok=false
    mysql_exec -e "DELETE FROM nebula_example.t_product WHERE name='nebula_e2e_preflight';" || ok=false
    [ "$(mysql_exec -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema IN ('nebula_example','nebula_sharding_0','nebula_sharding_1','oauth_client_demo');")" -ge 7 ] || ok=false
    [ "$ok" = true ] && [ -n "$version" ] || return 1
    record_version mysql "$version"
}

check_elasticsearch() {
    local base="http://127.0.0.1:$ES_PORT"
    local index="nebula_e2e_preflight_$(printf '%s' "$RESOURCE_SUFFIX" | tr '-' '_')"
    local version
    local create_code
    local search_response
    local delete_code
    local ok=true

    version=$(curl -sS --noproxy '*' "$base" | jq -r '.version.number // empty')
    create_code=$(curl -sS --noproxy '*' -o /dev/null -w '%{http_code}' -X PUT "$base/$index" \
        -H 'Content-Type: application/json' -d '{"mappings":{"properties":{"message":{"type":"text"}}}}')
    curl -sS --noproxy '*' -X PUT "$base/$index/_doc/nebula_e2e_doc?refresh=true" \
        -H 'Content-Type: application/json' -d '{"message":"nebula e2e search document"}' >/dev/null || ok=false
    search_response=$(curl -sS --noproxy '*' "$base/$index/_search?q=message:nebula")
    delete_code=$(curl -sS --noproxy '*' -o /dev/null -w '%{http_code}' -X DELETE "$base/$index")
    [ "$version" = 9.4.2 ] || ok=false
    [ "$create_code" = 200 ] || ok=false
    [ "$(printf '%s' "$search_response" | jq -r '.hits.total.value // 0')" = 1 ] || ok=false
    [ "$delete_code" = 200 ] || ok=false
    [ "$ok" = true ] || return 1
    record_version elasticsearch "$version"
}

check_xxl_job() {
    local status
    local health
    status=$(curl -sS --noproxy '*' -o /dev/null -w '%{http_code}' http://127.0.0.1:9001/xxl-job-admin/)
    health=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' nebula-xxl-job 2>/dev/null)
    record_version xxl-job "http-$status-container-$health"
    [ "$status" = 200 ] || [ "$status" = 302 ]
}

middleware_exit() {
    local exit_code=$?
    set +e
    if [ "$KEEP_CONTAINERS" != true ]; then
        stop_verification_stack >/dev/null 2>&1
    fi
    cleanup "$exit_code"
    return "$exit_code"
}

trap middleware_exit EXIT

: >"$VERSIONS_FILE"
log_info "========== 中间件协议级预检 =========="
log_info "隔离 Compose 项目：$VERIFICATION_PROJECT"

if start_verification_stack; then
    record_pass "隔离 MySQL 8.3 和 Elasticsearch 9.4.2 已就绪"
else
    record_fail "隔离 MySQL 或 Elasticsearch 启动失败"
fi

run_checked "Redis SET/GET/DEL 协议检查" check_redis
run_checked "RabbitMQ 创建、发布、消费和删除临时队列" check_rabbitmq
run_checked "Nacos 登录、注册、查询和注销临时实例" check_nacos
run_checked "MinIO 创建、上传、下载和删除临时对象" check_minio
run_checked "Chroma collection 写入、查询和删除" check_chroma
run_checked "MySQL 初始化示例库并完成真实读写" check_mysql
run_checked "Elasticsearch 9.4.2 索引读写和删除" check_elasticsearch
run_checked "XXL-JOB 宿主机 HTTP 探针" check_xxl_job

if [ "$KEEP_CONTAINERS" = true ]; then
    record_pass "隔离容器按调用方要求保留"
elif stop_verification_stack; then
    record_pass "隔离容器和独立卷已删除"
else
    record_fail "隔离容器或独立卷清理失败"
fi

print_summary "middleware-preflight"
