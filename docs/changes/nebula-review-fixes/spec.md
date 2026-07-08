# 审查问题修复（nebula-review-fixes）变更规格

> 状态：confirmed（2026-07-07 随总体方案 Q1-Q7 一并确认）
> 创建日期：2026-07-07
> 复杂度：复杂
> 来源：`docs/nebula-hardening-code-review-2026-07.md`（代码审查报告）
> 目标分支：`nebula-remaining-work`（2026-07-08 更正：工作分支已自 `nebula-hardening-b` 检出为本分支）

## 1. 背景与目标

代码审查报告对 `nebula-hardening-a` / `nebula-hardening-b` 两分支给出"有条件通过"结论，列出 2 个 CRITICAL、2 个 HIGH、4 个 MEDIUM、3 个 LOW 代码问题。本变更按报告逐项修复，目标：

1. 消除两个"编译通过但运行时静默失效"的 CRITICAL 问题（MVC 脱敏失效、gRPC 服务端不启动），使 `nebula-remaining-work` 分支达到可合入主干状态；
2. 补齐 HIGH 级功能缺口与注入风险；
3. 收敛 MEDIUM/LOW 级技术债，并补充能捕获"静默失效"类回归的集成测试。

做完后的可验证效果：全仓编译与测试通过；带 `@SensitiveData` 字段的 Controller 响应经 MockMvc 断言为脱敏值；gRPC 服务端可启动并被 `GrpcRpcClient` 回环调通（含 token 拦截器）；开启 RPC token 鉴权后框架自身 HTTP RPC 调用可通过认证。

## 2. 代码现状（Research）

> 以下结论均已在审查阶段逐一取证（源码阅读 + 官方 jar 反编译 + 依赖树）。

### 2.1 C-1：MVC 默认走 Jackson 3，脱敏与日期定制被架空

- 脱敏定制注册在 Jackson 2 体系：`application/nebula-web/src/main/java/io/nebula/web/autoconfigure/WebAuthAutoConfiguration.java`（`Jackson2ObjectMapperBuilderCustomizer` + `AnnotationIntrospectorPair`）；日期定制在 `application/nebula-web/src/main/java/io/nebula/web/config/JacksonConfig.java`（同为 Jackson2 customizer）。
- SB 4.1 的 `spring-boot-starter-web` 传递引入 `spring-boot-starter-jackson`（Jackson 3）。反编译 `spring-boot-http-converter-4.1.0.jar` 确认：Jackson 2 转换器启用条件为 `Jackson2HttpMessageConvertersConfiguration$PreferJackson2OrJacksonUnavailableCondition`，即必须显式配置 `spring.http.converters.preferred-json-mapper=jackson2`。
- 全仓无任何 `preferred-json-mapper` 配置（rg 全仓检索为空）。
- Starter 默认值注入机制：`autoconfigure/.../env/NebulaStarterDefaultsPostProcessor.java` 扫描 classpath 上所有 `META-INF/nebula-defaults.properties` 并以最低优先级注入（`addLast`），用户配置可覆盖——是注入该属性的现成通道。
- 含 `nebula-web` 的 starter：`nebula-starter-web`（`nebula-starter-service` 经由它传递获得）、`nebula-starter-all`、`nebula-starter-mcp`，三者均已有 defaults 文件。
- 现有测试 `SensitiveDataMaskingCustomizerTest` 只测 Jackson2 mapper 本体，覆盖不到 MVC 转换器路径。

### 2.2 C-2：gRPC 服务端无宿主

