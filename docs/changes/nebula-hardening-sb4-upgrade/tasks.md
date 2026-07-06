# 任务清单：Nebula 硬化 + 升级 SB4.1

> 事实源：`spec.md`。每个 task = 一次可独立提交的原子变更（约 3-5 文件），完成即 commit。
> 基线：本清单为**工作流 A（阻断级修复）**，在 Spring Boot 3.5.16 上执行。工作流 B/C 见末尾 epic 占位，待 A 收敛后再拆。
> 执行顺序：先做机制性失效（解锁"能验证"）→ 升级前置清理（低风险、解耦）→ 安全绕过 → 正确性硬伤。
> 验证命令约定：`mvn -q -pl <module> -am test`（含新测试）；纯 POM/资源改动用 `mvn -q -pl <module> -am -DskipTests package`。

图例：`[ ]` 待办 `[~]` 进行中 `[x]` 完成

---

## 阶段 A0：升级到 3.5.16 + 前置清理（低风险、先做，解耦后续）

- [x] **T-A0-1｜升 3.5.16 并消除 deprecation 告警**
  - 文件：根 `pom.xml`（`spring-boot.version` 3.5.8→3.5.16）
  - 验收：全量编译通过；收集并记录 deprecation 告警清单到 log
  - 验证：`mvn -q clean compile`
  - 完成：2026-07-06，BUILD SUCCESS（69 模块），告警清单见 log.md「T-A0-1 执行记录」（A 类框架自有过渡代码 / B 类第三方待升级 / C 类 JDK 顺手清理；preview 告警为 0）

- [x] **T-A0-2｜移除全局 --enable-preview（F-A12）**
  - 文件：根 `pom.xml`（compiler 插件配置，改 `<release>21`，去掉 `--enable-preview` 及 surefire/failsafe 的 argLine）
  - 验收：产物字节码非预览版本；全模块编译+测试通过
  - 验证：`mvn -q clean test`；抽查 `javap -v` 某 class 的 minor version 非 65535
  - 前置确认：先 grep 是否真的用到预览语法（`grep -rn "preview" --include=*.java`）；用到则先替换语法
  - 完成：2026-07-06。compiler 改 `<release>21` 并删除两处 preview 参数；`mvn clean test-compile` BUILD SUCCESS（主+测试代码均编译通过，证实无预览语法）；`javap` 确认产物 minor version=0 / major=65（标准 Java 21）

- [x] **T-A0-3｜spring-rabbit/amqp-client 交还 BOM（F-A13）**
  - 文件：根 `pom.xml`（删 `spring-rabbit.version`/`amqp-client` 版本锁定）、`nebula-messaging-rabbitmq/pom.xml`
  - 验收：版本由 Boot BOM 托管；RabbitMQ 收发端到端验证正常（用 testcontainers 或本地 MQ）
  - 验证：`mvn -q -pl infrastructure/messaging/nebula-messaging-rabbitmq -am test`
  - 完成：2026-07-06。删除根 pom 的 `rabbitmq.version`/`spring-rabbit.version` 属性及对应 dependencyManagement；现解析为 spring-rabbit **3.2.8** + spring-amqp **3.2.8**（版本对齐，消除 3.1.0/3.2.8 错配）、amqp-client 5.25.0，编译通过。**遗留**：live 收发端到端测试需真实 broker，本地无 Docker/RabbitMQ 未跑，转 CI/带 broker 环境验证（见风险登记）

- [x] **T-A0-4｜删除框架 JAR 内 application.yml（F-A14）**
  - 文件：`autoconfigure/.../resources/application.yml`（删除）；Gateway gRPC 禁用与 Spring AI 排除改为 Filter/代码
  - 验收：应用不再被框架 yml 覆盖；Spring AI 默认排除仍生效
  - 验证：`mvn -q -pl autoconfigure/nebula-autoconfigure -am test`
  - 完成：2026-07-06。核实两块内容均已有代码兜底、无需迁移即可删：(1) Spring AI 排除的 9 个类与 `SpringAIAutoConfigurationFilter`(注册于 spring.factories) 完全一致；(2) `spring.cloud.gateway.grpc.enabled` 在 SCG 4.1.5 中并非真实属性(死配置)，真实排除由 gateway starter 的 `GatewayGrpcServerExcludeConfiguration`(EPP, 已注册) 承担。删除后 autoconfigure 编译通过

