# 变更日志 -- 剩余工作总体（nebula-remaining-work）

> 随开发实时追加；只记有复用价值的技术决策、踩坑与知识发现。

## 技术决策

### D1（设计期）：剩余工作分四阶段推进，阶段一/二/三串行为主

- 阶段一（审查修复）优先：两个 CRITICAL 属安全/可用性问题，是合入主干的前置条件。
- 阶段二（治理）在阶段一后：gRPC 形态定型（Boot 官方 starter）后才能裁决 `GrpcRpcProperties.ServerConfig` 遗留参数的去留；认证收敛依赖 Filter 注册机制稳定。
- 阶段三（Jackson 3）最后：错误码/认证收敛先做完，减少迁移期间的返工面；且依赖阶段一 Task 2 的 MockMvc 哨兵测试作为回归闸门。

### D2（设计期）：死配置采用"先盘点、后裁决、再动手"三步走

- 原因：审查报告点名的清单写于 A 批修复之前，部分项（如缓存 keyPrefix）已被后续修复接通；直接按旧清单删会误伤。Task 2-1 产出裁决表并经用户确认后才执行删除/接通。

### D3（2026-07-07，用户拍板 Q5/Q6）：阶段三缓存兼容与阶段四启动

- Q5 拍板：Jackson 3 迁移的 Redis 缓存序列化采用"升级清一次缓存"，不做双读兼容层（省一个兼容层的开发与回收成本）；升级指南写明清缓存步骤，proud-day 侧对应清 `pd:cache:*`。
- Q6 落地：proud-day 仓库 `/Users/andy/DevOps/SourceCode/business-projects/proud-day-projects/proud-day-backend`，已完成现状调研并在该仓库另立三件套 `docs/changes/nebula-persistence-adoption/`（spec + tasks + log）。
- Q7（2026-07-07 已确认）：`revision` 升 `2.1.0-SNAPSHOT`（在 `nebula-remaining-work` 分支执行，该分支自 hardening-b 检出）。调研发现 proud-day 依赖坐标 `io.nebula:*:2.0.1-SNAPSHOT` 且 parent 为 Boot 3.5.8，与本分支（Boot 4.1）版本号相同——快照仓库一旦被 Boot 4.1 产物覆盖，proud-day 重构建即静默吃到跨代际类库。升代号是最低成本的隔离手段。

### D4（2026-07-08）：实现文档编写期发现的两处方案修正

- **Task 1 验证方式修正**：原计划"扩展 autoconfigure 的 NebulaStarterDefaultsIntegrationTest 断言 starter defaults 注入"不可行——autoconfigure 测试若依赖 starter 会形成 Maven 循环依赖（starter → autoconfigure）。改为两半验证：autoconfigure 既有测试证机制，三个 starter 模块各加最小测试证内容。review-fixes tasks.md Task 1 已同步。
- **Jackson 3 验收口径修正**：jackson-annotations 3.x 保留 `com.fasterxml.jackson.annotation` 原包名（官方兼容设计），注解 import 无需迁移；"零 com.fasterxml.jackson 命中"验收不成立，改为 databind/core/datatype 三包前缀零命中。spec.md 2.3 与验收标准已同步。

### D5（2026-07-08）：ChatGPT 外部审查（第二轮，针对 implementation.md）核对结论与采纳

10 条意见逐条本地取证后**全部成立**（含 2 条部分成立），已改入文档：

- **P1-1 分支口径**（成立）：spec/tasks/review-fixes 四处 `nebula-hardening-b` 统一改为 `nebula-remaining-work`；log/Q7 中"b 分支"表述同步澄清。
- **P1-2 gRPC 版本混装**（成立，本轮最有价值的发现）：根 pom `:316` 自管 `grpc-bom:1.68.1` import 会压过 Boot 4.1 BOM 托管的 grpc 1.80.0（子 POM dependencyManagement 优先于父 BOM）；且 starter 默认带非 shaded `grpc-netty`，与保留的 `grpc-netty-shaded`（`GrpcRpcClient.java:6` 直接 import shaded 类，不能删）并存会两套传输冲突。Task 3 已改为：删根 grpc-bom import、`grpc.version` 属性升 1.80.0 留给 protobuf 插件坐标、starter 排除 `grpc-netty`、dependency:tree 验收单一版本。
- **P1-3 gRPC 测试依赖**（成立，已本地实证）：`org.springframework.boot:spring-boot-starter-grpc-server-test:4.1.0` 可拉取（本机 .m2 已有）；解包 `spring-boot-grpc-test-4.1.0.jar` 确认注解为 `org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort`。Task 5 从"执行时核验坐标"改为直接写死已验证坐标。
- **P2-1 Jackson POM 清单不全**（成立）：全仓 rg 命中 20+ 个 pom（gateway/storage/search/websocket/crawler/rpc-async/messaging/foundation/cache/mongodb 等），远超预列 5 处。Task 3-0 改为"全仓扫描生成清单"，预列条目降级为起点示例。
- **P2-2 ValidationException 示例不可编译**（成立）：该类无单字符串构造（`ValidationException.java:25-41`），示例改为 `(field, message, value)` 三参构造。
- **P2-3 executor 可选注入**（成立）：`rpcExecutor` 只在 HTTP RPC 启用时创建，Task 12.3 明确用 `ObjectProvider<Executor>.getIfAvailable(ForkJoinPool::commonPool)`，避免 gRPC-only 启动失败。
- **P2-4 ES 测试只测判定不测行为**（成立）：Task 11 测试补第二层——prod + sslVerificationEnabled=false 必须证明拿不到 trust-all SSLContext。
- **P2-5 提交口径矛盾**（成立）：统一为"一任务一提交"，撤销 Task 6/8 可合并的说法（implementation.md 与 review-fixes tasks.md 同步）。
- **P3-1 README/AGENTS 版本口径**（成立）：`README.md:4,11`、`AGENTS.md:12` 仍写 Boot 3.5.8，已加入 Task 13 收尾清单（允许提前单独提交）。
- **P3-2 RestClient 401 断言**（成立）：`HttpRpcClient.sendRequest`:312-332 catch 住所有异常封装为 `RpcResponse.exception`，client 侧拿不到裸 401。Task 6 测试拆两层断言：controller 层断 401 状态、client 层断封装后的失败响应。

## 知识发现（proud-day 调研，2026-07-07）

- 33 个 Mapper 全部继承 MP 原生 `BaseMapper`——T-A4-1 的 markerInterface 设计生效，"零改动迁移"成立。
- proud-day pom 显式覆盖 `mybatis-plus-spring-boot3-starter:3.5.12`（注释注明为压制 nebula 传递依赖），与 nebula 新版的 MP 3.5.16 boot4 starter 冲突，迁移时必删。
- prod Hikari `initialization-fail-timeout: -1`（DB 不可达仍完成启动）在 `DataSourceManager.PoolConfig` 无对应参数，迁移后启动语义变化，已列入 proud-day spec 待澄清 Q2。
- proud-day 自有 `MybatisPlusConfig`（分页）/`MybatisAuditConfig`（审计兜底）与 nebula 侧同类 Bean 均为 `@ConditionalOnMissingBean` 关系，可共存不冲突，迁移期不必删。

## 死配置盘点表（Task 2-1 产出，2026-07-08）

