# Codex Goal 提示词 -- Nebula 示例应用完全验证

以下内容可直接设置为 Codex Goal：

```text
在仓库 /Users/andy/DevOps/SourceCode/nebula-projects/nebula 中，完成 Nebula 2.1 升级后的示例应用完全验证。

权威任务文档：
- docs/changes/examples-complete-validation/spec.md
- docs/changes/examples-complete-validation/tasks.md
- docs/changes/examples-complete-validation/log.md

当前环境：
- OrbStack 已启动。
- Docker Compose 项目 nebula-data 正在运行，配置文件位于
  /Users/andy/DevOps/SourceCode/nebula-projects/nebula-data/docker-compose.yml。
- 当前已发现 9 个容器：Redis、RabbitMQ、MinIO、Elasticsearch、MySQL、XXL-JOB、Nacos、Chroma、Etcd。
- 当前 Elasticsearch 服务端为 7.17.19，框架 Java Client 为 9.4.2，不能直接作为有效搜索验证环境。
- 当前 XXL-JOB 健康检查因镜像没有 curl 而长期显示 starting，但宿主机访问会返回 HTTP 302。
- DEEPSEEK_API_KEY 和 OPENAI_API_KEY 环境变量当前已设置。不得打印或写入文件。
- Vocoor OAuth 提供方 localhost:8080 当前不可达。
- Crawler 的 Playwright 浏览器容器当前未启动。

目标：
1. 严格按 tasks.md 顺序执行并持续更新复选框。
2. 覆盖 examples/ 下全部 13 组示例、16 个可运行进程和 starter-api 契约 JAR。
3. 不只编译：每个应用都要真实启动、等待就绪、调用代表性接口、核对状态变化和日志，再正常关闭。
4. MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、MinIO、Chroma 必须执行协议级真实读写，不能只探测端口。
5. 验证 HTTP RPC、异步 RPC、gRPC、Nacos 注册、Gateway 代理/JWT/限流、数据库 CRUD、缓存、消息、搜索、存储、AI、MCP、Crawler、WebSocket 和 OAuth 主流程。
6. 两个前端都必须执行 npm ci、npm run build，并使用浏览器自动化验证 WebSocket 和 OAuth 主流程。
7. 发现代码、配置、脚本或文档问题时，定位根因、添加最小回归验证、修复并重新执行受影响测试；不要只记录问题。
8. 最终生成 docs/changes/examples-complete-validation/results.md，逐项记录命令、退出码、结果、证据、修复提交和资源清理情况。

硬性纪律：
- 先阅读仓库 AGENTS.md 和上述三份任务文档。
- 使用现有 examples/e2e-common.sh 与 examples/*/e2e-test.sh 作为起点，但先修复其静默跳过、宽松断言和进程管理问题。
- 禁止把 SKIP、404、500、503、仅端口可达或仅日志出现 Started 记为功能通过。
- E2E_MODE=full 的最终结果必须是 0 FAIL、0 SKIP、0 BLOCKED、0 未验证。
- 外部条件缺失时继续完成其他任务并记录阻塞，但保持 Goal 未完成；不得缩小目标或把 smoke test 当完全验证。
- 不得打印、提交或写入 API Key、JWT Secret、OAuth Secret、数据库密码；只记录环境变量是否存在。
- 不得按端口强杀未知进程，只能关闭当前测试记录的 PID。
- 测试数据统一使用 nebula_e2e_ 前缀，结束后删除临时 Redis Key、RabbitMQ 队列、ES 索引、MinIO 对象/Bucket、Chroma collection 和数据库记录。
- 未经明确授权，不修改或删除独立 nebula-data 仓库的配置、容器卷和现有数据。需要 ES 9.4.2 时，优先启动隔离容器和独立卷，并通过测试参数指定地址。
- Elasticsearch 7 数据卷不得直接挂载到 9.4.2。
- 每完成一个阶段，按“已完成 / 进行中 / 阻塞与风险 / 下一步 / 待拍板”汇报，并同步 tasks.md。
- 修复应小步提交，使用 Conventional Commits；提交前运行对应模块验证。未明确要求时不要推送。

最终完成门禁：
- mvn clean test 通过，并核对实际 tests/failures/errors/skipped 数量。
- mvn -f infrastructure/crawler/pom.xml test 通过。
- E2E_MODE=full examples/e2e-all.sh 通过。
- OAuth 和 WebSocket 浏览器流程通过。
- 两个前端 npm ci 和 npm run build 通过。
- 全部 Shell 脚本 bash -n 通过。
- git diff --check 和活动 Markdown 本地链接检查通过。
- 13 组示例全部有强证据，16 个进程和契约 JAR 无遗漏。
- 无残留测试进程、临时容器和临时数据。
- tasks.md 全部勾选，log.md 已记录关键决策和问题，results.md 可复核且不含秘密。

只有以上条件全部满足，才可以把 Goal 标记为 complete。任何一项缺少直接证据都继续执行，不以“未发现问题”代替完成证明。
```
