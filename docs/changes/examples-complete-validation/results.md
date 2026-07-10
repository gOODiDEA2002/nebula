# 示例应用完全验证结果

> 状态：执行中
> 执行日期：2026-07-10
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
| 分支 | `main`，与 `vocoor/main` 同步 | 已固定 |
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
| Redis | 8.2.1，healthy | PENDING | 当前未启用密码 |
| RabbitMQ | 3.12，healthy | PENDING | 管理端口可达，待真实收发 |
| MinIO | healthy | PENDING | 待对象完整生命周期验证 |
| Elasticsearch | 7.17.19，healthy | PENDING | 与客户端 9.4.2 不匹配，不能作为有效验证环境 |
| MySQL | 8.3，healthy | PENDING | 待初始化示例库并执行读写 |
| XXL-JOB | 服务可访问，容器状态为 starting | PENDING | 镜像健康检查使用了不存在的 `curl` |
| Nacos | 2.5.1，healthy | PENDING | 待临时实例注册、查询和注销 |
| Chroma | v2 heartbeat 可访问 | PENDING | 待临时 collection 增删查 |
| Etcd | 3.5，healthy | 不适用 | 当前示例没有直接使用 |

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

E2E 总入口覆盖 13 组示例，运行证据统一保存到 `target/example-e2e/<run-id>/`。该目录是临时产物，
不提交到 Git；可复核结论持续写入本文档。

## 6. 示例结果

| 示例组 | 进程或产物 | 状态 | 证据 |
| --- | --- | --- | --- |
| `starter-minimal-example` | Minimal 应用 | PENDING | 待执行 |
| `starter-api-example` | 契约 JAR | PENDING | JAR 构建及 `UserApi.class` 检查已 PASS；提供方、消费方使用待验证 |
| `starter-web-example` | Web 应用 | FAIL | 默认启动因缺少 `primary` 数据源失败；临时关闭 persistence 后生命周期 PASS |
| `starter-service-example` | Service 应用 | PENDING | 待执行 |
| `starter-ai-example` | AI 应用 | PENDING | 待执行 |
| `starter-all-example` | All 应用 | PENDING | 待执行 |
| `rpc-async-example` | Service、Client | PENDING | 待执行 |
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
| 隔离容器和独立卷 | 不适用 | Task 0 尚未创建 |
| `nebula_e2e_` 临时数据 | 不适用 | Task 0 尚未写入 |

## 8. 已知告警

1. Fullstack 编译存在 Lombok `@Builder` 默认值告警及少量弃用、未检查操作告警，基线测试未失败。
2. Elasticsearch 7.17.19 不能用于验证框架的 9.4.2 客户端，后续使用隔离的 9.4.2 服务。
3. Vocoor OAuth 提供方 `localhost:8080` 当前不可达，完整授权码流程尚未执行。
4. Playwright 浏览器容器尚未启动，Browser Crawler 完整验证尚未执行。
5. `starter-web-example` 默认启动会加载 persistence，但示例没有配置主数据源；Task 3 需修复并重跑。
