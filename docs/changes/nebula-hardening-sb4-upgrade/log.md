# 变更日志：Nebula 硬化 + 升级 SB4.1

> 随开发实时追加。只记有复用价值的技术决策、踩坑、知识发现、Spec-Code 偏差，不记流水账。

---

## 技术决策

- **2026-07-06｜先修后升，全模块在范围**：用户确认新项目依赖全部模块，故按严重级别全局排序修复；先在 3.5.16 上做工作流 A（阻断级）再升 4.1，避免带未修的机制性失效跨版本。
- **2026-07-06｜ImportFilter 直接删除（Spec Q1）**：`NebulaAutoConfigurationImportFilter` 当前经 `.imports` 注册本就不生效（字节码已证），且其无条件排除 DataSource/MyBatis-Plus 行为过激，删除零风险。
- **2026-07-06｜密码哈希新增而非改旧（Spec Q6）**：`CryptoUtils.encrypt` 保留并 `@Deprecated`，新增 `hashPassword`(PBKDF2WithHmacSHA256, JDK 原生免加依赖)，避免破坏已落库的旧哈希。（勘误：本条原误写 BCrypt，实际实现为 PBKDF2，与 T-A3-7 完成记录一致）

## 知识发现

- **2026-07-06｜Spring Boot 3.5 的 `.imports` 只服务 AutoConfiguration**：反编译 spring-boot-3.5.8.jar 确认，`EnvironmentPostProcessor` 与 `AutoConfigurationImportFilter` 均只经 `SpringFactoriesLoader`（spring.factories）加载；`ImportCandidates.load`（读 `META-INF/spring/%s.imports`）全框架仅被 `AutoConfigurationImportSelector` 以 `AutoConfiguration.class` 调用。自建框架用 `.imports` 注册这两类处理器会静默失效。→ 值得沉淀到团队知识库。

## T-A0-1 执行记录（2026-07-06）

Spring Boot 3.5.8 → 3.5.16，`mvn clean compile` **BUILD SUCCESS**（69 模块全过）。弃用告警清单（去重）：

**A. 框架自有 @Deprecated API 的内部自用（属兼容期代码，由后续 task 消除，非升级阻塞）**
- `MessageHandler`（23 处）@ MessageHandlerProcessor.java → 由 T-A1-2（F-A2）消除
- `RpcClient` 注解（20 处）@ RpcClientFactoryBean/ScannerRegistrar/ServiceDiscoveryRpcClient/GrpcRpcServer/RpcServiceRegistrationProcessor → 过渡期兼容，属设计选择
- `JwtUtils`（21 处）@ WebAuthAutoConfiguration.java + DefaultAuthService.java → web 认证链仍用废弃类，随安全修复收敛

**B. 第三方弃用 API（与 SB4 升级相关，工作流 B 处理）**
- `JsonNode.fields()`（JsonUtils.java:459）→ Jackson 3 改 `properties()`（升级设计 §6.2 已列）
- `BaseMapper.selectBatchIds()`（示例 2 处）→ MyBatis-Plus 新 API
- `RedissonClient.getRedLock()`（RedisLockManager.java）→ Redisson 新 API
- `DefaultMQProducer.createTopic()`（RocketMQMessageManager.java）→ RocketMQ 4.x 弃用（迁 5.x 时处理）
- `okhttp3.RequestBody.create(MediaType,String)`（XxlJobHttpClient.java）→ OkHttp 新签名

**C. JDK 弃用（治理阶段/工作流 C 顺手清理）**
- `new URL(String)`（TencentCaptchaHandler.java、DefaultCookieManager.java）→ `URI.create(...).toURL()`
- `Thread.getId()`（TaskEngine.java、RabbitMQMessageProducer.java）→ `Thread.threadId()`

结论：A 类是我们自己的过渡代码，不构成升级阻塞；升级前需重点处理的是 B 类。**preview 相关告警为 0**，佐证代码未使用任何预览语法，为 T-A0-2 移除 `--enable-preview` 扫清前提。

## 踩坑记录

- **2026-07-06｜验证 task 的模块作用域是陷阱（来自 Codex 对抗性审查）**：T-A1-3 原把 RBAC 验证写成 `-pl core/nebula-security -am test`，但 Filter 的真实注册点 `SecurityAutoConfiguration` 在 `autoconfigure/nebula-autoconfigure`（imports:36），core 模块单测过了、Bean 却可能没被 starter 装配，链路照样断。已修正为 starter 级 Web 集成测试，并在阶段 A1 加了共用的"验证纪律"。教训：机制性失效的验证必须走 autoconfigure/starter 路径，不能停在拥有实现类的模块。

