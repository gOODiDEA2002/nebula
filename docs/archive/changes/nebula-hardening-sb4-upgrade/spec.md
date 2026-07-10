# 变更规格：Nebula 框架硬化 + 升级 Spring Boot 4.1.0

> 状态：draft（待澄清项见第 8 节）
> 创建：2026-07-06
> 事实源文档：`docs/nebula-framework-review-2026-07.md`（审查报告）、`docs/nebula-springboot4-upgrade-design.md`（升级设计）
> 变更名：nebula-hardening-sb4-upgrade

---

## 1. 背景

新项目将全面启用 Nebula 框架。2026-07 的全面审查（7 路分域深审，附一手 `文件:行` 证据）结论是：框架"接口设计强、工程收尾弱"，多个宣称能力因单点错误整条链路静默失效，且几乎无集成测试兜底。同时框架现基线 Spring Boot 3.5.8 所在的 3.5 线 OSS 支持已于 2026-06-30 到期，升级到 4.1.0 有紧迫性。

本次变更把"缺陷修复"与"升级 Spring Boot 4.1"作为一个程序统一推进。

## 2. 目标

1. 消除阻断生产的正确性与安全缺陷，让框架宣称的关键能力"真的生效"。
2. 把 Spring Boot 3.5.8 升级到 4.1.0（Spring Framework 7 / Jakarta EE 11 / Java 21 沿用）。
3. 建立自动装配集成测试兜底，作为升级验收硬门槛。

**非目标**：不新增业务能力；不改动框架对外 API 语义（除非为修复安全缺陷必须，且需在 log 记录并提供迁移说明）。

## 3. 范围

**模块范围：全部**（用户确认新项目会依赖全部模块，故按严重级别全局排序，不做子集过滤）。

**总体策略：先修后升**（用户确认）。即先在 Spring Boot 3.5.16 上完成阻断级修复并稳定，再切 4.1 基线。理由：避免带着未修的机制性失效跨版本，问题更难定位。

## 4. 三条工作流

### 工作流 A — 阻断级修复（在 3.5.16 上，本次先拆这批）
既是"上生产的前置"，也是"升级的前置清理"。对应审查报告第 2 节 P0 全清单 + 第 1 节三个机制性失效。完成标准：关键能力经集成测试验证真生效，无阻断性安全洞，升级前置清理到位。

### 工作流 B — 升级到 Spring Boot 4.1（依赖 A 完成）
对应升级设计第 6/8 节。阶段 1 切基线（模块化拆分迁包、Jackson 2→3、属性改名）→ 阶段 2 重灾重构（Gateway 坐标、gRPC 转 spring-grpc、ES Rest5Client、RabbitMQ 交 BOM / RocketMQ 评估迁 5.x）→ 阶段 3 收尾（Spring AI 2.0、测试改造、starter 细粒度依赖）。

### 工作流 C — 可靠性与治理（可与 B 并行/穿插）
P1 可靠性（拦截器顺序、XFF、双 MQ 冲突、读写分离、多级缓存失效、RocketMQ 停机、Netty 握手）+ 治理（补集成测试、清死配置、移除残留 stereotype、收紧默认值、收敛并行实现、删除 `nebula-example/` 空壳）。

## 5. 工作流 A 的功能点（本次拆 task 的依据）

按审查报告编号索引，逐项功能点见下。域代码：CF/CS/CWT/CD/MW/RDG/SSA/ASI（见审查报告第 4 节）。

### 5.1 机制性失效（最高优先）
- **F-A1**：EPP 与 ImportFilter 注册迁回 `spring.factories`（审查 1.1 / ASI-1）。字节码已证 `.imports` 对这两类不生效。ImportFilter 建议直接删除（其无条件排除行为过激，见待澄清 Q1）。
- **F-A2**：`@MessageListener` 扫描（审查 1.2 / MW-1）。`MessageHandlerProcessor` 同时扫新旧注解，新注解优先。
- **F-A3**：RBAC 链路（审查 1.3 / CS-7、CS-6）。提供 `JwtAuthenticationFilter` 解析 header 写入并清理 `SecurityContext`；`SecurityAspect` 切点补 `@within(...)` 使类级注解生效。

### 5.2 安全绕过 / 数据泄漏
- **F-A4**：`AuthInterceptor` OPTIONS 认证绕过 → `CorsUtils.isPreFlightRequest()`（CWT-8）。
- **F-A5**：响应缓存跨用户串数据 → 带认证请求默认不缓存 + 改注解白名单显式声明可缓存接口，默认关闭（CWT-9）。
- **F-A6**：`@SensitiveData` 挂到 Spring MVC 主 ObjectMapper（CWT-10）。
- **F-A7**：`/rpc` 与 gRPC 通用服务鉴权 + 参数类型改白名单匹配（禁 `Class.forName`）（RDG-5）。
- **F-A8**：xxl-job 遗留 REST 端点（`/run` `/kill` `/log`）鉴权或整体裁撤 + token 不进日志（CWT-11）。
- **F-A9**：性能端点鉴权 + `resetMetrics` 实现或移除（CWT-23）。
- **F-A10**：ES 自动配置认证回调被覆盖（三次 setHttpClientConfigCallback）→ 合并为单回调（SSA-2）。
- **F-A11**：Redis 缓存全量多态反序列化 RCE → 白名单限定业务包（CD-11）。

