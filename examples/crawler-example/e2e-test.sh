#!/usr/bin/env bash
# crawler-example E2E：验证 HTTP 与远程 Browser 两种爬虫引擎。
source "$(dirname "$0")/../e2e-common.sh"

PORT="${CRAWLER_APP_PORT:-8085}"
FIXTURE_PORT="${CRAWLER_FIXTURE_PORT:-18085}"
BROWSER_PORT="${CRAWLER_BROWSER_PORT:-19222}"
BROWSER_CONTAINER="crawler-browser-01"
COMPOSE_FILE="$PROJECT_ROOT/docker/crawler-browser/docker-compose.yml"
FIXTURE_PID=""
BROWSER_CREATED=0
NETWORK_CREATED=0

cleanup_crawler_resources() {
    local exit_code=${1:-$?}
    trap - EXIT
    set +e
    if [ "$BROWSER_CREATED" -eq 1 ]; then
        docker rm -f "$BROWSER_CONTAINER" >/dev/null 2>&1 || true
        BROWSER_CREATED=0
    fi
    if [ "$NETWORK_CREATED" -eq 1 ]; then
        docker network rm crawler-network >/dev/null 2>&1 || true
        NETWORK_CREATED=0
    fi
    if [ -n "$FIXTURE_PID" ] && pid_is_running "$FIXTURE_PID"; then
        kill -TERM "$FIXTURE_PID" >/dev/null 2>&1 || true
        wait "$FIXTURE_PID" 2>/dev/null || true
    fi
    FIXTURE_PID=""
    cleanup "$exit_code"
    exit "$exit_code"
}
trap 'cleanup_crawler_resources $?' EXIT

log_info "========== crawler-example E2E =========="
require_command python3
require_command docker
require_command jq
ensure_port_available "$PORT"
ensure_port_available "$FIXTURE_PORT"
ensure_port_available "$BROWSER_PORT"

FIXTURE_LOG="$E2E_CASE_DIR/fixture-server.log"
python3 "$PROJECT_ROOT/examples/crawler-example/e2e-fixture-server.py" "$FIXTURE_PORT" >"$FIXTURE_LOG" 2>&1 &
FIXTURE_PID=$!
if wait_for_port "$FIXTURE_PORT" 15 "$FIXTURE_PID"; then
    record_pass "本地受控页面服务已启动"
else
    record_fail "本地受控页面服务启动失败"
fi

if docker inspect "$BROWSER_CONTAINER" >/dev/null 2>&1; then
    record_fail "浏览器容器名 $BROWSER_CONTAINER 已被占用，拒绝操作非受管容器"
    print_summary "crawler-example"
    exit $?
fi
if ! docker network inspect crawler-network >/dev/null 2>&1; then
    NETWORK_CREATED=1
fi

CRAWLER_BROWSER_PORT="$BROWSER_PORT" docker compose -f "$COMPOSE_FILE" up -d crawler-browser-01
BROWSER_CREATED=1
if wait_until "Playwright Server HTTP 入口未就绪" 30 1 \
    curl -sf --noproxy '*' "http://localhost:$BROWSER_PORT/" -o "$E2E_CASE_DIR/playwright-endpoint.txt"; then
    record_pass "Playwright Server HTTP 入口可用"
else
    record_fail "Playwright Server HTTP 入口不可用"
fi

run_checked "Playwright 容器镜像版本为 1.0.0" \
    test "$(docker inspect "$BROWSER_CONTAINER" --format '{{index .Config.Labels "version"}}')" = "1.0.0"
run_checked "Playwright 运行时版本与 Java 客户端一致" \
    sh -c "docker exec '$BROWSER_CONTAINER' npx playwright --version | grep -Fx 'Version 1.41.0'"
docker logs "$BROWSER_CONTAINER" >"$E2E_CASE_DIR/playwright-container.log" 2>&1
assert_file_contains "Playwright HTTP 入口声明运行中" \
    "$E2E_CASE_DIR/playwright-endpoint.txt" '^Running$'
assert_file_contains "Playwright 容器监听 WebSocket 地址" \
    "$E2E_CASE_DIR/playwright-container.log" 'Listening on ws://0\.0\.0\.0:9222/'

