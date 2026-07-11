#!/usr/bin/env bash
# oauth-example E2E：使用隔离 MySQL 验证客户端，真实提供方不可用时明确阻塞。
source "$(dirname "$0")/../e2e-common.sh"

PORT="${OAUTH_BACKEND_PORT:-8081}"
FRONTEND_PORT="${OAUTH_FRONTEND_PORT:-4010}"
MYSQL_PORT="${OAUTH_E2E_MYSQL_PORT:-13306}"
COMPOSE_FILE="$PROJECT_ROOT/docker/verification/docker-compose.yml"
COMPOSE_PROJECT="nebula-oauth-${E2E_RUN_ID//[^a-zA-Z0-9]/-}"
COMPOSE_STARTED=0

mysql_healthy() {
    [ "$(docker inspect "$MYSQL_CONTAINER" --format '{{.State.Health.Status}}' 2>/dev/null || true)" = "healthy" ]
}

cleanup_oauth() {
    local exit_code=${1:-$?}
    trap - EXIT
    set +e
    if [ "$COMPOSE_STARTED" -eq 1 ]; then
        NEBULA_E2E_MYSQL_PORT="$MYSQL_PORT" docker compose -p "$COMPOSE_PROJECT" \
            -f "$COMPOSE_FILE" down -v --remove-orphans >/dev/null 2>&1 || true
    fi
    cleanup "$exit_code"
    exit "$exit_code"
}
trap 'cleanup_oauth $?' EXIT

log_info "========== oauth-example E2E =========="
require_command docker
require_command npm
require_command jq
ensure_port_available "$PORT"
ensure_port_available "$FRONTEND_PORT"
ensure_port_available "$MYSQL_PORT"

NEBULA_E2E_MYSQL_PORT="$MYSQL_PORT" docker compose -p "$COMPOSE_PROJECT" \
    -f "$COMPOSE_FILE" up -d mysql
COMPOSE_STARTED=1
MYSQL_CONTAINER=$(NEBULA_E2E_MYSQL_PORT="$MYSQL_PORT" docker compose -p "$COMPOSE_PROJECT" \
    -f "$COMPOSE_FILE" ps -q mysql)
if wait_until "隔离 MySQL 未就绪" 120 2 mysql_healthy; then
    record_pass "隔离 MySQL 8.3 已就绪"
else
    record_fail "隔离 MySQL 8.3 启动失败"
fi

docker exec -i "$MYSQL_CONTAINER" mysql -uroot \
    -e 'DROP DATABASE IF EXISTS oauth_client_demo; CREATE DATABASE oauth_client_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
docker exec -i "$MYSQL_CONTAINER" mysql -uroot oauth_client_demo \
    < "$PROJECT_ROOT/examples/oauth-example/backend/sql/init.sql"
record_pass "OAuth 隔离数据库初始化完成"

export OAUTH_DB_URL="jdbc:mysql://localhost:$MYSQL_PORT/oauth_client_demo?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
export OAUTH_DB_USERNAME=root
export OAUTH_DB_PASSWORD=""
export OAUTH_SERVER_URL="${OAUTH_SERVER_URL:-http://localhost:8080}"
export OAUTH_CLIENT_ID="${OAUTH_CLIENT_ID:-nebula_e2e_client}"
export OAUTH_CLIENT_SECRET="${OAUTH_CLIENT_SECRET:-nebula_e2e_rotated_secret}"
export OAUTH_REDIRECT_URI="http://localhost:$PORT/api/oauth/callback"
export OAUTH_FRONTEND_URL="http://localhost:$FRONTEND_PORT"
export OAUTH_JWT_SECRET="${OAUTH_JWT_SECRET:-nebula-e2e-oauth-jwt-secret-at-least-32-bytes}"

start_app "examples/oauth-example/backend" "$PORT"
assert_contains "OAuth 后端健康检查" "http://localhost:$PORT/health/ping" 'pong'
assert_json "授权入口生成带 state 的提供方地址" \
    "http://localhost:$PORT/api/oauth/authorize" \
    '.success == true and (.data.authUrl | startswith("'"$OAUTH_SERVER_URL"'/oauth/authorize?")) and (.data.authUrl | contains("state="))'
CALLBACK_HEADERS="$E2E_CASE_DIR/error-callback.headers"
CALLBACK_STATUS=$(curl -sS --noproxy '*' -o /dev/null -D "$CALLBACK_HEADERS" -w '%{http_code}' \
    "http://localhost:$PORT/api/oauth/callback?error=access_denied&error_description=e2e_denied")
if [ "$CALLBACK_STATUS" = "302" ] && grep -Fiq \
    "location: http://localhost:$FRONTEND_PORT/oauth/result?success=false" "$CALLBACK_HEADERS"; then
    record_pass "错误回调无需 code 即可重定向前端"
else
    record_fail "错误回调状态或 Location 不符合预期"
fi
assert_json "未登录用户被业务层拒绝" \
    "http://localhost:$PORT/api/oauth/user/current" \
    '.success == false and .code == "401"'

FRONTEND_DIR="$PROJECT_ROOT/examples/oauth-example/frontend"
run_checked "OAuth 前端 npm ci 成功" npm --prefix "$FRONTEND_DIR" ci
run_checked "OAuth 前端生产构建成功" npm --prefix "$FRONTEND_DIR" run build

if curl -fsS --noproxy '*' --connect-timeout 2 --max-time 3 "$OAUTH_SERVER_URL/" >/dev/null 2>&1; then
    record_pass "Vocoor OAuth 提供方可访问"
else
    skip_test "真实 OAuth 授权码与浏览器流程" \
        "Vocoor 提供方 $OAUTH_SERVER_URL 不可达，且外部仓库依赖现有业务中间件，禁止非隔离启动"
fi

stop_all_apps true
NEBULA_E2E_MYSQL_PORT="$MYSQL_PORT" docker compose -p "$COMPOSE_PROJECT" \
    -f "$COMPOSE_FILE" down -v --remove-orphans >/dev/null
COMPOSE_STARTED=0
if ! docker inspect "$MYSQL_CONTAINER" >/dev/null 2>&1 && wait_for_port_release "$MYSQL_PORT" 20; then
    record_pass "OAuth 隔离 MySQL、卷和端口已清理"
else
    record_fail "OAuth 隔离 MySQL 资源未清理"
fi

print_summary "oauth-example"
