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

- [x] **T-A1-3｜RBAC 链路：SecurityContext 填充 + 类级注解生效（F-A3）**
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

- [x] **T-A2-4a｜禁 Class.forName 任意类加载（F-A7 之一）**
  - 文件：`nebula-rpc-core/.../message/RpcRequest.java`（`parameterTypes` `Class<?>[]`→`String[]`）、`HttpRpcClient.java`（构建请求时转类型名）、`HttpRpcController.java`（findMethod 按类型名字符串匹配，不 Class.forName）
  - 验收：不再 `Class.forName` 任意类；合法调用仍正确解析
  - 完成：2026-07-06。`parameterTypes` 改 `String[]`（线格式不变，Jackson 本就把 Class 序列化为类名字符串），服务端仅按名字比对已声明方法的参数类型名。`HttpRpcControllerFindMethodTest` 3 用例（类型名匹配 / 运行时具体类型兜底 / 恶意类名不加载类且仍解析）全绿；rpc-http 5 测试全绿
  - 补漏：2026-07-06（hardening-b 审查发现）。gRPC 侧 `GrpcRpcServer.parseParameterTypes` 仍对请求类名逐一 `Class.forName`（F-A7 同源遗漏，原完成记录只覆盖 HTTP 侧）。已改为与 HTTP 侧一致的类型名字符串匹配：删除 `parseParameterTypes`/`isCompatible`/`parseParameters`，`findMethod` 按"名称+数量+声明参数类型名"精确匹配、失败则"名称+数量"兜底（覆盖客户端发运行时具体类型 ArrayList/Integer 对 List/int 声明、null 参数占位 java.lang.Object）。`GrpcRpcServerFindMethodTest` 6 用例（含恶意类名不触发类加载）全绿；gRPC 模块 51 测试全绿
- [x] **T-A2-4b｜/rpc 与 gRPC 端点可选鉴权（F-A7 之二，用户已同意：共享 token 默认关）**
  - 文件：`HttpRpcProperties.java`（`server.authToken`）、`HttpRpcController.java`（请求头 `X-Nebula-Rpc-Token` 校验）、`HttpRpcAutoConfiguration.java`（注入 token）
  - 验收：配置 token 后无 token/错 token 调用被拒（401）；未配置保持开放（默认关，不影响纯内网 RPC）
  - 完成：2026-07-06。HTTP `/rpc`：新增 `nebula.rpc.http.server.auth-token`(默认空=不鉴权)，配置后请求须带 `X-Nebula-Rpc-Token` 头，否则 401。`HttpRpcControllerAuthTest` 4 用例(缺/错 token 401、对 token 放行、无 token 配置放行)全绿。**gRPC token 归入工作流 B**：gRPC 服务器是 net.devh `@GrpcService`(将在 B 迁移到 spring-grpc)，现在写 net.devh 专用拦截器属注定丢弃的返工，故随 spring-grpc 迁移一起在目标框架上做。注：真正的安全漏洞(Class.forName 任意类加载)已在 T-A2-4a 修复；gRPC EmbedServer 在独立端口(非业务 web 端口)

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

- [x] **T-A3-1｜锁 tryLock 单位 bug + tryExecute 吞异常（F-A16）**
  - 文件：`nebula-lock-redis/.../RedisLock.java:89`（`unit.toMillis(timeout)` + leaseTimeMs + MILLISECONDS）、`RedisLockManager.java`（回调异常上抛，仅锁异常捕获）
  - 验收：`@Locked(timeUnit=SECONDS, leaseTime=60)` 实际租约 60 秒；业务异常不再被吞成 null
  - 验证：`mvn -q -pl infrastructure/lock/nebula-lock-redis -am test`
  - 完成：2026-07-06。tryLock 统一毫秒 `tryLock(unit.toMillis(timeout), leaseTimeMs, MILLISECONDS)`；tryExecute 只吞获取锁阶段异常、业务回调 RuntimeException 原样上抛、checked 包 LockException。**注意**：旧测试 `testTryLockWithTimeoutWithoutWatchdogUsesLeaseTime` 竟断言 buggy 行为 `(5,30000,SECONDS)`——已订正为 `(5000,30000,MILLISECONDS)`（这正是 bug 长期潜伏的原因）；新增 tryExecute 异常传播测试。lock 模块 46 测试全绿

