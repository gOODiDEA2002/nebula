# Nebula 示例应用完全验证交接

> 最后更新：2026-07-11 15:10 +08
> 当前分支：`main`
> 交接提交：本文档所在提交，可使用 `git log -1 --oneline` 读取

## 当前目标

完成 Nebula 2.1 升级后 `examples/` 下全部 13 组示例、16 个可运行进程和 API 契约 JAR 的真实运行验证。
最终 Full 模式要求 0 FAIL、0 SKIP、0 BLOCKED、0 未验证。

## 已完成

- Task 0：固定基线。整仓 913 个测试通过，0 失败、0 错误、3 个条件跳过；Crawler 聚合模块通过。
- Task 1：加固 E2E 通用框架，新增 13 组总入口、严格断言、受管进程清理、Full/Smoke 结果规则和证据目录。
- Task 2：完成 10 项中间件协议级预检，隔离 MySQL 8.3 与 Elasticsearch 9.4.2；临时容器、卷和数据均已清理。
- Task 3：API 契约 5/5、Minimal 3/3、Web 6/6 通过。Web 最终证据位于
  `target/example-e2e/20260710-211018-27839/`。
- Task 4：Service 16/16、All 7/7 通过；HTTP RPC、Redis Lock、失败路径和零依赖模式均有直接证据。
- Task 5 可执行部分：AI 禁用模式、启用时三项 Bean、依赖传递、`/v1` 地址、零重试和资源清理已验证。
  当前测试账号返回 OpenAI 429 quota，真实聊天、embedding 和 Chroma 读写仍为 `BLOCKED`。
- Task 6：异步 RPC 38/38 通过；单条、批量、同步、404、取消、Nacos 持久化和 Client 重启恢复
  均有真实证据，临时记录、实例和端口已清理。
- Task 7：微服务 34/34 通过；两个契约 JAR、User REST CRUD、HTTP RPC、gRPC、Nacos metadata、
  Order 到 User 跨服务调用和清理均有真实证据。
- Task 8：Gateway 23/23 通过；真实后端代理、无 Token 401、有效 JWT、Redis 429、令牌恢复和
  Redis/端口清理均有真实证据。
- Task 9：Fullstack 49/49 通过；默认、读写分离、分片和组合 Profile，产品 CRUD、逻辑删除、
  多级缓存与实际 SQL 路由均有真实证据。
- Task 10：Fullstack 93/93 通过；RabbitMQ、Elasticsearch、MinIO、Task、支付、通知和 Web 通用能力
  均有真实证据，5 个进程和全部临时资源已清理。
- Task 11 可执行部分：119 PASS、0 FAIL、0 SKIP、1 BLOCKED。RPC 发现调用、Fullstack gRPC Echo 和
  MCP 四类操作通过；OpenAI 429 quota 仍阻塞 AI、向量存储和 RAG 完整流程。
- Task 12：Crawler 17/17 通过；受控 HTTP 页面、Playwright WebSocket、JS 渲染 DOM、截图和
  全部临时资源清理均有真实证据。
- Task 13：WebSocket 19/19 通过；REST、双客户端、定向发送、心跳、前端构建、Playwright 和
  应用内浏览器均有真实证据。
