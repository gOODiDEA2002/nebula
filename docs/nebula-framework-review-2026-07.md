# Nebula Framework 全面审查报告（2026-07）

> 审查版本：2.0.1-SNAPSHOT
> 审查日期：2026-07-05
> 基础框架：Spring Boot 3.5.8 / Spring Cloud 2025.0.0 / Java 21
> 审查方式：7 路并行分域深审（逐文件通读 main 源码，约 430 个源文件 / 7.5 万行），所有高危结论均附 `文件:行` 一手证据
> 上一版审查：`docs/nebula-framework-review.md`（2026-04-15，137 项问题）

---

## 0. 一句话结论

Nebula 是一个"接口设计能力强、工程收尾能力弱"的自研框架：抽象分层、注解体系、条件装配的骨架相当完整，命名与文档也用心；但大量能力停留在"看起来能用"的状态——多个宣称的特性因一个注册键写错、一个条件写漏、一处单位换算错误而**整条链路静默失效**，且几乎没有集成测试兜底，导致这些断裂长期无人发现。

**在修复本报告 P0 清单之前，不建议在新项目中把 Nebula 作为生产底座全面启用。** 可行的做法是先小范围启用少数成熟模块（见第 5 节成熟度矩阵中 7 分及以上者），同时立项修复。

---

## 1. 最重要的发现：三个"整条链路静默失效"的机制性事故

这三个问题比任何单点 bug 都严重，因为它们让"框架宣称的核心卖点"根本没运行过，且被示例项目的写法意外掩盖。

### 1.1 Starter "开箱即用" 机制从未生效

- **现象**：所有 Starter 靠 `NebulaStarterDefaultsPostProcessor`（一个 `EnvironmentPostProcessor`）注入 `nebula-defaults.properties` 的默认启用项。但该处理器注册在 `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` 文件里。
- **根因**：Spring Boot 3.5 只从 `META-INF/spring.factories` 的 `org.springframework.boot.env.EnvironmentPostProcessor` 键加载这类处理器，**不支持用 `.imports` 文件注册 EnvironmentPostProcessor**。
- **字节码级证据（反编译 spring-boot-3.5.8.jar 确认）**：
  - `EnvironmentPostProcessorsFactory.fromSpringFactories(ClassLoader)` 内部只调 `SpringFactoriesLoader.forDefaultResourceLocation(...)`——即只读 `META-INF/spring.factories`，全程不触碰 `.imports`。`EnvironmentPostProcessorApplicationListener` 正是走这条工厂方法加载 EPP。
  - `.imports` 机制由 `ImportCandidates.load(Class, ClassLoader)` 实现，读取资源模板 `META-INF/spring/%s.imports`；但全 Spring Boot 中**只有 `AutoConfigurationImportSelector` 调用它，且只传入 `AutoConfiguration.class`**。没有任何加载器会用它去读 EnvironmentPostProcessor / AutoConfigurationImportFilter 的 `.imports`。
- **后果**：所有 starter 的默认值从未被注入，"引入 starter 即开箱即用"这一整套设计没运行过。示例项目都在 `application.yml` 里手写了 `enabled=true` 才跑得起来，恰好把这个 bug 盖住了。
- **同源问题（已被外部审查质疑，此处给出定音证据）**：`NebulaAutoConfigurationImportFilter`（本意屏蔽 DataSource/MyBatis-Plus/JPA 等原生自动配置）也用 `META-INF/spring/...AutoConfigurationImportFilter.imports` 注册，同样是死代码。字节码确认：`AutoConfigurationImportSelector.getAutoConfigurationImportFilters()` 加载 `AutoConfigurationImportFilter` 类常量后调用的是 `SpringFactoriesLoader.loadFactories(...)`，即**只读 spring.factories，不读 `.imports`**。其对照组 `SpringAIAutoConfigurationFilter` 就注册在 `spring.factories` 里，能生效——这从侧面印证了 `.imports` 注册 ImportFilter 是无效写法。
- **证据文件**：`autoconfigure/.../META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`、`...AutoConfigurationImportFilter.imports`（两文件物理存在，但无加载器读取）。
- **修复**：把两个处理器注册迁回 `spring.factories`；补一个 `SpringApplication` 级集成测试，断言"只引入 starter、不写任何 enabled"时默认模块确实启用。

### 1.2 `@MessageListener` 是空壳注解，消息静默不消费

