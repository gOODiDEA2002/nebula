#!/usr/bin/env bash
# rag-example E2E 测试
# Smoke：仅禁用态 S1-S3。Full：追加启用态 F1-F13，需真实 Chroma 与 Hy3 兼容端点。
# 注意：Hy3 为推理模型，单次生成约 20-30 秒；启用态调用统一放宽 HTTP 超时到 120 秒
#（经 E2E_HTTP_MAX_TIME 传入 e2e-common.sh 的 perform_request），避免生成用例超时返回 000。
source "$(dirname "$0")/../e2e-common.sh"

DISABLED_PORT=8087
ENABLED_PORT=18087
CHROMA_HOST="${CHROMA_HOST:-localhost}"
CHROMA_PORT="${CHROMA_PORT:-9002}"
CHROMA_COLLECTION="nebula_e2e_rag_${E2E_RUN_ID//-/_}"
CHROMA_BASE_URL="http://${CHROMA_HOST}:${CHROMA_PORT}/api/v2/tenants/default_tenant/databases/default_database/collections"
CHROMA_CLEANED=0
STATE_FILE="$E2E_CASE_DIR/rag-state.json"
TEST_AI_API_KEY="${vocoor_hy3_token:-}"
CHUNKS_500=0

BASE_DISABLED="http://localhost:$DISABLED_PORT"
BASE_ENABLED="http://localhost:$ENABLED_PORT"

cleanup_chroma_best_effort() {
    if [ "$CHROMA_CLEANED" -eq 1 ]; then
        return 0
    fi
    curl --silent --show-error --noproxy '*' --connect-timeout 3 --max-time 10 \
        -X DELETE "${CHROMA_BASE_URL}/${CHROMA_COLLECTION}" >/dev/null 2>&1 || true
}

cleanup_rag() {
    local exit_code=$?
    trap - EXIT
    set +e
    cleanup_chroma_best_effort
    cleanup "$exit_code"
    exit "$exit_code"
}

assert_secret_not_logged() {
    local log_file=$1
    if [ -n "$TEST_AI_API_KEY" ] && grep -Fq -- "$TEST_AI_API_KEY" "$log_file" 2>/dev/null; then
        record_fail "应用日志泄漏 AI API Key"
        return 0
    fi
    record_pass "应用日志未泄漏 AI API Key"
}

delete_chroma_collection() {
    perform_request DELETE "${CHROMA_BASE_URL}/${CHROMA_COLLECTION}" "" || true
    if [ "$HTTP_STATUS" = "200" ]; then
        record_pass "Chroma 临时 collection 已删除"
    elif [ "$HTTP_STATUS" = "404" ]; then
        record_pass "Chroma 临时 collection 不存在，无需删除"
    else
        record_fail "Chroma 临时 collection 删除失败，HTTP $HTTP_STATUS"
    fi

    perform_request GET "$CHROMA_BASE_URL" "" || true
    if [ "$HTTP_STATUS" = "200" ] &&
        jq -e "[.[] | select(.name == \"$CHROMA_COLLECTION\")] | length == 0" \
            "$HTTP_BODY_FILE" >/dev/null 2>&1; then
        record_pass "Chroma 临时 collection 删除后复核为 0"
        CHROMA_CLEANED=1
    else
        record_fail "Chroma 临时 collection 删除后仍然存在"
    fi
}

# 生成/嵌入用例：失败且日志含 429 quota 时按 full=BLOCKED / smoke=SKIP 记，返回 1 表示被阻塞
run_generation_case() {
    local desc=$1 url=$2 data=$3 jq_filter=$4 log_file=$5

    TOTAL=$((TOTAL + 1))
    perform_request POST "$url" "$data" || true
    if [ "$HTTP_STATUS" = "200" ] && jq -e "$jq_filter" "$HTTP_BODY_FILE" >/dev/null 2>&1; then
        PASS=$((PASS + 1))
        log_pass "${desc}，HTTP ${HTTP_STATUS}，JSON 断言通过"
        return 0
    fi
    if grep -Eq 'RateLimitException: 429: .*quota|429.*quota' "$log_file" 2>/dev/null; then
        if [ "$E2E_MODE" = "full" ]; then
            BLOCKED=$((BLOCKED + 1))
            log_fail "${desc}：AI 兼容服务额度不足（429），暂时阻塞"
        else
            SKIP=$((SKIP + 1))
            log_skip "${desc}：AI 兼容服务额度不足（429），跳过"
        fi
        return 1
    fi
    FAIL=$((FAIL + 1))
    log_fail "${desc}，期望 HTTP 200 且 JSON 满足「${jq_filter}」，实际 HTTP ${HTTP_STATUS}，证据：${HTTP_BODY_FILE}"
    return 1
}

