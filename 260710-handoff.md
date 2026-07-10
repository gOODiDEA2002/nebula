# Nebula 示例应用完全验证交接

> 最后更新：2026-07-11 07:38 +08
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

## 关键上下文

- Full 模式总入口默认先运行 `mvn -q install -DskipTests`，避免独立示例解析到本地仓库中的旧 SNAPSHOT。
- Web OpenAPI 曾因旧 SNAPSHOT 中的 Springdoc 2.2.0 出现 `NoSuchMethodError`；安装当前框架后使用 3.0.3，问题消失。
- Web 示例不使用数据库和缓存，已在示例配置中显式关闭 Starter 默认启用的数据模块。
- Minimal 示例明确设置为非 Web 应用。API 契约 JAR 的运行时依赖不含 Web Server。
- API Starter 实际仍包含 MyBatis-Plus Boot4 Starter；活动文档对此说法不一致。本轮未改变公开依赖，留给 Task 15 统一文档。
- HTTP RPC 的 HttpClient 5 依赖已归入 `nebula-rpc-http`，端口未显式配置时已改为跟随 `server.port`。
- 异步 RPC 执行线程现在会跳过明确标记为 `CANCELLED` 的排队任务。Nacos 发布后存在短暂读取窗口，
  `findById` 返回空时仍需继续正常执行，不能把暂时不可见当成取消或删除。
- Task 6 最终证据位于 `target/example-e2e/20260711-072653-44853/`，结果为 38 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。
- Task 7 最终证据位于 `target/example-e2e/20260711-073534-69263/`，结果为 34 PASS、0 FAIL、
  0 SKIP、0 BLOCKED。Order 通过 Nacos 的 `grpcPort=2001` metadata 调用 User。
- 外部 `nebula-data` 仓库、Compose 配置和数据卷必须保持只读；验证专用服务位于
  `docker/verification/docker-compose.yml`。

## 未完成

- Task 5 尚缺有额度的 OpenAI 测试密钥，Task 8 至 Task 16 待执行，当前没有可以标记为 Goal 完成的依据。
- 下一阶段先执行 Task 8 Gateway 真实代理、认证和限流；获得有额度的测试密钥后立即复跑 Task 5 Full E2E。
- Gateway、Fullstack、Crawler Browser、WebSocket 浏览器流程和 OAuth 全链路仍有已知配置或环境风险，详见
  `docs/changes/examples-complete-validation/results.md` 与 `log.md`。

## 推荐执行路径

1. 读取 `docs/changes/examples-complete-validation/next-goal-prompt.md` 并核对工作区状态。
2. 从 Task 8 的 Gateway 路由、认证过滤器、限流配置、服务名和现有 E2E 脚本核对开始。
3. 启动受控真实后端，分别验证代理 200、无 Token 401、有效 Token 200、限流 429 和令牌恢复。
4. 有额度的 OpenAI 测试密钥可用后复跑 Task 5，完成聊天、embedding 和 Chroma 写入查询删除。
5. 继续 Task 7 至 Task 16；不要用 Smoke 或 BLOCKED 结果替代最终 Full 验收。

## 风险与约束

- Vocoor OAuth 提供方 `localhost:8080` 当前不可达，后续需要可用提供方及已轮换的测试凭据。
- 当前 `OPENAI_API_KEY` 对正确的 `/v1` 地址返回 429 quota，需要有额度的测试账号才能完成 Task 5。
- Playwright 浏览器容器尚未启动，Crawler Browser 和两个前端浏览器流程仍待验证。
- XXL-JOB 镜像健康检查缺少 `curl`，但宿主机 HTTP 探针返回 302；启用该模块时应使用镜像支持的探针。
- 任何密钥、Token 和密码只能通过环境变量注入，不能写入 Git 或测试证据。

## 接手后的第一步

```bash
git status --short --branch
sed -n '1,260p' docs/changes/examples-complete-validation/tasks.md
sed -n '1,260p' docs/changes/examples-complete-validation/next-goal-prompt.md
```

确认工作区与最新阶段提交一致后，从 Task 8 开始，不需要重复执行 Task 0 至 Task 7；Task 5 在外部额度恢复后复跑。
