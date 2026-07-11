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
| `microservice-example` | User、Order | PASS | 34/34 PASS；REST CRUD、HTTP RPC、gRPC、Nacos metadata 和 Order 到 User 跨服务调用均通过 |
| `gateway-example` | Gateway | PASS | 23/23 PASS；真实代理、无 Token 401、有效 JWT、Redis 429 和令牌恢复均通过 |
| `fullstack-example` | Fullstack 应用 | BLOCKED | Task 11 当前 119 PASS、0 FAIL、0 SKIP、1 BLOCKED；RPC/gRPC/MCP 通过，OpenAI 账号 quota 阻塞 AI 与向量流程 |
| `crawler-example` | Crawler、Browser | PENDING | 待执行 |
| `websocket-example` | Backend、Frontend | PENDING | 待执行 |
| `oauth-example` | Backend、Frontend、OAuth 提供方 | PENDING | 待执行 |

## 7. 清理审计

| 项目 | 状态 | 证据 |
| --- | --- | --- |
| 本轮测试启动的进程 | PASS | 受控端口占用进程和示例进程均已关闭，1000 等已验证端口均释放 |
| 隔离容器和独立卷 | PASS | 容器 0、卷 0；13306 和 19200 端口均已释放 |
| `nebula_e2e_` 临时数据 | PASS | Redis、RabbitMQ、Nacos、MinIO、Chroma、Elasticsearch 和 Fullstack MySQL 复核均为 0 |

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

## 13. Task 7 复核记录

- 最终证据：`target/example-e2e/20260711-073534-69263/`，结果为 34 PASS、0 FAIL、0 SKIP、
  0 BLOCKED，退出码为 0。
- `user-api` 和 `order-api` 均安装成功，契约 JAR 分别包含 `UserRpcClient.class` 和
  `OrderRpcClient.class`。User、Order 的健康检查和 Nacos 注册通过，metadata 中的 gRPC 端口分别为
  2001 和 2002。
- User REST 完成创建、详情、筛选列表、更新、非法输入 400、删除和删除后复核。HTTP RPC 完成真实查询和
  未知服务 404；gRPC 完成真实查询和错误方法响应。
- Order 通过通用 HTTP RPC 创建订单，并由 `ServiceDiscoveryRpcClient` 使用 User 的 Nacos gRPC metadata
  调用 2001。Order gRPC 查询订单和未知用户错误均通过，Order 与 User 两端日志包含同一用户 ID。
- 清理复核：内存测试用户已删除；1001、1002、2001、2002 端口均释放；两个 Nacos 实例均注销。

## 14. Task 8 复核记录

- 最终证据：`target/example-e2e/20260711-074039-81648/`，结果为 23 PASS、0 FAIL、0 SKIP、
  0 BLOCKED，退出码为 0。
- Gateway 路由已改用 Nacos 真实服务名。User 和 Order Controller 同时提供 `/api/**` 前端入口，
  白名单用户列表经 `lb://nebula-example-user-service` 返回真实业务 JSON，后端日志确认收到请求。
- User 和 Order 受保护路径无 Token 均返回 401；测试现场签发的 HS256 JWT 可查询 User 并创建 Order，
  Order 随后通过 Nacos `grpcPort` metadata 调用 User。
- E2E 使用 Redis 15 号空测试库和 1/5 的临时令牌桶参数，连续请求出现 4 次 429，等待 2 秒后恢复 200。
  测试结束后限流键为 0，8000、1001、1002、2001、2002 端口全部释放。

## 15. Task 9 复核记录

- 最终证据：`target/example-e2e/20260711-121534-84726/`，结果为 49 PASS、0 FAIL、0 SKIP、
  0 BLOCKED，退出码为 0。
- 默认 Profile 使用隔离 MySQL 8.3 完成产品创建、查询、更新、分页筛选和逻辑删除；数据库确认
  `deleted=1` 后再删除临时行。Nebula 多级缓存完成 set/get/update/delete、L1 命中、L2 命名空间键、
  2 秒 TTL 到期和删除后 MISS；Spring Cache 完成创建、更新、读取和删除。