- `infrastructure/rpc/nebula-rpc-grpc/pom.xml:41` 仅引入 `spring-grpc-core`；本地仓库核实无任何 server starter/autoconfigure 构件。
- `GrpcRpcServer`（`infrastructure/rpc/nebula-rpc-grpc/.../server/GrpcRpcServer.java:32`）现在只是 `GenericRpcServiceGrpc.GenericRpcServiceImplBase` 子类（一个 `BindableService` Bean），无人创建/启动 `io.grpc.Server`；`GrpcRpcAutoConfiguration.java:66` 的 `@GlobalServerInterceptor` 无人处理。
- `GrpcRpcAutoConfiguration.java:42,55` server/client 的 `matchIfMissing=true`：`nebula.rpc.grpc.enabled=true` 时客户端优先走 gRPC，对端无监听，静默失败。
- **官方支持现状（已查证 Spring Boot 4.1 官方文档）**：Boot 4.1 起 gRPC 自动配置已并入 Spring Boot，提供 `spring-boot-starter-grpc-server`（Netty 宿主，BOM 托管，已在 `spring-boot-dependencies-4.1.0.pom:2399` 确认）；任何 `BindableService` Bean 自动注册；`@GlobalServerInterceptor` Bean 自动生效；端口配置 `spring.grpc.server.port`（默认 9090，0=随机端口，配 `@LocalGrpcServerPort` 可测）。
- Nacos 注册器 `NacosServiceAutoRegistrar.java:85-92` 已优先读取 `spring.grpc.server.port` 写入元数据——与官方属性天然对齐。
- 端口配置现状：`GrpcRpcProperties.ServerConfig.port`（默认 9090，`nebula.rpc.grpc.server.port`）在新体系下不再被消费，需桥接或废弃。
- 客户端 `GrpcRpcClient` 用 `grpc-netty-shaded` 自建 Channel（`GrpcRpcClient.java:54`），不依赖 starter，无需改动。

### 2.3 H-1：HTTP RPC 客户端不带鉴权 token

- 服务端校验：`infrastructure/rpc/nebula-rpc-http/.../server/HttpRpcController.java:29,52-56`（请求头 `X-Nebula-Rpc-Token`，配置 `nebula.rpc.http.server.auth-token`）。
- 客户端发请求仅设置 `X-Request-ID`（`HttpRpcClient.java:324`），无 token 注入。
- 客户端配置类 `HttpRpcProperties.ClientConfig`（`config/HttpRpcProperties.java:109-193`）无 token 字段；客户端 Bean 装配在 `autoconfigure/.../rpc/HttpRpcAutoConfiguration.java:92-102`（`new HttpRpcClient(restClient, baseUrl, executor, objectMapper)`）。

### 2.4 H-2：`findByField` 列名拼接

- `infrastructure/data/nebula-data-persistence/.../service/impl/ServiceImpl.java:43-62`：`findByField` / `findOneByField` / `findByFields` 将 `field` 参数直接传入 `QueryWrapper.eq(String column, ...)`（列名原样拼接进 SQL）。

### 2.5 MEDIUM/LOW 项

- M-1：`HttpRpcController.java:54` token 比较用 `String.equals`（非常量时间）。
- M-2：EPP 注册于废弃键 `org.springframework.boot.env.EnvironmentPostProcessor`（`autoconfigure/.../META-INF/spring.factories`）；实现类 `NebulaStarterDefaultsPostProcessor` / `NebulaMcpEnvironmentPostProcessor` implements 旧接口 `org.springframework.boot.env.EnvironmentPostProcessor`。反编译确认 SB4.1 经 `SpringFactoriesEnvironmentPostProcessorsFactory#loadDeprecatedPostProcessors` 兼容旧键（当前可用），新正主键为 `org.springframework.boot.EnvironmentPostProcessor`（新旧接口方法签名一致）。
- M-3：`DefaultCacheManager.java:708` 健康检查键 `health:check` 未加 `keyPrefix`（字段声明于 :45，默认 `nebula:cache:`）；`evictionCount` 非原子自增。
- M-4：`ElasticsearchAutoConfiguration.createSSLContext()`（:164-175）在 `ssl-verification-enabled=false` 时信任所有证书，无环境防护；爬虫模块已有可参照实现：`HttpCrawlerEngine.java:94-97,423`（生产 profile 检测 + 拒绝并告警）。
- L-1：`JwtAuthenticationFilter.readStringList` 末尾冗余 `filter(Objects::nonNull)`。
- L-2：`ElasticsearchAutoConfiguration.elasticsearchClient()`（:143-148）直接对注入的共享 `ObjectMapper` 调 `registerModule`，副作用外溢。
- L-3：`ServiceDiscoveryRpcClient.java:92` `callAsync` 用无执行器的 `supplyAsync`（commonPool），与 `HttpRpcClient.callAsync`（自有 executor）不一致。

## 3. 功能点

- [ ] F1（C-1）：框架默认锁定 MVC JSON 序列化为 Jackson 2 —— 引入含 `nebula-web` 的 starter 即自动注入 `spring.http.converters.preferred-json-mapper=jackson2`，用户可覆盖；补 MockMvc 集成测试断言脱敏生效。
- [ ] F2（C-2）：gRPC 服务端恢复可用 —— `nebula-rpc-grpc` 引入 Boot 官方 `spring-boot-starter-grpc-server`，`GrpcRpcServer`（BindableService）与 `GrpcAuthTokenInterceptor`（@GlobalServerInterceptor）由 Boot 自动配置托管启动；`nebula.rpc.grpc.server.port` 桥接到 `spring.grpc.server.port`；补服务端回环集成测试。
- [ ] F3（H-1）：HTTP RPC 客户端 token —— 新增 `nebula.rpc.http.client.auth-token`，`HttpRpcClient` 发请求携带 `X-Nebula-Rpc-Token`。
- [ ] F4（H-2）：`ServiceImpl` 列名白名单 —— `findByField` 系列对列名做 `[A-Za-z0-9_]+` 校验，非法即抛 `ValidationException`。
- [ ] F5（M 批）：token 常量时间比较；EPP 迁移新接口/新键；缓存健康键加前缀 + 统计原子化；ES trust-all 增加生产环境防护。
- [ ] F6（L 批）：冗余 filter 删除；ES 共享 ObjectMapper 改 copy()；`ServiceDiscoveryRpcClient.callAsync` 注入统一 executor。

## 4. 设计决策与业务规则

1. **C-1 采用"锁定 Jackson 2"过渡方案**：Jackson 2→3 全量迁移仍归 T-B3-2（已延后），本次只把 MVC 拉回与现有定制生态一致的 Jackson 2。注入走 starter defaults 机制（最低优先级，用户可覆盖）；不使用 starter 的裸 `nebula-web` 用户需自行配置，在模块 README/Javadoc 标注。
2. **C-2 采用 Boot 官方 starter**（`org.springframework.boot:spring-boot-starter-grpc-server`，BOM 托管无需版本号），不用 spring-grpc 项目自己的 starter——版本随 Boot 升级自动对齐。`spring-grpc-core` 依赖由 starter 传递，可从 pom 移除显式声明。
3. **端口配置桥接而非废弃**：新增 EPP 将 `nebula.rpc.grpc.server.port` 桥接为 `spring.grpc.server.port`（仅当用户未显式配置后者时），`GrpcRpcProperties.ServerConfig.port` 标注 `@Deprecated` 引导迁移。`ServerConfig` 其余线程/流控参数（maxInboundMessageSize 等）暂不桥接，属于原 net.devh 时代遗留，标注文档说明改用 `spring.grpc.server.*`。
4. **EPP 迁移只登记新键不双注册**：同一实现类若同时登记新旧两键会被执行两次；SB4.1 下只登记 `org.springframework.boot.EnvironmentPostProcessor` 新键并 implements 新接口。
5. **H-1 token 独立配置**：`client.auth-token` 与 `server.auth-token` 分开（调用方与被调方 token 语义不同），常见场景配同一值。
6. **H-2 校验策略**：白名单正则 `^[A-Za-z0-9_]+$`，拒绝时抛 `io.nebula.core.exception.ValidationException`；`findByFields` 的 Map key 逐一校验。不做"字段属于实体"级校验（需 TableInfo 反查，成本高于收益）。
7. **M-4 环境防护对齐爬虫模块**：生产 profile（prod/production）下忽略 `ssl-verification-enabled=false` 并 error 日志告警，复用 `HttpCrawlerEngine` 的判定逻辑（不抽公共类，两处独立实现均只有几行，避免为此新建跨模块依赖）。

## 5. 数据变更

无数据库表结构变更。

## 6. 接口/配置变更

| 操作 | 配置项/接口 | 变更内容 |
|------|------|----------|
| 新增 | `spring.http.converters.preferred-json-mapper=jackson2` | 由 starter defaults 注入（web/all/mcp 三个 starter），用户可覆盖 |
| 新增 | `nebula.rpc.http.client.auth-token`（默认空） | HTTP RPC 客户端出站 token |
| 新增依赖 | `spring-boot-starter-grpc-server`（nebula-rpc-grpc） | gRPC 服务端宿主，BOM 托管 |
| 桥接 | `nebula.rpc.grpc.server.port` → `spring.grpc.server.port` | EPP 注入，用户显式配置 `spring.grpc.server.port` 时不桥接 |
| 废弃标注 | `GrpcRpcProperties.ServerConfig.port` | `@Deprecated`，文档指向 `spring.grpc.server.port` |
| 行为变更 | `ServiceImpl.findByField/findOneByField/findByFields` | 非法列名由"拼进 SQL"改为抛 `ValidationException` |
| 行为变更 | ES `ssl-verification-enabled=false` | 生产 profile 下忽略并告警 |
| 迁移 | `spring.factories` EPP 注册键 | 旧键 `...boot.env.EnvironmentPostProcessor` → 新键 `...boot.EnvironmentPostProcessor` |

