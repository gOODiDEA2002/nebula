# 下一次 Codex Goal 提示词

以下内容用于继续当前的「Nebula 示例应用完全验证」Goal。Task 0 至 Task 4 已完成，Task 5 因
OpenAI 测试账号额度不足暂时阻塞，下一阶段从 Task 6 开始。

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
- Task 3：starter-api 5/5、starter-minimal 3/3、starter-web 6/6 通过；无残留进程或外部数据。
- Task 4：starter-service 16/16、starter-all 7/7 通过；HTTP RPC、Redis Lock、失败路径和零依赖模式均有直接证据。
- Task 5：禁用模式、AI Bean 创建、Starter 依赖、OpenAI `/v1` 地址、零重试和清理已验证；当前
  `OPENAI_API_KEY` 返回 429 quota，真实聊天、embedding 和 Chroma 读写仍为 BLOCKED。
- Full 模式总入口现在默认执行 mvn -q install -DskipTests，避免独立示例加载旧 SNAPSHOT。确认源码没有变化时可设置 E2E_INSTALL_FRAMEWORK=false 加快局部复跑。

本次 Goal 的第一目标：完成 tasks.md 的 Task 6，并做阶段提交；有额度的 OpenAI 密钥可用后复跑 Task 5。

Task 6 要求：
1. 构建并安装 rpc-async-example 的 API 模块，启动 Service 8081 和 Client 8082。
2. 核对 Nacos 中真实服务名、实例地址和临时注册数据，不以端口可达替代协议证据。
3. 提交异步任务并轮询 PENDING/RUNNING 到 SUCCESS，核对最终结果内容。
4. 验证批量任务、同步调用、不存在 ID、取消任务和 Client 重启后记录仍可读取。
5. 使用 nebula_e2e_ 前缀，结束后只关闭受管 PID，并清理 Nacos 临时实例和测试记录。

执行纪律：
- 先执行 git status --short --branch，遇到已有改动要识别来源并保留，禁止回退用户或前序 Agent 的改动。
- 严格按 tasks.md 顺序推进；只有直接证据齐全后才能勾选任务。
- 每发现一个问题，先定位根因，再做最小修复和回归验证；同步 log.md 与 results.md。
- E2E_MODE=full 中 0 SKIP、0 BLOCKED 才能算该阶段完全通过。
- 测试数据统一使用 nebula_e2e_ 前缀并在结束时删除；只关闭测试记录的 PID。
- 不修改独立 nebula-data 仓库的配置、卷或现有数据。需要 MySQL 8.3 和 Elasticsearch 9.4.2 时使用 docker/verification/docker-compose.yml。
- 不得打印、提交或写入 API Key、JWT Secret、OAuth Secret、数据库密码。
- 每完成一个阶段按“已完成 / 进行中 / 阻塞与风险 / 下一步 / 待拍板”汇报。
- 使用 Conventional Commits 小步提交；提交前运行相关 E2E、bash -n、git diff --check 和活动 Markdown 链接检查。不要推送。

Task 6 完成后继续 Task 7 至 Task 16，并保留 Task 5 的复跑要求，不要缩小原 Goal。最终门禁仍是：13 组示例、16 个进程和契约 JAR 全部有真实证据；E2E Full 为 0 FAIL、0 SKIP、0 BLOCKED、0 未验证；两个前端构建和浏览器流程通过；整仓测试、Shell、Markdown 链接、资源清理全部通过。只有 tasks.md 全部勾选时才能把 Goal 标记为 complete。
```
