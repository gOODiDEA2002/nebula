# 任务拆分 -- 剩余工作总体清单（nebula-remaining-work）

> 来源 Spec：`docs/changes/nebula-remaining-work/spec.md`
> 实现细节：`docs/changes/nebula-remaining-work/implementation.md`（编码代理执行手册：代码现场/精确改法/执行顺序，先读它再动手）
> 阶段顺序：一（审查修复）→ 二（EPIC-C2 治理）→ 三（Jackson 2→3）→ 四（proud-day，外部）
> 每个任务 = 可独立提交的原子变更；完成即勾选并 git commit

## 前置条件

- [x] Spec 已确认（Q1-Q7 全部拍板，2026-07-07；等用户对整体方案最终放行后开工）
- [ ] 开工第一个动作：将方案文档（`docs/changes/nebula-remaining-work/`、`docs/changes/nebula-review-fixes/`、`docs/nebula-hardening-code-review-2026-07.md`）单独提交一次，使代码工作区回到干净状态，保证后续"一任务一提交"账目清晰
- [x] 当前在 `nebula-remaining-work` 分支（2026-07-08 已检出并提交方案文档 `1f49a363`），代码工作区干净

---

## 阶段一：审查问题修复（13 个任务，已拆分）

> 详细任务见 `docs/changes/nebula-review-fixes/tasks.md`（Task 1-13），此处只挂总闸门，不重复维护两份清单。

- [x] **P1｜review-fixes Task 1-5 完成**（2 个 CRITICAL：MVC 脱敏失效、gRPC 服务端；含 T-B2-2 收尾）
- [x] **P1｜review-fixes Task 6-7 完成**（2 个 HIGH：RPC 客户端 token、findByField 注入）
- [x] **P1｜review-fixes Task 8-13 完成**（MEDIUM/LOW + 全量回归收尾）

---

## 阶段二：EPIC-C2 治理收尾

### Task 2-0: 阶段一欠账清理（2026-07-08 对账审计新增）

- **目标**: 修补阶段一对账审计发现的两处未完成项（细节见 implementation.md Task 2-0）
- **子项**:
  - [x] Task 13 文档欠账：README.md/AGENTS.md 版本口径改 4.1.0；CLAUDE.md v2.0.x 变更记录补充阶段一内容
  - [x] Task 12.3 装配缺口：`RpcDiscoveryAutoConfiguration` 用 `ObjectProvider<Executor>` 接通 `rpcExecutor` 注入 + 条件装配测试
- **依赖**: 无（阶段二开工第一动作）
- **验收标准**: 两项各自独立提交；`rg "3\.5\.8" README.md AGENTS.md` 零命中
- **验证命令**: `mvn test -pl autoconfigure/nebula-autoconfigure`
- [x] 完成（a2b469bb + 304791eb；2026-07-08 审计对账时补勾——两个子项当时已勾选，总项遗漏）

### Task 2-1: 死配置全量盘点（只盘点不改码）

- **目标**: 对框架审查报告 :109 点名的配置项逐一核实消费点，产出"接通/删除"裁决表
- **涉及文件**:
  - `docs/changes/nebula-remaining-work/log.md` -- 记录盘点表：配置项 | 定义位置 | 消费点（或"无"）| 裁决（接通/删除）| 理由
  - 盘点范围（至少）：`HttpRpcProperties.ClientConfig.*`、`GrpcRpcProperties.ServerConfig.*`（port 除外）、`SecurityProperties.anonymousUrls`/`rbac.*`、`NacosProperties.heartbeatInterval/heartbeatTimeout`、`RedisLockProperties.defaultWaitTime`、web/gateway 两套 CORS 配置（`WebCoreAutoConfiguration`）、以及 `rg "private .* [a-z]" **/config/*Properties.java` 全量扫描新发现项
- **依赖**: 阶段一完成（gRPC 形态定型后才能裁决 ServerConfig 参数）
- **验收标准**: 盘点表覆盖所有 `*Properties` 类；每项有明确裁决与理由；用户确认裁决表后才执行 Task 2-2/2-3
- **验证命令**: 人工评审盘点表
- [x] 完成（盘点表已产出于 log.md，待用户裁决）

### Task 2-2: 死配置处理 -- HTTP RPC 客户端参数接通

