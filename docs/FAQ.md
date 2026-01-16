# Nebula 框架常见问题 (FAQ)

> 本文档收集了使用 Nebula 框架时的常见问题和解决方案

## 目录

- [入门问题](#入门问题)
- [Starter 选择](#starter-选择)
- [配置问题](#配置问题)
- [数据访问](#数据访问)
- [缓存问题](#缓存问题)
- [消息队列](#消息队列)
- [RPC 通信](#rpc-通信)
- [服务发现](#服务发现)
- [安全认证](#安全认证)
- [性能优化](#性能优化)
- [错误处理](#错误处理)
- [部署运维](#部署运维)

## 入门问题

### Q1: Nebula 框架适合什么场景？

**A**: Nebula 框架适合以下场景：

✅ **适合**：
- 微服务架构
- 中大型企业应用
- 高并发系统
- 需要快速开发的项目
- 票务、电商、社交等业务系统

❌ **不太适合**：
- 简单的单体应用（可以用，但可能过重）
- 超小型项目（几个接口）
- 对框架有深度定制需求的项目

### Q2: Nebula 与 Spring Boot 的关系？

**A**: Nebula 是基于 Spring Boot 3.x 构建的企业级框架：

```
Spring Boot 3.x（基础）
    ↓
Nebula Framework（增强）
    ↓
您的业务应用
```

**Nebula 提供**：
- 开箱即用的常用功能
- 统一的配置和约定
- 企业级最佳实践
- 完整的技术栈集成

**您仍然可以**：
- 使用所有 Spring Boot 特性
- 添加其他 Spring 组件
- 自定义配置

### Q3: 需要什么基础才能使用 Nebula？

**A**: 建议掌握以下技术：

**必需**：
- Java 基础（Java 17+）
- Spring Boot 基础
- Maven/Gradle 基础

**推荐**：
- Spring Cloud 微服务
- MyBatis/MyBatis-Plus
- Redis 缓存
- RabbitMQ 消息队列

**文档资源**：
- [快速开始](framework/QUICK_START.md)
- [示例项目](../example/README.md)

## Starter 选择

### Q4: 应该选择哪个 Starter？

**A**: 根据项目类型选择：

| 项目类型 | 推荐 Starter | 说明 |
|---------|-------------|------|
| API 契约 | `nebula-starter-api` | 只定义接口，无实现 |
| 单体应用 | `nebula-starter-all` | 包含所有功能 |
| Web 应用 | `nebula-starter-web` | 适合前后端分离 |
| 微服务 | `nebula-starter-service` | 微服务间通信 |
| AI 应用 | `nebula-starter-ai` | 集成 AI 能力 |
| 极简应用 | `nebula-starter-minimal` | 最小依赖集 |

**详细指南**：[Starter 选择指南](STARTER_SELECTION_GUIDE.md)

### Q5: 可以同时使用多个 Starter 吗？

**A**: 不建议同时使用多个 Starter，因为它们之间可能存在依赖冲突。

**正确做法**：
```xml
<!-- 只选择一个 Starter -->
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>
</dependency>
```

**如需额外功能**：
```xml
<!-- 添加单独的功能模块 -->
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-search-elasticsearch</artifactId>
</dependency>
```

## 配置问题

### Q6: 配置文件怎么写？

**A**: Nebula 统一使用 `nebula` 前缀：

```yaml
# application.yml
nebula:
  # 数据访问
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
      type: redis
  
  # 安全认证
  security:
    jwt:
      enabled: true
      secret: your-secret-key
  
  # RPC 通信
  rpc:
    grpc:
      enabled: true
      port: 9090
```

**配置优先级**：
```
1. 命令行参数 (--nebula.xxx=yyy)
2. application-{profile}.yml
3. application.yml
4. 默认配置
```

**详细配置**：查看各模块的 `CONFIG.md`

### Q7: 如何区分开发和生产环境配置？

**A**: 使用 Spring Profile：

```yaml
# application.yml（通用配置）
spring:
  application:
    name: my-app

nebula:
  data:
    persistence:
      enabled: true

---
# application-dev.yml（开发环境）
nebula:
  data:
    cache:
      type: caffeine  # 本地缓存

---
# application-prod.yml（生产环境）
nebula:
  data:
    cache:
      type: redis     # Redis 缓存
  security:
    jwt:
      secret: ${JWT_SECRET}  # 从环境变量读取
```

**启动时指定**：
```bash
# 开发环境
java -jar app.jar --spring.profiles.active=dev

# 生产环境
java -jar app.jar --spring.profiles.active=prod
```

## 数据访问

### Q8: 如何配置多数据源？

**A**: Nebula 支持多数据源配置：

```yaml
nebula:
  data:
    persistence:
      datasources:
        master:
          url: jdbc:mysql://localhost:3306/main
          username: root
          password: password
        slave:
          url: jdbc:mysql://localhost:3307/main
          username: readonly
          password: password
```

**代码使用**：
```java
@Service
public class UserService {
    
    @DS("master")  // 指定数据源
    public void createUser(User user) {
        userMapper.insert(user);
    }
    
    @DS("slave")
    public List<User> listUsers() {
        return userMapper.selectList(null);
    }
}
```

### Q9: 如何处理分页查询？

**A**: MyBatis-Plus 内置分页支持：

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public IPage<User> getUsers(int page, int size) {
        Page<User> pageParam = new Page<>(page, size);
        return userMapper.selectPage(pageParam, null);
    }
}
```

**自定义查询分页**：
```java
public IPage<UserVO> searchUsers(SearchDTO dto) {
    Page<User> page = new Page<>(dto.getPage(), dto.getSize());
    
    QueryWrapper<User> query = new QueryWrapper<>();
    query.like("username", dto.getKeyword());
    
    IPage<User> result = userMapper.selectPage(page, query);
    
    // 转换为 VO
    return result.convert(this::toVO);
}
```

## 缓存问题

### Q10: 缓存不生效怎么办？

**A**: 检查以下几点：

**1. 配置是否启用**：
```yaml
nebula:
  data:
    cache:
      enabled: true  # 确保启用
      type: redis
```

**2. 是否添加注解**：
```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
}
```

**3. 方法是否被代理**：
```java
// ❌ 错误：同类调用不会触发缓存
public class UserService {
    public User getUser(Long id) {
        return this.getById(id);  // 不会缓存
    }
    
    @Cacheable(value = "users", key = "#id")
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
}

// ✅ 正确：通过注入调用
@Service
public class UserController {
    @Autowired
    private UserService userService;
    
    public User getUser(Long id) {
        return userService.getById(id);  // 会缓存
    }
}
```

**4. Redis 连接是否正常**：
```bash
# 检查 Redis 连接
redis-cli ping
# 应返回: PONG
```

### Q11: 如何清空缓存？

**A**: 使用 `@CacheEvict` 注解：

```java
@Service
public class UserService {
    
    // 更新时清空单个缓存
    @CacheEvict(value = "users", key = "#user.id")
    public void updateUser(User user) {
        userMapper.updateById(user);
    }
    
    // 清空所有用户缓存
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllCache() {
        // 清空逻辑
    }
}
```

## 消息队列

### Q12: 如何发送和接收消息？

**A**: 使用 Nebula 消息抽象：

**发送消息**：
```java
@Service
public class OrderService {
    
    @Autowired
    private MessagePublisher messagePublisher;
    
    public void createOrder(Order order) {
        // 保存订单
        orderMapper.insert(order);
        
        // 发送消息
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        messagePublisher.publish("order.created", event);
    }
}
```

**接收消息**：
```java
@Component
public class OrderEventListener {
    
    @MessageHandler(topic = "order.created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("订单创建: {}", event.getOrderId());
        // 处理业务逻辑
    }
}
```

### Q13: 消息消费失败怎么办？

**A**: Nebula 提供多种失败处理策略：

**1. 自动重试**：
```yaml
nebula:
  messaging:
    rabbitmq:
      consumer:
        max-retries: 3
        retry-interval: 5000  # 5秒
```

**2. 死信队列**：
```java
@MessageHandler(
    topic = "order.created",
    deadLetterTopic = "order.created.dlq"
)
public void handleOrderCreated(OrderCreatedEvent event) {
    // 处理逻辑
}
```

**3. 手动确认**：
```java
@MessageHandler(
    topic = "order.created",
    ackMode = AckMode.MANUAL
)
public void handleOrderCreated(OrderCreatedEvent event, MessageContext context) {
    try {
        // 处理逻辑
        context.ack();  // 手动确认
    } catch (Exception e) {
        context.nack();  // 拒绝消息
    }
}
```

## RPC 通信

### Q14: 如何调用远程服务？

**A**: Nebula 支持 HTTP 和 gRPC 两种方式：

**HTTP RPC**：
```java
@Service
public class OrderService {
    
    @Autowired
    @RpcClient(service = "user-service")
    private UserApi userApi;
    
    public Order createOrder(CreateOrderDTO dto) {
        // 调用远程用户服务
        User user = userApi.getById(dto.getUserId());
        
        // 创建订单逻辑
        Order order = new Order();
        order.setUserId(user.getId());
        return order;
    }
}
```

**gRPC**：
```java
@Service
public class OrderService {
    
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;
    
    public Order createOrder(CreateOrderDTO dto) {
        // gRPC 调用
        UserResponse response = userStub.getUser(
            UserRequest.newBuilder()
                .setId(dto.getUserId())
                .build()
        );
        
        // 创建订单逻辑
        return new Order();
    }
}
```

### Q15: RPC 调用超时怎么办？

**A**: 配置超时时间和重试策略：

```yaml
nebula:
  rpc:
    http:
      connect-timeout: 5000    # 连接超时 5秒
      read-timeout: 10000      # 读取超时 10秒
      max-retries: 3           # 最多重试 3 次
    grpc:
      deadline: 10             # gRPC 超时 10秒
```

**针对特定接口**：
```java
@RpcClient(
    service = "user-service",
    timeout = 30000  // 30秒超时
)
private UserApi userApi;
```

## 服务发现

### Q16: 服务注册失败怎么办？

**A**: 检查 Nacos 配置：

**1. Nacos 是否启动**：
```bash
# 检查 Nacos 是否运行
curl http://localhost:8848/nacos/
```

**2. 配置是否正确**：
```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: dev          # 命名空间
      group: DEFAULT_GROUP    # 分组
```

**3. 网络是否连通**：
```bash
# 测试网络连接
telnet localhost 8848
```

**4. 查看日志**：
```bash
# 应用日志
tail -f logs/app.log

# Nacos 日志
tail -f nacos/logs/naming-server.log
```

## 安全认证

### Q17: 如何实现 JWT 认证？

**A**: Nebula 内置 JWT 支持：

**1. 配置**：
```yaml
nebula:
  security:
    jwt:
      enabled: true
      secret: your-very-secure-secret-key-at-least-256-bits
      expiration: 86400  # 24小时
```

**2. 登录生成 Token**：
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        // 验证用户名密码
        User user = authService.authenticate(dto);
        
        // 生成 Token
        String token = tokenProvider.createToken(user.getId(), user.getRoles());
        
        return Result.success(new LoginVO(token));
    }
}
```

**3. 验证 Token**：
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/me")
    @RequiresAuthentication  // 需要认证
    public Result<UserVO> getCurrentUser() {
        Long userId = SecurityContext.getUserId();
        User user = userService.getById(userId);
        return Result.success(toVO(user));
    }
}
```

### Q18: 如何实现 RBAC 权限控制？

**A**: 使用 `@RequiresPermission` 注解：

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping
    @RequiresPermission("order:view")
    public Result<List<OrderVO>> listOrders() {
        // 查询订单
    }
    
    @PostMapping
    @RequiresPermission("order:create")
    public Result<OrderVO> createOrder(@RequestBody CreateOrderDTO dto) {
        // 创建订单
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("order:delete")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        // 删除订单
    }
}
```

**权限数据模型**：
```
User (用户)
  ↓ N:N
Role (角色)
  ↓ N:N
Permission (权限)
  ↓
Resource (资源)
```

## 性能优化

### Q19: 如何提升系统性能？

**A**: 多方面优化：

**1. 使用多级缓存**：
```yaml
nebula:
  data:
    cache:
      type: multi-level
      l1:
        type: caffeine
        max-size: 1000
      l2:
        type: redis
        ttl: 3600
```

**2. 启用数据库连接池**：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

**3. 使用异步处理**：
```java
@Service
public class NotificationService {
    
    @Async
    public void sendEmail(String to, String content) {
        // 异步发送邮件
    }
}
```

**4. 批量操作**：
```java
// ❌ 逐条插入
for (User user : users) {
    userMapper.insert(user);
}

// ✅ 批量插入
userService.saveBatch(users);
```

**5. 索引优化**：
```sql
-- 为常用查询字段添加索引
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_created_at ON orders(created_at);
```

### Q20: 如何监控性能？

**A**: 集成 Spring Boot Actuator：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

**访问监控端点**：
- 健康检查：`http://localhost:8080/actuator/health`
- 性能指标：`http://localhost:8080/actuator/metrics`
- Prometheus：`http://localhost:8080/actuator/prometheus`

## 错误处理

### Q21: 如何统一处理异常？

**A**: Nebula 提供全局异常处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统错误", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
```

**自定义业务异常**：
```java
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(Long orderId) {
        super(ErrorCode.ORDER_NOT_FOUND, "订单不存在: " + orderId);
    }
}
```

## 部署运维

### Q22: 如何打包部署？

**A**: 使用 Maven 打包：

```bash
# 打包
mvn clean package -DskipTests

# 生成的 JAR 位于
target/your-app-<version>.jar
```

**运行**：
```bash
java -jar target/your-app-<version>.jar
```

**Docker 部署**：
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/your-app-<version>.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# 构建镜像
docker build -t your-app:<version> .

# 运行容器
docker run -d -p 8080:8080 your-app:<version>
```

### Q23: 如何配置日志？

**A**: 使用 Logback 配置：

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
    
    <logger name="com.andy.nebula" level="DEBUG" />
</configuration>
```

## 更多问题？

如果您的问题没有在此列出，可以：

1. **查阅文档**：[完整文档](INDEX.md)
2. **查看示例**：[示例项目](../example/README.md)
3. **提交 Issue**：[GitHub Issues](https://github.com/nebula/nebula/issues)
4. **加入讨论**：[GitHub Discussions](https://github.com/nebula/nebula/discussions)

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

