# 系统实现文档 -- Nebula 剩余工作（交付编码代理执行）

> 面向对象：执行编码工作的 AI 代理（Opus 4.6）
> 配套文档：`spec.md`（为什么改）、`tasks.md`（任务清单与勾选）、`log.md`（过程记录）
> 本文档定位：**怎么改**——每个任务的代码现场、精确改法、测试要求、验证闸门
> 文档内全部行号已于 2026-07-08 在 `nebula-remaining-work` 分支（基线 commit `1f49a363`）核实

---

## 0. 执行纪律（必读）

1. **分支**：全部工作在 `nebula-remaining-work` 分支进行。
2. **一任务一提交**：每完成一个 Task，勾选 `tasks.md` 对应项后单独 `git commit`。提交信息格式：`fix(模块): 摘要 (Task N / 问题编号)`，如 `fix(web): starter 默认锁定 MVC 走 Jackson2 恢复脱敏 (Task 1 / C-1)`。
3. **验证闸门**：每个任务的"验证命令"必须实际执行且通过才算完成；失败不许跳过、不许注释测试。
4. **偏差处理**：实现与本文档不一致时，先改文档（本文件 + tasks.md）再写代码，偏差记入 `log.md` 的"Spec-Code 偏差"。
5. **禁止事项**：
   - 不夹带任务范围外的重构/清理（发现新问题记 `log.md`，不顺手修）
   - 不改 `core/nebula-foundation`、`core/nebula-security` 的公开方法签名（新增可以）
   - 不引入 spring-security / spring-data-jpa / Druid
   - 不在实现类上加 `@Service`（Bean 统一由 AutoConfiguration 的 `@Bean` 方法管理）
   - 代码与注释中不使用表情符号；注释用中文
6. **阶段收尾**：每阶段最后跑 `mvn clean compile && mvn test`（全仓），结果记入 `log.md`。
7. **顺序**：阶段一按 Task 1→2→3→9→4→5→6→8→7→10→11→12→13 执行（Task 9 必须先于 Task 4；Task 11 先于 Task 12，同文件避免冲突；其余按编号）。

---

## 1. 阶段一：审查问题修复（13 任务，代码级说明）

### Task 1 [C-1] Starter 默认锁定 MVC 走 Jackson 2

**背景**：SB4.1 下 MVC 默认走 Jackson 3 转换器，而框架的 `@SensitiveData` 脱敏与日期定制注册在 Jackson 2 的 `ObjectMapper` 上，导致 Controller 响应脱敏静默失效。

**改法**：

1. 三份 defaults 文件各追加一行（文件现状均只有 `nebula.*.enabled` 项）：

```properties
# MVC 消息转换器锁定 Jackson 2: 框架的脱敏(@SensitiveData)与日期定制注册在 Jackson2 ObjectMapper 上,
# SB4.1 默认优先 Jackson3 会绕过这些定制; Jackson3 全量迁移完成后此行随之移除(阶段三 Task 3-6)
spring.http.converters.preferred-json-mapper=jackson2
```

- `starter/nebula-starter-web/src/main/resources/META-INF/nebula-defaults.properties`
- `starter/nebula-starter-all/src/main/resources/META-INF/nebula-defaults.properties`
- `starter/nebula-starter-mcp/src/main/resources/META-INF/nebula-defaults.properties`

2. `application/nebula-web/src/main/java/io/nebula/web/config/JacksonConfig.java`（现 16 行，Bean 为 `Jackson2ObjectMapperBuilderCustomizer`）：类 Javadoc 追加说明——SB4.1 下本定制只作用于 Jackson 2 `ObjectMapper`；MVC 响应要走该 mapper 需 `spring.http.converters.preferred-json-mapper=jackson2`（nebula starter 已通过 defaults 注入；裸依赖 nebula-web 的应用需自行配置）。

**测试（重要，实现方式与 tasks.md 的字面表述有已知修正）**：

`autoconfigure` 测试模块**不能**依赖任何 starter（starter → autoconfigure 的依赖方向，反向加测试依赖会形成 Maven 循环依赖）。因此"web/all/mcp defaults 注入生效"拆成两半验证：

