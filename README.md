# Nebula Framework

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Nebula 是一个现代化的 Java 后端框架，基于 Spring Boot 3.x 和 Java 21 构建，提供企业级应用开发的完整解决方案。

## 特性

- **现代化技术栈**: Java 21 + Spring Boot 3.5.8 + Maven
- **模块化架构**: 清晰的分层设计，按需引入
- **安全认证**: JWT + RBAC 权限体系，安全注解支持
- **数据持久化**: MyBatis-Plus 集成，读写分离/分库分表
- **多级缓存**: Caffeine（本地） + Redis（分布式），防穿透/雪崩
- **消息队列**: RabbitMQ 集成，延迟消息支持
- **RPC 通信**: HTTP RPC（RestClient） + gRPC，内置负载均衡
- **服务发现**: Nacos 注册与发现，自动注册/注销
- **分布式锁**: 基于 Redisson，公平锁/读写锁/回调封装
- **WebSocket**: Spring WebSocket + Netty 双实现，Redis 集群消息
- **对象存储**: MinIO + 阿里云 OSS 双实现
- **搜索引擎**: Elasticsearch 集成，全文检索/聚合/建议
- **AI 集成**: Spring AI 封装，聊天/嵌入/向量存储
- **API 网关**: Spring Cloud Gateway，HTTP 反向代理/限流/日志
- **任务调度**: XXL-JOB 集成，声明式任务处理器
- **Web 框架**: 认证拦截/限流/响应缓存/性能监控/数据脱敏

## 架构设计

```
Nebula Framework

  核心层 (Core)
    nebula-foundation         基础工具、异常处理、统一结果封装
    nebula-security           JWT 认证、RBAC 授权、安全注解

  基础设施层 (Infrastructure)
    数据访问 (Data)
      nebula-data-persistence   MyBatis-Plus、读写分离、分库分表
      nebula-data-cache         Caffeine + Redis 多级缓存
      nebula-data-mongodb       MongoDB 支持（可选）

    消息传递 (Messaging)
      nebula-messaging-core     消息抽象
      nebula-messaging-rabbitmq RabbitMQ 实现

    RPC 通信 (RPC)
      nebula-rpc-core           RPC 抽象
      nebula-rpc-http           HTTP RPC（RestClient）
      nebula-rpc-grpc           gRPC 实现

    服务发现 (Discovery)
      nebula-discovery-core     服务发现核心
      nebula-discovery-nacos    Nacos 实现

    WebSocket
      nebula-websocket-spring   Spring WebSocket 实现
      nebula-websocket-netty    Netty WebSocket 实现

    分布式锁 (Lock)
      nebula-lock-core          锁抽象
      nebula-lock-redis         Redisson 实现

    存储服务 (Storage)
      nebula-storage-core       存储抽象
      nebula-storage-minio      MinIO 实现
      nebula-storage-aliyun-oss 阿里云 OSS 实现

    搜索服务 (Search)
      nebula-search-core        搜索抽象
      nebula-search-elasticsearch Elasticsearch 实现

    AI 服务 (AI)
      nebula-ai-core            AI 核心
      nebula-ai-spring          Spring AI 集成

    网关 (Gateway)
      nebula-gateway-core       HTTP 反向代理、限流、日志

    爬虫 (Crawler)
      nebula-crawler-core/http/browser/proxy/captcha

  应用层 (Application)
    nebula-web                认证/限流/缓存/性能监控/健康检查
    nebula-task               XXL-JOB 任务调度

  集成层 (Integration)
    nebula-integration-payment       支付集成
    nebula-integration-notification  短信通知

  自动配置层
    nebula-autoconfigure      统一自动配置（所有模块集中管理）

  启动器 (Starter)
    nebula-starter-minimal    最小化（仅 foundation）
    nebula-starter-web        Web 应用
    nebula-starter-service    微服务（RPC + 发现 + 锁）
    nebula-starter-gateway    API 网关
    nebula-starter-ai         AI 应用
    nebula-starter-all        单体应用（全功能）
    nebula-starter-api        API 契约模块
```

