# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4、Task 6 至 Task 10 已完成，
Task 5 和 Task 11 因 OpenAI 测试账号额度不足暂时阻塞，下一阶段从 Task 14 开始。

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

本次 Goal 的第一目标：完成 tasks.md 的 Task 14，并做阶段提交；有额度的 OpenAI 密钥可用后复跑 Task 5 和 Task 11。

Task 14 要求：
1. 数据库、OAuth Server、Client ID、Client Secret 和前端 URL 全部改为环境变量，轮换现有测试秘密。
2. 使用隔离 MySQL 初始化 oauth_client_demo，不依赖固定局域网地址或现有数据卷。
3. 后端健康、授权入口、回调错误输入和未登录用户接口严格通过。
4. 前端执行 npm ci 和 npm run build，开发与文档端口统一为 4010。
5. 启动 Vocoor OAuth 提供方，通过浏览器完成授权、回调、本地用户绑定、JWT 会话和退出。
6. 不得打印或提交秘密；Full 必须为 0 FAIL、0 SKIP、0 BLOCKED，并清理数据库、端口和受管进程。
7. 同步 tasks.md、log.md、results.md、260710-handoff.md 和下一次 Goal 提示词。

执行纪律：保留已有改动；只关闭受管 PID；不打印或提交密钥；先定位根因再最小修复；使用
Conventional Commits 小步提交；提交前运行相关测试、E2E、bash -n、git diff --check 和活动 Markdown
链接检查；禁止推送。

Task 14 完成后继续 Task 15 至 Task 16，并保留 Task 5 和 Task 11 的复跑要求。只有 13 组示例、16 个进程、
契约 JAR、两个前端和浏览器流程、整仓测试、Shell、Markdown 链接及资源清理全部通过，且 tasks.md
全部勾选时，才能把 Goal 标记为 complete。
```
