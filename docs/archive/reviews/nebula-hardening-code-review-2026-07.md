# Nebula 框架加固与 Spring Boot 4 升级 代码审查报告

- 审查日期: 2026-07-07
- 审查对象:
  - 分支 `nebula-hardening-a`（30 个提交, A0 前置清理 / A1 机制性事故 / A2 安全 / A3 正确性 / A4 持久化）
  - 分支 `nebula-hardening-b`（基于 a 分支, 新增 16 个提交: C1 前置批 + Spring Boot 4.1.0 升级 `78e595a4`）
- 对照文档: `docs/nebula-framework-review-2026-07.md`（框架审查报告）、`docs/nebula-springboot4-upgrade-design.md`（SB4 升级设计）
- 审查方式: 逐提交 diff 阅读 + 关键实现源码核对 + 依赖树与 Spring Boot 4.1.0 / Spring AI 2.0.0 官方 jar 反编译取证 + 全量编译验证

---

## 一、总体结论

**结论: 有条件通过（Conditional Approve）。**

两个分支的工作质量整体很高: 审查报告中列出的三大机制性事故（EPP 注册失效、`@MessageListener` 静默不消费、RBAC 链路断裂）和绝大多数 P0/P1 安全与正确性问题都得到了真实、正确的修复, 提交信息与实际改动一致, 且多数修复附带了单元测试。全仓 69 个模块 `mvn clean compile` 通过（BUILD SUCCESS）。

但 Spring Boot 4.1 升级批次存在 **2 个 CRITICAL 级新问题**, 均属于"编译通过但运行时静默失效"类型——恰恰是本次加固最想消灭的失效模式:

1. MVC 层 JSON 序列化默认切到 Jackson 3, 导致 A2 批修好的 `@SensitiveData` 脱敏对 Controller 响应**再次静默失效**（敏感数据明文外泄）;
2. gRPC 服务端在新依赖体系下**根本不会启动**, 但客户端默认仍优先走 gRPC, 调用全部静默失败。

上述两项修复完成前, 不建议将 b 分支合入主干或对外发布。

---

## 二、已验证通过的修复（按批次）

### A0 前置清理（5 项, 全部通过）

| 提交 | 内容 | 结论 |
|---|---|---|
| `766cd4fc` | Spring Boot 3.5.8 → 3.5.16 | 通过 |
| `ee8dcfaa` | 移除全局 `--enable-preview`, 改 `<release>21` | 通过, 消除了对运行方 JVM 参数的隐性要求 |
| `40a7f82e` | spring-rabbit/amqp-client 交还 Boot BOM 托管 | 通过 |
| `9e648be1` | 删除框架 JAR 内打包的 `application.yml` | 通过, 消除了框架配置污染应用配置的反模式 |
| `76264372` | 删除 nebula-task 死文件 `spring.factories` | 通过 |

### A1 三大机制性事故（3 项, 全部通过）

- **T-A1-1（`bb398eeb`）EPP 注册迁回 `spring.factories`**: 已核实 `NebulaStarterDefaultsPostProcessor` 与 `NebulaMcpEnvironmentPostProcessor` 注册在 `org.springframework.boot.env.EnvironmentPostProcessor` 键下, 原来两个 Spring Boot 不读取的 `.imports` 文件已删除。Starter 默认值注入链路恢复。修复正确。（注: 该键在 SB4.1 已是废弃兼容键, 见问题 M-2。）
- **T-A1-2（`afbb86de`）`MessageHandlerProcessor` 双注解扫描**: 已核实同时扫描 `@MessageListener`（新）与 `@MessageHandler`（废弃）, 属性通过 `HandlerAttributes` record 归一化。新注解静默不消费的事故消除。
- **T-A1-3（`afeb2390`）RBAC 链路补齐**: 新增 `JwtAuthenticationFilter`（只填充 `SecurityContext`、不拦截, opt-in 注册）, `SecurityAspect` 切点补 `@within` 使类级注解生效。设计克制, 不破坏存量应用的自建认证。修复正确。