## 外部审查核实

- **2026-07-06｜Codex "版本不可解析" [critical] 质疑不成立**：Codex 称 Maven Central 无 3.5.16 / 4.1.0 GA / spring-cloud 2025.1.2（其执行日志无任何网络请求，系凭训练数据）。对照权威 `maven-metadata.xml` 核实：`spring-boot-starter-parent` release=4.1.0（3.5.16、4.0.7 均在列），`spring-cloud-dependencies` release=2025.1.2。目标版本均可直接消费。已在升级设计报告 §1 补版本复核声明。

## 里程碑：机制性失效 #1 修复并验证（2026-07-06，T-A1-1）

审查报告头号事故（Starter 开箱即用机制从未生效）已修复：`NebulaStarterDefaultsPostProcessor` 迁到 spring.factories 注册。关键在于**用反向对照证明测试真的有效**——移除 EPP 注册后，`NebulaStarterDefaultsIntegrationTest` 的 sentinel 断言立即失败（Failures 1）；恢复后通过。这正面回应了本次审查反复强调的"没有一个测试去启动最小应用验证机制真生效"。此测试模式（@SpringBootTest 最小上下文 + 反向对照）作为后续 A1 机制性 task 的模板。

## 里程碑：A1 三个机制性事故全部修复（2026-07-06，T-A1-1/2/3）

审查报告第 1 节列的三大"整条链路静默失效"全部修复并带证明性测试：
1. Starter 开箱即用（EPP 注册）— T-A1-1
2. `@MessageListener` 静默不消费（扫描）— T-A1-2
3. RBAC 链路断裂（SecurityContext 填充 + 类级注解）— T-A1-3

T-A1-3 设计要点：JwtAuthenticationFilter **只填充不拦截**、**默认关闭 opt-in**——这是权衡存量项目（如 proud-day 用自有认证）后的选择：默认注册会给每个消费者塞一个额外 Filter（类似持久化默认开的隐患）。需要 Nebula RBAC 的项目显式开 `nebula.security.jwt.filter.enabled=true`。切面用 `@within` + 反射解析支持类级注解。三个机制修复统一采用"@SpringBootTest/AspectJ 装配级测试 + 反向对照证明测试有效"的模式。

## 决策：老项目 proud-day 与持久化默认值（2026-07-06）

- **背景**：审查发现 T-A1-1（Starter 默认值机制修好）会让 `nebula.data.persistence.enabled=true` 真正注入，打开 Nebula 持久化自动配置，与 proud-day（生产项目，用标准 Spring Boot MyBatis-Plus、自建 `spring.datasource` + `@MapperScan("com.proudday.**.mapper")`、33 个 mapper 全继承 MyBatis-Plus 原生 BaseMapper）冲突。
- **核实**：proud-day 未使用 Nebula 持久化任何特性（0 读写分离/0 分库分表/0 Nebula ServiceImpl）；Nebula 的 `@MapperScan` 写死 `basePackages={"io.nebula.**.mapper"}` + `markerInterface=Nebula BaseMapper`，根本扫不到 proud-day 的 mapper。
- **决策（用户拍板）**：proud-day 走 **B（正式接入 Nebula 持久化）**，且**先改好框架再迁 proud-day**。
- **关键洞察**：正因为"先改框架"，把 Nebula 的 mapper 扫描从写死改为可配置（包路径可配 + 放开强制 Nebula BaseMapper 标记），proud-day 的 33 个 mapper 无需改代码，B 迁移退化为配置级操作。
- **框架侧新增持久化工作**（见 tasks 阶段 A4）：mapper 扫描可配置 / 数据源防御性共存 / 数据源配置契约 / 修 ServiceImpl 假实现与读写分离。
- **sequencing 风险**：proud-day 吃 `2.0.1-SNAPSHOT`，框架发布后它一构建就吃到新行为，proud-day 迁移必须在其重新构建前落地。

## 对外行为变更 / 迁移说明（发布时汇总到 release notes）

- **T-A2-1 OPTIONS**：非预检 OPTIONS 不再免认证。若有依赖"OPTIONS 直接放行"的客户端行为需调整。
- **T-A2-8 缓存多态白名单（破坏性）**：Redis 缓存值反序列化改为白名单（默认仅 java.util/java.time/io.nebula）。**缓存自定义模型对象(如 com.myapp.*)的应用必须配置** `nebula.data.cache.redis.trusted-packages: [com.myapp]`，否则缓存读取反序列化失败。此为关闭 Jackson gadget RCE 的必要收紧。
- **T-A1-3 RBAC Filter**：默认关闭，需要 Nebula RBAC 的应用设 `nebula.security.jwt.filter.enabled=true`。
- **T-A2-6 性能端点**：`/performance/*` 改为默认不暴露，需 `nebula.web.performance.enabled=true` 开启（开启后由应用认证保护）。依赖这些端点的应用需显式启用。
- **T-A2-2 响应缓存（破坏性）**：改为默认关闭 + 仅缓存标注 `@ResponseCacheable` 且不带 `Authorization`/`Cookie` 的 GET。依赖"所有 GET 自动缓存"的应用需：设 `nebula.web.cache.enabled=true` 并在公共只读接口上加 `@ResponseCacheable`。

## 知识发现：多处旧测试把 bug 当"已知问题"断言（2026-07-06）

本次修复正确性 bug 时，发现至少 3 处既有单测把 bug 行为写成断言：
- `RedisLockTest.testTryLockWithTimeoutWithoutWatchdogUsesLeaseTime` 断言 `tryLock(5,30000,SECONDS)`（单位错乱的 bug）
- `JwtUtilsTest.testRefreshToken` 断言 refreshToken 抛 `UnsupportedOperationException`，注释还写"这是已知问题，需要重写源代码"
- `ExceptionFullTest.testValidationExceptionAddFieldError` 断言 addFieldError 抛 UOE，注释写"已知问题"
这类"测试固化 bug"是审查报告"没有测试兜底/测试质量低"论点的实证——测试不但没拦住 bug，反而把 bug 锁死为期望行为。修复时须同步订正这些测试。

## 对外行为变更 / 迁移说明补充

- **T-A2-4a RPC**：`RpcRequest.parameterTypes` `Class<?>[]`→`String[]`（线格式不变）。若有第三方直接构造 RpcRequest 需改类型。
- **T-A3-7 密码哈希**：`CryptoUtils.encrypt` 已 `@Deprecated`，新密码存储改用 `hashPassword`/`matchesPassword`(PBKDF2)。雪花默认 workerId/datacenterId 改为环境变量 `NEBULA_SNOWFLAKE_WORKER_ID`/`_DATACENTER_ID` 或本机派生，生产建议显式配置。

## 对外行为变更补充（工作流 C 前置批）

- **T-C1-3 双 MQ 选主**：RabbitMQ 与 RocketMQ 同时启用不再启动崩溃（此前双 @Primary MessageManager 冲突）。`@MessageListener` 注册到 `nebula.messaging.primary` 指定的 MQ（默认 rabbitmq）；配置值无匹配时启动快速失败并列出候选 Bean。单 MQ 部署行为不变，无需任何配置。
- **T-C1-7 多级缓存（增强，非破坏）**：multi-level 模式新增 Redis pub/sub 失效广播（频道 `{keyPrefix}invalidation`，自动装配 `RedisMessageListenerContainer`），节点间 L1 旧副本在写后被驱逐而非等 TTL；`getOrSet` 增加 single-flight 击穿防护（无需配置）；新增可选穿透防护 `nebula.data.cache.multi-level.null-caching-enabled=true` + `null-value-ttl`（默认关闭，开启后回源 null 写 60s 哨兵，业务创建数据后主动 set 或等哨兵过期）。
- **T-C1-6 RabbitMQ tag 路由（修复即变更）**：带 tag 的消息路由键从 topic 改为 tag——`subscribeWithTag` 的队列此前永远收不到消息（绑定键=tag 与发送键=topic 不匹配），修复后 tagged 队列开始真实收到消息；同时普通订阅队列（绑定键=topic）不再收到带 tag 的消息。依赖"tagged 消息也进普通队列"旧行为的应用需改用无 tag 发送。
- **T-C1-10 定时任务失败上报**：`TimedTaskJobHandler` 四个入口（everyMinute/FiveMinute/Hour/Day）子任务抛异常时该轮调度改为返回 FAIL（此前恒报 SUCCESS）。XXL-JOB 调度中心配置了失败告警/重试的任务将开始真实收到失败通知；单个子任务失败仍不中断同批其余任务。
- **T-C1-9 不安全默认值收紧（破坏性，共四处）**：
  1. `nebula.discovery.nacos.username/password` 默认 "nacos"/"nacos" → 空（不发送认证）。开鉴权的 Nacos 集群必须显式配置凭据；未开鉴权的集群无影响。
  2. `nebula.task.xxl-job.access-token` 默认 "xxl-job" → 空（不发送令牌头）。开启令牌校验的 XXL-JOB 调度中心必须显式配置；未开校验无影响。
  3. `nebula.websocket.allowed-origins` 默认 {"*"} → {}（仅同源可握手）。跨域接入 WebSocket 的前端部署必须显式配置来源。
  4. `nebula.web.cors.allow-credentials` 默认 true → false。需要跨域携带 Cookie/Authorization 的应用须显式开启，并同时精确配置 `allowed-origins`（浏览器规范禁止 credentials + 通配来源组合）。
- **T-C1-2 XFF 可信代理（破坏性）**：限流键与请求日志的客户端 IP 此前直接信任 `X-Forwarded-For` 首段（客户端每请求伪造新 IP 即绕过 IP 限流）。修复后**默认完全不读转发头**，一律使用 remoteAddr；部署在反向代理后的应用必须配置 `nebula.web.trusted-proxies: [<代理IP或IPv4 CIDR>]` 才恢复真实客户端 IP 解析（算法：XFF 从右向左取第一个不可信地址）。不配置的直连部署无影响。

## 对外行为变更补充（T-A3-3）

- **缓存 key 命名空间化（破坏性）**：`DefaultCacheManager` 此前 key 不带前缀存储，`clear()` 用 `KEYS "*"` 会清整个 Redis 库（误删同库业务数据）。修复后所有 key 统一加 `nebula.data.cache.redis.key-prefix`(默认 `nebula:cache:`) 前缀，`clear()`/统计按前缀 SCAN 圈定。**影响**：升级后旧的无前缀缓存条目读取会 miss（缓存自动重建，无数据丢失风险）；与其他系统共用 Redis 库的部署终于安全。proud-day 已配 `pd:cache:`，升级后其 key 前缀才真正生效（符合其配置本意）。

## 待补验证（环境受限）

- **T-A0-3 RabbitMQ live 收发**：本地无 Docker/RabbitMQ，只验证了版本对齐(spring-rabbit 3.1.0→3.2.8, 与 spring-amqp 3.2.8 一致)与编译；收发端到端需在带 broker 的 CI/环境补跑。同类:凡验证需要 Redis/MQ/Nacos/ES 外部服务的 task, 本地只做编译级验证, 行为级验证转 CI。

## Spec-Code 偏差

- **2026-07-06｜SSA-1 事实源路径勘误（main 审查阶段核验发现，随 hardening-b 合入）**：审查报告 SSA-1 与 tasks T-A3-8 原将 `CustomChromaVectorStore` 标注在 `nebula-ai-spring` 模块，逐源码核对后确认该类实际位于 `autoconfigure/nebula-autoconfigure/src/main/java/io/nebula/autoconfigure/ai/CustomChromaVectorStore.java`（问题成立、行号 157 精确：`similaritySearch` 仅传 query+topK、`delete(Filter.Expression)` 抛 UOE）。关键区别：`nebula-ai-spring` 的 `SpringAIVectorStoreService` 反而正确处理了 filter/threshold（其 :361-370 构建 filterExpression 与 similarityThreshold）。已同步修正报告 SSA-1 与 tasks T-A3-8 的路径标注，避免修复时改错文件。
- **2026-07-06｜升级目标版本联网复核通过**：对照 Maven 中央仓库 `maven-metadata.xml` 确认 `spring-boot-starter-parent` latest/release=4.1.0（3.5.16、4.0.7 在列）、`spring-cloud-dependencies` latest/release=2025.1.2、`spring-ai-bom` latest/release=2.0.0，升级设计 §1 版本断言成立。另全仓库 grep 无任何预览语法（`STR.`/`ScopedValue`/`StructuredTaskScope` 等），F-A12 移除 `--enable-preview` 确认零风险。
- **2026-07-06｜hardening-a 全量审查结论（28 提交逐一核对，hardening-b 修复）**：两处"已完成"记录与代码不符——(1) T-A3-8 称 SSA-1 不适用（"CustomChromaVectorStore 已不存在"），实际该类存在于 autoconfigure 模块且缺陷原样保留，根因是路径勘误被误读为类已删除，已在 hardening-b 修复并补 `CustomChromaVectorStoreTest`；(2) T-A2-4a 只修了 HTTP 侧 `Class.forName`，gRPC 侧 `GrpcRpcServer.parseParameterTypes` 同源漏洞被遗漏（tasks 文件清单本就未列 gRPC 文件，属 Spec 缺口而非实现走样），已在 hardening-b 以同一策略修复并补 `GrpcRpcServerFindMethodTest`。另发现 1 处 P2 行为缺口：MW-3 修复把发送路由键统一为 topic 后，`subscribeWithTag`（按 tag 绑定）永远收不到消息且生产者无发 tag 的 API——已登记进 EPIC-C1 待修。其余 26 项完成记录与代码逐一相符。

## 里程碑：工作流 A 收官（2026-07-06，全量回归通过）

- `mvn clean install` **BUILD SUCCESS**：69 模块全过，Total time 01:40。
- 测试合计 **750 run / 0 failures / 0 errors / 3 skipped**。3 个 skipped 均为 `nebula-crawler-browser` 的 `StealthIntegrationTest`（需真实 Playwright 浏览器环境，历史遗留的条件跳过，与本工作流改动无关）。
- 变更规模：87 文件（+2792 / -1239），32 个原子提交。变更摘要已回填 tasks.md 末尾。
- 待办出口：行为级验证清单转 CI（见「待补验证」）；下一步方向 B（升 SB4.1）/ C（P1 可靠性+治理）/ D（proud-day 迁移）待用户拍板。

## T-B1-1 执行记录（2026-07-06）

POM 切版本 + classic starters + 依赖版本升级。`mvn validate` **全 69 模块通过**。

### 版本变更清单

| 依赖 | 旧版本 | 新版本 | 备注 |
|------|--------|--------|------|
| spring-boot-starter-parent | 3.5.8 | 4.1.0 | Spring Framework 7 / Jakarta EE 11 |
| spring-cloud-dependencies | 2025.0.0 | 2025.1.2 | |
| spring-ai-bom | 1.1.0 | 2.0.0 | |
| mybatis-plus | 3.5.9 | 3.5.16 | artifactId: boot3→boot4 |
| redisson | 3.39.0 | 4.6.1 | |
| jjwt | 0.12.3 | 0.13.0 | |
| spring-grpc (new) | - | 1.1.0 | 替代 net.devh:grpc-spring-boot-starter |
| maven-surefire-plugin | 3.2.2 | 3.5.5 | |

### Spring Boot 4.1 breaking changes（POM 层面已处理）

1. `spring-boot-starter-aop` → `spring-boot-starter-aspectj`（3 处子模块已改）
2. `spring-cloud-gateway-server` → `spring-cloud-gateway-server-webflux`（gateway-core + autoconfigure 已改）
3. `spring-cloud-starter-gateway` → `spring-cloud-starter-gateway-server-webflux`（starter-gateway 已改）
4. `net.devh:grpc-spring-boot-starter` → `org.springframework.grpc:spring-grpc-core`（rpc-grpc 临时替换，T-B2-2 完整迁移）
5. `javax.annotation-api` → `jakarta.annotation-api`（rpc-grpc 已改）
6. 新增全局依赖 `spring-boot-starter-classic`（过渡桥，T-B3-2 移除）

### 编译验证（预期失败，后续任务修复）

`mvn compile` 出现两类编译错误，均属后续任务范围：
- `org.springframework.boot.autoconfigure.jdbc` 包不存在 → T-B1-2（包名迁移）
- `org.springframework.boot.actuate.health` 包不存在 → T-B1-2（包名迁移）

## T-B1-2 执行记录（2026-07-06）

Spring Boot 4.1 模块化包名迁移。排除 gRPC 模块后 **68 模块编译通过**。

### 发现的包名迁移规律

