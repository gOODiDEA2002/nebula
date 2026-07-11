# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4、Task 6 至 Task 10 已完成，
Task 5、Task 11 因 OpenAI 额度阻塞，Task 14 因隔离 Vocoor 提供方不可用而阻塞，Task 15 已完成，
下一阶段执行 Task 16。

```text
继续仓库 /Users/andy/DevOps/SourceCode/nebula-projects/nebula 中的 Nebula 2.1 示例应用完全验证。

开始前阅读 AGENTS.md、260710-handoff.md，以及 docs/changes/examples-complete-validation/ 下的
spec.md、tasks.md、log.md、results.md。

当前已完成：
- Task 0 至 Task 4：整仓基线、中间件预检和基础 Starter 验证完成。
- Task 5：可执行部分完成；OpenAI 测试账号 429 quota，真实调用仍为 BLOCKED。
- Task 6：rpc-async-example 38/38 通过，证据为 target/example-e2e/20260711-072653-44853/。
- Task 7：microservice-example 34/34 通过，证据为 target/example-e2e/20260711-073534-69263/。
- Task 8：gateway-example 23/23 通过，证据为 target/example-e2e/20260711-074039-81648/。
- Task 9：fullstack-example 数据与缓存 49/49 通过，证据为
  target/example-e2e/20260711-121534-84726/。四种数据模式、逻辑删除、多级缓存和组合路由均有真实证据。
- Task 10：fullstack-example 通用模块 93/93 通过，证据为
  target/example-e2e/20260711-124401-71092/。RabbitMQ、Elasticsearch、MinIO、Task、支付、通知、
  Web 缓存和限流均有真实证据，5 个进程和全部临时资源已清理。
- Task 11：可执行部分 119 PASS、0 FAIL、0 SKIP、1 BLOCKED，证据为
  target/example-e2e/20260711-130632-40522/。发现客户端、Fullstack gRPC Server 和 MCP tools/resources
  均有真实证据；OpenAI 测试账号仍返回 429 quota，因此 AI、向量存储和 RAG 未折算为 PASS。
- Task 12：crawler-example 17/17 通过，证据为 target/example-e2e/20260711-142401-29430/。
  HTTP 单页、批量、解析、超时、非法 URL，以及 Playwright WebSocket、JS 渲染 DOM 和截图均有真实证据。
- Task 13：websocket-example 19/19 通过，证据为 target/example-e2e/20260711-143510-53906/。
  REST、双客户端、定向发送、心跳、前端构建、Playwright 和应用内浏览器流程均通过。
- Task 14：可执行部分 11 PASS、0 FAIL、0 SKIP、1 BLOCKED，证据为
  target/example-e2e/20260711-144556-75187/；唯一阻塞为真实 Vocoor 提供方。
- Task 15：示例文档与配置统一完成；187 个活动 Markdown 链接 0 失效，15 份旧 Fullstack 手册已归档。

本次 Goal 的第一目标：执行 tasks.md 的 Task 16；外部条件恢复后复跑 Task 5、Task 11 和 Task 14。

Task 16 要求：
1. 运行 `mvn clean test` 和 `mvn -f infrastructure/crawler/pom.xml test`。
2. 运行 `E2E_MODE=full examples/e2e-all.sh`，13 组结果必须如实汇总。
3. WebSocket 与 OAuth 前端分别执行 `npm ci && npm run build`。
4. 对全部 Shell 执行 `bash -n`，再运行 `git diff --check` 和活动 Markdown 链接检查。
5. 审计 16 个进程、契约 JAR、端口、临时容器、卷和 namespaced 数据清理情况。
6. Task 5、Task 11、Task 14 的外部阻塞不得折算为通过；条件未恢复时 Goal 保持未完成。
7. 同步 tasks.md、log.md、results.md、260710-handoff.md 和下一次 Goal 提示词。

执行纪律：保留已有改动；只关闭受管 PID；不打印或提交密钥；先定位根因再最小修复；使用
Conventional Commits 小步提交；提交前运行相关测试、E2E、bash -n、git diff --check 和活动 Markdown
链接检查；禁止推送。

只有 13 组示例、16 个进程、
契约 JAR、两个前端和浏览器流程、整仓测试、Shell、Markdown 链接及资源清理全部通过，且 tasks.md
全部勾选时，才能把 Goal 标记为 complete。
```