### A2 安全批（8 项, 全部通过, 遗留 2 个衍生问题见问题清单）

- `350c9edb` OPTIONS 认证绕过: 改用 `CorsUtils.isPreFlightRequest()`, 只放行真预检。通过。
- `2a7a279e` 响应缓存跨用户泄露: 改 `@ResponseCacheable` 注解白名单 + 排除带 `Authorization`/`Cookie` 的请求 + 默认关闭。通过。
- `64cf0320` `@SensitiveData` 脱敏挂到主 `ObjectMapper`（`AnnotationIntrospectorPair`）: 修复本身正确, 但在 SB4.1 下被 MVC 换用 Jackson 3 架空, 见 **C-1**。
- `640c20ce` Redis 反序列化 RCE: 多态白名单（`java.util.*`/`java.time.*`/`io.nebula.*` + 用户配置包）替代全开放多态。通过。
- `e3aa7efa` `/rpc` 任意类加载: `RpcRequest.parameterTypes` 由 `Class<?>[]` 改 `String[]`, 服务端按声明类型字符串匹配, 不再 `Class.forName`。通过（gRPC 侧 `1adf0fe5` 同步修复）。
- `16b48837` `/rpc` 可选 token 鉴权（默认关）: 服务端实现正确, 但客户端未注入 token, 见 **H-1**; 比较方式见 **M-1**。
- `c62e28a0` ES 认证被覆盖: 三次 `setHttpClientConfigCallback` 合并为一次。通过, 且 SB4 升级重写为 Rest5Client 后该修复语义保留（已核实现行代码 107–117 行凭据设置在单一 callback 内）。
- `be1d6b84`/`01b603c9` 裁撤无鉴权 xxl-job REST 端点、性能端点改 opt-in。通过。

### A3/A4 正确性批（9 项, 全部通过, 遗留 1 个衍生问题）

- `8214b58f` 分布式锁: `tryLock` 时间单位统一为毫秒换算后传 `TimeUnit.MILLISECONDS`, `tryExecute` 不再吞业务异常。通过。
- `f73b31bd` foundation 五个运行期缺陷（含 `TreeUtil`/`Result` 等）。通过。
- `f0d87932` 缓存 key 命名空间化 + `SCAN` 替代 `KEYS`: `clear()` 误清全库的事故消除, 返回键正确剥前缀。通过（健康检查键未加前缀, 见 **M-3**）。
- `8498a42c` RPC 目标地址随请求传递: `ThreadLocal targetOverride` + `runWithTarget` 同步执行。**已专项核实异步路径**: `ServiceDiscoveryRpcClient.callAsync` 是把"发现 + callWithTarget"整体包进 `supplyAsync`, ThreadLocal 的写与读在同一执行线程, 无跨线程丢失问题。通过。
- `572ff1c4` 加权 LB 配置解析 + Nacos Reactive 注册。通过。
- `1b194a2a` RabbitMQ 毒消息重投限制（`requeue = !isRedeliver()`）+ 路由键对齐 + headers 空安全。通过。
- `ddf4a3bf`/`e26395f6`/`c51910ee`/`f1e2f451` 持久化四连修: mapper 扫描可配置、主数据源 fail-fast、`ServiceImpl` 假实现改真实现、读写分离路由接通。功能正确（`findByField` 列名拼接风险见 **H-2**）。

### C1 前置批（11 项, 全部通过）