- **目标**: `nebula.rpc.http.client.*` 的连接池/超时/重试参数真正作用于 RestClient 构建
- **涉及文件**:
  - `autoconfigure/nebula-autoconfigure/src/main/java/io/nebula/autoconfigure/rpc/HttpRpcAutoConfiguration.java` -- `rpcRestClient` Bean 构建时应用 `connectTimeout`/`readTimeout`/`maxConnections`/`maxConnectionsPerRoute`/`keepAliveTime`（基于 JDK HttpClient 或 HttpComponents 请求工厂）；重试参数若不实现则按 Task 2-1 裁决删除
  - `infrastructure/rpc/nebula-rpc-http/src/main/java/io/nebula/rpc/http/config/HttpRpcProperties.java` -- 删除裁决为"删除"的字段（如 `compressionEnabled`、`writeTimeout` 视裁决）
  - `autoconfigure/.../diagnostic/NebulaDiagnosticEndpoint.java` -- 诊断展示与真实生效参数对齐
  - 新增 `HttpRpcClientConfigTest` -- 断言超时/连接池参数生效
- **依赖**: Task 2-1
- **验收标准**: 每个保留的配置项有真实消费点；诊断端点不展示已删除项
- **验证命令**: `mvn test -pl autoconfigure/nebula-autoconfigure,infrastructure/rpc/nebula-rpc-http`
- [x] 完成

### Task 2-3: 死配置处理 -- 删除批（security/nacos/grpc 遗留）

- **目标**: 按裁决表删除无消费者的配置项，CHANGELOG 逐项标注
- **涉及文件**:
  - `core/nebula-security/src/main/java/io/nebula/security/config/SecurityProperties.java` -- 删除 `anonymousUrls`（rbac.* 按裁决处理）。注意：核心模块，删除字段属破坏性变更，Javadoc 指明替代项 `nebula.web.auth.ignore-paths`
  - `infrastructure/discovery/nebula-discovery-nacos/src/main/java/io/nebula/discovery/nacos/config/NacosProperties.java` -- 删除 `heartbeatInterval`/`heartbeatTimeout`（:93,101）
  - `infrastructure/rpc/nebula-rpc-grpc/src/main/java/io/nebula/rpc/grpc/config/GrpcRpcProperties.java` -- 删除 ServerConfig 遗留线程/流控参数
  - 对应 AutoConfiguration 诊断展示同步删除（`NacosDiscoveryAutoConfiguration.java:94`、`SecurityAutoConfiguration.java:94` 等）
  - `CLAUDE.md` -- v2.0.x 变更记录补充删除清单
- **依赖**: Task 2-1
- **验收标准**: `rg "anonymousUrls|heartbeatInterval|heartbeatTimeout" --type java` 主代码零命中；编译通过
- **验证命令**: `mvn clean compile && mvn test -pl core/nebula-security,infrastructure/discovery/nebula-discovery-nacos`
- [x] 完成

### Task 2-4: 错误码收敛 -- ResultCode 为唯一事实源

- **目标**: 三套错误码收敛为一套，保持 `Result` 对外契约不变
- **涉及文件**:
  - `core/nebula-foundation/src/main/java/io/nebula/core/common/enums/ResultCode.java` -- 修复 `getByCode()` 缺陷（报告 CF-40：永远查不到）；补齐与 `Result` 现有字符串码的映射
  - `core/nebula-foundation/src/main/java/io/nebula/core/common/result/Result.java` -- 新增 `public static <T> Result<T> of(ResultCode code)` / `of(ResultCode, T data)` 工厂；现有方法与码值**零改动**
  - `core/nebula-foundation/src/main/java/io/nebula/core/common/constant/Constants.java` -- 错误码常量标 `@Deprecated`，Javadoc 指向 `ResultCode`
  - `core/nebula-foundation/src/test/java/.../ResultCodeTest.java` -- `getByCode` 全枚举回查测试 + `Result` 既有码值回归测试
- **关键签名**:
  ```java
  public static <T> Result<T> of(ResultCode code, T data) { }
  public static ResultCode getByCode(String code) { }
  ```
- **依赖**: 无（可与 Task 2-2/2-3 并行）
- **验收标准**: `Result.success().getCode()` 等既有值不变；`ResultCode.getByCode` 对每个枚举值可回查
- **验证命令**: `mvn test -pl core/nebula-foundation`
- [x] 完成

### Task 2-5: 认证收敛 -- 单一 JWT 解析点（本批最高风险项）