- Task 14 可执行部分：11 PASS、0 FAIL、0 SKIP、1 BLOCKED；隔离客户端通过，真实 Vocoor 提供方不可达。
- Task 15：示例文档与配置统一完成；187 个活动链接 0 失效，15 份 Fullstack 旧手册已归档。
- 已提交的阶段节点：
  - `c8e63eff docs(validation): 建立示例应用完整验证基线`
  - `256aadce test(examples): 加固示例 E2E 验证框架`
  - `c2b3d6e5 test(examples): 增加中间件协议级预检`
  - `82e56465 perf(ai): 避免向量写入重复生成 embedding`
  - `0d4f712f fix(ai): 修复 Starter 依赖与 OpenAI 配置`
  - `54755646 test(examples): 加固 AI Starter 完整验证`
  - `9dd7cd36 fix(rpc): 阻止已取消异步任务继续执行`
  - `c8f7aa45 test(examples): 完成异步 RPC 运行验证`
  - `d4a0fbc7 fix(examples): 修复微服务运行配置与更新接口`
  - `db164b4e test(examples): 完成微服务双协议验证`
  - `01a46d76 fix(gateway): 对齐真实服务路由与后端 API`
  - `754f6ac1 test(examples): 完成 Gateway 真实流量验证`
  - `ed01fc4b fix(data): 修复多级缓存过期与组合路由`
  - `57216d95 test(examples): 完成 Fullstack 数据与缓存验证`
  - `9a2a8e0f fix(web): 允许 MVC 功能配置同时生效`
  - `6d7ad321 fix(messaging): 记录 RabbitMQ 生产者真实统计`
  - `797383a7 fix(storage): 修复 MinIO 递归对象列表`
  - `eea73a62 fix(payment): 支持大小写支付类型代码`
  - `96f0dfd2 test(examples): 完成 Fullstack 通用模块验证`
  - `1b9430e6 test(examples): 加固 Fullstack RPC AI MCP 验证`
  - `6889fd27 docs(validation): 记录 Fullstack 远程能力验证`
  - `ef32a807 fix(crawler): 验证远程浏览器与相对链接`
  - `d206c296 docs(validation): 记录 Crawler 完整验证`
  - `46f31827 fix(websocket): 完成双客户端与浏览器验证`
  - `a37c3ee4 docs(validation): 记录 WebSocket 完整验证`
  - `6f74e51c fix(oauth): 隔离配置并加固客户端验证`
  - `303e20a6 docs(validation): 记录 OAuth 隔离验证`
  - `1e4df82a docs(examples): 对齐 OAuth 与 Starter 说明`
  - `d24280c0 docs(examples): 统一运行说明并归档旧手册`

## 关键上下文

- Full 模式总入口默认先运行 `mvn -q install -DskipTests`，避免独立示例解析到本地仓库中的旧 SNAPSHOT。
- Web OpenAPI 曾因旧 SNAPSHOT 中的 Springdoc 2.2.0 出现 `NoSuchMethodError`；安装当前框架后使用 3.0.3，问题消失。
- Web 示例不使用数据库和缓存，已在示例配置中显式关闭 Starter 默认启用的数据模块。
- Minimal 示例明确设置为非 Web 应用。API 契约 JAR 的运行时依赖不含 Web Server。
- API Starter 实际包含 MyBatis-Plus Boot4 Starter，但运行时依赖不含 Web Server；活动说明已统一。
- HTTP RPC 的 HttpClient 5 依赖已归入 `nebula-rpc-http`，端口未显式配置时已改为跟随 `server.port`。
- 异步 RPC 执行线程现在会跳过明确标记为 `CANCELLED` 的排队任务。Nacos 发布后存在短暂读取窗口，
  `findById` 返回空时仍需继续正常执行，不能把暂时不可见当成取消或删除。
- Task 6 最终证据位于 `target/example-e2e/20260711-072653-44853/`，结果为 38 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。
- Task 7 最终证据位于 `target/example-e2e/20260711-073534-69263/`，结果为 34 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。Order 通过 Nacos 的 `grpcPort=2001` metadata 调用 User。
- Task 8 最终证据位于 `target/example-e2e/20260711-074039-81648/`，结果为 23 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。限流使用 Redis 15 号测试库，结束后键数量为 0。
- Task 9 最终证据位于 `target/example-e2e/20260711-121534-84726/`，结果为 49 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。Redis 14 号测试库、隔离 MySQL 容器与卷、1000 和 13306 端口均已清理。
- 多级缓存 L1 TTL 现在不会超过调用方传入的 TTL。ShardingSphere 组合模式会同时装配分片和读写规则；
  `sql-show` 可用于记录实际数据源和物理 SQL。
