# 示例应用完全验证

> 状态：apply
> 创建日期：2026-07-10
> 复杂度：复杂
> 基线提交：`6ddf69ff5594f3f3a62d418651a3f9a6fd641d54`

## 1. 背景与目标

Nebula 2.1 升级已经通过整仓编译和单元测试，但这只能证明代码能够构建，不能证明示例应用
能够完成启动、配置绑定、服务注册、真实中间件读写和跨进程调用。

本次验证覆盖 `examples/` 下 13 组示例。完成后需要形成以下可核验结果：

1. 每个可运行应用都在限定时间内启动，完成代表性业务请求，并由测试进程正常关闭。
2. `starter-api-example` 等契约模块完成打包，并由真实提供方和消费方共同验证。
3. MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、MinIO、Chroma 等外部服务执行真实读写，
   不能只探测端口。
4. 多进程示例完成跨服务调用，前端示例完成生产构建和浏览器主流程。
5. 发现的问题需要修复、补回归验证并更新文档；最终结果不允许存在静默跳过。

## 2. 代码现状

### 2.1 示例清单

`examples/pom.xml` 注册了快速入门和进阶示例。实际需要核对 13 组示例、16 个可运行进程：

| 示例组 | 进程或产物 | 默认端口 | 主要依赖 |
| --- | --- | --- | --- |
| `starter-minimal-example` | 非 Web 应用 | 无 | 无 |
| `starter-api-example` | 契约 JAR | 无 | 无 |
| `starter-web-example` | Web 应用 | 8080 | 无 |
| `starter-service-example` | Service 应用 | 8082 | Redis |
| `starter-ai-example` | AI Web 应用 | 8083 | AI 提供方、Chroma |
| `starter-all-example` | 单体应用 | 8084 | Redis |
| `rpc-async-example` | Service、Client | 8081、8082 | Nacos |
| `microservice-example` | User、Order | 1001、1002；gRPC 2001、2002 | Nacos |
| `gateway-example` | Gateway | 8000 | Nacos、Redis、微服务示例 |
| `fullstack-example` | 综合应用 | 1000；gRPC 2000 | MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、MinIO、Chroma、AI 提供方 |
| `crawler-example` | Crawler 应用、浏览器服务 | 8085、9222 | 外网、Playwright |
| `websocket-example` | Backend、Frontend | 8086、3000 | Node.js |
| `oauth-example` | Backend、Frontend、OAuth 提供方 | 8081、4010、8080 | MySQL、Vocoor OAuth、Node.js |

主要事实来源：

- 聚合模块：`examples/pom.xml`
- 应用端口与开关：各示例的 `src/main/resources/application.yml`
- 启动与接口说明：各示例的 `README.md`
- 现有验证脚本：`examples/e2e-common.sh` 与 `examples/*/e2e-test.sh`

### 2.2 已有验证能力

仓库已有 12 个 `e2e-test.sh`，覆盖除契约 JAR 外的大多数示例。现有脚本能够启动 Maven
应用并执行基础 HTTP 请求，但不能作为完全验证的最终依据：

- `examples/e2e-common.sh` 会对占用目标端口的进程执行 `kill -9`，可能误杀非测试进程。
- `skip_if_no_service` 在依赖缺失时以退出码 0 结束，汇总结果可能把未执行误判为成功。
- `gateway-example/e2e-test.sh` 接受 `404` 或 `503`，没有证明请求被正确代理到后端。
- `fullstack-example/e2e-test.sh` 主动禁用 RabbitMQ 和 AI，只按端口判断 Elasticsearch 可用。
- OAuth 和 WebSocket 前端只执行依赖安装，没有执行生产构建和浏览器流程。
- 多数断言只匹配字符串，没有核对状态码、JSON 字段类型和后续状态变化。

### 2.3 本机中间件现状

Docker Compose 项目 `nebula-data` 位于
`/Users/andy/DevOps/SourceCode/nebula-projects/nebula-data/docker-compose.yml`。