- **机制半**（已存在，勿动）：`autoconfigure/nebula-autoconfigure/src/test/java/io/nebula/autoconfigure/env/NebulaStarterDefaultsIntegrationTest.java` 用测试 classpath 的哨兵 defaults 文件证明"EPP 会把任意 nebula-defaults.properties 以最低优先级注入且用户可覆盖"。
- **内容半**（新增）：在**每个 starter 模块自身**新增最小集成测试（starter 依赖 autoconfigure，其 main resources 的 defaults 文件天然在自己的测试 classpath 上，无循环依赖）：

```java
// starter/nebula-starter-web/src/test/java/io/nebula/starter/web/StarterDefaultsInjectionTest.java
// starter-all、starter-mcp 各一份同构测试（包名对应调整）
@SpringBootTest(classes = StarterDefaultsInjectionTest.MinimalApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StarterDefaultsInjectionTest {

    @Autowired
    private Environment environment;

    @Test
    void preferredJsonMapperDefaultsToJackson2() {
        assertThat(environment.getProperty("spring.http.converters.preferred-json-mapper"))
                .isEqualTo("jackson2");
    }

    @SpringBootConfiguration
    static class MinimalApp { }
}
```

注意：`MinimalApp` 不加 `@EnableAutoConfiguration`，上下文最小化，不会拉起 DB/Redis 等外部依赖；starter 若无 `src/test` 目录则新建；starter pom 需含 `spring-boot-starter-test`（test scope，若缺则补）。

**验证命令**：
`mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=NebulaStarterDefaultsIntegrationTest && mvn test -pl starter/nebula-starter-web,starter/nebula-starter-all,starter/nebula-starter-mcp`

---

### Task 2 [C-1] MockMvc 脱敏回归哨兵测试

**新增** `application/nebula-web/src/test/java/io/nebula/web/autoconfigure/SensitiveDataMvcMaskingTest.java`：

- `@SpringBootTest` + `@AutoConfigureMockMvc`，测试属性带 `spring.http.converters.preferred-json-mapper=jackson2`（显式带是刻意设计，理由见 tasks.md Task 2 说明段）。
- 测试内注册一个 `@RestController`（内部静态类），返回含 `@SensitiveData(type = SensitiveType.PHONE)` 字段（值 `13812345678`）的 DTO。
- 断言 `jsonPath("$.phone").value("138****5678")`（若 Controller 用 `Result` 包装则为 `$.data.phone`；脱敏注解与掩码格式以 `application/nebula-web` 现有 `SensitiveData`/`SensitiveDataMaskingCustomizerTest` 为准，先读它们再写）。
- **测试有效性人工验证**：写完后临时删掉测试属性跑一次，确认测试转红（证明哨兵真的在看门），恢复后将该结果记入 `log.md`。

**验证命令**：`mvn test -pl application/nebula-web -Dtest=SensitiveDataMvcMaskingTest`

---

### Task 3 [C-2] nebula-rpc-grpc 接入 Boot 官方 gRPC Server Starter

**背景**：现状 `infrastructure/rpc/nebula-rpc-grpc/pom.xml:38-42` 依赖 `org.springframework.grpc:spring-grpc-core`——只有 API 没有自动配置，`GrpcRpcServer`（`BindableService` 实现）无宿主拉起，gRPC 服务端实际不监听（CRITICAL）。

**改法**：

