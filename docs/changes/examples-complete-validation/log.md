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
| 2026-07-11 | research | 异步 RPC 取消后仍执行远程调用 | `cancel` 只写入 `CANCELLED`，执行线程未在开跑前复核状态 |
| 2026-07-11 | apply | 修正异步取消并加固完整验证 | 执行线程跳过已取消任务；E2E 覆盖 Nacos、单条、批量、同步、404、取消和重启恢复 |
| 2026-07-11 | verify | 完成 Task 6 异步 RPC 验证 | 38/38 PASS；Nacos 记录和实例、8081/8082 端口全部清理 |
| 2026-07-11 | research | 微服务严格基线被默认持久化阻断 | User Service 没有数据源，但 Service Starter 默认启用 persistence |
| 2026-07-11 | apply | 修正微服务配置、更新接口和敏感日志 | 纯内存服务关闭未使用模块；使用当前 gRPC 端口键；请求体不再重复要求路径 ID；日志不打印密码和 Token |
| 2026-07-11 | verify | 完成 Task 7 微服务双协议验证 | 34/34 PASS；REST、HTTP RPC、gRPC、发现调用、四端口和两个实例清理通过 |
| 2026-07-11 | research | Gateway 路由无法命中真实后端 | 配置服务名与 Nacos 不一致，且 `/api/**` 在后端没有对应 Controller |
| 2026-07-11 | apply | 对齐 Gateway 服务名和应用层 API | User、Order 增加 `/api/**` 入口；JWT、Redis 和限流参数支持环境变量 |
| 2026-07-11 | verify | 完成 Task 8 Gateway 验证 | 23/23 PASS；代理、401、有效 JWT、429、令牌恢复和资源清理通过 |
| 2026-07-11 | research | Fullstack 首轮运行暴露缓存、删除和组合数据源问题 | 短 TTL 被 L1 延长；缓存 DTO 类型冲突；逻辑删除未写入；组合读写规则未装配 |
| 2026-07-11 | apply | 修复多级缓存 TTL、组合路由和 Fullstack 示例行为 | 补充框架回归测试，统一缓存响应类型，并使用 MyBatis-Plus 逻辑删除 API |
| 2026-07-11 | verify | 完成 Task 9 Fullstack 数据模式验证 | 49/49 PASS；四种 Profile、MySQL、Redis 和资源清理全部通过 |
| 2026-07-11 | research | Fullstack 通用模块首轮严格验证暴露运行缺陷 | RabbitMQ 统计为固定值；MinIO 非递归列表为空；多个 Web MVC 配置器因同类型条件互相排斥 |
| 2026-07-11 | apply | 修复消息统计、对象列表和 Web 功能共存 | 增加模块回归测试；示例补齐真实消息、搜索、存储、校验、缓存和限流验证 |
| 2026-07-11 | verify | 完成 Task 10 Fullstack 通用模块验证 | 93/93 PASS；5 个进程和所有临时中间件资源均已清理 |
| 2026-07-11 | research | Fullstack RPC、AI 和 MCP 配置包含过期字段 | gRPC 使用旧顶层键；MCP 使用旧传输字段；AI 混用 DeepSeek 聊天地址和 OpenAI embedding 模型 |
| 2026-07-11 | apply | 迁移 Fullstack 远程能力配置并补真实验证 | 显式选择 optional gRPC 实现；增加 Echo 服务、MCP 双开关和向量文档删除入口 |
| 2026-07-11 | verify | Task 11 可执行部分完成 | 119 PASS、0 FAIL、0 SKIP、1 BLOCKED；唯一阻塞为 OpenAI 429 quota |
| 2026-07-11 | research | Crawler 旧脚本没有验证 Browser，运维文档误把 Playwright Server 当成 CDP | 旧 E2E 只请求公共网站；`/json/version` 在当前容器中不存在；本机 9222 已被 Chrome 占用 |
| 2026-07-11 | apply | 加固双引擎验证并修复相对链接解析 | 增加受控页面、可配置容器端口和 17 项严格断言；Jsoup 使用最终响应 URL 作为 base URI |
| 2026-07-11 | verify | 完成 Task 12 Crawler 验证 | 17/17 PASS；Playwright 1.41.0、WebSocket、JS DOM、截图和全部资源清理通过 |
| 2026-07-11 | research | WebSocket 查询参数身份从未进入会话注册表 | Spring 会话包装器没有读取 Principal 或握手属性，旧脚本还会把缺少 Python 包记为跳过 |
| 2026-07-11 | apply | 接入握手拦截器并增加可复现的双客户端和 UI 测试 | Principal 优先于演示属性；前端声明 `ws` 与 Playwright，提交锁文件并使用 `npm ci` |
| 2026-07-11 | verify | 完成 Task 13 WebSocket 验证 | 19/19 PASS；协议、REST、构建、无头 Chrome、应用内浏览器和端口清理全部通过 |
| 2026-07-11 | research | OAuth 示例硬编码业务数据库与秘密，前端干净构建失败 | 旧 E2E 指向 192.168 地址；vue-tsc 1.8 与当前 TypeScript 不兼容；Vite 缺少后端代理 |
| 2026-07-11 | apply | 环境变量化 OAuth 配置并改用隔离 MySQL | 移除默认秘密和敏感日志，修正错误回调、退出路径、前端类型检查与 4010 代理 |
| 2026-07-11 | verify | Task 14 可执行部分完成 | 11 PASS、0 FAIL、0 SKIP、1 BLOCKED；唯一阻塞为不可安全隔离启动的 Vocoor 提供方 |

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
| 异步取消 | 执行线程开跑前复核持久化状态，仅跳过明确的 `CANCELLED` | 从线程池移除任务或把暂时查不到记录视为取消 | 当前执行器不暴露队列句柄；Nacos 发布后存在短暂读取窗口，空结果不能证明任务已删除 |
| 取消 E2E | 测试时用环境变量临时设置单线程执行器 | 把示例默认线程池永久改为单线程 | 单线程可稳定制造排队，正常示例仍保留 10/50/200 的吞吐配置 |
| 微服务 gRPC 端口 | 使用 `spring.grpc.server.port`，Nacos metadata 读取同一值 | 继续依赖旧 `grpc.server.port` 和桥接键 | 当前 Spring gRPC 服务器以 `spring.grpc.server.*` 为权威配置 |
| Order HTTP 入口 | `/api/orders`、`/rpc/orders` 由应用 Controller 提供，通用 RPC 仍使用 `POST /rpc` | 把 `@RpcCall` 当成自动 MVC 路由 | 前端和直连 HTTP 路径属于应用层，RPC 协议入口由框架提供 |
| Gateway 前端路径 | 应用 Controller 显式提供 `/api/**`，Gateway 保持原路径代理 | 在 Gateway 猜测并重写 `/api` 到 `/rpc` | 前端 HTTP 路径属于应用层，后端路由清晰且可直接测试 |
| Gateway 限流隔离 | E2E 使用 Redis 15 号空测试库，结束后删除本轮限流键 | 在默认数据库扫描并删除共享键 | 避免碰触其他开发进程的限流状态 |
| Fullstack MySQL 隔离 | 使用验证 Compose 的独立项目名、端口和卷 | 修改或复用 `nebula-data` 现有卷 | 保证升级验证不会碰触开发数据 |
| 组合数据模式 | 在同一个 ShardingSphere DataSource 中同时装配分片和读写规则 | 让读写切面在 ShardingSphere DataSource 外层切换 | 单一 MyBatis `SqlSessionFactory` 需要由 ShardingSphere 统一选择逻辑数据源 |
| Spring Cache 示例 | 显式使用 `simple` 类型并创建 `users` 缓存 | 依赖 Redisson JCache 的隐式选择 | 示例缓存注解与 Nebula 多级缓存分别验证，避免未声明缓存和共享 Redis 数据 |
| Web MVC 功能配置器 | 每个已启用功能独立注册 `WebMvcConfigurer` | 对同一接口类型使用 `@ConditionalOnMissingBean` | 多个配置器本来就应共同参与 MVC 配置；功能开关负责控制是否启用 |
| MinIO 对象列表 | 按前缀递归列出对象，目录占位项不读取缺失的修改时间 | 只返回一层目录并由示例过滤 | `StorageService.listObjects` 的调用方需要获得实际对象，且目录项可能没有修改时间 |
| Fullstack HTTP RPC Server | 保持关闭，Controller 提供 HTTP API，发现客户端负责调用下游 | 同时打开本地 `/rpc` Server | Fullstack 当前是 User RPC 消费方；本地提供方能力由独立 gRPC Echo 服务验证 |
| Fullstack gRPC 依赖 | 示例显式依赖 `nebula-rpc-grpc` | 期待 `nebula-starter-all` 传递 optional 实现 | Starter 只提供可选能力，示例必须明确选择实际协议实现 |
| Crawler Browser 协议 | 使用 Playwright WebSocket Server，`use-cdp=false` | 把服务当作 Chrome CDP 并请求 `/json/version` | 当前镜像运行 `playwright run-server`，根入口返回 `Running`，监听地址由容器日志给出 |
| WebSocket 用户身份 | 框架优先使用 Principal，其次读取握手拦截器写入的 `userId` 属性 | 框架直接信任任意查询参数 | 生产认证仍由应用负责，示例可用查询参数演示定向发送 |

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
| 异步任务取消后仍被执行 | 执行线程无条件把状态更新为 `RUNNING` | 开跑前读取状态，明确为 `CANCELLED` 时直接返回，并验证服务端没有收到请求 | [x] |
| Nacos 刚发布的配置短暂读不到 | 发布成功与本客户端读取可见之间存在数毫秒窗口 | 只有明确读到 `CANCELLED` 才跳过；空结果沿用正常执行路径，并补回归测试 | [x] |
| User 更新接口按 README 调用仍返回 400 | DTO 的 `id` 在方法执行前校验非空，但 Controller 和 RPC 都从独立参数设置 ID | 移除请求体 ID 的非空约束，以路径或 RPC 参数为唯一来源 | [x] |
| HTTP RPC 请求缺少 primitive 字段时返回 500 | Jackson 3 将缺失的 `long timeout` 作为 `null` 并拒绝映射 | E2E 按完整线格式发送 `timestamp`、`timeout` 和其他字段 | [x] |
| Gateway 返回 404/503 仍被旧脚本判通过 | 脚本只检查网关可达，服务名和后端路径均不正确 | 启动真实后端，只接受业务 200，并交叉检查后端日志 | [x] |
| 2 秒缓存 TTL 在 3 秒后仍命中 | L1 最小 TTL 为 1 分钟，计算结果超过调用方传入的总 TTL | L1 TTL 上限改为调用方 TTL，并补短 TTL 回归测试 | [x] |
| Spring Cache 更新后读取返回 500 | 创建、更新和读取向同一缓存键写入三种互不兼容的响应类型 | 创建和更新响应继承统一读取响应模型 | [x] |
| 产品接口返回删除成功但数据库仍为未删除 | `updateById` 不会用实体更新 MyBatis-Plus 逻辑删除字段 | 使用 `deleteByIds` 执行框架支持的逻辑删除 SQL | [x] |
| 组合模式产品写入被送到分片库 | 配置模型有读写规则，但 `ShardingSphereManager` 未转换为运行规则 | 同时装配 Sharding 和 Readwrite Splitting 规则，并核对实际 SQL 路由 | [x] |
| RabbitMQ 生产者统计始终为 0 | `getStats()` 返回固定占位数据 | 对同步与延迟发送记录总数、成功数、失败数和耗时，并补重置测试 | [x] |
| MinIO 上传成功但按前缀列表为空 | SDK 默认非递归，只返回目录占位项；目录项修改时间可能为空 | 启用递归列表，目录项不读取修改时间，并补回归测试 | [x] |
| 响应缓存与限流不能同时工作 | 五个 Web 功能配置器都按 `WebMvcConfigurer` 类型使用 `@ConditionalOnMissingBean` | 移除同类型互斥条件，并用上下文测试确认核心、缓存、限流配置器同时存在 | [x] |
| Nacos 有 gRPC metadata 但端口未监听 | Fullstack 未显式引入 Starter All 的 optional gRPC 实现 | 示例直接依赖 `nebula-rpc-grpc`，并验证 2100 端口 Echo 成功 | [x] |
| MCP 工具和资源可能未注册 | 应用配置在自动配置 Bean 创建前使用 `@ConditionalOnBean` 判断 | 改为 AI 与 MCP Server 双属性条件，真实验证列表、调用和读取 | [x] |
| Crawler 相对链接解析结果为空 | `CrawlerResponse.asDocument()` 没有向 Jsoup 传入页面 URL | 优先使用 `finalUrl`、回退 `url` 作为 base URI，并补重定向回归测试 | [x] |
| Playwright 正常运行却被启动脚本判超时 | 脚本访问 CDP 专用 `/json/version`，实际服务根入口只返回 `Running` | 启停脚本改用 Docker Compose v2 和真实存活入口，并输出运行时版本 | [x] |
| WebSocket 按用户发送始终为 0 | 会话注册时 `userId` 尚未从 Principal 或握手属性赋值 | 会话包装器初始化身份，自动配置接入应用提供的 HandshakeInterceptor | [x] |
| WebSocket 干净克隆无法执行 `npm ci` | 前端锁文件被局部 `.gitignore` 排除 | 提交当前 `package-lock.json`，协议与 UI 测试依赖均固定版本 | [x] |

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
| Crawler Browser 说明 | README 声称容器提供 CDP `/json/version` | 镜像实际提供 Playwright WebSocket Server，且 Java 客户端版本为 1.41.0 | 重写活动说明，统一协议、版本、端口覆盖和启停命令 |