# 流式用例：解析 SSE 事件序列（首 REFERENCES、末 COMPLETE、无 ERROR）
run_stream_case() {
    local desc=$1 url=$2 data=$3 log_file=$4

    TOTAL=$((TOTAL + 1))
    perform_request POST "$url" "$data" 'Accept: text/event-stream' || true
    local types first last
    types=$(grep -o '"type":"[A-Z]*"' "$HTTP_BODY_FILE" 2>/dev/null | sed 's/.*:"//; s/"//')
    first=$(printf '%s\n' "$types" | head -n 1)
    last=$(printf '%s\n' "$types" | tail -n 1)
    if [ "$HTTP_STATUS" = "200" ] && [ "$first" = "REFERENCES" ] && [ "$last" = "COMPLETE" ] &&
        ! printf '%s\n' "$types" | grep -q 'ERROR'; then
        PASS=$((PASS + 1))
        log_pass "${desc}，事件序列 REFERENCES...COMPLETE，无 ERROR"
        return 0
    fi
    if grep -Eq 'RateLimitException: 429: .*quota|429.*quota' "$log_file" 2>/dev/null; then
        if [ "$E2E_MODE" = "full" ]; then
            BLOCKED=$((BLOCKED + 1))
            log_fail "${desc}：AI 兼容服务额度不足（429），暂时阻塞"
        else
            SKIP=$((SKIP + 1))
            log_skip "${desc}：AI 兼容服务额度不足（429），跳过"
        fi
        return 1
    fi
    FAIL=$((FAIL + 1))
    log_fail "${desc}，期望首 REFERENCES 末 COMPLETE 无 ERROR，实际首「${first}」末「${last}」HTTP ${HTTP_STATUS}，证据：${HTTP_BODY_FILE}"
    return 1
}

trap cleanup_rag EXIT

log_info "========== rag-example E2E =========="

# ================= 禁用态 S1-S3（smoke + full）=================
export AI_ENABLED=false
export OPENAI_API_KEY=""
export vocoor_hy3_token=""
export NEBULA_AI_OPENAI_API_KEY=""
export SERVER_PORT="$DISABLED_PORT"
export RAG_STATE_FILE="$STATE_FILE"
start_app "examples/rag-example" "$DISABLED_PORT"
DISABLED_LOG="$CURRENT_APP_LOG"

# S1 无密钥启动，status.enabled=false
assert_json "S1 禁用态 /rag/status enabled=false" \
    "$BASE_DISABLED/rag/status" \
    '.success == true and .data.enabled == false'

# S2 七端点均 HTTP 200 且 success=true、返回禁用提示
assert_json "S2 /rag/status 可用" \
    "$BASE_DISABLED/rag/status" '.success == true'
assert_json "S2 /rag/index 返回禁用提示" \
    "$BASE_DISABLED/rag/index" '.success == true and .data.enabled == false' POST '{}'
assert_json "S2 /rag/search 返回禁用提示" \
    "$BASE_DISABLED/rag/search" '.success == true' POST '{"query":"test","topK":5}'
assert_json "S2 /rag/query 返回禁用提示" \
    "$BASE_DISABLED/rag/query" '.success == true and .data.enabled == false' POST '{"query":"test","topK":5}'
assert_json "S2 /rag/query/stream 返回禁用提示" \
    "$BASE_DISABLED/rag/query/stream" '.success == true and .data.enabled == false' POST '{"query":"test","topK":5}'
assert_json "S2 /rag/eval 返回禁用提示" \
    "$BASE_DISABLED/rag/eval" '.success == true'
assert_json "S2 /rag/documents 返回禁用提示" \
    "$BASE_DISABLED/rag/documents" '.success == true and .data.enabled == false' DELETE

# S3 禁用日志安全
assert_file_not_contains "S3 禁用日志无 RAG 自动配置启用" \
    "$DISABLED_LOG" 'Nebula AI RAG 模块自动配置已启用'
assert_secret_not_logged "$DISABLED_LOG"

stop_all_apps true

if [ "$E2E_MODE" != "full" ]; then
    print_summary "rag-example"
    exit $?
fi

# ================= 启用态 F1-F13（仅 full）=================
export OPENAI_API_KEY=""
export vocoor_hy3_token="$TEST_AI_API_KEY"
export NEBULA_AI_OPENAI_API_KEY="$TEST_AI_API_KEY"
skip_if_no_env vocoor_hy3_token "rag-example"
skip_if_no_service Chroma "$CHROMA_HOST" "$CHROMA_PORT" "rag-example"

# Hy3 推理模型单次生成慢，启用态统一放宽 HTTP 超时
export E2E_HTTP_MAX_TIME=120