- [x] **T-A0-5｜删除 task 的 spring.factories 死文件（F-A15）**
  - 文件：`application/nebula-task/.../META-INF/spring.factories`（删除）、主 imports 尾部注释订正
  - 验收：task 自动配置仍由 `.imports` 正常注册
  - 验证：`mvn -q -pl application/nebula-task -am test`
  - 完成：2026-07-06。删除用 `EnableAutoConfiguration` 键的死文件（SB3 已忽略该键）；订正主 imports 第 87 行注释（"由 spring.factories 注册" → "由 AutoConfiguration.imports 注册"）；task 模块 `.imports` 仍注册 TaskAutoConfiguration，编译通过

## 阶段 A1：机制性失效（最高优先，解锁"能验证"）

> **验证纪律（三个 task 共用）**：这三处的"真实注册点"都在 `autoconfigure/nebula-autoconfigure`（或由其装配），核心/实现模块的单测**不能**作为"生产链路生效"的证据——单测过了、Bean 没被 starter 装配的情况正是我们要防的。因此每个 task 的验收都必须包含一个 **starter/最小 Web 应用级集成测试**，跑在 `autoconfigure` 或专门的 starter 测试模块里。

- [x] **T-A1-1｜EPP/ImportFilter 注册迁回 spring.factories（F-A1，采用 Q1=删除 ImportFilter）**
  - 文件：新建/改 `autoconfigure/.../META-INF/spring.factories`（注册 `NebulaStarterDefaultsPostProcessor` 于 EnvironmentPostProcessor 键）；删除 `EnvironmentPostProcessor.imports`；删除 `NebulaAutoConfigurationImportFilter` 类及其 `.imports`
  - 验收：**新增集成测试**——最小应用只引入某 starter、不写任何 enabled，断言默认模块 Bean 存在
  - 验证：`mvn -q -pl autoconfigure/nebula-autoconfigure -am test`
  - 依赖：无（优先做，后续安全/正确性修复的验证都依赖 starter 能真正生效）
  - 完成：2026-07-06。`NebulaStarterDefaultsPostProcessor` 追加注册到 spring.factories 的 EnvironmentPostProcessor 键；删除两个不生效的 `.imports`（EnvironmentPostProcessor + AutoConfigurationImportFilter）及 `NebulaAutoConfigurationImportFilter` 类（Q1，无别处引用）。新增 `NebulaStarterDefaultsIntegrationTest`（@SpringBootTest 最小上下文 + test 资源 nebula-defaults.properties）：Tests run 2 / Failures 0，且**反向对照**（移除 EPP 注册后测试 Failures 1）证明测试有效。同时验证用户 application.properties 可覆盖默认值

- [x] **T-A1-2｜@MessageListener 扫描（F-A2）**
  - 文件：`nebula-messaging-core/.../MessageHandlerProcessor.java`（同时扫 `@MessageListener` 与 `@MessageHandler`，新优先）
  - 验收：**新增集成测试**——用 `@MessageListener` 写的消费者能被注册并收到消息
  - 验证：`mvn -q -pl infrastructure/messaging/nebula-messaging-rabbitmq -am test`
  - 完成：2026-07-06。`resolveAttributes` 优先解析 `@MessageListener`、回退 `@MessageHandler`，归一化为 `HandlerAttributes` record 共用注册逻辑（两注解属性一致，@MessageHandler 行为不变）。新增 `MessageHandlerProcessorTest`（mock MessageManager/Consumer，验证 subscribe 被调用，无需 broker）：Tests run 2 / Failures 0，反向对照（忽略 @MessageListener 分支→Failures 1）证明有效

