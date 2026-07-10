# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4、Task 6 和 Task 7 已完成，
Task 5 因 OpenAI 测试账号额度不足暂时阻塞，下一阶段从 Task 8 开始。

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
- Task 0：整仓基线为 913 tests、0 failures、0 errors、3 skips；Crawler 聚合模块通过。
- Task 1：E2E 通用框架和 13 组总入口已加固。
- Task 2：中间件协议级预检 10/10 通过。
- Task 3：starter-api 5/5、starter-minimal 3/3、starter-web 6/6 通过。
- Task 4：starter-service 16/16、starter-all 7/7 通过。
- Task 5：禁用模式、AI Bean 创建、依赖、`/v1` 地址、零重试和清理已验证；当前 OpenAI
  测试账号返回 429 quota，真实聊天、embedding 和 Chroma 读写仍为 BLOCKED。
- Task 6：rpc-async-example 38/38 通过，证据为 target/example-e2e/20260711-072653-44853/。
- Task 7：microservice-example 34/34 通过，证据为 target/example-e2e/20260711-073534-69263/。
  两个契约 JAR、User REST CRUD、HTTP RPC、gRPC、Nacos metadata、Order 到 User 的发现调用和
  四端口清理均有真实证据。

本次 Goal 的第一目标：完成 tasks.md 的 Task 8，并做阶段提交；有额度的 OpenAI 密钥可用后复跑 Task 5。

Task 8 要求：
1. 核对 gateway-example 的实际服务名、8000 端口、Nacos 路由、路径重写、认证过滤器和限流实现。
2. 启动或复用受控真实后端，确保白名单请求经过 Gateway 后得到后端 200 和可识别业务 JSON。
3. 受保护路径无 Token 必须返回 401；使用同一密钥签发的有效 Token 必须返回后端 200。
4. 使用测试配置降低限流阈值，连续请求必须出现 429，等待补充令牌后恢复 200。
5. 不接受 404、503 或端口可达作为代理成功；响应和后端日志必须能证明请求到达真实后端。
6. 使用 nebula_e2e_ 前缀，清理 Nacos 临时实例、Redis 限流键、端口和受管进程。
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

Task 8 完成后继续 Task 9 至 Task 16，并保留 Task 5 的复跑要求，不要缩小原 Goal。最终门禁仍是：
13 组示例、16 个进程和契约 JAR 全部有真实证据；E2E Full 为 0 FAIL、0 SKIP、0 BLOCKED、
0 未验证；两个前端构建和浏览器流程通过；整仓测试、Shell、Markdown 链接、资源清理全部通过。
只有 tasks.md 全部勾选时才能把 Goal 标记为 complete。
```
