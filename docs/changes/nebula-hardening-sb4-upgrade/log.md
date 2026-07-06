# 变更日志：Nebula 硬化 + 升级 SB4.1

> 随开发实时追加。只记有复用价值的技术决策、踩坑、知识发现、Spec-Code 偏差，不记流水账。

---

## 技术决策

- **2026-07-06｜先修后升，全模块在范围**：用户确认新项目依赖全部模块，故按严重级别全局排序修复；先在 3.5.16 上做工作流 A（阻断级）再升 4.1，避免带未修的机制性失效跨版本。
- **2026-07-06｜ImportFilter 直接删除（Spec Q1）**：`NebulaAutoConfigurationImportFilter` 当前经 `.imports` 注册本就不生效（字节码已证），且其无条件排除 DataSource/MyBatis-Plus 行为过激，删除零风险。
- **2026-07-06｜密码哈希新增而非改旧（Spec Q6）**：`CryptoUtils.encrypt` 保留并 `@Deprecated`，新增 `hashPassword`(BCrypt)，避免破坏已落库的旧哈希。

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

## 待补验证（环境受限）

- **T-A0-3 RabbitMQ live 收发**：本地无 Docker/RabbitMQ，只验证了版本对齐(spring-rabbit 3.1.0→3.2.8, 与 spring-amqp 3.2.8 一致)与编译；收发端到端需在带 broker 的 CI/环境补跑。同类:凡验证需要 Redis/MQ/Nacos/ES 外部服务的 task, 本地只做编译级验证, 行为级验证转 CI。

## Spec-Code 偏差

_（开发中追加：实现与 Spec 不一致时，先更新 Spec 再改代码，并在此记录）_
