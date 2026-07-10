# Nebula 框架升级 Spring Boot 4.1.0 系统设计报告

> 编制日期：2026-07-05
> 现状：Spring Boot 3.5.8 / Spring Cloud 2025.0.0 / Spring AI 1.1.0 / Java 21 / Maven（`${revision}` CI-friendly）
> 目标：Spring Boot 4.1.0 / Spring Framework 7 / Jakarta EE 11 / Java 21（沿用）
> 配套审查：`docs/nebula-framework-review-2026-07.md`

---

## 1. 为什么现在必须升级

- **3.5 线开源支持已到期**：Spring Boot 3.5.x 的 OSS 支持已于 **2026-06-30** 终止（最后一个 OSS 版本 3.5.16，2026-06-25 发布）。之后 3.5 只有商业支持，开源用户不再获得免费安全补丁。来源：https://endoflife.date/spring-boot 、https://spring.io/support-policy/
- **4.1.0 已 GA**：2026-06-10 发布到 Maven Central，截至 2026-07-05 是最新版（无 4.1.1）。4.0.0 于 2025-11-20 GA。来源：https://spring.io/blog/2026/06/10/spring-boot-4/
- **支持窗口**：4.0 OSS 支持至 2026-12-31；4.1 支持至 2027-07-31。直接上 4.1 比上 4.0 多半年支持期。来源：https://github.com/spring-projects/spring-boot/wiki/Supported-Versions

> **版本可获取性复核（2026-07-06，对照 Maven Central `maven-metadata.xml`）**：`spring-boot-starter-parent` 的 `<latest>/<release>` 为 **4.1.0**，`3.5.16`、`4.0.7` 均已发布；`spring-cloud-dependencies` 的 `<release>` 为 **2025.1.2**。本报告所用目标版本均可从 Maven Central 直接消费，非里程碑/RC。（此条为回应一次外部审查"版本不可解析"的质疑而补——经权威清单核实，质疑不成立。）

**结论：升级有紧迫性，目标直接定 4.1.0，但迁移动作分"先清废弃 → 再过 4.0 语义 → 最后到 4.1"三步走。**

---

## 2. 好消息与坏消息

**好消息（降低工作量）：**
- **Java 21 可直接沿用**：4.1 最低 JDK 17、最高支持 Java 26。不用换 JDK。
- 无 `RestTemplate`、无 `@MockBean`、无 javax 残留（仅 JDK 自带 javax.crypto）——这几个常见升级痛点本框架基本没踩。
- HTTP RPC 已用 `RestClient`（非 RestTemplate），SB4 友好。
- Nacos 直接用 `nacos-client`（非 spring-cloud-alibaba 版本车），受 Boot 版本约束小。
- MyBatis-Plus、Redisson、XXL-JOB、springdoc 都已有支持 Boot 4 的版本。

**坏消息（五大工作量集中点）：**
1. **模块化拆分冲击自建 autoconfigure/starter 体系**（最大影响面）。
2. **Jackson 2 → Jackson 3** 全量包名迁移。
3. **Elasticsearch 换 Rest5Client**（需重写客户端装配）。
4. **Spring Cloud Gateway 坐标改名**（WebFlux/WebMvc 拆分）。
5. **gRPC 从已停更的 net.devh 转 Spring 官方 spring-grpc**。

---

## 3. Spring Boot 4.0/4.1 核心变化及对 Nebula 的影响

来源：[4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)、[4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)、[4.1 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes)

