# Nebula 自动配置指南

## 注册入口

集中自动配置清单位于：

```text
autoconfigure/nebula-autoconfigure/src/main/resources/
  META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Starter 默认值处理器通过 Spring Boot 4 的 EnvironmentPostProcessor imports 注册：

```text
autoconfigure/nebula-autoconfigure/src/main/resources/
  META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports
```

各 Starter 的默认开关位于自身的 `META-INF/nebula-defaults.properties`。

## 启用规则

框架采用三级启用策略：

| 级别 | `matchIfMissing` | 默认策略 | 代表模块 |
| --- | --- | --- | --- |
| Level 1 | `true` | 默认启用 | Security 等纯内存基础能力 |
| Level 2 | `false` | 默认关闭 | Persistence、Redis Lock、RabbitMQ、Nacos、Elasticsearch、MinIO、OSS、Neo4j 等外部服务 |
| Level 3 | `false` | 默认关闭 | HTTP/gRPC/Async RPC、RPC Discovery、Gateway、AI、Crawler、Task 等特定部署形态 |

核心原则是「依赖决定是否具备能力，配置决定是否启动能力」。直接引入实现 JAR 不应造成
数据库、Redis、消息队列或远程服务连接。

## Starter 默认值

| Starter | 默认注入的开关 |
| --- | --- |
| `nebula-starter-minimal` | 无 |
| `nebula-starter-web` | `nebula.data.persistence.enabled=true`、`nebula.data.cache.enabled=true` |
| `nebula-starter-service` | Web 默认项、HTTP RPC、RPC Discovery、Nacos、Redis Lock |
| `nebula-starter-gateway` | Gateway、Nacos |
| `nebula-starter-ai` | AI、Cache |
| `nebula-starter-mcp` | AI、MCP |
| `nebula-starter-task` | Task、HTTP RPC |
| `nebula-starter-all` | Persistence、Cache、RabbitMQ、HTTP RPC、RPC Discovery、Nacos、Lock、Task、AI、WebSocket |
| `nebula-starter-api` | 无 |

`NebulaStarterDefaultsPostProcessor` 把这些属性放在 Environment 最低优先级。优先级从高到低为：

1. 命令行参数
2. 环境变量和系统属性
3. 应用配置文件
4. Starter 默认值
5. 配置属性类中的 Java 默认值

## Bean 定制

自动配置遵守以下规则：

- 公共服务 Bean 使用 `@ConditionalOnMissingBean`，允许应用替换实现。
- JSON 组件通过 `ObjectProvider<ObjectMapper>` 复用应用定制，缺失时再创建 Jackson 3 `JsonMapper`。
- 同类型 Bean 超过一个时使用明确的 `@Qualifier`，避免启动顺序决定注入结果。
- 长期资源单独声明为 Bean，并设置 `destroyMethod` 或实现 `AutoCloseable`。
- 配置类本身不使用组件扫描兜底，避免绕过 `@ConditionalOnProperty`。

## 新增自动配置检查表

1. 顶层开关是否与 `@ConfigurationProperties` 前缀一致。
2. 外部服务和特定部署形态是否 `matchIfMissing=false`。
3. `@ConditionalOnClass` 是否只引用对应模块的稳定入口类型。
4. 应用自定义 Bean 是否能覆盖默认 Bean。
5. 资源是否能随 Spring 上下文关闭。
6. 是否复用应用的 `ObjectMapper`、线程池或连接工厂。
7. Starter 默认键是否确实被某个条件读取，避免写入无效配置。
8. 是否增加「默认关闭、显式启用、显式关闭、缺类」四类条件测试。
9. 是否在组件摘要中暴露启用状态和关键非敏感参数。

## 排查

启用 Spring Boot 条件报告：

```yaml
debug: true
```

检查某个属性的最终值时，优先查看启动摘要和 Actuator Environment；不要只看
`application.yml`，Starter 默认值也属于 Environment 的一部分。详细步骤见
[排查指南](../Nebula%20Framework%20排查指南.md)。
