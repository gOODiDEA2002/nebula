# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4、Task 6 至 Task 8 已完成，
Task 5 因 OpenAI 测试账号额度不足暂时阻塞，下一阶段从 Task 9 开始。

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
  真实代理、401、有效 JWT、Order 到 User 调用、Redis 429 和令牌恢复均有证据。

本次 Goal 的第一目标：完成 tasks.md 的 Task 9，并做阶段提交；有额度的 OpenAI 密钥可用后复跑 Task 5。

Task 9 要求：
1. 核对 fullstack-example 的 SQL、默认、readwrite、sharding、combined 配置与真实端点。
2. 使用 docker/verification/docker-compose.yml 的隔离 MySQL 8.3，不修改 nebula-data 仓库或现有卷。
3. 默认模式完成产品 CRUD、分页和逻辑删除；数据使用 nebula_e2e_ 前缀并在结束时删除。
4. Redis 完成 set/get/delete、TTL 到期和 L1/L2 更新一致性，清理所有 namespaced Key。
5. readwrite、sharding、combined 分别启动，验证真实写入、读取和路由证据。
6. Full 必须为 0 FAIL、0 SKIP、0 BLOCKED；清理数据库、Redis、容器、卷、端口和受管进程。
7. 同步 tasks.md、log.md、results.md、260710-handoff.md 和下一次 Goal 提示词。

执行纪律：保留已有改动；只关闭受管 PID；不打印或提交密钥；先定位根因再最小修复；使用
Conventional Commits 小步提交；提交前运行相关测试、E2E、bash -n、git diff --check 和活动 Markdown
链接检查；禁止推送。

Task 9 完成后继续 Task 10 至 Task 16，并保留 Task 5 的复跑要求。只有 13 组示例、16 个进程、
契约 JAR、两个前端和浏览器流程、整仓测试、Shell、Markdown 链接及资源清理全部通过，且 tasks.md
全部勾选时，才能把 Goal 标记为 complete。
```
