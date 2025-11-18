# Nebula Framework

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.12-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Nebula 是一个现代化的 Java 后端框架，基于 Spring Boot 3.x 和 Java 21 构建，提供企业级应用开发的完整解决方案

##  特性

- **现代化技术栈**: Java 21 + Spring Boot 3.x + Maven
- **模块化架构**: 基于 DDD 原则的清晰模块划分
- **安全组件**: JWT 认证与 RBAC 支持
- **数据访问**: 统一的数据访问抽象层，支持多种存储后端
- **持久化层**: MyBatis-Plus集成，支持读写分离和分库分表
- **文档数据库**: MongoDB完整支持，包含地理查询和聚合
- **多级缓存**: 本地+分布式缓存，防穿透/雪崩保护
- **消息传递**: 统一的消息处理抽象
- **Web 支持**: 完整的 Web 开发支持
- **任务调度**: 灵活的任务调度系统
- **配置管理**: 类型安全的配置属性

## ️ 架构设计

```
Nebula Framework
核心层 (Core Layer)
   nebula-foundation       # 基础工具和异常处理
   nebula-security         # 安全配置与JWT/RBAC
基础设施层 (Infrastructure Layer)
   数据访问 (Data Access)
      nebula-data-persistence # MyBatis-Plus 集成
      nebula-data-mongodb     # MongoDB 支持
      nebula-data-cache       # 多级缓存
    消息传递 (Messaging)
       nebula-messaging-core   # 消息传递核心
       nebula-messaging-rabbitmq  # RabbitMQ 实现
   RPC 通信 (RPC)
       nebula-rpc-core         # RPC 抽象
       nebula-rpc-http         # HTTP RPC 实现
       nebula-rpc-grpc         # gRPC RPC 实现
    服务发现 (Discovery)
       nebula-discovery-core   # 服务发现核心
       nebula-discovery-nacos  # Nacos 实现
    存储服务 (Storage)
       nebula-storage-core     # 存储抽象
       nebula-storage-minio    # MinIO 实现
       nebula-storage-aliyun-oss # 阿里云OSS实现
    搜索服务 (Search)
       nebula-search-core      # 搜索抽象
       nebula-search-elasticsearch # Elasticsearch实现
    AI 服务 (AI)
        nebula-ai-core          # AI 核心
        nebula-ai-spring        # Spring AI 集成
    分布式锁 (Lock)
        nebula-lock-core        # 锁抽象
        nebula-lock-redis       # Redis 分布式锁实现
应用层 (Application Layer)
    nebula-web                 # Web 框架
    nebula-task                # 任务调度
自动配置层 (Auto-Configuration)
    nebula-autoconfigure       # 统一自动配置模块
集成层 (Integration Layer)
    nebula-integration-payment # 支付集成
    nebula-integration-notification # 通知集成
Starter 模块 (Starter Modules)
     nebula-starter-minimal     # 最小化 Starter（仅核心功能）
     nebula-starter-web         # Web 应用 Starter
     nebula-starter-service     # 微服务 Starter
     nebula-starter-ai          # AI 应用 Starter
     nebula-starter-all         # 完整 Starter（单体应用）
     nebula-starter-api         # API 契约模块 Starter
```

##  快速开始

### 1. 环境要求

- Java 21 或更高版本
- Maven 3.6+ 
- Spring Boot 3.x

### 2. 选择合适的 Starter

Nebula 提供多种 Starter 以满足不同场景需求：

#### 🚀 nebula-starter-minimal（最小化）
**适用场景**: 工具类、库项目、需要精细控制依赖的项目
**包含模块**: `nebula-foundation`（基础工具）

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-minimal</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```
#### 示例配置（minimal）
```yaml
spring:
  application:
    name: nebula-minimal-app
logging:
  level:
    io.nebula: INFO
```

#### 🌐 nebula-starter-web（Web应用）
**适用场景**: 传统 Web 应用、API 服务、管理后台
**包含模块**: Foundation + Web + Security + Data(Cache/Persistence) + RPC

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```
#### 示例配置（web）
```yaml
nebula:
  web:
    performance:
      enabled: true
  security:
    jwt:
      secret: your-secret-key
      expiration: 86400
  data:
    persistence:
      enabled: true
  rpc:
    http:
      enabled: true
      client:
        enabled: true
        base-url: http://localhost:8081
      server:
        enabled: true
        port: 8081
```

