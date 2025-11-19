# Nebula RPC gRPC 使用示例

> **核心特性**：泛化调用、自动服务发现、声明式接口、支持熔断降级。

## 1. 快速开始 (Quick Start)

### 引入依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-grpc</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 启用 RPC 客户端

在 Spring Boot 启动类上添加 `@EnableRpcClients` 注解：

```java
@SpringBootApplication
@EnableRpcClients(basePackages = "io.nebula.example.api") // 指定接口扫描路径
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 2. 配置示例 (Configuration)

### application.yml

```yaml
nebula:
  rpc:
    grpc:
      enabled: true
      server:
        port: 9090
      client:
        # 全局超时设置
        connect-timeout: 3000
        request-timeout: 5000
```

## 3. 代码示例 (Code Examples)

### 场景 1：定义服务接口

```java
/**
 * 用户服务接口
 * value = "user-service": 指定服务名，配合 Nacos 使用
 * fallback = UserServiceFallback.class: 指定降级类
 */
@RpcClient(value = "user-service", fallback = UserServiceFallback.class)
public interface UserService {
    
    UserDTO getUserById(Long id);
    
    // 自定义超时时间 (覆盖全局配置)
    @RpcCall(timeout = 10000) 
    Long createUser(UserDTO user);
}
```

### 场景 2：服务降级实现

```java
@Component
public class UserServiceFallback implements UserService {
    @Override
    public UserDTO getUserById(Long id) {
        return new UserDTO(-1L, "默认用户"); // 降级返回
    }

    @Override
    public Long createUser(UserDTO user) {
        return -1L; // 降级返回
    }
}
```

### 场景 3：服务端实现

```java
@Service
@RpcService // 自动注册为 RPC 服务
public class UserServiceImpl implements UserService {
    // 业务逻辑...
}
```

### 场景 4：开发调试 (直连模式)

在开发阶段，可以通过 `url` 属性绕过服务发现，直接连接本地服务：

```java
@RpcClient(name = "user-service", url = "static://localhost:9090")
public interface UserService {
    // ...
}
```

## 4. 最佳实践 (Best Practices)

1.  **接口包管理**：将 `@RpcClient` 接口、DTO 类放在独立的 Maven Module 中，供服务端和客户端引用。
2.  **异常处理**：框架会自动传播异常信息，建议在 Client 端使用全局异常处理器捕获 `RpcException`。
3.  **超时控制**：合理设置 `connectTimeout` 和 `readTimeout`，避免雪崩效应。
