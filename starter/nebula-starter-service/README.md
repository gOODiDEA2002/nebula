# Nebula Starter Service

微服务应用专用Starter，集成RPC、服务发现、消息队列等微服务能力。

## 适用场景

- 微服务应用
- 分布式系统
- RPC服务
- 事件驱动架构
- 服务网格

## 包含模块

继承`nebula-starter-web`的所有功能，额外包含:

- `nebula-rpc-core` + `nebula-rpc-http` - RPC调用
- `nebula-rpc-grpc` - gRPC支持(可选)
- `nebula-discovery-core` + `nebula-discovery-nacos` - 服务发现(可选)
- `nebula-messaging-core` + `nebula-messaging-rabbitmq` - 消息队列(可选)
- `nebula-lock-redis` - 分布式锁
- `nebula-task` - 任务调度(可选)

## 功能特性

### 继承自Web
- 所有Web功能 (REST API, JWT, 缓存等)

### 微服务能力
- HTTP RPC客户端
- gRPC服务端/客户端
- Nacos服务注册发现
- RabbitMQ消息队列
- Redis分布式锁
- XXL-JOB任务调度

## 内存占用

**~800MB** (包含所有微服务组件)

## 快速开始

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  
  rpc:
    http:
      enabled: true
    grpc:
      enabled: true
      port: 9090
  
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
```

## 项目结构规范

使用 `nebula-starter-service` 的微服务项目应遵循以下结构：

```
your-project/
  your-module-api/           # API契约模块（接口定义）
    src/main/java/.../api/
      - *RpcClient.java      # RPC接口定义，使用@RpcClient和@RpcCall注解
      - dto/                 # 数据传输对象
      - vo/                  # 视图对象
  your-module/               # 服务实现模块
    src/main/java/.../
      - rpc/
        - *RpcClientImpl.java  # RPC接口实现，使用@RpcService注解
      - service/             # 业务逻辑层
      - mapper/              # 数据访问层
      - entity/              # 实体类
      - config/              # 配置类
      - message/             # 消息处理器（可选）
      - task/                # 定时任务（可选）
```

**重要**：Service模块**不应该包含Controller**，所有对外接口都通过RpcClient接口暴露。

## RPC接口开发规范

### 1. API模块定义RpcClient接口

在 `*-api` 模块中定义RPC接口：

```java
package io.nebula.example.user.api;

import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RpcClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 用户服务RPC接口
 */
@RpcClient
public interface UserRpcClient {
    
    /**
     * 创建用户
     */
    @RpcCall(value = "/rpc/users", method = "POST")
    CreateUserDto.Response createUser(@RequestBody CreateUserDto.Request request);
    
    /**
     * 根据ID获取用户
     */
    @RpcCall(value = "/rpc/users/{userId}", method = "GET")
    UserVo getUserById(@PathVariable("userId") Long userId);
    
    /**
     * 获取用户列表
     */
    @RpcCall(value = "/rpc/users/list", method = "POST")
    UserListDto.Response listUsers(@RequestBody UserListDto.Request request);
}
```

**关键点**：
- 使用 `@RpcClient` 注解标记接口
- 使用 `@RpcCall` 注解定义HTTP路由（路径建议以 `/rpc/` 开头）
- 使用 `@RequestBody` 标记请求体参数
- 使用 `@PathVariable` 标记路径参数
- 使用 `@RequestParam` 标记查询参数

### 2. Service模块实现RpcClient接口

在 Service 模块的 `rpc/` 包中实现接口：

```java
package io.nebula.example.user.service.rpc;

import io.nebula.rpc.core.annotation.RpcService;
import io.nebula.example.user.api.UserRpcClient;
import io.nebula.example.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户RPC服务实现
 */
@Slf4j
@RpcService  // 无需指定接口类，框架自动推导
@RequiredArgsConstructor
public class UserRpcClientImpl implements UserRpcClient {
    
    private final UserService userService;
    
    @Override
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        log.info("RPC调用: 创建用户 username={}", request.getUsername());
        return userService.createUser(request);
    }
    
    @Override
    public UserVo getUserById(Long userId) {
        log.info("RPC调用: 获取用户 userId={}", userId);
        return userService.getUserById(userId);
    }
    
    @Override
    public UserListDto.Response listUsers(UserListDto.Request request) {
        log.info("RPC调用: 获取用户列表");
        return userService.listUsers(request);
    }
}
```

**关键点**：
- 使用 `@RpcService` 注解标记实现类（无需指定接口类）
- **不要使用** `@RestController` 或 `@Controller`
- 实现类中的方法签名必须与接口完全一致
- 框架会自动注册HTTP和gRPC两种协议的端点

### 3. 服务间调用

在其他服务中调用RpcClient：

```java
package io.nebula.example.order.service.impl;

import io.nebula.example.user.api.UserRpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    // 直接注入RpcClient，框架自动创建代理
    private final UserRpcClient userRpcClient;
    
    @Override
    public OrderVo createOrder(CreateOrderDto.Request request) {
        // 调用用户服务验证用户
        UserVo user = userRpcClient.getUserById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // ... 创建订单逻辑
    }
}
```

## 协议支持

同一个RpcClient接口实现**同时支持HTTP和gRPC两种协议**：

- **HTTP调用**：通过 `@RpcCall` 注解定义的路由访问
- **gRPC调用**：基于方法签名自动生成

客户端调用时，框架会根据配置自动选择协议：
- 如果启用了gRPC且目标服务支持，优先使用gRPC
- 否则回退到HTTP

## 常见问题

### 为什么Service模块不应该有Controller？

1. **协议无关性**：RpcClient接口同时支持HTTP和gRPC，Controller只支持HTTP
2. **契约优先**：API模块定义的接口就是服务契约，直接实现更清晰
3. **减少冗余**：避免在Controller中重复编写路由映射
4. **便于治理**：统一的RPC接口更便于服务治理和监控

### @RpcCall的路由会暴露为HTTP端点吗？

是的，框架会自动将 `@RpcCall` 定义的路由注册为HTTP端点，无需额外配置。

### 如何同时提供REST API和RPC接口？

如果需要提供面向前端的REST API，建议创建独立的Gateway模块：

```
your-project/
  your-module-api/      # RPC接口定义
  your-module/          # 服务实现（仅实现RpcClient）
  your-gateway/         # API网关（可以有Controller，调用各服务的RpcClient）
```

## 参考文档

- [RPC模块文档](../../infrastructure/rpc/nebula-rpc-core/README.md)
- [服务发现文档](../../infrastructure/discovery/nebula-discovery-core/README.md)
- [消息队列文档](../../infrastructure/messaging/nebula-messaging-core/README.md)

---

**版本**: 2.0.1-SNAPSHOT