#### ☁️ nebula-starter-service（微服务）
**适用场景**: 微服务架构、分布式系统、云原生应用
**包含模块**: Foundation + Web + Discovery + RPC + Data + Messaging + Lock

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```
#### 示例配置（service）
```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: 127.0.0.1:8848
      namespace: public
      group-name: DEFAULT_GROUP
      auto-register: true
  rpc:
    http:
      enabled: true
      client:
        enabled: true
      server:
        enabled: true
        port: 8081
    grpc:
      enabled: false
      client:
        enabled: false
      server:
        enabled: false
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      username: guest
      password: guest
  lock:
    enabled: true
    enable-aspect: true
  data:
    cache:
      enabled: true
```

#### 🤖 nebula-starter-ai（AI应用）
**适用场景**: AI/ML 应用、RAG 应用、智能对话系统
**包含模块**: Foundation + Web + AI(Spring AI) + Cache

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```
#### 示例配置（ai）
```yaml
nebula:
  ai:
    enabled: true
    openai:
      api-key: sk-xxxx
      base-url: https://api.openai.com
      chat:
        enabled: true
      embedding:
        enabled: true
    vector-store:
      chroma:
        url: http://localhost:8000
        collection-name: nebula_vectors
        initialize-schema: true
```

#### 📦 nebula-starter-all（单体应用）
**适用场景**: 单体应用、原型开发、快速启动
**包含模块**: 几乎所有 Nebula 模块

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-all</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```
#### 示例配置（all）
```yaml
nebula:
  rpc:
    http:
      enabled: true
      server:
        enabled: true
        port: 8081
  discovery:
    nacos:
      enabled: true
      server-addr: 127.0.0.1:8848
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
  lock:
    enabled: true
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
  storage:
    minio:
      enabled: false
```

#### 📋 nebula-starter-api（API契约）
**适用场景**: API 定义模块、共享接口、RPC 契约
**包含模块**: RPC Core + Spring Web (provided) + Validation + Lombok (provided)

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-api</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```
该 Starter 用于 API 契约定义，无需运行时配置。

### 3. 创建应用

```java
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 4. 创建控制器

```java
@RestController
@RequestMapping("/api")
public class YourController extends BaseController {
    
    @Override
    protected Long getCurrentUserId() {
        // 实现获取当前用户ID的逻辑
        return 1L;
    }
    
    @Override
    protected String getCurrentUsername() {
        // 实现获取当前用户名的逻辑
        return "user";
    }
    
    @GetMapping("/hello")
    public Result<String> hello() {
        return success("Hello, Nebula!");
    }
}
```

### 5. 配置应用

```yaml
# application.yml
nebula:
  web:
    performance:
      enabled: true
  security:
    jwt:
      secret: your-secret-key
      expiration: 86400
  data:
    persistence:
      enabled: true
      primary: primary
      sources:
        primary:
          type: h2
          driver-class-name: org.h2.Driver
          url: jdbc:h2:mem:testdb
          username: sa
          password: ""
```

##  模块说明

### 核心模块

#### nebula-foundation
提供基础功能和异常处理：
- 统一异常处理体系
- 常用工具类和工具方法
- 基础配置支持
- 通用工具函数

#### nebula-security
安全认证与授权支持：
- JWT 身份认证
- RBAC 角色/权限控制
- 注解驱动的权限校验

### 数据访问模块

#### nebula-data-persistence
关系型数据库完整解决方案：
- MyBatis-Plus深度集成
- 智能读写分离（主从路由/负载均衡）
- ShardingSphere分库分表支持
- 声明式和编程式事务
- 代码生成器和性能监控

#### nebula-data-mongodb
MongoDB文档数据库支持：
- 完整的CRUD操作
- 地理位置查询和索引
- 聚合管道查询
- 嵌入文档和数组操作
- 事务支持和性能优化

#### nebula-data-cache
企业级多级缓存：
- 本地缓存（Caffeine）+ 分布式缓存（Redis）
- Cache-Aside/Write-Through/Write-Back模式
- 缓存穿透/击穿/雪崩防护
- 注解驱动的缓存管理
- 缓存统计和监控

### 应用模块

#### nebula-web
Web 框架支持：
- 控制器基类和工具类
- 全局异常处理
- 参数验证和转换
- 认证和授权支持
- 性能监控和限流

#### nebula-task
任务调度框架：
- 定时任务管理
- 任务执行器抽象
- 执行结果跟踪
- 分布式任务协调

### 基础设施模块

#### 消息传递 (Messaging)
- **nebula-messaging-core**: 统一的消息处理抽象和核心接口
- **nebula-messaging-rabbitmq**: RabbitMQ 消息队列实现

#### RPC 通信 (RPC)
- **nebula-rpc-core**: RPC 调用抽象和协议定义
- **nebula-rpc-http**: 基于 HTTP 的 RPC 实现
- **nebula-rpc-grpc**: 基于 gRPC 的 RPC 实现

#### 服务发现 (Discovery)
- **nebula-discovery-core**: 服务发现核心抽象和负载均衡
- **nebula-discovery-nacos**: Nacos 服务注册与发现实现

#### 存储服务 (Storage)
- **nebula-storage-core**: 统一的对象存储抽象接口
- **nebula-storage-minio**: MinIO 对象存储实现
- **nebula-storage-aliyun-oss**: 阿里云 OSS 对象存储实现

#### 搜索服务 (Search)
- **nebula-search-core**: 统一的搜索服务抽象
- **nebula-search-elasticsearch**: Elasticsearch 搜索引擎实现

#### AI 服务 (AI)
- **nebula-ai-core**: AI 服务核心抽象和工具
- **nebula-ai-spring**: Spring AI 集成和自动化配置

### 集成模块

#### nebula-integration-payment
支付集成模块：
- 统一支付接口抽象
- 多支付渠道支持
- 支付结果回调处理
- 交易状态管理

#### nebula-integration-notification
通知集成模块：
- 统一通知接口抽象
- 短信/邮件通知支持
- 模板化消息发送
- 发送状态跟踪

## ️ 开发指南

### 构建项目

```bash
# 编译项目
mvn clean compile

# 安装所有模块到本地仓库（首次运行必需）
mvn install -DskipTests
```

### 快速验证框架功能

```bash
# 构建并运行 Starter 示例（推荐）
mvn -q -DskipTests -f examples/starter-minimal-example/pom.xml package
mvn -q -DskipTests -f examples/starter-web-example/pom.xml package
mvn -q -DskipTests -f examples/starter-service-example/pom.xml package
mvn -q -DskipTests -f examples/starter-ai-example/pom.xml package
mvn -q -DskipTests -f examples/starter-all-example/pom.xml package
mvn -q -DskipTests -f examples/starter-api-example/pom.xml package
```

### 运行应用

```bash
# 1. 安装核心与 Starter 模块到本地仓库
mvn -q -DskipTests install -pl core/nebula-foundation,starter/nebula-starter-minimal,starter/nebula-starter-web,starter/nebula-starter-service,starter/nebula-starter-ai,starter/nebula-starter-all,starter/nebula-starter-api -am

# 2. 在你的业务应用中引入合适的 Starter 并运行
mvn spring-boot:run
```

### 运行示例应用

```bash
# Web 示例（端口 8080）
mvn -q -f examples/starter-web-example spring-boot:run
curl http://localhost:8080/hello

# Service 示例（端口 8082）
mvn -q -f examples/starter-service-example spring-boot:run
curl http://localhost:8082/hello

# AI 示例（端口 8083）
# 先在 examples/starter-ai-example/src/main/resources/application.yml 中设置：
# nebula.ai.enabled=true 且配置 openai.api-key
mvn -q -f examples/starter-ai-example spring-boot:run
curl "http://localhost:8083/ai/echo?q=hello"

