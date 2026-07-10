#!/usr/bin/env bash
# starter-minimal-example E2E 测试
# 验证最小化 Starter 能正常启动（无 Web 端点）
source "$(dirname "$0")/../e2e-common.sh"

LOG_FILE="$E2E_CASE_DIR/starter-minimal.log"

log_info "========== starter-minimal-example E2E =========="

cd "$PROJECT_ROOT"
set +e
mvn -q -f examples/starter-minimal-example spring-boot:run >"$LOG_FILE" 2>&1
APP_EXIT=$?
set -e

assert_started "应用正常启动并退出" "$LOG_FILE"
if [ "$APP_EXIT" -eq 0 ]; then
    record_pass "非 Web 应用退出码为 0"
else
    record_fail "非 Web 应用退出码应为 0，实际为 $APP_EXIT"
fi
if grep -Eq 'Web server initialized|Tomcat started|Netty started|Undertow started|Jetty started|Started .* on port' "$LOG_FILE"; then
    record_fail "非 Web 应用不应启动网络服务器"
else
    record_pass "非 Web 应用未启动网络服务器"
fi

print_summary "starter-minimal-example"