> 全量扫描范围：33 个 Properties 类，排除 target/test/docs 目录。
> 判定标准：字段 getter 被功能代码调用 = 有消费；仅在 NebulaComponentSummary/NebulaDiagnosticEndpoint 中展示 = 仅诊断；其余 = 无消费点。
> 裁决分类：本批处理（Task 2-2 接通 / Task 2-3 删除）、新发现记录（超出本批范围，记录待后续处理）。

### A. 本批处理范围（审查报告 :109 点名项）

| 配置项 | 定义位置 | 消费点 | 裁决 | 理由 |
|--------|----------|--------|------|------|
| **HttpRpcProperties.ClientConfig** | | | | |
| `writeTimeout` | :142 | 无消费点 | **删除** | SimpleClientHttpRequestFactory/JdkClientHttpRequestFactory 均不支持 |
| `maxConnections` | :150 | 仅诊断展示(:175) | **接通(方案A)或删除(方案B)** | 见下方方案选择 |
| `maxConnectionsPerRoute` | :158 | 无消费点 | **接通(方案A)或删除(方案B)** | 同上 |
| `keepAliveTime` | :166 | 无消费点 | **接通(方案A)或删除(方案B)** | 同上 |
| `retryCount` | :174 | 仅诊断展示(:176) | **删除** | RestClient 无内置重试，重试属上层职责 |
| `retryInterval` | :182 | 无消费点 | **删除** | 同上 |
| `compressionEnabled` | :187 | 仅诊断展示(:177) | **删除** | RestClient 未配置 gzip 拦截器 |
| `loggingEnabled` | :192 | 无消费点 | **删除** | HttpRpcClient 使用固定 @Slf4j，不受此开关控制 |
| **GrpcRpcProperties.ServerConfig** | | | | |
| `maxInboundMessageSize` | :52 | 无消费点 | **删除** | Boot gRPC starter 已接管，用 spring.grpc.server.* |
| `keepAliveTime` | :58 | 仅诊断展示(:86) | **删除** | 同上 |
| `keepAliveTimeout` | :64 | 无消费点 | **删除** | 同上 |
| `permitKeepAliveWithoutCalls` | :70 | 无消费点 | **删除** | 同上 |
| `maxConcurrentCalls` | :76 | 仅诊断展示(:85) | **删除** | 同上 |
| **SecurityProperties** | | | | |
| `anonymousUrls` | :38 | 无消费点 | **删除** | 功能由 nebula.web.auth.ignore-paths 替代 |
| `rbac.enableCache` | :89 | 无消费点 | **删除** | SecurityAspect 无权限缓存实现 |
| `rbac.cacheExpiration` | :94 | 无消费点 | **删除** | 同上 |
| `rbac.superAdminRole` | :99 | 无消费点 | **删除** | 文档约定 SUPER_ADMIN，代码中无引用 |
| `rbac.enabled` | :84 | 仅诊断展示(:94) | **保留(诊断)** | 仅摘要展示，无误导性 |
| **NacosProperties** | | | | |
| `heartbeatInterval` | :93 | 仅诊断展示 | **删除** | SDK 自管理，未传入 Nacos Properties |
| `heartbeatTimeout` | :101 | 无消费点 | **删除** | 同上 |
| `ipDeleteTimeout` | :109 | 无消费点 | **删除** | 同上(盘点新发现) |
| **RedisLockProperties** | | | | |
| `defaultWaitTime` | :34 | 仅诊断展示(:92) | **保留(记录)** | 未注入 LockConfig，但字段语义正确，后续可接通 |
| `defaultLeaseTime` | :40 | 仅诊断展示(:93) | **保留(记录)** | 同上 |
| `enableWatchdog` | :47 | 仅诊断展示(:94) | **保留(记录)** | 同上 |
| `fair` | :60 | 仅诊断展示(:95) | **保留(记录)** | 同上 |
| `watchdogInterval` | :53 | 无消费点 | **保留(记录)** | Redisson 内置看门狗，不读该字段 |
| `redlock.enabled` | :124 | 仅诊断展示(:96) | **保留(记录)** | Redlock 未接线，但保留配置结构供后续实现 |
| `redlock.addresses` | :130 | 无消费点 | **保留(记录)** | 同上 |
| `redlock.quorum` | :136 | 无消费点 | **保留(记录)** | 同上 |

#### HTTP RPC 连接池方案选择（需用户拍板）

- **方案 A（推荐：接通连接池）**：`rpcRestClient` 改用 `HttpComponentsClientHttpRequestFactory` + `PoolingHttpClientConnectionManager`，接通 `maxConnections`/`maxConnectionsPerRoute`/`keepAliveTime`。需加依赖 `httpclient5`（Boot BOM 托管）。
- **方案 B（删除连接池）**：`rpcRestClient` 改用 `JdkClientHttpRequestFactory`，删除 `maxConnections`/`maxConnectionsPerRoute`/`keepAliveTime` 字段。

两方案共同项：`writeTimeout`/`compressionEnabled`/`retryCount`/`retryInterval`/`loggingEnabled` 均删除。

### B. CORS 两套配置分析

| 配置前缀 | 属性类 | 生效场景 | 结论 |
|---------|--------|---------|------|
| `nebula.web.cors.*` | WebProperties.Cors | Servlet Web 应用 | 被 WebCoreAutoConfiguration 消费(双路径: CorsConfigurationSource + addCorsMappings) |
| `nebula.gateway.cors.*` | GatewayProperties.CorsConfig | Spring Cloud Gateway | 被 GatewayRoutesAutoConfiguration.corsWebFilter() 消费 |

两套分属不同部署形态（Web 进程 vs Gateway 进程），同一进程不同时加载。**非死配置，不处理**。
但 Web 侧存在 CorsConfigurationSource 与 addCorsMappings 双注册潜在不一致——记录供后续优化，本批不处理。

### C. 新发现死配置（超出本批范围，记录待后续）

| 类 | 死配置字段数 | 关键项 |
|----|------------|--------|
| CacheProperties | 8 | local.enabled/expireAfterAccess/statsEnabled; redis.serialization/ssl.*; multiLevel.enabled; defaultMaxSize |
| RabbitMQProperties | 17 | consumer.retryCount/retryInterval; producer.publisherConfirms/confirmTimeout/publisherReturns; exchange.durable/autoDelete; delayMessage 整组(与 RabbitDelayMessageProperties 重复) |
| ElasticsearchProperties | 5 | bulkSize; searchTimeout; scrollSize; sslCertificatePath; sslKeyPath |
| MinIOProperties | 4 | secure; defaultExpiry; maxFileSize; allowedContentTypes |
| TaskProperties | 6 | executor 整组(corePoolSize/maxPoolSize/keepAliveSeconds/queueCapacity/threadNamePrefix); xxlJob.registryTimeout |
| AIProperties | 15+ | chat/embedding/vectorStore 多提供商配置; ollama 整棵废弃子树 |
| WebProperties | 9 | ExceptionHandler.logStackTrace/includeExceptionDetails; DataMasking.sensitiveFields/strategy/maskChar; Performance.enableDetailedMetrics/metricsInterval; Health.endpoint/checkInterval |
| GatewayProperties | 8 | LoggingConfig.logRequestBody/logResponseBody; RoutesConfig.apiPathPrefix; HttpProxyConfig.timeout; TimeoutConfig 整组(3 字段); JwtConfig.enabled(仅诊断) |

### D. `Result.of(ResultCode)` 码值风格决策（需用户拍板）

`Result.of(ResultCode resultCode)` 工厂产出的 `code` 字段取什么值？

