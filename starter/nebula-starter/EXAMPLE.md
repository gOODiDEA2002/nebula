# Nebula Starter - 使用示例

> Nebula框架一站式启动器的使用指南

## 目录

- [快速开始](#快速开始)
- [最小化应用](#最小化应用)
- [Web应用](#web应用)
- [微服务应用](#微服务应用)
- [AI应用](#ai应用)
- [完整功能应用](#完整功能应用)

---

## 快速开始

`nebula-starter` 是一个聚合模块，它本身不提供功能，而是作为依赖管理器。实际使用时，应该选择具体的子Starter模块。

### Starter模块选择

| Starter | 适用场景 | 说明 |
|---------|---------|------|
| `nebula-starter-minimal` | CLI工具、批处理、工具库 | 最小依赖，只包含核心功能 |
| `nebula-starter-web` | REST API、管理后台 | Web应用基础依赖 |
| `nebula-starter-service` | 微服务 | 完整的微服务依赖 |
| `nebula-starter-ai` | AI应用 | AI/ML应用依赖 |
| `nebula-starter-api` | RPC契约模块 | API定义模块依赖 |
| `nebula-starter-all` | 单体应用 | 全功能依赖 |

---

## 最小化应用

### 适用场景

- CLI命令行工具
- 批处理任务
- 工具类库
- 定时任务

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-minimal</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 示例：命令行工具

```java
@SpringBootApplication
public class CliApplication implements CommandLineRunner {
    
    @Autowired
    private IdGenerator idGenerator;
    
    @Autowired
    private JsonUtils jsonUtils;
    
    public static void main(String[] args) {
        SpringApplication.run(CliApplication.class, args);
    }
    
    @Override
    public void run(String... args) {
        // 生成ID
        String id = idGenerator.generateSnowflakeId();
        System.out.println("Generated ID: " + id);
        
        // 使用JSON工具
        Map<String, Object> data = Map.of("id", id, "timestamp", System.currentTimeMillis());
        String json = jsonUtils.toJson(data);
        System.out.println("JSON: " + json);
    }
}
```

---

## Web应用

### 适用场景

- REST API服务
- 管理后台
- 单页应用后端

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 示例：REST API

```java
@SpringBootApplication
public class WebApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }
    
    @PostMapping
    public Result<User> createUser(@RequestBody @Valid UserDTO userDTO) {
        User user = userService.createUser(userDTO);
        return Result.success(user);
    }
}

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    private final CacheManager cacheManager;
    
    public User getUserById(Long id) {
        return cacheManager.get("user:" + id, User.class)
                .orElseGet(() -> {
                    User user = userMapper.selectById(id);
                    if (user != null) {
                        cacheManager.set("user:" + id, user, Duration.ofHours(1));
                    }
                    return user;
                });
    }
    
    public User createUser(UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        userMapper.insert(user);
        return user;
    }
}
```

---

## 微服务应用

### 适用场景

- 微服务架构
- 分布式系统
- 云原生应用

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 示例：订单服务

```java
@SpringBootApplication
@EnableDiscoveryClient
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

// 1. 定义API契约（独立模块）
public interface OrderServiceApi {
    
    @PostMapping("/orders")
    Result<OrderDTO> createOrder(@RequestBody CreateOrderRequest request);
    
    @GetMapping("/orders/{orderId}")
    Result<OrderDTO> getOrder(@PathVariable String orderId);
}

// 2. 实现服务
@RestController
@RequiredArgsConstructor
public class OrderController implements OrderServiceApi {
    
    private final OrderService orderService;
    
    @Override
    public Result<OrderDTO> createOrder(CreateOrderRequest request) {
        OrderDTO order = orderService.createOrder(request);
        return Result.success(order);
    }
    
    @Override
    public Result<OrderDTO> getOrder(String orderId) {
        OrderDTO order = orderService.getOrderById(orderId);
        return Result.success(order);
    }
}

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;  // RPC调用
    private final MessageProducer messageProducer;      // 消息发送
    private final DistributedLock distributedLock;      // 分布式锁
    
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO createOrder(CreateOrderRequest request) {
        // 1. 验证用户
        UserDTO user = userServiceClient.getUser(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 2. 创建订单（使用分布式锁）
        String lockKey = "order:create:" + request.getUserId();
        
        return distributedLock.executeWithLock(lockKey, Duration.ofSeconds(10), () -> {
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setTotalAmount(request.getTotalAmount());
            order.setStatus("CREATED");
            
            orderMapper.insert(order);
            
            // 3. 发送订单创建消息
            Message<OrderDTO> message = Message.of("order.created", toDTO(order));
            messageProducer.send(message);
            
            return toDTO(order);
        });
    }
    
    private OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        BeanUtils.copyProperties(order, dto);
        return dto;
    }
}

// 3. RPC客户端
@Component
@RequiredArgsConstructor
public class UserServiceClient {
    
    private final RpcClient rpcClient;
    
    public UserDTO getUser(Long userId) {
        RpcRequest request = RpcRequest.builder()
                .serviceName("user-service")
                .methodName("getUser")
                .parameters(new Object[]{userId})
                .build();
        
        RpcResponse<UserDTO> response = rpcClient.call(request);
        
        if (!response.isSuccess()) {
            throw new RpcException("调用用户服务失败");
        }
        
        return response.getResult();
    }
}
```

---

## AI应用

### 适用场景

- AI/ML应用
- 智能推荐
- RAG应用
- LLM应用

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 示例：智能客服

```java
@SpringBootApplication
public class AiChatApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AiChatApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    
    @PostMapping
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request.getMessage());
        return Result.success(response);
    }
}

@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    
    public ChatResponse chat(String userMessage) {
        // 1. 检索相关文档（RAG）
        List<Document> relatedDocs = vectorStore.similaritySearch(
                userMessage, 
                5
        );
        
        // 2. 构建上下文
        String context = buildContext(relatedDocs);
        
        // 3. 生成回复
        Prompt prompt = new Prompt(String.format(
                "Context: %s\n\nUser: %s\n\nAssistant:", 
                context, 
                userMessage
        ));
        
        ChatResponse response = chatClient.call(prompt);
        
        return response;
    }
    
    private String buildContext(List<Document> docs) {
        return docs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));
    }
}
```

---

## 完整功能应用

### 适用场景

- 单体应用
- 全功能后端
- 原型开发

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-all</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 配置

```yaml
spring:
  application:
    name: full-app

nebula:
  # 所有模块都可用，按需配置
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
    mongodb:
      enabled: true
  
  messaging:
    rabbitmq:
      enabled: true
  
  rpc:
    http:
      enabled: true
  
  discovery:
    nacos:
      enabled: true
  
  storage:
    minio:
      enabled: true
  
  search:
    elasticsearch:
      enabled: true
  
  ai:
    spring:
      enabled: true
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

