# nebula-rpc-core 模块示例

## 模块简介

`nebula-rpc-core` 模块定义了 Nebula 框架的 RPC（远程过程调用）核心抽象。它提供了一套声明式的编程模型，通过注解轻松定义客户端和服务端，支持多种传输协议（如 HTTP, gRPC）的实现。

核心组件包括：
- **@RpcClient**: 客户端接口注解，用于声明远程服务调用。
- **@RpcService**: 服务端实现注解，用于发布服务。
- **@RpcCall**: 方法级别注解，定义调用的具体细节（如路径、方法）。
- **@EnableRpcClients**: 启用 RPC 客户端扫描。

## 核心功能示例

### 1. 定义 RPC 接口

创建一个 Java 接口，并使用 `@RpcClient` 注解。

**`io.nebula.example.rpc.api.UserService`**:

```java
package io.nebula.example.rpc.api;

import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.example.rpc.dto.UserDto;

import java.util.List;

/**
 * 用户服务接口
 * value = "user-service": 对应注册中心的服务名
 * connectTimeout: 连接超时时间
 * readTimeout: 读取超时时间
 */
@RpcClient(value = "user-service", connectTimeout = 2000, readTimeout = 5000)
public interface UserService {

    /**
     * 默认 POST 请求，路径由方法名推断或实现类决定
     */
    @RpcCall
    UserDto getUserById(Long id);

    /**
     * 指定路径和请求方法
     */
    @RpcCall(path = "/users", method = "POST")
    Long createUser(UserDto user);

    /**
     * 查询列表
     */
    @RpcCall(path = "/users/list", method = "GET")
    List<UserDto> listUsers();
}
```

### 2. 实现 RPC 服务端

在服务提供方，实现上述接口并使用 `@RpcService` 标记。

**`io.nebula.example.rpc.provider.UserServiceImpl`**:

```java
package io.nebula.example.rpc.provider;

import io.nebula.example.rpc.api.UserService;
import io.nebula.example.rpc.dto.UserDto;
import io.nebula.rpc.core.annotation.RpcService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RpcService(UserService.class) // 声明实现了 UserService 接口
public class UserServiceImpl implements UserService {

    @Override
    public UserDto getUserById(Long id) {
        // 业务逻辑
        return new UserDto(id, "User-" + id);
    }

    @Override
    public Long createUser(UserDto user) {
        // 业务逻辑
        return 1001L;
    }

    @Override
    public List<UserDto> listUsers() {
        return new ArrayList<>();
    }
}
```

### 3. 调用 RPC 客户端

在消费方，使用 `@EnableRpcClients` 扫描接口，并直接注入使用。

**`io.nebula.example.rpc.consumer.ConsumerApplication`**:

```java
package io.nebula.example.rpc.consumer;

import io.nebula.example.rpc.api.UserService;
import io.nebula.example.rpc.dto.UserDto;
import io.nebula.rpc.core.annotation.EnableRpcClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableRpcClients(basePackages = "io.nebula.example.rpc.api")
@RestController
public class ConsumerApplication {

    @Autowired
    private UserService userService;

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @GetMapping("/test")
    public UserDto test() {
        // 像调用本地方法一样调用远程服务
        return userService.getUserById(1L);
    }
}
```

## 进阶特性

### 1. 服务降级 (Fallback)

`@RpcClient` 支持 `fallback` 属性，用于在调用失败或超时时执行降级逻辑。

```java
@RpcClient(value = "user-service", fallback = UserServiceFallback.class)
public interface UserService { ... }

@Component
public class UserServiceFallback implements UserService {
    @Override
    public UserDto getUserById(Long id) {
        return new UserDto(-1L, "Fallback User");
    }
    // ...
}
```

### 2. 直连模式

在开发调试或特殊场景下，可以绕过注册中心，直接指定 URL。

```java
@RpcClient(url = "http://localhost:8080")
public interface DebugService { ... }
```

## 总结

`nebula-rpc-core` 提供了类似 Feign 的声明式调用体验，极大地简化了微服务间的通信代码。配合 `nebula-rpc-http` 或 `nebula-rpc-grpc` 模块，可以灵活选择底层传输协议。