- `b75ac158` 拦截器顺序显式化（`InterceptorOrders`: Logging < RateLimit < Auth < Cache < Performance）, 限流先于认证、缓存不再绕过限流。通过。
- `10d1b520` XFF 可信代理解析（`ClientIpResolver` + Gateway 侧 `ReactiveClientIpResolver`）, 默认不信任转发头。通过。
- `c52e0a7c` 双 MQ 并存统一选主, 消除双 `@Primary` 崩溃。通过。
- `7126da52` 读写分离静态检测写事务, 拒绝 `@Transactional` 写事务切读库。通过。
- `45e2cf69` Netty WebSocket 会话注册移至握手完成 + 空闲清理。通过。
- `3552f1de` RabbitMQ tagged 绑定路由键对齐 + tagged 路径毒消息防护。通过。
- `4e4784bd` 多级缓存: 跨节点失效广播（`CacheInvalidationBroadcaster`）+ single-flight 防击穿 + `NULL_SENTINEL` 防穿透。实现正确。
- `ed6cba87` 移除 25 个实现类残留 stereotype 注解。通过。
- `e13eeb99` 收紧四处不安全默认值（nacos 凭据/xxljob 令牌/ws 来源/cors 凭据）。通过。
- `7a8e6ee1` xxl-job 子任务失败如实上报 FAIL。通过。
- `e403d869` Chroma 向量库过滤/阈值生效（SSA-1）。通过。

### B 批 Spring Boot 4.1 升级（`78e595a4`, 部分通过）

已验证正确的部分:

- 父 POM 升级 4.1.0, Spring Cloud 2025.1.2, Spring AI 2.0.0, MyBatis-Plus 3.5.16（spring-boot4-starter）, Redisson 4.6.1（redisson-spring-boot-starter 适配）, JJWT 0.13.0 等版本组合可编译、依赖收敛无冲突。
- ES 客户端重写为 elasticsearch-java 9.4.2 + `Rest5Client`（HttpComponents 5）: URI 解析、超时/连接池、Basic 认证（单一 callback, 保留 A2 修复语义）、SSL 均正确迁移; 被排除的三个 Boot 自动配置类名已更新为 SB4 新包名（`org.springframework.boot.elasticsearch.autoconfigure.*` 等, 与 4.1.0 jar 实际类名核对一致）。
- Spring AI 2.0 适配: `OpenAiApi` 移除后改为 options 直传 apiKey/baseUrl, ChatModel/EmbeddingModel 构建正确; `SpringAIAutoConfigurationFilter` 排除的 7 个类名与 Spring AI 2.0.0 jar 内 `AutoConfiguration.imports` 逐一核对**全部仍然有效**。
- Jackson 2 兼容层: 引入 `spring-boot-jackson2`, 现有 `ObjectMapper` 生态（缓存序列化、RPC、ES mapper 等直接注入 `ObjectMapper` 的场景）继续工作。
- `MessageHandlerProcessor` 等对 Boot 4 包迁移的适配正确。

存在问题的部分: 见下方 **C-1**（Jackson 3 架空脱敏）与 **C-2**（gRPC 服务端不启动）。

---

## 三、问题清单

### CRITICAL

#### C-1. SB4.1 下 MVC 默认使用 Jackson 3, `@SensitiveData` 脱敏与日期定制对 Controller 响应静默失效 -- **已修复（Task 1 + Task 2）**

- 位置: `application/nebula-web/.../WebAuthAutoConfiguration.java`（脱敏 customizer）、`application/nebula-web/src/main/java/io/nebula/web/config/JacksonConfig.java`（JSR310 customizer）
- 证据链:
  1. SB 4.1 的 `spring-boot-starter-web` 传递引入 `spring-boot-starter-jackson`（Jackson 3, `tools.jackson.core:jackson-databind:3.1.4`）, 已在依赖树确认;
  2. 反编译 `spring-boot-http-converter-4.1.0.jar`: Jackson 2 转换器的启用条件是 `PreferJackson2OrJacksonUnavailableCondition`——只有显式配置 `spring.http.converters.preferred-json-mapper=jackson2` 或 classpath 无 Jackson 3 时才生效; 两个条件当前都不满足, MVC 实际使用 Jackson 3 的 `JacksonJsonHttpMessageConverter`;
  3. 全仓检索无任何 `preferred-json-mapper` 配置;
  4. 脱敏与 `JavaTimeModule` 均只注册在 **Jackson 2** 的 `Jackson2ObjectMapperBuilderCustomizer` 上, 对 Jackson 3 的 `JsonMapper` 不生效。