2026-07-10 的现场检查结果：

| 服务 | 状态 | 发现 |
| --- | --- | --- |
| Redis 8.2.1 | healthy | 容器未配置 `requirepass`，但 Fullstack 示例配置了 Redis 密码 |
| RabbitMQ 3.12 | healthy | 管理 API 可访问 |
| MinIO | healthy | API 端口为 9000，控制台映射为 9090 |
| Elasticsearch 7.17.19 | healthy | 与框架 Java Client 9.4.2 不匹配 |
| MySQL 8.3 | healthy | 已有 `nebula_example`，分片库和 OAuth 库尚未齐备 |
| XXL-JOB 2.4.1 | starting | 服务实际返回 302；健康检查调用了镜像中不存在的 `curl` |
| Nacos 2.5.1 | healthy | 认证接口可登录 |
| Chroma | 无容器健康检查 | `/api/v2/heartbeat` 可访问 |
| Etcd 3.5 | healthy | 当前示例没有直接使用 |

### 2.4 已发现的高风险矛盾

1. `starter-service-example/application.yml` 关闭 HTTP RPC，但 README 声称可访问 `/rpc/hello`。
2. Gateway 路由使用 `user-service`、`order-service`，实际注册名为
   `nebula-example-user-service`、`nebula-example-order-service`；网关路径为 `/api/**`，后端控制器为
   `/rpc/**`，没有配置路径重写。
3. Fullstack 示例使用 Elasticsearch 9.4.2 客户端，但当前容器为 7.17.19。
4. Fullstack 示例为 Redis 配置了密码，当前 Redis 容器没有启用密码。
5. Fullstack 默认启用 AI 和搜索，启动结果受密钥和中间件版本影响；已有脚本通过禁用功能绕过。
6. Fullstack 配置仍包含已经删除的 HTTP RPC 客户端字段，需要核对并清理。
7. OAuth 后端写死了远程数据库地址和客户端密钥。密钥不得继续进入日志或文档，需迁移到环境变量并轮换。
8. OAuth 提供方 `localhost:8080` 当前不可达；Crawler 浏览器容器当前未启动。
9. OAuth README 写前端端口 5173，Vite 实际配置为 4010；Examples 总览写 Gateway 端口 8090，实际为 8000。

## 3. 功能点

- [x] 提供可重复执行、不会误杀进程、不会静默跳过的 E2E 测试入口。
- [ ] 验证所有 Starter 示例的默认启动和代表性功能。
- [ ] 验证同步 RPC、异步 RPC、gRPC、Nacos 注册和跨服务调用。
- [ ] 验证 Gateway 真实代理、JWT 拒绝与放行、Redis 限流。
- [ ] 验证 Fullstack 的数据库、缓存、消息、搜索、存储、任务、支付、通知、AI 与 MCP。
- [ ] 验证 HTTP Crawler 和 Browser Crawler。
- [ ] 验证 WebSocket 后端、真实连接收发和前端生产构建。
- [ ] 验证 OAuth 后端、前端和真实授权码流程。
- [ ] 更新示例 README、配置和最终验证记录。

## 4. 验证规则

1. 「启动通过」必须同时满足：出现 Spring Boot `Started` 日志、预期端口可访问、代表性接口通过、
   日志没有 `APPLICATION FAILED` 或未预期异常、测试结束后进程退出。
