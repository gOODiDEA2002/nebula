# Nebula 微服务示例

本示例演示如何使用 `nebula-starter-service` 创建一个完整的微服务架构应用。

## 架构概览

```
微服务架构
├── gateway-service        (网关服务)
├── user-service           (用户服务)
├── order-service          (订单服务)
├── payment-service        (支付服务)
└── common-api             (共享 API 定义)
```

## 项目结构

```
ecommerce-platform/
├── pom.xml                          # 父 POM
├── common-api/                      # 共享 API
│   ├── pom.xml
│   └── src/main/java/
│       └── com/example/api/
│           ├── user/
│           │   ├── UserApi.java
│           │   └── dto/
│           └── order/
│               ├── OrderApi.java
│               └── dto/
├── user-service/                    # 用户服务
│   ├── pom.xml
│   └── src/main/java/
│       └── com/example/user/
│           ├── UserServiceApplication.java
│           ├── controller/
│           ├── service/
│           └── repository/
├── order-service/                   # 订单服务
│   ├── pom.xml
│   └── src/main/java/
│       └── com/example/order/
│           ├── OrderServiceApplication.java
│           ├── controller/
│           ├── service/
│           └── repository/
└── gateway-service/                 # 网关服务
    ├── pom.xml
    └── src/main/java/
        └── com/example/gateway/
            └── GatewayApplication.java
```

## 1. 父 POM 配置

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>ecommerce-platform</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.12</version>
    </parent>

    <properties>
        <java.version>21</java.version>
        <nebula.version>2.0.1-SNAPSHOT</nebula.version>
    </properties>

    <modules>
        <module>common-api</module>
        <module>user-service</module>
        <module>order-service</module>
        <module>gateway-service</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- 项目内部依赖 -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>common-api</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

## 2. 共享 API 模块

### common-api/pom.xml

```xml
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>common-api</artifactId>

    <dependencies>
        <!-- Nebula API Starter -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-api</artifactId>
            <version>${nebula.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

### UserApi.java

```java
package com.example.api.user;

import com.example.api.user.dto.UserDTO;
import io.nebula.foundation.common.Result;
import io.nebula.rpc.core.annotation.RpcService;
import org.springframework.web.bind.annotation.*;

@RpcService(value = "userService", protocol = "http")
public interface UserApi {

    @GetMapping("/api/users/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);

    @PostMapping("/api/users")
    Result<UserDTO> createUser(@RequestBody UserDTO user);

    @PutMapping("/api/users/{id}")
    Result<UserDTO> updateUser(@PathVariable("id") Long id, @RequestBody UserDTO user);

    @DeleteMapping("/api/users/{id}")
    Result<Void> deleteUser(@PathVariable("id") Long id);
}
```

### UserDTO.java

```java
package com.example.api.user.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {
    
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String nickname;
    
    private String avatar;
    
    private LocalDateTime createTime;
}
```

### OrderApi.java

```java
package com.example.api.order;

import com.example.api.order.dto.OrderDTO;
import io.nebula.foundation.common.Result;
import io.nebula.rpc.core.annotation.RpcService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RpcService(value = "orderService", protocol = "http")
public interface OrderApi {

    @PostMapping("/api/orders")
    Result<OrderDTO> createOrder(@RequestBody OrderDTO order);

    @GetMapping("/api/orders/{id}")
    Result<OrderDTO> getOrderById(@PathVariable("id") Long id);

    @GetMapping("/api/orders/user/{userId}")
    Result<List<OrderDTO>> getOrdersByUserId(@PathVariable("userId") Long userId);

    @PutMapping("/api/orders/{id}/cancel")
    Result<Void> cancelOrder(@PathVariable("id") Long id);
}
```

## 3. 用户服务

### user-service/pom.xml

```xml
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>user-service</artifactId>

    <dependencies>
        <!-- Nebula Service Starter -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-service</artifactId>
            <version>${nebula.version}</version>
        </dependency>

        <!-- 共享 API -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-api</artifactId>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### application.yml

```yaml
spring:
  application:
    name: user-service
  
  datasource:
    url: jdbc:mysql://localhost:3306/user_db
    username: root
    password: ${DB_PASSWORD}

server:
  port: 8081

# Nebula 配置
nebula:
  # 服务发现
  discovery:
    enabled: true
    nacos:
      server-addr: localhost:8848
      namespace: ecommerce
      group: DEFAULT_GROUP

  # RPC 配置
  rpc:
    http:
      enabled: true

  # 消息队列
  messaging:
    rabbitmq:
      host: localhost
      port: 5672
      username: guest
      password: guest

  # 缓存
  data:
    cache:
      enabled: true
      redis:
        enabled: true
        host: localhost
        port: 6379
        key-prefix: "user:"

  # 分布式锁
  lock:
    redis:
      enabled: true

logging:
  level:
    com.example.user: DEBUG
```

### UserServiceApplication.java

```java
package com.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### UserController.java

```java
package com.example.user.controller;

import com.example.api.user.UserApi;
import com.example.api.user.dto.UserDTO;
import com.example.user.service.UserService;
import io.nebula.foundation.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public Result<UserDTO> getUserById(Long id) {
        return userService.getUserById(id)
                .map(Result::success)
                .orElse(Result.error("用户不存在"));
    }

