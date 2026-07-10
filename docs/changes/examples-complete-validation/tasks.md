# 任务拆分 -- 示例应用完全验证

> 事实来源：[spec.md](spec.md)
> 执行结果写入：`docs/changes/examples-complete-validation/results.md`
> 验证过程中发现的决策与问题写入：[log.md](log.md)

## 前置条件

- [x] Spec 的待澄清项已处理，或已经准备不修改外部仓库的隔离验证方案。
- [x] 当前代码已提交，`git status --short` 没有来源不明的变更。
- [x] OrbStack 和 Docker Compose 项目 `nebula-data` 正在运行。
- [x] Java 21、Maven、Node.js、npm、Docker、curl、jq、nc 可用。
- [x] AI 密钥仅通过环境变量提供，不打印变量值。

## Task 0：建立基线和证据目录

- **目标**：固定验证基线，避免后续把旧产物或脏工作区当作结果。
- **涉及文件**：
  - `docs/changes/examples-complete-validation/results.md` -- 新增结果矩阵和证据索引。
  - `docs/changes/examples-complete-validation/log.md` -- 追加执行时间线。
- **执行**：
  1. 记录 `git rev-parse HEAD`、Java、Maven、Node.js、npm 和 Docker 版本。
  2. 运行 `mvn clean test`，统计 Surefire 的 tests、failures、errors、skipped。
  3. 运行 `mvn -f infrastructure/crawler/pom.xml test`。
  4. 运行 `npm ci && npm run build` 的前置工具检查，不在此阶段安装全局包。
- **验收标准**：基线命令、退出码、测试数量和已知告警写入 `results.md`。
- [x] 完成

## Task 1：加固 E2E 通用框架和总入口

- **目标**：测试失败必须返回非 0；完全模式不允许静默跳过或误杀进程。
- **涉及文件**：
  - `examples/e2e-common.sh` -- 进程、日志、断言、依赖检查和清理。
  - `examples/e2e-all.sh` -- 新增总入口，按依赖顺序执行全部示例。
  - `examples/README.md` -- 记录 E2E 运行方式和模式。
- **实施要求**：
  1. 删除对未知端口 PID 的 `kill -9`；端口占用时打印进程并失败。
  2. 只保存和关闭当前脚本创建的 PID，先发 `TERM`，超时后才对该 PID 发 `KILL`。
  3. 日志保存到 `target/example-e2e/<run-id>/`，不使用会被其他任务覆盖的固定 `/tmp` 文件。
  4. 增加状态码、JSON 字段、等待条件、日志禁用词和最终状态断言。
  5. `E2E_MODE=full` 时依赖缺失必须失败；`smoke` 模式才允许明确跳过。
  6. 总入口汇总每组示例的 PASS/FAIL/SKIP、耗时和日志路径。
- **验证命令**：`bash -n examples/e2e-common.sh examples/e2e-all.sh examples/*/e2e-test.sh`
- **验收标准**：人为制造一个失败断言时总入口返回非 0；正常运行不会清理非测试进程。
- [x] 完成

## Task 2：中间件协议级预检与隔离环境

- **目标**：所有外部依赖的版本和真实功能满足示例要求。
- **涉及文件**：
  - `examples/e2e-common.sh` -- 增加协议级预检。
  - `examples/e2e-all.sh` -- 增加中间件准备阶段。
  - 可选 `docker/verification/docker-compose.yml` -- 隔离的 Elasticsearch 9.4.2 和浏览器服务。
- **检查项**：
  1. Redis 执行 namespaced `SET/GET/DEL`，明确是否启用密码。
  2. RabbitMQ 调用管理 API，并创建、发布、消费、删除临时队列。
  3. MySQL 执行 `SELECT 1`，按仓库 SQL 初始化三个示例数据库。
  4. Nacos 使用认证 API 登录，并验证注册、查询、注销临时实例。
  5. MinIO 创建、上传、下载、删除临时对象和 Bucket。
  6. Chroma 调用 v2 heartbeat，并使用临时 collection 验证增删查。
  7. 启动独立 Elasticsearch 9.4.2 容器和独立卷，在非冲突端口执行索引读写。
  8. XXL-JOB 当前健康检查缺少 `curl`，记录为环境配置问题；若本轮启用 XXL-JOB，改用镜像支持的探针。
- **安全要求**：未经授权不修改或删除 `nebula-data` 的现有卷和数据。
- **验收标准**：协议级预检全部通过，版本信息写入 `results.md`，无端口探测替代真实读写。
- [x] 完成

## Task 3：验证契约、Minimal 和 Web Starter

- **目标**：验证最小依赖、契约打包和基础 Web 能力。
- **涉及文件**：
  - `examples/starter-minimal-example/e2e-test.sh`
  - `examples/starter-web-example/e2e-test.sh`
  - `examples/starter-api-example/README.md`
- **验证命令与断言**：
  1. `mvn -f examples/starter-api-example clean install`，检查 JAR 包含 `UserApi` 且不引入运行时 Web 服务。
  2. `starter-minimal-example` 出现 `Started MinimalApplication` 后正常退出，退出码为 0。
  3. `starter-web-example` 的 `/hello`、`/health/ping`、`/performance/status` 和 `/v3/api-docs` 返回预期状态及字段。
  4. 关闭 Web 应用后确认 8080 端口释放。
- **验收标准**：3 组示例全部 PASS，无外部依赖和残留进程。
- [x] 完成

## Task 4：验证 Service 和 All Starter

- **目标**：验证 Redis Lock、自动配置开关和 HTTP RPC 示例行为。
- **涉及文件**：
  - `examples/starter-service-example/application.yml`
  - `examples/starter-service-example/e2e-test.sh`
  - `examples/starter-service-example/README.md`
  - `examples/starter-all-example/e2e-test.sh`
- **实施要求**：
  1. 明确 Service 示例是默认启用 HTTP RPC，还是 README 只展示按需启用；配置、代码和文档必须一致。
  2. 默认模式验证 `/health/ping`、性能端点和 Redis Lock Bean 可用。
  3. HTTP RPC 模式验证 `/rpc/hello`、`/rpc/hello/greet`、`/rpc/hello/info` 的真实返回。
  4. All 示例验证 `/hello`、健康端点和 Starter 默认值；禁用模块不得建立外部连接。
  5. Redis 不可用时，显式启用 Lock 的应用应快速失败并给出可理解错误，不能假启动。
- **验收标准**：配置和 README 不再矛盾，两个示例的默认模式和功能模式均通过。
- [x] 完成

## Task 5：验证 AI Starter 的禁用和启用模式

- **目标**：同时证明无密钥可启动和有密钥可真实调用，不能只验证降级文案。
- **涉及文件**：
  - `examples/starter-ai-example/application.yml`
  - `examples/starter-ai-example/e2e-test.sh`
  - `examples/starter-ai-example/README.md`
- **实施要求**：
  1. 禁用模式：`/ai/echo` 返回 `AI disabled`，应用不创建 AI 远程客户端。
  2. 启用模式：使用环境变量中的测试密钥发送最少量聊天请求，返回非空内容。
  3. 核对 Chroma 地址与 `nebula-data` 的 9002 映射，执行一次 embedding、写入、相似度查询和清理。
  4. 记录调用次数，不记录密钥和完整敏感响应。
- **验收标准**：禁用模式和启用模式都 PASS，Chroma 临时 collection 已删除。
- **当前状态**：禁用模式、启用时 Bean 创建、依赖传递和清理已验证；测试账号返回 OpenAI 429
  quota 错误，真实聊天、embedding 和 Chroma 读写暂记 `BLOCKED`。待提供有额度的测试密钥后复跑。
- [ ] 完成

## Task 6：验证异步 RPC

- **目标**：验证 Service、Client、Nacos 存储和异步状态流转。
- **涉及文件**：
  - `examples/rpc-async-example/e2e-test.sh`
  - 发现问题时修改对应 Service、Client 配置或代码。
- **验证步骤**：
  1. 构建并安装 `api` 模块。
  2. 启动 Service 8081 和 Client 8082，核对 Nacos 注册名和实例地址。
  3. 提交异步任务，轮询 `PENDING/RUNNING` 到 `SUCCESS`，再核对结果内容。
  4. 验证批量任务、同步调用、查询不存在 ID 和取消任务。
  5. 重启 Client 后确认 Nacos 存储中的执行记录仍可读取。
- **验收标准**：状态流转和结果真实可见，不只断言存在 `executionId`。
- **完成证据**：`target/example-e2e/20260711-072653-44853/`，38 PASS、0 FAIL、0 SKIP、
  0 BLOCKED；Nacos 执行记录、注册实例、8081 和 8082 端口均已清理。
- [x] 完成

## Task 7：验证微服务 HTTP RPC、gRPC 和发现

- **目标**：证明 User、Order 通过 Nacos 和 RPC 完成跨服务业务。
- **涉及文件**：
  - `examples/microservice-example/e2e-test.sh`
  - `examples/microservice-example/user-service/test-rpc.sh`
  - `examples/microservice-example/user-service/test-grpc.sh`
  - `examples/microservice-example/order-service/test-grpc.sh`
- **验证步骤**：
  1. 构建两个 API 契约模块，启动 User 和 Order。
  2. 核对 Nacos 中两个真实 `spring.application.name`、HTTP 端口和 gRPC metadata。
  3. 通过 User HTTP API 完成新增、查询、更新、删除。
  4. 通过 Order 创建订单，证明内部真实调用 User，而不是返回本地固定数据。
  5. 对 HTTP RPC 和 gRPC 分别执行至少一次成功调用和一次错误输入。
- **验收标准**：两个协议都通过，跨服务调用有服务端日志和响应双重证据。
- [ ] 完成

## Task 8：验证 Gateway 真实代理、认证和限流

- **目标**：请求必须被代理到真实后端；`404/503` 不能算通过。
- **涉及文件**：
  - `examples/gateway-example/application.yml`
  - `examples/gateway-example/e2e-test.sh`
  - `examples/gateway-example/README.md`
  - 必要时修改 Gateway 路由配置实现并补单元测试。
- **实施要求**：
  1. 统一 Gateway 配置的服务名与 Nacos 实际注册名。
  2. 明确 `/api/**` 到后端 `/rpc/**` 的映射规则，配置路径重写或统一控制器路径。
  3. 白名单请求得到后端 200 和真实业务 JSON。
  4. 受保护路径无 Token 返回 401；由同一密钥签发的有效 Token 返回 200。
  5. 降低测试限流阈值，连续请求必须出现 429，等待补充令牌后恢复 200。
- **验收标准**：代理、认证、限流各有严格状态码和响应断言，不能接受替代状态。
- [ ] 完成

## Task 9：验证 Fullstack 数据库、缓存和数据模式

- **目标**：验证默认、读写分离、分片和组合 Profile。
- **涉及文件**：
  - `examples/fullstack-example/e2e-test.sh`
  - `examples/fullstack-example/application*.yml`
  - `examples/fullstack-example/sql/*.sql`
- **验证步骤**：
  1. 执行示例 SQL，核对 `t_product` 和两个分片库的表及基线数据。
  2. 修正 Redis 密码配置与实际容器认证方式不一致的问题。
  3. 默认 Profile 完成产品 CRUD、分页和逻辑删除。
  4. 缓存完成 set/get/delete、TTL 到期、L1/L2 更新一致性。
  5. `readwrite`、`sharding`、`combined` 分别启动并完成写入、读取、路由核对。
- **验收标准**：四种数据模式全部 PASS，数据库和 Redis 中无 E2E 临时数据。
- [ ] 完成

## Task 10：验证 Fullstack 消息、搜索、存储和通用模块

- **目标**：现有脚本不得再主动关闭 RabbitMQ 或把服务可达当成功。
- **涉及文件**：
  - `examples/fullstack-example/e2e-test.sh`
  - `examples/fullstack-example/application.yml`
  - 对应模块测试文档和控制器。
- **验证步骤**：
  1. RabbitMQ：发送、消费、延迟消息、失败重试和临时资源清理。
  2. Elasticsearch 9.4.2：创建索引、单条和批量写入、查询、建议、删除索引。
  3. MinIO：创建 Bucket、上传、下载字节比对、预签名 URL、删除对象和 Bucket。
  4. Task、Mock Payment、Mock Notification 各完成成功路径和错误输入。
  5. 健康、性能、限流、响应缓存和脱敏端点完成行为断言。
- **验收标准**：所有模块均为真实功能验证，0 SKIP；临时队列、索引、对象和缓存全部删除。
- [ ] 完成

## Task 11：验证 Fullstack RPC、AI 和 MCP

- **目标**：验证综合应用中最容易被默认配置掩盖的远程能力。
- **涉及文件**：
  - `examples/fullstack-example/e2e-test.sh`
  - `examples/fullstack-example/application.yml`
  - `examples/fullstack-example/docs/nebula-ai-test.md`
  - `examples/fullstack-example/docs/nebula-mcp-test.md`
- **验证步骤**：
  1. 删除或修正已经不存在的 HTTP RPC 配置字段。
  2. 核对 HTTP RPC Server 当前关闭是否符合控制器设计；验证发现客户端和 gRPC Server。
  3. 使用测试密钥执行聊天、embedding、文档写入、相似度搜索和问答。
  4. 验证 MCP tools 列表、工具调用、resources 列表和资源读取。
  5. 限制外部 AI 请求数量并清理 Chroma collection。
- **验收标准**：RPC、AI、Vector Store 和 MCP 均有真实成功响应，0 SKIP。
- [ ] 完成

## Task 12：验证 HTTP 和 Browser Crawler

- **目标**：验证静态页面和 JavaScript 页面两种抓取方式。
- **涉及文件**：
  - `examples/crawler-example/e2e-test.sh`
  - `docker/crawler-browser/docker-compose.yml`
  - `examples/crawler-example/README.md`
- **验证步骤**：
  1. HTTP 模式验证单页、批量、解析、超时和非法 URL。
  2. 使用本地受控测试页面代替只依赖公共网站，公共网站只作为补充。
  3. 启动 Playwright 容器，验证镜像版本和 WebSocket/CDP 地址。
  4. Browser 模式抓取 JavaScript 渲染内容，并验证截图或 DOM 结果。
  5. Browser 不可用时验证清晰降级，但完全模式仍应判为未完成。
- **验收标准**：两个引擎都 PASS，浏览器容器由测试清理。
- [ ] 完成

## Task 13：验证 WebSocket 后端和前端

- **目标**：验证 REST、真实 WebSocket 双向通信和前端构建。
- **涉及文件**：
  - `examples/websocket-example/e2e-test.sh`
  - `examples/websocket-example/frontend/package.json`
  - 必要时增加前端浏览器测试。
- **验证步骤**：
  1. 后端状态、广播、指定用户和指定会话接口通过。
  2. 使用仓库内声明的测试依赖，不允许因为缺少 Python `websockets` 包而跳过。
  3. 建立两个客户端，验证连接通知、广播、定向消息、心跳和断开后的在线数。
  4. 前端执行 `npm ci`、`npm run build`，再用浏览器完成连接和发送消息。
- **验收标准**：REST、协议和 UI 三层全部 PASS，3000 和 8086 端口释放。
- [ ] 完成

## Task 14：验证 OAuth 后端、前端和真实授权码流程

- **目标**：移除本机地址和硬编码秘密，完成真实 OAuth 登录。
- **涉及文件**：
  - `examples/oauth-example/backend/src/main/resources/application.yml`
  - `examples/oauth-example/e2e-test.sh`
  - `examples/oauth-example/README.md`
  - `examples/oauth-example/frontend/README.md`
- **实施要求**：
  1. 数据库、OAuth Server、Client ID、Client Secret 和前端 URL 改为环境变量；轮换现有测试秘密。
  2. 使用本机 MySQL 初始化 `oauth_client_demo`，不依赖固定局域网地址。
  3. 后端健康、授权入口、回调错误输入和未登录用户接口通过。
  4. 前端执行 `npm ci`、`npm run build`，统一文档端口为 4010。
  5. 启动 Vocoor OAuth 提供方，通过浏览器完成授权、回调、本地用户绑定、JWT 会话和退出。
- **验收标准**：完整授权码流程 PASS；没有秘密进入 Git、日志或结果文档。
- [ ] 完成

## Task 15：统一示例文档和配置

- **目标**：所有运行命令、端口、服务名、配置键和实际实现一致。
- **涉及文件**：
  - `examples/README.md`
  - 各示例 README 和 `application.yml`
  - `docs/INDEX.md`（如需增加验证结果入口）
- **检查项**：
  1. Gateway 端口统一为 8000。
  2. OAuth 前端端口统一为 4010。
  3. AI 的 Chroma 地址、Fullstack 的 Redis/ES 配置与验证环境一致。
  4. 删除已经不存在的配置键和不再存在的端点。
  5. 每个 README 给出最小启动、完整启动、验证命令和外部依赖。
- **验证命令**：活动 Markdown 本地链接检查；对文档中的命令抽样执行。
- **验收标准**：文档不再与代码冲突，0 失效本地链接。
- [ ] 完成

## Task 16：最终全量回归和交付审计

- **目标**：用一轮干净执行证明所有修复能一起工作。
- **执行顺序**：
  1. `mvn clean test`
  2. `mvn -f infrastructure/crawler/pom.xml test`
  3. `E2E_MODE=full examples/e2e-all.sh`
  4. 两个前端目录执行 `npm ci && npm run build`
  5. `bash -n` 检查全部 Shell 脚本
  6. `git diff --check` 和活动文档链接检查
  7. 检查端口、测试进程、临时容器和 namespaced 数据全部清理
- **完成门禁**：
  - 13 组示例有结果；16 个运行进程和契约 JAR 均已覆盖。
  - E2E 汇总为 0 FAIL、0 SKIP、0 BLOCKED。
  - `results.md` 中每一项都有命令、状态和证据，宽泛结论有同等范围的验证支撑。
  - 所有修复使用 Conventional Commits；未明确要求时不推送。
- [ ] 完成

## 变更摘要

> 全部 Task 完成后填写。

- 总文件数：待执行后统计
- Spec-Plan 偏差记录：待填写
- 遗留问题：必须为无；存在未验证项时不得完成 Goal