- **现象**：v2.0.1 宣布废弃 `@MessageHandler`、改用 `@MessageListener`。新注解定义了，旧注解也标了 `@Deprecated`。
- **根因**：全仓库没有任何处理器扫描 `@MessageListener`——`MessageHandlerProcessor.java:39,52` 仍只查 `@MessageHandler`，RabbitMQ/RocketMQ 两个自动配置注册的也都是它。
- **后果**：凡按官方文档改用 `@MessageListener` 写的消费者，根本不会注册，消息静默不被消费。`examples/fullstack-example/.../OrderNotificationHandler.java` 已经踩中。
- **修复**：让 Processor 同时扫描新旧注解（新注解优先），补集成测试。

### 1.3 RBAC 授权链路断裂 + 类级安全注解静默失效

两个问题叠加，使 `nebula-security` 的授权能力事实上不可用：

- **SecurityContext 从不被填充**：全仓库无任何生产代码调用 `SecurityContext.setAuthentication()`（仅测试代码），框架没有提供解析 JWT 并写入上下文的 Filter/Interceptor。`SecurityAspect` 每次拿到的 authentication 都是 null。`nebula-web` 的 `AuthInterceptor` 与 `nebula-security` 是两套互不打通的平行认证体系。证据：`SecurityContext.java:13`。
- **类级安全注解静默失效**：`@RequirePermission` 等三个注解声明了 `@Target({METHOD, TYPE})`，但切点只写了 `@annotation(...)`——**加在类上的注解完全不生效**。开发者以为整个 Controller 受保护，实则完全不设防，属授权绕过。证据：`SecurityAspect.java:35,46,83`。
- **修复**：提供 `JwtAuthenticationFilter`（解析 header → 校验 → 写入 SecurityContext，finally 清理）；切点补 `@within(...)`，并加类级注解测试。

---

## 2. P0 紧急清单（高危：影响正确性 / 安全性，须修复后方可上生产）

> 编号规则：域代码 + 序号。域代码见第 4 节。

### 2.1 安全绕过 / 数据泄漏

| 编号 | 文件:行 | 问题 |
|------|---------|------|
| CWT-8 | `nebula-web/.../interceptor/AuthInterceptor.java:48-51` | commit 0f6f2003 "预检请求直接放行" 无条件放行**所有** OPTIONS 请求。Spring Boot 默认 `dispatch-options-request=true`，凡 `@RequestMapping` 未限定 method 的业务接口，可被 `OPTIONS /api/xxx` 无认证触发。应改用 `CorsUtils.isPreFlightRequest()` 只放行真预检。 |
| CWT-9 | `nebula-web/.../cache/DefaultCacheKeyGenerator.java` + `ResponseCacheInterceptor.java` | 响应缓存**跨用户串数据**：缓存键不含 Authorization/用户身份，且默认启用（matchIfMissing=true），只排除 no-cache 的 GET。"当前用户信息"类接口，A 用户的响应会被缓存后原样返回给 B 用户。 |
| CWT-10 | `nebula-web/.../autoconfigure/WebAuthAutoConfiguration.java:65-76` | `@SensitiveData` 脱敏**事实上不生效**：introspector 只注册到独立的 `dataMaskingObjectMapper`，Spring MVC 序列化用的主 ObjectMapper 没挂它，手机号/身份证等原样出网。 |
| CWT-11 | `nebula-task/.../xxljob/service/XxlJobTaskService.java:80-112` | 遗留 REST 端点 `/run`、`/kill`、`/log` 映射在业务应用 Web 根路径，读 accessToken 头但**从不校验**——任何人可经业务端口触发任意任务、读日志；且 token 明文进 INFO 日志。 |
| RDG-5 | `nebula-rpc-http/.../server/HttpRpcController.java:40` + `nebula-rpc-grpc/.../GrpcRpcServer.java:193` | `/rpc` 与 gRPC 通用服务**完全无认证**，任何能访问端口者可按方法名调用所有已注册服务；且 `parameterTypes` 由请求侧提供，存在远端触发 `Class.forName` 任意类加载的风险。 |
| SSA-2 | `nebula-search-es 自动配置 ElasticsearchAutoConfiguration.java:102-127` | `setHttpClientConfigCallback` 被调三次互相覆盖，Basic 认证回调被连接池回调冲掉——**配了用户名密码也不生效**。 |
| CD-11 | `autoconfigure/.../data/CacheAutoConfiguration.java:127-131` | Redis 缓存序列化开了全量多态（`activateDefaultTyping(Object.class, NON_FINAL)`），若 Redis 可被写入存在 Jackson gadget 反序列化 RCE 风险。 |
| CWT-23 | `nebula-web/.../controller/PerformanceController.java` | `/performance/system` 无认证暴露 JVM/OS/线程/GC 信息；`/performance/reset` 无鉴权且是空实现（谎报 success）。 |

