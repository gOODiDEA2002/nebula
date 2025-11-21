# Nebula Starter Service - 使用示例

> 微服务应用专用Starter的完整使用示例，涵盖RPC调用、服务发现、消息队列、分布式锁等典型微服务场景。

## 示例概览

本文档包含以下示例：

- [示例1：HTTP RPC调用](#示例1http-rpc调用)
- [示例2：gRPC服务](#示例2grpc服务)
- [示例3：服务注册与发现](#示例3服务注册与发现)
- [示例4：消息队列](#示例4消息队列)
- [示例5：分布式锁](#示例5分布式锁)
- [示例6：分布式事务](#示例6分布式事务)
- [示例7：任务调度](#示例7任务调度)
- [票务系统微服务应用](#票务系统微服务应用)

## 前提条件

### 环境要求

- **Java**：21+
- **Maven**：3.8+
- **Spring Boot**：3.2+
- **Nacos**：2.2+（如使用服务发现）
- **RabbitMQ**：3.12+（如使用消息队列）
- **Redis**：7.0+（如使用分布式锁）

### 依赖配置

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

---

## 示例1：HTTP RPC调用

### 场景说明

服务A通过HTTP RPC调用服务B的接口。

### 实现步骤

#### 步骤1：定义RPC接口（服务B）

```java
package com.example.user.api;

import io.nebula.rpc.annotation.RpcService;
import io.nebula.core.model.Result;

/**
 * 用户服务RPC接口
 */
@RpcService(name = "user-service")
public interface UserRpcService {
    
    /**
     * 根据ID查询用户
     */
    Result<User> getById(String userId);
    
    /**
     * 验证用户凭证
     */
    Result<Boolean> validateCredentials(String username, String password);
}
```

#### 步骤2：实现RPC接口（服务B）

```java
package com.example.user.service;

import io.nebula.rpc.annotation.RpcServiceImpl;
import lombok.RequiredArgsConstructor;

/**
 * 用户服务RPC实现
 */
@RpcServiceImpl
@RequiredArgsConstructor
public class UserRpcServiceImpl implements UserRpcService {
    
    private final UserService userService;
    
    @Override
    public Result<User> getById(String userId) {
        User user = userService.getById(userId);
        return Result.success(user);
    }
    
    @Override
    public Result<Boolean> validateCredentials(String username, String password) {
        boolean valid = userService.validateCredentials(username, password);
        return Result.success(valid);
    }
}
```

#### 步骤3：配置RPC服务端（服务B）

`application.yml`:

```yaml
nebula:
  rpc:
    http:
      enabled: true
      port: 8081
```

#### 步骤4：调用RPC接口（服务A）

```java
package com.example.order.service;

import io.nebula.rpc.annotation.RpcClient;
import io.nebula.core.model.Result;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单服务
 */
@Service
@Slf4j
public class OrderService {
    
    @RpcClient(name = "user-service")
    private UserRpcService userRpcService;
    
    public Order createOrder(CreateOrderRequest request) {
        // 1. 验证用户
        Result<User> userResult = userRpcService.getById(request.getUserId());
        if (!userResult.isSuccess()) {
            throw new BusinessException("用户不存在");
        }
        
        User user = userResult.getData();
        log.info("用户信息: {}", user.getUsername());
        
        // 2. 创建订单
        Order order = new Order();
        order.setUserId(user.getId());
        order.setUserName(user.getUsername());
        // ... 其他订单逻辑
        
        return order;
    }
}
```

#### 步骤5：配置RPC客户端（服务A）

`application.yml`:

```yaml
nebula:
  rpc:
    http:
      enabled: true
      services:
        user-service:
          url: http://localhost:8081
```

---

## 示例2：gRPC服务

### 场景说明

使用gRPC实现高性能的服务间通信。

### 实现步骤

#### 步骤1：定义Proto文件

`user.proto`:

```protobuf
syntax = "proto3";

package user;

option java_package = "com.example.user.grpc";
option java_outer_classname = "UserProto";

service UserGrpcService {
  rpc GetUser (UserRequest) returns (UserResponse);
  rpc ValidateCredentials (CredentialsRequest) returns (CredentialsResponse);
}

message UserRequest {
  string user_id = 1;
}

message UserResponse {
  string id = 1;
  string username = 2;
  string email = 3;
}

message CredentialsRequest {
  string username = 1;
  string password = 2;
}

message CredentialsResponse {
  bool valid = 1;
}
```

#### 步骤2：实现gRPC服务

```java
package com.example.user.grpc;

import io.grpc.stub.StreamObserver;
import io.nebula.rpc.grpc.annotation.GrpcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户gRPC服务实现
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcServiceImpl extends UserGrpcServiceGrpc.UserGrpcServiceImplBase {
    
    private final UserService userService;
    
    @Override
    public void getUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        log.info("gRPC调用: getUser({})", request.getUserId());
        
        User user = userService.getById(request.getUserId());
        
        UserResponse response = UserResponse.newBuilder()
            .setId(user.getId())
            .setUsername(user.getUsername())
            .setEmail(user.getEmail())
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void validateCredentials(CredentialsRequest request, 
                                   StreamObserver<CredentialsResponse> responseObserver) {
        boolean valid = userService.validateCredentials(
            request.getUsername(), 
            request.getPassword()
        );
        
        CredentialsResponse response = CredentialsResponse.newBuilder()
            .setValid(valid)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

#### 步骤3：配置gRPC服务端

`application.yml`:

```yaml
nebula:
  rpc:
    grpc:
      enabled: true
      port: 9090
```

#### 步骤4：调用gRPC服务

```java
package com.example.order.service;

import io.nebula.rpc.grpc.annotation.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * 订单服务（使用gRPC客户端）
 */
@Service
public class OrderGrpcService {
    
    @GrpcClient(name = "user-service")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userGrpcStub;
    
    public void createOrder(String userId) {
        // 调用gRPC服务
        UserRequest request = UserRequest.newBuilder()
            .setUserId(userId)
            .build();
        
        UserResponse response = userGrpcStub.getUser(request);
        
        log.info("用户信息: {}", response.getUsername());
    }
}
```

---

## 示例3：服务注册与发现

### 场景说明

使用Nacos实现服务的自动注册与发现。

### 实现步骤

#### 步骤1：配置服务提供者

`application.yml`:

```yaml
spring:
  application:
    name: user-service

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: dev
      group: DEFAULT_GROUP
```

#### 步骤2：配置服务消费者

`application.yml`:

```yaml
spring:
  application:
    name: order-service

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: dev
      group: DEFAULT_GROUP
  
  rpc:
    http:
      enabled: true
      discovery:
        enabled: true  # 启用服务发现
```

#### 步骤3：服务自动发现

```java
@Service
public class OrderService {
    
    // RPC客户端会自动从Nacos获取user-service的地址
    @RpcClient(name = "user-service")
    private UserRpcService userRpcService;
    
    public void process() {
        // 自动调用到正确的服务实例
        Result<User> result = userRpcService.getById("123");
    }
}
```

---

## 示例4：消息队列

### 场景说明

使用RabbitMQ实现异步消息处理。

### 实现步骤

#### 步骤1：定义消息

```java
package com.example.event;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 订单创建事件
 */
@Data
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private LocalDateTime createTime;
}
```

#### 步骤2：发送消息

```java
package com.example.order.service;

import io.nebula.messaging.core.MessageProducer;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final MessageProducer messageProducer;
    
    public Order createOrder(CreateOrderRequest request) {
        // 1. 创建订单
        Order order = new Order();
        order.setId(IdGenerator.snowflakeIdString());
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        orderRepository.save(order);
        
        // 2. 发送订单创建事件
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setAmount(order.getAmount());
        event.setCreateTime(LocalDateTime.now());
        
        messageProducer.send("order.created", event);
        log.info("订单创建事件已发送: {}", order.getId());
        
        return order;
    }
}
```

#### 步骤3：接收消息

```java
package com.example.notification.listener;

import io.nebula.messaging.core.annotation.MessageListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单事件监听器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    
    private final NotificationService notificationService;
    
    /**
     * 监听订单创建事件
     */
    @MessageListener(topic = "order.created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单创建事件: {}", event.getOrderId());
        
        // 发送通知
        notificationService.sendOrderNotification(event);
    }
}
```

#### 步骤4：配置RabbitMQ

`application.yml`:

```yaml
nebula:
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      username: guest
      password: guest
      virtual-host: /
```

---

## 示例5：分布式锁

### 场景说明

使用Redis分布式锁防止并发问题。

### 实现代码

```java
package com.example.order.service;

import io.nebula.lock.annotation.DistributedLock;
import io.nebula.lock.core.LockService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 库存服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    
    private final LockService lockService;
    private final InventoryRepository inventoryRepository;
    
    /**
     * 使用注解方式加锁
     */
    @DistributedLock(key = "inventory:#{productId}", waitTime = 3000, leaseTime = 10000)
    public void deductStock(String productId, int quantity) {
        log.info("扣减库存: productId={}, quantity={}", productId, quantity);
        
        // 查询库存
        Inventory inventory = inventoryRepository.findByProductId(productId);
        if (inventory.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }
        
        // 扣减库存
        inventory.setStock(inventory.getStock() - quantity);
        inventoryRepository.update(inventory);
        
        log.info("库存扣减成功: remaining={}", inventory.getStock());
    }
    
    /**
     * 使用编程方式加锁
     */
    public void deductStockManual(String productId, int quantity) {
        String lockKey = "inventory:" + productId;
        
        boolean locked = lockService.tryLock(lockKey, 3000, 10000);
        if (!locked) {
            throw new BusinessException("获取锁失败，请稍后重试");
        }
        
        try {
            // 执行业务逻辑
            Inventory inventory = inventoryRepository.findByProductId(productId);
            if (inventory.getStock() < quantity) {
                throw new BusinessException("库存不足");
            }
            
            inventory.setStock(inventory.getStock() - quantity);
            inventoryRepository.update(inventory);
        } finally {
            lockService.unlock(lockKey);
        }
    }
}
```

---

## 示例6：分布式事务

### 场景说明

跨服务的订单创建场景，需要保证数据一致性。

### 实现代码

```java
package com.example.order.service;

import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单服务（使用Seata分布式事务）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTxService {
    
    @RpcClient(name = "user-service")
    private UserRpcService userRpcService;
    
    @RpcClient(name = "inventory-service")
    private InventoryRpcService inventoryRpcService;
    
    @RpcClient(name = "payment-service")
    private PaymentRpcService paymentRpcService;
    
    private final OrderRepository orderRepository;
    
    /**
     * 创建订单（分布式事务）
     */
    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        log.info("开始创建订单（分布式事务）: userId={}", request.getUserId());
        
        // 1. 验证用户余额
        Result<Boolean> balanceResult = userRpcService.checkBalance(
            request.getUserId(), 
            request.getAmount()
        );
        if (!balanceResult.getData()) {
            throw new BusinessException("余额不足");
        }
        
        // 2. 扣减库存
        Result<Void> inventoryResult = inventoryRpcService.deductStock(
            request.getProductId(),
            request.getQuantity()
        );
        if (!inventoryResult.isSuccess()) {
            throw new BusinessException("库存扣减失败");
        }
        
        // 3. 创建订单
        Order order = new Order();
        order.setId(IdGenerator.snowflakeIdString());
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
        
        // 4. 扣减余额
        Result<Void> paymentResult = paymentRpcService.deductBalance(
            request.getUserId(),
            request.getAmount()
        );
        if (!paymentResult.isSuccess()) {
            throw new BusinessException("扣减余额失败");
        }
        
        log.info("订单创建成功: orderId={}", order.getId());
        return order;
    }
}
```

---

## 示例7：任务调度

### 场景说明

使用XXL-JOB实现定时任务。

### 实现代码

```java
package com.example.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderJob {
    
    private final OrderService orderService;
    
    /**
     * 取消超时未支付的订单
     */
    @XxlJob("cancelTimeoutOrders")
    public void cancelTimeoutOrders() {
        log.info("开始取消超时订单");
        
        List<Order> timeoutOrders = orderService.findTimeoutOrders();
        log.info("找到{}个超时订单", timeoutOrders.size());
        
        for (Order order : timeoutOrders) {
            try {
                orderService.cancelOrder(order.getId());
                log.info("取消订单成功: {}", order.getId());
            } catch (Exception e) {
                log.error("取消订单失败: {}", order.getId(), e);
            }
        }
        
        log.info("取消超时订单完成");
    }
    
    /**
     * 生成每日报表
     */
    @XxlJob("generateDailyReport")
    public void generateDailyReport() {
        log.info("开始生成每日报表");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        orderService.generateDailyReport(yesterday);
        
        log.info("每日报表生成完成");
    }
}
```

**配置**:

`application.yml`:

```yaml
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      appname: order-service
      port: 9999
```

---

## 票务系统微服务应用

### 微服务架构设计

```
票务系统微服务架构:
├── user-service          # 用户服务
├── movie-service         # 电影服务
├── cinema-service        # 影院服务
├── showtime-service      # 场次服务
├── order-service         # 订单服务
├── payment-service       # 支付服务
├── notification-service  # 通知服务
└── gateway              # 网关服务
```

### 场景1：用户服务

```java
package com.ticketsystem.user;

import io.nebula.rpc.annotation.RpcServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户服务
 */
@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

/**
 * 用户RPC服务实现
 */
@RpcServiceImpl
@RequiredArgsConstructor
public class UserRpcServiceImpl implements UserRpcService {
    
    private final UserService userService;
    
    @Override
    public Result<User> getById(String userId) {
        User user = userService.getById(userId);
        return Result.success(user);
    }
    
    @Override
    public Result<Boolean> checkBalance(String userId, BigDecimal amount) {
        boolean sufficient = userService.checkBalance(userId, amount);
        return Result.success(sufficient);
    }
    
    @Override
    public Result<Void> deductBalance(String userId, BigDecimal amount) {
        userService.deductBalance(userId, amount);
        return Result.success();
    }
}
```

**配置** `application.yml`:

```yaml
spring:
  application:
    name: user-service

server:
  port: 8081

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  rpc:
    http:
      enabled: true
  messaging:
    rabbitmq:
      enabled: true
```

### 场景2：订单服务

```java
package com.ticketsystem.order;

/**
 * 订单服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    @RpcClient(name = "user-service")
    private UserRpcService userRpcService;
    
    @RpcClient(name = "showtime-service")
    private ShowtimeRpcService showtimeRpcService;
    
    private final MessageProducer messageProducer;
    private final LockService lockService;
    private final OrderRepository orderRepository;
    
    /**
     * 创建订单（完整流程）
     */
    @GlobalTransactional(name = "create-ticket-order", rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        String lockKey = "seat:" + request.getShowtimeId() + ":" + String.join(",", request.getSeatIds());
        
        // 1. 获取分布式锁
        boolean locked = lockService.tryLock(lockKey, 3000, 10000);
        if (!locked) {
            throw new BusinessException("座位被锁定，请稍后重试");
        }
        
        try {
            // 2. 验证场次
            Result<Showtime> showtimeResult = showtimeRpcService.getById(request.getShowtimeId());
            if (!showtimeResult.isSuccess()) {
                throw new BusinessException("场次不存在");
            }
            
            // 3. 验证用户余额
            BigDecimal totalAmount = calculateAmount(request);
            Result<Boolean> balanceResult = userRpcService.checkBalance(request.getUserId(), totalAmount);
            if (!balanceResult.getData()) {
                throw new BusinessException("余额不足");
            }
            
            // 4. 锁定座位
            Result<Void> lockSeatsResult = showtimeRpcService.lockSeats(
                request.getShowtimeId(),
                request.getSeatIds()
            );
            if (!lockSeatsResult.isSuccess()) {
                throw new BusinessException("座位锁定失败");
            }
            
            // 5. 创建订单
            Order order = new Order();
            order.setId(IdGenerator.snowflakeIdString());
            order.setUserId(request.getUserId());
            order.setShowtimeId(request.getShowtimeId());
            order.setSeatIds(request.getSeatIds());
            order.setAmount(totalAmount);
            order.setStatus(OrderStatus.PENDING);
            order.setCreateTime(LocalDateTime.now());
            orderRepository.save(order);
            
            // 6. 发送订单创建事件
            OrderCreatedEvent event = new OrderCreatedEvent();
            event.setOrderId(order.getId());
            event.setUserId(order.getUserId());
            event.setAmount(totalAmount);
            messageProducer.send("order.created", event);
            
            log.info("订单创建成功: orderId={}", order.getId());
            return order;
            
        } finally {
            lockService.unlock(lockKey);
        }
    }
    
    /**
     * 支付订单
     */
    @GlobalTransactional(name = "pay-order", rollbackFor = Exception.class)
    public void payOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单状态不正确");
        }
        
        // 1. 扣减余额
        Result<Void> deductResult = userRpcService.deductBalance(order.getUserId(), order.getAmount());
        if (!deductResult.isSuccess()) {
            throw new BusinessException("扣减余额失败");
        }
        
        // 2. 确认座位
        Result<Void> confirmResult = showtimeRpcService.confirmSeats(
            order.getShowtimeId(),
            order.getSeatIds()
        );
        if (!confirmResult.isSuccess()) {
            throw new BusinessException("确认座位失败");
        }
        
        // 3. 更新订单状态
        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        orderRepository.update(order);
        
        // 4. 发送支付成功事件
        OrderPaidEvent event = new OrderPaidEvent();
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        messageProducer.send("order.paid", event);
        
        log.info("订单支付成功: orderId={}", orderId);
    }
}
```

### 场景3：通知服务

```java
package com.ticketsystem.notification;

/**
 * 通知服务 - 监听订单事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SmsService smsService;
    
    /**
     * 监听订单创建事件
     */
    @MessageListener(topic = "order.created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单创建事件: orderId={}", event.getOrderId());
        
        // 发送短信提醒
        smsService.send(event.getUserId(), "您的订单已创建，请在15分钟内完成支付");
    }
    
    /**
     * 监听订单支付事件
     */
    @MessageListener(topic = "order.paid")
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("收到订单支付事件: orderId={}", event.getOrderId());
        
        // 发送电子票
        emailService.sendTicket(event.getUserId(), event.getOrderId());
        
        // 发送短信通知
        smsService.send(event.getUserId(), "您的订单支付成功，电子票已发送到邮箱");
    }
}
```

---

## 最佳实践

### 实践1：服务拆分原则

- 按业务领域拆分
- 保持服务独立性
- 避免服务间循环依赖
- 控制服务粒度

### 实践2：RPC调用优化

- 使用gRPC提升性能
- 启用服务发现
- 实现服务降级
- 添加超时控制

### 实践3：分布式锁使用

- 锁粒度要细
- 设置合理的超时时间
- 使用try-finally确保释放锁
- 避免死锁

### 实践4：消息队列使用

- 消息幂等性处理
- 消息重试机制
- 死信队列处理
- 消息持久化

---

## 完整示例项目

参考示例项目：
- `examples/nebula-example-user-service` - 用户微服务
- `examples/nebula-example-order-service` - 订单微服务

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划
- [RPC文档](../../infrastructure/rpc/nebula-rpc-core/README.md) - RPC模块详细文档
- [服务发现文档](../../infrastructure/discovery/nebula-discovery-core/README.md) - 服务发现详细文档
- [消息队列文档](../../infrastructure/messaging/nebula-messaging-core/README.md) - 消息队列详细文档

---

> 如有问题或建议，欢迎提Issue。

