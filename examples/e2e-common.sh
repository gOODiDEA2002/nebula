#!/usr/bin/env bash
# 示例应用 E2E 测试通用函数库
# 使用方式：source "$(dirname "$0")/../e2e-common.sh"

set -euo pipefail

PASS=0
FAIL=0
SKIP=0
BLOCKED=0
TOTAL=0
APP_PID=""
APP_PIDS=()
APP_PORTS=()
APP_LOGS=()
HTTP_REQUEST_COUNT=0
HTTP_STATUS=""
HTTP_BODY_FILE=""
SUMMARY_PRINTED=0
LOGS_CHECKED=0
CLEANUP_RUNNING=0

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
E2E_MODE="${E2E_MODE:-smoke}"
E2E_RUN_ID="${E2E_RUN_ID:-$(date '+%Y%m%d-%H%M%S')-$$}"
E2E_RESULTS_ROOT="${E2E_RESULTS_ROOT:-$PROJECT_ROOT/target/example-e2e}"
E2E_RESULTS_DIR="${E2E_RESULTS_DIR:-$E2E_RESULTS_ROOT/$E2E_RUN_ID}"
E2E_TEST_NAME="${E2E_TEST_NAME:-$(basename "$(cd "$(dirname "$0")" && pwd)")}"
E2E_CASE_NAME="$(printf '%s' "$E2E_TEST_NAME" | tr '/ :.' '----' | tr -cd '[:alnum:]_-')"
E2E_CASE_DIR="${E2E_CASE_DIR:-$E2E_RESULTS_DIR/cases/$E2E_CASE_NAME}"
E2E_START_TIMEOUT="${E2E_START_TIMEOUT:-120}"
E2E_STOP_TIMEOUT="${E2E_STOP_TIMEOUT:-15}"

case "$E2E_MODE" in
    smoke|full) ;;
    *)
        echo "[FAIL] E2E_MODE 仅支持 smoke 或 full，实际值：$E2E_MODE" >&2
        exit 2
        ;;
esac

mkdir -p "$E2E_RESULTS_DIR" "$E2E_CASE_DIR"

if [ -t 1 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[0;33m'
    CYAN='\033[0;36m'
    NC='\033[0m'
else
    RED=''
    GREEN=''
    YELLOW=''
    CYAN=''
    NC=''
fi

log_info() { printf '%b[INFO]%b %s\n' "$CYAN" "$NC" "$*"; }
log_pass() { printf '%b[PASS]%b %s\n' "$GREEN" "$NC" "$*"; }
log_fail() { printf '%b[FAIL]%b %s\n' "$RED" "$NC" "$*" >&2; }
log_warn() { printf '%b[WARN]%b %s\n' "$YELLOW" "$NC" "$*"; }
log_skip() { printf '%b[SKIP]%b %s\n' "$YELLOW" "$NC" "$*"; }

safe_name() {
    printf '%s' "$1" | tr '/ :.' '----' | tr -cd '[:alnum:]_-'
}

record_pass() {
    local desc=$1
    TOTAL=$((TOTAL + 1))
    PASS=$((PASS + 1))
    log_pass "$desc"
}

record_fail() {
    local desc=$1
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
    log_fail "$desc"
}

require_command() {
    local command_name=$1
    if command -v "$command_name" >/dev/null 2>&1; then
        log_info "命令 $command_name 可用"
        return 0
    fi
    log_fail "缺少命令：$command_name"
    return 1
}

# TCP 探测只用于快速判断依赖是否存在，不能作为中间件功能通过的证据。
require_service() {
    local name=$1
    local host=$2
    local port=$3
    if nc -z "$host" "$port" >/dev/null 2>&1; then
        log_info "外部服务 $name ($host:$port) 可连接"
        return 0
    fi
    log_warn "外部服务 $name ($host:$port) 不可连接"
    return 1
}

require_env() {
    local variable_name=$1
    if [ -n "${!variable_name:-}" ]; then
        log_info "环境变量 $variable_name 已设置"
        return 0
    fi
    log_warn "环境变量 $variable_name 未设置"
    return 1
}

skip_if_no_env() {
    local variable_name=$1
    local test_name=$2

    if require_env "$variable_name"; then
        return 0
    fi

    TOTAL=$((TOTAL + 1))
    if [ "$E2E_MODE" = "full" ]; then
        BLOCKED=$((BLOCKED + 1))
        log_fail "完整验证缺少环境变量：$variable_name"
        print_summary "$test_name" || true
        exit 2
    fi

    SKIP=$((SKIP + 1))
    log_skip "跳过 ${test_name}：需要环境变量 $variable_name"
    print_summary "$test_name"
    exit 0
}

skip_if_no_service() {
    local name=$1
    local host=$2
    local port=$3
    local test_name=$4

    if require_service "$name" "$host" "$port"; then
        return 0
    fi

    TOTAL=$((TOTAL + 1))
    if [ "$E2E_MODE" = "full" ]; then
        BLOCKED=$((BLOCKED + 1))
        log_fail "完整验证缺少依赖：$name ($host:$port)"
        print_summary "$test_name" || true
        exit 2
    fi

    SKIP=$((SKIP + 1))
    log_skip "跳过 ${test_name}：需要 $name ($host:$port)"
    print_summary "$test_name"
    exit 0
}

ensure_port_available() {
    local port=$1
    local owners
    owners=$(lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
    if [ -z "$owners" ]; then
        return 0
    fi

    log_fail "端口 $port 已被其他进程占用，拒绝启动测试应用"
    printf '%s\n' "$owners" >&2
    return 1
}

wait_for_port() {
    local port=$1
    local timeout=${2:-60}
    local pid=${3:-}
    local elapsed=0

    log_info "等待端口 $port 就绪，超时 ${timeout}s"
    while ! nc -z localhost "$port" >/dev/null 2>&1; do
        if [ -n "$pid" ] && ! pid_is_running "$pid"; then
            log_fail "应用进程 $pid 已退出，端口 $port 尚未就绪"
            return 1
        fi
        if [ "$elapsed" -ge "$timeout" ]; then
            log_fail "端口 $port 在 ${timeout}s 内未就绪"
            return 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    log_info "端口 $port 已就绪，耗时 ${elapsed}s"
}

wait_for_port_release() {
    local port=$1
    local timeout=${2:-15}
    local elapsed=0

    while lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; do
        if [ "$elapsed" -ge "$timeout" ]; then
            return 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
}

wait_for_log() {
    local log_file=$1
    local pattern=$2
    local timeout=${3:-60}
    local pid=${4:-}
    local elapsed=0

    log_info "等待日志匹配「${pattern}」，超时 ${timeout}s"
    while ! grep -Eq "$pattern" "$log_file" 2>/dev/null; do
        if [ -n "$pid" ] && ! pid_is_running "$pid"; then
            log_fail "应用进程 $pid 已退出，日志未匹配「${pattern}」"
            return 1
        fi
        if [ "$elapsed" -ge "$timeout" ]; then
            log_fail "日志在 ${timeout}s 内未匹配「${pattern}」"
            return 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    log_info "日志匹配成功，耗时 ${elapsed}s"
}

wait_for_app_started() {
    local log_file=$1
    local pid=$2
    local timeout=${3:-120}
    local elapsed=0
    local failure_pattern='APPLICATION FAILED|cancelling refresh attempt|OutOfMemoryError'

    while true; do
        if grep -Eq "$failure_pattern" "$log_file" 2>/dev/null; then
            log_fail "应用启动日志包含失败标记"
            return 1
        fi
        if grep -Eq '(^|[[:space:]])Started [^[:space:]]+' "$log_file" 2>/dev/null; then
            log_info "应用启动日志已出现 Started，耗时 ${elapsed}s"
            return 0
        fi
        if ! pid_is_running "$pid"; then
            log_fail "应用进程 $pid 在 Started 日志出现前退出"
            return 1
        fi
        if [ "$elapsed" -ge "$timeout" ]; then
            log_fail "应用在 ${timeout}s 内未完成启动"
            return 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
}

collect_process_tree() {
    local parent=$1
    local children
    local child
    children=$(pgrep -P "$parent" 2>/dev/null || true)
    for child in $children; do
        collect_process_tree "$child"
    done
    PROCESS_TREE_PIDS+=("$parent")
}

pid_is_running() {
    local pid=$1
    local state
    if ! kill -0 "$pid" 2>/dev/null; then
        return 1
    fi
    state=$(ps -o stat= -p "$pid" 2>/dev/null | tr -d '[:space:]')
    [ -n "$state" ] && [ "${state#Z}" = "$state" ]
}

terminate_process_tree() {
    local root_pid=$1
    local elapsed=0
    local pid
    local alive
    PROCESS_TREE_PIDS=()

    if ! pid_is_running "$root_pid"; then
        wait "$root_pid" 2>/dev/null || true
        return 0
    fi

    collect_process_tree "$root_pid"
    for pid in "${PROCESS_TREE_PIDS[@]}"; do
        kill -TERM "$pid" 2>/dev/null || true
    done

    while [ "$elapsed" -lt "$E2E_STOP_TIMEOUT" ]; do
        alive=false
        for pid in "${PROCESS_TREE_PIDS[@]}"; do
            if pid_is_running "$pid"; then
                alive=true
                break
            fi
        done
        if [ "$alive" = false ]; then
            break
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done

    for pid in "${PROCESS_TREE_PIDS[@]}"; do
        if pid_is_running "$pid"; then
            log_warn "进程 $pid 未在 ${E2E_STOP_TIMEOUT}s 内退出，仅对该测试进程发送 KILL"
            kill -KILL "$pid" 2>/dev/null || true
        fi
    done
    wait "$root_pid" 2>/dev/null || true
}

start_app() {
    local module_path=$1
    local port=${2:-8080}
    local extra_args=${3:-}
    local module_name
    local log_file
    local -a mvn_args=()

    module_name=$(safe_name "$module_path")
    log_file="$E2E_CASE_DIR/${module_name}-${port}.log"
    CURRENT_APP_LOG="$log_file"

    if ! ensure_port_available "$port"; then
        return 1
    fi

    if [ -n "$extra_args" ]; then
        read -r -a mvn_args <<< "$extra_args"
    fi

    log_info "启动 ${module_path}，端口 ${port}，日志 $log_file"
    (
        cd "$PROJECT_ROOT"
        exec mvn -q -f "$module_path" spring-boot:run "${mvn_args[@]}"
    ) >"$log_file" 2>&1 &

    APP_PID=$!
    APP_PIDS+=("$APP_PID")
    APP_PORTS+=("$port")
    APP_LOGS+=("$log_file")

    if ! wait_for_app_started "$log_file" "$APP_PID" "$E2E_START_TIMEOUT"; then
        log_fail "应用 $module_path 启动失败，详见 $log_file"
        tail -n 40 "$log_file" >&2 || true
        return 1
    fi
    if ! wait_for_port "$port" 30 "$APP_PID"; then
        log_fail "应用 $module_path 已打印 Started，但端口 $port 未监听"
        tail -n 40 "$log_file" >&2 || true
        return 1
    fi

    log_info "应用 $module_path 已启动，受管 PID=$APP_PID"
}

assert_app_startup_failure() {
    local desc=$1
    local module_path=$2
    local port=$3
    local expected_pattern=$4
    local timeout=${5:-30}
    local module_name
    local log_file
    local pid
    local exit_code
    local elapsed=0

    module_name=$(safe_name "$module_path")
    log_file="$E2E_CASE_DIR/${module_name}-${port}-expected-failure.log"

    if ! ensure_port_available "$port"; then
        record_fail "${desc}：测试端口 $port 不可用"
        return 0
    fi

    log_info "验证应用启动失败：${module_path}，端口 ${port}，日志 $log_file"
    (
        cd "$PROJECT_ROOT"
        exec mvn -q -f "$module_path" spring-boot:run
    ) >"$log_file" 2>&1 &

    pid=$!
    APP_PIDS+=("$pid")
    APP_PORTS+=("$port")

    while pid_is_running "$pid" && [ "$elapsed" -lt "$timeout" ]; do
        if nc -z localhost "$port" >/dev/null 2>&1; then
            terminate_process_tree "$pid"
            record_fail "${desc}：应用意外监听端口 $port"
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done

    if pid_is_running "$pid"; then
        terminate_process_tree "$pid"
        record_fail "${desc}：应用未在 ${timeout}s 内快速失败"
        return 0
    fi

    if wait "$pid"; then
        exit_code=0
    else
        exit_code=$?
    fi

    if [ "$exit_code" -ne 0 ] &&
        ! grep -Eq '(^|[[:space:]])Started [^[:space:]]+' "$log_file" 2>/dev/null &&
        grep -Eq "$expected_pattern" "$log_file" 2>/dev/null; then
        record_pass "${desc}：非 0 退出，且错误原因符合预期"
        return 0
    fi

    record_fail "${desc}：退出码 $exit_code 或失败日志不符合预期，详见 $log_file"
    return 0
}

stop_app() {
    local pid=${1:-$APP_PID}
    if [ -z "$pid" ]; then
        return 0
    fi
    log_info "停止受管应用，PID=$pid"
    terminate_process_tree "$pid"
    if [ "$pid" = "$APP_PID" ]; then
        APP_PID=""
    fi
}

stop_all_apps() {
    local record_cleanup=${1:-false}
    local index
    local port

    for ((index=${#APP_PIDS[@]} - 1; index >= 0; index--)); do
        if pid_is_running "${APP_PIDS[$index]}"; then
            stop_app "${APP_PIDS[$index]}"
        else
            wait "${APP_PIDS[$index]}" 2>/dev/null || true
        fi
    done

    for port in "${APP_PORTS[@]}"; do
        [ -n "$port" ] || continue
        if wait_for_port_release "$port" "$E2E_STOP_TIMEOUT"; then
            if [ "$record_cleanup" = true ]; then
                record_pass "端口 $port 已释放"
            fi
        elif [ "$record_cleanup" = true ]; then
            record_fail "端口 $port 未在 ${E2E_STOP_TIMEOUT}s 内释放"
        else
            log_fail "端口 $port 未在 ${E2E_STOP_TIMEOUT}s 内释放"
        fi
    done

    APP_PIDS=()
    APP_PORTS=()
    APP_PID=""
}

perform_request() {
    local method=$1
    local url=$2
    local data=${3:-}
    shift 3 || true
    local header
    local -a curl_args

    HTTP_REQUEST_COUNT=$((HTTP_REQUEST_COUNT + 1))
    HTTP_BODY_FILE="$E2E_CASE_DIR/http-$(printf '%04d' "$HTTP_REQUEST_COUNT").body"
    curl_args=(--silent --show-error --noproxy '*' --connect-timeout 5 --max-time 30
        -X "$method" -o "$HTTP_BODY_FILE" -w '%{http_code}')

    if [ -n "$data" ]; then
        curl_args+=(-H 'Content-Type: application/json' --data "$data")
    fi
    for header in "$@"; do
        curl_args+=(-H "$header")
    done

    if HTTP_STATUS=$(curl "${curl_args[@]}" "$url"); then
        return 0
    fi
    HTTP_STATUS=000
    return 1
}

assert_contains() {
    local desc=$1
    local url=$2
    local expected=$3
    local method=${4:-GET}
    local data=${5:-}
    local expected_status=${6:-200}

    TOTAL=$((TOTAL + 1))
    perform_request "$method" "$url" "$data" "${@:7}" || true
    if [ "$HTTP_STATUS" = "$expected_status" ] && grep -Fq "$expected" "$HTTP_BODY_FILE" 2>/dev/null; then
        PASS=$((PASS + 1))
        log_pass "${desc}，HTTP $HTTP_STATUS"
        return 0
    fi

    FAIL=$((FAIL + 1))
    log_fail "${desc}，期望 HTTP $expected_status 且响应包含「${expected}」，实际 HTTP $HTTP_STATUS"
    log_fail "响应证据：$HTTP_BODY_FILE"
    return 0
}

assert_status() {
    local desc=$1
    local url=$2
    local expected_status=${3:-200}
    local method=${4:-GET}
    local data=${5:-}

    TOTAL=$((TOTAL + 1))
    perform_request "$method" "$url" "$data" "${@:6}" || true
    if [ "$HTTP_STATUS" = "$expected_status" ]; then
        PASS=$((PASS + 1))
        log_pass "${desc}，HTTP $HTTP_STATUS"
        return 0
    fi

    FAIL=$((FAIL + 1))
    log_fail "${desc}，期望 HTTP ${expected_status}，实际 HTTP $HTTP_STATUS"
    log_fail "响应证据：$HTTP_BODY_FILE"
    return 0
}

assert_not_contains() {
    local desc=$1
    local url=$2
    local unexpected=$3
    local method=${4:-GET}
    local data=${5:-}
    local expected_status=${6:-200}

    TOTAL=$((TOTAL + 1))
    perform_request "$method" "$url" "$data" "${@:7}" || true
    if [ "$HTTP_STATUS" = "$expected_status" ] && ! grep -Fq "$unexpected" "$HTTP_BODY_FILE" 2>/dev/null; then
        PASS=$((PASS + 1))
        log_pass "${desc}，HTTP $HTTP_STATUS"
        return 0
    fi

    FAIL=$((FAIL + 1))
    log_fail "${desc}，期望 HTTP $expected_status 且响应不包含「${unexpected}」，实际 HTTP $HTTP_STATUS"
    log_fail "响应证据：$HTTP_BODY_FILE"
    return 0
}

assert_json() {
    local desc=$1
    local url=$2
    local jq_filter=$3
    local method=${4:-GET}
    local data=${5:-}
    local expected_status=${6:-200}

    TOTAL=$((TOTAL + 1))
    perform_request "$method" "$url" "$data" "${@:7}" || true
    if [ "$HTTP_STATUS" = "$expected_status" ] && jq -e "$jq_filter" "$HTTP_BODY_FILE" >/dev/null 2>&1; then
        PASS=$((PASS + 1))
        log_pass "${desc}，HTTP ${HTTP_STATUS}，JSON 断言通过"
        return 0
    fi

    FAIL=$((FAIL + 1))
    log_fail "${desc}，期望 HTTP $expected_status 且 JSON 满足「${jq_filter}」，实际 HTTP $HTTP_STATUS"
    log_fail "响应证据：$HTTP_BODY_FILE"
    return 0
}

assert_started() {
    local desc=$1
    local log_file=$2
    TOTAL=$((TOTAL + 1))
    if grep -Eq '(^|[[:space:]])Started [^[:space:]]+' "$log_file" 2>/dev/null &&
        ! grep -Eq 'APPLICATION FAILED|cancelling refresh attempt|OutOfMemoryError' "$log_file" 2>/dev/null; then
        PASS=$((PASS + 1))
        log_pass "$desc"
        return 0
    fi
    FAIL=$((FAIL + 1))
    log_fail "${desc}，详见 $log_file"
    return 0
}

assert_file_exists() {
    local desc=$1
    local path=$2
    if [ -f "$path" ]; then
        record_pass "$desc"
        return 0
    fi
    record_fail "${desc}：文件不存在 $path"
    return 0
}

assert_file_contains() {
    local desc=$1
    local path=$2
    local pattern=$3
    if [ -f "$path" ] && grep -Eq "$pattern" "$path"; then
        record_pass "$desc"
        return 0
    fi
    record_fail "${desc}：文件缺失或未匹配到「${pattern}」"
    return 0
}

assert_file_not_contains() {
    local desc=$1
    local path=$2
    local pattern=$3
    if [ -f "$path" ] && ! grep -Eq "$pattern" "$path"; then
        record_pass "$desc"
        return 0
    fi
    record_fail "${desc}：文件缺失或匹配到「${pattern}」"
    return 0
}

assert_archive_contains() {
    local desc=$1
    local archive=$2
    local entry=$3
    local contents

    if ! contents=$(jar tf "$archive" 2>/dev/null); then
        record_fail "${desc}：无法读取 $archive"
        return 0
    fi
    if printf '%s\n' "$contents" | grep -Fq "$entry"; then
        record_pass "$desc"
        return 0
    fi
    record_fail "${desc}：归档中缺少 $entry"
    return 0
}

run_checked() {
    local desc=$1
    shift
    TOTAL=$((TOTAL + 1))
    if "$@"; then
        PASS=$((PASS + 1))
        log_pass "$desc"
        return 0
    fi
    FAIL=$((FAIL + 1))
    log_fail "$desc"
    return 0
}

wait_until() {
    local desc=$1
    local timeout=$2
    local interval=$3
    shift 3
    local elapsed=0

    while [ "$elapsed" -lt "$timeout" ]; do
        if "$@"; then
            log_info "${desc}，耗时 ${elapsed}s"
            return 0
        fi
        sleep "$interval"
        elapsed=$((elapsed + interval))
    done
    log_fail "$desc 在 ${timeout}s 内未满足"
    return 1
}

skip_test() {
    local desc=$1
    local reason=$2
    TOTAL=$((TOTAL + 1))
    if [ "$E2E_MODE" = "full" ]; then
        BLOCKED=$((BLOCKED + 1))
        log_fail "$desc 未执行：$reason"
    else
        SKIP=$((SKIP + 1))
        log_skip "${desc}：$reason"
    fi
}

assert_logs_clean() {
    local log_file
    local fatal_pattern='APPLICATION FAILED|cancelling refresh attempt|OutOfMemoryError|StackOverflowError'
    if [ "$LOGS_CHECKED" -eq 1 ]; then
        return 0
    fi
    LOGS_CHECKED=1

    for log_file in "${APP_LOGS[@]}"; do
        TOTAL=$((TOTAL + 1))
        if grep -Eq "$fatal_pattern" "$log_file" 2>/dev/null; then
            FAIL=$((FAIL + 1))
            log_fail "应用日志包含致命错误：$log_file"
        else
            PASS=$((PASS + 1))
            log_pass "应用日志无致命错误：$log_file"
        fi
    done
}

write_result() {
    local name=$1
    local status=$2
    local exit_code=$3
    local result_file="$E2E_RESULTS_DIR/$(safe_name "$name").result"

    {
        printf 'name=%s\n' "$name"
        printf 'status=%s\n' "$status"
        printf 'exit_code=%s\n' "$exit_code"
        printf 'pass=%s\n' "$PASS"
        printf 'fail=%s\n' "$FAIL"
        printf 'skip=%s\n' "$SKIP"
        printf 'blocked=%s\n' "$BLOCKED"
        printf 'total=%s\n' "$TOTAL"
        printf 'evidence_dir=%s\n' "$E2E_CASE_DIR"
    } >"$result_file"

    printf 'E2E_RESULT name=%s status=%s exit_code=%s pass=%s fail=%s skip=%s blocked=%s total=%s evidence=%s\n' \
        "$name" "$status" "$exit_code" "$PASS" "$FAIL" "$SKIP" "$BLOCKED" "$TOTAL" "$E2E_CASE_DIR"
}

print_summary() {
    local name=${1:-$E2E_TEST_NAME}
    local status=PASS
    local exit_code=0

    assert_logs_clean
    stop_all_apps true

    if [ "$FAIL" -gt 0 ]; then
        status=FAIL
        exit_code=1
    elif [ "$BLOCKED" -gt 0 ]; then
        status=BLOCKED
        exit_code=2
    elif [ "$SKIP" -gt 0 ]; then
        if [ "$E2E_MODE" = "full" ]; then
            status=FAIL
            exit_code=1
        else
            status=SKIP
        fi
    fi

    printf '\n==================================\n'
    printf ' %s\n' "$name"
    printf '==================================\n'
    printf ' 通过: %b%s%b\n' "$GREEN" "$PASS" "$NC"
    printf ' 失败: %b%s%b\n' "$RED" "$FAIL" "$NC"
    printf ' 跳过: %b%s%b\n' "$YELLOW" "$SKIP" "$NC"
    printf ' 阻塞: %b%s%b\n' "$RED" "$BLOCKED" "$NC"
    printf ' 总计: %s\n' "$TOTAL"
    printf ' 证据: %s\n' "$E2E_CASE_DIR"
    printf '==================================\n'

    SUMMARY_PRINTED=1
    write_result "$name" "$status" "$exit_code"
    return "$exit_code"
}

cleanup() {
    local original_exit=$?
    local exit_code=${1:-$original_exit}
    if [ "$CLEANUP_RUNNING" -eq 1 ]; then
        return "$exit_code"
    fi
    CLEANUP_RUNNING=1
    set +e
    stop_all_apps false
    if [ "$SUMMARY_PRINTED" -eq 0 ]; then
        FAIL=$((FAIL + 1))
        TOTAL=$((TOTAL + 1))
        log_fail "$E2E_TEST_NAME 未正常生成测试摘要，原始退出码 $exit_code"
        write_result "$E2E_TEST_NAME" FAIL "${exit_code:-1}"
    fi
    return "$exit_code"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
