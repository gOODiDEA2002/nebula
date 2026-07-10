# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4 和 Task 6 已完成，
Task 5 因 OpenAI 测试账号额度不足暂时阻塞，下一阶段从 Task 7 开始。

```text
继续仓库 /Users/andy/DevOps/SourceCode/nebula-projects/nebula 中的 Nebula 2.1 示例应用完全验证。

开始前先阅读：
- AGENTS.md
- 260710-handoff.md
- docs/changes/examples-complete-validation/spec.md
- docs/changes/examples-complete-validation/tasks.md
- docs/changes/examples-complete-validation/log.md
- docs/changes/examples-complete-validation/results.md

当前已完成：
- Task 0：整仓基线。mvn clean test 为 913 tests、0 failures、0 errors、3 skips；Crawler 聚合模块通过。
- Task 1：E2E 通用框架和 13 组总入口已加固，Full 模式不允许静默跳过或误杀未知进程。
- Task 2：Redis、RabbitMQ、Nacos、MinIO、Chroma、隔离 MySQL 8.3 和 Elasticsearch 9.4.2 的协议级预检 10/10 通过。
- Task 3：starter-api 5/5、starter-minimal 3/3、starter-web 6/6 通过。
- Task 4：starter-service 16/16、starter-all 7/7 通过。
- Task 5：禁用模式、AI Bean 创建、Starter 依赖、OpenAI `/v1` 地址、零重试和清理已验证；当前
  `OPENAI_API_KEY` 返回 429 quota，真实聊天、embedding 和 Chroma 读写仍为 BLOCKED。
- Task 6：rpc-async-example 38/38 通过；单条、批量、同步、404、取消、Nacos 持久化和 Client
  重启恢复均有真实证据。最终证据为 target/example-e2e/20260711-072653-44853/。
- 异步取消缺陷已修复并提交为 9dd7cd36；Nacos 刚发布后的短暂不可见场景已有回归测试。

本次 Goal 的第一目标：完成 tasks.md 的 Task 7，并做阶段提交；有额度的 OpenAI 密钥可用后复跑 Task 5。

Task 7 要求：
1. 构建并安装 microservice-example 的 user-api 和 order-api，启动 User 与 Order 两个服务。
2. 核对两个真实 spring.application.name、HTTP 端口、gRPC 端口以及 Nacos metadata。
3. 通过 User HTTP API 完成新增、查询、更新和删除，严格核对状态码与业务字段。
4. 通过 Order 创建订单，证明内部真实调用 User；响应和 User 服务端日志都要包含同一测试用户证据。
5. 对 HTTP RPC 和 gRPC 分别执行至少一次成功调用和一次错误输入，不以端口可达替代协议证据。
6. 使用 nebula_e2e_ 前缀；清理数据库临时数据、Nacos 实例、端口和全部受管进程。
7. E2E Full 必须为 0 FAIL、0 SKIP、0 BLOCKED，并同步 tasks.md、log.md、results.md、
   260710-handoff.md 和下一次 Goal 提示词。

执行纪律：
- 先执行 git status --short --branch，保留已有改动，禁止回退前序 Agent 或用户的修改。
- 严格按 tasks.md 顺序推进；只有直接证据齐全后才能勾选任务。
- 每发现一个问题，先定位根因，再做最小修复和回归验证。
- 测试数据统一使用 nebula_e2e_ 前缀并在结束时删除；只关闭测试记录的 PID。
- 不修改独立 nebula-data 仓库的配置、卷或现有数据。
- 不得打印、提交或写入 API Key、JWT Secret、OAuth Secret、数据库密码。
- 使用 Conventional Commits 小步提交；提交前运行相关测试、E2E、bash -n、git diff --check
  和活动 Markdown 链接检查。不要推送。

Task 7 完成后继续 Task 8 至 Task 16，并保留 Task 5 的复跑要求，不要缩小原 Goal。最终门禁仍是：
13 组示例、16 个进程和契约 JAR 全部有真实证据；E2E Full 为 0 FAIL、0 SKIP、0 BLOCKED、
0 未验证；两个前端构建和浏览器流程通过；整仓测试、Shell、Markdown 链接、资源清理全部通过。
只有 tasks.md 全部勾选时才能把 Goal 标记为 complete。
```
