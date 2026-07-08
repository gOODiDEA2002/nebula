# 任务拆分 -- 审查问题修复（nebula-review-fixes）

> 来源 Spec：`docs/changes/nebula-review-fixes/spec.md`
> 拆分顺序：CRITICAL -> HIGH -> MEDIUM -> LOW -> 回归收尾
> 每个任务 = 可独立提交的原子变更；完成即勾选并 git commit

## 前置条件

- [ ] Spec 已确认（HARD-GATE 通过）
- [x] 当前在 `nebula-remaining-work` 分支，工作区干净（2026-07-08 更正分支口径）
- [ ] 全仓可编译（基线：`mvn clean compile` 已通过）

---

## Task 1: [C-1] Starter 默认锁定 MVC 走 Jackson 2

- **目标**: 含 nebula-web 的 starter 自动注入 `spring.http.converters.preferred-json-mapper=jackson2`，恢复 `@SensitiveData` 脱敏与日期定制在 Controller 响应上的效力
- **涉及文件**:
  - `starter/nebula-starter-web/src/main/resources/META-INF/nebula-defaults.properties` -- 追加 `spring.http.converters.preferred-json-mapper=jackson2`
  - `starter/nebula-starter-all/src/main/resources/META-INF/nebula-defaults.properties` -- 同上
  - `starter/nebula-starter-mcp/src/main/resources/META-INF/nebula-defaults.properties` -- 同上
  - `application/nebula-web/src/main/java/io/nebula/web/config/JacksonConfig.java` -- 类 Javadoc 补充说明：SB4.1 下需 preferred-json-mapper=jackson2 方能作用于 MVC（starter 已注入，裸依赖 nebula-web 需自行配置）
- **依赖**: 无
- **验收标准**: 引入 starter-web 的应用中 `Environment.getProperty("spring.http.converters.preferred-json-mapper")` 为 `jackson2`；用户 application.yml 显式配置可覆盖
- **验证方式**: 拆两半验证（2026-07-08 修正：autoconfigure 测试模块不能反向依赖 starter，否则 Maven 循环依赖）——机制半：既有 `NebulaStarterDefaultsIntegrationTest`（autoconfigure）证明 EPP 注入与覆盖语义；内容半：在 web/all/mcp 三个 starter 模块各新增最小 `StarterDefaultsInjectionTest`，断言 `preferred-json-mapper=jackson2` 被注入（starter 自己的 defaults 文件天然在其测试 classpath 上）。详见 `nebula-remaining-work/implementation.md` Task 1
- **验证命令**: `mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=NebulaStarterDefaultsIntegrationTest && mvn test -pl starter/nebula-starter-web,starter/nebula-starter-all,starter/nebula-starter-mcp`
- [x] 完成

## Task 2: [C-1] MockMvc 脱敏回归哨兵测试

- **目标**: 在 MVC 转换器路径（而非 mapper 本体）断言脱敏生效，防止再次静默回归
- **涉及文件**:
  - `application/nebula-web/src/test/java/io/nebula/web/autoconfigure/SensitiveDataMvcMaskingTest.java` -- 新增：`@SpringBootTest` + MockMvc，注册一个返回含 `@SensitiveData` 字段 DTO 的测试 Controller，测试属性显式带 `spring.http.converters.preferred-json-mapper=jackson2`，断言响应体为脱敏值（如手机号 `138****5678`）
- **关键签名**:
  ```java
  @Test
  void sensitiveFieldMaskedInMvcResponse() throws Exception {
      mockMvc.perform(get("/test/masking"))
             .andExpect(jsonPath("$.data.phone").value("138****5678"));
  }
  ```
- **依赖**: Task 1
- **说明**: 测试属性显式带 `preferred-json-mapper=jackson2` 是刻意设计——nebula-web 模块测试的 classpath 上没有 starter jar 的 defaults 文件，无法验证注入；"defaults 注入生效"由 Task 1 的 `NebulaStarterDefaultsIntegrationTest` 覆盖，本测试只负责"该属性下脱敏在 MVC 路径生效"，两者合起来才是完整链路
- **验收标准**: 测试通过；临时移除 preferred-json-mapper 属性时该测试失败（人工验证一次测试有效性，结果记入 log.md）
- **验证命令**: `mvn test -pl application/nebula-web -Dtest=SensitiveDataMvcMaskingTest`
- [x] 完成

## Task 3: [C-2] nebula-rpc-grpc 接入 Boot 官方 gRPC Server Starter