- [x] **T-A3-2｜ServiceImpl 假实现（F-A17，采用 Q4）**（已并入 T-A4-4 完成，见阶段 A4）
  - 前置：`grep -rn "findByField\|findOneByField\|findTopN\|findRandomN\|saveBatchIgnore\|removeByIdPhysical" --include=*.java` 确认调用方
  - 文件：`nebula-data-persistence/.../service/impl/ServiceImpl.java`（有用的用 QueryWrapper 真实现，无用的删）
  - 验收：`findByField` 按条件查询而非全表；`findOneByField` 不再当主键查；语义误导方法清理
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-persistence -am test`
  - 完成：2026-07-06，实现与验证记录见 T-A4-4（唯一实现处，此处仅状态同步）

- [x] **T-A3-3｜缓存 clear()/stats KEYS *（F-A18）**
  - 文件：`nebula-data-cache/.../DefaultCacheManager.java`（引入 `keyPrefix` 命名空间 + 全部 key 操作加前缀 + SCAN 游标）、`autoconfigure/.../CacheAutoConfiguration.java`（注入配置的 keyPrefix）
  - 验收：`clear()` 只删本前缀键，不再 `KEYS *` 全库
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-cache -am test`
  - 完成：2026-07-06。启用此前完全未用的 `CacheProperties.redis.keyPrefix`(默认 `nebula:cache:`)：全部 41 处 key 操作(string/hash/list/set/zset)统一加前缀；`clear()`/`getSize()`/`keys()`/`scan()` 改用 SCAN 游标并按前缀圈定，`clear()` 绝不 `KEYS "*"`，无前缀时拒绝执行。`DefaultCacheManagerKeyPrefixTest` 3 用例；cache 模块 11 测试全绿。**破坏性**：缓存 key 现带前缀存储，旧的无前缀缓存数据将 miss(会自动重建)，见 log 迁移说明

- [x] **T-A3-4｜RPC 共享单例客户端并发串地址（F-A19）**
  - 文件：`ServiceDiscoveryRpcClient.java`（目标地址随请求传递，不改写共享 delegate；`ConfigurableRpcClient` 加默认 `callWithTarget`）、`HttpRpcClient.java`（per-thread ThreadLocal 目标覆盖，无锁）
  - 验收：并发调不同服务不再互相覆盖目标
  - 验证：`mvn -q -pl infrastructure/rpc/nebula-rpc-core -am test`；grpc/http 各自测试
  - 完成：2026-07-06。`ServiceDiscoveryRpcClient.call` 改为 `configurable.callWithTarget(target, ...)`，不再 `setTargetAddress + call` 两步改共享状态；`ConfigurableRpcClient.callWithTarget` 默认实现同步 setTarget+call(gRPC 等自动获得，消除竞态)；`HttpRpcClient` 重写为 per-thread ThreadLocal 目标覆盖(无锁、无跨线程共享)。`HttpRpcClientTargetIsolationTest` 2 用例(per-request 清理 + 并发不串号)；rpc-http 7 测试全绿。**遗留(RDG-2)**：gRPC 走默认同步兜底(正确但串行)，其按 target 维护 Channel Map 复用的优化留后续

