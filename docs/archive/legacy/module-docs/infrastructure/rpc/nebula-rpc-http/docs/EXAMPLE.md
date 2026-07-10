# Nebula RPC HTTP - 使用示例

> HTTP RPC远程调用完整使用指南，以票务微服务为例

## 目录

- [快速开始](#快速开始)
- [服务提供者](#服务提供者)
- [服务消费者](#服务消费者)
- [参数传递](#参数传递)
- [异常处理](#异常处理)
- [超时和重试](#超时和重试)
- [负载均衡](#负载均衡)
- [服务发现集成](#服务发现集成)
- [拦截器](#拦截器)
- [链路追踪](#链路追踪)
- [票务微服务完整示例](#票务微服务完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-http</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  rpc:
    http:
      enabled: true
      
      # 服务端配置（提供RPC服务）
      server:
        enabled: true
        port: ${server.port:8080}  # 复用Web端口
        context-path: /rpc         # RPC基础路径
        max-request-size: 10485760 # 10MB
        request-timeout: 60000     # 60秒
      
      # 客户端配置（调用RPC服务）
      client:
        enabled: true
        connect-timeout: 5000      # 连接超时5秒
        read-timeout: 10000        # 读取超时10秒
        write-timeout: 10000       # 写入超时10秒
        max-connections: 200       # 最大连接数
        retry-count: 3             # 失败重试次数
        logging-enabled: true      # 启用日志
```

---

## 服务提供者

### 1. 定义RPC服务接口

```java
/**
 * 用户服务接口（API契约）
 * 放在独立的API模块中，供服务提供者和消费者共同依赖
 */
package io.nebula.ticket.user.api;

import io.nebula.rpc.core.annotation.RpcService;

@RpcService
public interface UserService {
    
    /**
     * 获取用户信息
     */
    UserDTO getUserById(Long userId);
    
    /**
     * 批量获取用户信息
     */
    List<UserDTO> getUsersByIds(List<Long> userIds);
    
    /**
     * 验证用户身份
     */
    boolean validateUser(Long userId, String token);
    
    /**
     * 更新用户余额
     */
    boolean updateBalance(Long userId, BigDecimal amount);
}

/**
 * 用户DTO
 */
@Data
public class UserDTO implements Serializable {
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private BigDecimal balance;
    private String level;  // VIP等级
}
```

### 2. 实现RPC服务

```java
/**
 * 用户服务实现（服务提供者）
 */
package io.nebula.ticket.user.service;

import io.nebula.rpc.core.annotation.RpcService;
import io.nebula.ticket.user.api.UserService;
import io.nebula.ticket.user.api.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RpcService  // 标记为RPC服务
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    
    @Override
    public UserDTO getUserById(Long userId) {
        log.info("RPC调用：获取用户信息，userId={}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        return convertToDTO(user);
    }
    
    @Override
    public List<UserDTO> getUsersByIds(List<Long> userIds) {
        log.info("RPC调用：批量获取用户信息，数量={}", userIds.size());
        
        List<User> users = userMapper.selectBatchIds(userIds);
        
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean validateUser(Long userId, String token) {
        log.info("RPC调用：验证用户身份，userId={}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 验证token
        return jwtUtils.validateToken(token, user.getUsername());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBalance(Long userId, BigDecimal amount) {
        log.info("RPC调用：更新用户余额，userId={}, amount={}", userId, amount);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        BigDecimal newBalance = user.getBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("余额不足");
        }
        
        user.setBalance(newBalance);
        int updated = userMapper.updateById(user);
        
        return updated > 0;
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setBalance(user.getBalance());
        dto.setLevel(user.getLevel());
        return dto;
    }
}
```

---

## 服务消费者

### 1. 声明RPC客户端

```java
/**
 * RPC客户端配置
 */
@Configuration
@EnableRpcClients(basePackages = "io.nebula.ticket.order.client")
public class RpcClientConfig {
    // 自动扫描并注册RPC客户端
}
```

### 2. 定义RPC客户端接口

```java
/**
 * 用户服务RPC客户端
 */
package io.nebula.ticket.order.client;

import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.ticket.user.api.UserService;
import io.nebula.ticket.user.api.UserDTO;

/**
 * 指定服务名称（用于服务发现）
 */
@RpcClient(name = "user-service", fallback = UserServiceFallback.class)
public interface UserServiceClient extends UserService {
    // 继承API接口，无需重复定义方法
}

/**
 * 降级处理（Fallback）
 */
@Component
@Slf4j
public class UserServiceFallback implements UserServiceClient {
    
    @Override
    public UserDTO getUserById(Long userId) {
        log.warn("用户服务降级：getUserById, userId={}", userId);
        
        // 返回默认用户信息
        UserDTO dto = new UserDTO();
        dto.setUserId(userId);
        dto.setUsername("未知用户");
        return dto;
    }
    
    @Override
    public List<UserDTO> getUsersByIds(List<Long> userIds) {
        log.warn("用户服务降级：getUsersByIds, 数量={}", userIds.size());
        return new ArrayList<>();
    }
    
    @Override
    public boolean validateUser(Long userId, String token) {
        log.warn("用户服务降级：validateUser");
        return false;
    }
    
    @Override
    public boolean updateBalance(Long userId, BigDecimal amount) {
        log.warn("用户服务降级：updateBalance");
        return false;
    }
}
```

### 3. 使用RPC客户端

```java
/**
 * 订单服务（消费用户服务）
 */
package io.nebula.ticket.order.service;

import io.nebula.ticket.order.client.UserServiceClient;
import io.nebula.ticket.user.api.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final UserServiceClient userServiceClient;
    private final ShowtimeServiceClient showtimeServiceClient;
    
    /**
     * 创建订单（调用用户服务和演出服务）
     */
    public String createOrder(CreateOrderRequest request) {
        // 1. 远程调用用户服务，验证用户
        UserDTO user = userServiceClient.getUserById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 2. 远程调用演出服务，获取演出信息
        ShowtimeDTO showtime = showtimeServiceClient.getShowtimeById(request.getShowtimeId());
        if (showtime == null) {
            throw new BusinessException("演出不存在");
        }
        
        // 3. 检查库存（远程调用）
        boolean hasStock = showtimeServiceClient.checkStock(
                request.getShowtimeId(), request.getQuantity());
        if (!hasStock) {
            throw new BusinessException("库存不足");
        }
        
        // 4. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setShowtimeId(request.getShowtimeId());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(showtime.getPrice().multiply(new BigDecimal(request.getQuantity())));
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        
        orderMapper.insert(order);
        
        log.info("订单创建成功：orderNo={}", order.getOrderNo());
        
        return order.getOrderNo();
    }
    
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis();
    }
}
```

---

## 参数传递

### 1. 基本类型参数

```java
/**
 * 基本类型参数传递
 */
@RpcService
public interface BasicParamService {
    
    /**
     * 单个参数
     */
    String echo(String message);
    
    /**
     * 多个基本类型参数
     */
    int add(int a, int b);
    
    /**
     * Long类型参数
     */
    boolean exists(Long id);
}
```

### 2. 对象参数

```java
/**
 * 对象参数传递
 */
@RpcService
public interface ObjectParamService {
    
    /**
     * 单个对象参数
     */
    OrderDTO getOrderInfo(OrderQuery query);
    
    /**
     * 多个对象参数
     */
    boolean createOrder(UserDTO user, ShowtimeDTO showtime, OrderRequest request);
}

/**
 * 查询对象
 */
@Data
public class OrderQuery implements Serializable {
    private String orderNo;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

### 3. 集合参数

```java
/**
 * 集合参数传递
 */
@RpcService
public interface CollectionParamService {
    
    /**
     * List参数
     */
    List<UserDTO> getUsersByIds(List<Long> userIds);
    
    /**
     * Set参数
     */
    Set<String> getUniqueOrderNos(Set<Long> userIds);
    
    /**
     * Map参数
     */
    Map<String, Object> getOrderDetails(Map<String, String> params);
}
```

### 4. 泛型参数

```java
/**
 * 泛型参数传递
 */
@RpcService
public interface GenericParamService {
    
    /**
     * 泛型返回值
     */
    <T> Result<T> query(String id);
    
    /**
     * 泛型分页返回
     */
    <T> PageResult<T> queryPage(PageRequest request);
}

/**
 * 通用结果对象
 */
@Data
public class Result<T> implements Serializable {
    private boolean success;
    private String message;
    private T data;
}

/**
 * 分页结果
 */
@Data
public class PageResult<T> implements Serializable {
    private int pageNum;
    private int pageSize;
    private long total;
    private List<T> records;
}
```

---

## 异常处理

### 1. 业务异常传递

```java
/**
 * 业务异常定义
 */
public class BusinessException extends RuntimeException {
    private String code;
    private String message;
    
    public BusinessException(String message) {
        super(message);
        this.message = message;
    }
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}

/**
 * RPC服务抛出业务异常
 */
@Service
@RpcService
public class ShowtimeServiceImpl implements ShowtimeService {
    
    @Override
    public ShowtimeDTO getShowtimeById(Long showtimeId) {
        Showtime showtime = showtimeMapper.selectById(showtimeId);
        
        if (showtime == null) {
            // 抛出业务异常，客户端可以捕获
            throw new BusinessException("SHOWTIME_NOT_FOUND", "演出不存在");
        }
        
        return convertToDTO(showtime);
    }
}

/**
 * RPC客户端捕获异常
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceConsumer {
    
    private final ShowtimeServiceClient showtimeServiceClient;
    
    public void processOrder(Long showtimeId) {
        try {
            ShowtimeDTO showtime = showtimeServiceClient.getShowtimeById(showtimeId);
            // 业务处理
        } catch (BusinessException e) {
            log.error("RPC调用失败：code={}, message={}", e.getCode(), e.getMessage());
            throw new BusinessException("订单处理失败：" + e.getMessage());
        }
    }
}
```

### 2. 全局异常处理器

```java
/**
 * RPC全局异常处理器
 */
@ControllerAdvice
@Slf4j
public class RpcExceptionHandler {
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.error("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        
        Result<Void> result = Result.error("SYSTEM_ERROR", "系统错误");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}
```

---

## 超时和重试

### 1. 配置超时时间

```yaml
nebula:
  rpc:
    http:
      client:
        connect-timeout: 5000      # 连接超时5秒
        read-timeout: 10000        # 读取超时10秒
        write-timeout: 10000       # 写入超时10秒
```

### 2. 方法级别超时

```java
/**
 * 方法级别超时配置
 */
@RpcClient(name = "user-service")
public interface UserServiceClient {
    
    /**
     * 默认超时时间
     */
    UserDTO getUserById(Long userId);
    
    /**
     * 自定义超时时间（30秒）
     */
    @RpcCall(timeout = 30000)
    List<UserDTO> exportAllUsers();
}
```

### 3. 重试配置

```yaml
nebula:
  rpc:
    http:
      client:
        retry-count: 3             # 失败重试3次
        retry-interval: 1000       # 重试间隔1秒
```

### 4. 自定义重试策略

```java
/**
 * 自定义重试策略
 */
@Configuration
public class RpcRetryConfig {
    
    @Bean
    public RetryPolicy rpcRetryPolicy() {
        return new RetryPolicy() {
            @Override
            public boolean shouldRetry(Exception e, int retryCount) {
                // 只重试网络异常和超时异常
                if (e instanceof ConnectException || e instanceof SocketTimeoutException) {
                    return retryCount < 3;
                }
                // 业务异常不重试
                return false;
            }
            
            @Override
            public long getRetryInterval(int retryCount) {
                // 递增延迟
                return 1000L * retryCount;
            }
        };
    }
}
```

---

## 负载均衡

### 1. 配置负载均衡策略

```yaml
nebula:
  rpc:
    http:
      client:
        load-balancer:
          strategy: round-robin  # 轮询（默认）| random | weighted
```

### 2. 自定义负载均衡器

```java
/**
 * 自定义负载均衡器
 */
@Component
public class CustomLoadBalancer implements LoadBalancer {
    
    @Override
    public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }
        
        // 自定义负载均衡逻辑：根据响应时间选择
        return instances.stream()
                .min(Comparator.comparingLong(ServiceInstance::getAvgResponseTime))
                .orElse(instances.get(0));
    }
}
```

---

## 服务发现集成

### 1. 配置Nacos服务发现

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: ticket-system
        group: DEFAULT_GROUP

nebula:
  rpc:
    http:
      client:
        discovery:
          enabled: true
          type: nacos  # 使用Nacos服务发现
```

### 2. 服务注册

```java
/**
 * 服务提供者自动注册
 */
@SpringBootApplication
@EnableDiscoveryClient  // 启用服务发现
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### 3. 服务发现

```java
/**
 * 服务消费者使用服务名调用
 */
@RpcClient(name = "user-service")  // 使用服务名，不需要指定IP和端口
public interface UserServiceClient extends UserService {
    // RPC客户端会自动从Nacos获取user-service的实例列表
}
```

---

## 拦截器

### 1. 请求拦截器

```java
/**
 * RPC请求拦截器
 */
@Component
@Slf4j
public class RpcRequestInterceptor implements RequestInterceptor {
    
    @Override
    public void beforeRequest(RpcRequest request) {
        log.info("RPC请求开始：service={}, method={}", 
                request.getServiceName(), request.getMethodName());
        
        // 1. 添加请求头（如认证token）
        String token = SecurityContext.getCurrentToken();
        request.addHeader("Authorization", "Bearer " + token);
        
        // 2. 添加链路追踪ID
        String traceId = TraceContext.getTraceId();
        request.addHeader("X-Trace-Id", traceId);
        
        // 3. 记录请求时间
        request.setAttribute("startTime", System.currentTimeMillis());
    }
    
    @Override
    public void afterResponse(RpcRequest request, RpcResponse response) {
        long startTime = (long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("RPC请求完成：service={}, method={}, duration={}ms, success={}", 
                request.getServiceName(), request.getMethodName(), 
                duration, response.isSuccess());
    }
    
    @Override
    public void onError(RpcRequest request, Exception e) {
        log.error("RPC请求失败：service={}, method={}", 
                request.getServiceName(), request.getMethodName(), e);
    }
}
```

### 2. 响应拦截器

```java
/**
 * RPC响应拦截器
 */
@Component
@Slf4j
public class RpcResponseInterceptor implements ResponseInterceptor {
    
    @Override
    public void beforeReturn(RpcResponse response) {
        // 在返回结果前处理
        log.debug("RPC响应：status={}, data={}", 
                response.getStatusCode(), response.getData());
    }
    
    @Override
    public void afterReturn(RpcResponse response) {
        // 在返回结果后处理
    }
}
```

---

## 链路追踪

### 1. 启用链路追踪

```yaml
nebula:
  rpc:
    http:
      trace:
        enabled: true
        trace-id-header: X-Trace-Id  # 链路ID的Header名称
```

### 2. 链路追踪上下文

```java
/**
 * 链路追踪上下文
 */
public class TraceContext {
    
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    
    public static String getTraceId() {
        return TRACE_ID.get();
    }
    
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }
    
    public static void clear() {
        TRACE_ID.remove();
    }
}

/**
 * 链路追踪拦截器
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                            Object handler) {
        // 1. 获取或生成TraceID
        String traceId = request.getHeader("X-Trace-Id");
        if (StringUtils.isEmpty(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        
        // 2. 设置到上下文
        TraceContext.setTraceId(traceId);
        
        // 3. 添加到响应头
        response.setHeader("X-Trace-Id", traceId);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理上下文
        TraceContext.clear();
    }
}
```

---

## 票务微服务完整示例

### 架构图

```
[用户服务] ←→ [订单服务] ←→ [票务服务]
    ↓            ↓             ↓
[用户数据库]  [订单数据库]  [票务数据库]
```

### 1. 用户服务（user-service）

```java
/**
 * 用户服务API
 */
package io.nebula.ticket.user.api;

@RpcService
public interface UserService {
    UserDTO getUserById(Long userId);
    boolean updateBalance(Long userId, BigDecimal amount);
}

/**
 * 用户服务实现
 */
@Service
@RpcService
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    
    @Override
    public UserDTO getUserById(Long userId) {
        log.info("获取用户信息：userId={}", userId);
        User user = userMapper.selectById(userId);
        return convertToDTO(user);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBalance(Long userId, BigDecimal amount) {
        log.info("更新用户余额：userId={}, amount={}", userId, amount);
        
        User user = userMapper.selectById(userId);
        BigDecimal newBalance = user.getBalance().add(amount);
        
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("余额不足");
        }
        
        user.setBalance(newBalance);
        return userMapper.updateById(user) > 0;
    }
}
```

### 2. 演出服务（showtime-service）

```java
/**
 * 演出服务API
 */
package io.nebula.ticket.showtime.api;

@RpcService
public interface ShowtimeService {
    ShowtimeDTO getShowtimeById(Long showtimeId);
    boolean checkStock(Long showtimeId, int quantity);
    boolean deductStock(Long showtimeId, int quantity);
    boolean restoreStock(Long showtimeId, int quantity);
}

/**
 * 演出服务实现
 */
@Service
@RpcService
@RequiredArgsConstructor
@Slf4j
public class ShowtimeServiceImpl implements ShowtimeService {
    
    private final ShowtimeMapper showtimeMapper;
    private final LockManager lockManager;
    
    @Override
    public ShowtimeDTO getShowtimeById(Long showtimeId) {
        log.info("获取演出信息：showtimeId={}", showtimeId);
        Showtime showtime = showtimeMapper.selectById(showtimeId);
        return convertToDTO(showtime);
    }
    
    @Override
    public boolean checkStock(Long showtimeId, int quantity) {
        log.info("检查库存：showtimeId={}, quantity={}", showtimeId, quantity);
        Showtime showtime = showtimeMapper.selectById(showtimeId);
        return showtime.getAvailableSeats() >= quantity;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Locked(key = "'stock:' + #showtimeId", waitTime = 10, leaseTime = 30)
    public boolean deductStock(Long showtimeId, int quantity) {
        log.info("扣减库存：showtimeId={}, quantity={}", showtimeId, quantity);
        
        Showtime showtime = showtimeMapper.selectById(showtimeId);
        
        if (showtime.getAvailableSeats() < quantity) {
            throw new BusinessException("库存不足");
        }
        
        showtime.setAvailableSeats(showtime.getAvailableSeats() - quantity);
        return showtimeMapper.updateById(showtime) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreStock(Long showtimeId, int quantity) {
        log.info("恢复库存：showtimeId={}, quantity={}", showtimeId, quantity);
        
        Showtime showtime = showtimeMapper.selectById(showtimeId);
        showtime.setAvailableSeats(showtime.getAvailableSeats() + quantity);
        
        return showtimeMapper.updateById(showtime) > 0;
    }
}
```

### 3. 订单服务（order-service）

```java
/**
 * 订单服务（消费用户服务和演出服务）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final UserServiceClient userServiceClient;
    private final ShowtimeServiceClient showtimeServiceClient;
    private final OrderMapper orderMapper;
    
    /**
     * 创建订单（跨服务调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(CreateOrderRequest request) {
        Long userId = request.getUserId();
        Long showtimeId = request.getShowtimeId();
        int quantity = request.getQuantity();
        
        log.info("创建订单：userId={}, showtimeId={}, quantity={}", 
                userId, showtimeId, quantity);
        
        // 1. RPC调用：获取用户信息
        UserDTO user = userServiceClient.getUserById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 2. RPC调用：获取演出信息
        ShowtimeDTO showtime = showtimeServiceClient.getShowtimeById(showtimeId);
        if (showtime == null) {
            throw new BusinessException("演出不存在");
        }
        
        // 3. RPC调用：检查库存
        boolean hasStock = showtimeServiceClient.checkStock(showtimeId, quantity);
        if (!hasStock) {
            throw new BusinessException("库存不足");
        }
        
        // 4. 计算金额
        BigDecimal totalAmount = showtime.getPrice().multiply(new BigDecimal(quantity));
        
        // 5. RPC调用：检查用户余额
        if (user.getBalance().compareTo(totalAmount) < 0) {
            throw new BusinessException("余额不足");
        }
        
        // 6. RPC调用：扣减库存
        boolean stockDeducted = showtimeServiceClient.deductStock(showtimeId, quantity);
        if (!stockDeducted) {
            throw new BusinessException("库存扣减失败");
        }
        
        // 7. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setShowtimeId(showtimeId);
        order.setQuantity(quantity);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        
        orderMapper.insert(order);
        
        log.info("订单创建成功：orderNo={}", order.getOrderNo());
        
        return order.getOrderNo();
    }
    
    /**
     * 支付订单（跨服务调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(String orderNo) {
        log.info("支付订单：orderNo={}", orderNo);
        
        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        
        // 2. RPC调用：扣减用户余额
        boolean balanceUpdated = userServiceClient.updateBalance(
                order.getUserId(), order.getTotalAmount().negate());
        
        if (!balanceUpdated) {
            throw new BusinessException("余额扣减失败");
        }
        
        // 3. 更新订单状态
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        log.info("订单支付成功：orderNo={}", orderNo);
        
        return true;
    }
    
    /**
     * 取消订单（跨服务调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderNo) {
        log.info("取消订单：orderNo={}", orderNo);
        
        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        
        // 2. RPC调用：恢复库存
        boolean stockRestored = showtimeServiceClient.restoreStock(
                order.getShowtimeId(), order.getQuantity());
        
        if (!stockRestored) {
            log.warn("库存恢复失败：orderNo={}", orderNo);
        }
        
        // 3. 更新订单状态
        order.setStatus("CANCELLED");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        log.info("订单取消成功：orderNo={}", orderNo);
        
        return true;
    }
    
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis();
    }
}
```

---

## 最佳实践

### 1. 服务拆分原则

- **单一职责**：每个服务只负责一个业务领域
- **高内聚低耦合**：服务内部紧密相关，服务之间松散耦合
- **数据独立**：每个服务有独立的数据库
- **接口契约**：使用独立的API模块定义接口契约

### 2. 异常处理

- **业务异常**：定义明确的业务异常，客户端可以捕获并处理
- **系统异常**：统一转换为RPC异常，避免暴露内部实现
- **降级策略**：为RPC客户端提供降级实现（Fallback）

### 3. 超时设置

- **连接超时**：5秒（默认）
- **读取超时**：10-30秒（根据业务复杂度）
- **写入超时**：10秒（默认）
- **长时间任务**：使用异步调用或消息队列

### 4. 重试策略

- **幂等性**：确保重试不会导致数据不一致
- **重试条件**：只重试网络异常和超时，不重试业务异常
- **重试次数**：3次（默认）
- **重试间隔**：递增延迟（1s, 2s, 4s）

### 5. 性能优化

- **连接池**：复用HTTP连接，减少连接开销
- **批量调用**：合并多个请求为一个批量请求
- **异步调用**：非关键路径使用异步RPC
- **缓存**：缓存热点数据，减少RPC调用

### 6. 监控和日志

- **链路追踪**：使用TraceID跟踪跨服务调用
- **性能监控**：记录RPC调用耗时、成功率、失败率
- **日志记录**：记录请求参数、响应结果、异常信息
- **告警机制**：异常率超过阈值时告警

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0