- **目标**: gRPC 服务端由 Spring Boot 4.1 官方自动配置托管启动，`BindableService` 与 `@GlobalServerInterceptor` 自动注册
- **涉及文件**:
  - `infrastructure/rpc/nebula-rpc-grpc/pom.xml` -- `spring-grpc-core` 替换为 `org.springframework.boot:spring-boot-starter-grpc-server`（BOM 托管，无需版本号），并从中排除默认 `grpc-netty`（保留 `grpc-netty-shaded`——客户端直接 import 了 shaded NettyChannelBuilder，两套传输并存会冲突；2026-07-08 外部审查 P1-2 补充）
  - `pom.xml`（根） -- **删除自管的 `grpc-bom:1.68.1` import**（Boot 4.1 BOM 托管 grpc 1.80.0，子 POM 自管条目会钉死旧版造成混装；`grpc.version` 属性改 1.80.0 保留给 protobuf 插件坐标）；dependencyManagement 中 `spring-grpc-core` 条目移除（若无其他模块引用）；`<spring-grpc.version>` 属性视引用情况清理
  - 验收补充：`mvn dependency:tree -pl infrastructure/rpc/nebula-rpc-grpc` 只剩一套 gRPC 版本（1.80.0）、传输层无 `grpc-netty`
  - `infrastructure/rpc/nebula-rpc-grpc/src/main/java/io/nebula/rpc/grpc/server/GrpcRpcServer.java` -- 类 Javadoc 更新：说明由 Boot gRPC 自动配置托管为 BindableService
- **依赖**: 无
- **验收标准**: 编译通过；`mvn dependency:tree -pl infrastructure/rpc/nebula-rpc-grpc` 中出现 `spring-boot-grpc-server` 且无 Netty/grpc 版本冲突（结果记入 log.md）
- **验证命令**: `mvn -q compile -pl infrastructure/rpc/nebula-rpc-grpc -am && mvn dependency:tree -pl infrastructure/rpc/nebula-rpc-grpc`
- [x] 完成

## Task 4: [C-2] gRPC 端口桥接 EPP + 旧端口配置废弃标注

- **目标**: 兼容存量 `nebula.rpc.grpc.server.port` 配置，统一收敛到 `spring.grpc.server.port`
- **涉及文件**:
  - `autoconfigure/nebula-autoconfigure/src/main/java/io/nebula/autoconfigure/env/NebulaGrpcServerPortBridgePostProcessor.java` -- 新增：读取 `nebula.rpc.grpc.server.port`，仅当 `spring.grpc.server.port` 未配置时以 MapPropertySource 注入
  - `autoconfigure/nebula-autoconfigure/src/main/resources/META-INF/spring.factories` -- 按 SB4 新注册键 `org.springframework.boot.EnvironmentPostProcessor` 注册该 EPP（Task 9 先行完成键迁移，本任务直接写新键，不再产生旧键增量）
  - `infrastructure/rpc/nebula-rpc-grpc/src/main/java/io/nebula/rpc/grpc/config/GrpcRpcProperties.java` -- `ServerConfig.port` 标注 `@Deprecated` + Javadoc 指向 `spring.grpc.server.port`；线程/流控参数 Javadoc 注明改用 `spring.grpc.server.*`
- **关键签名**:
  ```java
  public class NebulaGrpcServerPortBridgePostProcessor implements EnvironmentPostProcessor {
      public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) { }
  }
  ```
- **依赖**: Task 3、**Task 9（硬依赖，先做键迁移再建本 EPP，避免刚写旧钥匙又换锁；2026-07-07 外部审查意见 P3-1）**
- **验收标准**: 仅配 `nebula.rpc.grpc.server.port=9527` 时 gRPC 服务端监听 9527；两者都配时 `spring.grpc.server.port` 优先
- **验证命令**: `mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=NebulaGrpcServerPortBridge*`
- [x] 完成

## Task 5: [C-2] gRPC 服务端回环集成测试（含 token 拦截器）

- **目标**: 用真实 Server 验证"服务端启动 + 服务注册 + 全局拦截器生效"，覆盖本次 CRITICAL 的静默失效模式
- **涉及文件**:
  - `infrastructure/rpc/nebula-rpc-grpc/src/test/java/io/nebula/rpc/grpc/GrpcServerLoopbackIntegrationTest.java` -- 新增：`@SpringBootTest(properties = "spring.grpc.server.port=0")` + `@LocalGrpcServerPort`，`GrpcRpcClient` 指向本地端口回环调用 `GrpcRpcServer`；第二组用例配置 `nebula.rpc.grpc.server.auth-token`，断言无 token 被拒（UNAUTHENTICATED）、带 token 通过
- **依赖**: Task 3, Task 4
- **验收标准**: 两组用例均绿；`tasks.md`（sb4-upgrade）中 T-B2-2 可标记完成
- **验证命令**: `mvn test -pl infrastructure/rpc/nebula-rpc-grpc -Dtest=GrpcServerLoopbackIntegrationTest`
- [x] 完成