### 2.2 正确性 / 可靠性错误

| 编号 | 文件:行 | 问题 |
|------|---------|------|
| CD-1 | `nebula-lock-redis/.../RedisLock.java:87-89` | `tryLock` 时间单位混用：leaseTime 已转毫秒却按调用方 unit 解释。`@Locked(timeUnit=SECONDS, leaseTime=60)` 实际租约变成 60000 秒（约 16.7 小时），锁基本永不过期。 |
| CD-2 | `nebula-data-persistence/.../service/impl/ServiceImpl.java:43-80` | `findByField/findByFields/findTopN/findRandomN` 是假实现直接返回全表；`findOneByField` 竟把字段值当主键 `findById((Serializable)value)`——静默返回错误数据。 |
| CD-5 | `nebula-data-cache/.../DefaultCacheManager.java:642-700` | `clear()` 用 `keys("*")` 删整个 Redis DB（误删同库其他业务数据），`getStats()` 同样 `KEYS *` 生产阻塞。 |
| RDG-1 | `nebula-rpc-core/.../ServiceDiscoveryRpcClient.java:76` + `HttpRpcClient.java:33` | 每次调用前改写共享单例客户端的 baseUrl，多线程并发调不同服务时目标地址互相覆盖，请求打到错误的服务/实例。 |
| RDG-3 | `RpcDiscoveryProperties.java:40` + `RpcDiscoveryAutoConfiguration.java:313` | 配置校验允许 `weighted`，但 `LoadBalanceStrategy` 枚举无 `WEIGHTED` 常量，配了加权负载均衡直接**启动崩溃**。 |
| RDG-4 | `nebula-discovery-nacos/.../NacosServiceAutoRegistrar.java:54` | `onApplicationEvent` 只对 `event instanceof ServletWebServerInitializedEvent` 分支做注册（第 9 行 `import WebServerInitializedEvent` 未被使用，属误导）。故凡不是 Servlet Web 应用的都不会触发自动注册——既包括 **Reactive Web（网关 starter 默认开 nacos）**，也包括**纯 gRPC/非 Web 服务**。已核对全模块无第二条注册路径（仅此一个 Registrar）。修复应改判 `event instanceof WebServerInitializedEvent`（父类，同时覆盖 Servlet 与 Reactive），非 Web 的 gRPC 场景另需独立注册钩子。 |
| MW-2 | `nebula-messaging-rabbitmq/.../RabbitMQMessageConsumer.java:119,187` | 消费失败一律 `basicNack(requeue=true)`，无重试上限无 DLQ——毒消息无限重投死循环。 |
| MW-3 | `RabbitMQMessageProducer.java:89` vs `Consumer.java:79` | 生产端 `send(topic,payload)` 发 routingKey=""，消费端队列按 topic 绑定，topic 交换机上 "" 不匹配任何绑定且未设 mandatory → 消息静默丢弃。 |
| MW-4 | `nebula-messaging-redis/.../RedisStreamConsumer.java:328` | `Message.headers` 为 null 时 NPE：不带 headers 的 Stream 消息消费端必抛，永不 ack。 |
| SSA-1 | `nebula-ai-spring/.../CustomChromaVectorStore.java:157-186` | `similaritySearch` 忽略 `filterExpression` 与 `similarityThreshold`，导致向量存储的 get(id)/exists/deleteByFilter/带过滤搜索全部失效。 |
| CF-1 | `nebula-foundation/.../util/IdGenerator.java:32` | `DEFAULT_SNOWFLAKE = new SnowflakeIdGenerator(1,1)` 写死 workerId/datacenterId，集群多实例同毫秒必然生成重复 ID。 |
| CF-2 | `nebula-foundation/.../util/JwtUtils.java:276` | `refreshToken()` 对 jjwt 0.12 的不可变 Claims 调 `remove()`，运行时 100% 抛 UOE，方法完全不可用。 |
| CF-3 | `nebula-foundation/.../exception/ValidationException.java:38` | 构造器用 `List.of()` 不可变列表，随后 `addFieldError().add()` 必抛 UOE。 |
| CF-5 | `nebula-foundation/.../security/CryptoUtils.java:383` | `encrypt(password)` 用单轮 SHA-256 做密码哈希，GPU 可秒试数十亿次，拖库即裸奔。应改 BCrypt/Argon2。 |
| ASI-1 | `autoconfigure` EPP/ImportFilter `.imports` 注册 | 见 1.1，Starter 默认值机制与自动配置排除过滤器双双失效。 |
| ASI-4 | `autoconfigure/.../data/Neo4jAutoConfiguration.java` | 两个 Neo4j 自动配置类没注册进任何 imports，Neo4j 支持整体是死功能。 |

### 2.3 构建 / 交付级隐患

| 编号 | 位置 | 问题 |
|------|------|------|
| ASI-build | 根 `pom.xml` 编译参数 | 全局 `--enable-preview`：产物字节码 minor version 65535，**强制所有消费方在同一 JDK 大版本 + 运行时加 `--enable-preview`**。若未实际使用预览语法应立即移除，否则会成为升级 Spring Boot 4 的第一颗雷。 |
| ASI-rabbit | 根 `pom.xml` spring-rabbit 3.1.0 | 显式钉死的版本与 Boot 3.5.8 配套的 spring-amqp 3.2.x 严重不匹配，应删除覆盖交还 BOM 托管。 |

---

## 3. P1 重要问题（中危：影响可靠性 / 一致性，摘录高频与高影响项）

- **拦截器顺序缺陷**（CWT-18）：限流拦截器排在认证之后，无法保护认证逻辑挡匿名洪水；限流与缓存相对顺序未定义，缓存命中会绕过限流计数。建议显式分配 order：Logging < RateLimit < Auth < Cache < Perf。
- **X-Forwarded-For 可伪造绕过限流**：Gateway（RDG-15）、Web（CWT-28）、爬虫多处直接信任 XFF 首段，客户端每请求换假 IP 即绕过 IP 限流。应仅在来自可信代理时解析。
- **双 MQ 并存冲突**（MW-20）：RabbitMQ 与 RocketMQ 同时启用时，两个 `@Primary` MessageManager + 同名 bean 冲突，`@MessageHandler` 只注册到先装配的一个。
- **读写分离动态路由不可达**（CD-3）：`primaryDataSource` 的条件表达式漏排除 `dynamic-routing`，`dynamicDataSource` 的 `@ConditionalOnMissingBean` 永不成立。
- **读写分离切面顺序错**（CD-9）：`@Order(1)` 使数据源切面先于事务切面，`@ReadDataSource + @Transactional` 同方法时事务会在从库开启，事务内写操作落到从库。
- **多级缓存无跨节点失效**（CD-10）：无 Redis pub/sub 广播，多实例部署其他节点 L1 脏读到 TTL 到期；`getOrSet` 无 single-flight 缓存击穿；无空值缓存无穿透防护。
- **RocketMQ 生命周期不对称**（MW-15）：Push 消费者应用关闭时不 shutdown，非优雅停机；事务状态表只写不清内存泄漏，重启后回查失忆。
- **Netty WebSocket 握手时机错误**（MW-5）：`channelActive`（握手前）就注册会话+发帧，`/health` 等 HTTP 请求也产生幽灵会话；IdleStateHandler 无事件消费，空闲清理配置无效。
- **大量"死配置"**：RPC 连接池/重试/压缩、gRPC server.*、Nacos 心跳、锁的 defaultWaitTime、Security 的 anonymousUrls/rbac.*、缓存 keyPrefix 等配置项定义了却从未被消费，误导使用者。
- **异常吞噬**：`JsonUtils` 全类失败静默返回 null；`RedisLockManager.tryExecute` 吞业务异常返回 null；`TimedTaskJobHandler` 子任务异常吞掉仍返回 SUCCESS，xxl-job 管理端永远显示成功。
- **多套错误码体系割裂**（CF-40）：`Result`（字符串 "SUCCESS"）/ `ResultCode`（枚举 "0000"）/ `Constants`（数字码）三套互不一致，`ResultCode.getByCode()` 永远查不到。

完整的中低危清单（合计约 200 项）保留在各域审查原始记录中，本报告只汇总结构性与高频问题。

---

## 4. 分域审查小结

域代码：**CF**=core/foundation，**CS**=core/security，**CWT**=web+task，**CD**=data+cache+lock，**MW**=messaging+websocket，**RDG**=rpc+discovery+gateway，**SSA**=storage+search+ai+crawler，**ASI**=autoconfigure+starter+integration。

- **CF 基础组件**：结构清晰、防御式风格统一，JWT 验签拒绝 alg=none、AES 随机 IV、恒定时间比较等核心安全路径做对了，高于常见自研基础库。但有 5 个运行期必炸的正确性缺陷（见 P0）+ 三套错误码割裂。
- **CS 安全**：JWT 子系统单看质量不错（新 API、token 类型区分、启动密钥强校验已落地），但作为安全模块有两个致命断点（RBAC 上下文不填充、类级注解失效），加上死配置和 `@Data` 造成的密码/token 日志泄漏面，目前是脚手架而非可投产组件。
- **CWT Web+任务**：功能覆盖面广，v2.0.1 四项修复确已落地，子配置拆分后职责清晰。但安全默认值与"配置生效链路"问题密集（OPTIONS 绕过、缓存串数据、脱敏不生效、限流顺序、性能端点裸奔），多处"看似生效实则空转"。任务模块 d2a0132e 打通了核心链路但 executorName 缺省仍会注册失败，遗留 REST 协议成纯攻击面。
- **CD 数据+缓存+锁**：锁模块近几轮修复真实落地（Redisson 冲突、看门狗、NPE），但 tryLock 单位 bug 会造成锁长期滞留；持久化的 ServiceImpl 一批假实现会静默返回错数据、读写分离基本不可达；缓存号称多级却无跨节点失效、`clear()` 是事故级操作。
- **MW 消息+WebSocket**：`@MessageListener` 空壳是纪律性事故；RabbitMQ 实现可靠性失守（毒消息死循环、prefetch 摆设、事务假成功、路由键打架）；**新增的 RocketMQ 模块质量反而最高**（语义映射清晰、唯一认真做统计），主要缺停机钩子和事务持久化；WebSocket core 最干净，Netty 版握手时机错误是硬伤。
- **RDG RPC+发现+网关**：共享单例客户端并发串地址、`/rpc` 无认证 + 任意类加载、加权 LB 配置即崩、Reactive 应用不注册——RPC 层"能跑但边界模糊"，深度绑定已停更的 net.devh gRPC starter；网关职责收敛符合三原则，但路由钉死单实例、XFF 可伪造、LoadBalancer supplier 阻塞事件循环需生产前解决。
- **SSA 存储+搜索+AI+爬虫**：v2.0.1 声称的 AI/存储/爬虫修复全部落地；ES 认证回调覆盖 bug 是硬伤，向量检索忽略过滤条件、API key 进日志需处理；**爬虫是工程完成度最高的部分**（浏览器池管理细致）；`docker/proxy-pool/` 是基于开源 proxy_pool 二次定制的 Python 代理池，用途正当，入库前需清理硬编码密码与 .pyc 缓存。
- **ASI 装配+Starter+集成**：三级启用策略大面上执行到位，但两个核心机制文件（EPP、ImportFilter）注册方式错误导致静默失效（见 1.1），JAR 内打包 application.yml、多个死键、聚合 starter 滥用 optional、api starter 夹带 ORM；integration 的 payment/notification 仍是占位骨架。

---

## 5. 模块成熟度矩阵

评分口径：10=可直接生产；7-8=修小问题即可用；5-6=有结构性缺陷需返工；≤4=半成品/原型。