| 变化 | 说明 | 对 Nebula 的影响 |
|------|------|------------------|
| **基线升级** | Spring Framework 7.0、Spring Security 7、Jakarta EE 11（Servlet 6.1）、Tomcat 11 | Java 21 满足；Undertow 被移除（本框架未用，无影响） |
| **模块化拆分** | 单体 `spring-boot-autoconfigure` 拆成 `spring-boot-<technology>` 小模块，包根变 `org.springframework.boot.<technology>` | **最高影响**：所有 `@AutoConfigureBefore/After` 引用的 Boot 自动配置类全限定名会变位置 |
| **包名迁移** | `EnvironmentPostProcessor` 移到 `org.springframework.boot`；`@EntityScan` 移到 `...persistence.autoconfigure` 等 | 影响 `NebulaStarterDefaultsPostProcessor`、健康检查类 import |
| **自动配置注册** | `AutoConfiguration.imports` 机制不变；`spring.factories` 的 `EnableAutoConfiguration` 键彻底不再被读取 | task 模块的 spring.factories 死文件须删；EPP/ImportFilter 仍走 spring.factories |
| **Jackson 3** | groupId `tools.jackson`，包名变更，成默认；Jackson 2 经 `spring-boot-jackson2` 保留 | 影响所有序列化定制：foundation JsonUtils/Beans、web JacksonConfig/脱敏、messaging 序列化器、缓存序列化器 |
| **HttpHeaders 不再实现 MultiValueMap** | `containsKey()`→`containsHeader()` 等 | 网关/RPC 中把 HttpHeaders 当 Map 用处需改 |
| **测试变化** | `@MockBean`→`@MockitoBean`；`@SpringBootTest` 不再自动配 MockMvc/TestRestTemplate | 现有测试需改造 |
| **属性改名** | 如 `management.tracing.enabled`→`...export.enabled` 等 | 用 `spring-boot-properties-migrator` 扫描 |
| **Elasticsearch** | 自动配置从 `RestClient` 换 `Rest5Client`（ES Java Client 9.x） | nebula-search-elasticsearch 需重写客户端装配 |
| **4.1 新增：Spring gRPC 官方支持** | 服务端+客户端+测试 | nebula-rpc-grpc 的转投目标 |
| **4.1 破坏性** | Maven 插件 AOT 跳过测试只认 `-Dmaven.test.skip`（`-DskipTests` 不再生效） | CI 脚本需核对 |
| **虚拟线程** | 4.1 仍需显式 `spring.threads.virtual.enabled=true`，未默认开启 | 无强制动作 |

---

## 4. 生态兼容矩阵（升级目标版本）

来源见每行；标注"需实测/待确认"的项目动手前二次核对。

| 依赖 | 当前版本 | 升级目标（支持 Boot 4） | 状态/风险 | 来源 |
|------|---------|----------------------|----------|------|
| Spring Boot | 3.5.8 | **4.1.0** | GA | spring.io/blog/2026/06/10 |
| Spring Cloud | 2025.0.0 | **2025.1.2（Oakwood）** | 兼容 Boot 4.1.0；2025.0 与 Boot 4 明确不兼容 | spring.io/blog/2026/06/11 |
| Spring Cloud Gateway | `spring-cloud-starter-gateway`（旧名） | `spring-cloud-gateway-server-webflux` / `-webmvc` | 旧坐标已删，须改名；有 OpenRewrite recipe | docs.openrewrite.org |
| Spring AI | 1.1.0 | **2.0.0 GA（2026-06-12）** | 刚 GA，API 有破坏性变化，需专项评估 | spring.io/blog/2026/06/12 |
| MyBatis-Plus | mybatis-plus-spring-boot3-starter 3.5.9 | **`mybatis-plus-spring-boot4-starter`** | 换坐标即可 | baomidou.com/getting-started |
| MyBatis starter | 3.x | mybatis-spring-boot-starter **4.0.x** | 官方已支持 | github.com/mybatis/spring-boot-starter |
| Redisson | redisson-spring-boot-starter 3.39.0（框架已切核心包） | **4.x**（4.0 首支持 Boot 4，最新 4.4.0 对应 4.0.6） | 已支持；对 4.1.0 需实测 | github.com/redisson/redisson/issues/6869 |
| gRPC starter | net.devh 3.1.0.RELEASE | **Spring gRPC（`org.springframework.grpc`）1.x** 或 Boot 4.1 内建 | net.devh 停更无 Boot 4 版本，**必须迁移** | spring.io/blog/2025/11/05 |
| XXL-JOB | 3.2.0 | **3.3.1+**（admin 升 Boot 4；executor 侧 core 无强耦合） | executor 风险低 | github.com/xuxueli/xxl-job/releases |
| Nacos client | 2.5.1 | 可继续用；SCA 新版升 3.1.1 | 纯 SDK，风险低 | github.com/alibaba/spring-cloud-alibaba |
| Elasticsearch Java | 8.11.1 | **9.x + Rest5Client** | 需重写客户端构建 | github.com/spring-projects/spring-boot/issues/46061 |
| jjwt | 0.12.3 | 0.12.6+（jjwt-jackson 仍基于 Jackson 2） | 低风险，会引入 Jackson 2 共存，或改 jjwt-gson | github.com/jwtk/jjwt/issues/1029 |
| springdoc-openapi | 2.2.0 | **3.x**（支持 Boot 4、OpenAPI 3.1） | 换大版本坐标 | springdoc.org |
| RocketMQ client | 4.9.8 | 评估迁 **5.x**（见 6.5） | 4.x 内部包 import 升级即编译失败 | — |
| Micrometer | BOM 托管 | Boot 4.1 管理 1.17.0 | 属性改名 `management.tracing.export.*` | 4.1 Release Notes |
| MySQL / HikariCP | BOM 托管 | 跟随 Boot 4 BOM | 低风险 | — |
| Lombok | 1.18.3x | **1.18.42+** | 升级后先编译验证注解全量生成 | projectlombok.org/changelog |

