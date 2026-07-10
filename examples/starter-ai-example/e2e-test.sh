#!/usr/bin/env bash
# starter-ai-example E2E 测试
# Full 模式同时验证无密钥禁用模式、真实 OpenAI 调用和 Chroma 向量操作。
source "$(dirname "$0")/../e2e-common.sh"

DISABLED_PORT=8083
ENABLED_PORT=18083
CHROMA_HOST="${CHROMA_HOST:-localhost}"
CHROMA_PORT="${CHROMA_PORT:-9002}"
CHROMA_COLLECTION="nebula_e2e_ai_${E2E_RUN_ID//-/_}"
CHROMA_BASE_URL="http://${CHROMA_HOST}:${CHROMA_PORT}/api/v2/tenants/default_tenant/databases/default_database/collections"
CHROMA_CLEANED=0
MODEL_REQUEST_COUNT=0
TEST_OPENAI_API_KEY="${OPENAI_API_KEY:-}"
AI_LIVE_READY=1

cleanup_chroma_best_effort() {
    if [ "$CHROMA_CLEANED" -eq 1 ]; then
        return 0
    fi
    curl --silent --show-error --noproxy '*' --connect-timeout 3 --max-time 10 \
        -X DELETE "${CHROMA_BASE_URL}/${CHROMA_COLLECTION}" >/dev/null 2>&1 || true
}

cleanup_ai() {
    local exit_code=$?
    trap - EXIT
    set +e
    cleanup_chroma_best_effort
    cleanup "$exit_code"
    exit "$exit_code"
}

