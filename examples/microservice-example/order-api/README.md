# Nebula Example Order API

> 订单服务 API 契约模块，包含 RPC 接口定义DTOVO实体等

##  模块说明

这是一个独立的 API 契约模块（API Contract Module），包含订单服务的 RPC 接口定义数据传输对象（DTO）视图对象（VO）和实体定义，用于在服务提供方和消费方之间共享接口契约

##  设计原则

遵循微服务架构的最佳实践：

- **接口与实现分离**: RPC 接口定义独立于具体实现
- **契约共享**: 服务提供方和消费方依赖同一份接口定义
- **版本管理**: 接口变更可以独立版本控制
- **降低耦合**: 避免实现细节泄漏到接口定义中
- **零配置注入**: 支持在消费方直接注入使用，无需手动配置

##  模块结构

```
nebula-example-order-api/
 src/main/java/io/nebula/example/order/api/
    rpc/                           # RPC服务接口定义
       OrderRpcClient.java        # 订单RPC客户端接口
    dto/                           # 数据传输对象
       CreateOrderDto.java        # 创建订单 DTO
       GetOrderDto.java           # 获取订单 DTO
    vo/                            # 视图对象
       OrderVo.java               # 订单视图对象
    entity/                        # 实体对象
       Order.java                 # 订单实体
    OrderApiAutoConfiguration.java # 自动配置类
 pom.xml
```

##  依赖关系

### 架构图

```
nebula-example-order-api (契约层)
                      
    依赖               依赖
                      
nebula-example-      nebula-example
order-service           (client)
  (提供者)             (消费者)
```

### 谁应该依赖这个模块

1. **服务提供方** (`nebula-example-order-service`): 
   - 依赖此模块获取接口定义
   - 实现 `OrderRpcClient` 接口
   - 使用 `@RpcService` 注解发布服务

2. **服务消费方** (`nebula-example`, `nebula-example-user-service` 等): 
   - 依赖此模块获取接口定义  
   - 直接注入 `OrderRpcClient` 接口即可调用远程服务
   - 无需任何额外配置

### 依赖配置

在消费方的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-example-order-api</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

##  核心组件

### 1. RPC 接口定义

#### OrderRpcClient

订单服务的 RPC 客户端接口，使用声明式注解方式定义

```java
@RemoteService
public interface OrderRpcClient {
    
    /**
     * 创建订单
     * 业务流程：
     * 1. 验证用户是否存在（调用UserService）
     * 2. 创建订单
     * 3. 返回订单信息
     */
    @RpcCall(value = "/rpc/orders", method = "POST")
    CreateOrderDto.Response createOrder(@RequestBody CreateOrderDto.Request request);
    
    /**
     * 获取订单详情
     * 业务流程：
     * 1. 查询订单信息
     * 2. 关联查询用户信息（调用UserService）
     * 3. 返回订单详情
     */
    @RpcCall(value = "/rpc/orders/{id}", method = "GET")
    GetOrderDto.Response getOrderById(@PathVariable("id") Long id);
}
```

**注解说明：**

- `@RemoteService`: 标识这是一个 RPC 客户端接口，框架会自动生成代理实现
- `@RpcCall`: 定义 RPC 调用的路径和 HTTP 方法
- `@RequestBody`: 标识请求体参数
- `@PathVariable`: 标识路径变量

### 2. 数据传输对象 (DTO)

#### CreateOrderDto

创建订单的请求和响应 DTO：

```java
public class CreateOrderDto {
    
    @Data
    public static class Request {
        @NotNull(message = "用户ID不能为空")
        private Long userId;
        
        @NotNull(message = "商品名称不能为空")
        private String productName;
        
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于0")
        private Integer quantity;
        
        @NotNull(message = "单价不能为空")
        @Min(value = 0, message = "单价不能为负数")
        private BigDecimal price;
    }
    
    @Data
    public static class Response {
        private Long orderId;
        private String orderNo;
        private BigDecimal totalAmount;
    }
}
```

#### GetOrderDto

获取订单详情的请求和响应 DTO：

```java
public class GetOrderDto {
    
    @Data
    public static class Request {
        @NotNull(message = "订单ID不能为空")
        private Long id;
    }
    
    @Data
    public static class Response {
        private OrderVo order;
        private String userName;  // 关联查询的用户信息
    }
}
```

### 3. 实体对象

#### Order

订单实体，包含订单的完整信息：

```java
@Data
public class Order {
    private Long id;              // 订单ID
    private String orderNo;       // 订单号
    private Long userId;          // 用户ID
    private String productName;   // 商品名称
    private Integer quantity;     // 数量
    private BigDecimal price;     // 单价
    private BigDecimal totalAmount; // 订单总金额
    private String status;        // 订单状态
    private LocalDateTime createTime;  // 创建时间
    private LocalDateTime updateTime;  // 更新时间
}
```

### 4. 视图对象 (VO)

#### OrderVo

订单视图对象，用于前端展示：

```java
@Data
public class OrderVo {
    private Long id;
    private String orderNo;
    private Long userId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

##  使用示例

### 服务提供方实现

在 `nebula-example-order-service` 中实现接口：

```java
@Slf4j
@RpcService  // 无需指定接口，自动推导
@RequiredArgsConstructor
public class OrderRpcClientImpl implements OrderRpcClient {
    
    private final OrderService orderService;
    private final UserRpcClient userRpcClient;  // 可以调用其他服务
    
    @Override
    public CreateOrderDto.Response createOrder(CreateOrderDto.Request request) {
        log.info("创建订单: userId={}, product={}", 
                request.getUserId(), request.getProductName());
        
        // 1. 验证用户是否存在
        GetUserDto.Response userResponse = userRpcClient.getUserById(request.getUserId());
        if (userResponse == null || userResponse.getUser() == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        
        // 2. 创建订单
        Order order = orderService.createOrder(request);
        
        // 3. 返回响应
        CreateOrderDto.Response response = new CreateOrderDto.Response();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setTotalAmount(order.getTotalAmount());
        
        return response;
    }
    
    @Override
    public GetOrderDto.Response getOrderById(Long id) {
        log.info("获取订单详情: orderId={}", id);
        
        // 1. 查询订单
        Order order = orderService.getById(id);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        
        // 2. 关联查询用户信息
        GetUserDto.Response userResponse = userRpcClient.getUserById(order.getUserId());
        
        // 3. 构造响应
        GetOrderDto.Response response = new GetOrderDto.Response();
        response.setOrder(convertToVo(order));
        response.setUserName(userResponse.getUser().getName());
        
        return response;
    }
}
```

**关键点：**
-  使用 `@RpcService` 注解，无需指定接口类
-  可以注入并调用其他 RPC 服务（如 `UserRpcClient`）
-  框架自动处理服务注册路由负载均衡

### 服务消费方使用

在 `nebula-example` 或其他服务中使用：

#### 方式1：零配置注入（推荐）

```java
@Service
@RequiredArgsConstructor
public class ShoppingService {
    
    // 直接注入，无需任何配置
    private final OrderRpcClient orderRpcClient;
    
    public void placeOrder(Long userId, String productName, Integer quantity, BigDecimal price) {
        // 创建订单请求
        CreateOrderDto.Request request = new CreateOrderDto.Request();
        request.setUserId(userId);
        request.setProductName(productName);
        request.setQuantity(quantity);
        request.setPrice(price);
        
        // 调用订单服务
        CreateOrderDto.Response response = orderRpcClient.createOrder(request);
        
        log.info("订单创建成功: orderNo={}, totalAmount={}", 
                response.getOrderNo(), response.getTotalAmount());
    }
    
    public void checkOrder(Long orderId) {
        // 查询订单详情
        GetOrderDto.Response response = orderRpcClient.getOrderById(orderId);
        
        log.info("订单详情: {}, 用户: {}", 
                response.getOrder().getOrderNo(), 
                response.getUserName());
    }
}
```

#### 方式2：控制器中使用

```java
@RestController
@RequestMapping("/api/shopping")
@RequiredArgsConstructor
public class ShoppingController {
    
    private final OrderRpcClient orderRpcClient;
    
    @PostMapping("/orders")
    public Result<CreateOrderDto.Response> createOrder(@RequestBody CreateOrderDto.Request request) {
        CreateOrderDto.Response response = orderRpcClient.createOrder(request);
        return Result.success(response);
    }
    
    @GetMapping("/orders/{id}")
    public Result<GetOrderDto.Response> getOrder(@PathVariable Long id) {
        GetOrderDto.Response response = orderRpcClient.getOrderById(id);
        return Result.success(response);
    }
}
```

**关键点：**
-  无需 `@EnableRpcClients` 注解
-  无需 `@Qualifier` 注解
-  自动服务发现和负载均衡
-  支持 gRPC 和 HTTP 协议自动切换

##  零配置特性

Nebula RPC 框架支持零配置 RPC 客户端注入，提供极致的开发体验：

### 传统方式 vs Nebula 方式

#### 传统方式（繁琐）

```java
// 1. 启动类需要配置
@EnableRpcClients(basePackages = "io.nebula.example.order.api.rpc")
public class Application { }

// 2. 注入需要 @Qualifier
@Autowired
@Qualifier("orderRpcClient")
private OrderRpcClient orderRpcClient;

// 3. 需要手动配置服务名
@RemoteService(serviceName = "nebula-example-order-service")
public interface OrderRpcClient { }
```

#### Nebula 方式（简洁）

```java
// 1. 启动类无需任何配置
@SpringBootApplication
public class Application { }

// 2. 直接注入即可
@RequiredArgsConstructor
public class Service {
    private final OrderRpcClient orderRpcClient;
}

// 3. 接口定义也无需配置
@RemoteService  // 仅此一个注解
public interface OrderRpcClient { }
```

### 工作原理

1. **自动包扫描**: 框架自动扫描 `@RemoteService` 注解的接口
2. **服务名推导**: 根据包路径自动推导服务名（`order.api`  `nebula-example-order-service`）
3. **动态代理**: 自动生成代理实现并注册到 Spring 容器
4. **服务发现**: 自动从 Nacos 发现目标服务
5. **协议选择**: 优先使用 gRPC，降级到 HTTP

##  最佳实践

### 1. DTO 设计规范

**Request/Response 分离：**

```java
public class CreateOrderDto {
    @Data
    public static class Request {
        // 请求参数
    }
    
    @Data
    public static class Response {
        // 响应数据
    }
}
```

**添加参数验证：**

```java
@Data
public static class Request {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;
}
```

### 2. 接口设计规范

**清晰的 JavaDoc：**

```java
/**
 * 创建订单
 * 
 * 业务流程：
 * 1. 验证用户是否存在
 * 2. 创建订单
 * 3. 返回订单信息
 * 
 * @param request 订单创建请求
 * @return 订单创建响应
 * @throws BusinessException 业务异常
 */
@RpcCall(value = "/rpc/orders", method = "POST")
CreateOrderDto.Response createOrder(@RequestBody CreateOrderDto.Request request);
```

**RESTful 风格路径：**

```java
@RpcCall(value = "/rpc/orders", method = "POST")      // 创建
@RpcCall(value = "/rpc/orders/{id}", method = "GET")  // 查询
@RpcCall(value = "/rpc/orders/{id}", method = "PUT")  // 更新
@RpcCall(value = "/rpc/orders/{id}", method = "DELETE") // 删除
```

### 3. 版本管理

**接口变更策略：**

- **向后兼容**: 添加可选字段，不删除已有字段
- **版本隔离**: 重大变更时创建新版本（如 `OrderRpcClientV2`）
- **废弃标记**: 使用 `@Deprecated` 标记过时接口

```java
@Deprecated(since = "2.1.0", forRemoval = true)
CreateOrderDto.Response createOrderOld(@RequestBody CreateOrderDto.Request request);

@RpcCall(value = "/rpc/v2/orders", method = "POST")
CreateOrderDto.Response createOrder(@RequestBody CreateOrderDto.Request request);
```

## ️ 注意事项

1. **保持轻量**: 此模块应只依赖必要的框架，不要引入业务逻辑或第三方业务库
2. **接口稳定**: 接口变更需要考虑向后兼容性，避免影响已有消费方
3. **文档完善**: 为所有接口和 DTO 添加清晰的 JavaDoc 注释
4. **参数验证**: 在 DTO 中使用 Jakarta Validation 注解进行参数校验
5. **同步更新**: 接口变更后，需要同时更新服务提供方和消费方

##  依赖管理

### pom.xml

```xml
<dependencies>
    <!-- Nebula RPC Core -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-rpc-core</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- Spring Web (注解支持) -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
        <scope>provided</scope>
    </dependency>
    
    <!-- Jakarta Validation -->
    <dependency>
        <groupId>jakarta.validation</groupId>
        <artifactId>jakarta.validation-api</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

##  相关文档

- [订单服务实现](../nebula-example-order-service/README.md) - 服务提供方文档
- [用户服务 API](../nebula-example-user-api/README.md) - 类似的 API 契约示例
- [Nebula RPC 文档](../../nebula/infrastructure/rpc/) - RPC 框架核心文档
- [服务发现文档](../../nebula/infrastructure/discovery/) - Nacos 服务发现

##  更新日志

- **2025-10** - 支持零配置 RPC 客户端注入
- **2025-10** - 实现跨服务调用（订单服务调用用户服务）
- **2025-09** - 初始版本，定义订单服务 RPC 接口

##  许可证

Apache License 2.0

---

**Nebula Example Order API** - 订单服务契约层，支持零配置 RPC 调用