- [x] **T-A3-5｜加权 LB 配置即崩 + Nacos Reactive 不注册（F-A20、F-A21）**
  - 文件：`RpcDiscoveryAutoConfiguration.java`（`resolveStrategy` 别名映射，weighted→WEIGHTED_RANDOM）、`RpcDiscoveryProperties.java`（校验正则扩展）、`NacosServiceAutoRegistrar.java`（改判父类 `WebServerInitializedEvent` 覆盖 Servlet+Reactive）
  - 验收：配 `weighted` 不再启动崩溃；Reactive（网关）应用能自动注册
  - 验证：`mvn -q -pl infrastructure/discovery/nebula-discovery-nacos -am test`
  - 完成：2026-07-06。加权 LB：`valueOf("WEIGHTED")` 崩溃改为 `resolveStrategy` 容错映射(weighted/weighted_random→WEIGHTED_RANDOM、weighted_round_robin、未知回退 ROUND_ROBIN)，校验正则同步扩展。Nacos：`instanceof ServletWebServerInitializedEvent` 改父类 `WebServerInitializedEvent`(同覆盖 Reactive/网关)。`RpcLoadBalanceStrategyTest` 3 + `NacosServiceAutoRegistrarReactiveTest` 1；nacos 40 测试全绿

- [x] **T-A3-6｜Neo4j 自动配置注册进 imports（F-A22）**
  - 文件：`autoconfigure/.../META-INF/spring/...AutoConfiguration.imports`（补 `Neo4jAutoConfiguration`、`Neo4jHealthAutoConfiguration`）
  - 验收：配 `nebula.data.neo4j.enabled=true` 后 Neo4j 支持生效
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-neo4j -am test`
  - 完成：2026-07-06。两个带 `@AutoConfiguration` 却未注册的 Neo4j 类补入主 imports（Data Layer 段，默认关闭）。`Neo4jAutoConfigurationRegistrationTest` 用 `ImportCandidates.load` 断言两类进入候选清单

- [x] **T-A3-7｜Foundation 五个必炸缺陷（F-A23，采用 Q6）**
  - 文件：`security/JwtUtils.java`（refreshToken 拷贝 claims 到可变 Map）、`exception/ValidationException.java`（构造器防御性拷贝）、`util/IdGenerator.java`（雪花默认从环境变量/本机地址派生 + SequenceGenerator getAndUpdate 原子回绕）、`security/CryptoUtils.java`（新增 `hashPassword`/`matchesPassword` PBKDF2，旧 `encrypt` @Deprecated）
  - 验收：refreshToken/addFieldError 不再抛 UOE；雪花默认实例不撞号；序列不重号；提供强密码哈希
  - 验证：`mvn -q -pl core/nebula-foundation -am test`（补单测覆盖这 5 点）
  - 完成：2026-07-06。新增 `FoundationHardeningTest` 5 用例全绿；密码哈希用 JDK 原生 PBKDF2(免加依赖)。**注意**：`JwtUtilsTest.testRefreshToken` 与 `ExceptionFullTest.testValidationExceptionAddFieldError` 两个旧测试把 bug 当"已知问题"断言其抛 UOE，已订正为断言正确行为（本次第 2、3 处此类测试）

- [x] **T-A3-8｜MQ 静默丢消息/NPE + 向量存储过滤失效（F-A24）**
  - 文件：`RabbitMQMessageConsumer.java`（requeue 用 `isRedeliver()` 限住，去掉无限重投）、`RabbitMQMessageProducer.java`（路由键统一为 topic）、`messaging-core/Message.java`（headers 空安全 getter）
  - 验收：毒消息不再无限重投；topic 消息不再静默丢；无头 Stream 消息不 NPE
  - 验证：各模块 `mvn -q -pl <module> -am test`
  - 完成：2026-07-06。(MW-2) 消费失败改 `requeue = !isRedeliver()`——已重投过就不再入队(转 DLX 或丢弃)，堵死毒消息死循环；(MW-3) 生产者路由键从 `queue/""` 统一为 `topic`，与消费者 `queueBind(queue,topic,topic)` 匹配，不再静默丢；(MW-4) `Message.getHeaders()` 改懒初始化，任何构造/反序列化路径都不 NPE(修 Redis Stream 无头消息)。`MessageHeadersTest` 3 用例；messaging-core 5 + rabbitmq 41 测试全绿。MW-2/MW-3 的行为级验证需 broker，转 CI
  - 勘误：2026-07-06（hardening-b 审查发现）。原完成记录称"SSA-1 不适用：CustomChromaVectorStore 当前代码已不存在"——**事实错误**。该类一直存在，位于 `autoconfigure/nebula-autoconfigure/.../ai/CustomChromaVectorStore.java`（审查报告最初误标在 nebula-ai-spring，路径勘误后被误读为类已删除）。SSA-1 已在 hardening-b 修复：`similaritySearch` 经 `ChromaFilterExpressionConverter` 把 filterExpression 转为 Chroma where 子句随查询下发、按 `similarityThreshold` 过滤结果（score=1-distance，写入 `Document.score`）；`delete(Filter.Expression)` 从抛 `UnsupportedOperationException` 改为真实调用 `deleteEmbeddings(where)` 并校验状态码。`CustomChromaVectorStoreTest` 4 用例全绿，反向对照（回退主类→4 用例全错）证明有效

---

## 阶段 A4：持久化可用性与正确性（proud-day B 接入的前置，"先改好框架")

> 目标：让标准 MyBatis-Plus 消费者(如 proud-day)能无痛接入 Nebula 持久化，同时修掉已知正确性 bug。
> 决策见 log「决策：老项目 proud-day 与持久化默认值」。

- [x] **T-A4-1｜Nebula 持久化 mapper 扫描可配置**
  - 文件：`autoconfigure/.../data/DataPersistenceAutoConfiguration.java`（移除静态 `@MapperScan`，改编程式 `MapperScannerConfigurer`）
  - 验收：集成测试——配置自定义 mapper 包后，标准 MyBatis-Plus BaseMapper 的 mapper 能被扫到注册
  - 依赖：这是 proud-day B 迁移无需改 33 个 mapper 的关键
  - 完成：2026-07-06。静态 `@MapperScan(io.nebula.**.mapper, marker=nebula BaseMapper)` 换成编程式 `MapperScannerConfigurer`：basePackage 由 `nebula.data.persistence.mapper-packages`(默认 io.nebula)配置、markerInterface 改用 MyBatis-Plus 原生 BaseMapper(nebula BaseMapper 亦继承它)。`MapperScannerConfigurableTest` 验证标准 MP BaseMapper 的 mapper 在配置包内被扫描注册。proud-day 只需设该配置指向自己的包，33 个 mapper 一行不改

- [x] **T-A4-2｜数据源防御性共存 + fail-fast（CD-13/14）**
  - 文件：`DataPersistenceAutoConfiguration.java`（primaryDataSource fail-fast；persistenceSummary 连接 try-with-resources）
  - 验收：无 nebula 数据源配置时启动明确报错；有用户 DataSource 时不硬抢
  - 完成：2026-07-06。`primaryDataSource()` 由返回 null 改为抛 `IllegalStateException`(带明确原因)，避免下游报与根因脱节的错；`persistenceSummary` 诊断连接改 try-with-resources 不再泄漏。`@ConditionalOnMissingBean(DataSource.class)` 已在(用户已有 DataSource 时让路)。`DataPersistenceFailFastTest` 2 用例。注：nebula 持久化默认关闭(matchIfMissing=false)，仅显式启用时才建数据源

- [x] **T-A4-3｜数据源配置契约文档化**
  - 文件：`docs/nebula-persistence-adoption.md`
  - 完成：2026-07-06。写了持久化接入指南：最小配置(`nebula.data.persistence.sources.<name>.*` + `mapper-packages` + `primary`)、从标准 Spring Boot MyBatis-Plus 迁移的 proud-day B 步骤(迁数据源配置、去自建 @MapperScan、33 个 mapper 不改)、可选读写分离/分库分表、注意事项

- [x] **T-A4-4｜ServiceImpl 假实现修复（F-A17/CD-2）**（并入原 T-A3-2，改在此统一做）
  - 文件：`nebula-data-persistence/.../service/impl/ServiceImpl.java`
  - 完成：2026-07-06。`findByField`(原返回全表)、`findOneByField`(原把字段值当主键 selectById)、`findByFields`(原全表)、`findTopN`(原不限数量)、`findRandomN`(原全表)全部用 `QueryWrapper` 真实现(findOneByField 走条件查询取一条；findTopN 用分页限数；findRandomN 用 ORDER BY RAND() LIMIT)。零调用方但属 IService 公共 API，故真实现而非删除。`ServiceImplQueryTest` 2 用例(findByField 建条件查询/findOneByField 不当主键)。注：`saveBatchIgnore`/`removeByIdPhysical`/`removePhysical` 语义误导(非错数据)留治理阶段

- [x] **T-A4-5｜读写分离动态路由修复（CD-3）**（framework 质量，proud-day 暂不用）
  - 文件：`autoconfigure/.../data/DataPersistenceAutoConfiguration.java`（primaryDataSource 条件补排除 dynamic-routing）
  - 完成：2026-07-06。`dynamic-routing=true` 时 primaryDataSource 与 dynamicDataSource 条件都成立、primaryDataSource 先注册占了 @Primary DataSource，导致 dynamicDataSource 的 `@ConditionalOnMissingBean` 永不成立(动态路由不可达)。给 primaryDataSource 的 `@ConditionalOnExpression` 补上 `&& dynamic-routing != true`(与注释声明的"读写分离优先"一致)。行为级(多库路由)验证需读写分离环境，转 CI

## 阶段 D：proud-day B 迁移（下游、框架发布后，独立回归）

- [ ] **T-D-1｜proud-day 接入 Nebula 持久化**（在 proud-day 仓库）
  - 数据源 `spring.datasource.*` → `nebula.data.persistence.datasource.*`；设 `nebula.data.persistence.enabled=true` + 配 mapper 包；移除自建 `@MapperScan`（改由 Nebula 扫描）；生产数据层全回归
  - 前置：阶段 A4 完成并发布快照；**必须在 proud-day 重新构建吃到新快照前落地**（sequencing 风险）

## 工作流 B / C（用户拍板：B+C+D 全做；顺序 = C 前置批 → B 三阶段 → C 收尾，D 穿插）

> 排序原则（2026-07-06 用户确认）：C 里与"B 要大拆的模块"无关的修复先做（升级后不会被推翻）；
> 与重灾模块（Gateway 路由、gRPC net.devh、ES RestClient、RocketMQ 4.x 内部 API）耦合的项并入 B 阶段 2 一起做，避免返工。
> `nebula-example/` 空壳目录经核实当前工作区已不存在，原 EPIC-C3 撤销。

### 阶段 C-pre：C 前置批（在 3.5.16 上先做，升级不推翻）

- [x] **T-C1-1｜拦截器顺序显式化（CWT-18）**
  - 文件：`nebula-web/.../interceptor/InterceptorOrders.java`（新增常量）、5 个 `Web*AutoConfiguration` 注册点补 `.order(...)`
  - 顺序：Logging(100) < RateLimit(200) < Auth(300) < Cache(400) < Perf(500)——限流先于认证挡匿名洪水，缓存命中不绕过限流计数
  - 验证：`mvn -q -pl application/nebula-web -am test`
  - 完成：2026-07-06。此前 Auth=-1、Perf=0、其余默认 0（按装配顺序），限流实际排在认证之后。统一改常量并全部显式注册；间隔 100 便于应用插入自定义拦截器。`InterceptorOrdersTest` 3 用例固化顺序契约；nebula-web 75 测试全绿
- [ ] **T-C1-2｜Web 侧 XFF 可信代理解析（CWT-28）**
  - 文件：新增 `nebula-web/.../util/ClientIpResolver.java`；`RequestLoggingInterceptor`、`DefaultRateLimitKeyGenerator` 改用之；`WebProperties` 加 `trustedProxies`
  - 语义：默认不信任 XFF（直接用 remoteAddr）；仅当 remoteAddr 命中可信代理列表才解析 XFF。**破坏性**：依赖 XFF 的部署需显式配置
  - 验证：`mvn -q -pl application/nebula-web -am test`
- [ ] **T-C1-3｜双 MQ 并存冲突（MW-20）**
  - 文件：`RabbitMQAutoConfiguration` / `RocketMQAutoConfiguration`（@Primary MessageManager 按 `nebula.messaging.primary` 条件化，默认 rabbitmq）；`MessageHandlerProcessor` 注册挪到共享配置、注入 @Primary MessageManager
  - 验收：双 MQ 同时启用不再崩溃；@MessageListener 注册到 primary MQ（语义明确）
  - 验证：`mvn -q -pl infrastructure/messaging/nebula-messaging-rabbitmq,infrastructure/messaging/nebula-messaging-rocketmq -am test`
- [ ] **T-C1-4｜读写分离切面 vs 事务（CD-9）**
  - 文件：`ReadWriteDataSourceAspect`（检测目标方法/类 @Transactional：非 readOnly 拒绝切读库 + warn；readOnly=true 放行）
  - 背景：现有 isActualTransactionActive 检查对"同方法 @ReadDataSource+@Transactional"失效（切面先于事务执行）
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-persistence -am test`
- [ ] **T-C1-5｜Netty WebSocket 握手时机 + 空闲清理（MW-5）**
  - 文件：`WebSocketFrameHandler`（会话注册从 channelActive 移到 HandshakeComplete 事件；userEventTriggered 消费 IdleStateEvent 关闭空闲连接）
  - 验收：非 WebSocket 的 HTTP 请求不再产生幽灵会话；空闲连接被清理
  - 验证：`mvn -q -pl infrastructure/websocket/nebula-websocket-netty -am test`
