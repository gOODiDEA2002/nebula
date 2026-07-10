# 变更日志 -- 示例应用完全验证

> 只记录可复用的决策、问题和事实，不记录流水账。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-07-10 | propose | 建立 13 组示例的运行验证计划 | 整仓已有 913 个测试通过，但尚未逐示例运行 |
| 2026-07-10 | research | 核对 `nebula-data` 9 个容器 | 发现 ES 版本不匹配和 XXL-JOB 健康检查失效 |
| 2026-07-10 | research | 审计 12 个现有 E2E 脚本 | 发现静默跳过、宽松断言、误杀端口进程等问题 |
| 2026-07-10 | apply | 固定提交并完成整仓测试基线 | 70 个 Reactor 模块成功；913 个测试、0 失败、0 错误、3 个条件跳过 |
| 2026-07-10 | apply | 单独验证 Crawler 聚合模块 | 6 个模块成功；3 个 Stealth4j 测试因外部条件跳过，留待完整 E2E 覆盖 |
| 2026-07-10 | apply | 加固 E2E 通用框架并新增 13 组总入口 | 失败、阻塞、跳过、未知筛选和端口占用均已执行自测 |
| 2026-07-10 | apply | 首次真实启动 `starter-web-example` 失败 | Web Starter 默认开启持久化，但示例没有主数据源配置 |
| 2026-07-10 | apply | 完成中间件协议级预检 | 10 项全部通过；隔离 MySQL 8.3、Elasticsearch 9.4.2 和独立卷已删除 |
| 2026-07-10 | verify | 总入口串联中间件和契约示例 | 预检 10/10、示例 1/1；临时容器、卷和协议资源均为 0 |
| 2026-07-10 | apply | 修正 Minimal、Web 和 API 契约示例验证 | Minimal 明确使用非 Web 模式；Web 明确关闭未使用的数据模块；API 增加运行时依赖检查 |
| 2026-07-10 | verify | 完成 Task 3 三组基础 Starter 验证 | API 5/5、Minimal 3/3、Web 6/6；无外部依赖、残留进程或占用端口 |
| 2026-07-10 | research | 首次运行 Service 和 All 均失败 | Service 被无配置数据源阻断；All 实际启动 WebSocket、Task、Redis Lock，且性能端点未注册 |
| 2026-07-10 | apply | 修正 HTTP RPC 运行时依赖与端口解析 | `nebula-rpc-http` 补充 HttpClient 5；未显式配置 RPC 端口时跟随 `server.port` |
| 2026-07-10 | verify | 完成 Task 4 Starter 验证 | Service 16/16、All 7/7；Redis 失败路径、端口和临时锁键清理通过 |
| 2026-07-11 | research | AI Starter 启用后没有创建任何 AI Bean | `nebula-ai-spring` 将 OpenAI 和 Chroma 两个必需依赖错误标记为 optional |
| 2026-07-11 | apply | 修正 AI 依赖、OpenAI 基础地址和请求重试 | Starter 已传递 Spring AI 2.0 实现；默认地址补 `/v1`；示例禁用自动重试 |
| 2026-07-11 | verify | Task 5 可执行部分通过，真实调用阻塞 | 禁用模式和三项 AI Bean 通过；测试账号返回 429 quota，脚本在首次请求后停止并清理 |

## 技术决策