## 快速开始

### 环境要求

- Java 21+
- Maven 3.6+

### 1. 选择 Starter

根据项目类型选择合适的 Starter：

| Starter | 适用场景 | 默认启用的模块 |
|---------|---------|---------------|
| `nebula-starter-web` | REST API / 管理后台 / 单体应用 | Persistence, Cache |
| `nebula-starter-service` | 微服务 | Persistence, Cache, HTTP RPC, Nacos, Lock |
| `nebula-starter-gateway` | API 网关 | Gateway, Nacos |
| `nebula-starter-ai` | AI 应用 | AI, Cache |
| `nebula-starter-minimal` | CLI / 批处理 | 无（仅 Security 默认） |

### 2. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 3. 创建应用

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 4. 创建控制器

```java
@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Hello, Nebula!");
    }
}
```

### 5. 配置应用

```yaml
spring:
  application:
    name: my-app
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: ${DB_PASSWORD}

nebula:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400
```

> Starter 已通过 `NebulaStarterDefaultsPostProcessor` 自动启用对应模块，
> 无需重复声明 `enabled: true`。如需额外模块，显式配置即可。

## 模块启用策略

框架采用三级启用策略，通过 `@ConditionalOnProperty` 的 `matchIfMissing` 控制：

| 级别 | 策略 | 说明 |
|------|------|------|
| Level 1 | 默认启用 | Security（纯内存组件） |
| Level 2 | 默认禁用 | 需要外部服务（DB/Redis/MQ/ES） |
| Level 3 | 默认禁用 | 特定部署形态（RPC/Gateway/AI/Crawler） |

各 Starter 通过 `META-INF/nebula-defaults.properties` 为目标应用类型预置默认值，
由 `NebulaStarterDefaultsPostProcessor` 以最低优先级注入 Environment，
用户 `application.yml` 始终可以覆盖。

## 开发指南

### 构建项目

```bash
# 编译项目
mvn clean compile

# 安装到本地仓库（首次运行必需）
mvn install -DskipTests

# 编译特定模块
mvn clean compile -pl core/nebula-foundation

# 运行测试
mvn test
```

### 运行示例应用

```bash
# 安装所有模块
mvn install -DskipTests

# Web 示例
mvn -q -f examples/starter-web-example spring-boot:run

# 微服务示例
mvn -q -f examples/starter-service-example spring-boot:run

# AI 示例（需配置 API Key）
mvn -q -f examples/starter-ai-example spring-boot:run
```

### 验证接口

```bash
# 健康检查
curl http://localhost:8080/health/ping

# 性能监控
curl http://localhost:8080/performance/status
```

## 监控

框架内置监控体系：

- **健康检查**: `/health/ping`, `/health/status`, `/health/liveness`, `/health/readiness`
- **性能监控**: `/performance/metrics`, `/performance/system`, `/performance/status`
- **请求日志**: 自动记录请求耗时，慢请求标记
- **Micrometer**: 可对接 Prometheus/Grafana

## 文档

- [框架使用指南](docs/Nebula框架使用指南.md) -- 各模块详细用法与代码示例
- [配置说明](docs/Nebula框架配置说明.md) -- 配置项完整参考
- [Starter 选择指南](docs/Nebula%20Starter%20选择指南.md) -- 选型建议与对比
- [框架审查报告](docs/nebula-framework-review.md) -- 架构评审与优化记录
- [自动配置指南](docs/framework/AUTO_CONFIGURATION_GUIDE.md) -- 自动配置机制详解
- [快速开始](docs/framework/QUICK_START.md) -- 分步骤详细教程
- [架构说明](docs/framework/ARCHITECTURE.md) -- 架构设计与决策
- [FAQ](docs/FAQ.md) -- 常见问题
- [贡献指南](docs/CONTRIBUTING.md)

## 许可证

本项目采用 Apache License 2.0 许可证。详情请查看 [LICENSE](LICENSE) 文件。
