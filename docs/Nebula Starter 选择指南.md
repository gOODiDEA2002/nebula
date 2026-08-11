# Nebula Starter 选择指南

Starter 负责组合依赖并提供最低优先级的默认开关。应用只选择一个最接近自身形态的
主 Starter，再按需增加独立模块，避免同时引入多个职责重叠的 Starter。

## 选择表

| Starter | 适用场景 | 主要能力 |
| --- | --- | --- |
| `nebula-starter-minimal` | CLI、批处理、只需要基础能力的服务 | Foundation、自动配置、基础 Spring Boot |
| `nebula-starter-web` | REST API、管理后台、单体 Web 应用 | Web、Security、Persistence、Cache |
| `nebula-starter-service` | 需要注册发现和服务间调用的业务服务 | Web、HTTP/gRPC RPC、Nacos、RabbitMQ、Redis Lock |
| `nebula-starter-gateway` | API 网关 | Gateway WebFlux、Nacos、Redis 限流、Resilience4j |
| `nebula-starter-ai` | LLM、Embedding、RAG 应用 | Spring AI、AI Core、Cache |
| `nebula-starter-mcp` | MCP Server 或 MCP Client 应用 | AI Starter、MCP、WebMVC |
| `nebula-starter-task` | XXL-JOB 执行器 | Task、Web、HTTP RPC，可选 Nacos |
| `nebula-starter-api` | 服务间共享契约 | RPC 接口与注解、Validation、MyBatis-Plus 类型 |
| `nebula-starter-all` | 需要大部分框架能力的全功能单体 | Web、数据、RPC、发现、消息、存储、搜索、任务、AI、锁、WebSocket |

## 决策顺序

```text
只定义接口和 DTO                 -> nebula-starter-api
只需要基础工具                   -> nebula-starter-minimal
需要 MCP                         -> nebula-starter-mcp
主要运行定时或调度任务           -> nebula-starter-task
需要 AI，但不需要完整业务服务栈  -> nebula-starter-ai
作为统一入口转发 HTTP 请求       -> nebula-starter-gateway
需要 Nacos 和 RPC                -> nebula-starter-service
普通 REST API                    -> nebula-starter-web
明确需要大部分模块               -> nebula-starter-all
```

## 添加依赖

```xml
<properties>
    <nebula.version>2.1.0</nebula.version>
</properties>

<dependency>
    <groupId>com.nebula-projects</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

## 默认启用项

| Starter | 默认开关 |
| --- | --- |
| Minimal | 无；Security 本身为默认启用的纯内存能力 |
| Web | Persistence、Cache |
| Service | Persistence、Cache、HTTP RPC、RPC Discovery、Nacos、Redis Lock |
| Gateway | Gateway、Nacos |
| AI | AI、Cache |
| MCP | AI、MCP |
| Task | Task、HTTP RPC |
| API | 无 |
| All | Persistence、Cache、RabbitMQ、HTTP RPC、RPC Discovery、Nacos、Redis Lock、Task、AI、Spring WebSocket |

依赖存在不等于功能一定启用。例如 Service Starter 携带 gRPC 和 RabbitMQ 实现，但默认只
启用 HTTP RPC；需要使用 gRPC 或 RabbitMQ 时应显式配置对应开关。

## 覆盖默认值

Starter 默认值优先级低于应用配置，可以直接关闭：

```yaml
nebula:
  data:
    persistence:
      enabled: false
  discovery:
    nacos:
      enabled: false
  lock:
    enabled: false
```

`nebula-starter-all` 默认启用较多外部组件。开发环境缺少 RabbitMQ、Nacos 或 Redis 时，
需要在应用配置中逐项关闭，仓库中的 `examples/starter-all-example` 提供了可运行示例。

## API 契约模块

API 模块只放以下内容：

- RPC 服务接口
- DTO、枚举和校验注解
- 与接口签名直接相关的通用类型

API 模块不放 Controller、Repository、服务实现、数据库连接配置或 Starter 默认开关。

## 依赖排除

确实不需要某个传递实现时，可使用 Maven exclusion：

```xml
<dependency>
    <groupId>com.nebula-projects</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>${nebula.version}</version>
    <exclusions>
        <exclusion>
            <groupId>com.nebula-projects</groupId>
            <artifactId>nebula-messaging-rabbitmq</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

排除实现后同步关闭对应开关，避免配置表达的能力与 classpath 不一致。

## 相关文档

- [快速开始](framework/QUICK_START.md)
- [配置说明](Nebula框架配置说明.md)
- [自动配置指南](framework/AUTO_CONFIGURATION_GUIDE.md)
- [示例应用](../examples/README.md)