assert_secret_not_logged() {
    local log_file=$1
    if grep -Fq -- "$OPENAI_API_KEY" "$log_file" 2>/dev/null; then
        record_fail "应用日志未泄漏 OpenAI API Key"
        return 0
    fi
    record_pass "应用日志未泄漏 OpenAI API Key"
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

verify_live_chat() {
    local url="http://localhost:$ENABLED_PORT/ai/echo?q=Reply%20with%20exactly%20NEBULA_AI_OK"

    TOTAL=$((TOTAL + 1))
    perform_request GET "$url" "" || true
    MODEL_REQUEST_COUNT=$((MODEL_REQUEST_COUNT + 1))

    if [ "$HTTP_STATUS" = "200" ] &&
        jq -e '.success == true and (.data | type == "string") and
            (.data | length > 0) and .data != "AI disabled"' \
            "$HTTP_BODY_FILE" >/dev/null 2>&1; then
        PASS=$((PASS + 1))
        log_pass "OpenAI 真实聊天返回非空内容，HTTP 200，JSON 断言通过"
        return 0
    fi

    if grep -Eq 'RateLimitException: 429: .*quota' "$ENABLED_LOG" 2>/dev/null; then
        if [ "$E2E_MODE" = "full" ]; then
            BLOCKED=$((BLOCKED + 1))
            log_fail "OpenAI 测试账号额度不足，真实调用暂时阻塞"
        else
            SKIP=$((SKIP + 1))
            log_skip "OpenAI 测试账号额度不足，跳过真实调用"
        fi
    else
        FAIL=$((FAIL + 1))
        log_fail "OpenAI 真实聊天失败，HTTP $HTTP_STATUS，证据：$HTTP_BODY_FILE"
    fi
    AI_LIVE_READY=0
}

trap cleanup_ai EXIT

log_info "========== starter-ai-example E2E =========="

export AI_ENABLED=false
export OPENAI_API_KEY=""
start_app "examples/starter-ai-example" "$DISABLED_PORT"
DISABLED_LOG="$CURRENT_APP_LOG"

assert_json "AI 禁用状态未创建远程服务" \
    "http://localhost:$DISABLED_PORT/ai/status" \
    '.success == true and .data.chat == false and .data.embedding == false and .data.vectorStore == false'
assert_json "AI 禁用时返回明确降级结果" \
    "http://localhost:$DISABLED_PORT/ai/echo?q=test" \
    '.success == true and .data == "AI disabled"'
assert_file_not_contains "AI 禁用日志没有自动配置或远程客户端" \
    "$DISABLED_LOG" \
    'Nebula AI 模块自动配置已启用|配置 OpenAI|配置 Chroma|配置 Nebula (ChatService|EmbeddingService|VectorStoreService)'
stop_all_apps true

export OPENAI_API_KEY="$TEST_OPENAI_API_KEY"
skip_if_no_env OPENAI_API_KEY "starter-ai-example"
skip_if_no_service Chroma "$CHROMA_HOST" "$CHROMA_PORT" "starter-ai-example"

export AI_ENABLED=true
export CHROMA_HOST
export CHROMA_PORT
export CHROMA_COLLECTION
export SERVER_PORT="$ENABLED_PORT"
start_app "examples/starter-ai-example" "$ENABLED_PORT"
ENABLED_LOG="$CURRENT_APP_LOG"

assert_json "AI 启用状态包含三项真实服务" \
    "http://localhost:$ENABLED_PORT/ai/status" \
    '.success == true and .data.chat == true and .data.embedding == true and .data.vectorStore == true'

verify_live_chat

if [ "$AI_LIVE_READY" -eq 0 ]; then
    if [ "$MODEL_REQUEST_COUNT" -eq 1 ]; then
        record_pass "外部调用失败后立即停止，模型请求为 1 次"
    else
        record_fail "外部调用失败后仍发出了多余模型请求"
    fi
    assert_secret_not_logged "$ENABLED_LOG"
    stop_all_apps true
    delete_chroma_collection
    print_summary "starter-ai-example"
    exit $?
fi

assert_json "OpenAI 真实 embedding 返回有效维度" \
    "http://localhost:$ENABLED_PORT/ai/embedding?q=nebula%20embedding%20verification" \
    '.success == true and (.data.model | length > 0) and .data.dimension > 0'
MODEL_REQUEST_COUNT=$((MODEL_REQUEST_COUNT + 1))

DOCUMENT_ID="nebula_e2e_ai_doc_${E2E_RUN_ID//-/_}"
DOCUMENT_CONTENT="Nebula framework uses semantic vector search for AI verification ${E2E_RUN_ID}"
DOCUMENT_JSON=$(jq -cn --arg id "$DOCUMENT_ID" --arg content "$DOCUMENT_CONTENT" \
    '{id: $id, content: $content}')

assert_json "Chroma 写入临时文档" \
    "http://localhost:$ENABLED_PORT/ai/vector/documents" \
    ".success == true and .data.id == \"$DOCUMENT_ID\" and .data.success == true" \
    POST "$DOCUMENT_JSON"
MODEL_REQUEST_COUNT=$((MODEL_REQUEST_COUNT + 1))

assert_json "Chroma 相似度查询命中临时文档" \
    "http://localhost:$ENABLED_PORT/ai/vector/search?q=semantic%20vector%20search&topK=1" \
    ".success == true and .data.totalFound >= 1 and any(.data.documents[]; .id == \"$DOCUMENT_ID\")"
MODEL_REQUEST_COUNT=$((MODEL_REQUEST_COUNT + 1))

assert_json "Chroma 删除临时文档" \
    "http://localhost:$ENABLED_PORT/ai/vector/documents/$DOCUMENT_ID" \
    ".success == true and .data.id == \"$DOCUMENT_ID\" and .data.success == true" \
    DELETE

assert_file_contains "启用日志记录实际聊天模型" \
    "$ENABLED_LOG" \
    '配置 OpenAI ChatModel.*Model: .+'
assert_file_contains "启用日志记录实际 embedding 模型" \
    "$ENABLED_LOG" \
    '配置 OpenAI EmbeddingModel, Model: .+'
assert_secret_not_logged "$ENABLED_LOG"

if [ "$MODEL_REQUEST_COUNT" -eq 4 ]; then
    record_pass "外部模型请求按设计限制为 4 次"
else
    record_fail "外部模型请求计数不是 4 次"
fi

stop_all_apps true
delete_chroma_collection

print_summary "starter-ai-example"