# All 示例（端口 8084）
mvn -q -f examples/starter-all-example spring-boot:run
curl http://localhost:8084/hello

# Minimal 示例：无 Web 端点，仅验证最小化启动
mvn -q -f examples/starter-minimal-example spring-boot:run
```

### 验证应用接口

应用启动成功后，可以访问以下接口：
```bash
# 健康检查
curl http://localhost:8080/health

# Hello接口
curl http://localhost:8080/api/hello

# 性能监控（需要启用性能监控配置）
curl http://localhost:8080/performance/status
curl http://localhost:8080/performance/metrics
curl http://localhost:8080/performance/system
```

### 配置说明

#### 环境化配置（推荐）
使用标准的 Spring Boot 环境配置文件：
- `application.yml`: 通用配置
- `application-dev.yml`: 开发环境
- `application-test.yml`: 测试环境
- `application-prod.yml`: 生产环境

示例默认配置与自动配置入口参考：
`autoconfigure/nebula-autoconfigure/src/main/resources/application.yml`
`autoconfigure/nebula-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### 故障排除

如果应用启动失败，请检查：
1. Java 版本是否为 21+
2. Maven 依赖是否正确安装：`mvn install -DskipTests`
3. 端口 8080 是否被占用：`netstat -an | grep :8080`
4. 对照自动配置入口与默认配置排查：
   `autoconfigure/nebula-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   `autoconfigure/nebula-autoconfigure/src/main/resources/application.yml`

### 运行测试

```bash
mvn test
```

##  监控

框架内置了完整的监控体系：

1. **性能监控**: 自动收集HTTP请求性能指标，包括响应时间成功率失败率等
2. **系统监控**: 实时监控CPU内存线程等系统资源使用情况
3. **健康检查**: 提供 `/health`, `/health/status`, `/health/ping`, `/health/liveness`, `/health/readiness` 等端点
4. **性能端点**: 提供 `/performance/metrics`, `/performance/system`, `/performance/status`, `/performance/reset` 等接口

##  配置

### 基础配置

```yaml
nebula:
  web:
    performance:
      enabled: true
  
  # 数据源配置（持久化）
  data:
    persistence:
      enabled: true
      sources:
        primary:
          url: ${DB_URL:jdbc:h2:mem:nebula}
          username: ${DB_USERNAME:sa}
          password: ${DB_PASSWORD:}
```

### 高级配置

```yaml
nebula:
  # 数据访问配置
  data:
    # 数据源配置
    sources:
      primary:
        type: mysql
        url: jdbc:mysql://localhost:3306/nebula_db
        username: root
        password: password
        pool:
          min-size: 5
          max-size: 20
    
    # 缓存配置
    cache:
      enabled: true
      type: multi-level  # local, redis, multi-level
      local:
        max-size: 10000
        expire-after-write: 300s
      redis:
        enabled: true
        key-prefix: "nebula:cache:"
    
    # 读写分离配置
    read-write-separation:
      enabled: true
      clusters:
        default:
          master: primary
          slaves: [slave1, slave2]
          load-balance-strategy: ROUND_ROBIN
    
    # 分库分表配置
    sharding:
      enabled: true
      schemas:
        default:
          data-sources: [ds0, ds1]
          tables:
            - logic-table: t_user
              actual-data-nodes: ds${0..1}.t_user_${0..1}
    
    # MongoDB配置
    mongodb:
      enabled: true
      database: nebula_mongo
      
  # 安全配置
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      expiration: 86400
  
  # 消息配置
  messaging:
    provider: rabbitmq
    rabbitmq:
      host: ${RABBITMQ_HOST:localhost}
      port: ${RABBITMQ_PORT:5672}
```

##  贡献

我们欢迎所有形式的贡献！请查看 [贡献指南](CONTRIBUTING.md) 了解详情

##  许可证

本项目采用 Apache License 2.0 许可证详情请查看 [LICENSE](LICENSE) 文件

##  相关链接

- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Java 21 文档](https://openjdk.java.net/projects/jdk/21/)
- [Maven 指南](https://maven.apache.org/guides/)

---

**Nebula Framework** - 构建现代化 Java 应用的最佳选择！