export AI_ENABLED=true
export CHROMA_HOST
export CHROMA_PORT
export CHROMA_COLLECTION
export SERVER_PORT="$ENABLED_PORT"
export RAG_STATE_FILE="$STATE_FILE"
unset CHUNK_SIZE 2>/dev/null || true
start_app "examples/rag-example" "$ENABLED_PORT"
ENABLED_LOG="$CURRENT_APP_LOG"

# F1 启用日志：自动配置、双写目标（顺序无关）、两路检索器
assert_file_contains "F1 日志含 RAG 自动配置启用" \
    "$ENABLED_LOG" 'Nebula AI RAG 模块自动配置已启用'
assert_file_contains "F1 日志含双写目标（顺序无关）" \
    "$ENABLED_LOG" '写目标: \[(vector-store, keyword-memory|keyword-memory, vector-store)\]'
assert_file_contains "F1 日志含向量与关键词两路检索器" \
    "$ENABLED_LOG" '检索器: \[vector, keyword\]'

# F2 首次灌库 added=5
if run_generation_case "F2 POST /rag/index 首次灌库" \
    "$BASE_ENABLED/rag/index" '{}' \
    '.success == true and .data.added == 5 and .data.updated == 0 and .data.deleted == 0 and .data.failed == 0' \
    "$ENABLED_LOG"; then
    perform_request GET "$BASE_ENABLED/rag/status" "" || true
    CHUNKS_500=$(jq -r '.data.indexedChunks // 0' "$HTTP_BODY_FILE" 2>/dev/null || echo 0)
    assert_json "F2 status.indexedChunks > 5" \
        "$BASE_ENABLED/rag/status" '.success == true and .data.indexedChunks > 5'
else
    # 嵌入不可用则后续用例无意义，收尾退出
    assert_secret_not_logged "$ENABLED_LOG"
    stop_all_apps true
    delete_chroma_collection
    print_summary "rag-example"
    exit $?
fi

# F3 重复 index 幂等，四计数全 0
run_generation_case "F3 POST /rag/index 幂等" \
    "$BASE_ENABLED/rag/index" '{}' \
    '.success == true and .data.added == 0 and .data.updated == 0 and .data.deleted == 0 and .data.failed == 0' \
    "$ENABLED_LOG" || true

# F4 search 常规问句：非空、score 降序、id 形如 nebula-faq-xxx#N
run_generation_case "F4 POST /rag/search 常规问句" \
    "$BASE_ENABLED/rag/search" '{"query":"Nebula 的模块启用开关怎么配","topK":5}' \
    '.success == true and (.data | length > 0)
        and (.data | all(.id | test("^nebula-faq-[a-z]+#[0-9]+$")))
        and ((.data | map(.score)) as $s | $s == ($s | sort | reverse))' \
    "$ENABLED_LOG" || true

# F5 query 常规问句：answer/references 非空、degraded=false
run_generation_case "F5 POST /rag/query 常规问句" \
    "$BASE_ENABLED/rag/query" '{"query":"Nebula 的模块启用开关怎么配","topK":5}' \
    '.success == true and (.data.answer | length > 0) and (.data.references | length > 0) and .data.degraded == false' \
    "$ENABLED_LOG" || true

# F6 search NBX-2077：前 5 含 nebula-faq-codes# 前缀（关键词路贡献）
run_generation_case "F6 POST /rag/search 查询 NBX-2077" \
    "$BASE_ENABLED/rag/search" '{"query":"NBX-2077 是什么故障","topK":5}' \
    '.success == true and (.data | any(.id | startswith("nebula-faq-codes#")))' \
    "$ENABLED_LOG" || true

# F7 eval：perQuery 长 10、recallAtK>=0.7、configSnapshot.chunkSize=="500"
TOTAL=$((TOTAL + 1))
perform_request GET "$BASE_ENABLED/rag/eval" "" || true
if [ "$HTTP_STATUS" = "200" ] && jq -e \
    '.success == true and (.data.perQuery | length == 10) and (.data.recallAtK >= 0.7) and (.data.configSnapshot.chunkSize == "500")' \
    "$HTTP_BODY_FILE" >/dev/null 2>&1; then
    PASS=$((PASS + 1))
    log_pass "F7 GET /rag/eval 通过，HTTP 200，JSON 断言通过"
else
    FAIL=$((FAIL + 1))
    log_fail "F7 GET /rag/eval 失败，HTTP ${HTTP_STATUS}，证据：${HTTP_BODY_FILE}"
fi