- Task 10 最终证据位于 `target/example-e2e/20260711-124401-71092/`，结果为 93 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。Web 核心、缓存、限流、认证和性能配置器不再因同类型 Bean 条件互相排斥。
- MinIO 对象列表现在递归返回前缀下的实际对象，并兼容没有修改时间的目录项。RabbitMQ 生产者统计已改为真实计数。
- Task 11 当前证据位于 `target/example-e2e/20260711-130632-40522/`。Fullstack 显式选择 optional gRPC
  实现，2100 端口 Echo 和发现客户端到 User 2101 的调用均通过；MCP 四类操作全部通过。
- Task 12 最终证据位于 `target/example-e2e/20260711-142401-29430/`，结果为 17 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。Playwright Server 和 Java 客户端均为 1.41.0，使用 WebSocket 而非 CDP。
- `CrawlerResponse.asDocument()` 现在以 `finalUrl` 或原始 `url` 作为 Jsoup base URI，相对链接可正确
  展开为绝对地址。Crawler 示例显式关闭未使用的 persistence 和 cache。
- Task 13 最终证据位于 `target/example-e2e/20260711-143510-53906/`，结果为 19 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。前端锁文件已提交，干净克隆可直接运行 `npm ci`。
- Spring WebSocket 会话现在优先使用认证 Principal，并允许应用通过 HandshakeInterceptor 提供身份属性。
  示例查询参数只用于展示按用户发送，不应作为生产认证方案。
- 外部 `nebula-data` 仓库、Compose 配置和数据卷必须保持只读；验证专用服务位于
  `docker/verification/docker-compose.yml`。

## 未完成

- Task 5 和 Task 11 尚缺有额度的 OpenAI 测试密钥，Task 14 尚缺隔离的 Vocoor 提供方，Task 16 待执行。
- 下一阶段执行 Task 16 全量回归；外部条件恢复后复跑 Task 5、Task 11 和 Task 14 Full E2E。
- OAuth 全流程仍有已知配置或环境风险，详见
  `docs/changes/examples-complete-validation/results.md` 与 `log.md`。

## 推荐执行路径

1. 读取 `docs/changes/examples-complete-validation/next-goal-prompt.md` 并核对工作区状态。
2. 执行 Task 16 的 Maven、Crawler、前端、Shell、链接和清理审计。
3. 运行 Full E2E，保留三个外部阻塞的真实结果，不得把 BLOCKED 改写为 PASS。
4. 有额度的 OpenAI 测试密钥可用后复跑 Task 5，完成聊天、embedding 和 Chroma 写入查询删除。
5. 继续 Task 11 至 Task 16；不要用 Smoke 或 BLOCKED 结果替代最终 Full 验收。

## 风险与约束

- Vocoor OAuth 提供方 `localhost:8080` 当前不可达，后续需要可用提供方及已轮换的测试凭据。
- 当前 `OPENAI_API_KEY` 对正确的 `/v1` 地址返回 429 quota，需要有额度的测试账号才能完成 Task 5。
- Crawler 和 WebSocket 浏览器流程已通过；OAuth 前端浏览器流程仍待验证。
- XXL-JOB 镜像健康检查缺少 `curl`，但宿主机 HTTP 探针返回 302；启用该模块时应使用镜像支持的探针。
- 任何密钥、Token 和密码只能通过环境变量注入，不能写入 Git 或测试证据。

## 接手后的第一步

```bash
git status --short --branch
sed -n '1,260p' docs/changes/examples-complete-validation/tasks.md
sed -n '1,260p' docs/changes/examples-complete-validation/next-goal-prompt.md
```

确认工作区与最新阶段提交一致后，从 Task 16 开始；Task 5、Task 11 和 Task 14 在外部条件恢复后复跑。