- [ ] **T-C1-6｜RabbitMQ tag 发送路由 + tagged 消费毒消息防护**
  - 文件：`RabbitMQMessageProducer`（send 时 message.tag 非空则 routingKey=tag，匹配 tagged 绑定）、`RabbitMQMessageConsumer.subscribeWithTag`（补 requeue=!isRedeliver，与 subscribe 路径一致）
  - 验证：`mvn -q -pl infrastructure/messaging/nebula-messaging-rabbitmq -am test`
- [ ] **T-C1-7｜多级缓存跨节点失效 + 击穿/穿透防护（CD-10）**
  - 文件：`MultiLevelCacheManager`（getOrSet single-flight per-key 锁；可配置空值哨兵缓存）、新增 `CacheInvalidationBroadcaster` 接口 + Redis pub/sub 实现、`CacheAutoConfiguration` 装配（multi-level + Redis 时启用广播）
  - 验收：节点 A delete 后节点 B 的 L1 被驱逐；并发同 key miss 只回源一次；null 结果可选缓存
  - 验证：`mvn -q -pl infrastructure/data/nebula-data-cache -am test`
- [ ] **T-C1-8｜移除实现类残留 stereotype 注解**
  - 范围（已核实由 AutoConfiguration @Bean 管理或不应被组件扫描）：ReadWriteDataSourceAspect、DefaultCacheManager、RabbitMQ 一族（Producer/Consumer/ExchangeManager/Manager 等）、SpringAIEmbeddingService、MongoTemplate、DataSourceManager、ShardingConfig、ShardingSphereManager、DefaultTransactionManager、ReadWriteDataSourceManager、LockedAspect、RedisLockManager、AsyncRpcExecutionManager、TimedTaskJobHandler
  - 保留：注解定义的元注解 @Component（@MessageListener/@MessageHandler/@RpcService 设计如此）
  - 验证：`mvn -q clean compile` + 相关模块测试
- [ ] **T-C1-9｜收紧不安全默认值**
  - 文件：`NacosProperties`（username/password 默认 "nacos"→空）、`TaskProperties`（accessToken 默认 "xxl-job"→空）、`WebSocketProperties`（allowedOrigins {"*"}→{}）、`WebProperties.Cors`（allowCredentials true→false）
  - **破坏性**：依赖默认值的部署需显式配置，记 log 迁移说明
  - 验证：`mvn -q clean compile` + 相关模块测试
- [ ] **T-C1-10｜TimedTaskJobHandler 吞异常谎报成功**
  - 文件：`nebula-task/.../scheduled/TimedTaskJobHandler.java`（子任务失败计数，任何失败返回 FAIL 并列出失败任务）
  - 验证：`mvn -q -pl application/nebula-task -am test`

### 阶段 B / C 收尾 / D（C-pre 完成后拆）

- [ ] **EPIC-B｜升级 Spring Boot 4.1**（升级设计第 8 节阶段 1-3；C 中与重灾模块耦合的项——Gateway XFF/路由钉死、gRPC Channel 生命周期(RDG-2)、RocketMQ 停机钩子/事务表、gRPC token 鉴权——并入阶段 2 一起做）
- [ ] **EPIC-C2｜C 收尾（B 后）**（死配置全量清理、错误码三套体系收敛、web/security 认证两套收敛、xxl-job 孤儿 DTO 清理、补更多自动装配集成测试）
- [ ] **T-D-1｜proud-day 接入 Nebula 持久化**（见阶段 D，仓库位置待用户提供时再启动）