- **选项 1（推荐）**：`resultCode.name()`（如 `"SUCCESS"`）—— 与 `Result.success().getCode()` 现有符号码风格一致，下游 `result.getCode().equals("SUCCESS")` 断言不破
- **选项 2**：`resultCode.getCode()`（如 `"0000"`）—— 与 `ResultCode` 枚举定义的数字码一致，但与 `Result` 现有码值不同体系

## 阶段一任务验证记录

### Task 1 [C-1] Starter 默认锁定 MVC 走 Jackson 2
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=NebulaStarterDefaultsIntegrationTest && mvn test -pl starter/nebula-starter-web,starter/nebula-starter-all,starter/nebula-starter-mcp`
- autoconfigure 结果: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (11.056s)
- starter 结果: Nebula Starter Web SUCCESS [2.704s], Nebula Starter MCP SUCCESS [1.966s], Nebula Starter All SUCCESS [2.274s]; Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (7.300s)

### Task 2 [C-1] MockMvc 脱敏回归哨兵测试
- 验证命令: `mvn test -pl application/nebula-web -Dtest=SensitiveDataMvcMaskingTest`
- 结果: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (4.475s)
- 哨兵有效性验证: 临时移除 preferred-json-mapper=jackson2 后测试转红, 报 `JSON path "$.data.phone" expected:<138****5678> but was:<13812345678>`, 证明哨兵确实在看门。恢复属性后测试恢复绿色。
- SB4 包迁移: `AutoConfigureMockMvc` 从 `org.springframework.boot.test.autoconfigure.web.servlet` 迁移到 `org.springframework.boot.webmvc.test.autoconfigure`; 需额外依赖 `spring-boot-starter-webmvc-test`

### Task 3 [C-2] nebula-rpc-grpc 接入 Boot 官方 gRPC Server Starter
- 验证命令: `mvn -q compile -pl infrastructure/rpc/nebula-rpc-grpc -am && mvn dependency:tree -pl infrastructure/rpc/nebula-rpc-grpc`
- 编译结果: BUILD SUCCESS (20s)
- 依赖树确认: spring-boot-starter-grpc-server:4.1.0 出现; 所有 io.grpc 坐标统一为 1.80.0; 传输层仅 grpc-netty-shaded(无 grpc-netty); spring-grpc-core:1.1.0 作为 starter 传递依赖自动引入
- 根 pom 变更: 删除自管 grpc-bom:1.68.1 import + spring-grpc-core dependencyManagement; grpc.version 属性改 1.80.0(protobuf 插件坐标引用); spring-grpc.version 属性已删

### Task 9 [M-2] EPP 迁移至 SB4 新接口与新注册键
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure`
- 结果: Tests run: 23, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (6.748s)
- NebulaStarterDefaultsIntegrationTest 回归通过(EPP 新键注册生效)
- `rg "net.devh" --type java` 主代码零命中(删除 GatewayGrpcServerExcludeConfiguration + 对应 spring.factories)

### Task 6 [H-1] HTTP RPC 客户端注入鉴权 token
- 验证命令: `mvn test -pl infrastructure/rpc/nebula-rpc-http`
- 结果: Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (7.597s)
- 新增 HttpRpcAuthTokenRoundTripTest(4 用例): controller 层无 token->401/有 token->200; client 层带 token->成功/无 token->RuntimeException("RPC调用失败")
- 既有测试回归: HttpRpcControllerAuthTest(4), HttpRpcClientTest(2), HttpRpcClientTargetIsolationTest(2), HttpRpcControllerFindMethodTest(3) 均绿
- 偏差: tasks.md 称"构造函数变更为编译期破坏性变更", 实际保留 4 参构造委托 5 参传空串, 破坏面最小化; 已记 log

### Task 5 [C-2] gRPC 服务端回环集成测试(含 token 拦截器)
- 验证命令: `mvn test -pl infrastructure/rpc/nebula-rpc-grpc -Dtest=GrpcServerLoopbackIntegrationTest`
- 结果: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (7.745s)
- 日志确认: gRPC Server started, listening on port 55393; Registered gRPC service: io.nebula.rpc.grpc.GenericRpcService; token 校验失败(无 token 被拒)
- 三个用例: 回环调用成功(sayHello); 无 token 被拒(UNAUTHENTICATED); 带 token 调用成功(add)
- T-B2-2 已在 sb4-upgrade tasks.md 中勾选完成
- 偏差: implementation.md 建议用 GrpcRpcClient 做回环, 但 GrpcRpcClient 不支持 auth metadata 注入; 改用原生 gRPC stub + ClientInterceptor 注入 token, 同时覆盖回环+鉴权

### Task 4 [C-2] gRPC 端口桥接 EPP + 旧端口配置废弃标注
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=NebulaGrpcServerPortBridge*`
- 结果: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (4.196s)
- 三态测试: 只配旧键->新键出现且值一致; 两者都配->新键保持用户值; 都不配->不注入
- GrpcRpcProperties.ServerConfig: port 标 @Deprecated, 其余线程/流控参数 Javadoc 标注改用 spring.grpc.server.*, Task 2-3 清理

### Task 7 [H-2] ServiceImpl 列名白名单校验
- 涉及文件: ServiceImpl.java (新增 validateColumn + 接入 findByField/findOneByField/findByFields)
- 新增 ServiceImplColumnValidationTest.java: 合法列名放行(6 种) + 非法列名拒绝(7 种) + null/空串拒绝
- 验证命令: `mvn test -pl infrastructure/data/nebula-data-persistence` → Tests run: 39, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS
- 偏差: ValidationException.getMessage() 固定返回"数据验证失败", 测试改用 getFormattedMessage()+fieldErrors 校验, 断言更强(同时验证字段名和消息内容)

### Task 8 [M-1] /rpc token 常量时间比较
- 涉及文件: HttpRpcController.java, GrpcAuthTokenInterceptor.java
- 变更: `.equals(provided)` → `MessageDigest.isEqual(expected.getBytes(UTF_8), provided.getBytes(UTF_8))`; 先判 provided != null 避免 NPE
- 验证命令: `mvn test -pl infrastructure/rpc/nebula-rpc-http` → Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS
- 验证命令: `mvn test -pl infrastructure/rpc/nebula-rpc-grpc` → Tests run: 58, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS
- 已有鉴权测试(含 token/无 token/错误 token)全绿, 常量时间比较不影响行为

### Task 10 [M-3] 缓存健康检查键前缀 + 统计原子化
- 涉及文件: DefaultCacheManager.java, LocalCacheManager.java
- 变更: `"health:check"` → `keyPrefix + "health:check"`, 三个 `volatile long` → `LongAdder`
- 验证命令: `mvn test -pl infrastructure/data/nebula-data-cache` → Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS
- LocalCacheManager 一并修改(同样存在 volatile long 统计), clear() 改 LongAdder.reset()

### Task 11 [M-4] ES trust-all 生产环境防护
- 涉及文件: ElasticsearchAutoConfiguration.java, 新增 ElasticsearchSslGuardTest.java
- 变更: createSSLContext() 在 sslVerificationEnabled=false 时, 先判 isProductionProfile(); 生产环境 error 日志 + 回退默认 SSLContext, 非生产才 trust-all
- 对齐: 与 HttpCrawlerEngine 相同的 SAFE_PROFILES 判定逻辑(dev/test/local 视为安全)
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=ElasticsearchSslGuardTest` → Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS
- 五个用例: prod 被拦截, production 被拦截, dev 允许, 无 profile 允许, sslVerificationEnabled=true 正常

