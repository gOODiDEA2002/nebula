#!/usr/bin/env bash
# E2E 测试通用函数库
# 使用方式: source "$(dirname "$0")/../e2e-common.sh"

set -euo pipefail

PASS=0
FAIL=0
SKIP=0
TOTAL=0
APP_PID=""
APP_PIDS=()
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { echo -e "${CYAN}[INFO]${NC} $*"; }
log_pass()  { echo -e "${GREEN}[PASS]${NC} $*"; }
log_fail()  { echo -e "${RED}[FAIL]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_skip()  { echo -e "${YELLOW}[SKIP]${NC} $*"; }

# 检查外部服务是否可达（端口探测）
require_service() {
    local name=$1
    local host=$2
    local port=$3
    if nc -z "$host" "$port" 2>/dev/null; then
        log_info "外部服务 $name ($host:$port) 已就绪"
        return 0
    else
        log_warn "外部服务 $name ($host:$port) 不可达"
        return 1
    fi
}

# 若外部服务不可达则跳过整个测试
skip_if_no_service() {
    local name=$1
    local host=$2
    local port=$3
    if ! require_service "$name" "$host" "$port"; then
        log_skip "跳过测试: 需要 $name ($host:$port)"
        echo ""
        echo "=================================="
        echo " $4 -- SKIPPED"
        echo "=================================="
        exit 0
    fi
}

# 等待端口可用
wait_for_port() {
    local port=$1
    local timeout=${2:-60}
    local elapsed=0
    log_info "等待端口 $port 就绪 (超时 ${timeout}s)..."
    while ! curl -sf --noproxy '*' "http://localhost:$port" >/dev/null 2>&1 && \
          ! curl -sf --noproxy '*' "http://localhost:$port/health/ping" >/dev/null 2>&1 && \
          ! nc -z localhost "$port" 2>/dev/null; do
        sleep 1
        elapsed=$((elapsed + 1))
        if [ $elapsed -ge $timeout ]; then
            log_fail "端口 $port 在 ${timeout}s 内未就绪"
            return 1
        fi
    done
    log_info "端口 $port 就绪 (${elapsed}s)"
}

# 等待应用启动（通过日志关键字）
wait_for_log() {
    local log_file=$1
    local keyword=$2
    local timeout=${3:-60}
    local elapsed=0
    log_info "等待日志匹配 '$keyword' (超时 ${timeout}s)..."
    while ! grep -q "$keyword" "$log_file" 2>/dev/null; do
        sleep 1
        elapsed=$((elapsed + 1))
        if [ $elapsed -ge $timeout ]; then
            log_fail "日志在 ${timeout}s 内未匹配 '$keyword'"
            return 1
        fi
    done
    log_info "日志匹配成功 (${elapsed}s)"
}

# 启动 Spring Boot 应用（支持多实例追踪）
start_app() {
    local module_path=$1
    local port=${2:-8080}
    local extra_args=${3:-}
    local log_file="/tmp/e2e-$(echo "$module_path" | tr '/' '-').log"

    log_info "启动 $module_path (port=$port)..."

    lsof -i ":$port" -t 2>/dev/null | xargs kill -9 2>/dev/null || true
    sleep 1

    cd "$PROJECT_ROOT"
    mvn -q -f "$module_path" spring-boot:run $extra_args > "$log_file" 2>&1 &
    APP_PID=$!
    APP_PIDS+=("$APP_PID")

    if ! wait_for_log "$log_file" "Started\|APPLICATION FAILED\|cancelling refresh attempt" 120; then
        log_fail "应用启动超时"
        tail -30 "$log_file"
        return 1
    fi

    if grep -q "APPLICATION FAILED\|cancelling refresh attempt\|Process terminated with exit code" "$log_file"; then
        log_fail "应用启动失败"
        grep "Description:" "$log_file" || tail -20 "$log_file"
        return 1
    fi

    log_info "应用已启动 (PID=$APP_PID)"
}

# 停止单个应用
stop_app() {
    if [ -n "$APP_PID" ]; then
        log_info "停止应用 (PID=$APP_PID)..."
        kill "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
        APP_PID=""
    fi
}

# 停止所有已启动的应用
stop_all_apps() {
    for pid in "${APP_PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            log_info "停止应用 (PID=$pid)..."
            kill "$pid" 2>/dev/null || true
            wait "$pid" 2>/dev/null || true
        fi
    done
    APP_PIDS=()
    APP_PID=""
}

# 断言 HTTP 响应包含预期内容
assert_contains() {
    local desc=$1
    local url=$2
    local expected=$3
    local method=${4:-GET}
    local data=${5:-}
    TOTAL=$((TOTAL + 1))

    local response
    if [ "$method" = "POST" ]; then
        response=$(curl -s --noproxy '*' -X POST "$url" -H "Content-Type: application/json" -d "$data" 2>/dev/null || echo "CURL_FAILED")
    else
        response=$(curl -s --noproxy '*' "$url" 2>/dev/null || echo "CURL_FAILED")
    fi

    if echo "$response" | grep -q "$expected"; then
        log_pass "$desc"
        PASS=$((PASS + 1))
    else
        log_fail "$desc (期望包含: '$expected')"
        echo "  实际响应: $(echo "$response" | head -3)"
        FAIL=$((FAIL + 1))
    fi
}

# 断言 HTTP 状态码
assert_status() {
    local desc=$1
    local url=$2
    local expected_code=${3:-200}
    TOTAL=$((TOTAL + 1))

    local status
    status=$(curl -s --noproxy '*' -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")

    if [ "$status" = "$expected_code" ]; then
        log_pass "$desc (HTTP $status)"
        PASS=$((PASS + 1))
    else
        log_fail "$desc (期望 HTTP $expected_code, 实际 HTTP $status)"
        FAIL=$((FAIL + 1))
    fi
}

# 断言 HTTP 响应不包含指定内容（反向断言）
assert_not_contains() {
    local desc=$1
    local url=$2
    local unexpected=$3
    TOTAL=$((TOTAL + 1))

    local response
    response=$(curl -s --noproxy '*' "$url" 2>/dev/null || echo "CURL_FAILED")

    if echo "$response" | grep -q "$unexpected"; then
        log_fail "$desc (不应包含: '$unexpected')"
        echo "  实际响应: $(echo "$response" | head -3)"
        FAIL=$((FAIL + 1))
    else
        log_pass "$desc"
        PASS=$((PASS + 1))
    fi
}

# 断言应用启动成功（非 Web）
assert_started() {
    local desc=$1
    local log_file=$2
    TOTAL=$((TOTAL + 1))

    if grep -q "Started" "$log_file" 2>/dev/null; then
        log_pass "$desc"
        PASS=$((PASS + 1))
    else
        log_fail "$desc"
        FAIL=$((FAIL + 1))
    fi
}

# 跳过测试用例
skip_test() {
    local desc=$1
    local reason=$2
    TOTAL=$((TOTAL + 1))
    SKIP=$((SKIP + 1))
    log_skip "$desc ($reason)"
}

# 打印测试摘要
print_summary() {
    local name=${1:-"E2E Test"}
    echo ""
    echo "=================================="
    echo " $name"
    echo "=================================="
    echo -e " 通过: ${GREEN}$PASS${NC}"
    echo -e " 失败: ${RED}$FAIL${NC}"
    echo -e " 跳过: ${YELLOW}$SKIP${NC}"
    echo -e " 总计: $TOTAL"
    echo "=================================="

    if [ "$FAIL" -gt 0 ]; then
        exit 1
    fi
}

# 清理钩子
cleanup() {
    stop_all_apps
}
trap cleanup EXIT