**未查证/需动手前核实**：Spring Cloud Alibaba 支持 Boot 4 的 GA 版本号；Redisson 4.4.0 对 Boot 4.1.0（非 4.0.6）的正式声明；flatten-maven-plugin 对 Maven 4 兼容声明；Boot 4.1 BOM 中 HikariCP/MySQL/ES/JUnit 精确 managed 版本。

---

## 5. 升级前置清理（必须先做，否则升级必卡）

这些是本框架自身的问题，与 SB4 无关但会阻断升级，应在切版本号之前完成：

1. **移除全局 `--enable-preview`**（根 pom）：产物换 JDK 即失效，是升级第一颗雷。若未实际使用预览语法直接删；compiler 建议改用 `<release>21`。
2. **删除 spring-rabbit 3.1.0 / amqp-client 5.20.0 的版本锁定**：交还 BOM 托管，先在 3.5.16 上验证 RabbitMQ 正常。
3. **修复 EPP/ImportFilter 注册方式**（审查 1.1）：迁回 `spring.factories`。这一步在 SB4 也是必需的，一次到位避免返工。
4. **删除 task 模块的 spring.factories 死文件**、修正 imports 尾部错误注释。
5. **删除框架 JAR 内打包的 `application.yml`**（审查 ASI-3）：反模式，把里面的 Gateway gRPC 禁用/Spring AI 排除改为代码或 Filter 实现。
6. **在 3.5.16 上消除全部 deprecation 告警**：这是官方迁移指南的第一步。

---

## 6. 分模块升级设计

### 6.1 autoconfigure（工作量最大）

- **模块化拆分应对**：所有 `@AutoConfiguration(before/after=...)` 引用的 Boot 类全限定名需逐个改到新包。已确认受影响：`DataPersistenceAutoConfiguration`（DataSourceAutoConfiguration → `org.springframework.boot.jdbc.autoconfigure`）、`Neo4jAutoConfiguration`（→ `...neo4j.autoconfigure`）、`CacheAutoConfiguration`（RedisAutoConfiguration → `...data.redis.autoconfigure`）。
- **NebulaAutoConfigurationImportFilter 硬编码排除表**：里面写死的 `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` 等全限定名，SB4 迁包后字符串匹配静默失效。建议借升级窗口决策：直接删除该过滤器（推荐，其"无条件排除"行为本就过于激进），或改条件化后注册到 spring.factories。
- **依赖坐标策略**：先用 `spring-boot-starter-classic` 一次拉全旧世界依赖跑通编译，再逐模块替换成细粒度 `spring-boot-<technology>`。

### 6.2 Jackson 2 → Jackson 3

- **收口策略**：先把"获取 ObjectMapper"收敛到 `JsonUtils` 单点，再统一迁移，避免散弹式改动。
- 受影响清单：`foundation/JsonUtils.java`（`fields()` 已被 `properties()` 取代）、`foundation/Beans.java`、`web/JacksonConfig.java`、`web/mask/*` 全套脱敏 introspector/serializer、`messaging` 的 `JsonMessageSerializer`/`RedisMessageSerializer`/`RedisClusterMessageBroker`、缓存的 `GenericJackson2JsonRedisSerializer`。
- **注意 Jackson 双轨共存**：jjwt-jackson、nacos-client 自带 Jackson 2，classpath 会同时存在两代，需验证无冲突。

### 6.3 Web

- `AntPathMatcher` → `PathPattern`（`AuthInterceptor`、`RequestLoggingInterceptor`、`PerformanceMonitorInterceptor` 手写通配符匹配）。
- `UrlPathHelper` → `ServletRequestPathUtils`（`DefaultPerformanceMonitor`）。
- `HttpHeaders` 当 Map 用处按新 API 改。
- springdoc 2.2.0 → 3.x。
- 建议：借此窗口一并修复审查中 web 的安全默认值链路问题（OPTIONS 绕过、缓存串数据、脱敏不生效）。

