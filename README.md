# Nebula Framework

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Nebula 是一个现代化的 Java 后端框架，基于 Spring Boot 3.x 和 Java 21 构建，提供企业级应用开发的完整解决方案。

## ✨ 特性

- 🚀 **现代化技术栈**: Java 21 + Spring Boot 3.x + Maven
- 🏗️ **模块化架构**: 基于 DDD 原则的清晰模块划分
- 📊 **监控与指标**: 内置性能监控和指标收集
- 🔒 **安全组件**: 加密工具、JWT 支持
- 💾 **数据访问**: 支持关系型和 NoSQL 数据库
- 📨 **消息传递**: 统一的消息处理抽象
- 🌐 **Web 支持**: 完整的 Web 开发支持
- ⚙️ **任务调度**: 灵活的任务调度系统
- 📦 **批处理**: Spring Batch 集成
- 🔧 **配置管理**: 类型安全的配置属性

## 🏛️ 架构设计

```
Nebula Framework
├── 核心层 (Core Layer)
│   ├── nebula-core-common      # 通用工具和异常处理
│   ├── nebula-core-config      # 配置管理
│   ├── nebula-core-metrics     # 监控指标
│   └── nebula-core-security    # 安全组件
├── 基础设施层 (Infrastructure Layer)
│   ├── nebula-data-access      # 数据访问抽象
│   ├── nebula-data-persistence # 关系型数据库支持
│   ├── nebula-data-nosql       # NoSQL 数据库支持
│   ├── nebula-data-cache       # 缓存支持
│   ├── nebula-messaging-core   # 消息传递核心
│   ├── nebula-rpc-core         # RPC 抽象
│   ├── nebula-rpc-http         # HTTP RPC 实现
│   └── nebula-discovery-nacos  # 服务发现
├── 应用层 (Application Layer)
│   ├── nebula-web              # Web 框架
│   ├── nebula-scheduling       # 任务调度
│   └── nebula-batch            # 批处理
└── 集成层 (Integration Layer)
    ├── nebula-starter          # Spring Boot Starter
    └── nebula-example          # 使用示例
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

#### nebula-core-common
提供基础的通用功能：
- 统一异常处理体系
- 标准化响应格式
- 常用工具类

#### nebula-core-config
配置管理功能：
- 类型安全的配置属性
- 配置验证
- 环境特定配置

#### nebula-core-metrics
监控指标功能：
- 性能监控
- 指标收集
- 自动监控切面

#### nebula-core-security
安全组件：
- 加密工具
- JWT 支持
- 密码哈希

### 数据访问模块

#### nebula-data-access
数据访问抽象层：
- 通用仓储接口
- 查询构建器
- 事务管理

#### nebula-data-persistence
关系型数据库支持：
- MyBatis-Plus 集成
- 分页插件
- 乐观锁支持

#### nebula-data-nosql
NoSQL 数据库支持：
- MongoDB 集成
- Redis 支持
- 文档操作模板

#### nebula-data-cache
缓存支持：
- 多级缓存
- 缓存策略
- 自动过期

### 应用模块

#### nebula-web
Web 框架支持：
- 控制器基类
- 全局异常处理
- 参数验证

#### nebula-scheduling
任务调度：
- 定时任务
- 任务执行器
- 执行结果跟踪

#### nebula-batch
批处理支持：
- Spring Batch 集成
- 作业管理
- 批量数据处理

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
javac -cp "$(find ~/.m2 -name 'nebula-core-common-*.jar' | head -1)" TestApp.java

# 运行基础功能测试
java -cp ".:$(find ~/.m2 -name 'nebula-core-common-*.jar' | head -1)" TestApp
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
curl http://localhost:8080/api/example/info

# 健康检查
curl http://localhost:8080/api/example/health

# 用户管理
curl http://localhost:8080/api/users
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

1. **性能监控**: 使用 `@Monitored` 注解自动收集方法执行时间
2. **指标收集**: 支持计数器、定时器、仪表盘等指标类型
3. **健康检查**: 集成 Spring Boot Actuator

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
  # 安全配置
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      expiration: 86400
  
  # 缓存配置
  cache:
    type: redis
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  
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
