# 示例应用完全验证结果

> 状态：执行中
> 执行日期：2026-07-10 至 2026-07-11
> 基线提交：`6ddf69ff5594f3f3a62d418651a3f9a6fd641d54`
> 事实来源：[spec.md](spec.md)、[tasks.md](tasks.md)、[log.md](log.md)

## 1. 结果口径

- `PASS`：命令退出码为 0，且该项要求的启动、行为、集成和清理证据齐全。
- `FAIL`：实现或验证结果不符合预期。
- `BLOCKED`：外部条件不足，尚未完成验证；不得折算为通过。
- `PENDING`：尚未执行。
- 单元测试中的条件跳过按原始结果记录；最终 `E2E_MODE=full` 仍要求 0 `FAIL`、0 `SKIP`、
  0 `BLOCKED`、0 未验证。

## 2. 基线环境

| 项目 | 实际值 | 结论 |
| --- | --- | --- |
| Git 提交 | `6ddf69ff5594f3f3a62d418651a3f9a6fd641d54` | 已固定 |
| 分支 | `main`，基线提交与 `vocoor/main` 同步 | 已固定；后续修复以阶段提交继续累积 |
| Java | 21.0.7 | 满足 Java 21 要求 |
| Maven | 3.9.9 | 可用 |
| Node.js | v22.16.0 | 可用 |
| npm | 10.9.2 | 可用 |
| Docker | Client/Server 29.4.0 | 可用 |
| Docker Compose | v5.1.2 | 可用 |
| curl | 8.7.1 | 可用 |
| jq | 1.7.1 | 可用 |
| nc | 系统命令可用 | 可用 |
| AI 密钥 | `DEEPSEEK_API_KEY`、`OPENAI_API_KEY` 已设置 | 仅记录存在性 |

Docker Compose 项目 `nebula-data` 的 9 个服务已启动。该独立仓库和已有数据卷在本轮验证中保持只读；
Elasticsearch 9.4.2 等新增依赖使用隔离容器、独立端口和独立卷。

## 3. 构建与单元测试

| 验证项 | 命令 | 退出码 | 结果 | 证据 |
| --- | --- | ---: | --- | --- |
| 整仓测试 | `mvn clean test` | 0 | PASS | 70 个 Reactor 模块全部成功，耗时 1 分 20 秒 |
| Surefire 汇总 | 读取全部 `TEST-*.xml` | 0 | PASS | 126 份报告；913 个测试、0 失败、0 错误、3 个跳过 |
| Crawler 聚合模块 | `mvn -f infrastructure/crawler/pom.xml test` | 0 | PASS | 6 个模块全部成功，耗时 1.494 秒 |

3 个跳过均来自 `nebula-crawler-browser` 的 Stealth4j 集成测试。它们依赖测试类判定的外部条件，
不影响本节的构建基线结论，也不会替代 Task 12 的 Browser Crawler 完整运行验证。

## 4. 中间件预检

| 服务 | 当前版本或状态 | 完整验证结果 | 备注 |
| --- | --- | --- | --- |
| Redis | 8.2.1，healthy | PASS | 临时 Key 完成 SET/GET/DEL，删除后扫描为 0 |
| RabbitMQ | 3.12.14，healthy | PASS | 临时队列完成创建、发布、消费、删除，删除后返回 404 |
| MinIO | `RELEASE.2025-04-22T22-12-26Z`，healthy | PASS | 临时 Bucket 和对象完成上传、下载比对、删除，复核为 0 |
| Elasticsearch | 隔离服务 9.4.2 | PASS | 临时索引完成建索引、写入、搜索、删除；未使用现有 7.17.19 数据卷 |
| MySQL | 隔离服务 8.3.0 | PASS | 初始化 4 个示例库，执行产品写入、读取和删除；独立卷已删除 |
| XXL-JOB | HTTP 302，容器 unhealthy | PASS | 宿主机 HTTP 探针通过；容器健康检查缺少 `curl` 的环境问题仍保留 |
| Nacos | 2.5.1，healthy | PASS | 登录、临时实例注册、查询、注销通过，空服务清理后复核为 0 |
| Chroma | 1.0.0，v2 API | PASS | 临时 collection 完成写入、读取、向量查询和按名称删除，复核为 0 |
| Etcd | 3.5，healthy | 不适用 | 当前示例没有直接使用 |

执行命令：`E2E_MODE=full examples/e2e-middleware.sh`。最终结果为 10 PASS、0 FAIL、0 SKIP、
0 BLOCKED。验证服务使用 [docker/verification/docker-compose.yml](../../../docker/verification/docker-compose.yml)，
仅监听本机 13306 和 19200，不复用 `nebula-data` 的 MySQL、Elasticsearch 容器或数据卷。

## 5. E2E 框架验证