### 6.4 Gateway

- 依赖坐标 `spring-cloud-starter-gateway` → `spring-cloud-gateway-server-webflux`。
- **最高风险点**：`GatewayRoutesAutoConfiguration` 直接注入并改写 `org.springframework.cloud.gateway.config.GatewayProperties`，Gateway 5.x 该类包路径与配置前缀重组（`spring.cloud.gateway.server.webflux.*`）。建议改用 `RouteLocatorBuilder`/`RouteDefinitionLocator` Bean，不再改属性对象。
- 现有过滤器（`CorsWebFilter`、`GlobalFilter`、`KeyResolver`）都是 WebFlux 形态，迁 `-server-webflux`。

### 6.5 RPC-gRPC（需重构）

- net.devh starter 无 Boot 4 版本，转投 **Spring gRPC**（`org.springframework.grpc`）或 Boot 4.1 内建 gRPC。
- 受影响：`GrpcRpcServer`（`net.devh...@GrpcService` 注解）、examples 的 `grpc.server.port` 配置（spring-grpc 属性是 `spring.grpc.server.port`）、`NacosServiceAutoRegistrar` 读取的 grpc 端口 key。
- 移除遗留 `javax.annotation-api:1.3.2`（改 `org.apache.tomcat:annotations-api` 或重新生成 proto stub）。
- 建议：借重构一并修复审查 RDG-2（Channel 生命周期按实例重建）、RDG-6（metadata 传播两路径不一致）。

### 6.6 Messaging

- **RabbitMQ**：删版本锁定交 BOM；`Jackson2JsonMessageConverter` 在 spring-amqp 4.0 被替换/弃用需改。
- **RocketMQ**：`RocketMQMessageConsumer` import 的 `org.apache.rocketmq.common.protocol.heartbeat.MessageModel` 是 4.x 内部包，5.x 已迁 `...remoting.protocol.heartbeat`，升级即编译失败；18 级延迟映射是 4.x 语义，5.x 支持任意精度定时需重写；4.x 传递旧 netty/fastjson 与 SB4 BOM 冲突面大。**建议随升级一并迁 RocketMQ 5.x 客户端。**
- 自动配置类 `@Configuration` → `@AutoConfiguration(proxyBeanMethods=false)`。

### 6.7 Search

- ES Client 8.11.1 → 9.x，`RestClient` → `Rest5Client`，`RestClientBuilderCustomizer` → `Rest5ClientBuilderCustomizer`。
- `ElasticsearchAutoConfiguration` 大量用 `org.apache.http.*`（HttpClient 4.x）需迁 HttpComponents 5。
- 建议：一并修复审查 SSA-2（认证回调被覆盖）。

### 6.8 AI

- Spring AI 1.1.0 → 2.0.0，基于 Framework 7，不能跑 Boot 3。API 破坏性变化，需专项评估：`OpenAiApi.builder`、`OpenAiChatModel.builder`、`ChromaApi` 构造签名、`VectorStore` 接口、`ToolCallback`/`ToolDefinition`（MCP）、`spring.ai.mcp.server.*` 属性名。
- 这是生态里最新 GA（不到一个月）的一环，建议放到升级后期单独立项。

### 6.9 Data / Lock

- MyBatis-Plus 换 `mybatis-plus-spring-boot4-starter`。
- HikariCP 直接用原生 API，Boot 4 迁包不影响；HikariCP 6/7 API 兼容。
- ShardingSphere 5.x 官方尚未声明 Boot 4/Framework 7 兼容，需单独验证（其传递依赖 groovy/snakeyaml 历来是钉子户）。
- Redisson 交 BOM 升到 4.x。
- 测试依赖 `it.ozimov:embedded-redis 0.7.3` 已停维护，换 testcontainers。
- 健康检查类 `org.springframework.boot.actuate.health.*` → `org.springframework.boot.health.contributor.*`。

### 6.10 Starter 体系

- `spring-boot-starter-web` → `spring-boot-starter-webmvc`（模块化拆分后 web 场景推荐名），逐个 starter 核对。
- `spring-boot-starter-data-redis-reactive`、resilience4j starter 等改名敏感点。
- 借升级修复审查发现的死键、optional 滥用、api starter 夹带 ORM。

