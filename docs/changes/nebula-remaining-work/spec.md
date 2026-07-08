# Nebula 加固与 SB4 升级 -- 剩余工作总体规格（nebula-remaining-work）

> 状态：confirmed（2026-07-07 Q1-Q7 全部拍板，待用户对整体方案最终放行）
> 创建日期：2026-07-07
> 复杂度：复杂
> 目标分支：`nebula-hardening-b`
> 关联文档：
> - `docs/nebula-hardening-code-review-2026-07.md`（代码审查报告，剩余问题来源之一）
> - `docs/changes/nebula-hardening-sb4-upgrade/tasks.md`（原始任务清单，未勾选项来源）
> - `docs/changes/nebula-review-fixes/spec.md` + `tasks.md`（阶段一详细规格，本文引用不重复）
> - `docs/nebula-framework-review-2026-07.md`（框架审查报告，EPIC-C2 各项的原始出处）

## 1. 背景与目标

框架加固（EPIC-A/C1）与 Spring Boot 4.1 升级（EPIC-B）主体已完成（807 测试全绿），但重构**尚未收官**。剩余工作共四个阶段：

| 阶段 | 内容 | 来源 | 状态 |
|------|------|------|------|
| 阶段一 | 审查问题修复（2 CRITICAL + 2 HIGH + 4 MEDIUM + 3 LOW，含 T-B2-2 gRPC 迁移收尾） | 审查报告 | 规格已立（review-fixes），未开工 |
| 阶段二 | EPIC-C2 治理收尾（死配置清理、错误码收敛、认证两套收敛、孤儿 DTO 清理、自动装配集成测试、CLAUDE.md 行号同步） | sb4-upgrade tasks.md:401 | 未拆分（本文档拆分） |
| 阶段三 | Jackson 2 → 3 全量迁移（T-B1-3 延后项，主代码 61 个文件） | sb4-upgrade tasks.md:309 | 未拆分（本文档概要拆分） |
| 阶段四 | T-D-1 proud-day 接入 Nebula 持久化 | sb4-upgrade tasks.md:207 | 外部仓库，等用户提供后启动 |

执行顺序：**阶段一 → 阶段二 → 阶段三**（阶段三与二可部分并行，但错误码/认证收敛涉及 foundation/security 改动，先做完可减少 Jackson 迁移的返工面）；阶段四独立。

做完后的可验证效果：审查报告全部问题关闭；配置项"定义即消费"无死配置；错误码与认证各只剩一套体系；框架主代码不再依赖 `com.fasterxml.jackson`（Jackson 2）；全仓编译与测试通过。

## 2. 代码现状（Research）

### 2.1 阶段一（审查修复）现状

详见 `docs/changes/nebula-review-fixes/spec.md` 第 2 节（已完成取证），此处不重复。关键点：MVC 默认走 Jackson 3 致脱敏静默失效；gRPC 服务端无宿主；HTTP RPC 客户端缺 token 注入；`findByField` 列名拼接。

### 2.2 阶段二（EPIC-C2）现状

**a) 死配置**（框架审查报告 :109 列举，本次已抽查核实）：

- `HttpRpcProperties.ClientConfig` 的 `retryCount`/`retryInterval`/`compressionEnabled`/`maxConnections`/`maxConnectionsPerRoute`/`keepAliveTime`/`writeTimeout`：仅被 `HttpRpcAutoConfiguration.java:174-176` 与 `NebulaDiagnosticEndpoint.java:145` 读来做**诊断展示**，未参与 `RestClient` 实际构建——典型"仪表盘显示了不存在的引擎参数"。
- `GrpcRpcProperties.ServerConfig` 的线程/流控参数（`maxInboundMessageSize`/`keepAliveTime`/`keepAliveTimeout`/`permitKeepAliveWithoutCalls`/`maxConcurrentCalls`）：net.devh 时代遗留，新体系（Boot gRPC starter）下无消费者（阶段一 Task 4 只处理 port 桥接）。
- `SecurityProperties` 的 `anonymousUrls`：全仓无读取点；`rbac.*` 仅 `SecurityAutoConfiguration.java:94` 诊断展示。
- `NacosProperties.heartbeatInterval/heartbeatTimeout`（`config/NacosProperties.java:93,101`）：仅诊断展示（`NacosDiscoveryAutoConfiguration.java:94`），Nacos SDK 心跳自管理。
- `RedisLockProperties.defaultWaitTime`（`RedisLockProperties.java:34`）：待核实消费点（A3 修复 tryLock 后可能已接通，盘点任务中确认）。
- CORS 两套配置与悬空 Bean（框架审查报告 :173）：`application/nebula-web/.../WebCoreAutoConfiguration.java` 内 CORS 配置与 gateway 侧并存，需盘点。
- 注意：审查报告 :109 提到的"缓存 keyPrefix 死配置"已在 T-A3-3 修复中接通，**不在**本次清理范围。

