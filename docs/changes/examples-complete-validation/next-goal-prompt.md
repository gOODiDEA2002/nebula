# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4、Task 6 至
Task 10、Task 12、Task 13 和 Task 15 已完成。Task 5、Task 11、Task 14 及 Task 16 只剩外部条件门禁。

```text
继续仓库 /Users/andy/DevOps/SourceCode/nebula-projects/nebula 中的 Nebula 2.1 示例应用完全验证。

开始前阅读 AGENTS.md、260710-handoff.md，以及 docs/changes/examples-complete-validation/ 下的
spec.md、tasks.md、log.md、results.md。

当前状态：
- Task 0 至 Task 4、Task 6 至 Task 10、Task 12、Task 13 和 Task 15 已完成。
- Task 5 和 Task 11 的可执行部分已完成；当前 OpenAI 测试账号返回 429 quota，
  真实聊天、embedding、向量存储和 RAG 仍为 BLOCKED。
- Task 14 的可执行部分为 11 PASS、0 FAIL、0 SKIP、1 BLOCKED；真实 Vocoor 提供方不可达，
  且外部仓库不能依赖现有业务中间件做非隔离启动。
- Task 16 的可执行检查已完成：Maven 合计 928 tests、0 failures、0 errors、3 skips；
  两个前端、31 个 Shell、187 个活动 Markdown 链接和全部资源清理通过。
- 最终 Full E2E 证据为 target/example-e2e/20260711-222120-72573/，13 组汇总为
  10 PASS、0 FAIL、0 SKIP、3 BLOCKED。三个阻塞正是两个 OpenAI 场景和一个 Vocoor 场景。
- 聚合预检的 Nacos 回收窗口和 MySQL/Elasticsearch 容器生命周期已修复，提交为
  f3f659d5。禁止推送。

本次 Goal 的第一目标：在外部条件可用后复跑 Task 5、Task 11 和 Task 14，然后取得
0 FAIL、0 SKIP、0 BLOCKED 的最终 Full E2E 汇总并完成 Task 16。

验收要求：
1. 密钥只能从环境变量读取，不得打印或提交密钥和完整敏感响应。
2. Task 5 完成最少量真实聊天、显式 embedding、Chroma 写入、相似度查询和删除复核。
3. Task 11 完成 Fullstack 聊天、embedding、文档写入、相似度查询、RAG 问答和删除复核。
4. Task 14 使用可隔离的 Vocoor 提供方完成授权码、state、用户绑定、JWT 会话、退出和
   浏览器流程。
5. 复跑 `E2E_MODE=full examples/e2e-all.sh`，13 组必须为 0 FAIL、0 SKIP、0 BLOCKED。
6. 再次执行两个前端构建、全部 Shell `bash -n`、`git diff --check`、活动 Markdown 链接和资源审计。
7. 同步 tasks.md、log.md、results.md、260710-handoff.md 和本提示词。

执行纪律：保留已有改动；只关闭受管 PID；先定位根因再最小修复；使用 Conventional Commits
小步提交；禁止推送。外部条件仍不可用时保留 BLOCKED，不得折算为 PASS。

只有 tasks.md 全部勾选，13 组示例、16 个进程、契约 JAR、两个前端和浏览器流程、整仓测试、
Shell、Markdown 链接及资源清理全部通过时，才能把 Goal 标记为 complete。
```