    @Override
    public Result<UserDTO> createUser(UserDTO user) {
        UserDTO created = userService.createUser(user);
        return Result.success(created);
    }

    @Override
    public Result<UserDTO> updateUser(Long id, UserDTO user) {
        user.setId(id);
        return userService.updateUser(user)
                .map(Result::success)
                .orElse(Result.error("更新失败"));
    }

    @Override
    public Result<Void> deleteUser(Long id) {
        boolean success = userService.deleteUser(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
```

### UserService.java

```java
package com.example.user.service;

import com.example.api.user.dto.UserDTO;
import com.example.user.model.User;
import com.example.user.repository.UserRepository;
import io.nebula.data.cache.CacheManager;
import io.nebula.lock.core.DistributedLock;
import io.nebula.messaging.core.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private final DistributedLock distributedLock;
    private final MessagePublisher messagePublisher;

    private static final String CACHE_KEY_PREFIX = "user:";
    private static final String LOCK_KEY_PREFIX = "user:lock:";

    public Optional<UserDTO> getUserById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 从缓存获取
        Optional<UserDTO> cached = cacheManager.get(cacheKey, UserDTO.class);
        if (cached.isPresent()) {
            return cached;
        }
        
        // 从数据库查询
        User user = userRepository.selectById(id);
        if (user != null) {
            UserDTO dto = convertToDTO(user);
            cacheManager.set(cacheKey, dto, 3600);
            return Optional.of(dto);
        }
        
        return Optional.empty();
    }

    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        String lockKey = LOCK_KEY_PREFIX + userDTO.getUsername();
        
        // 使用分布式锁防止并发创建
        return distributedLock.tryLock(lockKey, 10, () -> {
            User user = convertToEntity(userDTO);
            userRepository.insert(user);
            
            UserDTO created = convertToDTO(user);
            
            // 发布用户创建事件
            messagePublisher.publish("user.created", created);
            
            return created;
        });
    }

    @Transactional
    public Optional<UserDTO> updateUser(UserDTO userDTO) {
        User user = convertToEntity(userDTO);
        int updated = userRepository.updateById(user);
        
        if (updated > 0) {
            // 清除缓存
            String cacheKey = CACHE_KEY_PREFIX + userDTO.getId();
            cacheManager.delete(cacheKey);
            
            // 发布用户更新事件
            messagePublisher.publish("user.updated", userDTO);
            
            return Optional.of(userDTO);
        }
        
        return Optional.empty();
    }

    @Transactional
    public boolean deleteUser(Long id) {
        int deleted = userRepository.deleteById(id);
        
        if (deleted > 0) {
            // 清除缓存
            String cacheKey = CACHE_KEY_PREFIX + id;
            cacheManager.delete(cacheKey);
            
            // 发布用户删除事件
            messagePublisher.publish("user.deleted", id);
            
            return true;
        }
        
        return false;
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setCreateTime(user.getCreateTime());
        return dto;
    }

    private User convertToEntity(UserDTO dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        return user;
    }
}
```

## 4. 订单服务

### order-service/pom.xml

```xml
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>order-service</artifactId>

    <dependencies>
        <!-- Nebula Service Starter -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-service</artifactId>
            <version>${nebula.version}</version>
        </dependency>

        <!-- 共享 API -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-api</artifactId>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### application.yml

```yaml
spring:
  application:
    name: order-service

  datasource:
    url: jdbc:mysql://localhost:3306/order_db
    username: root
    password: ${DB_PASSWORD}

server:
  port: 8082

nebula:
  discovery:
    enabled: true
    nacos:
      server-addr: localhost:8848
      namespace: ecommerce

  rpc:
    http:
      enabled: true

  messaging:
    rabbitmq:
      host: localhost
      port: 5672

  data:
    cache:
      enabled: true
      redis:
        host: localhost
        port: 6379
        key-prefix: "order:"

  lock:
    redis:
      enabled: true
```

### OrderServiceApplication.java

```java
package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### OrderController.java

```java
package com.example.order.controller;