## 7. 影响范围

| 模块 | 文件 |
|------|------|
| starter/nebula-starter-web, -all, -mcp | `META-INF/nebula-defaults.properties` |
| application/nebula-web | 新增 MockMvc 脱敏集成测试 |
| infrastructure/rpc/nebula-rpc-grpc | `pom.xml`、`GrpcRpcServer`（仅注释）、新增集成测试 |
| autoconfigure/nebula-autoconfigure | `GrpcRpcAutoConfiguration`、新增 `NebulaGrpcServerPortBridge`、`NebulaStarterDefaultsPostProcessor`、`NebulaMcpEnvironmentPostProcessor`、`spring.factories`、`ElasticsearchAutoConfiguration` |
| infrastructure/rpc/nebula-rpc-http | `HttpRpcProperties`、`HttpRpcClient`、`HttpRpcController` |
| infrastructure/rpc/nebula-rpc-core | `ServiceDiscoveryRpcClient` |
| infrastructure/data/nebula-data-persistence | `ServiceImpl` |
| infrastructure/data/nebula-data-cache | `DefaultCacheManager` |
| core/nebula-security | `JwtAuthenticationFilter`（注意：属"不可随意修改"核心模块，本次仅删一行冗余 filter，无行为变化） |

## 8. 风险与关注点

1. **C-1 属安全修复**（敏感数据明文外泄），合入前必须完成；MockMvc 测试是防再次静默回归的哨兵，不可省。
2. **gRPC starter 引入 Netty 依赖**：`spring-boot-starter-grpc-server` 带非 shaded Netty，与现有 `grpc-netty-shaded`（客户端用）并存无冲突，但需编译后核对依赖树确认无版本碰撞。
3. **EPP 迁移是 A1 事故同类高危区**：改错键=Starter 默认值全部静默失效。必须保留/补充"defaults 注入生效"的集成测试作回归哨兵，迁移后显式验证。
4. **H-2 行为收紧**：存量代码若曾传入带反引号/函数的"列名"将开始抛异常——属预期收紧，在 CHANGELOG 标注。
5. **端口桥接次序**：桥接 EPP 与 defaults EPP 均为最低优先级注入，两者键不重叠，无次序依赖。

## 9. 待澄清

> 以下默认按"推荐"执行，若用户有异议在开工前提出。

- [x] Q1：C-1 走"锁定 Jackson2"过渡（推荐，Jackson3 全量迁移仍归 T-B3-2）还是本次直接迁 Jackson3？—— 推荐前者
- [x] Q2：C-2 用 Boot 官方 `spring-boot-starter-grpc-server`（推荐，BOM 托管）还是 spring-grpc 项目的 starter？—— 推荐前者
- [x] Q3：H-1 客户端 token 独立配置项（推荐）还是复用 server.auth-token？—— 推荐独立
- [x] Q4：`nebula.rpc.grpc.server.port` 桥接兼容（推荐）还是直接废弃？—— 推荐桥接 + @Deprecated

## 10. 验收标准

- [ ] `mvn clean compile` 全仓通过；`mvn test` 全量通过
- [ ] MockMvc 测试：带 `@SensitiveData` 字段的 Controller 响应体为脱敏值（不加 defaults 属性时该测试应失败，证明测试有效）
- [ ] gRPC 集成测试：`spring.grpc.server.port=0` 启动真实服务端，`GrpcRpcClient` 回环调用成功；配置 authToken 后无 token 调用被拒、带 token 调用通过
- [ ] 开启 `nebula.rpc.http.server.auth-token` 后，框架 `HttpRpcClient`（配置 client.auth-token）调用返回 200
- [ ] `findByField("name; DROP TABLE x", ...)` 抛 `ValidationException`
- [ ] EPP 迁移后，"引入 starter-web 即启用 persistence/cache 默认值"的既有集成测试仍绿
- [ ] 生产 profile + `ssl-verification-enabled=false` 时 ES 客户端仍做证书校验且输出 error 日志
