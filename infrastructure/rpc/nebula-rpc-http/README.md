# Nebula RPC HTTP 模块

##  模块简介

`nebula-rpc-http` 是 Nebula 框架的 HTTP RPC 实现模块，提供了基于 HTTP 协议的远程过程调用能力该模块支持编程式和声明式两种调用方式，集成了服务发现负载均衡等企业级特性

##  功能特性

###  核心功能
- **HTTP RPC 客户端**: 基于 RestTemplate 的高性能 RPC 客户端
- **HTTP RPC 服务器**: 基于 Spring MVC 的 RPC 服务端
- **声明式调用**: 通过 `@RpcClient` 和 `@RpcCall` 注解简化 RPC 调用
- **编程式调用**: 灵活的编程式 API，支持同步和异步调用
- **服务发现集成**: 与 Nebula 服务发现模块无缝集成
- **负载均衡**: 支持多种负载均衡策略（轮询随机等）

###  增强特性
- **自动配置**: Spring Boot 自动配置，零配置启动
- **超时控制**: 支持连接超时和读取超时配置
- **异步调用**: 内置异步调用支持，提高系统吞吐量
- **异常处理**: 统一的异常处理机制
- **连接池管理**: 高效的 HTTP 连接池管理

##  快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-http</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 基础配置

在 `application.yml` 中配置 RPC：

```yaml
nebula:
  rpc:
    http:
      enabled: true
      # 服务器配置
      server:
        enabled: true
        port: 8080
        context-path: /rpc
        request-timeout: 60000
      # 客户端配置
      client:
        enabled: true
        base-url: http://localhost:8080
        connect-timeout: 30000
        read-timeout: 60000
        retry-count: 3
```

##  使用方式

### 方式一：声明式调用（推荐）

#### 1. 定义 RPC 服务接口

```java
@RpcClient(name = "user-service", url = "http://localhost:8080")
public interface UserRpcService {
    
    @RpcCall("/api/users/{id}")
    User getUserById(@PathVariable Long id);
    
    @RpcCall(value = "/api/users", method = "POST")
    User createUser(@RequestBody CreateUserRequest request);
    
    @RpcCall(value = "/api/users", method = "GET")
    List<User> getUsers(@RequestParam String keyword);
}
```

#### 2. 启用 RPC 客户端扫描

```java
@SpringBootApplication
@EnableRpcClients(basePackages = "io.nebula.example.rpc")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 3. 使用 RPC 服务

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRpcService userRpcService;
    
    public User getUser(Long id) {
        return userRpcService.getUserById(id);
    }
    
    public User createUser(CreateUserRequest request) {
        return userRpcService.createUser(request);
    }
}
```

### 方式二：编程式调用

#### 1. 直接使用 RpcClient

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final RpcClient rpcClient;
    
    public User getUser(Long id) {
        return rpcClient.call(UserService.class, "getUserById", id);
    }
    
    public CompletableFuture<User> getUserAsync(Long id) {
        return rpcClient.callAsync(UserService.class, "getUserById", id);
    }
}
```

#### 2. 使用动态代理

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final RpcClient rpcClient;
    private UserRpcService userRpcService;
    
    @PostConstruct
    public void init() {
        this.userRpcService = rpcClient.createProxy(UserRpcService.class);
    }
    
    public User getUser(Long id) {
        return userRpcService.getUserById(id);
    }
}
```

##  高级特性

### 服务发现集成

结合 Nebula 服务发现模块，实现动态服务路由：

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: nebula-dev
      
  rpc:
    http:
      client:
        base-url: "" # 留空，通过服务发现获取地址
    discovery:
      enabled: true
      load-balance-strategy: ROUND_ROBIN
```

使用服务名调用：

```java
@RpcClient(name = "user-service")  // 从服务发现获取地址
public interface UserRpcService {
    
    @RpcCall("/api/users/{id}")
    User getUserById(@PathVariable Long id);
}
```

### 负载均衡

支持多种负载均衡策略：

```yaml
nebula:
  rpc:
    discovery:
      load-balance-strategy: ROUND_ROBIN  # 可选: ROUND_ROBIN, RANDOM, WEIGHTED
```

### 异步调用

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final RpcClient rpcClient;
    
    public CompletableFuture<User> getUserAsync(Long id) {
        return rpcClient.callAsync(UserService.class, "getUserById", id)
            .thenApply(user -> {
                // 处理结果
                log.info("获取用户成功: {}", user.getName());
                return user;
            })
            .exceptionally(ex -> {
                // 异常处理
                log.error("获取用户失败", ex);
                return null;
            });
    }
}
```

### 降级处理

定义降级处理类：

```java
@Component
public class UserRpcServiceFallback implements UserRpcService {
    
    @Override
    public User getUserById(Long id) {
        // 返回默认用户或缓存数据
        User defaultUser = new User();
        defaultUser.setId(id);
        defaultUser.setName("Unknown");
        return defaultUser;
    }
    
    @Override
    public User createUser(CreateUserRequest request) {
        throw new RuntimeException("服务暂时不可用");
    }
}
```

使用降级：

```java
@RpcClient(name = "user-service", fallback = UserRpcServiceFallback.class)
public interface UserRpcService {
    // ...
}
```

### 自定义配置

```java
@Configuration
public class RpcConfiguration {
    
    /**
     * 自定义 RestTemplate
     */
    @Bean
    @Primary
    public RestTemplate customRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .interceptors((request, body, execution) -> {
                // 添加自定义请求头
                request.getHeaders().add("X-Client-Version", "1.0.0");
                return execution.execute(request, body);
            })
            .build();
    }
    
    /**
     * 自定义执行器
     */
    @Bean
    public Executor customRpcExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("custom-rpc-");
        executor.initialize();
        return executor;
    }
}
```

##  服务端实现

### 使用 @RpcService 注解（推荐）

使用 `@RpcService` 注解自动注册 RPC 服务实现：

```java
@RpcService(UserRpcService.class)
@RequiredArgsConstructor
public class UserRpcServiceImpl implements UserRpcService {
    
    private final UserService userService;
    
    @Override
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        log.info("RPC服务端: createUser, username={}", request.getUsername());
        return userService.createUser(request);
    }
    
    @Override
    public GetUserDto.Response getUserById(Long id) {
        log.info("RPC服务端: getUserById, id={}", id);
        return userService.getUserById(id);
    }
}
```

**说明**：
- `@RpcService` 会自动将服务实现注册到 RPC 服务器
- 服务名称默认为接口的全限定名，也可通过 `serviceName` 属性自定义
- 无需手动调用 `HttpRpcServer.registerService()`

### 手动注册 RPC 服务

如果需要更灵活的控制，也可以手动注册：

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final HttpRpcServer rpcServer;
    
    @PostConstruct
    public void registerRpcService() {
        // 注册服务实现到RPC服务器
        rpcServer.registerService(UserService.class, this);
        log.info("注册RPC服务: {}", UserService.class.getName());
    }
    
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    @Override
    public User createUser(CreateUserRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return userRepository.save(user);
    }
}
```

### Controller 方式暴露服务

也可以通过传统的 Spring MVC Controller 方式暴露 RPC 服务：

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
}
```

##  故障排查

### 常见问题

1. **连接超时**
   - 检查网络连接和防火墙配置
   - 调整 `connect-timeout` 配置
   - 确认服务端是否正常运行

2. **读取超时**
   - 检查服务端处理时间
   - 调整 `read-timeout` 配置
   - 优化服务端性能

3. **服务发现失败**
   - 确认服务发现配置正确
   - 检查服务是否已注册
   - 验证网络连接

### 开启调试日志

```yaml
logging:
  level:
    io.nebula.rpc: DEBUG
    org.springframework.web.client: DEBUG
```

##  性能优化

### 连接池优化

```yaml
nebula:
  rpc:
    http:
      client:
        max-connections: 500
        max-connections-per-route: 200
        keep-alive-time: 120000
```

### 异步调用

对于非关键路径的 RPC 调用，建议使用异步方式以提高吞吐量：

```java
// 并行调用多个服务
CompletableFuture<User> userFuture = rpcClient.callAsync(UserService.class, "getUser", id);
CompletableFuture<Order> orderFuture = rpcClient.callAsync(OrderService.class, "getOrder", orderId);

// 等待所有调用完成
CompletableFuture.allOf(userFuture, orderFuture).join();
```

### 请求压缩

```yaml
nebula:
  rpc:
    http:
      client:
        compression-enabled: true
```

##  与其他模块集成

### 与服务发现集成

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  rpc:
    discovery:
      enabled: true
```

### 与链路追踪集成

RPC 调用会自动传播链路追踪信息，无需额外配置

##  DTO 设计规范

为了保证 RPC 接口的清晰性和可维护性，建议遵循以下 DTO 设计规范：

### 基本结构

每个 RPC 接口对应一个 DTO 类，包含 `Request` 和 `Response` 两个内部静态类：

```java
@Data
public class CreateUserDto {
    
    /**
     * 创建用户请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
        private String username;
        
        @NotBlank(message = "姓名不能为空")
        private String name;
        
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;
        
        private String status;
    }
    
    /**
     * 创建用户响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        
        /**
         * 新创建的用户ID
         */
        private Long id;
    }
}
```

### 设计原则

1. **一个接口一个 DTO 类**：每个 RPC 方法对应一个独立的 DTO 类
2. **Request/Response 分离**：使用内部静态类分别定义请求和响应结构
3. **数据验证**：使用 Jakarta Validation 注解进行参数校验
4. **序列化友好**：添加 `@NoArgsConstructor` 和 `@AllArgsConstructor` 支持 JSON 序列化/反序列化
5. **文档注释**：为重要字段添加清晰的注释说明

### 命名规范

- DTO 类名：`{操作名}Dto`，如 `CreateUserDto``GetUserDto`
- Request 类：`Dto.Request`
- Response 类：`Dto.Response`

### 完整示例

更多 DTO 示例请参考 [nebula-example-api](../../../nebula-example-api) 模块

##  更多示例

详细的使用示例请参考：
- [基础 RPC 调用示例](../../../nebula-example/docs/nebula-rpc-test.md)
- [RPC API 定义示例](../../../nebula-example-api/README.md)
- [完整示例项目](../../../nebula-example)

##  贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个模块

##  许可证

本项目基于 Apache 2.0 许可证开源


## 🧪 测试

本模块提供完整的单元测试文档和示例，详见 [TESTING.md](./TESTING.md)

