# 系统实现文档 -- Nebula 剩余工作（交付编码代理执行）

> 面向对象：执行编码工作的 AI 代理（Opus 4.6）
> 配套文档：`spec.md`（为什么改）、`tasks.md`（任务清单与勾选）、`log.md`（过程记录）
> 本文档定位：**怎么改**——每个任务的代码现场、精确改法、测试要求、验证闸门
> 文档内全部行号已于 2026-07-08 在 `nebula-remaining-work` 分支（基线 commit `1f49a363`）核实

---

## 0. 执行纪律（必读）

1. **分支**：全部工作在 `nebula-remaining-work` 分支进行。
2. **一任务一提交**：每完成一个 Task，勾选 `tasks.md` 对应项后单独 `git commit`。提交信息格式：`fix(模块): 摘要 (Task N / 问题编号)`，如 `fix(web): starter 默认锁定 MVC 走 Jackson2 恢复脱敏 (Task 1 / C-1)`。
3. **验证闸门**：每个任务的"验证命令"必须实际执行且通过才算完成；失败不许跳过、不许注释测试。**每次验证命令的关键输出行（测试数/BUILD SUCCESS 等）必须记入 `log.md` 对应任务条目**——只留"已通过"三个字不算数，要留能对账的证据。
4. **失败上限**：同一任务的验证连续失败 2 次，**停止该任务**：把失败现象（命令、报错关键行、已尝试的修法）记入 `log.md` 踩坑记录并向用户汇报，等待指示。禁止的绕道行为包括但不限于：注释/删除/弱化测试断言、给测试加 `@Disabled`、调低验收标准、跳过该任务继续下一个。
5. **偏差处理**：实现与本文档不一致时，先改文档（本文件 + tasks.md）再写代码，偏差记入 `log.md` 的"Spec-Code 偏差"。
6. **行号时效**：文档内行号基于基线 commit `1f49a363`，随着任务推进会漂移属正常现象。动手前用 rg 按类名/方法名现场定位，以代码现状为准；若现场与文档描述的"现场"结构性不符（类不存在、方法签名不同），按第 5 条偏差处理，不许凭猜测硬改。
7. **禁止事项**：
   - 不夹带任务范围外的重构/清理（发现新问题记 `log.md`，不顺手修）
   - 不改 `core/nebula-foundation`、`core/nebula-security` 的公开方法签名（新增可以）
   - 不引入 spring-security / spring-data-jpa / Druid
   - 不在实现类上加 `@Service`（Bean 统一由 AutoConfiguration 的 `@Bean` 方法管理）
   - 代码与注释中不使用表情符号；注释用中文
8. **阶段收尾**：每阶段最后跑 `mvn clean compile && mvn test`（全仓），结果记入 `log.md`。
9. **顺序**：阶段一按 Task 1→2→3→9→4→5→6→8→7→10→11→12→13 执行（Task 9 必须先于 Task 4；Task 11 先于 Task 12，同文件避免冲突；其余按编号）。

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
    <exclusions>
        <!-- 客户端 GrpcRpcClient 直接 import 了 shaded NettyChannelBuilder(GrpcRpcClient.java:6),
             必须保留 grpc-netty-shaded; starter 默认带的 grpc-netty(非 shaded)与之并存会导致
             两套 Netty 传输 provider 竞争, 故排除 -->
        <exclusion>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-netty</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

（保留 `grpc-netty-shaded`/`grpc-protobuf`/`grpc-stub`——客户端自建 Channel 仍需。）

2. 根 `pom.xml` 的 gRPC 版本对齐（2026-07-08 外部审查 P1-2 已核实，必做）：
   - 现状：根 pom `:316-322` 自行 import `io.grpc:grpc-bom:${grpc.version}`（`grpc.version=1.68.1`，属性 `:163`），子 POM 的 dependencyManagement 优先级高于父 Boot BOM，会把 gRPC 钉死在 1.68.1；而 Boot 4.1 BOM 托管的是 grpc-bom **1.80.0** + spring-grpc 1.1.0，starter 按 1.80.0 编译——混用两个年代的版本，依赖树可能"看着绿"但运行时出怪问题。
   - 改法：**删除根 pom 的 `grpc-bom` import 条目**（让 Boot BOM 的 grpc-bom 1.80.0 接管全部 io.grpc 坐标）；`grpc.version` 属性**改为 1.80.0 保留**（`nebula-rpc-grpc/pom.xml:122` 的 protobuf 插件坐标 `protoc-gen-grpc-java:${grpc.version}` 仍引用它，须与运行时版本一致；若该插件段实为注释状态则属性也可直接删，现场确认）。
   - 同步：检索 `spring-grpc-core` 是否还有其他模块引用（`rg "spring-grpc" --type xml -g '!target'`），无引用则删除 dependencyManagement 条目 `:326-330` 与 `spring-grpc.version` 属性。
   - 验收：`mvn dependency:tree -pl infrastructure/rpc/nebula-rpc-grpc | grep -E "io.grpc|netty"` 确认只剩**一套一致的 gRPC 版本（1.80.0）**、传输层只有 `grpc-netty-shaded` 无 `grpc-netty`。

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
- 测试依赖（2026-07-08 已本地核实，直接照用）：`nebula-rpc-grpc/pom.xml` 加

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-grpc-server-test</artifactId>
    <scope>test</scope>
</dependency>
```

（Boot 4.1 BOM 托管，4.1.0 可拉取；注意**不是** `org.springframework.grpc:spring-grpc-test`——该坐标 1.1.0 拉取失败。）端口注入注解为 `org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort`（已解包 `spring-boot-grpc-test-4.1.0.jar` 确认该类存在）。若集成测试中该注解仍不可用，退路：`ServerSocket(0)` 预取空闲端口配置给 `spring.grpc.server.port`。
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

4. **新增测试** `infrastructure/rpc/nebula-rpc-http/src/test/java/io/nebula/rpc/http/server/HttpRpcAuthTokenRoundTripTest.java`（同包已有 `HttpRpcControllerAuthTest` 可参考其测法）：server 配 token + client 配相同 token 调通；client 无 token 被拒。
   - **断言口径注意**（2026-07-08 外部审查 P3-2）：`HttpRpcClient.sendRequest`（:312-332）用 try-catch 包住整个请求，`RestClient.retrieve()` 遇 401 抛的 `RestClientResponseException` 会被吃掉并封装为 `RpcResponse.exception(...)`——所以 client 侧**拿不到裸 401**。两层分开断言：
     - controller 层（不经 client）：直接构造带/不带头的请求调 `handleRpcRequest`（或 MockMvc），断言 401 状态；
     - client 侧（走 `HttpRpcClient`）：断言返回的 `RpcResponse` 为失败（`success=false`/异常信息含 401），不要断言抛出原始 HTTP 异常。

**验证命令**：`mvn test -pl infrastructure/rpc/nebula-rpc-http`

---

### Task 8 [M-1] /rpc token 常量时间比较

**现场**：`HttpRpcController.java:52-60`，现为 `!authToken.equals(provided)`。

**改法**（2026-07-08 统一口径：与 Task 6 同模块但**仍各自独立提交**，遵守"一任务一提交"，撤销此前"可合并提交"的说法）：

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
        // ValidationException 无单字符串构造函数(已核实 ValidationException.java:25-41),
        // 用 (field, message, value) 三参构造或静态工厂 of(field, message, value)
        throw new io.nebula.core.common.exception.ValidationException(
                "field", "非法列名(仅允许字母/数字/下划线)", field);
    }
    return field;
}
```

三个方法接入：`eq(validateColumn(field), value)`；`findByFields` 改 `fieldValues.forEach((f, v) -> wrapper.eq(validateColumn(f), v))`。
`ValidationException` 位于 `io.nebula.core.common.exception`，可用构造：`(List<FieldError>)`、`(String field, String message, Object value)`、静态 `of(field, message)` / `of(field, message, value)`——**没有**单字符串构造（2026-07-08 已核实，勿凭直觉写）。

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

**新增测试** `autoconfigure/.../search/ElasticsearchSslGuardTest.java`，两层都要测（只测 profile 判定等于只看门牌不试门锁）：

1. **判定逻辑三态**：无 profile→非生产；`dev`→非生产；`prod`→生产（判定逻辑抽为 package-private 静态方法便于直测）。
2. **行为验证**（2026-07-08 外部审查 P2-6 补强）：`prod` profile + `sslVerificationEnabled=false` 时，断言产出的 `SSLContext` **不是** trust-all 的那条路径——推荐做法：把"构建 trust-all SSLContext"的分支也抽为独立 package-private 方法，行为测试断言 prod 下该方法不被调用（或对比返回的 SSLContext 与 `SSLContext.getDefault()` 语义一致/`getSocketFactory` 走默认校验）；同时可用 logback 捕获断言 error 日志出现。具体断言手法执行时按抽取后的方法边界定，原则不变：**必须证明生产环境下拿不到 trust-all 上下文**，不能只证明"判定出了生产环境"。

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

3. `infrastructure/rpc/nebula-rpc-core/.../discovery/ServiceDiscoveryRpcClient.java:92`：`callAsync` 的 `CompletableFuture.supplyAsync(() -> call(...))` 未指定 executor（吃公共 ForkJoinPool）。构造函数新增 `Executor asyncExecutor` 参数（同 Task 6 手法：保留旧构造函数委托新构造传 `ForkJoinPool.commonPool()` 保持兼容），`supplyAsync(..., asyncExecutor)`。装配点在 `autoconfigure/.../rpc/RpcDiscoveryAutoConfiguration.java:94-100`（`serviceDiscoveryRpcClient` Bean）。**注入必须写成可选**：`rpcExecutor` Bean 只在 HTTP RPC 自动配置启用时创建（定义在 `HttpRpcAutoConfiguration`），若给 `serviceDiscoveryRpcClient(...)` 方法加必填 `Executor` 参数，gRPC-only 场景（`nebula.rpc.http.enabled=false`）会因缺 Bean 启动失败。写法：方法参数用 `ObjectProvider<Executor> rpcExecutor`，构造时 `rpcExecutor.getIfAvailable(ForkJoinPool::commonPool)`（如上下文有多个 Executor Bean 导致歧义，改用 `@Qualifier("rpcExecutor")` + `ObjectProvider`，现场按实际 Bean 名确认）。

**验证命令**：`mvn -q compile && mvn test -pl core/nebula-security,infrastructure/rpc/nebula-rpc-core`

---

### Task 13 全量回归 + 文档收尾

按 tasks.md Task 13 列表执行：T-B2-2 勾选、审查报告逐条标"已修复（commit hash）"、tasks.md 变更摘要回填、spec 状态改 done、`CLAUDE.md` v2.0.x 变更记录补充本批（重点：新增配置项 `nebula.rpc.http.client.auth-token`、`spring.http.converters.preferred-json-mapper` defaults、EPP 新键迁移、gateway 死代码删除）。

**追加（2026-07-08 外部审查 P3-1）**：`README.md:4,11` 与 `AGENTS.md:12` 仍写 Spring Boot 3.5.8，根 pom 已是 4.1.0——本任务一并改为 4.1.0（徽章与技术栈两处），避免后续编码代理被旧口径带偏。若执行阶段一途中就发现被误导，可提前单独提交这一处。

**验证命令**：`mvn clean compile && mvn test`（全仓，结果记 log.md）

---

## 2. 阶段二：EPIC-C2 治理（Task 2-0 ~ 2-8，代码级说明）

> 本节行号已于 2026-07-08 在阶段一收尾提交 `c6ce845f` 上核实。任务定义见 `tasks.md` 阶段二；Task 2-0 为阶段一对账审计新增的欠账清理任务。

### Task 2-0 阶段一欠账清理（开工第一动作，两项，各自独立提交）

阶段一对账审计（2026-07-08）发现两处未完成项：

1. **Task 13 文档项漏做**：`README.md:4,11` 与 `AGENTS.md:12` 仍写 Spring Boot 3.5.8（应为 4.1.0）；`CLAUDE.md` v2.0.x 变更记录未补充阶段一内容（`rg "auth-token|preferred-json-mapper" CLAUDE.md` 零命中）。补法：README 徽章与技术栈两处、AGENTS 技术栈一处改 4.1.0；CLAUDE.md 变更记录补充——新配置项 `nebula.rpc.http.client.auth-token`、starter defaults 注入 `spring.http.converters.preferred-json-mapper=jackson2`、EPP 迁移新注册键、gateway 死代码删除、ServiceImpl 列名校验、ES trust-all 生产防护。
2. **Task 12.3 装配缺口**：`ServiceDiscoveryRpcClient` 已加 5 参构造（`asyncExecutor`），但装配点 `RpcDiscoveryAutoConfiguration.java:100-101` 仍调 4 参构造——HTTP RPC 场景下 `rpcExecutor` Bean 存在也不会被用上，`callAsync` 恒走 `ForkJoinPool.commonPool()`，且该偏差未记 log.md。补法：`serviceDiscoveryRpcClient(...)` 方法加参数 `ObjectProvider<Executor> rpcExecutor`（不能用必填 `Executor`——该 Bean 只在 HTTP RPC 启用时创建，必填会让 gRPC-only 场景启动失败），构造改传 `rpcExecutor.getIfAvailable(ForkJoinPool::commonPool)`；若上下文存在多个 Executor Bean 产生歧义，配合 `@Qualifier("rpcExecutor")`。补一条 `ApplicationContextRunner` 测试：有 rpcExecutor Bean 时注入它、无时回落 commonPool（可通过反射读 `asyncExecutor` 字段断言）。

**验证命令**：`mvn test -pl autoconfigure/nebula-autoconfigure` + 人工核对三份文档改动；两项分别提交（`docs: ...` 与 `fix(rpc): ...`）。

---

### Task 2-1 死配置全量盘点（只盘点不改码，产出 HARD-GATE 裁决表）

产出盘点表填入 `log.md` 既有表格，**经用户确认裁决后才可执行 Task 2-2/2-3**。已核实的起点事实（省调研，但仍须全量扫描补漏）：

- `HttpRpcProperties.ClientConfig`（`:109-198`）：
  - **已接通**：`connectTimeout`(:126)/`readTimeout`(:134) → `HttpRpcAutoConfiguration.rpcRestClient`(:51-53, `SimpleClientHttpRequestFactory`)；`authToken`(:198) → 阶段一 Task 6；`baseUrl`(:118)、`enabled`(:113)。
  - **无消费点**：`writeTimeout`(:142)/`maxConnections`(:150)/`maxConnectionsPerRoute`(:158)/`keepAliveTime`(:166)/`retryCount`(:174)/`retryInterval`(:182)/`compressionEnabled`(:187)；`loggingEnabled`(:192) 消费点待查。
- `GrpcRpcProperties.ServerConfig`：`maxInboundMessageSize`(:52)/`keepAliveTime`(:58)/`keepAliveTimeout`(:64)/`permitKeepAliveWithoutCalls`(:70)/`maxConcurrentCalls`(:76) 已在阶段一 Task 4 标 Javadoc 预告废弃；诊断展示在 `GrpcRpcAutoConfiguration.java:85-86`。`port`(:45-46, @Deprecated 桥接生效) 与 `authToken`(:83) **保留不裁**。
- `SecurityProperties.anonymousUrls`（`:38`，`rg "anonymousUrls" --type java` 当前仅定义处命中，无消费点）。
- `NacosProperties.heartbeatInterval`(:93)/`heartbeatTimeout`(:101)。注意：tasks.md 提到的"`NacosDiscoveryAutoConfiguration.java:94` 诊断展示"已失实——该类现无 heartbeat 输出（rg 零命中），以现场为准。
- 全量扫描：`rg -l "Properties" --type java -g '*Properties.java'` 列出全部配置类，逐类核对每个字段的消费点。

**随盘点表一并提请用户拍板的设计决策**（Task 2-4 需要）：`Result.of(ResultCode)` 工厂产出的 code 用枚举名 `name()`（"SUCCESS"，与 Result 现有符号码风格一致，**推荐**）还是数字码 `getCode()`（"0000"，与 ResultCode 定义一致）。

---

### Task 2-2 HTTP RPC 客户端参数接通（按 2-1 裁决执行）

**现场**：`HttpRpcAutoConfiguration.rpcRestClient`(:46-61) 现用 `SimpleClientHttpRequestFactory`，只接了 connect/read 超时；诊断展示 :170-176。

**改法**（按裁决表二选一，推荐 A）：

- **方案 A（保留连接池参数）**：改 `HttpComponentsClientHttpRequestFactory` + `PoolingHttpClientConnectionManager`——`maxConnections` → `setMaxTotal`、`maxConnectionsPerRoute` → `setDefaultMaxPerRoute`、`keepAliveTime` → 连接存活策略。需在 `nebula-rpc-http`（或 autoconfigure）加依赖 `org.apache.httpcomponents.client5:httpclient5`（Boot BOM 托管无需版本号）。
- **方案 B（裁决删除连接池参数）**：改 `JdkClientHttpRequestFactory`（JDK HttpClient 无连接池调节参数），随 Task 2-3 删除 `maxConnections`/`maxConnectionsPerRoute`/`keepAliveTime` 字段。
- 两方案共同项：`writeTimeout`/`compressionEnabled`/`retryCount`/`retryInterval` 预期裁决为删除（RestClient 无对应语义，重试属上层职责）；诊断展示只保留真实生效项。

**新增测试** `autoconfigure/.../rpc/HttpRpcClientConfigTest.java`：`ApplicationContextRunner` 装配 `HttpRpcAutoConfiguration`，断言 `rpcRestClient` Bean 存在且请求工厂类型/超时值与属性一致（反射或 `RestClient.mutate()` 探查；不可行则以工厂 Bean 单测替代，记 log.md）。

**验证命令**：`mvn test -pl autoconfigure/nebula-autoconfigure,infrastructure/rpc/nebula-rpc-http`

---

### Task 2-3 死配置删除批（按 2-1 裁决执行）

**已核实改点**（裁决通过后执行）：

- `core/nebula-security/.../config/SecurityProperties.java:38` 删 `anonymousUrls`（核心模块破坏性变更：CLAUDE.md 变更记录注明替代项 `nebula.web.auth.ignore-paths`）。
- `infrastructure/discovery/nebula-discovery-nacos/.../config/NacosProperties.java:93,101` 删 `heartbeatInterval`/`heartbeatTimeout`。
- `infrastructure/rpc/nebula-rpc-grpc/.../config/GrpcRpcProperties.java:52-76` 删 ServerConfig 五个遗留参数（Task 4 已 Javadoc 预告）；**同步删** `GrpcRpcAutoConfiguration.java:85-86` 的 Max Concurrent/KeepAlive 诊断展示，否则编译不过。
- 其余按裁决表逐项，每删一项 `rg` 确认零残留引用。

**验收**：`rg "anonymousUrls|heartbeatInterval|heartbeatTimeout" --type java` 主代码零命中；CLAUDE.md v2.0.x 逐项标注破坏性变更。
**验证命令**：`mvn clean compile && mvn test -pl core/nebula-security,infrastructure/discovery/nebula-discovery-nacos,infrastructure/rpc/nebula-rpc-grpc`

---

### Task 2-4 错误码收敛（ResultCode 为唯一事实源）

**重要事实修正**（2026-07-08 核实，与审查报告 CF-40 表述不符）：`ResultCode.getByCode()`(:100-102) 委托 `EnumBase.EnumUtils.getByCode`(:44-55，遍历比较 `code.equals(getCode())`)，**当前实现对数字码（"0000"等）回查是正确的**。CF-40 说"永远查不到"的实际语义是**跨体系互查不通**：`Result` 用符号码（`Result.java:62` `"SUCCESS"`）、`ResultCode` 用数字码（`"0000"`）、`Constants.ResponseCode`(:174-206) 又是一套数字码常量——三套互不相认。执行时先写全枚举回查测试验证上述结论，成立则把 CF-40 的修复对象改判为"跨体系收敛"，记 log.md 偏差。

**改法**：

1. `Result.java` 新增工厂（现有方法与码值**零改动**，下游可能字符串匹配 `"SUCCESS"`）：

```java
public static <T> Result<T> of(ResultCode resultCode) { return of(resultCode, null); }
public static <T> Result<T> of(ResultCode resultCode, T data) {
    // code 取枚举名(与 Result 既有符号码风格一致), 具体取 name() 还是 getCode() 以 Task 2-1 用户拍板为准
    ...
}
```

2. `ResultCode` 补 `getByName(String)`（`valueOf` + 非法值返回 null 包装）。
3. `Constants.java:174-206` 的 `ResponseCode` 数字码常量标 `@Deprecated`，Javadoc 指向 `ResultCode`。
4. **新增测试** `core/nebula-foundation/src/test/java/.../ResultCodeTest.java`：全枚举 `getByCode(getCode())` 回查一致；`Result.of(SUCCESS)` 的 code/message 断言；`Result.success().getCode()` 等既有值回归断言。

**验证命令**：`mvn test -pl core/nebula-foundation`

---

### Task 2-5 认证收敛（本批最高风险，动手前先通读六个文件）

**已核实的两条平行链路**（问题本质：同一请求 JWT 可能被解析两次，且两层各用一套 secret 配置）：

| | web 链路 | security 链路 |
|---|---|---|
| 开关 | `nebula.web.auth.enabled=true` | `nebula.security.jwt.filter.enabled=true`（opt-in，`SecurityAutoConfiguration.java:70-77`） |
| 装配 | `WebAuthAutoConfiguration`（nebula-web 模块 :27-57）：`JwtUtils`(@Deprecated) → `DefaultAuthService` → `AuthInterceptor`（order=`InterceptorOrders.AUTH`，拦 `/**`） | autoconfigure 的 `SecurityAutoConfiguration`：`JwtAuthenticationFilter`（`OncePerRequestFilter`） |
| 密钥 | `nebula.web.auth.jwt-secret` | `nebula.security.jwt.secret` |
| 上下文 | `AuthContext`（ThreadLocal） | `SecurityContext` |

**改法**（方向已在 tasks.md 确认：Filter 唯一解析、Interceptor 消费）：

1. `AuthInterceptor.preHandle`(:41-82)：删除 `extractToken` + `authService.getUser(token)` 自行解析路径，改为读取 `SecurityContext` 的当前认证信息（方法名以 `core/nebula-security/.../authentication/SecurityContext.java` 实际 API 为准，动手前先读）——未认证→维持现有 401 响应格式；已认证→由 security 层用户信息构造 `AuthUser` 填充 `AuthContext`，并保留 `request.setAttribute("currentUser", user)`(:80) 兼容行为。CORS 预检放行(:51-53)与忽略路径(:57-60)逻辑不动。
2. **注册联动**：`nebula.web.auth.enabled=true` 时必须保证 `JwtAuthenticationFilter` 已注册（否则 Interceptor 读不到认证信息全部 401）。`@ConditionalOnProperty` 不支持 OR——用 `AnyNestedCondition` 实现"`nebula.security.jwt.filter.enabled=true` 或 `nebula.web.auth.enabled=true` 任一即注册"，原 opt-in 键保留为独立覆盖手段。
3. **遗留 Bean 去留**：`JwtUtils`/`DefaultAuthService`/`AuthService` Bean 定义保留（API 兼容硬约束，删除留给下个大版本），但 `AuthInterceptor` 构造不再依赖 `AuthService`——改构造签名前先 `rg "new AuthInterceptor"` 确认全部装配点。
4. **密钥归一说明**：收敛后生效密钥为 `nebula.security.jwt.secret`；`nebula.web.auth.jwt-secret` 在 CLAUDE.md 标注废弃并给迁移说明。
5. **新增集成测试** `application/nebula-web/src/test/java/.../AuthConvergenceIntegrationTest.java`：MockMvc + `@MockitoSpyBean JwtService`——(a) 同一请求 JWT 解析方法仅调用一次；(b) `AuthContext.getCurrentUser()` 与 `SecurityContext` 用户一致；(c) 无 token 访问受保护路径 401；(d) 白名单路径放行。

**验证命令**：`mvn test -pl application/nebula-web,core/nebula-security,autoconfigure/nebula-autoconfigure`

---

### Task 2-6 xxl-job 孤儿 DTO 清理

已核实：`application/nebula-task/.../xxljob/dto/` 下四个文件齐在；`XxlJobResult` 被 `XxlJobRegistryService.java:169,205` 使用**保留**，其余三个（`XxlJobExecuteRequest`/`XxlJobLogRequest`/`XxlJobLogResult`）删除前 `rg` 逐一确认零引用后删。

**验证命令**：`mvn test -pl application/nebula-task` + `rg "XxlJobExecuteRequest|XxlJobLogRequest|XxlJobLogResult"` 零命中

---

### Task 2-7 自动装配三态条件测试（ApplicationContextRunner）

**清单修订**（对照 tasks.md 的 8 项，已核实两处变化）：

- `env/StarterDefaultsInjectionTest` **从清单移除**——阶段一 Task 1 已在三个 starter 模块内落地同名测试，勿重复；记 log.md。
- `security/SecurityAutoConfigurationConditionTest`：autoconfigure 测试目录已有 `SecurityAutoConfigurationTest` 覆盖 filter 的 opt-in 条件——**扩展该类**补 enabled=false/缺类两态，不新建重名近似类。
- 其余 6 类新建：web/（目录需新建）`WebAutoConfigurationConditionTest`、data/`CacheAutoConfigurationConditionTest`、messaging/`RabbitMQAutoConfigurationConditionTest`、rpc/`HttpRpcAutoConfigurationConditionTest`、rpc/`GrpcRpcAutoConfigurationConditionTest`、search/`ElasticsearchAutoConfigurationConditionTest`（只测条件评估，不连真实 ES）。

每类三态：`enabled=true` 有 Bean / `enabled=false` 无 Bean / `FilteredClassLoader` 模拟缺类无 Bean。模式：

```java
new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(XxxAutoConfiguration.class))
    .withPropertyValues("nebula.xxx.enabled=true")
    .run(ctx -> assertThat(ctx).hasSingleBean(XxxService.class));
```

**依赖**：Task 2-2/2-3/2-5 之后执行（配置面定型再锁测试）。
**验证命令**：`mvn test -pl autoconfigure/nebula-autoconfigure`

---

### Task 2-8 文档同步 + 阶段二收尾

- `CLAUDE.md`/`AGENTS.md`"关键实现引用"逐条核对行号（对每条引用 rg 类名/方法名取实际行号），过期即更新；"模块核对提示词"中失实描述（net.devh gRPC、已删配置项等）同步修正。
- `.cursor/rules/project.mdc`：被删配置项、鉴权收敛说明（第 6 章双层鉴权架构需反映 Filter 唯一解析）同步。
- 阶段二全量回归 + tasks.md 勾选 + spec/log 回填。

**验证命令**：`mvn clean compile && mvn test`（全仓）+ 抽查 20 条行号引用全部命中

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