- **目标**: JWT 只在 security 层解析一次，web 层复用；`AuthContext` API 兼容
- **涉及文件**:
  - `application/nebula-web/src/main/java/io/nebula/web/interceptor/AuthInterceptor.java` -- 移除自行解析 JWT 的逻辑，改为：读取 `SecurityContext.getAuthentication()` 判定放行/401，并桥接填充 `AuthContext`
  - `application/nebula-web/src/main/java/io/nebula/web/auth/AuthContext.java` -- 公开 API 不变，内部由 SecurityContext 适配（或由 AuthInterceptor 填充，实现自选，API 兼容为硬约束）
  - `autoconfigure/nebula-autoconfigure/.../security/SecurityAutoConfiguration.java`（或 WebAuthAutoConfiguration） -- `JwtAuthenticationFilter` 注册条件调整：`nebula.web.auth.enabled=true` 时自动注册（原 opt-in 开关保留为覆盖手段）
  - `starter/nebula-starter-web/src/main/resources/META-INF/nebula-defaults.properties` -- 视条件调整补默认值
  - 新增集成测试 `AuthConvergenceIntegrationTest` -- 断言：同一请求 `JwtService.validateAccessToken` 仅调用一次（Mockito spy）；`AuthContext` 与 `SecurityContext` 用户信息一致；无 token 访问受保护路径 401、白名单路径放行
- **依赖**: 阶段一完成（Filter 注册机制稳定后）
- **验收标准**: 见集成测试；`nebula-web` 与 `nebula-security` 既有测试全绿
- **验证命令**: `mvn test -pl application/nebula-web,core/nebula-security`
- [x] 完成

### Task 2-6: xxl-job 孤儿 DTO 清理

- **目标**: 删除 T-A2-5 遗留的无引用 DTO
- **涉及文件**:
  - 删除 `application/nebula-task/src/main/java/io/nebula/task/xxljob/dto/XxlJobExecuteRequest.java`
  - 删除 `application/nebula-task/src/main/java/io/nebula/task/xxljob/dto/XxlJobLogRequest.java`
  - 删除 `application/nebula-task/src/main/java/io/nebula/task/xxljob/dto/XxlJobLogResult.java`
  - 保留 `XxlJobResult.java`（`XxlJobRegistryService.java:169,205` 在用）
- **依赖**: 无
- **验收标准**: task 模块编译与测试通过；`rg "XxlJobExecuteRequest|XxlJobLogRequest|XxlJobLogResult"` 零命中
- **验证命令**: `mvn test -pl application/nebula-task`
- [x] 完成

### Task 2-7: 自动装配集成测试补齐（ApplicationContextRunner）

- **目标**: 为核心 AutoConfiguration 建立"开/关/缺类"三态条件装配测试，形成 A1 类事故的常设防线
- **涉及文件**（新增于 `autoconfigure/nebula-autoconfigure/src/test/java/io/nebula/autoconfigure/`）:
  - `web/WebAutoConfigurationConditionTest.java`
  - `data/CacheAutoConfigurationConditionTest.java`
  - `messaging/RabbitMQAutoConfigurationConditionTest.java`
  - `security/SecurityAutoConfigurationConditionTest.java`
  - `rpc/HttpRpcAutoConfigurationConditionTest.java`
  - `rpc/GrpcRpcAutoConfigurationConditionTest.java`
  - `search/ElasticsearchAutoConfigurationConditionTest.java`（条件评估，不连真实 ES）
  - `env/StarterDefaultsInjectionTest.java`（若阶段一 Task 9 未覆盖）
- **关键模式**:
  ```java
  new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(XxxAutoConfiguration.class))
      .withPropertyValues("nebula.xxx.enabled=true")
      .run(ctx -> assertThat(ctx).hasSingleBean(XxxService.class));
  ```
- **依赖**: Task 2-2/2-3/2-5（配置面定型后再锁测试）
- **验收标准**: 8 个测试类各覆盖 enabled=true / false / 缺依赖类三态
- **验证命令**: `mvn test -pl autoconfigure/nebula-autoconfigure`
- [x] 完成

### Task 2-8: CLAUDE.md / AGENTS.md 行号引用同步 + 阶段二收尾