Spring Boot 4 把 `o.s.b.autoconfigure.<tech>.*` 拆到 `o.s.b.<tech>.autoconfigure.*`，部分类还改了名（加 `Data` 前缀）：
- `autoconfigure.jdbc` → `jdbc.autoconfigure`
- `autoconfigure.data.redis` → `data.redis.autoconfigure`（类名 `RedisAutoConfiguration` → `DataRedisAutoConfiguration`）
- `autoconfigure.neo4j` → `neo4j.autoconfigure`
- `autoconfigure.elasticsearch` → `elasticsearch.autoconfigure`
- `autoconfigure.data.elasticsearch` → `data.elasticsearch.autoconfigure`（类名加 `Data` 前缀）
- `autoconfigure.jackson` → `jackson2.autoconfigure`（注意是 `jackson2` 不是 `jackson`）
- `actuate.health` → `health.contributor`
- `web.context` → `web.server.context`

### Spring Boot 4.1 的 starter 改名（T-B1-1 已处理）

| 旧名 | 新名 |
|------|------|
| `spring-boot-starter-aop` | `spring-boot-starter-aspectj` |
| `spring-cloud-gateway-server` | `spring-cloud-gateway-server-webflux` |
| `spring-cloud-starter-gateway` | `spring-cloud-starter-gateway-server-webflux` |

### 注意

`spring-boot-starter-web` 在 4.1 中仍然存在（版本 4.1.0），不需要立即改为 `spring-boot-starter-webmvc`，T-B1-4 中可评估是否迁移。

## T-B2-1 执行记录（2026-07-07）

Gateway 坐标改名 + 路由重构 + XFF 可信代理 + 路由钉死修复。

### 变更清单

1. **Gateway XFF 可信代理解析（RDG-15 并入）**
   - 新增 `io.nebula.gateway.util.ReactiveClientIpResolver`：与 nebula-web 的 `ClientIpResolver` 同源安全语义（默认不信任 XFF，仅 remoteAddr 命中可信代理列表才解析 XFF，从右向左取第一个不可信地址），适配 WebFlux `ServerHttpRequest`
   - `GatewayProperties.LoggingConfig` 新增 `trustedProxies`（默认空=不信任 XFF）
   - `LoggingGlobalFilter` 改为注入 `ReactiveClientIpResolver`，删除旧的直接信任 XFF 的 `getClientIp` 方法
   - `RateLimitKeyResolverConfig` 改为注入 `ReactiveClientIpResolver`，删除旧的直接信任 XFF 的 `getClientIp` 方法
   - `GatewayAutoConfiguration` 注册 `ReactiveClientIpResolver` Bean（@ConditionalOnMissingBean 可替换），注入到日志过滤器

2. **路由钉死修复**
   - `GatewayRoutesAutoConfiguration.determineTargetUri`：移除启动时通过 `ServiceDiscovery` 解析到固定 IP 的逻辑（`@PostConstruct` 时解析的地址会钉死到单实例，运行期服务实例变化不可感知）
   - `useDiscovery=true` 时统一返回 `lb://serviceName`，由 `NebulaServiceInstanceListSupplier`（已有）适配 Spring Cloud LoadBalancer 动态路由
   - `GatewayRoutesAutoConfiguration` 移除 `ServiceDiscovery` 注入（不再直接依赖）

3. **gateway-example 版本修正**
   - 移除 `spring-cloud.version=2025.0.1` 覆盖（继承根 POM 的 `2025.1.2`，否则 `spring-cloud-starter-gateway-server-webflux` 在 `2025.0.x` 中不存在会解析失败）
   - 移除 `jjwt.version=0.12.5` 覆盖（继承根 POM 的 `0.13.0`）

### 破坏性变更

- Gateway XFF 解析行为变更：之前直接信任 XFF 首段，现在默认不读 XFF（直接用 remoteAddr）。反代场景需显式配置 `nebula.gateway.logging.trusted-proxies`
- 路由解析方式变更：之前启动时解析 ServiceDiscovery 到固定 IP，现在统一走 `lb://`。需确保 Spring Cloud LoadBalancer 在 classpath 上（`nebula-starter-gateway` 已包含）

### 测试

- `ReactiveClientIpResolverTest` 7 用例：默认不信任/可信代理解析/CIDR/不可信 remoteAddr/X-Real-IP 回退/无 header/全 XFF 可信
- `GatewayRoutesAutoConfigurationTest` 3 用例：服务发现生成 lb:// URI/静态地址直接使用/自定义 serviceName
- 全量测试 BUILD SUCCESS

_（开发中追加：实现与 Spec 不一致时，先更新 Spec 再改代码，并在此记录）_