| 验证项 | 命令或方式 | 退出码 | 结果 | 证据 |
| --- | --- | ---: | --- | --- |
| Shell 语法 | `bash -n examples/e2e-common.sh examples/e2e-all.sh examples/*/e2e-test.sh` | 0 | PASS | 全部脚本语法有效 |
| 失败传播 | 对不可达地址执行状态码断言 | 1 | PASS | 结果为 `FAIL`，统一摘要保留失败数 |
| Full 依赖缺失 | 对不可达测试服务执行 `skip_if_no_service` | 2 | PASS | 结果为 `BLOCKED`，没有折算为成功 |
| Smoke 依赖缺失 | 对不可达测试服务执行 `skip_if_no_service` | 0 | PASS | 结果明确为 `SKIP` |
| 未知筛选 | `E2E_MODE=full E2E_ONLY=does-not-exist examples/e2e-all.sh` | 1 | PASS | 未执行任何示例时拒绝返回成功 |
| 端口安全 | 用受控 HTTP 服务占用 18999 后调用 `start_app` | 0 | PASS | 拒绝启动应用，原服务仍返回 HTTP 200 |
| 生命周期 | 临时关闭 persistence 后在 18080 启动 Web 示例 | 0 | PASS | Started、业务 HTTP 200、日志干净、TERM 关闭、端口释放 |
| 总入口单组执行 | `E2E_MODE=full E2E_ONLY=starter-api-example examples/e2e-all.sh` | 0 | PASS | 1 PASS、0 FAIL、0 SKIP、0 BLOCKED |
| 当前框架安装 | Full 模式运行总入口 | 0 | PASS | 默认先执行 `mvn -q install -DskipTests`，避免独立示例解析到旧 SNAPSHOT；可用 `E2E_INSTALL_FRAMEWORK=false` 复用已安装产物 |
| 预期启动失败 | Service 使用不可达 Redis 端口启动 | 0 | PASS | 应用非 0 退出、未监听测试端口，日志明确包含 Redis 连接失败；只清理受管 PID |

E2E 总入口覆盖 13 组示例，运行证据统一保存到 `target/example-e2e/<run-id>/`。该目录是临时产物，
不提交到 Git；可复核结论持续写入本文档。

## 6. 示例结果

| 示例组 | 进程或产物 | 状态 | 证据 |
| --- | --- | --- | --- |
| `starter-minimal-example` | Minimal 应用 | PASS | 独立构建退出码为 0，出现 `Started MinimalApplication`，且没有启动 Web Server；3/3 PASS |
| `starter-api-example` | 契约 JAR | PASS | JAR 包含 `UserApi.class`，运行时依赖树不含 Web Server；5/5 PASS；提供方和消费方的跨服务使用留在 Task 7 |
| `starter-web-example` | Web 应用 | PASS | `/hello`、健康、性能和 OpenAPI 均返回严格预期，日志干净且 8080 已释放；6/6 PASS |
| `starter-service-example` | Service 应用 | PASS | 16/16 PASS；三个 GET、通用 HTTP RPC、Redis Lock、模块开关和 Redis 失败路径均通过 |
| `starter-ai-example` | AI 应用 | BLOCKED | 禁用模式与 AI Bean 创建通过；真实 OpenAI 调用因测试账号 429 quota 阻塞，首次失败后已停止并清理 |
| `starter-all-example` | All 应用 | PASS | 零外部依赖模式 7/7 PASS；Hello、健康、性能、OpenAPI 和禁用模块日志均符合预期 |
| `rpc-async-example` | Service、Client | PASS | 38/38 PASS；单条、批量、同步、404、取消、Nacos 持久化和 Client 重启恢复均通过 |
| `microservice-example` | User、Order | PENDING | 待执行 |
| `gateway-example` | Gateway | PENDING | 待执行 |
| `fullstack-example` | Fullstack 应用 | PENDING | 待执行 |
| `crawler-example` | Crawler、Browser | PENDING | 待执行 |
| `websocket-example` | Backend、Frontend | PENDING | 待执行 |
| `oauth-example` | Backend、Frontend、OAuth 提供方 | PENDING | 待执行 |

## 7. 清理审计

| 项目 | 状态 | 证据 |
| --- | --- | --- |
| 本轮测试启动的进程 | PASS | 受控端口占用进程和 Web 生命周期进程均已关闭 |
| 隔离容器和独立卷 | PASS | 容器 0、卷 0；13306 和 19200 端口均已释放 |
| `nebula_e2e_` 临时数据 | PASS | Redis、RabbitMQ、Nacos、MinIO、Chroma 复核均为 0 |

## 8. 已知告警

1. Fullstack 编译存在 Lombok `@Builder` 默认值告警及少量弃用、未检查操作告警，基线测试未失败。
2. Elasticsearch 7.17.19 不能用于验证框架的 9.4.2 客户端，后续使用隔离的 9.4.2 服务。
3. Vocoor OAuth 提供方 `localhost:8080` 当前不可达，完整授权码流程尚未执行。
4. Playwright 浏览器容器尚未启动，Browser Crawler 完整验证尚未执行。
5. Nacos 2.5.1 按默认 60 秒周期清理空临时服务，预检会等待服务名消失后才判定通过。

## 9. Task 3 复核记录

- API、Minimal、Web 最终聚合运行证据：`target/example-e2e/20260710-211018-27839/`。API 5/5、
  Minimal 3/3、Web 6/6；汇总为 3 PASS、0 FAIL、0 SKIP、0 BLOCKED，命令退出码为 0。
- Web 首次失败的根因有两层：示例未关闭 Starter 默认启用的数据模块；独立运行时又解析到本地仓库中的旧
  `nebula-web` SNAPSHOT。前者通过示例配置修正，后者通过 Full 模式默认安装当前框架解决。
- Task 3 完成后，没有遗留受管 Java 进程，8080 端口已释放，且未创建外部中间件数据。

## 10. Task 4 复核记录

- 首次聚合运行 `target/example-e2e/20260710-211711-51774/` 如实失败：Service 因 Starter 默认启用
  persistence 但没有数据源而退出；All 启动了 Lock、Task、WebSocket，且性能端点返回 500。
- Service 与 All 最终聚合证据：`target/example-e2e/20260710-213422-14162/`，汇总 2 PASS、
  0 FAIL、0 SKIP、0 BLOCKED，命令退出码为 0。
- Service 组为 16 PASS、0 FAIL、0 SKIP、0 BLOCKED。三个 GET 接口、通用 `POST /rpc`、真实
  Redis Lock 回调、RPC 端口、锁键清理和 Redis 不可达快速失败均通过。
- All 组为 7 PASS、0 FAIL、0 SKIP、
  0 BLOCKED。零外部依赖模式下 Hello、健康、性能和 OpenAPI 通过，日志没有被禁用模块的连接或运行组件。
- `HttpRpcClientConfigTest` 为 7 tests、0 failures、0 errors、0 skipped，覆盖默认跟随
  `server.port` 和显式 `nebula.rpc.http.server.port` 优先两种端口规则。
- Service 运行时依赖树确认 `nebula-starter-service -> nebula-rpc-http -> httpclient5:5.6.1`；修复后不再出现
  `HttpClientConnectionManager` 缺类。
- 清理复核：8082、8084、18082 均已释放；Redis 中 `nebula_e2e_starter_service_*` 键数量为 0。

## 11. Task 5 复核记录

- 当前最终证据：`target/example-e2e/20260711-065908-67620/`，结果为 12 PASS、0 FAIL、0 SKIP、
  1 BLOCKED，退出码为 2。
- 无密钥禁用模式严格返回 `AI disabled`，`ChatService`、`EmbeddingService`、`VectorStoreService`
  均未创建，日志没有 OpenAI 或 Chroma 客户端初始化记录。
- AI 启用模式已证明三项服务 Bean 全部创建。运行时依赖树确认
  `nebula-ai-spring` 传递 Spring AI 2.0 的 OpenAI 和 Chroma Starter。
- OpenAI Java SDK 4.39.1 的生产基础地址需要 `/v1`。修正后错误由 404 变为 429 quota，证明请求已到达
  正确 API，但当前测试账号没有可用额度，真实聊天和 embedding 尚不能标记为 PASS。
- 示例将聊天和 embedding 的 `max-retries` 设为 0。额度错误后只执行 1 个测试动作，不再继续向量写入和查询。
- 清理复核：8083 和 18083 均已释放；Chroma 中 `nebula_e2e_ai_` 临时 collection 数量为 0；
  应用日志没有出现 API Key。

## 12. Task 6 复核记录

- 最终证据：`target/example-e2e/20260711-072653-44853/`，结果为 38 PASS、0 FAIL、0 SKIP、
  0 BLOCKED，退出码为 0。
- API 契约安装、Service 8081、Client 8082、健康检查以及两个 Nacos 实例注册全部通过。单条异步任务经历
  非终态并进入 `SUCCESS`，结果中的任务 ID、处理类型和成功标记来自真实服务端。
- 批量异步、同步 RPC、不存在 ID 的 404、`PENDING` 取消均通过。取消任务保持 `CANCELLED`，客户端日志明确
  记录跳过执行，服务端日志中没有对应任务 ID。
- Client 重启后仍能从 Nacos 读取此前的 `SUCCESS` 状态和结果。4 条 namespaced 执行记录均按精确
  dataId 删除并复核为 404，两个实例已注销，8081 和 8082 均释放。
- 运行中发现 `AsyncRpcExecutionManager` 会把已取消的排队任务重新改为 `RUNNING` 并发起 RPC。修复后执行线程
  会在开跑前读取状态并跳过明确的 `CANCELLED`；2 条单元测试同时覆盖取消和 Nacos 刚发布后短暂不可见的情况。