export BROWSER_CRAWLER_ENABLED=true
export PLAYWRIGHT_SERVER=localhost
export PLAYWRIGHT_PORT="$BROWSER_PORT"
export PLAYWRIGHT_USE_CDP=false
export PLAYWRIGHT_CONNECTIONS=1
start_app "examples/crawler-example" "$PORT"

STATIC_URL="http://localhost:$FIXTURE_PORT/static"
SECOND_URL="http://localhost:$FIXTURE_PORT/static-two"
SLOW_URL="http://localhost:$FIXTURE_PORT/slow"
DYNAMIC_URL="http://host.docker.internal:$FIXTURE_PORT/dynamic"

assert_json "健康检查确认 HTTP 与 Browser 引擎可用" \
    "http://localhost:$PORT/crawler/health" \
    '.success == true and .data.httpEngine.healthy == true and .data.browserEngine.healthy == true'

assert_json "HTTP 单页抓取返回受控内容" \
    "http://localhost:$PORT/crawler/crawl" \
    '.success == true and .data.success == true and .data.statusCode == 200 and (.data.contentPreview | contains("nebula_e2e_static_content"))' \
    POST "{\"url\":\"$STATIC_URL\",\"method\":\"GET\",\"timeout\":2000}"

assert_json "HTTP 批量抓取保留两个独立响应" \
    "http://localhost:$PORT/crawler/batch" \
    '.success == true and (.data | length) == 2 and all(.data[]; .success == true and .statusCode == 200) and (.data[0].contentPreview | contains("nebula_e2e_static_content")) and (.data[1].contentPreview | contains("nebula_e2e_batch_second"))' \
    POST "{\"urls\":[\"$STATIC_URL\",\"$SECOND_URL\"],\"timeout\":2000}"

assert_json "HTTP 解析提取标题、描述和绝对链接" \
    "http://localhost:$PORT/crawler/parse?url=$STATIC_URL" \
    '.success == true and .data.success == true and .data.title == "Nebula E2E Static" and .data.description == "nebula_e2e_static_description" and .data.totalLinks == 2 and .data.links[0].href == "http://localhost:'"$FIXTURE_PORT"'/alpha"'

assert_json "HTTP 超时返回明确失败" \
    "http://localhost:$PORT/crawler/crawl" \
    '.success == true and .data.success == false and (.data.errorMessage | length) > 0' \
    POST "{\"url\":\"$SLOW_URL\",\"method\":\"GET\",\"timeout\":100}"

assert_json "非法 URL 返回明确失败" \
    "http://localhost:$PORT/crawler/crawl" \
    '.success == true and .data.success == false and (.data.errorMessage | length) > 0' \
    POST '{"url":"not-a-valid-url","method":"GET","timeout":500}'

assert_json "Browser 引擎获取 JavaScript 渲染后的 DOM 和截图" \
    "http://localhost:$PORT/crawler/browser" \
    '.success == true and .data.success == true and .data.statusCode == 200 and .data.engineType == "BROWSER" and .data.title == "Nebula E2E Browser" and (.data.contentPreview | contains("nebula_e2e_browser_rendered")) and (.data.screenshotSize | test("^[1-9][0-9]* bytes$"))' \
    POST "{\"url\":\"$DYNAMIC_URL\",\"waitUntil\":\"domcontentloaded\",\"waitSelector\":\"#dynamic-result[data-ready='true']\",\"waitTimeout\":5000,\"screenshot\":true}"

stop_all_apps true

docker rm -f "$BROWSER_CONTAINER" >/dev/null
BROWSER_CREATED=0
if ! docker inspect "$BROWSER_CONTAINER" >/dev/null 2>&1 && wait_for_port_release "$BROWSER_PORT" 15; then
    record_pass "Playwright 容器和宿主端口已清理"
else
    record_fail "Playwright 容器或宿主端口未清理"
fi
if [ "$NETWORK_CREATED" -eq 1 ]; then
    docker network rm crawler-network >/dev/null 2>&1 || true
    NETWORK_CREATED=0
fi

kill -TERM "$FIXTURE_PID" >/dev/null 2>&1 || true
wait "$FIXTURE_PID" 2>/dev/null || true
FIXTURE_PID=""
if wait_for_port_release "$FIXTURE_PORT" 15; then
    record_pass "本地受控页面端口已释放"
else
    record_fail "本地受控页面端口未释放"
fi

print_summary "crawler-example"