2. 外部服务不能只使用 `nc` 探测端口，必须调用协议级健康接口或执行一次真实读写。
3. HTTP 验证必须同时检查状态码和响应内容。预期成功的接口不能接受 `404`、`500` 或 `503`。
4. 跨服务验证必须核对 Nacos 中的注册名、地址和 gRPC metadata，并从消费方发起真实调用。
5. 前端必须执行 `npm ci` 和 `npm run build`。需要交互的主流程使用浏览器自动化验证。
6. 测试数据使用 `nebula_e2e_` 前缀；结束后删除 Redis Key、RabbitMQ 队列、ES 索引、MinIO 对象和测试记录。
7. 不打印 API Key、JWT Secret、OAuth Secret 或数据库密码。日志只记录变量是否存在。
8. 不得强杀未知端口进程。端口被占用时先识别归属，只清理当前测试启动并记录的 PID。
9. 完全验证模式不允许 `SKIP`。外部条件不足时应记录为阻塞并保持 Goal 未完成。

## 5. 数据变更

不修改生产表结构。验证环境需要使用仓库内 SQL 初始化以下测试库：

| 操作 | 数据库 | 来源 | 清理要求 |
| --- | --- | --- | --- |
| 初始化 | `nebula_example` | `examples/fullstack-example/sql/data-demo-tables.sql` | 保留示例基线，清除 E2E 临时记录 |
| 初始化 | `nebula_sharding_0/1` | `examples/fullstack-example/sql/sharding-tables.sql` | 验证结束清除 E2E 临时记录 |
| 初始化 | `oauth_client_demo` | `examples/oauth-example/backend/sql/init.sql` | 验证结束清除测试用户和绑定记录 |

## 6. 接口变更

本轮以验证和示例修复为主，不预设框架公共 API 变更。若运行验证发现框架缺陷：

1. 先在本 Spec 记录问题和影响范围。
2. 添加最小回归测试后修复。
3. 公共 API 行为变化必须补充升级说明。

## 7. 影响范围

- `examples/e2e-common.sh`
- `examples/*/e2e-test.sh`
- `examples/e2e-all.sh`（计划新增）
- 各示例 `application.yml`、README、后端和前端代码
- 运行中发现缺陷时涉及的框架模块
- `docs/changes/examples-complete-validation/`

默认不修改独立仓库 `nebula-data`。Elasticsearch 9.4.2 等验证依赖优先使用隔离的临时容器；确需修改
`nebula-data/docker-compose.yml` 时必须先取得明确授权。

## 8. 风险与关注点

- Elasticsearch 7 到 9 不能直接复用数据目录进行无损升级，验证容器必须使用独立卷。
- OAuth 当前存在疑似有效的硬编码密钥，执行前应轮换并改为环境变量。
- AI 和 OAuth 会调用外部服务，需控制费用、请求次数并避免把响应中的敏感信息写入仓库。
- Crawler 依赖外网和私有浏览器镜像，需要区分产品缺陷与网络、镜像权限问题。
- 多应用并行运行容易产生端口冲突和残留进程，测试框架必须统一管理 PID。

## 9. 外部条件

- [x] 不修改独立的 `nebula-data` 仓库；验证所需的新版本中间件使用隔离容器、独立端口和独立卷。
- [ ] Vocoor OAuth 提供方是否可在 `localhost:8080` 启动，并提供重新生成的测试客户端凭据。
- [ ] `harbor.vocoor.com/ci/browser-playwright:latest` 是否具有拉取权限。

AI 相关环境变量当前已设置，但执行时仍需验证服务可达和账户可用，不能在文档中记录变量值。

## 10. 验收标准

- [ ] 13 组示例全部有明确结论，16 个可运行进程全部完成运行验证。
- [ ] 契约模块完成构建，并被提供方、消费方真实使用。
- [ ] 完全验证汇总为 0 失败、0 跳过、0 未验证。
- [ ] 所有中间件完成真实读写验证，Elasticsearch 服务端与客户端版本匹配。
- [ ] 两个前端完成生产构建，OAuth 和 WebSocket 主流程通过浏览器验证。
- [ ] `mvn clean test`、独立 Crawler 聚合构建、Shell 语法和文档链接检查全部通过。
- [ ] 所有测试进程和临时资源完成清理。
- [ ] `results.md` 记录命令、结果、证据和已知告警，且不包含秘密。
