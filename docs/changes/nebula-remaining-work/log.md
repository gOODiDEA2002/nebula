# 变更日志 -- 剩余工作总体（nebula-remaining-work）

> 随开发实时追加；只记有复用价值的技术决策、踩坑与知识发现。

## 技术决策

### D1（设计期）：剩余工作分四阶段推进，阶段一/二/三串行为主

- 阶段一（审查修复）优先：两个 CRITICAL 属安全/可用性问题，是合入主干的前置条件。
- 阶段二（治理）在阶段一后：gRPC 形态定型（Boot 官方 starter）后才能裁决 `GrpcRpcProperties.ServerConfig` 遗留参数的去留；认证收敛依赖 Filter 注册机制稳定。
- 阶段三（Jackson 3）最后：错误码/认证收敛先做完，减少迁移期间的返工面；且依赖阶段一 Task 2 的 MockMvc 哨兵测试作为回归闸门。

### D2（设计期）：死配置采用"先盘点、后裁决、再动手"三步走

- 原因：审查报告点名的清单写于 A 批修复之前，部分项（如缓存 keyPrefix）已被后续修复接通；直接按旧清单删会误伤。Task 2-1 产出裁决表并经用户确认后才执行删除/接通。

### D3（2026-07-07，用户拍板 Q5/Q6）：阶段三缓存兼容与阶段四启动

- Q5 拍板：Jackson 3 迁移的 Redis 缓存序列化采用"升级清一次缓存"，不做双读兼容层（省一个兼容层的开发与回收成本）；升级指南写明清缓存步骤，proud-day 侧对应清 `pd:cache:*`。
- Q6 落地：proud-day 仓库 `/Users/andy/DevOps/SourceCode/business-projects/proud-day-projects/proud-day-backend`，已完成现状调研并在该仓库另立三件套 `docs/changes/nebula-persistence-adoption/`（spec + tasks + log）。
- Q7（2026-07-07 已确认）：`revision` 升 `2.1.0-SNAPSHOT`（在 `nebula-remaining-work` 分支执行，该分支自 hardening-b 检出）。调研发现 proud-day 依赖坐标 `io.nebula:*:2.0.1-SNAPSHOT` 且 parent 为 Boot 3.5.8，与本分支（Boot 4.1）版本号相同——快照仓库一旦被 Boot 4.1 产物覆盖，proud-day 重构建即静默吃到跨代际类库。升代号是最低成本的隔离手段。

### D4（2026-07-08）：实现文档编写期发现的两处方案修正

- **Task 1 验证方式修正**：原计划"扩展 autoconfigure 的 NebulaStarterDefaultsIntegrationTest 断言 starter defaults 注入"不可行——autoconfigure 测试若依赖 starter 会形成 Maven 循环依赖（starter → autoconfigure）。改为两半验证：autoconfigure 既有测试证机制，三个 starter 模块各加最小测试证内容。review-fixes tasks.md Task 1 已同步。
- **Jackson 3 验收口径修正**：jackson-annotations 3.x 保留 `com.fasterxml.jackson.annotation` 原包名（官方兼容设计），注解 import 无需迁移；"零 com.fasterxml.jackson 命中"验收不成立，改为 databind/core/datatype 三包前缀零命中。spec.md 2.3 与验收标准已同步。

### D5（2026-07-08）：ChatGPT 外部审查（第二轮，针对 implementation.md）核对结论与采纳

10 条意见逐条本地取证后**全部成立**（含 2 条部分成立），已改入文档：

- **P1-1 分支口径**（成立）：spec/tasks/review-fixes 四处 `nebula-hardening-b` 统一改为 `nebula-remaining-work`；log/Q7 中"b 分支"表述同步澄清。
- **P1-2 gRPC 版本混装**（成立，本轮最有价值的发现）：根 pom `:316` 自管 `grpc-bom:1.68.1` import 会压过 Boot 4.1 BOM 托管的 grpc 1.80.0（子 POM dependencyManagement 优先于父 BOM）；且 starter 默认带非 shaded `grpc-netty`，与保留的 `grpc-netty-shaded`（`GrpcRpcClient.java:6` 直接 import shaded 类，不能删）并存会两套传输冲突。Task 3 已改为：删根 grpc-bom import、`grpc.version` 属性升 1.80.0 留给 protobuf 插件坐标、starter 排除 `grpc-netty`、dependency:tree 验收单一版本。
- **P1-3 gRPC 测试依赖**（成立，已本地实证）：`org.springframework.boot:spring-boot-starter-grpc-server-test:4.1.0` 可拉取（本机 .m2 已有）；解包 `spring-boot-grpc-test-4.1.0.jar` 确认注解为 `org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort`。Task 5 从"执行时核验坐标"改为直接写死已验证坐标。
- **P2-1 Jackson POM 清单不全**（成立）：全仓 rg 命中 20+ 个 pom（gateway/storage/search/websocket/crawler/rpc-async/messaging/foundation/cache/mongodb 等），远超预列 5 处。Task 3-0 改为"全仓扫描生成清单"，预列条目降级为起点示例。
- **P2-2 ValidationException 示例不可编译**（成立）：该类无单字符串构造（`ValidationException.java:25-41`），示例改为 `(field, message, value)` 三参构造。
- **P2-3 executor 可选注入**（成立）：`rpcExecutor` 只在 HTTP RPC 启用时创建，Task 12.3 明确用 `ObjectProvider<Executor>.getIfAvailable(ForkJoinPool::commonPool)`，避免 gRPC-only 启动失败。
- **P2-4 ES 测试只测判定不测行为**（成立）：Task 11 测试补第二层——prod + sslVerificationEnabled=false 必须证明拿不到 trust-all SSLContext。
- **P2-5 提交口径矛盾**（成立）：统一为"一任务一提交"，撤销 Task 6/8 可合并的说法（implementation.md 与 review-fixes tasks.md 同步）。
- **P3-1 README/AGENTS 版本口径**（成立）：`README.md:4,11`、`AGENTS.md:12` 仍写 Boot 3.5.8，已加入 Task 13 收尾清单（允许提前单独提交）。
- **P3-2 RestClient 401 断言**（成立）：`HttpRpcClient.sendRequest`:312-332 catch 住所有异常封装为 `RpcResponse.exception`，client 侧拿不到裸 401。Task 6 测试拆两层断言：controller 层断 401 状态、client 层断封装后的失败响应。

## 知识发现（proud-day 调研，2026-07-07）

- 33 个 Mapper 全部继承 MP 原生 `BaseMapper`——T-A4-1 的 markerInterface 设计生效，"零改动迁移"成立。
- proud-day pom 显式覆盖 `mybatis-plus-spring-boot3-starter:3.5.12`（注释注明为压制 nebula 传递依赖），与 nebula 新版的 MP 3.5.16 boot4 starter 冲突，迁移时必删。
- prod Hikari `initialization-fail-timeout: -1`（DB 不可达仍完成启动）在 `DataSourceManager.PoolConfig` 无对应参数，迁移后启动语义变化，已列入 proud-day spec 待澄清 Q2。
- proud-day 自有 `MybatisPlusConfig`（分页）/`MybatisAuditConfig`（审计兜底）与 nebula 侧同类 Bean 均为 `@ConditionalOnMissingBean` 关系，可共存不冲突，迁移期不必删。

## 死配置盘点表（Task 2-1 产出，待填写）

| 配置项 | 定义位置 | 消费点 | 裁决 | 理由 |
|--------|----------|--------|------|------|
| （盘点时填写） | | | | |

## 阶段一任务验证记录

### Task 1 [C-1] Starter 默认锁定 MVC 走 Jackson 2
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure -Dtest=NebulaStarterDefaultsIntegrationTest && mvn test -pl starter/nebula-starter-web,starter/nebula-starter-all,starter/nebula-starter-mcp`
- autoconfigure 结果: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (11.056s)
- starter 结果: Nebula Starter Web SUCCESS [2.704s], Nebula Starter MCP SUCCESS [1.966s], Nebula Starter All SUCCESS [2.274s]; Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (7.300s)

### Task 2 [C-1] MockMvc 脱敏回归哨兵测试
- 验证命令: `mvn test -pl application/nebula-web -Dtest=SensitiveDataMvcMaskingTest`
- 结果: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (4.475s)
- 哨兵有效性验证: 临时移除 preferred-json-mapper=jackson2 后测试转红, 报 `JSON path "$.data.phone" expected:<138****5678> but was:<13812345678>`, 证明哨兵确实在看门。恢复属性后测试恢复绿色。
- SB4 包迁移: `AutoConfigureMockMvc` 从 `org.springframework.boot.test.autoconfigure.web.servlet` 迁移到 `org.springframework.boot.webmvc.test.autoconfigure`; 需额外依赖 `spring-boot-starter-webmvc-test`

### Task 3 [C-2] nebula-rpc-grpc 接入 Boot 官方 gRPC Server Starter
- 验证命令: `mvn -q compile -pl infrastructure/rpc/nebula-rpc-grpc -am && mvn dependency:tree -pl infrastructure/rpc/nebula-rpc-grpc`
- 编译结果: BUILD SUCCESS (20s)
- 依赖树确认: spring-boot-starter-grpc-server:4.1.0 出现; 所有 io.grpc 坐标统一为 1.80.0; 传输层仅 grpc-netty-shaded(无 grpc-netty); spring-grpc-core:1.1.0 作为 starter 传递依赖自动引入
- 根 pom 变更: 删除自管 grpc-bom:1.68.1 import + spring-grpc-core dependencyManagement; grpc.version 属性改 1.80.0(protobuf 插件坐标引用); spring-grpc.version 属性已删

### Task 9 [M-2] EPP 迁移至 SB4 新接口与新注册键
- 验证命令: `mvn test -pl autoconfigure/nebula-autoconfigure`
- 结果: Tests run: 23, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS (6.748s)
- NebulaStarterDefaultsIntegrationTest 回归通过(EPP 新键注册生效)
- `rg "net.devh" --type java` 主代码零命中(删除 GatewayGrpcServerExcludeConfiguration + 对应 spring.factories)

## 踩坑记录

（待开发过程中填写）

## 知识发现

（待开发过程中填写）

## Spec-Code 偏差

（实现与 Spec 不一致时，先更新 Spec 再改代码，并在此登记）