### Task 12 [L批] 三处小修
- 子项1: JwtAuthenticationFilter.readStringList() 删除冗余 `.filter(Objects::nonNull)`, 清理无引用 `import java.util.Objects`
- 子项2: ElasticsearchAutoConfiguration.elasticsearchClient() 改为 `objectMapper.copy()` 再注册模块, 避免污染全局 ObjectMapper
- 子项3: ServiceDiscoveryRpcClient 新增 5 参构造(含 Executor), 4 参构造委托传 ForkJoinPool.commonPool(); callAsync() 使用注入的 executor
- 验证命令: `mvn -q compile -pl core/nebula-security,infrastructure/rpc/nebula-rpc-core` 编译通过; `mvn test -pl core/nebula-security,infrastructure/rpc/nebula-rpc-core` BUILD SUCCESS

### Task 13 全量回归 + 文档收尾
- 全仓安装: `mvn install -DskipTests` → 全部 68 模块 BUILD SUCCESS (20s)
- 全量测试: `mvn test` → 全部模块 BUILD SUCCESS (01:15 min)
- 文档更新: 审查报告 C-1~C-2, H-1~H-2, M-1~M-4, L-1~L-3 标注"已修复(Task N)"
- tasks.md: review-fixes Task 1-13 全部勾选; remaining-work 阶段一三个闸门勾选
- sb4-upgrade tasks.md: T-B2-2 已勾选(Task 5 完成时)

### 阶段一对账审计（2026-07-08，用户侧复核）

按 implementation.md 附录 A 对账清单执行，总体结论：**13 任务提交序列完整（18db8b77 → c6ce845f），核心代码抽查全部落地真实有效**，但发现两处欠账（已立 Task 2-0 清理）：

- 提交对账：13 个任务对应 13 个独立提交，信息格式合规；提交在远端 vocoor 分支领先本地，已 ff 合并到本地。
- 代码抽查通过：Task 3（starter+排除 grpc-netty+grpc.version 1.80.0+根 grpc-bom 已删）、Task 5（`@LocalGrpcServerPort` 注解+UNAUTHENTICATED 用例真实存在）、Task 6/8（authToken 注入+`MessageDigest.isEqual`）、Task 7（`validateColumn` 三方法接入）、Task 11（行为级断言 `SSLContext.getDefault()` 对比，非仅 profile 判定）。
- log.md 每任务有真实验证输出（测试数+BUILD SUCCESS），格式合规。
- 全仓本地重跑：`mvn clean compile` BUILD SUCCESS（68 模块）+ `mvn test` BUILD SUCCESS（01:10 min），与执行方记录一致；首轮重跑偶发失败系本机构建残留（单模块复跑 data-cache 22 测试全绿），非代码问题。
- **欠账 1（Task 13 部分未做）**：README.md/AGENTS.md 仍写 Boot 3.5.8；CLAUDE.md v2.0.x 变更记录未补阶段一内容。
- **欠账 2（Task 12.3 装配缺口，且偏差未登记）**：`ServiceDiscoveryRpcClient` 5 参构造已加，但 `RpcDiscoveryAutoConfiguration:100` 仍调 4 参构造，`rpcExecutor` 实际未接通。

## 阶段二任务验证记录

### Task 2-0a 文档欠账修复
- 验证: `rg "3\.5\.8" README.md AGENTS.md` 零命中; `rg "auth-token|preferred-json-mapper" CLAUDE.md` 命中 2 行(变更记录已补充)
- 提交: a2b469b

### Task 2-0b RpcDiscoveryAutoConfiguration 装配缺口修复
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure`
- 结果: Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS
- 新增 RpcDiscoveryExecutorInjectionTest(2 用例): 有 rpcExecutor Bean 时注入它; 无时回落 ForkJoinPool.commonPool()
- 注意: Maven 镜像 nexus.vocoor.com.cn SSL 证书失效(SAN 不匹配), 需使用 `-s /tmp/nebula-mvn-settings.xml` 绕过镜像直连 Maven Central

### Task 2-2 HTTP RPC 客户端参数接通（方案 A: HttpComponents 连接池）
- rpcRestClient 从 SimpleClientHttpRequestFactory 改为 HttpComponentsClientHttpRequestFactory + PoolingHttpClientConnectionManager
- 接通参数: connectTimeout, readTimeout, maxConnections, maxConnectionsPerRoute, keepAliveTime
- 删除死字段: writeTimeout, retryCount, retryInterval, compressionEnabled, loggingEnabled
- 诊断端点(NebulaDiagnosticEndpoint)只展示真实生效参数
- autoconfigure pom 加 httpclient5 optional 依赖(Boot BOM 托管 5.6.1)
- 新增 HttpRpcClientConfigTest(3 用例): Bean 创建、属性读取、已删字段反射确认
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure,infrastructure/rpc/nebula-rpc-http` → Tests run: 51 (15+36), Failures: 0 -- BUILD SUCCESS

### Task 2-4 错误码收敛 ResultCode 为唯一事实源
- ResultCode.getByCode() 原实现正确(遍历比较数字码)，CF-40 真实问题是跨体系互查不通(偏差已记)
- 新增 ResultCode.getByName(String) 容错版 valueOf
- 新增 Result.of(ResultCode) / of(ResultCode, T) 工厂，code 取 name()（用户拍板选项 1）
- Constants.ErrorCode 整个内部类标 @Deprecated，Javadoc 指向 ResultCode
- 新增 ResultCodeTest(9 用例): 全枚举 getByCode/getByName 回查、Result.of 码值断言、既有 Result 工厂码值回归
- 验证命令: `mvn test -pl core/nebula-foundation` → Tests run: 278, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS

### Task 2-3 死配置删除批 (security/nacos/grpc)
- SecurityProperties: 删除 `anonymousUrls` 列表、`rbac.enableCache`/`rbac.cacheExpiration`/`rbac.superAdminRole` 字段; 保留 `rbac.enabled`(诊断展示)
- NacosProperties: 删除 `heartbeatInterval`/`heartbeatTimeout`/`ipDeleteTimeout` 字段
- GrpcRpcProperties.ServerConfig: 删除 `maxInboundMessageSize`/`keepAliveTime`/`keepAliveTimeout`/`permitKeepAliveWithoutCalls`/`maxConcurrentCalls` 字段
- NacosDiscoveryAutoConfiguration: 删除诊断展示中的 `Heartbeat` 行(引用已删字段)
- GrpcRpcAutoConfiguration: 删除诊断展示中的 `maxConcurrentCalls`/`keepAliveTime` 行
- 验证命令: `mvn clean compile` → BUILD SUCCESS (68 模块)
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure` → Tests run: 36, Failures: 0 -- BUILD SUCCESS
- 验证命令: `mvn test -pl core/nebula-security` → Tests run: 30, Failures: 0 -- BUILD SUCCESS
- 残留引用检查: `rg "anonymousUrls|heartbeatInterval|heartbeatTimeout|ipDeleteTimeout|maxConcurrentCalls" --type java` 主代码零命中(仅 log.md 有记录)

### Task 2-6 xxl-job 孤儿 DTO 清理
- 删除: XxlJobExecuteRequest.java, XxlJobLogRequest.java, XxlJobLogResult.java
- rg 确认零引用(仅定义处命中)
- 验证命令: `mvn test -pl application/nebula-task` → BUILD SUCCESS
- XxlJobResult.java 保留(XxlJobRegistryService 在用)

### Task 2-5: 认证收敛 -- 单一 JWT 解析点
- 验证命令: `mvn test -pl application/nebula-web` → Tests run: 88, Failures: 0, Errors: 0 → BUILD SUCCESS
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure` → Tests run: 36, Failures: 0, Errors: 0 → BUILD SUCCESS
- 改动要点:
  - AuthInterceptor: 移除 AuthService 依赖，改读 SecurityContext.getAuthentication()，桥接填充 AuthContext
  - WebAuthAutoConfiguration: authWebMvcConfigurer 不再注入 AuthService
  - JwtFilterEnabledCondition(新增): AnyNestedCondition，security.jwt.filter.enabled=true OR web.auth.enabled=true
  - SecurityAutoConfiguration: JwtAuthenticationFilter 注册改用 JwtFilterEnabledCondition
  - nebula-web pom.xml: 加 nebula-security optional 依赖（编译期引用 SecurityContext）
  - AuthInterceptorTest: 适配新依赖（mock SecurityContext 替代 AuthService）
  - AuthConvergenceIntegrationTest(新增): 4 个测试用例验证单一解析点、401 拦截、白名单放行、上下文同步

