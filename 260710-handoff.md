# Nebula 示例应用完全验证交接

> 生成时间：2026-07-10 21:08 +08
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
- 已提交的阶段节点：
  - `c8e63eff docs(validation): 建立示例应用完整验证基线`
  - `256aadce test(examples): 加固示例 E2E 验证框架`
  - `c2b3d6e5 test(examples): 增加中间件协议级预检`

## 关键上下文

- Full 模式总入口默认先运行 `mvn -q install -DskipTests`，避免独立示例解析到本地仓库中的旧 SNAPSHOT。
- Web OpenAPI 曾因旧 SNAPSHOT 中的 Springdoc 2.2.0 出现 `NoSuchMethodError`；安装当前框架后使用 3.0.3，问题消失。
- Web 示例不使用数据库和缓存，已在示例配置中显式关闭 Starter 默认启用的数据模块。
- Minimal 示例明确设置为非 Web 应用。API 契约 JAR 的运行时依赖不含 Web Server。
- API Starter 实际仍包含 MyBatis-Plus Boot4 Starter；活动文档对此说法不一致。本轮未改变公开依赖，留给 Task 15 统一文档。
- 外部 `nebula-data` 仓库、Compose 配置和数据卷必须保持只读；验证专用服务位于
  `docker/verification/docker-compose.yml`。

## 未完成

- Task 4 至 Task 16 全部待执行，当前没有可以标记为 Goal 完成的依据。
- 下一阶段优先处理 Task 4 的 Service/All Starter 和 Task 5 的 AI Starter。
- Gateway、Fullstack、Crawler Browser、WebSocket 浏览器流程和 OAuth 全链路仍有已知配置或环境风险，详见
  `docs/changes/examples-complete-validation/results.md` 与 `log.md`。

## 推荐执行路径

1. 读取 `docs/changes/examples-complete-validation/next-goal-prompt.md` 并核对工作区状态。
2. 审计 Service/All 的配置、控制器、README 和 E2E，统一 HTTP RPC 与 Redis Lock 的真实行为。
3. 完成 Task 4 局部 Full E2E，更新三份台账并做阶段提交。
4. 验证 AI 禁用模式，再用环境变量中的测试密钥和 Chroma 完成最小真实调用，清理临时 collection。
5. 完成 Task 5 后继续 Task 6 至 Task 16；不要用 Smoke 结果替代最终 Full 验收。

## 风险与约束

- Vocoor OAuth 提供方 `localhost:8080` 当前不可达，后续需要可用提供方及已轮换的测试凭据。
- Playwright 浏览器容器尚未启动，Crawler Browser 和两个前端浏览器流程仍待验证。
- XXL-JOB 镜像健康检查缺少 `curl`，但宿主机 HTTP 探针返回 302；启用该模块时应使用镜像支持的探针。
- 任何密钥、Token 和密码只能通过环境变量注入，不能写入 Git 或测试证据。

## 接手后的第一步

```bash
git status --short --branch
sed -n '1,260p' docs/changes/examples-complete-validation/tasks.md
sed -n '1,260p' docs/changes/examples-complete-validation/next-goal-prompt.md
```

确认工作区与交接提交一致后，从 Task 4 的代码和配置核对开始，不需要重复执行 Task 0 至 Task 3；只有后续改动影响这些范围时才回归。