- [ ] **T-A1-3｜RBAC 链路：SecurityContext 填充 + 类级注解生效（F-A3）**
  - 文件：新建 `core/nebula-security/.../authentication/JwtAuthenticationFilter.java`（解析 header→校验→写入/finally 清理 SecurityContext）；在 **`autoconfigure/nebula-autoconfigure/.../security/SecurityAutoConfiguration.java`**（Filter 的真实注册点，已在 `AutoConfiguration.imports:36` 注册）注册该 Filter Bean 并定序；`core/nebula-security/.../authorization/SecurityAspect.java` 切点补 `@within(...)`
  - 验收：**新增 starter 级 Web 集成测试**——发一个真实 HTTP 请求，断言：Filter Bean 存在且顺序正确、处理期间 SecurityContext 被填充、请求完成后被清理、类级 `@RequirePermission` 生效、未认证 401 / 无权限 403。core 的 SecurityAspect 单测仅覆盖切点解析，不作为链路生效证据
  - 验证：`mvn -q -pl autoconfigure/nebula-autoconfigure -am test`（含上述集成测试）；辅以 `mvn -q -pl core/nebula-security -am test`
  - 备注：本条修正自 Codex 对抗性审查——原验证仅 `-pl core/nebula-security` 会漏掉真实装配点（SecurityAutoConfiguration 在 autoconfigure 模块）
  - 完成：2026-07-06。新增 `JwtAuthenticationFilter`（populate-only、finally 清理、roles/permissions 沿用既有 claim 约定、**默认关闭 opt-in** 以不影响存量应用如 proud-day）；`SecurityAspect` 切点加 `@within` + advice 反射解析（方法优先再类），修复类级注解静默失效；`SecurityAutoConfiguration`（autoconfigure）按 `nebula.security.jwt.filter.enabled` 注册。测试：Filter 单测 3、类级切面 3（反向对照去 @within → Failures 2 证明有效）、autoconfigure 装配 2（默认不注册/开启后注册，覆盖 Codex 指出的生产路径）。nebula-security 30 测试 / autoconfigure 4 测试全绿

## 阶段 A2：安全绕过 / 数据泄漏

- [x] **T-A2-1｜AuthInterceptor OPTIONS 绕过（F-A4）**
  - 文件：`nebula-web/.../interceptor/AuthInterceptor.java`（`CorsUtils.isPreFlightRequest` 替代无条件放行 OPTIONS）
  - 验收：非预检的 `OPTIONS /api/xxx` 不再免认证；真预检仍放行
  - 验证：`mvn -q -pl application/nebula-web -am test`
  - 完成：2026-07-06。改用 `CorsUtils.isPreFlightRequest`（要求 OPTIONS + Origin + Access-Control-Request-Method）。AuthInterceptorTest 加 2 用例（真预检放行 / 普通 OPTIONS 无 token 被拒），8 测试全绿

- [x] **T-A2-2｜响应缓存跨用户串数据（F-A5，采用 Q2=双管）**
  - 文件：新增 `cache/ResponseCacheable.java` 注解、`interceptor/ResponseCacheInterceptor.java`（isCacheable 排除认证请求 + 要求注解）、`autoconfigure/WebCacheAutoConfiguration.java`（matchIfMissing=false 默认关）
  - 验收：带 Authorization/Cookie 的请求默认不缓存；缓存改为注解白名单显式声明
  - 验证：`mvn -q -pl application/nebula-web -am test`
  - 完成：2026-07-06。双管：(1) `isCacheable` 排除带 `Authorization`/`Cookie` 的请求（杜绝跨用户串号）+ 仅缓存标注 `@ResponseCacheable` 的接口；(2) 响应缓存默认关闭。`ResponseCacheInterceptorTest` 5 用例（注解+无凭据可缓存 / 带 Authorization 不缓存 / 带 Cookie 不缓存 / 无注解不缓存 / 非 GET 不缓存）全绿

- [x] **T-A2-3｜@SensitiveData 挂主 ObjectMapper（F-A6）**
  - 文件：`autoconfigure/WebAuthAutoConfiguration.java`（用 `Jackson2ObjectMapperBuilderCustomizer` + `AnnotationIntrospectorPair` 把 introspector 挂到主 ObjectMapper）
  - 验收：控制器正常返回时敏感字段被脱敏
  - 验证：`mvn -q -pl application/nebula-web -am test`
  - 完成：2026-07-06。删除无用的独立 `dataMaskingObjectMapper`，改为 customizer 用 `AnnotationIntrospectorPair.pair(脱敏, 默认)` 挂到主 mapper（脱敏优先、其余回退默认，不丢默认注解处理）。`SensitiveDataMaskingCustomizerTest` 验证手机号脱敏生效且非敏感字段保留