1. `infrastructure/rpc/nebula-rpc-grpc/pom.xml`：`spring-grpc-core` 依赖替换为：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-grpc-server</artifactId>
</dependency>
```

（Boot 4.1 BOM 托管版本；保留 `grpc-netty-shaded`/`grpc-protobuf`/`grpc-stub`——客户端自建 Channel 仍需。）

2. 根 `pom.xml`：检索 `spring-grpc-core` 是否还有其他模块引用（`rg "spring-grpc" --type xml -g '!target'`），无引用则删除 dependencyManagement 条目与相关 version 属性。

3. `GrpcRpcServer.java`（`infrastructure/rpc/nebula-rpc-grpc/.../server/GrpcRpcServer.java:24-33`）：类 Javadoc 更新——本类 extends `GenericRpcServiceGrpc.GenericRpcServiceImplBase`（即 `BindableService`），由 Boot gRPC Server 自动配置发现并挂载，无需自建 `Server`。

**已核实的现状**（不需要改）：`GrpcRpcAutoConfiguration`（autoconfigure 模块）已定义 `grpcRpcServer` Bean、`GrpcAuthTokenInterceptor` 已带 `@GlobalServerInterceptor`（import `org.springframework.grpc.server.GlobalServerInterceptor`，与 Boot starter 同一体系，无需改）。

**验证命令**：`mvn -q compile -pl infrastructure/rpc/nebula-rpc-grpc -am && mvn dependency:tree -pl infrastructure/rpc/nebula-rpc-grpc`（确认出现 spring-boot-starter-grpc-server、无 grpc/netty 版本冲突，结论记 log.md）

---

### Task 9 [M-2] EPP 迁移至 SB4 新接口与新注册键（先于 Task 4）

**已核实**：SB4.1 的 `spring-boot-4.1.0.jar` 中新旧接口并存——`org/springframework/boot/EnvironmentPostProcessor.class`（新）与 `org/springframework/boot/env/EnvironmentPostProcessor.class`（旧，兼容保留）。

**改法**：

1. `autoconfigure/.../env/NebulaStarterDefaultsPostProcessor.java:4`：import `org.springframework.boot.env.EnvironmentPostProcessor` 改为 `org.springframework.boot.EnvironmentPostProcessor`。
2. `autoconfigure/.../ai/NebulaMcpEnvironmentPostProcessor.java`（import 同改；该类还实现 `Ordered`，保留不动）。
3. `autoconfigure/nebula-autoconfigure/src/main/resources/META-INF/spring.factories`：注册键 `org.springframework.boot.env.EnvironmentPostProcessor=` 改为 `org.springframework.boot.EnvironmentPostProcessor=`（**只登记新键，不双注册**，否则 EPP 执行两次）；文件内注释同步更新。
4. **删除死代码**（外部审查 P1-4 已核实）：
   - 删 `starter/nebula-starter-gateway/src/main/java/io/nebula/autoconfigure/gateway/GatewayGrpcServerExcludeConfiguration.java`（其排除的 3 个 `net.devh.*` 类全仓已无依赖，排除不存在的类是空操作）
   - 删 `starter/nebula-starter-gateway/src/main/resources/META-INF/spring.factories`（该文件只注册这一个 EPP）

**验证命令**：`mvn test -pl autoconfigure/nebula-autoconfigure`（`NebulaStarterDefaultsIntegrationTest` 是本任务的回归哨兵——若新键注册失败它必红）；另 `rg "net.devh" --type java` 主代码零命中。

---

### Task 4 [C-2] gRPC 端口桥接 EPP + 旧端口配置废弃标注

**关键语义**：`Environment.getProperty("nebula.rpc.grpc.server.port")` 只在用户**显式配置**时非 null（`GrpcRpcProperties.ServerConfig.port` 的默认值 9090 在类字段上，不在 Environment 里），所以"仅在用户配了 nebula 键且没配 spring 键时桥接"天然成立。

**新增** `autoconfigure/.../env/NebulaGrpcServerPortBridgePostProcessor.java`：

```java
public class NebulaGrpcServerPortBridgePostProcessor implements EnvironmentPostProcessor {
    // 注意 import 用新接口 org.springframework.boot.EnvironmentPostProcessor（Task 9 已迁移）
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String legacy = environment.getProperty("nebula.rpc.grpc.server.port");
        if (legacy == null || environment.getProperty("spring.grpc.server.port") != null) {
            return; // 未配旧键,或新键已配(新键优先),都不桥接
        }
        Map<String, Object> props = Map.of("spring.grpc.server.port", legacy);
        environment.getPropertySources().addLast(
                new MapPropertySource("nebula-grpc-port-bridge", props));
    }
}
```

注册到 `autoconfigure` 的 `spring.factories` 新键下（追加到 Task 9 改好的列表）。

`GrpcRpcProperties.java`（:35-77 `ServerConfig`）：`port` 字段加 `@Deprecated` + Javadoc 指向 `spring.grpc.server.port`（桥接仍生效）；`maxInboundMessageSize`/`keepAliveTime`/`keepAliveTimeout`/`permitKeepAliveWithoutCalls`/`maxConcurrentCalls` 各补 Javadoc"改用 spring.grpc.server.*，本参数已无消费者，阶段二 Task 2-3 删除"。`authToken`（:76）不动。

**新增测试** `autoconfigure/.../env/NebulaGrpcServerPortBridgeTest.java`：不需要起 gRPC，直接 new EPP + `MockEnvironment` 三态断言——只配旧键→新键出现且值一致；两者都配→新键保持用户值；都不配→不注入。

**验证命令**：`mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=NebulaGrpcServerPortBridge*`

---

### Task 5 [C-2] gRPC 服务端回环集成测试

**新增** `infrastructure/rpc/nebula-rpc-grpc/src/test/java/io/nebula/rpc/grpc/GrpcServerLoopbackIntegrationTest.java`：

- `@SpringBootTest` 最小配置类（自行 `@Bean` 装配 `GrpcRpcServer`/`GrpcAuthTokenInterceptor`，或引入 autoconfigure 测试依赖），属性 `spring.grpc.server.port=0`（随机端口）。
- 端口注入注解：spring-grpc 测试支持在 `org.springframework.grpc.test` 包（`spring-grpc-test` artifact，Boot BOM 托管），注解名以实际 jar 为准（预期 `@LocalGrpcPort`）；**执行时先 `unzip -l` 或查依赖确认坐标与注解名，结论记 log.md**。若测试支持不可用，退路：固定用 `ServerSocket(0)` 预取空闲端口配置给 `spring.grpc.server.port`。
- 用例一（回环）：`GrpcRpcClient` 指向 `localhost:<port>`，注册一个带 `@RpcService` 的简单测试服务，调用并断言返回值。
- 用例二（token）：属性加 `nebula.rpc.grpc.server.auth-token=test-token`，无 metadata 调用断言 `StatusRuntimeException` 且 `Status.UNAUTHENTICATED`；带 `x-nebula-rpc-token: test-token`（metadata key 见 `GrpcAuthTokenInterceptor.java:24`）调用成功。

**完成后**：`docs/changes/nebula-hardening-sb4-upgrade/tasks.md` 中 T-B2-2 勾选（引用本测试）。

**验证命令**：`mvn test -pl infrastructure/rpc/nebula-rpc-grpc -Dtest=GrpcServerLoopbackIntegrationTest`

---

### Task 6 [H-1] HTTP RPC 客户端注入鉴权 token

**现场**：`HttpRpcClient` 构造函数在 :42（4 参）；发请求在 `sendRequest(...)`:312-332，请求头设置点 :320-325；服务端头名常量 `HttpRpcController.AUTH_TOKEN_HEADER = "X-Nebula-Rpc-Token"`（`HttpRpcController.java:29`）。

**改法**：

1. `HttpRpcProperties.ClientConfig`（`infrastructure/rpc/nebula-rpc-http/.../config/HttpRpcProperties.java`，ClientConfig 起于 :113）：新增字段

```java
/**
 * 调用下游 /rpc 端点时携带的鉴权 token(可选,默认空=不携带)。
 * 下游服务端配置 nebula.rpc.http.server.auth-token 后,本端必须配置一致的值才能调通。
 */