- Task 3-0 盘点先行，**必须全仓扫描生成清单，不得只按预列条目**：`rg -n "jackson|jjwt-jackson" -g 'pom.xml'` 逐文件登记（2026-07-08 外部审查 P2-1 复核：gateway/storage/search/websocket/crawler/rpc-async/messaging/foundation/cache/mongodb/payment/task 等 20+ 个 pom 均有显式 `jackson-databind`/`jackson-datatype-jsr310`/`jjwt-jackson` 声明，远多于最初预列的 5 处）。每项定策略：换 `tools.jackson` 坐标 / 删除（jsr310 并入 databind）/ 保留（第三方桥接需要）。jjwt 的 Jackson 3 支持情况上网核实（JJWT 0.13+ release notes），无则按 tasks.md 预判二选一。
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
        (T9 必须先于 T4; T11 先于 T12; T6/T8 同模块但仍各自独立提交)
阶段一完成后 ──→ Task 4-0(版本号) ──→ Task 4-1(proud-day, 可与阶段二并行)
阶段二: T2-0(欠账清理) → T2-1(盘点,HARD-GATE 用户确认) → T2-2/2-3/2-4/2-6(可并行) → T2-5 → T2-7 → T2-8
阶段三: T3-0(盘点) → T3-1 → T3-2 → T3-3 → T3-4 → T3-5 → T3-6(拆桥)
```

每阶段收尾：全仓 `mvn clean compile && mvn test` + `log.md` 回填 + tasks.md 勾选。

---

## 附录 A：交接编码代理的启动提示词（逐阶段使用）

> 使用方式：交接时把下面提示词原样发给编码代理，只替换【】内的阶段范围。**一次只交付一个阶段**，上一阶段验收对账通过后再启动下一阶段。阶段二开工前，先按当时代码现状把阶段二的实现细节补写到本文档同等粒度（该补写动作本身是阶段一收尾后的独立任务）。

```text
你在 nebula 仓库的 nebula-remaining-work 分支上执行编码任务。

开工前按顺序通读四份文档：
1. docs/changes/nebula-remaining-work/spec.md          —— 为什么改（背景与决策，禁止推翻）
2. docs/changes/nebula-remaining-work/tasks.md         —— 任务清单（完成后逐项勾选）
3. docs/changes/nebula-remaining-work/implementation.md —— 怎么改（执行以此为准）
4. docs/changes/nebula-remaining-work/log.md           —— 已有技术决策与踩坑（禁止推翻，只许追加）

本次只执行 implementation.md 的【阶段一 Task 1 到 Task 13】，严格遵守其
"0. 执行纪律"一节的全部条款。特别强调：

- 每个任务的验证命令必须真实执行，把输出关键行（测试数/BUILD SUCCESS 等）
  记入 log.md 对应条目；不许凭"应该能过"宣称通过。
- 测试失败时修代码不修测试；禁止注释/删除/弱化测试断言或加 @Disabled。
- 同一任务验证连续失败 2 次，停下来：把命令、报错关键行、已尝试的修法记入
  log.md 踩坑记录，向我汇报并等待指示，不许自行绕道或跳过。
- 文档行号基于 commit 1f49a363，动手前用 rg 按类名/方法名现场核实，
  行号漂移属正常；现场与文档结构性不符时按纪律"偏差处理"条执行。
- 每完成一个任务：勾选 tasks.md、按纪律规定格式单独 git commit，
  再开始下一个任务。除方案文档提交外，不产生任务之外的提交。
- 全部任务完成后输出总结：每任务的提交 hash、验证结果、log.md 新增条目索引。
```

### 交付后的验收对账清单（用户侧使用）

- [ ] `git log --oneline`：提交序列与阶段任务一一对应（一任务一提交，信息格式符合纪律第 2 条）
- [ ] `tasks.md`：本阶段任务全部勾选，勾选项附有变更摘要
- [ ] `log.md`：每个任务有真实验证输出记录（可抽查 1-2 条重跑核实）；踩坑/偏差如实登记
- [ ] 全仓 `mvn clean compile && mvn test` 本地重跑通过
- [ ] 抽查高风险任务的实际代码（本阶段清单见各任务"验证命令"，阶段一重点：Task 3 依赖树、Task 5 集成测试、Task 7 列名校验）
