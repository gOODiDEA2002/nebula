#!/usr/bin/env bash
# starter-api-example E2E 测试
# 验证 API 契约能够打包，且产物包含示例接口。

source "$(dirname "$0")/../e2e-common.sh"

log_info "========== starter-api-example E2E =========="

run_checked "构建并安装 API 契约" \
    mvn -q -f "$PROJECT_ROOT/examples/starter-api-example" clean install

JAR_FILE="$PROJECT_ROOT/examples/starter-api-example/target/starter-api-example-2.1.0-SNAPSHOT.jar"
assert_file_exists "API 契约 JAR 已生成" "$JAR_FILE"
assert_archive_contains "API 契约包含 UserApi" \
    "$JAR_FILE" "io/nebula/examples/api/UserApi.class"

print_summary "starter-api-example"