---

## 7. 构建工具链要求

- **Maven** ≥ 3.6.3。
- **maven-compiler-plugin**：Java 21 沿用现版本（3.11.0）一般可用；移除 `--enable-preview`，改 `<release>21`。
- **maven-surefire-plugin**：建议升 **3.5.5+**（Boot 4 测试走 JUnit Platform）。
- **spring-boot-maven-plugin 4.x**：uber jar 嵌入式 launch script 移除；war 部署需 `spring-boot-starter-tomcat-runtime`；4.1 起 AOT 跳过测试只认 `-Dmaven.test.skip`。
- **flatten-maven-plugin**：`${revision}` 机制保留，1.7.x 与 Boot 项目兼容；与 Maven 4 兼容性未查到明确声明（当前 Maven 3 无碍）。
- **Lombok** 升 1.18.42+。

---

## 8. 分阶段实施计划

```
阶段 0：前置清理（在 3.5.16 上，1 周）
  - 升 3.5.8 → 3.5.16，消除全部 deprecation 告警
  - 移除 --enable-preview；删 spring-rabbit 版本锁定
  - 修 EPP/ImportFilter 注册；删 task spring.factories 死文件；删 JAR 内 application.yml
  - 引入 spring-boot-properties-migrator（临时），跑一遍收集属性改名报告
  验收：3.5.16 全绿，无 deprecation，starter defaults 集成测试通过

阶段 1：切基线到 Boot 4.1（2-3 周）
  - parent 升 4.1.0，Spring Cloud 升 2025.1.2
  - 先用 spring-boot-starter-classic 跑通编译
  - 处理包名迁移（EPP、健康检查、@AutoConfigureBefore 全限定名）
  - Jackson 2 → 3（收口 JsonUtils 后统一迁）
  - 属性改名按 migrator 报告落实
  验收：全模块编译通过，最小应用可启动

阶段 2：重灾模块重构（3-4 周，可并行）
  - Gateway 坐标改名 + 路由改 RouteLocator
  - gRPC 转 Spring gRPC / Boot 4.1 内建
  - ES 换 Rest5Client
  - RabbitMQ 交 BOM；RocketMQ 评估迁 5.x
  验收：各模块示例应用端到端跑通

阶段 3：AI 模块 + 收尾（2 周）
  - Spring AI 1.1 → 2.0 专项
  - 测试改造（@MockitoBean 等）；移除 properties-migrator
  - 逐 starter 从 classic 换细粒度依赖
  - 全量回归 + 性能对比
  验收：examples 全部绿；生产灰度

强烈建议：阶段 0/1 的每一步都补集成测试（审查报告横切主题第 1 条）。
没有测试兜底的升级，等于把三个"机制性静默失效"的风险放大到整个 4.x 基线。
```

---

## 9. 风险登记

| 风险 | 等级 | 应对 |
|------|------|------|
| Spring AI 2.0 刚 GA，API 不稳定 | 高 | 放到阶段 3 单独立项，锁定具体 patch 版本 |
| ShardingSphere 未声明 Boot 4 兼容 | 高 | 阶段 1 优先验证；不通过则临时禁用分库分表模块 |
| RocketMQ 4.x → 5.x 语义变化（延迟等级） | 中 | 迁移时重写延迟映射，端到端验证 |
| net.devh → Spring gRPC 行为差异 | 中 | 保留旧分支灰度对比，metadata 传播重点回归 |
| 无集成测试导致升级后静默失效 | 高 | 阶段 0 起同步补测试，作为升级验收门槛 |
| SCA / Redisson 对 4.1.0 精确兼容未确认 | 中 | 动手前二次核对 releases，必要时实测 |
| flatten-plugin + `${revision}` 在新工具链行为 | 低 | 阶段 1 早期验证 install/deploy |

---

## 10. 一句话给决策者

3.5 线已停免费支持，升级不是"要不要"而是"什么时候"；建议直接升到 4.1.0（多半年支持期），按"先清废弃、再过 4.0、最后到 4.1"三步走；最大工作量在模块化拆分对自建装配层的冲击、Jackson 3、ES 客户端、Gateway 坐标、gRPC 转投这五处；**并且必须把"补自动装配集成测试"作为升级的硬门槛**——否则本次审查暴露的三个"整条链路静默失效"会在 4.x 上重演且更难发现。