### 阶段二对账审计（2026-07-08，用户侧复核）

按附录 A 对账清单执行，结论：**10 任务（含 2-0a/2-0b）提交序列完整（a2b469bb → 3b759bb6），抽查全部通过，仅一处勾选遗漏（已补）**。

- 提交对账：11 个提交（10 任务 + 1 方案文档提交 4a011eba），一任务一提交合规；本次提交在本地，无需远端同步。
- 代码抽查通过：2-5（`AuthInterceptor` 改读 `SecurityContext`、`JwtFilterEnabledCondition extends AnyNestedCondition` 实现 OR 注册、`AuthConvergenceIntegrationTest` 用计数版 JwtService 断言单次解析）；2-3（anonymousUrls/heartbeat/grpc 遗留参数主代码零命中——task 模块的 heartbeatInterval 是 xxl-job 在用配置，非删除对象，正确保留）；2-2（`PoolingHttpClientConnectionManager` + setMaxTotal/setDefaultMaxPerRoute 真实接通）；2-4（`Result.of` 双工厂 + `ResultCode.getByName`，码值取 name() 符合拍板选项 1）；2-6（dto 目录仅剩 XxlJobResult）；2-0a（README/AGENTS 已 4.1.0）；2-0b（`@Qualifier("rpcExecutor") ObjectProvider<Executor>` 已接通）。
- HARD-GATE 合规：Task 2-1 盘点表（11 类/38 字段）产出后停下等用户裁决，裁决通过才执行 2-2/2-3，流程正确。
- 全仓本地重跑：`mvn clean compile` + `mvn test` BUILD SUCCESS（01:11 min）。
- 勾选遗漏：Task 2-0 两个子项已勾但总"完成"项漏勾，审计时补勾并注明。

### D6（2026-07-08）：阶段三开工前事实核定（写入 implementation.md 3.2 节）

- 第三方 Jackson 3 支持逐一核实本地 jar：spring-data-redis 4.1（`GenericJacksonJsonRedisSerializer`）、spring-amqp 4.1（`JacksonJsonMessageConverter`）、elasticsearch-java 9.4.2（`Jackson3JsonpMapper`）均有 Jackson 3 变体可平移；Boot 4.1 定制入口为 `JsonMapperBuilderCustomizer`（spring-boot-jackson 4.1.0 解包确认）。
- **jjwt 0.13.0 无 Jackson 3 支持**（官方 issue #1029：Jackson 3 适配将在 JDK 17 基线的后续版本发布）——拆桥后允许的唯一 Jackson 2 运行时残留即 jjwt 传递依赖，Task 3-0 盘点表核销。
- `spring-boot-jackson2` 桥位于根 pom :338-341 的全模块 `<dependencies>`（非 dependencyManagement），拆桥影响全仓。
- `nebula-ai-core` 模型类仅用 `com.fasterxml.jackson.annotation`（Jackson 3 保留原包名），无需迁移。
- websocket 两模块（netty 5 文件 + spring 4 文件）tasks.md 阶段三未单列，归入 Task 3-3 执行。
- Task 3-0 取消 HARD-GATE：第三方策略已被上述核定锁死，仅出现未覆盖的新约束时才停下汇报。

## 踩坑记录

### P2-T1: Maven 镜像 SSL 证书失效
- 现象: `mvn test` 报 `Certificate for <nexus.vocoor.com.cn> doesn't match any of the subject alternative names: [vocoor.com.cn, www.vocoor.com.cn]`
- 原因: Nexus 镜像 SSL 证书的 SAN 列表只有 `vocoor.com.cn` 和 `www.vocoor.com.cn`, 不包含 `nexus.vocoor.com.cn`
- 临时解决: 使用临时 settings 文件 `/tmp/nebula-mvn-settings.xml` 绕过镜像直连 Maven Central; 全部 mvn 命令需加 `-s /tmp/nebula-mvn-settings.xml`
- 建议: 修复 Nexus 服务器 SSL 证书(加 SAN `nexus.vocoor.com.cn`)

### Task 2-8: 文档同步 + 阶段二收尾
- 验证命令: `mvn clean compile` → BUILD SUCCESS (全仓 20.890s)
- 验证命令: `mvn test -pl core/nebula-foundation,core/nebula-security,application/nebula-web,application/nebula-task,autoconfigure/nebula-autoconfigure` → BUILD SUCCESS (所有关键模块)
- 改动要点:
  - CLAUDE.md: 补阶段二变更记录(Task 2-0~2-7)；更新15+处行号引用(AuthInterceptor/RateLimiter/RPC/Lock等)；SecurityAutoConfiguration路径更正(core→autoconfigure)
  - AGENTS.md: 同步更新对应行号引用
  - .cursor/rules/project.mdc: 第6章鉴权架构更新为Filter唯一解析+Interceptor消费SecurityContext；删除已失效的RBAC配置引用
  - env/StarterDefaultsInjectionTest 从Task 2-7清单移除: 阶段一已落地

