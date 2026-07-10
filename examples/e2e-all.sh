#!/usr/bin/env bash
# 按依赖顺序执行全部示例应用 E2E 测试并生成统一摘要。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
E2E_MODE="${E2E_MODE:-smoke}"
E2E_RUN_ID="${E2E_RUN_ID:-$(date '+%Y%m%d-%H%M%S')-$$}"
E2E_RESULTS_ROOT="${E2E_RESULTS_ROOT:-$PROJECT_ROOT/target/example-e2e}"
E2E_RESULTS_DIR="${E2E_RESULTS_DIR:-$E2E_RESULTS_ROOT/$E2E_RUN_ID}"
E2E_ONLY="${E2E_ONLY:-}"

case "$E2E_MODE" in
    smoke|full) ;;
    *)
        echo "[FAIL] E2E_MODE 仅支持 smoke 或 full，实际值：$E2E_MODE" >&2
        exit 2
        ;;
esac

mkdir -p "$E2E_RESULTS_DIR/groups"
SUMMARY_FILE="$E2E_RESULTS_DIR/summary.tsv"
printf 'name\tstatus\texit_code\tduration_seconds\tlog\n' >"$SUMMARY_FILE"

EXAMPLE_NAMES=(
    starter-api-example
    starter-minimal-example
    starter-web-example
    starter-service-example
    starter-ai-example
    starter-all-example
    rpc-async-example
    microservice-example
    gateway-example
    fullstack-example
    crawler-example
    websocket-example
    oauth-example
)

EXAMPLE_SCRIPTS=(
    "$SCRIPT_DIR/starter-api-example/e2e-test.sh"
    "$SCRIPT_DIR/starter-minimal-example/e2e-test.sh"
    "$SCRIPT_DIR/starter-web-example/e2e-test.sh"
    "$SCRIPT_DIR/starter-service-example/e2e-test.sh"
    "$SCRIPT_DIR/starter-ai-example/e2e-test.sh"
    "$SCRIPT_DIR/starter-all-example/e2e-test.sh"
    "$SCRIPT_DIR/rpc-async-example/e2e-test.sh"
    "$SCRIPT_DIR/microservice-example/e2e-test.sh"
    "$SCRIPT_DIR/gateway-example/e2e-test.sh"
    "$SCRIPT_DIR/fullstack-example/e2e-test.sh"
    "$SCRIPT_DIR/crawler-example/e2e-test.sh"
    "$SCRIPT_DIR/websocket-example/e2e-test.sh"
    "$SCRIPT_DIR/oauth-example/e2e-test.sh"
)

for command_name in bash mvn curl jq nc lsof; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "[FAIL] 缺少命令：$command_name" >&2
        exit 2
    fi
done

should_run() {
    local name=$1
    if [ -z "$E2E_ONLY" ]; then
        return 0
    fi
    case ",$E2E_ONLY," in
        *",$name,"*) return 0 ;;
        *) return 1 ;;
    esac
}

result_value() {
    local result_file=$1
    local key=$2
    sed -n "s/^${key}=//p" "$result_file" | tail -n 1
}

PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0
BLOCKED_COUNT=0
RUN_COUNT=0

echo "[INFO] E2E 模式：$E2E_MODE"
echo "[INFO] 运行 ID：$E2E_RUN_ID"
echo "[INFO] 证据目录：$E2E_RESULTS_DIR"

for ((index=0; index<${#EXAMPLE_NAMES[@]}; index++)); do
    name=${EXAMPLE_NAMES[$index]}
    script=${EXAMPLE_SCRIPTS[$index]}
    if ! should_run "$name"; then
        continue
    fi

    RUN_COUNT=$((RUN_COUNT + 1))
    group_log="$E2E_RESULTS_DIR/groups/$name.log"
    result_file="$E2E_RESULTS_DIR/$name.result"
    started_at=$(date +%s)

    echo ""
    echo "========== $name =========="
    set +e
    E2E_MODE="$E2E_MODE" \
        E2E_RUN_ID="$E2E_RUN_ID" \
        E2E_RESULTS_ROOT="$E2E_RESULTS_ROOT" \
        E2E_RESULTS_DIR="$E2E_RESULTS_DIR" \
        E2E_TEST_NAME="$name" \
        bash "$script" 2>&1 | tee "$group_log"
    script_exit=${PIPESTATUS[0]}
    set -e

    duration=$(( $(date +%s) - started_at ))
    if [ -f "$result_file" ]; then
        status=$(result_value "$result_file" status)
        recorded_exit=$(result_value "$result_file" exit_code)
    else
        status=FAIL
        recorded_exit=$script_exit
        echo "[FAIL] $name 未生成结果文件" | tee -a "$group_log"
    fi

    case "$recorded_exit" in
        ''|*[!0-9]*)
            status=FAIL
            recorded_exit=$script_exit
            echo "[FAIL] $name 的结果文件缺少有效退出码" | tee -a "$group_log"
            ;;
    esac

    if [ "$script_exit" -ne "$recorded_exit" ]; then
        status=FAIL
        echo "[FAIL] $name 实际退出码 $script_exit 与结果文件 $recorded_exit 不一致" | tee -a "$group_log"
    fi

    case "$status" in
        PASS) PASS_COUNT=$((PASS_COUNT + 1)) ;;
        SKIP) SKIP_COUNT=$((SKIP_COUNT + 1)) ;;
        BLOCKED) BLOCKED_COUNT=$((BLOCKED_COUNT + 1)) ;;
        *)
            status=FAIL
            FAIL_COUNT=$((FAIL_COUNT + 1))
            ;;
    esac
    printf '%s\t%s\t%s\t%s\t%s\n' "$name" "$status" "$script_exit" "$duration" "$group_log" >>"$SUMMARY_FILE"
done

echo ""
echo "=================================="
echo " 示例应用 E2E 汇总"
echo "=================================="
echo " 通过: $PASS_COUNT"
echo " 失败: $FAIL_COUNT"
echo " 跳过: $SKIP_COUNT"
echo " 阻塞: $BLOCKED_COUNT"
echo " 执行: $RUN_COUNT"
echo " 证据: $E2E_RESULTS_DIR"
echo "=================================="
printf 'E2E_AGGREGATE mode=%s pass=%s fail=%s skip=%s blocked=%s run=%s evidence=%s\n' \
    "$E2E_MODE" "$PASS_COUNT" "$FAIL_COUNT" "$SKIP_COUNT" "$BLOCKED_COUNT" "$RUN_COUNT" "$E2E_RESULTS_DIR"

if [ "$RUN_COUNT" -eq 0 ]; then
    echo "[FAIL] E2E_ONLY 未匹配任何示例" >&2
    exit 1
fi
if [ "$FAIL_COUNT" -gt 0 ] || [ "$BLOCKED_COUNT" -gt 0 ]; then
    exit 1
fi
if [ "$E2E_MODE" = "full" ] && [ "$SKIP_COUNT" -gt 0 ]; then
    exit 1
fi