- **目标**: 清理文档债，阶段二全量回归
- **涉及文件**:
  - `CLAUDE.md`、`AGENTS.md` -- "关键实现引用"章节逐条核对行号（脚本辅助：对每条引用 rg 类名/方法名取实际行号），过期即更新；"模块核对提示词"中已失实的描述（如 net.devh gRPC）同步修正
  - `.cursor/rules/project.mdc` -- 涉及被删配置项/已变更鉴权说明的段落同步
  - `docs/changes/nebula-remaining-work/tasks.md` -- 勾选阶段二
- **依赖**: Task 2-1 ~ 2-7
- **验收标准**: 抽查 20 条行号引用全部命中；`mvn clean compile && mvn test` 全仓通过
- **验证命令**: `mvn clean compile && mvn test`
- [x] 完成

---

## 阶段三：Jackson 2 → 3 全量迁移（T-B1-3 遗留）

> 前置拍板：Q5 已确认"升级清缓存"（2026-07-07）。
> 迁移方向：`com.fasterxml.jackson.*` → `tools.jackson.*`，`ObjectMapper` → `JsonMapper`；自底向上，每模块一任务一提交，全程编译绿，最后拆桥。

### Task 3-0: Jackson 相关 POM 依赖盘点与替代策略（只盘点不改码）

- **目标**: 代码 import 清零之外，把 POM 里显式声明的 Jackson 2 系依赖逐项定策略，避免"import 干净了、运行时包还没搬完"（2026-07-07 外部审查意见 P2-1）
- **盘点方式**（2026-07-08 修订，外部审查 P2-1）: 必须对**全仓所有 pom.xml** 执行 `rg -n "jackson|jjwt-jackson" -g 'pom.xml'` 生成完整清单逐项定策略——复核发现 gateway/storage/search/websocket/crawler/rpc-async/messaging/foundation/cache 等 20+ 个 pom 均有显式 Jackson 2 系声明，下方"已知清单"仅是起点示例，**不是全集**
- **已知清单**（盘点起点，rg 核实于 2026-07-07）:
  - 根 `pom.xml:290` -- `jjwt-jackson`（dependencyManagement）
  - 根 `pom.xml:355` -- `spring-boot-jackson2`（桥模块，Task 3-6 拆除）
  - `core/nebula-security/pom.xml:74`、`application/nebula-web/pom.xml:73` -- `jjwt-jackson:0.12.3`
  - `integration/nebula-integration-payment/pom.xml:56` -- `jackson-databind`（直接依赖）
- **策略预判**（盘点时逐项核实定稿）:
  - `jjwt-jackson`：**已核验（2026-07-08）**——JJWT 0.13.0 无 Jackson 3 支持（官方 issue #1029，适配在未来版本），采用选项 (a) 保留 Jackson 2 作为 JWT 序列化的孤立第三方传递依赖（记录清单）；执行时复查 0.14+ 是否已发布，有则直接升级消除残留
  - payment 的 `jackson-databind`：随 Task 3-5 代码迁移改为 Jackson 3 坐标或删除（若仅传递可用）
- **产出**: 盘点表记入 `log.md`；各项策略并入对应模块迁移任务执行
- **依赖**: 无（阶段三开工首个任务）
- **验证命令**: 人工评审盘点表
- [x] 完成（盘点表已记入 log.md：24 个 pom 命中，17 处换坐标，11 处删 jsr310，8 组保留 jjwt/annotations；jjwt 0.14+ 未发布 Jackson 3 支持，确认保留为唯一运行时残留）

### Task 3-1: foundation 层迁移（JsonUtils 等）

- **涉及文件**: `core/nebula-foundation` 内全部 Jackson import（`JsonUtils` 为核心）；公开方法签名若含 `ObjectMapper` 需评估兼容（核心模块，优先内部封装隔离）
- **验证命令**: `mvn test -pl core/nebula-foundation`
- [x] 完成（Tests run: 278, Failures: 0; 全仓 `mvn clean compile` BUILD SUCCESS）

### Task 3-2: data 层迁移（cache Redis 序列化 + persistence）

- **涉及文件**: `nebula-data-cache`（Redis 值序列化器、多态白名单——Jackson 3 的 PolymorphicTypeValidator API 对应迁移）、`nebula-data-persistence`
- **关注**: Q5 拍板的缓存兼容策略在此落地；多态白名单安全语义不得减弱
- **验证命令**: `mvn test -pl infrastructure/data/nebula-data-cache,infrastructure/data/nebula-data-persistence`
- [x] 完成（Tests run: 61, Failures: 0; 全仓 compile BUILD SUCCESS）

