# Nebula Framework

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.12-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Nebula 是一个现代化的 Java 后端框架，基于 Spring Boot 3.x 和 Java 21 构建，提供企业级应用开发的完整解决方案。

## ✨ 特性

- **现代化技术栈**: Java 21 + Spring Boot 3.x + Maven
- **模块化架构**: 基于 DDD 原则的清晰模块划分
- **安全组件**: 加密工具、JWT 支持
- **数据访问**: 统一的数据访问抽象层，支持多种存储后端
- **持久化层**: MyBatis-Plus集成，支持读写分离和分库分表
- **文档数据库**: MongoDB完整支持，包含地理查询和聚合
- **多级缓存**: 本地+分布式缓存，防穿透/雪崩保护
- **消息传递**: 统一的消息处理抽象
- **Web 支持**: 完整的 Web 开发支持
- **任务调度**: 灵活的任务调度系统
- **配置管理**: 类型安全的配置属性

## 🏛️ 架构设计

```
Nebula Framework
├── 核心层 (Core Layer)
│   └── nebula-foundation       # 基础工具和异常处理
├── 基础设施层 (Infrastructure Layer)
│   ├── 数据访问 (Data Access)
│   │   ├── nebula-data-access      # 数据访问抽象层
│   │   ├── nebula-data-persistence # MyBatis-Plus 集成
│   │   ├── nebula-data-mongodb     # MongoDB 支持
│   │   └── nebula-data-cache       # 多级缓存
│   ├── 消息传递 (Messaging)
│   │   ├── nebula-messaging-core   # 消息传递核心
│   │   └── nebula-messaging-rabbitmq  # RabbitMQ 实现
│   ├── RPC 通信 (RPC)
│   │   ├── nebula-rpc-core         # RPC 抽象
│   │   └── nebula-rpc-http         # HTTP RPC 实现
│   ├── 服务发现 (Discovery)
│   │   ├── nebula-discovery-core   # 服务发现核心
│   │   └── nebula-discovery-nacos  # Nacos 实现
│   ├── 存储服务 (Storage)
│   │   ├── nebula-storage-core     # 存储抽象
│   │   ├── nebula-storage-minio    # MinIO 实现
│   │   └── nebula-storage-aliyun-oss # 阿里云OSS实现
│   ├── 搜索服务 (Search)
│   │   ├── nebula-search-core      # 搜索抽象
│   │   └── nebula-search-elasticsearch # Elasticsearch实现
│   └── AI 服务 (AI)
│       ├── nebula-ai-core          # AI 核心
│       └── nebula-ai-spring        # Spring AI 集成
├── 应用层 (Application Layer)
│   ├── nebula-web                 # Web 框架
│   └── nebula-task                # 任务调度
├── 集成层 (Integration Layer)
│   └── nebula-integration-payment # 支付集成
└── Starter 模块 (Starter Modules)
    ├── nebula-starter             # Spring Boot Starter
    └── nebula-example             # 使用示例
```

## 🚀 快速开始

### 1. 环境要求

- Java 21 或更高版本
- Maven 3.6+ 
- Spring Boot 3.x

### 2. 添加依赖

在您的 `pom.xml` 中添加 Nebula Starter:

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

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
    @Monitored(name = "hello.api", description = "Hello API")
    public Result<String> hello() {
        return success("Hello, Nebula!");
    }
}
```

### 5. 配置应用

```yaml
# application.yml
nebula:
  metrics:
    enabled: true
  datasources:
    primary:
      url: jdbc:h2:mem:testdb
      username: sa
      password: ""
```

## 📖 模块说明

### 核心模块

#### nebula-foundation
提供基础功能和异常处理：
- 统一异常处理体系
- 常用工具类和工具方法
- 基础配置支持
- 通用工具函数

### 数据访问模块

#### nebula-data-access
统一数据访问抽象层：
- 通用Repository接口和实现
- 链式QueryBuilder查询构建器
- 统一事务管理接口
- 完善的异常处理体系

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

## 🛠️ 开发指南

### 构建项目

```bash
# 编译项目
mvn clean compile

# 安装所有模块到本地仓库（首次运行必需）
mvn install -DskipTests
```

### 快速验证框架功能

```bash
# 编译基础测试程序
javac -cp "$(find ~/.m2 -name 'nebula-foundation-*.jar' | head -1)" TestApp.java

# 运行基础功能测试
java -cp ".:$(find ~/.m2 -name 'nebula-foundation-*.jar' | head -1)" TestApp
```

### 运行完整示例应用

```bash
# 1. 首先确保所有模块已安装到本地Maven仓库
mvn install -DskipTests

# 2. 运行示例应用（使用简化配置）
cd nebula-example
mvn spring-boot:run -Dspring-boot.run.profiles=simple
```

### 验证应用接口

应用启动成功后，可以访问以下接口：
```bash
# 系统信息
curl http://localhost:8080/api/info

# 健康检查
curl http://localhost:8080/api/health

# Hello接口
curl http://localhost:8080/api/hello

# 用户管理
curl http://localhost:8080/api/users

# 测试接口
curl http://localhost:8080/api/test/success

# 性能监控（需要启用性能监控配置）
curl http://localhost:8080/performance/status
curl http://localhost:8080/performance/metrics
```

### 配置说明

#### 简化配置（推荐用于快速开始）
使用 `application-simple.yml` 配置：
- **数据库**: H2 内存数据库（无需安装）
- **缓存**: 内存缓存（无需Redis）
- **端口**: 8080

#### 完整配置
使用 `application.yml` 配置：
- **数据库**: MySQL（需要单独安装和配置）
- **缓存**: Redis（需要单独安装和配置）
- **消息队列**: RabbitMQ（可选）
- **服务发现**: Nacos（可选）
- **对象存储**: MinIO/阿里云OSS（可选）
- **搜索引擎**: Elasticsearch（可选）
- **AI服务**: Spring AI集成（可选）

#### 其他配置选项
- `application-minimal.yml`: 最小化配置，仅包含基础功能
- `application-docker.yml`: Docker容器化部署配置
- `application-xxljob-optimized.yml`: XXL-Job任务调度优化配置

### 故障排除

如果应用启动失败，请检查：
1. Java 版本是否为 21+
2. Maven 依赖是否正确安装：`mvn install -DskipTests`
3. 端口 8080 是否被占用：`netstat -an | grep :8080`
4. 使用简化配置启动：`-Dspring-boot.run.profiles=simple`

### 运行测试

```bash
mvn test
```

## 📊 监控

框架内置了完整的监控体系：

1. **性能监控**: 自动收集HTTP请求性能指标，包括响应时间、成功率、失败率等
2. **系统监控**: 实时监控CPU、内存、线程等系统资源使用情况
3. **健康检查**: 集成 Spring Boot Actuator 健康端点
4. **性能端点**: 提供 `/performance/metrics`, `/performance/system`, `/performance/status` 等监控接口

## 🔧 配置

### 基础配置

```yaml
nebula:
  # 启用监控
  metrics:
    enabled: true
  
  # 数据源配置
  datasources:
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

## 🤝 贡献

我们欢迎所有形式的贡献！请查看 [贡献指南](CONTRIBUTING.md) 了解详情。

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详情请查看 [LICENSE](LICENSE) 文件。

## 🔗 相关链接

- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Java 21 文档](https://openjdk.java.net/projects/jdk/21/)
- [Maven 指南](https://maven.apache.org/guides/)

---

**Nebula Framework** - 构建现代化 Java 应用的最佳选择！