| 模块 | 评分 | 可用性判断 |
|------|------|-----------|
| nebula-lock-core | 8 | 接口设计佳，可用 |
| storage (core/minio/oss) | 7 | 工程质量较高，对齐 OSS 桶缓存即可 |
| crawler (全套) | 7 | 完成度最高，修 ProxyPool 并发即可 |
| websocket-core | 7 | 最干净模块 |
| examples 示例体系 | 7 | 质量扎实（但掩盖了 starter defaults bug） |
| nebula-data-neo4j | 7 | 小而干净，但需先修 imports 未注册（否则整体死） |
| RocketMQ | 6.5 | 补齐停机+事务后可达 8 |
| nebula-lock-redis | 6 | 修 tryLock 单位 bug 后可用 |
| gateway-core | 6 | 修路由/XFF/阻塞三项 |
| websocket-spring | 6 | 集群故事只讲一半 |
| autoconfigure (整体) | 5 | 最缺装配自身的集成测试 |
| nebula-foundation | 6 | 修 5 个必炸缺陷后达 7.5 |
| nebula-web | 5.5 | 修安全默认值链路前不建议生产 |
| discovery-nacos | 6 / core 5 | Nacos 封装扎实，core 高级 LB 有并发缺陷且多不可达 |
| rpc-http | 5 | 无连接池、错误传播弱、端点无认证 |
| nebula-task | 5 | 遗留 REST 协议成攻击面 |
| data-persistence | 4 | ServiceImpl 假实现 + 读写分离不可达 |
| data-cache | 4 | 无跨节点失效 + KEYS * |
| starter 体系 | 4 | 核心注入机制从未生效 |
| messaging-redis | 4.5 | Stream 可靠消息名不副实 |
| rpc-grpc | 4 | Channel 生命周期硬伤 + 绑定停更 starter |
| websocket-netty | 4 | 握手时机错误 |
| messaging-rabbitmq | 3.5 | 可靠性失守，需返工 |
| rpc-async | 3 | 注解参数全未实现，原型阶段 |
| nebula-security | 4.5 | 授权链路断裂，半成品 |
| integration (payment/notification) | 3 | 占位骨架 |

---

## 6. 横切主题（比单点更值得治理的系统性问题）

1. **零集成测试是根因**：全框架 430 个源文件仅 74 个测试文件，autoconfigure、ai-core、crawler、messaging-core/redis、rpc-core、search-core、websocket 全部 0 测试。三个"机制性事故"（第 1 节）本质都是"没有一个测试去启动一个最小应用验证它真的工作"。**最高优先级的非 bug 改进就是补自动装配的集成测试。**
2. **"配置项即摆设"泛滥**：大量 `@ConfigurationProperties` 字段定义了却无消费方。建议做一次全量清理：能实现的接线，不实现的删除，避免持续误导。
3. **实现类残留 stereotype 注解**：task、search、ai 等模块的实现类仍带 `@Service/@Component`，被业务应用组件扫描时绕过所有 `@Conditional` 防护，复现双注册/脏地址问题。v2.0.1 已在部分模块清理，需全量排查。
4. **默认值不安全**：Nacos `nacos/nacos`、xxl-job `xxl-job` token、WebSocket `allowedOrigins={"*"}`、web `allowCredentials=true`、jwtSecret 默认弱值——安全默认值应统一收紧为"默认拒绝/默认关闭"。
5. **两套并行实现未收敛**：xxl-job 有新旧两套接入、CORS 有两套配置（悬空 Bean）、认证有 web 与 security 两套互不打通的体系。应各自裁撤为一套。

---

## 7. 建议的修复路线（供排期参考）

- **第一批（阻断上生产，1-2 周）**：第 2 节全部 P0。重点是三个机制性事故（1.1/1.2/1.3）+ 安全绕过五项 + 锁单位 bug + ServiceImpl 假实现 + 缓存 KEYS *。
- **第二批（可靠性，2-3 周）**：P1 中的拦截器顺序、XFF、双 MQ 冲突、读写分离、多级缓存失效、RocketMQ 停机、Netty 握手。
- **第三批（治理，持续）**：补自动装配集成测试、清理死配置、移除残留 stereotype、收紧默认值、收敛并行实现、同步 CLAUDE.md 中已过期的行号引用。
- **删除项（零风险清理）**：根目录 `nebula-example/` 空壳目录（源码已迁至 `examples/`，git 零跟踪）。

---

## 8. 与升级 Spring Boot 4 的关系

本审查发现的多个问题与升级强相关，应"修复"与"升级"合并规划（详见 `docs/nebula-springboot4-upgrade-design.md`）：

- `--enable-preview`、spring-rabbit 3.1.0 错配、EPP/ImportFilter 注册方式——升级前必须先处理。
- net.devh gRPC starter 停更、Jackson 3 迁移、ES Rest5Client、Gateway 坐标改名——正好借升级窗口一并重构。
- 修复 1.1 的 EPP 注册时，直接迁到 SB4 兼容的位置，避免返工两次。

