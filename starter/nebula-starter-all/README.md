# Nebula Starter All

全功能 Starter，包含 Nebula 框架的所有功能模块，适用于单体应用（Monolithic Application）。

## 适用场景

- **单体应用**：不需要微服务架构，所有功能集成在一个应用中
- **全功能演示**：展示框架的所有能力
- **原型开发**：快速构建包含多种功能的应用原型
- **内部工具**：开发内部管理系统、后台工具等

## 包含的模块

### 基础层
- `nebula-foundation` - 框架基础组件（Result、异常、工具类等）

### Web 层
- `nebula-web` - Web 框架（REST API、异常处理、CORS、认证、限流等）
- `spring-boot-starter-web` (optional)
- `spring-boot-starter-validation` (optional)
- `spring-boot-starter-actuator` (optional)

### 数据层
- `nebula-data-persistence` - 数据持久化（JPA、MyBatis-Plus、分库分表）
- `nebula-data-cache` - 多级缓存（Caffeine + Redis）

### 服务发现
- `nebula-discovery-core` - 服务发现核心
- `nebula-discovery-nacos` (optional) - Nacos 实现

### RPC
- `nebula-rpc-core` - RPC 核心抽象
- `nebula-rpc-http` - HTTP 协议实现
- `nebula-rpc-grpc` (optional) - gRPC 协议实现

### 消息队列
- `nebula-messaging-core` - 消息队列核心
- `nebula-messaging-rabbitmq` (optional) - RabbitMQ 实现
- `nebula-messaging-kafka` (optional) - Kafka 实现

### 搜索
- `nebula-search-core` - 搜索核心
- `nebula-search-elasticsearch` (optional) - Elasticsearch 实现

### 存储
- `nebula-storage-core` - 对象存储核心
- `nebula-storage-minio` (optional) - MinIO 实现

### 任务调度
- `nebula-task-core` - 任务调度核心

### AI 模块
- `nebula-ai-core` - AI 核心抽象
- `nebula-ai-spring` - Spring AI 集成
- `spring-ai-openai-spring-boot-starter` (optional) - OpenAI 集成
- `spring-ai-chroma-store-spring-boot-starter` (optional) - Chroma 向量数据库

### 分布式锁
- `nebula-lock-core` - 分布式锁核心
- `nebula-lock-redis` (optional) - Redis 实现

### 安全
- `nebula-security` - 安全模块（认证、授权）

### 自动配置
- `nebula-autoconfigure` - 自动配置所有功能模块

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-all</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 2. 根据需要显式添加可选依赖

由于某些实现模块标记为 `optional`，你需要根据实际需求显式声明：

#### 使用 Nacos 服务发现
默认已包含，无需额外配置。

#### 使用 gRPC
默认已包含，无需额外配置（但可通过配置禁用）。

#### 使用 RabbitMQ
默认已包含，无需额外配置。

#### 使用 Kafka
需要显式启用（如果不使用 RabbitMQ）。

#### 使用 Elasticsearch
默认已包含，无需额外配置。

#### 使用 MinIO
默认已包含，无需额外配置。

#### 使用 Spring AI
默认已包含 OpenAI 和 Chroma，无需额外配置。

### 3. 配置文件示例

```yaml
server:
  port: 8080

spring:
  application:
    name: my-monolithic-app

nebula:
  # Web 配置
  web:
    exception-handler:
      enabled: true
    cors:
      enabled: true
      allowed-origins: ["*"]
    auth:
      enabled: true
      jwt-secret: your-secret-key
    rate-limit:
      enabled: true
  
  # 数据持久化配置
  data:
    persistence:
      enabled: true
      sources:
        primary:
          type: mysql
          url: jdbc:mysql://localhost:3306/mydb
          username: root
          password: password
    cache:
      enabled: true
      type: multi-level
      redis:
        enabled: true
        host: localhost
        port: 6379
  
  # 服务发现配置
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  
  # RPC 配置
  rpc:
    http:
      enabled: true
    grpc:
      enabled: true
      server:
        port: 9090
  
  # 消息队列配置
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
  
  # 搜索配置
  search:
    elasticsearch:
      enabled: true
      uris:
        - http://localhost:9200
  
  # 存储配置
  storage:
    minio:
      enabled: true
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
  
  # AI 配置
  ai:
    enabled: true
    openai:
      api-key: ${OPENAI_API_KEY}
    vector-store:
      chroma:
        host: localhost
        port: 8000
```

## 注意事项

### 1. 内存占用
由于包含所有功能模块，建议配置足够的 JVM 内存：

```bash
java -Xms1g -Xmx2g -jar your-app.jar
```

### 2. 按需启用功能
虽然所有模块都包含在内，但可以通过配置文件选择性启用：

```yaml
nebula:
  discovery:
    nacos:
      enabled: false  # 禁用 Nacos
  
  messaging:
    rabbitmq:
      enabled: false  # 禁用 RabbitMQ
  
  ai:
    enabled: false    # 禁用 AI 模块
```

### 3. 排除不需要的自动配置
如果某些功能完全不需要，可以在 `application.yml` 中排除：

```yaml
spring:
  autoconfigure:
    exclude:
      - io.nebula.autoconfigure.discovery.DiscoveryAutoConfiguration
      - io.nebula.autoconfigure.messaging.MessagingAutoConfiguration
```

### 4. 与其他 Starter 的对比

| Starter | 适用场景 | 包含模块 | 内存占用 |
|---------|---------|---------|---------|
| `nebula-starter-minimal` | 最小应用 | Foundation | 最低 |
| `nebula-starter-web` | Web 应用 | Foundation + Web | 低 |
| `nebula-starter-service` | 微服务 | Web + RPC + Discovery + Messaging | 中等 |
| `nebula-starter-ai` | AI 应用 | Web + AI + Cache | 中等 |
| **`nebula-starter-all`** | **单体应用** | **所有模块** | **高** |

## 迁移指南

### 从手动依赖迁移

**迁移前**：
```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-web</artifactId>
    </dependency>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-data-persistence</artifactId>
    </dependency>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-data-cache</artifactId>
    </dependency>
    <!-- ... 10+ 个依赖 ... -->
</dependencies>
```

**迁移后**：
```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-all</artifactId>
        <version>${nebula.version}</version>
    </dependency>
    
    <!-- 只需添加业务相关的依赖 -->
    <dependency>
        <groupId>your.project</groupId>
        <artifactId>your-business-module</artifactId>
    </dependency>
</dependencies>
```

## 示例项目

参考 `nebula-example` 项目，它演示了如何使用 `nebula-starter-all` 构建全功能单体应用。

## 相关文档

- [Nebula 框架使用指南](../../docs/Nebula框架使用指南.md)
- [多场景 Starter 方案](../../docs/Nebula-Starter优化建议-多场景Starter方案.md)
- [其他 Starter 说明](../README.md)

