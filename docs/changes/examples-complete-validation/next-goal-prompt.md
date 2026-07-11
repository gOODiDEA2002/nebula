# 下一次 Codex Goal 提示词

继续仓库 `/Users/andy/DevOps/SourceCode/nebula-projects/nebula` 中的 Nebula 2.1 示例应用完全验证。

开始前阅读 `AGENTS.md`、`260710-handoff.md`，以及
`docs/changes/examples-complete-validation/` 下的 `spec.md`、`tasks.md`、`log.md` 和 `results.md`。

当前状态：

- Task 0 至 Task 13、Task 15 已完成。
- Task 5 已使用腾讯 MaaS Hy3 完成，证据为
  `target/example-e2e/20260712-072205-15624/`，19 PASS、0 FAIL、0 SKIP、0 BLOCKED。
- Task 11 已使用腾讯 MaaS Hy3 完成，证据为
  `target/example-e2e/20260712-072229-16825/`，125 PASS、0 FAIL、0 SKIP、0 BLOCKED。
- Task 14 的可执行部分为 11 PASS、0 FAIL、0 SKIP、1 BLOCKED；真实 Vocoor 提供方不可达，
  授权码、用户绑定、JWT 会话、退出和浏览器流程尚未完成。
- Task 16 的 Maven、Crawler、前端、Shell、链接和资源审计已通过。上一轮 13 组 Full 汇总含
  3 个 BLOCKED，其中两个 AI 阻塞已通过定向复跑解除，但尚未产生新的最终聚合证据。
- 禁止推送；任何密钥、Token 和密码只能从环境变量读取，不得打印或提交。

本次 Goal 的第一目标：完成 Task 14，然后取得 0 FAIL、0 SKIP、0 BLOCKED 的最终 Full E2E 汇总，
完成 Task 16。

执行要求：

1. 使用可隔离的真实 Vocoor 提供方，不得连接或修改共享业务数据。
2. Task 14 完成授权地址、state、防重放、授权码、用户绑定、JWT 会话、退出和真实浏览器流程。
3. 清理 OAuth 隔离 MySQL、容器、卷、端口、受管进程和临时数据。
4. 复跑 `E2E_MODE=full examples/e2e-all.sh`，13 组必须为 0 FAIL、0 SKIP、0 BLOCKED。
5. 核对两个前端构建、浏览器流程、整仓测试、Crawler 测试、Shell、活动 Markdown 链接和资源清理。
6. 同步 `tasks.md`、`log.md`、`results.md`、`260710-handoff.md` 和本提示词。
7. 使用 Conventional Commits 小步提交；提交前运行相关测试、`bash -n`、`git diff --check`
   和活动 Markdown 链接检查；禁止推送。

只有 `tasks.md` 全部勾选，13 组示例、16 个进程和契约 JAR 均有真实证据，最终 Full E2E、
两个前端、浏览器流程、整仓测试、Shell、Markdown 链接和资源清理全部通过时，才能将 Goal 标记为 complete。