- 影响: 所有走 Controller 的 JSON 响应不再经过 Jackson 2 `ObjectMapper` —— A2 批修复的 `@SensitiveData` 脱敏在 HTTP 出口**完全失效**, 手机号/身份证等敏感字段明文返回; 自定义日期格式/时区行为同时改变。现有测试 `SensitiveDataMaskingCustomizerTest` 只测 Jackson 2 mapper 本体, 测不到 MVC 转换器路径, 因此 750 条测试全绿也发现不了。
- 修复建议（二选一, 推荐 1 先行、2 跟进）:
  1. 立即在框架默认值中注入 `spring.http.converters.preferred-json-mapper=jackson2`（放入 `NebulaStarterDefaultsPostProcessor` 的注入集或各 starter 的 `nebula-defaults.properties`）, 把 MVC 拉回 Jackson 2, 与现有定制生态一致;
  2. 在 T-B3-2（Jackson 2→3 全量迁移）中将脱敏/JSR310 定制同步实现为 Jackson 3 版本后再切换。
  另外必须补一条 **MockMvc 级集成测试**: 断言带 `@SensitiveData` 字段的 Controller 响应体是脱敏后的值, 防止再次静默回归。

#### C-2. gRPC 服务端在新依赖体系下不会启动, 但客户端默认仍优先 gRPC -- **已修复（Task 3 + Task 4 + Task 5）**

- 位置: `infrastructure/rpc/nebula-rpc-grpc/pom.xml`（仅引入 `spring-grpc-core:1.1.0`）、`autoconfigure/.../GrpcRpcAutoConfiguration.java`
- 证据链:
  1. 旧实现依赖 net.devh starter 的 `@GrpcService` 扫描与 Server 生命周期管理, 本次升级已移除;
  2. 新依赖只有 `spring-grpc-core`, **没有** `spring-grpc-server-spring-boot-starter`/autoconfigure（本地仓库核实只有 core 构件）—— 没有任何组件会创建并启动 `io.grpc.Server`, 也没有组件处理 `@GlobalServerInterceptor`;
  3. `GrpcRpcServer` 现在只是一个 `BindableService`（`GenericRpcServiceGrpc.GenericRpcServiceImplBase` 子类）Bean, 无人绑定端口;
  4. `GrpcRpcAutoConfiguration` 中 `nebula.rpc.grpc.server.enabled` / `client.enabled` 均 `matchIfMissing = true`——只要 `nebula.rpc.grpc.enabled=true`, `RpcDiscoveryAutoConfiguration` 的优先级逻辑会让客户端优先走 gRPC, 而对端无监听, 调用全部失败。
- 说明: `tasks.md` 中 `T-B2-2｜gRPC 从 net.devh 转 Spring gRPC` 确实是未勾选状态（团队已知未完成）, 但提交信息"升级 Spring Boot 4.1 全栈"未体现该能力已断, 且失效方式是运行时静默的, 与本次加固要消灭的失效模式相同。
- 修复建议:
  1. 完成 T-B2-2: 引入 `spring-grpc-spring-boot-starter`（server + client autoconfigure）, 由 Spring gRPC 托管 Server 生命周期、注册 `BindableService` 与全局拦截器, 端口配置桥接 `nebula.rpc.grpc.port` → `spring.grpc.server.port`;
  2. 在完成之前的过渡期, 把 gRPC server/client 的 `matchIfMissing` 改为 `false`, 或在 `GrpcRpcAutoConfiguration` 中检测到无 Server 托管能力时 **fail-fast 抛出明确异常**, 禁止静默降级。