| 决策 | 选了什么 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| 验证层次 | 构建、启动、行为、集成、清理五层证据 | 只运行 `mvn test` | 编译和单元测试不能证明配置及外部服务可用 |
| E2E 结果 | Full 模式 0 跳过 | 依赖缺失时退出 0 | 静默跳过会制造完全通过的假象 |
| ES 验证 | 隔离的 9.4.2 容器和独立卷 | 复用当前 7.17 数据卷升级 | 跨大版本直接复用数据有损坏风险 |
| 进程管理 | 只关闭测试创建的 PID | 按端口 `kill -9` | 避免误杀开发者正在运行的服务 |
| 外部仓库 | 默认只读使用 `nebula-data` | 自动修改 Compose 和数据 | 独立仓库修改需要明确授权 |
| 敏感信息 | 环境变量注入并脱敏记录 | 沿用 YAML 硬编码密钥 | 防止秘密进入 Git 和日志 |
| 单元测试跳过 | 基线如实记录，最终 E2E 不允许跳过 | 把条件跳过视为完全通过 | 单元测试基线与示例完全验收是两个不同门禁 |
| 独立示例构建 | Full 模式默认先安装当前框架快照 | 直接使用本地 Maven 仓库中的旧 SNAPSHOT | 避免源码已升级而独立示例仍加载旧框架产物 |
| Service HTTP 入口 | Controller 提供三个 GET 接口，通用 `POST /rpc` 验证真实 RPC Server | 把 `@RpcCall` 当作动态 MVC 路由 | 当前 HTTP RPC Server 只提供通用协议入口，前端 HTTP 路由属于应用层 |
| All 默认模式 | 显式关闭所有需要外部服务或独立运行组件的模块 | 依赖 Starter 默认值或只关闭部分模块 | README 承诺零外部依赖，配置必须完整覆盖 Starter 的启用默认值 |
| AI 实现依赖 | `nebula-ai-spring` 传递 OpenAI 和 Chroma 两个必需实现 | 在每个示例中重复声明依赖 | AI 自动配置直接引用两者，缺少任一依赖都会使 Starter 静默失效 |
| AI 外部请求 | 示例通过 `max-retries=0` 禁用 SDK 自动重试 | 使用 SDK 默认重试次数 | 完整验证应限制费用，外部额度不足时首次失败后立即停止 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 是否形成规则 |
| --- | --- | --- | --- |
| XXL-JOB 长期显示 starting | 镜像没有健康检查使用的 `curl` | 使用镜像支持的命令或宿主机协议探针 | [ ] |
| 搜索端口可达但版本错误 | 现有脚本只执行 TCP 检查 | 读取服务端版本并执行真实索引操作 | [ ] |
| Gateway 返回 404/503 也被判通过 | 脚本只证明网关进程可达 | 必须返回后端业务 200，并核对响应内容 | [ ] |
| Fullstack 看似通过但关闭多个模块 | 脚本主动禁用 RabbitMQ 和 AI | Full 模式禁止通过关闭目标功能绕过 | [ ] |
| Web Starter 示例无法默认启动 | Starter 默认值开启 persistence，示例没有 `primary` 数据源 | Task 3 核对 Starter 定位后修正示例配置并补运行验证 | [x] |
| Chroma 删除返回假阳性 | 1.0.0 的 DELETE 实际使用 collection 名称，OpenAPI 参数说明写成 UUID | 按名称删除，并再次列出 collection 确认 0 条 | [x] |
| Nacos 注销后服务名短暂保留 | 空临时服务默认每 60 秒清理一次 | 先确认实例数为 0，再轮询到服务名消失 | [x] |
| Web OpenAPI 出现 `NoSuchMethodError` | 独立示例使用了本地仓库中带 Springdoc 2.2.0 的旧 SNAPSHOT，而当前源码已升级到 3.0.3 | Full 模式运行示例前执行当前框架 `mvn install -DskipTests`，并允许后续阶段显式复用已安装产物 | [x] |
| Service 启用 HTTP RPC 后缺类 | `HttpRpcAutoConfiguration` 使用 HttpClient 5，但依赖只存在于 optional 自动配置依赖中 | 在 `nebula-rpc-http` 声明必需依赖，由实现模块向使用方传递 | [x] |
| HTTP RPC Server 端口固定为 8080 | `ServerConfig.port` 注释说默认跟随 `server.port`，字段却写死为 8080 | 保留 `int` API 并以 0 表示未配置；默认跟随应用端口，显式值优先，并补两条回归测试 | [x] |
| AI Starter 启用后仍返回 disabled | OpenAI 和 Chroma Starter 被 `nebula-ai-spring` 标记为 optional，没有传递到应用 | 两个核心实现改为必需依赖，MCP 和 Ollama 等扩展仍保持可选 | [x] |
| OpenAI 请求统一返回 404 | OpenAI Java SDK 4.39.1 需要包含 `/v1` 的基础地址 | 默认值和活动示例改为 `https://api.openai.com/v1`，并补默认值测试 | [x] |
| OpenAI 测试账号返回 429 | 当前环境变量中的测试账号没有可用额度 | 记录 `BLOCKED`，零重试并在首次失败后停止；不把阻塞折算为通过 | [ ] |

## 知识发现

- [ ] **运行验证门禁**：端口可达只证明进程监听，不能证明协议和业务正确。
- [ ] **跨版本中间件**：客户端与服务端版本必须作为 E2E 前置证据记录。
- [ ] **示例即产品**：README、默认配置和 E2E 脚本需要共同描述同一个行为。
- [ ] **无跳过原则**：发布前的完全验证不能把环境阻塞折算为成功。

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| Web 示例的数据模块 | Web 示例定位为无外部依赖的基础 Web 验证 | Web Starter 默认启用 persistence 和 cache，示例没有数据源 | 在示例配置中显式关闭未使用模块，并在 README 说明 |
| API Starter 依赖说明 | 当前文档对 MyBatis-Plus 是否属于契约依赖存在冲突 | 实际运行时依赖不包含 Web Server，但包含 MyBatis-Plus Boot4 Starter | 本轮不改变公开依赖；Task 15 统一 `AGENTS.md`、`CLAUDE.md` 和 Starter 指南 |
| Service HTTP 路由 | 任务要求三个 GET 接口和 HTTP RPC 真实调用 | `@RpcCall` 不会动态注册 MVC GET 路由，框架只暴露通用 `POST /rpc` | 增加应用层 Controller 复用 RPC 实现，并同时验证通用 RPC 协议入口 |
| All 零依赖说明 | README 声称默认无外部依赖 | 配置仍启用 Lock，且未覆盖 Task、AI、WebSocket 等 Starter 默认值 | 显式关闭全部相关模块，保留 Web 健康、性能和 OpenAPI |