import com.example.api.order.OrderApi;
import com.example.api.order.dto.OrderDTO;
import com.example.order.service.OrderService;
import io.nebula.foundation.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    public Result<OrderDTO> createOrder(OrderDTO order) {
        OrderDTO created = orderService.createOrder(order);
        return Result.success(created);
    }

    @Override
    public Result<OrderDTO> getOrderById(Long id) {
        return orderService.getOrderById(id)
                .map(Result::success)
                .orElse(Result.error("订单不存在"));
    }

    @Override
    public Result<List<OrderDTO>> getOrdersByUserId(Long userId) {
        List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
        return Result.success(orders);
    }

    @Override
    public Result<Void> cancelOrder(Long id) {
        boolean success = orderService.cancelOrder(id);
        return success ? Result.success() : Result.error("取消失败");
    }
}
```

### OrderService.java

```java
package com.example.order.service;

import com.example.api.order.dto.OrderDTO;
import com.example.api.user.UserApi;
import com.example.api.user.dto.UserDTO;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import io.nebula.foundation.common.Result;
import io.nebula.messaging.core.MessagePublisher;
import io.nebula.rpc.core.client.RpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;
    private final RpcClient rpcClient;

    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // 调用用户服务验证用户
        UserApi userApi = rpcClient.create(UserApi.class);
        Result<UserDTO> userResult = userApi.getUserById(orderDTO.getUserId());
        
        if (!userResult.isSuccess()) {
            throw new RuntimeException("用户不存在");
        }
        
        // 创建订单
        Order order = convertToEntity(orderDTO);
        orderRepository.insert(order);
        
        OrderDTO created = convertToDTO(order);
        
        // 发布订单创建事件
        messagePublisher.publish("order.created", created);
        
        return created;
    }

    public Optional<OrderDTO> getOrderById(Long id) {
        Order order = orderRepository.selectById(id);
        return Optional.ofNullable(order).map(this::convertToDTO);
    }

    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.selectByUserId(userId);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean cancelOrder(Long id) {
        Order order = orderRepository.selectById(id);
        if (order != null && "PENDING".equals(order.getStatus())) {
            order.setStatus("CANCELLED");
            orderRepository.updateById(order);
            
            // 发布订单取消事件
            messagePublisher.publish("order.cancelled", id);
            
            return true;
        }
        return false;
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setAmount(order.getAmount());
        dto.setStatus(order.getStatus());
        dto.setCreateTime(order.getCreateTime());
        return dto;
    }

    private Order convertToEntity(OrderDTO dto) {
        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setAmount(dto.getAmount());
        order.setStatus("PENDING");
        return order;
    }
}
```

## 5. 网关服务

### gateway-service/pom.xml

```xml
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>gateway-service</artifactId>

    <dependencies>
        <!-- Spring Cloud Gateway -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>

        <!-- Nebula Foundation -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-foundation</artifactId>
            <version>${nebula.version}</version>
        </dependency>

        <!-- Nacos Discovery -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-discovery-nacos</artifactId>
            <version>${nebula.version}</version>
        </dependency>
    </dependencies>
</project>
```

### application.yml

```yaml
spring:
  application:
    name: gateway-service
  
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**

server:
  port: 8080

nebula:
  discovery:
    enabled: true
    nacos:
      server-addr: localhost:8848
      namespace: ecommerce
```

## 6. 运行微服务集群

### 启动 Nacos

```bash
docker run -d --name nacos \
  -p 8848:8848 \
  -e MODE=standalone \
  nacos/nacos-server:latest
```

### 启动 RabbitMQ

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:management
```

### 启动 Redis

```bash
docker run -d --name redis \
  -p 6379:6379 \
  redis:latest
```

### 启动 MySQL

```bash
docker run -d --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=your_password \
  mysql:8.0
```

### 启动服务

```bash
# 启动用户服务
cd user-service
mvn spring-boot:run

# 启动订单服务
cd order-service
mvn spring-boot:run

# 启动网关服务
cd gateway-service
mvn spring-boot:run
```

## 7. 测试微服务

```bash
# 通过网关创建用户
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "email": "john@example.com"}'

# 通过网关创建订单
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "amount": 99.99}'

# 查询用户订单
curl http://localhost:8080/api/orders/user/1
```

## 8. 核心特性

### ✅ 服务发现（Nacos）
- 自动服务注册
- 健康检查
- 负载均衡

### ✅ RPC 调用
- HTTP RPC 支持
- gRPC 支持
- 服务间通信

### ✅ 消息队列（RabbitMQ）
- 异步事件发布
- 消息订阅
- 事件驱动架构

### ✅ 分布式锁（Redis）
- 防止并发冲突
- 资源互斥访问

### ✅ 分布式缓存（Redis）
- 跨服务缓存共享
- 缓存一致性

---

**完整代码**: 参见 `nebula-example-user-service` 和 `nebula-example-order-service`
**相关文档**:
- [服务发现文档](../nebula-discovery-core/README.md)
- [RPC 文档](../nebula-rpc-core/README.md)
- [消息队列文档](../nebula-messaging-core/README.md)