### HIGH

#### H-1. `HttpRpcClient` 不注入 `X-Nebula-Rpc-Token`, 开启服务端 RPC 鉴权后框架自身调用全部 401 -- **已修复（Task 6）**

- 位置: `infrastructure/rpc/nebula-rpc-http/.../client/HttpRpcClient.java:324`（仅设置 `X-Request-ID`）; 服务端校验在 `HttpRpcController.java:52-56`。
- 影响: T-A2-4b 的 token 鉴权是"只做了锁, 没配钥匙"——一旦应用配置 `nebula.rpc.http.server.auth-token`, 框架内建的 HTTP RPC 客户端（含服务发现路径）无法通过认证, 服务间调用全断。该功能当前默认关, 所以未爆发, 但等于不可用。
- 建议: `HttpRpcProperties` 增加 client 侧 token 配置（或复用同一值）, `HttpRpcClient` 发请求时携带 `X-Nebula-Rpc-Token`; 补一条"开启鉴权后客户端可调通"的集成测试。

#### H-2. `ServiceImpl.findByField` 列名直接拼入 SQL, 存在注入风险 -- **已修复（Task 7）**

- 位置: `infrastructure/data/nebula-data-persistence/.../service/impl/ServiceImpl.java`（T-A4-4 新实现, `queryWrapper.eq(field, value)` 系列）。
- 影响: MyBatis-Plus 的 `QueryWrapper.eq(String column, ...)` 第一个参数是**原样拼接的列名**。`findByField/findOneByField` 是公开泛型 API, 若上层把用户输入当字段名传入（如动态排序/筛选场景）, 即构成 SQL 注入。
- 建议: 对 `field` 做白名单校验——仅允许 `[A-Za-z0-9_]`（正则拒绝其余）, 或通过 `TableInfoHelper` 校验字段确属该实体的列; 在 Javadoc 中明确"禁止传入用户输入"。

### MEDIUM

#### M-1. `/rpc` token 比较使用 `String.equals`, 非常量时间 -- **已修复（Task 8）**

- 位置: `HttpRpcController.java:54`。
- 影响: 理论上可被计时侧信道逐字节猜测 token。内网场景风险有限, 但改造成本极低。
- 建议: 改用 `MessageDigest.isEqual(authToken.getBytes(UTF_8), provided.getBytes(UTF_8))`。

#### M-2. EPP 注册在 SB4.1 使用的是"废弃兼容键", 存在未来版本再次静默失效的风险 -- **已修复（Task 9）**

- 位置: `autoconfigure/.../META-INF/spring.factories`（键 `org.springframework.boot.env.EnvironmentPostProcessor`）。
- 证据: 反编译 `spring-boot-4.1.0.jar` 的 `SpringFactoriesEnvironmentPostProcessorsFactory`, 确认存在 `loadDeprecatedPostProcessors` 兼容加载旧键, **当前功能正常**; 但新正主键是 `org.springframework.boot.EnvironmentPostProcessor`, 旧键随时可能在后续大版本移除——届时会精确复刻 A1 修掉的那类静默失效。
- 建议: 双注册过渡（同一实现类可同时登记在新旧两个键下, 新接口签名一致）, 并让 `NebulaStarterDefaultsPostProcessor` 改为 implements 新包接口; 已有的"defaults 注入生效"集成测试保留作回归哨兵。

#### M-3. `DefaultCacheManager.isAvailable()` 健康检查键未加命名空间前缀 -- **已修复（Task 10）**

- 位置: `infrastructure/data/nebula-data-cache/.../DefaultCacheManager.java:708`（`set("health:check", ...)`）。
- 影响: 与 T-A3-3 的"所有键收进 `nebula:cache:` 命名空间"原则不一致, 向共享 Redis 库写入裸键; 另 `evictionCount` 统计为非原子自增, 并发下少计。均不影响正确性主线。
- 建议: 改为 `keyPrefix + "health:check"`; 统计字段改 `LongAdder`。