## Task 6: [H-1] HTTP RPC 客户端注入鉴权 token

- **目标**: 开启服务端 token 鉴权后，框架自身 HTTP RPC 调用可通过认证
- **涉及文件**:
  - `infrastructure/rpc/nebula-rpc-http/src/main/java/io/nebula/rpc/http/config/HttpRpcProperties.java` -- `ClientConfig` 新增 `private String authToken = "";`
  - `infrastructure/rpc/nebula-rpc-http/src/main/java/io/nebula/rpc/http/client/HttpRpcClient.java` -- 构造函数增加 authToken 参数；发请求处（现 :324 附近）非空时设置请求头 `X-Nebula-Rpc-Token`
  - `autoconfigure/nebula-autoconfigure/src/main/java/io/nebula/autoconfigure/rpc/HttpRpcAutoConfiguration.java` -- `httpRpcClient(...)`（:92-102）传入 `properties.getClient().getAuthToken()`
  - `infrastructure/rpc/nebula-rpc-http/src/test/java/.../HttpRpcAuthTokenTest.java` -- 新增/扩展：server 开鉴权 + client 配 token 调通；client 无 token 得 401
- **关键签名**:
  ```java
  public HttpRpcClient(RestClient restClient, String baseUrl, Executor executor,
                       ObjectMapper objectMapper, String authToken) { }
  ```
- **依赖**: 无
- **验收标准**: 见测试用例；构造函数变更为编译期破坏性变更，全仓检索调用点同步更新
- **验证命令**: `mvn test -pl infrastructure/rpc/nebula-rpc-http`
- [x] 完成

## Task 7: [H-2] ServiceImpl 列名白名单校验

- **目标**: 阻断 `findByField` 系列的 SQL 注入面
- **涉及文件**:
  - `infrastructure/data/nebula-data-persistence/src/main/java/io/nebula/data/persistence/service/impl/ServiceImpl.java` -- 新增 `private static String validateColumn(String field)`（正则 `^[A-Za-z0-9_]+$`，非法抛 `ValidationException`）；`findByField`(:43)、`findOneByField`(:48)、`findByFields`(:55) 接入
  - `infrastructure/data/nebula-data-persistence/src/test/java/.../ServiceImplColumnValidationTest.java` -- 新增：合法列名通过、`"name; DROP TABLE x"` / `"name`)"` 等非法输入抛异常
- **关键签名**:
  ```java
  private static String validateColumn(String field) { }
  ```
- **依赖**: 无
- **验收标准**: 非法列名抛 `ValidationException` 且信息明确；合法查询行为不变
- **验证命令**: `mvn test -pl infrastructure/data/nebula-data-persistence`
- [ ] 完成

## Task 8: [M-1] /rpc token 常量时间比较

- **目标**: 消除计时侧信道
- **涉及文件**:
  - `infrastructure/rpc/nebula-rpc-http/src/main/java/io/nebula/rpc/http/server/HttpRpcController.java` -- :54 `authToken.equals(provided)` 改 `MessageDigest.isEqual(authToken.getBytes(UTF_8), provided.getBytes(UTF_8))`（provided 判空前置）
- **依赖**: 无（与 Task 6 同模块，但仍各自独立提交，遵守"一任务一提交"；2026-07-08 统一口径）
- **验收标准**: 既有鉴权测试仍绿
- **验证命令**: `mvn test -pl infrastructure/rpc/nebula-rpc-http`
- [ ] 完成

## Task 9: [M-2] EPP 迁移至 SB4 新接口与新注册键

- **目标**: 摆脱废弃兼容键，避免未来版本移除时复刻 A1 静默事故
- **涉及文件**:
  - `autoconfigure/nebula-autoconfigure/src/main/java/io/nebula/autoconfigure/env/NebulaStarterDefaultsPostProcessor.java` -- import 改 `org.springframework.boot.EnvironmentPostProcessor`
  - `autoconfigure/nebula-autoconfigure/src/main/java/io/nebula/autoconfigure/ai/NebulaMcpEnvironmentPostProcessor.java` -- 同上
  - `autoconfigure/nebula-autoconfigure/src/main/resources/META-INF/spring.factories` -- 注册键改为 `org.springframework.boot.EnvironmentPostProcessor`（只登记新键，不双注册，避免执行两次）；同步更新注释
  - **删除 `starter/nebula-starter-gateway/src/main/java/io/nebula/autoconfigure/gateway/GatewayGrpcServerExcludeConfiguration.java` 及 `starter/nebula-starter-gateway/src/main/resources/META-INF/spring.factories`** -- 该 EPP 排除的 3 个 `net.devh.*` 类在全仓 POM 已无依赖（仅剩注释引用），排除不存在的类是空操作，属死代码；直接删除而非迁移（2026-07-07 外部审查意见 P1-4，核实后加码为删除）
