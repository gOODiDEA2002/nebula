# Nebula Framework 排查指南

## 先看启动摘要

应用就绪后，Nebula 会输出启动摘要。摘要中的模块状态来自最终 Environment，包含应用
配置和 Starter 默认值。未显式开启且没有 Starter 默认值的 Nacos、RPC、Async RPC 应显示
为 `DISABLED`。

```text
======================================================================
                    NEBULA FRAMEWORK STARTUP SUMMARY
======================================================================
  [Framework Info]
    Version              : 2.1.0-SNAPSHOT
    Profile              : default

  [Service Discovery (Nacos)]
    Status               : DISABLED
======================================================================
```

## 诊断端点

项目包含 Actuator 时，可暴露 Nebula 诊断端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,nebula-diagnostic
```

```bash
curl http://localhost:8080/actuator/nebula-diagnostic | jq
```

诊断结果包含框架版本、活动 Profile、发现、RPC、Async RPC 和关键依赖是否存在。端点不返回
密码、Token 或 API Key。

## 自动配置没有生效

按以下顺序检查：

1. 对应实现 JAR 是否在依赖树中。
2. 顶层 `enabled` 是否为 `true`。
3. Starter 的 `nebula-defaults.properties` 是否被打进最终 JAR。
4. 应用配置是否用更高优先级把 Starter 默认值覆盖为 `false`。
5. 是否缺少 `@ConditionalOnClass` 要求的第三方类。
6. 是否有业务 Bean 触发 `@ConditionalOnMissingBean`，替换了框架默认实现。

```bash
mvn dependency:tree
mvn spring-boot:run -Dspring-boot.run.arguments=--debug
```

`--debug` 会输出 Spring Boot 条件评估报告，可直接查看某个自动配置未命中的原因。

## Nacos 连接失败

确认配置已显式开启：

```yaml
spring:
  application:
    name: order-service

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: ""
      username: ${NACOS_USERNAME:}
      password: ${NACOS_PASSWORD:}
```

检查项：

- `server-addr` 从应用运行环境是否可达。
- public 命名空间 ID 是空字符串，不是 `public`。
- Nacos 开启鉴权时必须显式配置账号；框架不再使用 `nacos/nacos` 默认弱凭据。
- `spring.application.name` 不得为空。
- 容器环境下检查注册 IP、`preferred-networks` 和 `ignored-interfaces`。

## RPC Bean 不存在

检查顶层开关和扫描入口：

```yaml
nebula:
  rpc:
    http:
      enabled: true
    discovery:
      enabled: true
```

API 契约模块使用 `@RemoteService`，由 `@EnableRpcClients` 或 API 模块自己的自动配置扫描。
旧 `@RpcClient` 注解仅用于兼容，不应继续新增。

RPC Discovery 还要求 `ServiceDiscovery` 和至少一个可用 `RpcClient` Bean。使用 gRPC 时同时检查：

```yaml
nebula:
  rpc:
    grpc:
      enabled: true
      client:
        auth-token: ${GRPC_AUTH_TOKEN:}
      server:
        auth-token: ${GRPC_AUTH_TOKEN:}
```

服务端配置令牌而客户端为空时，调用会返回 `UNAUTHENTICATED`。

## HTTP RPC 超时或连接耗尽

HTTP RPC 超时字段当前使用毫秒数：

```yaml
nebula:
  rpc:
    http:
      enabled: true
      client:
        connect-timeout: 5000
        read-timeout: 10000
        max-connections: 200
        max-connections-per-route: 100
        keep-alive-time: 60000
        idle-evict-time: 60000
```

检查连接池总上限与单路由上限，确认下游超时没有大于网关或调用方超时。应用关闭时
`rpcHttpClient` 应随 Spring 上下文关闭；若线程仍不退出，检查是否存在业务侧自建客户端。
`keep-alive-time` 控制连接最大存活时间，`idle-evict-time` 控制空闲连接清理时间；后者未配置时
沿用前者。

## JWT 登录后仍返回 401

检查项：

- JWT 密钥长度不少于 32 个字符，所有实例使用同一密钥。
- 登录服务使用 `JwtService`，不再手工调用旧 `io.nebula.web.auth.JwtUtils`。
- access token 的 `type` 为 `access`；refresh token 不能直接访问业务接口。
- 请求头名称和 `Bearer ` 前缀与 `SecurityProperties` 一致。
- 机器时间没有明显漂移。

## Redis 消息 payload 变成 Map

Nebula 2.1 的 `RedisMessageSerializer.deserialize(json, payloadType)` 会按参数化类型恢复
`Message<T>`。出现 Map 时优先检查：

1. 运行时是否仍使用旧 2.1 快照。
2. 生产者和消费者是否使用相同 DTO。
3. DTO 是否能被应用的 Jackson 3 `ObjectMapper` 反序列化。
4. 是否存在旧 Jackson 2 消息或缓存数据未清理。

## TLS 配置被忽略

`ssl-verification-enabled=false` 和 `trust-all-certs=true` 仅在活动 Profile 非空，且全部属于
`dev`、`test`、`local` 时生效。默认 Profile、`prod`、未知 Profile 或 `prod,dev` 混合 Profile
都会保持证书校验，这是预期安全行为。

## 干净构建与增量构建结果不一致

升级依赖后，旧 `target/classes` 可能留下与当前源码不一致的字节码，表现为 reactor 编译报
「class file not found」或 JaCoCo 报「classes do not match execution data」。执行：

```bash
mvn clean test
```

只有干净构建仍失败时，才继续按当前源码和依赖树定位。

## 提交问题时附带的信息

- 完整异常链的首个业务异常和最底层原因
- `mvn dependency:tree` 中相关组件版本
- 启动摘要和 `/actuator/nebula-diagnostic` 输出，先删除地址和敏感信息
- 活动 Profile 与相关配置项，密钥只保留是否为空和长度
- 可重复的最小命令或示例模块
