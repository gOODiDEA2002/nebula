# Nebula 2.1 实现审查

> 审查日期：2026-07-10
> 审查基线：Nebula `2.1.0-SNAPSHOT`、Java 21、Spring Boot 4.1.0
> 审查范围：Spring Boot 4 / Jackson 3 升级后的依赖、自动配置、认证、RPC、消息、TLS 与 Starter

## 结论

升级主线已经完成，但审查发现若干会在真实接入中触发的依赖错配、鉴权失败和默认配置
失效问题。下表问题均已在本次审查中修复并补充针对性测试；整仓验证结果见「验证记录」。

## 已修复问题

| 级别 | 问题 | 影响 | 修复 |
| --- | --- | --- | --- |
| 高 | Web 登录仍用旧 `JwtUtils` 生成不带 `type=access` 的令牌 | 登录成功后会被 `JwtAuthenticationFilter` 拒绝 | `DefaultAuthService` 与过滤器统一使用 `JwtService`，保留废弃兼容构造器 |
| 高 | Springdoc 2.2.0 与 Spring Boot 4.1 不兼容 | `/v3/api-docs` 可能启动失败或运行异常 | 升至 Springdoc 3.0.3，并增加真实 Web 上下文测试 |
| 高 | 自动配置子模块覆盖 Spring Cloud 2025.1.2 为 2025.0.0 | 实际解析到旧 Spring Cloud 4.3 组件 | 删除子模块 BOM 覆盖，统一由根 POM 管理 |
| 高 | gRPC 服务端支持令牌校验，框架客户端无法发送令牌 | 开启安全配置后框架客户端全部调用失败 | 增加 `nebula.rpc.grpc.client.auth-token` 并附加 gRPC Metadata |
| 高 | Redis 消息反序列化忽略 `payloadType` | 泛型 payload 退化为 Map，消费端可能类型转换失败 | 使用 Jackson 3 参数化类型构造 `Message<T>`，恢复公共对象序列化辅助方法 |
| 高 | Redis Lock 缺少配置时默认启用 | 直接引入模块可能意外连接 Redis | 恢复 `matchIfMissing=false`，Starter 负责按场景开启 |
| 高 | MongoDB 模块被注释出 Reactor，仍依赖已删除的 `Repository`，且文本搜索、聚合和专用索引是空实现或错误实现 | 整仓成功会掩盖该模块无法编译，运行时还可能静默返回空结果 | 恢复 Reactor 构建；改为自包含仓储接口；实现全文查询、聚合、文本/地理索引与当前 Fluent API；增加 8 个测试 |
| 中 | 多个自动配置自行创建裸 `JsonMapper` | 应用注册的时间、枚举和业务模块失效 | HTTP/gRPC/Async RPC、Redis 消息、Nacos 存储、XXL-JOB 优先复用应用 `ObjectMapper` |
| 中 | HTTP RPC 的 Apache 客户端藏在 RestClient 内部 | 上下文关闭时连接池无法可靠释放 | 独立声明可关闭 Bean，启用过期和空闲连接清理 |
| 中 | Elasticsearch 与爬虫在无 Profile 或混合 Profile 时允许跳过 TLS 校验 | 配置遗漏可能把开发设置带入生产 | 仅当活动 Profile 非空且全部为 `dev/test/local` 时允许跳过校验 |
| 中 | JJWT 子模块硬编码 0.12.3，根 POM 管理 0.13.0 | 同一仓库版本不收敛，升级行为不确定 | 删除子模块版本，统一解析为 0.13.0 |
| 中 | `RpcClient.call` 把返回泛型绑定到服务接口类型 | 直接调用无法按方法返回类型编译 | 服务类型改为 `Class<?>`，返回泛型独立推断，JVM 方法描述符不变 |
| 中 | `starter-all` 的消息默认键无人读取 | 文档声称默认启用，运行时实际未启用 | 改为 `nebula.messaging.rabbitmq.enabled=true` 并增加默认值测试 |
| 中 | `starter-all` 开启 WebSocket 但未携带实现 | 开关存在但功能不可用 | 加入 WebSocket Core 和 Spring WebSocket 实现 |
| 中 | `starter-all` 默认开启 Redis Lock，但实现依赖标记为 optional | 业务只引入 Starter 时无法获得锁实现 | 让 `nebula-lock-redis` 作为全功能 Starter 的传递依赖 |
| 中 | 启动摘要和诊断端点硬编码 2.0.1，并把外部模块缺省报告为启用 | 排障信息与真实自动配置相反 | 集中解析框架版本，诊断默认状态与 `matchIfMissing=false` 对齐 |
| 中 | Redis 注解处理器通过非静态工厂提前创建配置类和依赖 Bean | 部分 Bean 无法经过完整后处理，代理类可能失效 | 改为静态工厂与延迟依赖，并断言启动日志不再出现提前初始化告警 |
| 中 | 全功能示例同时配置通配来源和凭据 CORS | 浏览器请求会被 Spring 拒绝 | 示例改为明确的本地开发来源 |
| 中 | 中间件脚本写死开发者目录、遗漏 Elasticsearch 传输端口，并启动 7.17 服务供 9.4.2 客户端联调 | 换机器执行会写错目录，Compose 端口无效，搜索联调没有兼容保证 | 默认使用脚本目录，可用 `NEBULA_DATA_DIR` 覆盖；补充 `9300` 端口；服务端与客户端统一为 9.4.2；容器内健康检查固定访问 `9200` |
| 低 | Rabbit 延迟消息继续使用废弃的 Jackson 2 命名转换器 | 升级后持续产生弃用告警 | 改用 Spring AMQP 的 `JacksonJsonMessageConverter` |
| 低 | 自动配置缺少直接 `nebula-lock-core` 依赖，Neo4j 缺少注解元数据依赖 | 干净 reactor 编译可能失败或产生元数据告警 | 补充最小直接依赖 |
| 低 | 爬虫独立聚合 POM 声明了不会传递给子模块的旧版本管理 | 单独构建时容易误判依赖来源 | 删除无效配置，所有版本继续由根 POM 管理 |
| 低 | OpenAPI 元数据和部分示例仍展示 2.0.x / Spring Boot 3 | 使用者会按旧版本排障 | 统一为 Nebula 2.1 与 Spring Boot 4.1，并在兼容测试中断言 OpenAPI 版本 |

## 稳定性与性能判断

- HTTP RPC 连接池的连接数、单路由上限和超时配置继续生效；连接最大存活时间与空闲驱逐时间可独立配置，不改变调用协议。
- gRPC 鉴权只在配置令牌时增加固定 Metadata，不增加额外网络调用。
- JSON 组件复用单例 `ObjectMapper`，减少重复模块注册和配置漂移。
- Redis 泛型反序列化使用缓存友好的 Jackson `JavaType` 构造，不引入额外字符串二次解析。
- TLS 防误配在启动阶段判断 Profile，不进入请求热路径。
- Springdoc 通过真实 `/v3/api-docs` 请求验证，不只依赖编译通过。

## 兼容性说明

- `DefaultAuthService(JwtUtils)` 继续保留并标记废弃，旧应用可以编译运行；新自动配置使用 `JwtService`。
- `RpcClient.call` 的泛型声明变化不改变类型擦除后的 JVM 签名，已有二进制调用保持兼容。
- gRPC 和 HTTP 鉴权默认仍为空，不会改变未启用鉴权的现有调用。
- Redis Lock 默认行为恢复到既定三级启用策略；通过 Service/All Starter 接入时仍由 Starter 自动开启。
- MongoDB `saveAll` 统一为逐项新增或更新，可能增加数据库往返次数；只需批量新增时改用 `insertAll`，重复主键将抛出异常。

## 验证记录

已通过的定向测试：

```text
DefaultAuthServiceTest
SpringdocCompatibilityTest
GrpcServerLoopbackIntegrationTest
RedisMessageSerializerTest
HttpRpcClientConfigTest
GrpcRpcAutoConfigurationConditionTest
ElasticsearchSslGuardTest
RedisLockAutoConfigurationConditionTest
RedisMessagingContainerAmbiguityTest
MongoTemplateTest
```

依赖树已确认：

```text
Spring Cloud 5.0.2（由 2025.1.2 BOM 管理）
Springdoc 3.0.3
JJWT 0.13.0
Elasticsearch Java Client 9.4.2
starter-all: nebula-lock-redis、nebula-messaging-rabbitmq、nebula-websocket-spring 均为 compile 依赖
```

整仓验证结果：

```text
mvn clean test: BUILD SUCCESS，70 个 Reactor 模块全部成功，总耗时 01:21
Surefire 报告：913 tests，0 failures，0 errors，3 skipped
bash -n docker/middleware/setup-dev-env.sh: 通过
mvn -f infrastructure/crawler/pom.xml test: BUILD SUCCESS，6 个模块全部成功
活动文档本地链接检查：0 个失效链接
git diff --check: 通过
```

## 剩余风险

- Mockito 仍提示未来 JDK 将限制动态 Agent 加载，当前不影响测试，后续需要在测试构建中显式配置 Agent。
- 少数测试模块存在多个 SLF4J Provider，当前实际选择 Logback；后续可从测试依赖中排除多余的 `slf4j-simple`。
- 部分兼容入口仍引用已废弃的 `@RpcClient` 注解和旧 `JwtUtils`，计划移除前需要给出独立迁移窗口。
- 外部服务的真实联调仍依赖对应环境；MongoDB 本次完成编译和 mock 行为回归，尚未连接真实副本集验证事务与地理查询。
- 其余测试重点覆盖本地可重复的配置、序列化和 RPC 回环链路。