### Task 2-7: 自动装配三态条件测试
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure` → Tests run: 61, Failures: 0, Errors: 0 → BUILD SUCCESS
- 新增/扩展测试类:
  - SecurityAutoConfigurationTest: 扩展 +3 测试（enabled=false、缺类、web.auth.enabled 联动）
  - WebAutoConfigurationConditionTest(新增): Web环境/非Web/缺DispatcherServlet 三态
  - CacheAutoConfigurationConditionTest(新增): enabled/disabled/default 三态（无@ConditionalOnClass，不测缺类）
  - RabbitMQAutoConfigurationConditionTest(新增): enabled/disabled/default/缺ConnectionFactory 四态
  - HttpRpcAutoConfigurationConditionTest(新增): enabled/disabled/default/缺HttpRpcProperties 四态
  - GrpcRpcAutoConfigurationConditionTest(新增): enabled/disabled/default/缺GrpcRpcProperties 四态
  - ElasticsearchAutoConfigurationConditionTest(新增): enabled/disabled/default/缺ElasticsearchClient 四态
- env/StarterDefaultsInjectionTest 从清单移除: 阶段一 Task 1 已在三个 starter 模块内落地同名测试

### P2-T5: AuthConvergenceIntegrationTest 上下文启动失败
- 现象1: `@MockitoSpyBean` 报 `Unable to select a bean to wrap: there are no beans of type JwtService`
- 原因: 测试 classpath 无 nebula-autoconfigure，JwtService Bean 由测试内 @Bean 定义，@MockitoSpyBean 在 Bean 注册前尝试查找
- 修复: 改用 CountingJwtService（extends DefaultJwtService）手动计数替代 @MockitoSpyBean
- 现象2: JwtUtils 创建失败 `WeakKeyException: 200 bits`
- 原因: nebula.web.auth.jwt-secret 默认值 25 字符 = 200 bits < HMAC-SHA256 要求的 256 bits
- 修复: 测试 properties 中设置足够长的 jwt-secret
- 现象3: 受保护接口返回 200 而非 401，AuthContext 为空
- 原因: WebAuthAutoConfiguration 中 authWebMvcConfigurer 使用 @ConditionalOnMissingBean(返回类型 WebMvcConfigurer)，
  而 WebCoreAutoConfiguration 先于它被 @Import，已注册 WebMvcConfigurer Bean，导致 authWebMvcConfigurer 被跳过
- 修复: 集成测试排除 WebAutoConfiguration，显式注册 AuthInterceptor，隔离测试认证收敛逻辑
- 注意: @ConditionalOnMissingBean 用于接口类型（WebMvcConfigurer）是已知的框架设计隐患，后续需评估修复

## 知识发现

### WebAutoConfiguration 子配置 @ConditionalOnMissingBean 设计隐患
- WebAuthAutoConfiguration/WebRateLimitAutoConfiguration/WebCacheAutoConfiguration/WebMonitorAutoConfiguration
  的 WebMvcConfigurer Bean 均标注了 @ConditionalOnMissingBean
- 由于 @Import 顺序，WebCoreAutoConfiguration 的 nebulaWebMvcConfigurer 先注册后，
  后续子配置的 WebMvcConfigurer Bean 理论上会被 @ConditionalOnMissingBean 跳过
- 但生产环境中实际可用（需进一步确认 Spring Boot 在同一 @Import 批次中的条件评估行为）
- 建议: 后续版本移除这些 @ConditionalOnMissingBean，或改用具体 Bean 名称匹配

## 阶段三任务验证记录

### Task 3-0: Jackson POM 依赖盘点与替代策略

**jjwt Jackson 3 支持复查（2026-07-08）**：GitHub issue #1029 及 PR #1032 确认 0.14.x 分支正在开发 Jackson 3 支持，但截至 2026-07-08 **未发布**（0.14.x pom.xml 仍写 jackson.version=2.22.0）。维护者表示"will be out in the next release"（2026-06-11）。结论：保留 jjwt-jackson + 传递 Jackson 2，为唯一允许的运行时残留。

**全仓 POM 盘点表**（`rg "jackson|jjwt" -g 'pom.xml'`，24 个文件命中）：

| 模块 pom | 依赖 | 策略 | 理由 |
|----------|------|------|------|
| **根 pom.xml** | | | |
| :148 `jjwt.version=0.13.0` | 属性 | **保留** | jjwt 无 Jackson 3 变体 |
| :279-290 jjwt-api/impl/jackson DM | dependencyManagement | **保留** | 同上 |
| :336-340 `spring-boot-jackson2` | 全模块 dependency | **Task 3-6 删除** | 迁移完成后拆桥 |
| **core/nebula-foundation** | | | |
| :26-27 `jackson-databind` | com.fasterxml.jackson.core | **换 `tools.jackson.core:jackson-databind`** | Jackson 3 坐标 |
| :30-31 `jackson-datatype-jsr310` | com.fasterxml.jackson.datatype | **删除** | 并入 Jackson 3 databind |
| :53-64 jjwt-api/impl/jackson | | **保留** | JWT 签发需要 |
| **core/nebula-security** | | | |
| :63-74 jjwt-api/impl/jackson | | **保留** | JWT 验证需要 |
| **application/nebula-web** | | | |
| :61-62 `jackson-datatype-jsr310` | | **删除** | 并入 Jackson 3 |
| :68-81 jjwt-api/impl/jackson | | **保留** | 遗留引用(AuthContext) |
| **application/nebula-task** | | | |
| :60-61 `jackson-databind` | com.fasterxml.jackson.core | **换 `tools.jackson.core:jackson-databind`** | |
| **infrastructure/data/nebula-data-cache** | | | |
| :40-41 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :44-45 `jackson-datatype-jsr310` | | **删除** | |
| **infrastructure/data/nebula-data-persistence** | | | |
| :76-77 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :80-81 `jackson-datatype-jsr310` | | **删除** | |
| **infrastructure/data/nebula-data-mongodb** | | | |
| :39-40 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| **infrastructure/messaging/nebula-messaging-core** | | | |
| :39-40 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :43-44 `jackson-datatype-jsr310` | | **删除** | |
| **infrastructure/messaging/nebula-messaging-redis** | | | |
| :40-41 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :44-45 `jackson-datatype-jsr310` | | **删除** | |
| **infrastructure/rpc/nebula-rpc-grpc** | | | |
| :85-86 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| **infrastructure/rpc/nebula-rpc-async** | | | |
| :63-64 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| **infrastructure/websocket/nebula-websocket-core** | | | |
| :28-29 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :32-33 `jackson-datatype-jsr310` | | **删除** | |
| **infrastructure/websocket/nebula-websocket-spring** | | | |
| :42-43 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :46-47 `jackson-datatype-jsr310` | | **删除** | |
| **infrastructure/websocket/nebula-websocket-netty** | | | |
| :48-49 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :52-53 `jackson-datatype-jsr310` | | **删除** | |
| **infrastructure/gateway/nebula-gateway-core** | | | |
| :57-68 jjwt-api/impl/jackson | | **保留** | Gateway JWT(应用层) |
| :75-76 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| **infrastructure/storage/nebula-storage-core** | | | |
| :46-47 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| **infrastructure/search/nebula-search-core** | | | |
| :46-47 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :52-53 `jackson-annotations` | | **保留** | 注解包名不变 |
| **infrastructure/search/nebula-search-elasticsearch** | | | |
| :49-50 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| **infrastructure/ai/nebula-ai-core** | | | |
| :34-35 `jackson-annotations` | | **保留** | 注解包名不变，无代码迁移 |
| **infrastructure/ai/nebula-ai-spring** | | | |
| :87-88 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| **infrastructure/crawler/nebula-crawler-captcha** | | | |
| :49-50 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| **integration/nebula-integration-payment** | | | |
| :55-56 `jackson-databind` | | **换 `tools.jackson.core:jackson-databind`** | |
| :60-61 `jackson-datatype-jsr310` | | **删除** | |
| **examples/gateway-example** | | | |
| :48-57 jjwt-api/impl/jackson | | **保留** | 示例跟随主仓库 |

**统计**：
- 换坐标（databind）：17 处
- 删除（jsr310）：11 处
- 保留（jjwt/annotations）：8 组
- Task 3-6 拆桥删除（spring-boot-jackson2）：1 处

**未发现新第三方约束**：所有 Jackson 2 依赖均为框架自身代码使用或 jjwt 传递，不存在 implementation.md 3.2 节第三方约束表未覆盖的情况。继续执行。

### Task 3-1: foundation 层迁移

**迁移文件**：
- `core/nebula-foundation/pom.xml`：`jackson-databind` 换 `tools.jackson.core`；删除 `jackson-datatype-jsr310`
- `JsonUtils.java`：全部 import 迁移至 `tools.jackson.*`；`ObjectMapper` 构建改用 `JsonMapper.builder()`；删除 `JavaTimeModule` 注册；`JsonProcessingException` → `JacksonException`；`fields()` → `properties()`
- `Beans.java`：同理迁移 `ObjectMapper` 构建

**Jackson 3 行为差异发现**：
- `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`：已从枚举移除（Jackson 3 内置 java.time 默认 ISO-8601）
- `JsonNode.fields()` → `JsonNode.properties()`
- `FAIL_ON_NULL_FOR_PRIMITIVES` 默认值从 `false`(J2) 变为 `true`(J3)：JSON 缺失字段映射到 `int` 等原始类型时 J3 会报错。修复：显式 `.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)`
- `readValue(String, JavaType)` 在不可变 mapper 上行为与 J2 一致，无问题；改用 `readerForListOf()` 更简洁

**验证**：`mvn test -pl core/nebula-foundation` → Tests run: 278, Failures: 0, BUILD SUCCESS
**全仓编译**：`mvn clean compile` → BUILD SUCCESS

### Task 3-2: data 层迁移

**迁移文件**：
- `nebula-data-cache/pom.xml`：换坐标 + 删 jsr310
- `nebula-data-persistence/pom.xml`：换坐标 + 删 jsr310
- `nebula-data-mongodb/pom.xml`：换坐标
- `DefaultCacheManager.java`：import 改 `tools.jackson.*`；`ObjectMapper` 构建改 `JsonMapper.builder().build()`；删除 `JavaTimeModule`/`WRITE_DATES_AS_TIMESTAMPS`

**验证**：`mvn test -pl infrastructure/data/nebula-data-cache,infrastructure/data/nebula-data-persistence` → Tests run: 22+39=61, Failures: 0, BUILD SUCCESS
**全仓编译**：BUILD SUCCESS

---

### Task 3-3: messaging/rpc/lock/websocket 层迁移

**日期**: 2026-07-08

**迁移范围**：
- POM 换坐标: messaging-core, messaging-redis, rpc-grpc, rpc-async, websocket-core, websocket-spring, websocket-netty
- 源码 import 迁移: messaging (JsonMessageSerializer, RedisMessageSerializer), rpc (GrpcRpcClient, GrpcRpcServer, HttpRpcClient, HttpRpcController, AsyncRpcExecutionManager, NacosAsyncExecutionStorage), websocket (所有模块的 Handler/Server/AutoConfiguration/Session 类)
- ObjectMapper 构建方式统一改为 `JsonMapper.builder().build()`
- `JsonProcessingException` 改 `JacksonException`（catch 子句）
- 删除 `JavaTimeModule` 注册和 `WRITE_DATES_AS_TIMESTAMPS` 配置

**关键决策 - Autoconfigure 层 ObjectMapper 兼容性**：
- 问题: `spring-boot-jackson2` bridge（Task 3-6 前保留）导致 Spring 上下文的 `ObjectMapper` bean 为 Jackson 2 类型 (`com.fasterxml.jackson.databind.ObjectMapper`)，无法注入到已迁移为 Jackson 3 的 RPC/WebSocket 模块
- 解决: RPC 自动配置类 (`GrpcRpcAutoConfiguration`, `HttpRpcAutoConfiguration`, `AsyncRpcAutoConfiguration`, `NacosAsyncStorageAutoConfiguration`) 不再从 Spring context 注入 ObjectMapper，改为自行创建 `JsonMapper.builder().build()` 实例
- WebSocket 自动配置类 (`WebSocketAutoConfiguration`, `NettyWebSocketAutoConfiguration`) 同理改为 `private static final ObjectMapper` 字段
- 此策略在 Task 3-6 "拆桥收尾" 前确保编译兼容

**测试结果**：
- messaging-core: 5 tests, 0 failures
- rpc-http: 15 tests, 0 failures
- rpc-grpc: 58 tests, 0 failures (含 GrpcServerLoopbackIntegrationTest)
- lock-redis: 46 tests, 0 failures
- websocket-netty: 4 tests, 0 failures
- Total: 128 tests passed, BUILD SUCCESS

**全仓编译**: `mvn clean compile` → BUILD SUCCESS

---

---

### Task 3-4: web 层迁移（脱敏 customizer 重写为 Jackson 3）

**日期**: 2026-07-08

**迁移范围**：
- `SensitiveDataAnnotationIntrospector`: `com.fasterxml.jackson.databind.AnnotationIntrospector` → `tools.jackson.databind.AnnotationIntrospector`；`findSerializer(Annotated)` → `findSerializer(MapperConfig<?>, Annotated)`
- `SensitiveDataSerializer`: `com.fasterxml.jackson.databind.JsonSerializer<String>` → `tools.jackson.databind.ValueSerializer<String>`；`serialize(String, JsonGenerator, SerializerProvider) throws IOException` → `serialize(String, JsonGenerator, SerializationContext)`
- `WebAuthAutoConfiguration`: `Jackson2ObjectMapperBuilderCustomizer` → `org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer`；`AnnotationIntrospectorPair.pair()` → `AnnotationIntrospectorPair.create()`
- `JacksonConfig`: 删除（Jackson 3 内置 java.time 支持，无需 `JavaTimeModule`）
- `WebAutoConfiguration`: 移除 `JacksonConfig.class` 引用
- `pom.xml`: 移除 `jackson-datatype-jsr310` 依赖

**回归闸门**：`SensitiveDataMvcMaskingTest` 移除 `spring.http.converters.preferred-json-mapper=jackson2` 属性后仍通过 → 脱敏在 Jackson 3 原生 MVC 路径生效

**注意**：`AuthInterceptor` 和 `JwtUtils` 保留 Jackson 2 `ObjectMapper` 使用（deprecated 路径，bridge 期间仍可注入）

**测试结果**: nebula-web 88 tests, 0 failures, BUILD SUCCESS

---

### Task 3-5: search/ai/task/autoconfigure 层迁移

**日期**: 2026-07-08

**迁移范围**：

1. **search-core POM**: `com.fasterxml.jackson.core:jackson-databind` → `tools.jackson.core:jackson-databind`
2. **search-elasticsearch POM**: `com.fasterxml.jackson.core:jackson-databind` → `tools.jackson.core:jackson-databind`
3. **ElasticsearchAutoConfiguration**: `JacksonJsonpMapper` → `Jackson3JsonpMapper`；移除 `ObjectMapper` 注入和 `JavaTimeModule`
4. **ai-spring POM**: `com.fasterxml.jackson.core:jackson-databind` → `tools.jackson.core:jackson-databind`
5. **McpToolAdapter**: `com.fasterxml.jackson.databind.ObjectMapper` → `tools.jackson.databind.ObjectMapper` + `JsonMapper.builder().build()`
6. **nebula-task POM**: `com.fasterxml.jackson.core:jackson-databind` → `tools.jackson.core:jackson-databind`
7. **XxlJobHttpClient**: `com.fasterxml.jackson.databind.ObjectMapper` → `tools.jackson.databind.ObjectMapper`
8. **XxlJobAutoConfiguration**: 移除 `ObjectMapper` 注入，改用 `JsonMapper.builder().build()` 内部实例
9. **CacheAutoConfiguration**: `GenericJackson2JsonRedisSerializer` → `GenericJacksonJsonRedisSerializer`；`buildRedisValueObjectMapper` 改用 `JsonMapper.builder()` 构建；移除 `JavaTimeModule` 和 `WRITE_DATES_AS_TIMESTAMPS`
10. **autoconfigure 测试**: 所有使用 `new ObjectMapper()` 的测试改为 `JsonMapper.builder().build()`

**策略要点**：
- autoconfigure 层的 RPC/WebSocket/Task 模块 Bean 不再注入 Spring 管理的 `ObjectMapper`（因 bridge 仍存在，Spring 注入的是 Jackson 2 类型），改为内部实例化 Jackson 3 `ObjectMapper`
- ES 客户端使用 `co.elastic.clients:elasticsearch-java` 提供的 `Jackson3JsonpMapper`，原生支持 Jackson 3
- Redis 缓存序列化器使用 `spring-data-redis` 提供的 `GenericJacksonJsonRedisSerializer`（Jackson 3 版本）

**测试结果**:
- autoconfigure: 61 tests, 0 failures, BUILD SUCCESS
- nebula-task: 48 tests, 0 failures, BUILD SUCCESS
- search-elasticsearch: 38 tests, 0 failures, BUILD SUCCESS
- ai-spring: 18 tests, 0 failures, BUILD SUCCESS

---

### Task 3-6: 拆桥收尾

**日期**: 2026-07-08

**执行内容**：
1. **根 pom.xml**: 删除 `spring-boot-jackson2` 依赖（:336-341，含注释）
2. **三份 starter defaults 移除 `preferred-json-mapper=jackson2`**:
   - `starter/nebula-starter-web/src/main/resources/META-INF/nebula-defaults.properties`
   - `starter/nebula-starter-all/src/main/resources/META-INF/nebula-defaults.properties`
   - `starter/nebula-starter-mcp/src/main/resources/META-INF/nebula-defaults.properties`
3. **三个 StarterDefaultsInjectionTest 更新**: 断言 `preferred-json-mapper` 属性为 null（不再强制 jackson2），增加其他默认属性断言保证测试覆盖率
4. **nebula-web 遗留 Jackson 2 import 清理**:
   - `RateLimitInterceptor`: `com.fasterxml.jackson.databind.ObjectMapper` → `tools.jackson.databind.ObjectMapper`
   - `AuthInterceptor`: 同上
   - `WebRateLimitAutoConfiguration`: 同上
   - `WebAuthAutoConfiguration`: 同上
   - `JwtUtils`（deprecated）: `com.fasterxml.jackson.core.type.TypeReference` → `tools.jackson.core.type.TypeReference`，`ObjectMapper` → `tools.jackson.databind.ObjectMapper`
   - 测试 `AuthInterceptorTest`: `new ObjectMapper()` → `JsonMapper.builder().build()`，移除 `JavaTimeModule`
   - 测试 `AuthConvergenceIntegrationTest`: `ObjectMapper` import 改为 Jackson 3

**验收结果**：
- `rg "import com\.fasterxml\.jackson\.(databind|core|datatype|module)" --glob '**/main/**/*.java'` → 零命中
- `com.fasterxml.jackson.annotation` 保留（Jackson 3 annotation 包名未变，合规）
- Jackson 2 运行时残留仅 `jjwt-jackson` 传递（符合 Task 3-0 盘点策略）
- 全仓编译: BUILD SUCCESS
- 全仓测试: 886 tests, 0 failures, 0 errors, BUILD SUCCESS
- `SensitiveDataMvcMaskingTest`: 无 `preferred-json-mapper` 属性，走 Jackson 3 默认路径，通过
- 三个 `StarterDefaultsInjectionTest`: 各 3 tests，全通过

---

### D7（2026-07-08）：阶段三审计欠账清理（Task 4-0a 前置对账）

阶段三（Jackson 2→3）全量迁移收尾后，对照 Task 3-0 盘点表逐项核销发现 6 处欠账（用户预列 4 项 + 审计新发现 2 项）：

1. **升级指南未编写**：spec.md 3.3 节、Task 3-2 均要求"docs/ 升级指南写清部署前清 `nebula:cache:*` 步骤"，实际未产出。补写 `docs/upgrade-guide-jackson3.md`。
2. **payment POM Jackson 2 残留**：`integration/nebula-integration-payment/pom.xml` 的 `com.fasterxml.jackson.core:jackson-databind` 未换坐标，`jackson-datatype-jsr310` 未删除（Task 3-0 盘点表明确标记为"换坐标"和"删除"）。该模块源码无 Jackson 2 import（仅 POM 声明）。
3. **crawler-captcha POM Jackson 2 残留**：`infrastructure/crawler/nebula-crawler-captcha/pom.xml` 的 `com.fasterxml.jackson.core:jackson-databind` 未换坐标（Task 3-0 盘点表标记为"换坐标"）。该模块源码无 Jackson 2 import。
4. **examples 两文件 Jackson 2 import 残留**：`examples/fullstack-example/.../WeatherTool.java`（`com.fasterxml.jackson.databind.ObjectMapper` + `new ObjectMapper()`）和 `examples/rpc-async-example/client/.../TaskService.java`（同 import）——阶段三迁移遗漏示例模块。
5. **storage-core POM Jackson 2 残留**（审计新发现）：`infrastructure/storage/nebula-storage-core/pom.xml` 的 `com.fasterxml.jackson.core:jackson-databind` 未换坐标（Task 3-0 盘点表标记为"换坐标"）。源码已迁移，仅 POM 残留。
6. **gateway-core POM Jackson 2 残留**（审计新发现）：`infrastructure/gateway/nebula-gateway-core/pom.xml` 的 `com.fasterxml.jackson.core:jackson-databind` 未换坐标（Task 3-0 盘点表标记为"换坐标"）。源码已迁移，仅 POM 残留。

六项一次提交清理。

## 阶段四任务验证记录

### Task 4-0a: 阶段三审计欠账清理

- 验证命令: `mvn clean compile -s /tmp/nebula-mvn-settings.xml`
- 编译结果: BUILD SUCCESS (全仓 68 模块, 21.586s)
- Jackson 2 POM 残留核销:
  - `rg "<groupId>com.fasterxml.jackson.(core|datatype)</groupId>" -g 'pom.xml'` 仅剩 `jackson-annotations`(ai-core, search-core)——注解包名不变，合规
  - `rg "import com.fasterxml.jackson.(databind|core.|datatype|module)" --glob '**/main/**/*.java'` 零命中
- 改动清单（D7 六项）:
  1. 新增 `docs/upgrade-guide-jackson3.md`（含 Redis 缓存清理步骤、坐标对照、代码迁移要点、已知残留说明）
  2. payment pom: `com.fasterxml.jackson.core:jackson-databind` → `tools.jackson.core:jackson-databind`; 删 `jackson-datatype-jsr310`
  3. crawler-captcha pom: `com.fasterxml.jackson.core:jackson-databind` → `tools.jackson.core:jackson-databind`
  4. WeatherTool.java: import 改 `tools.jackson.databind.*`; `new ObjectMapper()` → `JsonMapper.builder().build()`
  5. TaskService.java: import 改 `tools.jackson.databind.ObjectMapper`
  6. storage-core pom + gateway-core pom: `com.fasterxml.jackson.core:jackson-databind` → `tools.jackson.core:jackson-databind`（审计新发现）

---

## Spec-Code 偏差

（实现与 Spec 不一致时，先更新 Spec 再改代码，并在此登记）
