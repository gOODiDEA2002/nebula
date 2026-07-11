# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4、Task 6 至 Task 10 已完成，
Task 5 和 Task 11 因 OpenAI 测试账号额度不足暂时阻塞，下一阶段从 Task 12 开始。

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

本次 Goal 的第一目标：完成 tasks.md 的 Task 12，并做阶段提交；有额度的 OpenAI 密钥可用后复跑 Task 5 和 Task 11。

Task 12 要求：
1. HTTP 模式完成单页、批量、解析、超时和非法 URL 验证。
2. 使用本地受控页面作为主要证据，公共网站只能作为补充，不能决定 Full 结果。
3. 启动 docker/crawler-browser/docker-compose.yml 的 Playwright 容器，核对镜像版本和 WebSocket/CDP 地址。
4. Browser 模式抓取 JavaScript 渲染内容，并保存可核对的 DOM 或截图证据。
5. Browser 不可用时可以验证清晰降级，但不得把 Task 12 Full 标记为完成。
6. Full 必须为 0 FAIL、0 SKIP、0 BLOCKED；清理浏览器容器、端口、临时页面和受管进程。
7. 同步 tasks.md、log.md、results.md、260710-handoff.md 和下一次 Goal 提示词。

执行纪律：保留已有改动；只关闭受管 PID；不打印或提交密钥；先定位根因再最小修复；使用
Conventional Commits 小步提交；提交前运行相关测试、E2E、bash -n、git diff --check 和活动 Markdown
链接检查；禁止推送。

Task 12 完成后继续 Task 13 至 Task 16，并保留 Task 5 和 Task 11 的复跑要求。只有 13 组示例、16 个进程、
契约 JAR、两个前端和浏览器流程、整仓测试、Shell、Markdown 链接及资源清理全部通过，且 tasks.md
全部勾选时，才能把 Goal 标记为 complete。
```