- **依赖**: 无（**必须先于 Task 4 执行**）
- **验收标准**: 既有"starter defaults 注入生效"集成测试仍绿（回归哨兵）；启动日志无 EPP 相关废弃警告；`rg "net.devh" --type java` 主代码零命中
- **验证命令**: `mvn test -pl autoconfigure/nebula-autoconfigure`
- [x] 完成

## Task 10: [M-3] 缓存健康检查键前缀 + 统计原子化

- **目标**: 收敛命名空间纪律，统计并发安全
- **涉及文件**:
  - `infrastructure/data/nebula-data-cache/src/main/java/io/nebula/data/cache/manager/impl/DefaultCacheManager.java` -- :708 `"health:check"` 改 `keyPrefix + "health:check"`；`evictionCount` 改 `java.util.concurrent.atomic.LongAdder`（或 `AtomicLong`），读写点同步调整
- **依赖**: 无
- **验收标准**: 既有缓存测试仍绿；健康检查写入的键带 `nebula:cache:` 前缀
- **验证命令**: `mvn test -pl infrastructure/data/nebula-data-cache`
- [ ] 完成

## Task 11: [M-4] ES trust-all 生产环境防护

- **目标**: 与爬虫模块安全策略对齐：生产环境忽略"跳过证书校验"配置
- **涉及文件**:
  - `autoconfigure/nebula-autoconfigure/src/main/java/io/nebula/autoconfigure/search/ElasticsearchAutoConfiguration.java` -- 注入 `Environment`；`createSSLContext()`(:164) 中 `!sslVerificationEnabled` 分支先判定生产 profile（对齐 `HttpCrawlerEngine.java:423` 判定逻辑：activeProfiles 含 prod/production），生产则 error 日志并按默认校验流程走
  - `autoconfigure/nebula-autoconfigure/src/test/java/.../ElasticsearchSslGuardTest.java` -- 新增：prod profile 下 trust-all 被忽略
- **依赖**: 无
- **验收标准**: 见测试；非生产 profile 行为不变
- **验证命令**: `mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=ElasticsearchSslGuardTest`
- [ ] 完成

## Task 12: [L 批] 三处小修

- **目标**: 清理 LOW 级问题
- **涉及文件**:
  - `core/nebula-security/src/main/java/io/nebula/security/authentication/JwtAuthenticationFilter.java` -- `readStringList` 删除冗余 `.filter(Objects::nonNull)`（若 `Objects` import 仅此处使用则一并清理）。注意：核心模块，仅此一行，无行为变化
  - `autoconfigure/nebula-autoconfigure/src/main/java/io/nebula/autoconfigure/search/ElasticsearchAutoConfiguration.java` -- `elasticsearchClient()`(:143-148) 改 `ObjectMapper mapper = objectMapper.copy();` 后注册模块
  - `infrastructure/rpc/nebula-rpc-core/src/main/java/io/nebula/rpc/core/discovery/ServiceDiscoveryRpcClient.java` -- :92 `supplyAsync` 增加 executor 参数（构造注入，缺省 `ForkJoinPool.commonPool()` 保持兼容）
- **依赖**: Task 11（同文件 ElasticsearchAutoConfiguration，避免冲突，顺序执行）
- **验收标准**: 编译与既有测试通过
- **验证命令**: `mvn -q compile && mvn test -pl core/nebula-security,infrastructure/rpc/nebula-rpc-core`
- [ ] 完成

## Task 13: 全量回归 + 文档收尾

- **目标**: 确认合入条件全部满足，回填记录
- **涉及文件**:
  - `docs/changes/nebula-hardening-sb4-upgrade/tasks.md` -- T-B2-2 勾选完成（依据 Task 5）
  - `docs/nebula-hardening-code-review-2026-07.md` -- 各问题条目标注"已修复（commit hash）"
  - `docs/changes/nebula-review-fixes/tasks.md` -- 填写变更摘要
  - `docs/changes/nebula-review-fixes/spec.md` -- 状态改 done
  - `CLAUDE.md` -- v2.0.x 变更记录补充本批修复（涉及 core 模块改动与配置项新增）
- **依赖**: Task 1-12 全部完成
- **验收标准**: `mvn clean compile` 与 `mvn test` 全仓通过；Spec 第 10 节验收标准逐项勾选
- **验证命令**: `mvn clean compile && mvn test`
- [ ] 完成

---

## 变更摘要

> 全部 Task 完成后填写

- 总文件数:
- Spec-Plan 偏差记录:
- 遗留问题:
