# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4、Task 6 至 Task 10 已完成，
Task 5 因 OpenAI 测试账号额度不足暂时阻塞，下一阶段从 Task 11 开始。

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

本次 Goal 的第一目标：完成 tasks.md 的 Task 11，并做阶段提交；有额度的 OpenAI 密钥可用后复跑 Task 5。

Task 11 要求：
1. 核对 Fullstack 当前 HTTP RPC、发现客户端、gRPC Server、AI 和 MCP 配置与真实端点，删除或修正失效字段。
2. 明确 HTTP RPC Server 是否应在 Fullstack 中启用；验证发现客户端和 gRPC Server 的真实请求与响应。
3. MCP 完成 tools 列表、至少一次工具调用、resources 列表和资源读取，不得只检查进程或 Bean。
4. AI 使用环境变量注入密钥、模型、Chroma host/port 和临时 collection；不得打印密钥或完整敏感响应。
5. 有额度的密钥可用时，完成最少量聊天、显式 embedding、文档写入、相似度查询、问答和删除复核。
6. 若当前密钥仍返回 429，保留真实 BLOCKED 证据，不得折算为 PASS；继续完成 RPC、MCP 和后续独立任务。
7. Full 必须为 0 FAIL、0 SKIP、0 BLOCKED 才能勾选 Task 11；清理 Nacos 记录、Chroma collection、
   端口和受管进程。
8. 同步 tasks.md、log.md、results.md、260710-handoff.md 和下一次 Goal 提示词。

执行纪律：保留已有改动；只关闭受管 PID；不打印或提交密钥；先定位根因再最小修复；使用
Conventional Commits 小步提交；提交前运行相关测试、E2E、bash -n、git diff --check 和活动 Markdown
链接检查；禁止推送。

Task 11 完成后继续 Task 12 至 Task 16，并保留 Task 5 的复跑要求。只有 13 组示例、16 个进程、
契约 JAR、两个前端和浏览器流程、整仓测试、Shell、Markdown 链接及资源清理全部通过，且 tasks.md
全部勾选时，才能把 Goal 标记为 complete。
```