**b) 错误码三套体系**（框架审查报告 CF-40）：

- `core/nebula-foundation/src/main/java/io/nebula/core/common/result/Result.java`：字符串码（`"SUCCESS"` 等）。
- `core/nebula-foundation/src/main/java/io/nebula/core/common/enums/ResultCode.java`：枚举四位码（`"0000"` 等），报告指出 `getByCode()` 存在查不到的缺陷。
- `core/nebula-foundation/src/main/java/io/nebula/core/common/constant/Constants.java`：数字码常量。
- 三套互不一致。`Result` 是全框架 REST 统一返回封装（`nebula-foundation` 属"不可随意修改"核心模块），收敛必须保持对外契约兼容。

**c) 认证两套体系**（框架审查报告 :46,173）：

- `nebula-web`：`AuthInterceptor`（`application/nebula-web/.../interceptor/AuthInterceptor.java:25`）自行解析 JWT → 写 `AuthContext`（web 模块自有 ThreadLocal）。
- `nebula-security`：`JwtAuthenticationFilter`（A1 新增）解析 JWT → 写 `SecurityContext`（供 `SecurityAspect` RBAC 判定）。
- 同一请求两次解析 JWT、两个互不相通的上下文。A1 修复打通了 security 链路，但未与 web 链路合并。

**d) xxl-job 孤儿 DTO**（sb4-upgrade tasks.md:438）：

- `application/nebula-task/src/main/java/io/nebula/task/xxljob/dto/` 下 4 个 DTO，已核实：`XxlJobResult` 仍被 `XxlJobRegistryService.java:169,205` 使用（保留）；`XxlJobExecuteRequest`/`XxlJobLogRequest`/`XxlJobLogResult` 自 T-A2-5 删除 `XxlJobTaskService` 后无任何引用（孤儿，删除）。

**e) 自动装配集成测试缺口**：现有测试以单元测试为主，AutoConfiguration 的条件装配（enabled 开关、ConditionalOnClass、defaults 注入）缺少 `ApplicationContextRunner` 级验证——A1 三大事故（EPP 不生效、Bean 不装配）正是这类测试能拦住的。

**f) 文档债**：`CLAUDE.md`/`AGENTS.md` 中"关键实现引用"章节的行号在多轮重构后已大面积过期（框架审查报告 :181 点名）。

### 2.3 阶段三（Jackson 2→3）现状

- 主代码 61 个文件 import `com.fasterxml.jackson.*`（rg 统计，不含测试），分布在 foundation(JsonUtils)、web(脱敏/JacksonConfig)、cache(Redis 序列化)、messaging、rpc(http/grpc 序列化)、search(ES JacksonJsonpMapper)、ai、task 等模块。
- 当前靠根 POM `spring-boot-jackson2` 桥模块 + （阶段一注入的）`preferred-json-mapper=jackson2` 维持 Jackson 2 全链路。
- Jackson 3 坐标 `tools.jackson.*`（包名同变），`JsonMapper` 取代 `ObjectMapper` 为主入口；ES `JacksonJsonpMapper` 9.x 已有 Jackson 3 变体；Spring AI 2.0 内部已用 Jackson 3（T-B3-1 记录 ChromaApi 为 Jackson 3）。
- 除代码 import 外，POM 显式声明的 Jackson 2 系依赖也在迁移范围（2026-07-07 外部审查补充）：根 POM `jjwt-jackson`(:290)/`spring-boot-jackson2`(:355)、security/web 模块的 `jjwt-jackson:0.12.3`、payment 的 `jackson-databind`——由 Task 3-0 盘点定策（jjwt 的 Jackson 3 变体可用性待核验）。
- 迁移完成的标志：根 POM 移除 `spring-boot-jackson2` 依赖、defaults 移除 `preferred-json-mapper` 注入、主代码零 `com.fasterxml.jackson.databind/core/datatype` import、POM 无未裁决的 Jackson 2 系显式依赖。
- 口径修正（2026-07-08）：Jackson 3 的注解包**保留** `com.fasterxml.jackson.annotation` 原包名（jackson-annotations 3.x 兼容设计），`@JsonProperty`/`@JsonIgnore` 等 import 无需也不应改动；因此"零 com.fasterxml.jackson 命中"不成立，验收以 databind/core/datatype 三个包前缀为准。

### 2.4 阶段四（T-D-1）现状

proud-day 仓库：`/Users/andy/DevOps/SourceCode/business-projects/proud-day-projects/proud-day-backend`（Q6 已提供）。现状调研已完成，详细规格立于该仓库 `docs/changes/nebula-persistence-adoption/spec.md`（状态 confirmed，Q1/Q2/Q3 已拍板）。关键点：33 个 Mapper 继承 MP 原生 BaseMapper 可零改动迁移；pom 显式覆盖 `mybatis-plus-spring-boot3-starter:3.5.12` 迁移必删；启动条件为本仓库 Task 4-0（版本号升 2.1.0-SNAPSHOT）+ 阶段一完成。

## 3. 功能点

- [ ] F1（阶段一）：审查报告 11 项问题修复 —— 详见 `nebula-review-fixes/spec.md`，13 个任务。
- [ ] F2（阶段二 a）：死配置全量盘点与清理 —— 逐项"接通或删除"，产出盘点表记入 log。
- [ ] F3（阶段二 b）：错误码收敛为一套 —— `ResultCode` 为唯一事实源，`Result`/`Constants` 兼容过渡。
- [ ] F4（阶段二 c）：认证收敛为一套 —— JWT 解析只发生一次（security 层），web 层复用其结果。
- [ ] F5（阶段二 d）：删除 3 个 xxl-job 孤儿 DTO。
- [ ] F6（阶段二 e）：为 8 个核心 AutoConfiguration 补 `ApplicationContextRunner` 条件装配测试。
- [ ] F7（阶段二 f）：CLAUDE.md/AGENTS.md 行号引用全量同步。
- [ ] F8（阶段三）：Jackson 2→3 全量迁移，移除 jackson2 桥与 preferred-json-mapper 注入。
- [ ] F9（阶段四）：proud-day 接入 Nebula 持久化（待仓库提供后另立规格）。

## 4. 设计决策与业务规则

1. **死配置处理原则**："定义即消费"。每个配置项二选一：(a) 接通——把值真正用于组件构建；(b) 删除——连同诊断展示一起移除，并在 CHANGELOG 标注破坏性变更。诊断端点只允许展示真实生效的参数。倾向：HTTP RPC 客户端连接池/超时参数**接通**（RestClient 构建时应用）；gRPC ServerConfig 遗留参数**删除**（官方 `spring.grpc.server.*` 已覆盖同能力）；`anonymousUrls` **删除**（`nebula.web.auth.ignore-paths` 已提供同能力）；Nacos 心跳参数**删除**（SDK 自管理）。
2. **错误码收敛方向**：`ResultCode` 枚举为唯一事实源。`Result` 保持对外字段与现有字符串码**不变**（避免破坏下游断言），新增 `Result.of(ResultCode)` 工厂；`Constants` 中的错误码常量标 `@Deprecated` 指向 `ResultCode`，下一主版本删除；修复 `ResultCode.getByCode()` 缺陷并补测试。`nebula-foundation` 属核心模块：只增不改现有公开方法签名。
3. **认证收敛方向**：单一解析点在 `nebula-security`（`JwtAuthenticationFilter`，Filter 先于 Interceptor 执行）。`AuthInterceptor` 不再自行解析 JWT，改为读取 `SecurityContext` 判定是否放行，并将结果桥接进 `AuthContext`（保留 `AuthContext` 公开 API 不变，内部变为 SecurityContext 的适配视图）。`JwtAuthenticationFilter` 默认注册条件随之调整：`nebula.web.auth.enabled=true` 时自动启用（web starter defaults 注入），保持"引入即用"。
4. **Jackson 3 迁移策略**：自底向上按依赖序迁移（foundation → cache/messaging/rpc → web/search/ai/task → autoconfigure），每模块一个任务；全程保持编译绿，最后一步才拆桥（移除 `spring-boot-jackson2` 与 defaults 注入）。脱敏 customizer 在此阶段重写为 Jackson 3 版本，MockMvc 哨兵测试（阶段一 Task 2）换属性后必须仍绿——它是本阶段最重要的回归闸门。
5. **阶段边界纪律**：阶段二、三均不夹带新功能；发现新问题记 log 不顺手修（除非一行级）。

## 5. 数据变更

无数据库表结构变更。

## 6. 接口/配置变更（阶段二、三部分）

| 操作 | 项 | 变更内容 |
|------|------|----------|
| 删除 | `nebula.security.anonymous-urls` | 死配置，用 `nebula.web.auth.ignore-paths` 替代 |
| 删除 | `nebula.discovery.nacos.heartbeat-interval/-timeout` | 死配置，SDK 自管理 |
| 删除 | `nebula.rpc.grpc.server.*` 遗留线程/流控参数 | 由 `spring.grpc.server.*` 覆盖 |
| 接通 | `nebula.rpc.http.client.*` 连接池/超时/重试参数 | 真正应用到 RestClient 构建 |
| 废弃 | `Constants` 错误码常量 | `@Deprecated` 指向 `ResultCode` |
| 行为变更 | `AuthInterceptor` | 不再自行解析 JWT，复用 SecurityContext |
| 删除（阶段三收尾） | 根 POM `spring-boot-jackson2`、defaults 中 `preferred-json-mapper` | Jackson 3 迁移完成后拆桥 |