private String authToken = "";
```

2. `HttpRpcClient.java`：
   - 主构造函数扩为 5 参：`(RestClient, String baseUrl, Executor, ObjectMapper, String authToken)`，字段 `private final String authToken;`（null 归一为 ""）。
   - **保留 4 参构造函数**，委托 5 参传 `""`（兼容既有测试 `HttpRpcClientTest`/`HttpRpcClientTargetIsolationTest` 与潜在下游，破坏面最小化——与 tasks.md"破坏性变更"表述的偏差记 log.md）。
   - `sendRequest` :320 的链式调用中追加：

```java
var spec = restClient.post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("X-Request-ID", request.getRequestId());
if (!authToken.isEmpty()) {
    spec = spec.header("X-Nebula-Rpc-Token", authToken);
}
return spec.body(jsonBody).retrieve().body(RpcResponse.class);
```

（头名直接用字符串或引用 `HttpRpcController.AUTH_TOKEN_HEADER`——注意 client 与 server 同模块，可引用常量，优先引用。）

3. `HttpRpcAutoConfiguration.java:102`：`new HttpRpcClient(rpcRestClient, baseUrl, rpcExecutor, objectMapper)` 改传 5 参，追加 `properties.getClient().getAuthToken()`。

4. **新增测试** `infrastructure/rpc/nebula-rpc-http/src/test/java/io/nebula/rpc/http/server/HttpRpcAuthTokenRoundTripTest.java`（同包已有 `HttpRpcControllerAuthTest` 可参考其测法）：server 配 token + client 配相同 token 调通；client 无 token 得 401。

**验证命令**：`mvn test -pl infrastructure/rpc/nebula-rpc-http`

---

### Task 8 [M-1] /rpc token 常量时间比较

**现场**：`HttpRpcController.java:52-60`，现为 `!authToken.equals(provided)`。

**改法**（与 Task 6 同模块，允许合并为一次提交，但 tasks.md 分开勾选）：

```java
if (!authToken.isEmpty()) {
    String provided = httpRequest.getHeader(AUTH_TOKEN_HEADER);
    boolean pass = provided != null && java.security.MessageDigest.isEqual(
            authToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            provided.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    if (!pass) {
        // ... 原 401 分支不变
    }
}
```

**验证命令**：`mvn test -pl infrastructure/rpc/nebula-rpc-http`（`HttpRpcControllerAuthTest` 既有用例必须仍绿）

---

### Task 7 [H-2] ServiceImpl 列名白名单校验

**现场**：`infrastructure/data/nebula-data-persistence/.../service/impl/ServiceImpl.java` —— `findByField`:43、`findOneByField`:48、`findByFields`:55（`fieldValues.forEach(wrapper::eq)` :59）。

**改法**：

```java
private static final java.util.regex.Pattern SAFE_COLUMN = java.util.regex.Pattern.compile("^[A-Za-z0-9_]+$");

/**
 * 校验列名仅含字母/数字/下划线,阻断 findByField 系列的 SQL 注入面。
 * QueryWrapper.eq(String column, ...) 的列名会被直接拼进 SQL,不能信任调用方传入的任意字符串。
 */
private static String validateColumn(String field) {
    if (field == null || !SAFE_COLUMN.matcher(field).matches()) {
        throw new io.nebula.core.common.exception.ValidationException(
                "非法列名: " + field + " (仅允许字母/数字/下划线)");
    }
    return field;
}
```

三个方法接入：`eq(validateColumn(field), value)`；`findByFields` 改 `fieldValues.forEach((f, v) -> wrapper.eq(validateColumn(f), v))`。
注意：`ValidationException` 的实际包路径与构造函数先 `rg "class ValidationException" core/` 核实再引用。

**新增测试** `ServiceImplColumnValidationTest`：合法列名（`user_name`、`age2`）不抛；非法输入（`"name; DROP TABLE x"`、"name`)"、`"a-b"`、`null`、空串）抛 `ValidationException`。纯校验逻辑可直接测静态行为（通过反射或包内可见性；若不便，容忍把 `validateColumn` 设为 package-private）。

**验证命令**：`mvn test -pl infrastructure/data/nebula-data-persistence`（既有 `ServiceImplQueryTest` 必须仍绿）

---

### Task 10 [M-3] 缓存健康检查键前缀 + 统计原子化

**现场**：`infrastructure/data/nebula-data-cache/.../impl/DefaultCacheManager.java` —— `keyPrefix` 字段 :45（默认 `nebula:cache:`）；`evictionCount` 为 `volatile long` :39，写点 :189（`++`）、:210（`+= deleted`）、:687（`+= keys.size()`），读点 :750；健康检查键 :708 `set("health:check", ...)`。

**改法**：

- :708 → `redisTemplate.opsForValue().set(keyPrefix + "health:check", "ok", Duration.ofSeconds(1));`
- :39 → `private final java.util.concurrent.atomic.LongAdder evictionCount = new LongAdder();`
- :189 → `evictionCount.increment();`；:210 → `evictionCount.add(deleted);`；:687 → `evictionCount.add(keys.size());`；:750 → `return evictionCount.sum();`

**验证命令**：`mvn test -pl infrastructure/data/nebula-data-cache`

---

### Task 11 [M-4] ES trust-all 生产环境防护

**现场**：`autoconfigure/.../search/ElasticsearchAutoConfiguration.java` —— `createSSLContext()`:164，`!properties.isSslVerificationEnabled()` 分支 :165-173 直接 trust-all。

**参照实现**（对齐爬虫模块语义，`HttpCrawlerEngine.java:44,419-433`）：`SAFE_PROFILES = Set.of("dev","test","local")`；**无 active profile → 视为非生产**；有 profile 且全不在安全集 → 生产。

**改法**：

1. 类中注入 `org.springframework.core.env.Environment`（该类如为构造注入风格则加构造参数；先读类头部确认注入方式）。
2. :165 分支改为：

```java
if (!properties.isSslVerificationEnabled()) {
    if (isProductionProfile()) {
        logger.error("生产环境禁用 SSL 证书校验被拒绝: sslVerificationEnabled=false 在生产 profile 下忽略, 走默认证书校验");
        // 不 return trust-all, 落到方法末尾的默认校验流程
    } else {
        // ... 原 trust-all 逻辑（仅非生产可达）
        return sslContext;
    }
}
```

3. `isProductionProfile()` 私有方法照抄爬虫语义（见上）。

**新增测试** `autoconfigure/.../search/ElasticsearchSslGuardTest.java`：由于 `createSSLContext` 是私有方法，用反射或将判定逻辑抽为 package-private 静态方法测三态：无 profile→非生产；`dev`→非生产；`prod`→生产。

**验证命令**：`mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=ElasticsearchSslGuardTest`

---

### Task 12 [L 批] 三处小修（Task 11 之后做，同文件顺序执行）

1. `core/nebula-security/.../JwtAuthenticationFilter.java:161`：`readStringList` 末尾流水线 `.filter(s -> !s.isEmpty()).filter(Objects::nonNull)` 中的 `.filter(Objects::nonNull)` 冗余（`map(String::trim)` 之后不可能有 null），删除该行；`Objects` import 若仅此处使用一并删（先 `rg "Objects\." 该文件` 确认）。核心模块，仅此一行，无行为变化。
2. `autoconfigure/.../search/ElasticsearchAutoConfiguration.java:143-144`：`elasticsearchClient(...)` 中 `objectMapper.registerModule(new JavaTimeModule())` 直接改写了全局共享的 `ObjectMapper` Bean。改为：

```java
ObjectMapper mapper = objectMapper.copy();
mapper.registerModule(new JavaTimeModule());
JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);
```

3. `infrastructure/rpc/nebula-rpc-core/.../discovery/ServiceDiscoveryRpcClient.java:92`：`callAsync` 的 `CompletableFuture.supplyAsync(() -> call(...))` 未指定 executor（吃公共 ForkJoinPool）。构造函数新增 `Executor asyncExecutor` 参数（同 Task 6 手法：保留旧构造函数委托新构造传 `ForkJoinPool.commonPool()` 保持兼容），`supplyAsync(..., asyncExecutor)`。装配点在 `autoconfigure/.../rpc/RpcDiscoveryAutoConfiguration.java`——先读它确认建 `ServiceDiscoveryRpcClient` 的位置，把已有的 `rpcExecutor` Bean 传入（若该上下文拿不到则维持 commonPool 缺省并记 log.md）。

**验证命令**：`mvn -q compile && mvn test -pl core/nebula-security,infrastructure/rpc/nebula-rpc-core`

---

### Task 13 全量回归 + 文档收尾

按 tasks.md Task 13 列表执行：T-B2-2 勾选、审查报告逐条标"已修复（commit hash）"、tasks.md 变更摘要回填、spec 状态改 done、`CLAUDE.md` v2.0.x 变更记录补充本批（重点：新增配置项 `nebula.rpc.http.client.auth-token`、`spring.http.converters.preferred-json-mapper` defaults、EPP 新键迁移、gateway 死代码删除）。

**验证命令**：`mvn clean compile && mvn test`（全仓，结果记 log.md）

---

## 2. 阶段二：EPIC-C2 治理（8 任务，实现要点）

> 详细任务定义见 `tasks.md` 阶段二。以下只补充已核实的代码事实与实现约束，避免执行时重复调研。

**Task 2-1 死配置盘点**：产出盘点表填入 `log.md` 既有表格。已核实的起点事实：
- `HttpRpcProperties.ClientConfig` 全字段清单见上文 Task 6 现场（:113-192）：`writeTimeout`/`maxConnections`/`maxConnectionsPerRoute`/`keepAliveTime`/`retryCount`/`retryInterval`/`compressionEnabled` 均无真实消费点（仅 `HttpRpcAutoConfiguration.java:172-176` 诊断展示）。
- `connectTimeout`/`readTimeout` 是否已用于 `rpcRestClient` Bean 构建需现场确认（`HttpRpcAutoConfiguration` :40-61 区域）。
- 盘点表须经用户确认后才执行 2-2/2-3（HARD-GATE）。

**Task 2-2 HTTP RPC 参数接通**：`rpcRestClient` Bean 改用 `JdkClientHttpRequestFactory` 或 HttpComponents 工厂应用超时/连接池参数；诊断端点只展示真实生效项。

**Task 2-3 删除批**：`SecurityProperties.anonymousUrls`、`NacosProperties.heartbeatInterval/Timeout`（:93,101）、`GrpcRpcProperties.ServerConfig` 遗留参数（Task 4 已 Javadoc 预告）。删除时同步清对应 AutoConfiguration 诊断展示（`GrpcRpcAutoConfiguration.java:85-86` 的 Max Concurrent/KeepAlive 展示在列）。CHANGELOG（CLAUDE.md v2.0.x）逐项标注破坏性变更。

**Task 2-4 错误码收敛**：`ResultCode.getByCode()` 缺陷修复 + `Result.of(ResultCode)` 工厂新增。硬约束：`Result` 现有方法与码值零改动（下游可能字符串匹配 `"SUCCESS"`）。

**Task 2-5 认证收敛（本批最高风险）**：方向 = `JwtAuthenticationFilter`（security 层，Filter 先行）唯一解析点，`AuthInterceptor` 改读 `SecurityContext` 并桥接 `AuthContext`（公开 API 不变）。集成测试断言同一请求 `JwtService` 解析仅一次（Mockito spy）。动手前通读 `AuthInterceptor`、`AuthContext`、`JwtAuthenticationFilter`、`SecurityContext` 四个类现状。

**Task 2-6 孤儿 DTO**：删 `XxlJobExecuteRequest`/`XxlJobLogRequest`/`XxlJobLogResult`；**保留** `XxlJobResult`（`XxlJobRegistryService.java:169,205` 在用，已核实）。

**Task 2-7 ApplicationContextRunner 测试**：8 个测试类清单与模式见 tasks.md；注意 `ElasticsearchAutoConfigurationConditionTest` 只测条件评估不连真实 ES。

**Task 2-8 文档同步**：CLAUDE.md/AGENTS.md 行号引用逐条核对（辅助脚本：对每条引用 rg 类名取实际行号）；`.cursor/rules/project.mdc` 中被删配置项/鉴权说明段落同步。

---

## 3. 阶段三：Jackson 2 → 3 迁移（7 任务，API 对照与关键事实）

> 任务划分见 `tasks.md` 阶段三（Task 3-0 ~ 3-6）。以下是执行时直接用的迁移对照。

### 3.1 坐标与包名对照

| Jackson 2 | Jackson 3 | 备注 |
|---|---|---|
| groupId `com.fasterxml.jackson.core` | `tools.jackson.core` | jackson-core/jackson-databind |
| 包 `com.fasterxml.jackson.databind.*` | `tools.jackson.databind.*` | ObjectMapper/JsonNode/JavaType 等 |
| 包 `com.fasterxml.jackson.core.*` | `tools.jackson.core.*` | JsonParser/JsonGenerator 等 |
| **注解包 `com.fasterxml.jackson.annotation`** | **不变**（jackson-annotations 3.x 保留原包名） | `@JsonProperty`/`@JsonIgnore` 等**不用改** |
| `jackson-datatype-jsr310`（JavaTimeModule） | 并入 databind 内置 | java.time 开箱支持，`JavaTimeModule` 注册代码直接删 |
| `ObjectMapper` 可变配置 | `ObjectMapper` 不可变；构造用 `JsonMapper.builder()...build()` | `registerModule` → builder `.addModule(...)` |
| 受检 `JsonProcessingException` | 非受检 `tools.jackson.core.JacksonException` | try-catch 与 throws 声明需调整 |

**重要修正**（对 spec/tasks 验收标准的口径澄清）：由于注解包不改名，"`rg com.fasterxml.jackson` 主代码零命中"应修正为"**零 `com.fasterxml.jackson.databind` / `com.fasterxml.jackson.core` / `com.fasterxml.jackson.datatype` import**；`com.fasterxml.jackson.annotation` 允许保留"。执行 Task 3-6 时以此口径验收，并同步修正 spec.md 第 2.3 节与验收标准（偏差记 log.md）。

### 3.2 执行要点

- Task 3-0 盘点先行（POM 直接依赖：根 pom `jjwt-jackson`:290 / `spring-boot-jackson2`:355、security:74 与 web:73 的 `jjwt-jackson`、payment 的 `jackson-databind`:56）；jjwt 的 Jackson 3 支持情况上网核实（JJWT 0.13+ release notes），无则按 tasks.md 预判二选一。
- 每模块迁移 = 一次提交；迁移顺序 foundation → data → messaging/rpc/lock → web → search/ai/task/autoconfigure；全程 `mvn clean compile` 保持绿。
- web 层脱敏 customizer 重写后，Task 2 的 MockMvc 哨兵测试把测试属性从 `jackson2` 切到默认（Jackson 3 路径）必须仍绿——这是整个阶段的回归闸门。
- Redis 缓存序列化：Q5 已拍板"升级清缓存"，多态白名单（PolymorphicTypeValidator）安全语义不得减弱；升级指南写清缓存步骤。
- 收尾拆桥：根 POM 删 `spring-boot-jackson2`（:355）、三份 starter defaults 删 `preferred-json-mapper=jackson2`（Task 1 注入项）。

---

## 4. 阶段四：proud-day 接入（外部仓库）

- **Task 4-0（本仓库）**：根 `pom.xml` 的 `<revision>` 由 `2.0.1-SNAPSHOT` 改 `2.1.0-SNAPSHOT`，全仓 `mvn install -DskipTests` 后提交。执行时机：阶段一完成后即可（不必等阶段二三）。
- **Task 4-1（proud-day 仓库）**：按 `/Users/andy/DevOps/SourceCode/business-projects/proud-day-projects/proud-day-backend/docs/changes/nebula-persistence-adoption/tasks.md` 三任务执行，该文档自含全部细节（配置映射表、验收清单）。前置：Task 4-0 落地且本仓库快照可拉取。

---

## 5. 执行顺序总览

```
阶段一: T1 → T2 → T3 → T9 → T4 → T5 → T6 → T8 → T7 → T10 → T11 → T12 → T13
        (T9 必须先于 T4; T11 先于 T12; T6/T8 同模块可合并提交)
阶段一完成后 ──→ Task 4-0(版本号) ──→ Task 4-1(proud-day, 可与阶段二并行)
阶段二: T2-1(盘点,HARD-GATE 用户确认) → T2-2/2-3/2-4/2-6(可并行) → T2-5 → T2-7 → T2-8
阶段三: T3-0(盘点) → T3-1 → T3-2 → T3-3 → T3-4 → T3-5 → T3-6(拆桥)
```

每阶段收尾：全仓 `mvn clean compile && mvn test` + `log.md` 回填 + tasks.md 勾选。