---

## 附录 A：外部审查意见（GPT）逐条核对（2026-07-05）

收到一份 GPT 对本报告的复核意见，全部基于源码逐条核实。结论：GPT 提出的两处"要求撤回"的意见经字节码/源码验证均不成立，本报告结论保持；一处措辞按更精确的代码事实做了增强。

| GPT 意见 | 代码事实核对 | 处置 |
|---------|-------------|------|
| 1. `NebulaAutoConfigurationImportFilter` 是通过 `.imports` 注册的，Spring Boot 3 支持这种方式，"死代码"结论应撤回 | **GPT 错，本报告对。** 反编译 spring-boot-autoconfigure-3.5.8.jar：`AutoConfigurationImportSelector.getAutoConfigurationImportFilters()` 加载 `AutoConfigurationImportFilter` 后走 `SpringFactoriesLoader.loadFactories`（只读 spring.factories）；`.imports` 机制的 `ImportCandidates.load` 全框架仅被 `AutoConfigurationImportSelector` 以 `AutoConfiguration.class` 调用。Spring Boot 3 **不**支持用 `.imports` 注册 ImportFilter。 | 保留原结论，已在 1.1 补字节码证据 |
| 2. `NacosServiceAutoRegistrar` 只在 Web 环境注册这条成立，但措辞应从"完全不注册"改为"非 Web（纯 gRPC）不触发" | 部分成立，但 GPT 的建议措辞本身不够准确。源码 `:54` 判的是 `ServletWebServerInitializedEvent`（Servlet 专属），因此 **Reactive Web（网关）也一并被排除**，不只是纯 gRPC。 | 采纳"精确化"方向，但按代码事实改为"Servlet 之外（含 Reactive 与非 Web）均不触发"，比 GPT 建议更准 |
| 3. RPC 泛型解析"全靠 Jackson 猜"不完全对，建议写清单层/深层嵌套的差异 | 本报告原文已是平衡表述（RDG 评价称"JavaType 双端转换是亮点"），未采信"全靠猜"的说法。 | 无需改；本就未过度解读 |
| 4. 网关限流 KeyResolver 用 `getRemoteAddress()`，未解析 XFF，"XFF 可伪造"应撤回 | **GPT 错，本报告对。** `RateLimitKeyResolverConfig.java:56` 明确 `getHeaders().getFirst("X-Forwarded-For")`，`getRemoteAddress()` 仅作兜底（:68）；`LoggingGlobalFilter.getClientIp:95` 同样优先取 XFF。 | 保留 RDG-15 |
| 5. 网关 CORS 默认是空 allowedOrigins（需显式配置），"CORS 默认不安全"应撤回 | 无冲突。本报告对**网关** CORS 的评价本就是"默认值安全"。GPT 撤回的对象不是本报告的结论。注意：`allowCredentials=true` 的高危默认值出现在**另一个模块** `nebula-web`（CWT-20），与网关无关，不可混为一谈。 | 无需改；提示勿混淆两个模块 |
| 6. `LoggingGlobalFilter` 未缓存 body，"读 body 导致内存问题"应撤回 | 无冲突。本报告未主张网关读 body（RDG 评价明确"不读 body 无内存隐患"）。RDG-15 针对的是该过滤器 `getClientIp` 的 XFF 问题（已于第 4 条验证成立）。 | 无需改 |
| 7. `@RemoteService/@RpcClient` 双注解并存是过渡期兼容设计，不是 bug | 一致。本报告未将"双注解并存"判为 bug，而是判为"基本落地"并指出**两个具体兼容缺口**（均已源码核实）：`ServiceDiscoveryRpcClient.getServiceName():337` 只读 `@RpcClient`；`RpcClientScannerRegistrar.registerSpecifiedClients():175` 只校验 `@RpcClient`（扫描器 :144-145 虽含双注解，但显式 `clients=` 指定的 `@RemoteService` 会在 :175 被跳过）。 | 无需改；两缺口成立 |

**总体判断**：GPT 的复核中，两条明确的"撤回"要求（第 1、4 条）与代码事实相反——恰是 GPT 判断有误、本报告正确；第 2 条方向对但本报告给出的措辞更精确。这轮核对反而进一步印证了本报告"所有高危结论均附一手证据"的可靠性。也说明外部 AI 审查意见必须回到源码验证，不能直接采信。