## 7. 影响范围

- 阶段一：见 `nebula-review-fixes/spec.md` 第 7 节。
- 阶段二：`core/nebula-foundation`（错误码，核心模块谨慎）、`core/nebula-security` + `application/nebula-web`（认证收敛）、`infrastructure/rpc/nebula-rpc-http|grpc`、`infrastructure/discovery/nebula-discovery-nacos`、`infrastructure/lock/nebula-lock-redis`、`application/nebula-task`、`autoconfigure/nebula-autoconfigure`（诊断端点+测试）、`CLAUDE.md`/`AGENTS.md`。
- 阶段三：全仓 61 个主代码文件 + 根 POM + starter defaults。

## 8. 风险与关注点

1. **认证收敛是本批最高风险项**：涉及"不可随意修改"的 security 核心模块与所有 Web 应用的登录链路。必须保持 `AuthContext` 公开 API 兼容，且 Filter/Interceptor 执行顺序有集成测试锁定。
2. **错误码收敛的兼容陷阱**：下游可能字符串匹配 `result.getCode().equals("SUCCESS")`，`Result` 现有码值绝不能变。
3. **死配置删除是破坏性变更**：用户 yaml 中已配置这些项的，Boot 默认忽略未知属性不会报错，但行为预期落空——CHANGELOG 必须逐项列出。
4. **Jackson 3 迁移体量大**（61 文件），Redis 缓存序列化格式兼容性需专项验证（Jackson2 写入的缓存被 Jackson3 读取的滚动升级场景），必要时约定"升级需清缓存"并写入迁移指南。
5. 阶段三开始前必须确认阶段一 Task 2 的 MockMvc 哨兵测试已存在，否则脱敏回归无闸门。

## 9. 待澄清

> 默认按"推荐"执行，异议请在对应阶段开工前提出。

- [x] Q1：阶段顺序一（审查修复）→ 二（治理）→ 三（Jackson3）？—— 推荐此序，二三不强并行
- [x] Q2：死配置处理倾向（HTTP RPC 接通 / gRPC遗留参数删除 / anonymousUrls 删除 / Nacos 心跳删除）？—— 推荐如设计决策 1
- [x] Q3：错误码以 `ResultCode` 为事实源、`Result` 码值不动？—— 推荐（兼容优先）
- [x] Q4：认证收敛方向为"security 解析、web 复用"？—— 推荐（与 A1 修复同向）
- [x] Q5：阶段三 Redis 缓存序列化滚动兼容——用户已确认接受"升级需清缓存"（2026-07-07），不做双读兼容层；升级指南中写明清缓存步骤
- [x] Q6：proud-day 仓库路径已提供：`/Users/andy/DevOps/SourceCode/business-projects/proud-day-projects/proud-day-backend`；阶段四规格已另立于该仓库 `docs/changes/nebula-persistence-adoption/`
- [x] Q7：nebula 版本号升代——用户已确认（2026-07-07）：b 分支 `revision` `2.0.1-SNAPSHOT` → `2.1.0-SNAPSHOT`（SB4 代际与 proud-day 现依赖的 SB3.5 代际坐标区分，解除快照误吞的 sequencing 风险），对应 Task 4-0

## 10. 验收标准

- [ ] 阶段一：`nebula-review-fixes/spec.md` 第 10 节全部勾选
- [ ] 阶段二：盘点表中每个配置项状态为"已接通"或"已删除"，无"定义未消费"残留；`rg "anonymousUrls|heartbeatInterval" --type java` 主代码零命中
- [ ] 阶段二：同一请求 JWT 只解析一次（集成测试断言 JwtService 调用次数）；`AuthContext` 与 `SecurityContext` 内容一致
- [ ] 阶段二：`ResultCode.getByCode()` 修复且有测试；`Result` 既有码值回归测试通过
- [ ] 阶段二：8 个核心 AutoConfiguration 均有 `ApplicationContextRunner` 开/关/缺类三态测试
- [ ] 阶段三：`rg -l "com.fasterxml.jackson.(databind|core|datatype)" --type java` 主代码零命中（annotation 包保留，见 2.3 口径修正）；根 POM 无 `spring-boot-jackson2`；MockMvc 脱敏哨兵测试在 Jackson 3 下仍绿
- [ ] 每阶段收尾：`mvn clean compile` + `mvn test` 全仓通过