---

## 变更摘要（工作流 A 已全部完成，2026-07-06 收尾）

- **总规模**：相对 main 共 87 文件（+2792 / -1239），新增 29 / 删除 7 / 修改 51；32 个原子提交，任务与提交一一对应。
- **全量回归**：`mvn clean install` BUILD SUCCESS（69 模块，01:40）；测试 750 run / 0 failures / 0 errors / 3 skipped（skipped 均为 `nebula-crawler-browser` 的 `StealthIntegrationTest`，需真实浏览器环境，属历史遗留跳过，与本工作流无关）。
- **Spec-Plan 偏差**（详见 log.md）：
  1. 新增阶段 A4（持久化可用性）——原 spec 无，源自 proud-day 走 B 方案的决策；T-A3-2 并入 T-A4-4 统一实现。
  2. T-A2-4b 的 gRPC token 鉴权移入工作流 B——net.devh 将迁 spring-grpc，现写专用拦截器属注定丢弃的返工。
  3. hardening-a 两处完成记录与代码不符（SSA-1 被误判"类已删除"、gRPC 侧 Class.forName 遗漏），已在 hardening-b 审查发现并补修（本文件 T-A2-4a 补漏、T-A3-8 勘误两条记录）。
- **破坏性变更**（发布时须随 release notes，详见 log「对外行为变更/迁移说明」）：缓存多态白名单（trusted-packages）、缓存 key 前缀化、响应缓存默认关+注解白名单、非预检 OPTIONS 不再免认证、性能端点默认关、RBAC Filter 默认关、`RpcRequest.parameterTypes` 改 `String[]`。
- **遗留问题**：
  1. 行为级验证转 CI（本地无外部服务）：RabbitMQ live 收发（T-A0-3）、ES Basic 认证（T-A2-7）、毒消息重投/topic 路由（T-A3-8）、读写分离多库路由（T-A4-5）。
  2. RDG-2：gRPC `callWithTarget` 走默认同步兜底（正确但串行），按 target 维护 Channel Map 的优化留后续。
  3. `subscribeWithTag` 与发送路由键不匹配（tag 队列收不到消息）——已登记 EPIC-C1。
  4. T-A2-5 删除 `XxlJobTaskService` 后的孤儿 DTO——留治理阶段（EPIC-C2）。
  5. 阶段 D（proud-day 迁移）未启动，须在 proud-day 重新构建吃到新快照前落地。