#### M-4. ES `ssl-verification-enabled=false` 的 trust-all 无环境防护 -- **已修复（Task 11）**

- 位置: `ElasticsearchAutoConfiguration.createSSLContext()`（信任所有证书的 `X509TrustManager`）。
- 影响: 与爬虫模块"trustAll 仅限非生产 profile, 生产自动拒绝"的既定安全策略不一致, 生产环境可被配置绕过证书校验。
- 建议: 对齐爬虫模块策略——检测 active profile, 生产环境忽略该开关并告警。

### LOW

- **L-1** `JwtAuthenticationFilter.readStringList` 末尾 `filter(Objects::nonNull)` 冗余（前一步 `map(String::trim)` 后不可能为 null）, 可删。 -- **已修复（Task 12）**
- **L-2** `ElasticsearchAutoConfiguration.elasticsearchClient()` 直接对注入的共享 `ObjectMapper` 调 `registerModule(new JavaTimeModule())`, 副作用外溢到全局 mapper; 建议 `objectMapper.copy()` 后再注册。 -- **已修复（Task 12）**
- **L-3** `ServiceDiscoveryRpcClient.callAsync` 使用无执行器的 `supplyAsync`（commonPool）, 与 `HttpRpcClient.callAsync` 用自有 executor 不一致; 建议注入统一 executor。 -- **已修复（Task 12）**
- **L-4** `78e595a4` 单提交约 87 文件, 混合了"版本升级/ES 重写/AI 适配/gRPC 临时替换"多个关注点, 回滚与 bisect 粒度差; 后续大升级建议按 tasks.md 的任务边界拆提交。 -- 流程建议, 不涉及代码修改

---

## 四、验证记录

| 验证项 | 结果 |
|---|---|
| `mvn clean compile`（b 分支, 全仓 69 模块） | BUILD SUCCESS（15.6s, 增量; 此前全量亦通过） |
| SB4.1 `spring-boot-http-converter` Jackson2 启用条件反编译 | 确认需 `preferred-json-mapper=jackson2`, 当前未配置 → C-1 成立 |
| SB4.1 `spring-boot` EPP 加载机制反编译 | 确认旧键走 `loadDeprecatedPostProcessors` 兼容 → 当前可用, M-2 成立 |
| Spring AI 2.0.0 自动配置类名 vs `SpringAIAutoConfigurationFilter` 排除清单 | 7/7 类名一致, 过滤仍有效 |
| 本地仓库 `org/springframework/grpc` 构件清单 | 仅 `spring-grpc-core`, 无 server starter → C-2 成立 |
| `ServiceDiscoveryRpcClient` 异步路径 ThreadLocal 可见性 | 发现+调用同线程执行, 无问题（排除一项疑似缺陷） |
| 单元测试 | 团队记录 750 条全绿（tasks.md T-B1-6）; 本次审查未重跑全量测试, 但确认现有测试覆盖不到 C-1 的 MVC 路径 |

---

## 五、合入建议

1. **合入前必须完成**: C-1（一行配置 + 一条 MockMvc 回归测试）、C-2（过渡期 fail-fast 或默认关闭, 完整修复走 T-B2-2）。
2. **合入前强烈建议**: H-1（token 客户端注入, 否则该功能形同虚设）、H-2（列名白名单, 5 行改动）。
3. **可随后续批次处理**: M-1 ~ M-4 建议纳入 EPIC-C2 收尾批; L 级酌情。
4. 流程建议: 后续在 CI 增加一个最小 Web 应用的启动冒烟测试（起容器 → 打一个带脱敏字段的接口 → 断言响应）, 专门捕获"编译通过但装配/序列化静默失效"这一类问题——本次两个 CRITICAL 都属于此类, 单元测试与编译均无法发现。