- `readwrite` Profile 的写操作选择主库、读操作选择从库；`sharding` Profile 的订单写入并读取
  `ds0.t_order_0`。`combined` Profile 同一进程内将产品写入 `master`、从 `slave01` 读取，并将订单
  路由到 `ds1.t_order_1`，应用日志和数据库均有直接证据。
- 首轮 47/49 暴露两个示例缺陷：不同 Spring Cache 操作向同一键写入不兼容 DTO 类型；产品删除使用
  `updateById` 时逻辑删除字段被 MyBatis-Plus 忽略。响应 DTO 统一继承读取模型，删除改用
  `deleteByIds` 后，两项均由第二轮 E2E 验证通过。
- 运行验证还发现多级缓存的最小 L1 TTL 会把调用方的 2 秒 TTL 延长到 1 分钟，以及组合配置声明的
  ShardingSphere 读写规则从未装配。修复后增加短 TTL 回归测试和独立读写库组合路由测试，相关模块
  16 个定向测试全部通过。
- 清理复核：Redis 14 号测试库为 0；隔离 MySQL 容器、网络和卷为 0；1000、13306 端口均释放。

## 16. Task 10 复核记录

- 最终证据：`target/example-e2e/20260711-124401-71092/`，结果为 93 PASS、0 FAIL、0 SKIP、
  0 BLOCKED，退出码为 0；Task 9 的 49 项数据验证在同一轮继续通过。
- RabbitMQ 使用临时 vhost 完成普通消息消费、首次失败后重投、真实 TTL/DLX 延迟消息和生产消费统计。
  Elasticsearch 9.4.2 使用隔离容器完成临时索引创建、单条和批量写入、刷新、查询、建议、文档删除和索引删除。
- MinIO 使用临时 Bucket 完成上传、递归列表、下载字节比对、预签名 URL、对象删除和 Bucket 删除。
  Task 完成执行器发现、成功执行与未知执行器失败；Mock Payment 和 Mock Notification 均覆盖成功与非法输入。
- Web 健康、性能、数据脱敏、响应缓存和限流均通过。缓存第二次请求正文保持一致并返回 `X-Cache: HIT`；
  同一 API 连续请求真实出现 429。
- 运行验证发现并修复三个框架问题：RabbitMQ 生产者统计返回占位值；MinIO 默认非递归且目录项可能没有修改时间；
  五个 MVC 功能配置器按同一 Bean 类型互相排斥。相关 Web、RabbitMQ、MinIO 和 Payment 模块完整测试均通过。
- 清理复核：RabbitMQ vhost、Elasticsearch 索引、MinIO Bucket 和对象、Redis 14 号库、隔离 MySQL/Elasticsearch
  容器与卷均为 0；1000、13306、19200 端口和 5 个受管进程均已释放。

## 17. Task 11 复核记录

- 当前证据：`target/example-e2e/20260711-130632-40522/`，结果为 119 PASS、0 FAIL、0 SKIP、
  1 BLOCKED，退出码为 2。Task 9 和 Task 10 的 93 项既有验证在同一轮继续通过。
- Fullstack 保持 HTTP RPC Server 关闭，通过 `user-api` 自动配置注册声明式客户端。真实 User Provider
  注册到 Nacos 后，Fullstack 根据 `grpcPort=2101` metadata 完成 User gRPC 查询；两端日志包含同一请求。
- Fullstack 显式选择 `nebula-rpc-grpc`，在 2100 端口注册 `FullstackEchoRpcService`。`grpcurl` 调用返回
  `fullstack:nebula_e2e_echo`，证明 gRPC Server 不是只监听端口。
- MCP 使用短期 HS256 JWT 完成 tools 列表、`get_weather` 工具调用、resources 列表和文档资源读取。
  配置已迁移到 `protocol` 和 `streamable-http.mcp-endpoint`，工具与资源按 AI/MCP 双开关注册。
- AI 配置统一改由 OpenAI 和 Chroma 环境变量注入，模型重试为 0。真实聊天首次请求仍返回 429 quota，
  脚本立即停止后续 embedding、文档、搜索和 RAG 请求，模型请求计数为 1，日志未出现 API Key。
- 清理复核：User 与 Fullstack Nacos 实例、Chroma 临时 collection、Redis 14 号库、隔离容器与卷均为 0；
  1000、1101、2100、2101、13306、19200 端口和 7 个受管进程均已释放。