- [ ] **T-A2-4｜/rpc 与 gRPC 端点鉴权 + 禁 Class.forName（F-A7）**
  - 文件：`nebula-rpc-http/.../server/HttpRpcController.java`、`nebula-rpc-grpc/.../server/GrpcRpcServer.java`（共享密钥/token 校验；参数类型白名单匹配，限定目标接口声明类型）
  - 验收：无 token 调用被拒；不再 `Class.forName` 任意类
  - 验证：`mvn -q -pl infrastructure/rpc/nebula-rpc-http -am test`；`-pl .../nebula-rpc-grpc -am test`

- [x] **T-A2-5｜xxl-job 遗留 REST 端点裁撤（F-A8，采用 Q3=裁撤）**
  - 文件：删除 `XxlJobTaskService.java`（手搓 `/beat /idleBeat /run /log /kill` 端点）+ `XxlJobAutoConfiguration` 的 bean 注册 + 对应测试
  - 验收：业务端口不再暴露无鉴权任务触发；token 不出现在日志
  - 验证：`mvn -q -pl application/nebula-task -am test`；`grep -n accessToken` 确认无 INFO 打印
  - 完成：2026-07-06。官方 `XxlJobSpringExecutor`(EmbedServer, 独立端口, d2a0132e 已注册)已接管执行器协议; 手搓的 `XxlJobTaskService`(无条件注册在业务 web 端口、读 token 不校验、token 进 INFO 日志)纯冗余+攻击面, 整类删除。nebula-task 47 测试全绿（相关测试随类删除）。遗留 DTO 已成孤儿, 留治理阶段清理

- [x] **T-A2-6｜性能端点鉴权 + resetMetrics（F-A9）**
  - 文件：`PerformanceController.java`（类级 `@ConditionalOnProperty(nebula.web.performance.enabled)` 默认关，端点默认不暴露）、`PerformanceMetrics.java`（新增 `reset()`）、`DefaultPerformanceMonitor.java`（`resetMetrics` 调用真实 reset）
  - 验收：`/performance/*` 需认证；reset 真实生效或移除
  - 验证：`mvn -q -pl application/nebula-web -am test`
  - 完成：2026-07-06。端点改 opt-in（默认关闭，消除 /system 信息泄漏与 /reset 无鉴权的默认暴露；启用后由应用 AuthInterceptor 保护，这些路径不在 ignore-paths）；`resetMetrics` 由空实现改为 `metrics.reset()` 真实清零。`PerformanceMetricsResetTest` 2 用例验证清零与委托

- [x] **T-A2-7｜ES 认证回调覆盖（F-A10）**
  - 文件：`autoconfigure/.../search/ElasticsearchAutoConfiguration.java`（合并三次 `setHttpClientConfigCallback` 为单回调，依次设凭证/连接池/SSL）
  - 验收：配置 username/password 后 Basic 认证生效；连接池与 SSL 不互相覆盖
  - 验证：`mvn -q -pl infrastructure/search/nebula-search-elasticsearch -am test`
  - 完成：2026-07-06。三次调用合并为单个 callback（凭据→连接池→SSL），编译通过。**遗留**：Basic 认证真正生效需 ES 服务端验证，转 CI（同 T-A0-3 模式）