### 5.3 升级前置清理
- **F-A12**：移除全局 `--enable-preview`，compiler 改 `<release>21`（ASI-build）。
- **F-A13**：删除 spring-rabbit 3.1.0 / amqp-client 5.20.0 版本锁定，交 BOM 托管（ASI-rabbit）。
- **F-A14**：删除框架 JAR 内 `application.yml`，Gateway gRPC 禁用/Spring AI 排除改代码或 Filter（ASI-3）。
- **F-A15**：删除 task 模块 `spring.factories` 死文件、修正 imports 注释（ASI / CWT）。

### 5.4 正确性硬伤
- **F-A16**：锁 `tryLock` 时间单位混用（60s 变 16.7h）+ `tryExecute` 吞业务异常（CD-1、CD-6）。
- **F-A17**：`ServiceImpl` 假实现（`findByField`/`findOneByField`/`findTopN` 等）真实现或删除（CD-2）。
- **F-A18**：缓存 `clear()`/`stats` 的 `KEYS *` → SCAN + `keyPrefix` 圈定（CD-5）。
- **F-A19**：RPC 共享单例客户端并发串地址（HTTP + gRPC）→ 目标地址随请求传递/按实例缓存（RDG-1、RDG-2）。
- **F-A20**：加权负载均衡配置即启动崩溃 → 枚举映射修正（RDG-3）。
- **F-A21**：Nacos 自动注册只认 Servlet 事件 → 改判 `WebServerInitializedEvent` 覆盖 Reactive，非 Web/gRPC 另加注册钩子（RDG-4）。
- **F-A22**：Neo4j 自动配置未注册进 imports（死功能）→ 注册（ASI-4）。
- **F-A23**：Foundation 五个运行期必炸缺陷：`refreshToken` UOE、`addFieldError` UOE、雪花默认实例集群撞号、`SequenceGenerator` 并发重号、密码哈希强度不足（CF-1..5）。
- **F-A24**：MQ 静默丢消息 / NPE：RabbitMQ 毒消息无限重投+路由键打架（MW-2、MW-3）、Redis Stream headers NPE（MW-4）、向量存储忽略过滤条件（SSA-1）。

## 6. 影响范围

- 涉及模块：core/foundation、core/security、web、task、data-persistence、data-cache、lock、messaging（rabbitmq/redis）、rpc（core/http/grpc）、discovery-nacos、search-es、ai-spring、autoconfigure、starter、gateway。
- 对外行为变更（需在 log 记录 + 提供迁移说明）：CORS 预检行为、响应缓存默认关闭、`/rpc` 与 xxl-job/性能端点需鉴权、`CryptoUtils.encrypt` 密码哈希语义（见待澄清 Q6）、`@MessageListener` 从"不生效"变"生效"（可能激活此前静默的消费者）。
- 构建变更：移除 `--enable-preview`（消费方不再需要该运行参数，属正向解绑）。

## 7. 风险点

1. **无测试兜底放大修复风险**：修复本身可能引入回归。缓解：F-A1/A2/A3 每项配套集成测试，作为该 task 验收的一部分。
2. **对外行为变更影响现有示例/使用方**：`@MessageListener` 激活、缓存默认关闭、端点鉴权可能让"原本能跑"的示例行为改变。缓解：同步更新 examples 并在 log 记录迁移点。
3. **F-A13 交还 BOM 后 RabbitMQ 版本跳变**：需在 3.5.16 上验证 RabbitMQ 收发正常再提交。
4. **先修后升期间 3.5 已 EOL**：修复窗口内 3.5.16 不再有新安全补丁，需控制工作流 A 周期，尽快进入 B。

## 8. 待澄清（采用推荐默认继续，如需调整请指出）

| # | 问题 | 推荐默认 |
|---|------|---------|
| Q1 | `NebulaAutoConfigurationImportFilter` 删除还是条件化后注册？ | **删除**（无条件排除 DataSource/MyBatis-Plus 行为过激，且当前本就没生效，删除零风险） |
| Q2 | 响应缓存改造力度 | **双管**：带认证请求默认不缓存 + 默认关闭改注解白名单 |
| Q3 | xxl-job 遗留 REST 端点 | **整体裁撤**（EmbedServer 已接管执行器协议，遗留端点纯攻击面） |
| Q4 | `ServiceImpl` 假实现 | 有调用方的用 QueryWrapper 真实现，无调用方的直接删方法（拆 task 时先 grep 调用方） |
| Q5 | RocketMQ 是否随升级迁 5.x | 属工作流 B 决策，Spec 暂记为待定；工作流 A 只修 4.x 上的正确性 bug |
| Q6 | `CryptoUtils.encrypt` 密码哈希 | 新增 `hashPassword`（BCrypt）+ 旧方法 `@Deprecated`，不直接改旧方法语义以免破坏已存哈希 |

## 9. 验收标准（工作流 A 整体）

- 新增的 starter defaults 集成测试通过：只引入 starter、不写任何 `enabled`，默认模块确实启用。
- 新增的 `@MessageListener` 集成测试通过：按新注解写的消费者能收到消息。
- 新增的 RBAC 集成测试通过：类级安全注解生效、`SecurityContext` 被正确填充与清理。
- 全部 P0 安全项有对应测试或手工验证记录。
- `mvn clean install -DskipTests=false` 全绿（3.5.16 基线）。
- log.md 沉淀对外行为变更的迁移说明。
