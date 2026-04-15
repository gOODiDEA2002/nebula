#!/usr/bin/env bash
# starter-minimal-example E2E 测试
# 验证最小化 Starter 能正常启动（无 Web 端点）
source "$(dirname "$0")/../e2e-common.sh"

LOG_FILE="/tmp/e2e-starter-minimal.log"

log_info "========== starter-minimal-example E2E =========="

cd "$PROJECT_ROOT"
mvn -q -f examples/starter-minimal-example spring-boot:run > "$LOG_FILE" 2>&1 || true

assert_started "应用正常启动并退出" "$LOG_FILE"

print_summary "starter-minimal-example"