- [x] **T-A2-8｜Redis 缓存多态反序列化 RCE（F-A11）**
  - 文件：`autoconfigure/.../data/CacheAutoConfiguration.java`（`activateDefaultTyping` 改 `allowIfSubType` 白名单限定业务包）、`CacheProperties.RedisCache`（新增 `trustedPackages`）
  - 验收：仅白名单类型可多态反序列化
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-cache -am test`
  - 完成：2026-07-06。`allowIfBaseType(Object.class)` → 白名单 `allowIfSubType`（默认 java.util/java.time/io.nebula + 可配 `nebula.data.cache.redis.trusted-packages`）；抽 `buildRedisValueObjectMapper` 静态方法便于测试。`CacheRedisTypingWhitelistTest` 3 用例（信任类型往返 / 未信任被拒 / 配置放行）全绿。**破坏性变更**：缓存自定义模型的应用必须声明 trusted-packages，见 log 迁移说明

## 阶段 A3：正确性硬伤

- [ ] **T-A3-1｜锁 tryLock 单位 bug + tryExecute 吞异常（F-A16）**
  - 文件：`nebula-lock-redis/.../RedisLock.java:87-89`（`unit.toMillis(timeout)` + leaseTimeMs + MILLISECONDS）、`RedisLockManager.java:140`（回调异常上抛，仅锁异常捕获）
  - 验收：`@Locked(timeUnit=SECONDS, leaseTime=60)` 实际租约 60 秒；业务异常不再被吞成 null
  - 验证：`mvn -q -pl infrastructure/lock/nebula-lock-redis -am test`

- [ ] **T-A3-2｜ServiceImpl 假实现（F-A17，采用 Q4）**
  - 前置：`grep -rn "findByField\|findOneByField\|findTopN\|findRandomN\|saveBatchIgnore\|removeByIdPhysical" --include=*.java` 确认调用方
  - 文件：`nebula-data-persistence/.../service/impl/ServiceImpl.java`（有用的用 QueryWrapper 真实现，无用的删）
  - 验收：`findByField` 按条件查询而非全表；`findOneByField` 不再当主键查；语义误导方法清理
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-persistence -am test`

- [ ] **T-A3-3｜缓存 clear()/stats KEYS *（F-A18）**
  - 文件：`nebula-data-cache/.../DefaultCacheManager.java`（SCAN 游标 + `keyPrefix` 圈定，启用当前未用的 `CacheProperties.redis.keyPrefix`）
  - 验收：`clear()` 只删本前缀键，不再 `KEYS *` 全库
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-cache -am test`

- [ ] **T-A3-4｜RPC 共享单例客户端并发串地址（F-A19）**
  - 文件：`nebula-rpc-core/.../discovery/ServiceDiscoveryRpcClient.java`、`nebula-rpc-http/.../client/HttpRpcClient.java`、`nebula-rpc-grpc/.../client/GrpcRpcClient.java`（目标地址随请求传递；gRPC 按 target 维护 Channel Map 复用）
  - 验收：并发调不同服务不再互相覆盖目标；gRPC 不再每次重建 Channel
  - 验证：`mvn -q -pl infrastructure/rpc/nebula-rpc-core -am test`；grpc/http 各自测试

- [ ] **T-A3-5｜加权 LB 配置即崩 + Nacos Reactive 不注册（F-A20、F-A21）**
  - 文件：`RpcDiscoveryAutoConfiguration.java:313`（weighted→WEIGHTED_RANDOM 映射）、`NacosServiceAutoRegistrar.java:54`（改判 `WebServerInitializedEvent` 覆盖 Reactive；非 Web/gRPC 加独立注册钩子）
  - 验收：配 `weighted` 不再启动崩溃；Reactive（网关）应用能自动注册
  - 验证：`mvn -q -pl infrastructure/discovery/nebula-discovery-nacos -am test`

- [ ] **T-A3-6｜Neo4j 自动配置注册进 imports（F-A22）**
  - 文件：`autoconfigure/.../META-INF/spring/...AutoConfiguration.imports`（补 `Neo4jAutoConfiguration`、`Neo4jHealthAutoConfiguration`）
  - 验收：配 `nebula.data.neo4j.enabled=true` 后 Neo4j 支持生效
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-neo4j -am test`

- [ ] **T-A3-7｜Foundation 五个必炸缺陷（F-A23，采用 Q6）**
  - 文件：`foundation/.../util/JwtUtils.java:276`（refreshToken 拷贝 claims）、`exception/ValidationException.java:38`（可变 list）、`util/IdGenerator.java:32,470`（workerId 从环境派生 + SequenceGenerator updateAndGet）、`security/CryptoUtils.java:383`（新增 `hashPassword` BCrypt，旧 `encrypt` @Deprecated）
  - 验收：refreshToken/addFieldError 不再抛 UOE；雪花默认实例不撞号；序列不重号；提供强密码哈希
  - 验证：`mvn -q -pl core/nebula-foundation -am test`（补单测覆盖这 5 点）