### Task 3-3: messaging / rpc / lock / websocket 层迁移

- **涉及文件**: messaging-core/redis（序列化器）、rpc-http/grpc/async（客户端/服务器/控制器）、lock-redis（无Jackson依赖）、websocket-core/spring/netty（Handler/Server/AutoConfig）
- **关注**: RPC 双端序列化格式一致性；Autoconfigure 层 ObjectMapper 兼容性（bridge 期间不注入 Spring 管理的 Jackson 2 ObjectMapper，改为自行创建 Jackson 3 实例）
- **验证命令**: `mvn test -pl infrastructure/messaging/nebula-messaging-core,infrastructure/rpc/nebula-rpc-http,infrastructure/rpc/nebula-rpc-grpc,infrastructure/lock/nebula-lock-redis,infrastructure/websocket/nebula-websocket-netty`
- **验证结果**: 128 tests passed, BUILD SUCCESS
- [x] 完成

### Task 3-4: web 层迁移（脱敏 customizer 重写为 Jackson 3）

- **涉及文件**: `application/nebula-web` 的 `WebAuthAutoConfiguration`（脱敏 AnnotationIntrospector → Jackson 3 对应 API）、`JacksonConfig`（JavaTimeModule——Jackson 3 内置 java.time，评估直接删除）、`SensitiveDataMaskingCustomizerTest` 同步
- **关注**: 阶段一 Task 2 的 MockMvc 哨兵测试改为 Jackson 3 路径后必须仍绿（本阶段回归闸门）
- **验证命令**: `mvn test -pl application/nebula-web`
- [ ] 完成

### Task 3-5: search / ai / task / autoconfigure 层迁移

- **涉及文件**: `nebula-search-elasticsearch`（`JacksonJsonpMapper` 换 Jackson 3 变体）、`nebula-ai-*`、`nebula-task`、`nebula-autoconfigure`（CacheAutoConfiguration 序列化器装配、诊断端点等）
- **验证命令**: `mvn test -pl infrastructure/search/...,infrastructure/ai/...,application/nebula-task,autoconfigure/nebula-autoconfigure`
- [ ] 完成

### Task 3-6: 拆桥收尾

- **涉及文件**:
  - 根 `pom.xml` -- 移除 `spring-boot-jackson2` 依赖管理与各模块引用
  - `starter/*/nebula-defaults.properties` -- 移除 `spring.http.converters.preferred-json-mapper=jackson2`（阶段一 Task 1 注入项）
  - 全仓确认 `rg -l "com.fasterxml.jackson" --type java`（主代码）零命中
- **依赖**: Task 3-0 ~ 3-5
- **验收标准**: 全仓编译 + 测试通过；MockMvc 脱敏哨兵在 Jackson 3 默认路径下绿；`mvn dependency:tree` 无 `com.fasterxml.jackson` 运行时依赖（第三方传递除外，按 Task 3-0 盘点表逐项核销并记录清单）；POM 中无 Task 3-0 裁决为"迁移/删除"的 Jackson 2 系显式依赖残留
- **验证命令**: `mvn clean compile && mvn test`
- [ ] 完成

---

## 阶段四：proud-day 接入 Nebula 持久化（T-D-1）

> 仓库：`/Users/andy/DevOps/SourceCode/business-projects/proud-day-projects/proud-day-backend`
> 详细规格与任务已立于该仓库 `docs/changes/nebula-persistence-adoption/`（spec 已 confirmed，Q1/Q2/Q3 于 2026-07-07 拍板；3 任务）

- [ ] **Task 4-0: [nebula 侧] 版本号升代 `2.0.1-SNAPSHOT` → `2.1.0-SNAPSHOT`**（根 pom `<revision>`，Q7 已确认；proud-day 迁移的前置硬依赖，防 SB3.5 应用误吞 SB4 快照）
- [ ] **Task 4-1: [proud-day 侧] 按其仓库 tasks.md 执行三任务**（版本对齐 → 数据源/Mapper 扫描迁移 → 全量回归；框架侧若暴露问题回流本清单）

---

## 变更摘要

> 全部阶段完成后填写

- 总文件数:
- Spec-Plan 偏差记录:
- 遗留问题:
