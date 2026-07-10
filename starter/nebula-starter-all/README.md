# Nebula Starter All

`nebula-starter-all` 面向需要大部分框架能力的全功能单体应用。它会携带较多依赖并默认
开启多个外部组件，不适合作为所有项目的默认入口。

## 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-all</artifactId>
    <version>2.1.0-SNAPSHOT</version>
</dependency>
```

## 包含能力

- Foundation、Web、Validation、Actuator、Security
- Persistence、Cache
- Nacos Discovery
- HTTP RPC；gRPC 实现为可选依赖
- RabbitMQ Messaging
- Elasticsearch Search
- MinIO Storage
- Task、Spring AI
- Redis Lock
- Spring WebSocket
- Nebula AutoConfiguration

RocketMQ、Redis Messaging、Aliyun OSS、Neo4j、Crawler、Netty WebSocket 和 MCP 不在本
Starter 的默认组合中，需要时显式添加对应模块或使用更合适的 Starter。

## 默认开关

```properties
nebula.data.persistence.enabled=true
nebula.data.cache.enabled=true
nebula.messaging.rabbitmq.enabled=true
nebula.rpc.http.enabled=true
nebula.rpc.discovery.enabled=true
nebula.discovery.nacos.enabled=true
nebula.lock.enabled=true
nebula.task.enabled=true
nebula.ai.enabled=true
nebula.websocket.enabled=true
```

这些值通过 `META-INF/nebula-defaults.properties` 以最低优先级注入，应用配置可以覆盖。

## 本地最小化配置

本地没有对应外部服务时，应显式关闭：

```yaml
nebula:
  data:
    persistence:
      enabled: false
  messaging:
    rabbitmq:
      enabled: false
  rpc:
    http:
      enabled: false
    discovery:
      enabled: false
  discovery:
    nacos:
      enabled: false
  lock:
    enabled: false
  task:
    enabled: false
  ai:
    enabled: false
```

Security JWT 启用时仍必须提供至少 32 个字符的 `nebula.security.jwt.secret`。

## 按需增加 gRPC

`nebula-rpc-grpc` 在本 Starter 中标记为 Maven optional。需要 gRPC 时显式添加：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-grpc</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

```yaml
nebula:
  rpc:
    grpc:
      enabled: true
```

## 验证

```bash
mvn test -pl starter/nebula-starter-all -am
mvn -q -f examples/starter-all-example spring-boot:run
```

## 相关文档

- [Starter 选择指南](../../docs/Nebula%20Starter%20选择指南.md)
- [配置说明](../../docs/Nebula框架配置说明.md)
- [全功能示例](../../examples/starter-all-example/README.md)