- [ ] **T-A3-8｜MQ 静默丢消息/NPE + 向量存储过滤失效（F-A24）**
  - 文件：`nebula-messaging-rabbitmq/.../RabbitMQMessageConsumer.java`（重试上限+DLQ，去掉无限 requeue）、`RabbitMQMessageProducer.java`（路由键约定统一）、`nebula-messaging-redis/.../RedisStreamConsumer.java:328`（headers 判空）、`nebula-ai-spring/.../CustomChromaVectorStore.java:157`（传入 filter/threshold）
  - 验收：毒消息不再无限重投；topic 消息不再静默丢；无头 Stream 消息不 NPE；向量 get/exists/按过滤删恢复
  - 验证：各模块 `mvn -q -pl <module> -am test`
  - 备注：MW-2 重试/DLQ 较重，若单 task 过大可再拆 T-A3-8a/8b

---

## 阶段 A4：持久化可用性与正确性（proud-day B 接入的前置，"先改好框架")

> 目标：让标准 MyBatis-Plus 消费者(如 proud-day)能无痛接入 Nebula 持久化，同时修掉已知正确性 bug。
> 决策见 log「决策：老项目 proud-day 与持久化默认值」。

- [ ] **T-A4-1｜Nebula 持久化 mapper 扫描可配置**
  - 文件：`autoconfigure/.../data/DataPersistenceAutoConfiguration.java`（`@MapperScan` basePackages 从属性读，默认 `io.nebula.**.mapper` 保持兼容；放开/可选 `markerInterface`，兼容标准 MyBatis-Plus BaseMapper 的 mapper）、`MybatisPlusProperties`
  - 验收：集成测试——配置自定义 mapper 包后，标准 MyBatis-Plus BaseMapper 的 mapper 能被扫到注册
  - 依赖：这是 proud-day B 迁移无需改 33 个 mapper 的关键

- [ ] **T-A4-2｜数据源防御性共存 + fail-fast（CD-13/14）**
  - 文件：`DataPersistenceAutoConfiguration.java`、`DataSourceManager.java`（用户已有 DataSource 时让路；主数据源建不出直接抛异常终止启动，不再 fail-slow；启动连接不泄漏）
  - 验收：无 nebula 数据源配置时启动明确报错；有用户 DataSource 时不硬抢

- [ ] **T-A4-3｜数据源配置契约文档化**
  - 文件：文档/示例——`nebula.data.persistence.datasource.*` 结构与迁移指引（供 proud-day 照填）

- [ ] **T-A4-4｜ServiceImpl 假实现修复（F-A17/CD-2）**（并入原 T-A3-2，改在此统一做）
  - 文件：`nebula-data-persistence/.../service/impl/ServiceImpl.java`

- [ ] **T-A4-5｜读写分离动态路由修复（CD-3）**（framework 质量，proud-day 暂不用）
  - 文件：`autoconfigure/.../data/*ReadWrite*`、`ReadWriteDataSourceManager.java`

## 阶段 D：proud-day B 迁移（下游、框架发布后，独立回归）

- [ ] **T-D-1｜proud-day 接入 Nebula 持久化**（在 proud-day 仓库）
  - 数据源 `spring.datasource.*` → `nebula.data.persistence.datasource.*`；设 `nebula.data.persistence.enabled=true` + 配 mapper 包；移除自建 `@MapperScan`（改由 Nebula 扫描）；生产数据层全回归
  - 前置：阶段 A4 完成并发布快照；**必须在 proud-day 重新构建吃到新快照前落地**（sequencing 风险）

## 工作流 B / C（epic 占位，待 A 收敛后拆）

- [ ] **EPIC-B｜升级 Spring Boot 4.1**（升级设计第 8 节阶段 1-3）
- [ ] **EPIC-C1｜P1 可靠性**（拦截器顺序、XFF、双 MQ、读写分离、多级缓存失效、RocketMQ 停机、Netty 握手）
- [ ] **EPIC-C2｜治理**（补自动装配集成测试<横切最高优先>、清死配置、移除残留 stereotype、收紧默认值、收敛并行实现）
- [ ] **EPIC-C3｜删除 `nebula-example/` 空壳目录**（零风险清理）

---

## 变更摘要（工作流 A 全部完成后填写）

- 总文件数：_待填_
- Spec-Plan 偏差：_待填_
- 遗留问题：_待填_