# F8 stream：SSE 首 REFERENCES、末 COMPLETE、无 ERROR
run_stream_case "F8 POST /rag/query/stream" \
    "$BASE_ENABLED/rag/query/stream" '{"query":"三级启用策略是怎么划分的","topK":5}' \
    "$ENABLED_LOG" || true

# F9 search 提示词注入：security 块内容被清洗
run_generation_case "F9 POST /rag/search 提示词注入清洗" \
    "$BASE_ENABLED/rag/search" '{"query":"检索内容防注入 提示词注入","topK":5}' \
    '.success == true
        and (.data | any(.id | startswith("nebula-faq-security#")))
        and (.data | map(select(.id | startswith("nebula-faq-security#"))) | all(.content | contains("ignore all previous instructions") | not))
        and (.data | any(.content | contains("[内容因安全策略未进入上下文]")))' \
    "$ENABLED_LOG" || true

# F10 指标端点
assert_json "F10 /actuator/metrics/nebula.rag.query.duration COUNT>=1" \
    "$BASE_ENABLED/actuator/metrics/nebula.rag.query.duration" \
    '.measurements | any(.statistic == "COUNT" and .value >= 1)'

# F11 重启（CHUNK_SIZE=200）后 index + eval：判更新、块数增多、快照 chunkSize=200
stop_all_apps true
export CHUNK_SIZE=200
start_app "examples/rag-example" "$ENABLED_PORT"
ENABLED_LOG2="$CURRENT_APP_LOG"

run_generation_case "F11 CHUNK_SIZE=200 重灌判更新" \
    "$BASE_ENABLED/rag/index" '{}' \
    '.success == true and .data.updated == 5 and .data.added == 0' \
    "$ENABLED_LOG2" || true

TOTAL=$((TOTAL + 1))
perform_request GET "$BASE_ENABLED/rag/status" "" || true
NEW_CHUNKS=$(jq -r '.data.indexedChunks // 0' "$HTTP_BODY_FILE" 2>/dev/null || echo 0)
if [ "$HTTP_STATUS" = "200" ] && [ "$NEW_CHUNKS" -gt "$CHUNKS_500" ]; then
    PASS=$((PASS + 1))
    log_pass "F11 status.indexedChunks（${NEW_CHUNKS}）大于 F2（${CHUNKS_500}）"
else
    FAIL=$((FAIL + 1))
    log_fail "F11 块数未增多：新 ${NEW_CHUNKS}，旧 ${CHUNKS_500}，HTTP ${HTTP_STATUS}"
fi

TOTAL=$((TOTAL + 1))
perform_request GET "$BASE_ENABLED/rag/eval" "" || true
if [ "$HTTP_STATUS" = "200" ] && jq -e \
    '.success == true and (.data.configSnapshot.chunkSize == "200") and (.data.recallAtK >= 0.7)' \
    "$HTTP_BODY_FILE" >/dev/null 2>&1; then
    PASS=$((PASS + 1))
    log_pass "F11 eval configSnapshot.chunkSize=200 且 recallAtK>=0.7"
else
    FAIL=$((FAIL + 1))
    log_fail "F11 eval 断言失败，HTTP ${HTTP_STATUS}，证据：${HTTP_BODY_FILE}"
fi

# F12 DELETE /rag/documents：deleted=5、再 search 空、状态文件文档数 0
assert_json "F12 DELETE /rag/documents deleted=5" \
    "$BASE_ENABLED/rag/documents" \
    '.success == true and .data.deleted == 5 and .data.added == 0 and .data.updated == 0 and .data.failed == 0' DELETE

run_generation_case "F12 删除后 search 返回空" \
    "$BASE_ENABLED/rag/search" '{"query":"Nebula 的模块启用开关怎么配","topK":5}' \
    '.success == true and (.data | length == 0)' \
    "$ENABLED_LOG2" || true

TOTAL=$((TOTAL + 1))
if [ -f "$STATE_FILE" ] && jq -e '((."rag-example-docs" // {}) | length) == 0' "$STATE_FILE" >/dev/null 2>&1; then
    PASS=$((PASS + 1))
    log_pass "F12 状态文件文档数为 0"
else
    FAIL=$((FAIL + 1))
    log_fail "F12 状态文件文档数不为 0，证据：${STATE_FILE}"
fi

# F13 收尾
export E2E_HTTP_MAX_TIME=30
assert_secret_not_logged "$ENABLED_LOG"
assert_secret_not_logged "$ENABLED_LOG2"
stop_all_apps true
delete_chroma_collection
if rm -f "$STATE_FILE" 2>/dev/null && [ ! -f "$STATE_FILE" ]; then
    record_pass "F13 状态文件已清理"
else
    record_fail "F13 状态文件清理失败：${STATE_FILE}"
fi

print_summary "rag-example"
