# nebula-example-api 架构说明

## 核心问题: 如何同时支持 HTTP 和 gRPC?

### 简单回答: **实体只定义一次,自动支持两种协议**

## 架构设计

```
┌──────────────────────────────────────────────────────────────────┐
│  nebula-example-api (API 定义模块)                                 │
│  ├── UserRpcService (接口)                                        │
│  │   └── @RpcClient 注解                                          │
│  ├── CreateUserDto (DTO)                                          │
│  │   ├── Request                                                  │
│  │   └── Response                                                 │
│  └── GetUserDto, UpdateUserDto, DeleteUserDto... (其他 DTOs)      │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ (依赖)
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│  nebula-example (服务实现模块)                                     │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  UserRpcServiceImpl (唯一实现)                              │  │
│  │  └── @RpcService 注解                                        │  │
│  │  └── implements UserRpcService                              │  │
│  └───────────────────────────────────────────────────────────┘  │
│                   │                     │                         │
│                   │                     │                         │
│          ┌────────┘                     └────────┐                │
│          ↓                                       ↓                │
│  ┌──────────────────┐              ┌──────────────────┐          │
│  │  HTTP RPC 服务器  │              │  gRPC 服务器      │          │
│  │  端口: 8000       │              │  端口: 9090       │          │
│  │  协议: HTTP/1.1   │              │  协议: HTTP/2     │          │
│  │  格式: JSON       │              │  格式: Protobuf   │          │
│  └──────────────────┘              └──────────────────┘          │
└──────────────────────────────────────────────────────────────────┘
                   │                                │
                   │ (客户端调用)                    │
                   ↓                                ↓
          ┌──────────────────┐          ┌──────────────────┐
          │  HttpRpcClient    │          │  GrpcRpcClient    │
          │  (HTTP + JSON)    │          │  (gRPC + Protobuf)│
          └──────────────────┘          └──────────────────┘
```

## 关键点

### 1. ✅ 只定义一次

```java
// nebula-example-api/src/main/java/io/nebula/example/api/rpc/UserRpcService.java

@RpcClient(value = "nebula-example", name = "user-rpc-service")
public interface UserRpcService {
    @RpcCall(value = "/rpc/users", method = "POST")
    CreateUserDto.Response createUser(@RequestBody CreateUserDto.Request request);
}

// nebula-example-api/src/main/java/io/nebula/example/api/dto/CreateUserDto.java

public class CreateUserDto {
    @Data
    public static class Request {
        @NotBlank private String username;
        @NotBlank private String name;
        @Email private String email;
        private String phone;
        private String status;
    }
    
    @Data
    public static class Response {
        private Long id;
    }
}
```

**不需要额外的 `.proto` 文件,不需要重复定义 DTOs!**

### 2. ✅ 只实现一次

```java
// nebula-example/src/main/java/.../UserRpcServiceImpl.java

@RpcService  // 这个注解会自动注册到 HTTP 和 gRPC 服务器
@Component
public class UserRpcServiceImpl implements UserRpcService {
    @Autowired
    private RpcDemoService rpcDemoService;
    
    @Override
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        // 同一个实现,同时服务于 HTTP 和 gRPC!
        return rpcDemoService.createUser(request);
    }
}
```

### 3. ✅ 自动协议转换

#### HTTP RPC 流程
```
JSON 请求 
  → Jackson 反序列化 
  → Java 对象 (CreateUserDto.Request)
  → UserRpcServiceImpl.createUser()
  → Java 对象 (CreateUserDto.Response)
  → Jackson 序列化 
  → JSON 响应
```

#### gRPC 流程
```
Protobuf bytes 请求 
  → ObjectMapper 反序列化 
  → Java 对象 (CreateUserDto.Request)
  → UserRpcServiceImpl.createUser() (同一个方法!)
  → Java 对象 (CreateUserDto.Response)
  → ObjectMapper 序列化 
  → Protobuf bytes 响应
```

### 4. ✅ 通用 Protobuf 定义

我们使用一个**通用的** `rpc_common.proto`,而不是为每个服务定义单独的 `.proto` 文件:

```protobuf
// nebula-rpc-grpc/src/main/proto/rpc_common.proto

syntax = "proto3";

message RpcRequest {
  string service_name = 1;        // "io.nebula.example.api.rpc.UserRpcService"
  string method_name = 2;         // "createUser"
  repeated bytes args = 3;        // [序列化的 CreateUserDto.Request]
  repeated string arg_types = 4;  // ["io.nebula.example.api.dto.CreateUserDto$Request"]
}

message RpcResponse {
  bool success = 1;
  bytes result = 2;               // 序列化的 CreateUserDto.Response
  string error_message = 3;
  string error_code = 4;
}
```

**优势**:
- 不需要为每个 RPC 服务写 `.proto` 文件
- Java 类型信息保留在 `arg_types` 中
- `bytes` 字段可以容纳任意 Java 对象

## 服务端自动注册机制

```java
// nebula-rpc-http/src/main/java/.../RpcServiceRegistrationProcessor.java

@Component
public class RpcServiceRegistrationProcessor implements BeanPostProcessor {
    @Autowired
    private HttpRpcServer httpRpcServer;
    
    @Autowired(required = false)
    private GrpcRpcServer grpcRpcServer;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            // 自动注册到 HTTP RPC 服务器
            httpRpcServer.registerService(serviceInterface, bean);
            
            // 自动注册到 gRPC 服务器 (如果启用)
            if (grpcRpcServer != null) {
                grpcRpcServer.registerService(serviceInterface, bean);
            }
        }
        return bean;
    }
}
```

**一个 `@RpcService` 注解,同时注册到两个服务器!**

## 客户端协议选择

```yaml
# application.yml

nebula:
  rpc:
    http:
      enabled: true    # 启用 HTTP RPC
    grpc:
      enabled: true    # 启用 gRPC
```

当两者都启用时,Spring 会根据 `@Primary` 注解选择 gRPC 客户端(性能更好):

```java
// nebula-rpc-grpc/.../GrpcRpcAutoConfiguration.java

@Bean("grpcRpcClient")
@Primary  // 优先使用 gRPC
public RpcClient grpcRpcClient() {
    return new GrpcRpcClient(...);
}
```

## 实际使用示例

### 服务提供方 (nebula-example)

```java
// 1. 依赖 nebula-example-api
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-example-api</artifactId>
</dependency>

// 2. 实现接口
@RpcService
@Component
public class UserRpcServiceImpl implements UserRpcService {
    // 实现所有方法
}

// 3. 自动注册到 HTTP 和 gRPC 服务器 ✅
```

### 服务消费方 (nebula-example-client 或其他服务)

```java
// 1. 依赖 nebula-example-api
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-example-api</artifactId>
</dependency>

// 2. 启用 RPC 客户端扫描
@SpringBootApplication
@EnableRpcClients  // 扫描 @RpcClient 注解
public class ClientApplication { }

// 3. 直接注入使用
@RestController
public class MyController {
    @Autowired
    private UserRpcService userRpcService;  // 自动代理,默认使用 gRPC
    
    @PostMapping("/create-user")
    public CreateUserDto.Response createUser(@RequestBody CreateUserDto.Request request) {
        // 透明调用,底层可能是 HTTP 或 gRPC
        return userRpcService.createUser(request);
    }
}
```

## 对比传统 gRPC

### 传统 gRPC 方式 ❌

```
1. 编写 user_service.proto
2. 使用 protoc 生成 Java 代码
3. 实现 UserServiceGrpc.UserServiceImplBase
4. 客户端使用 UserServiceGrpc.UserServiceBlockingStub

问题:
- 需要维护 .proto 文件和 Java 代码两份
- protoc 生成的代码不够友好
- 切换协议需要重写代码
```

### Nebula RPC 方式 ✅

```
1. 定义 UserRpcService 接口 (Java)
2. 定义 DTOs (Java)
3. 实现接口,加 @RpcService
4. 自动支持 HTTP 和 gRPC

优势:
- 只写 Java 代码,无需 .proto
- 协议透明,随时切换
- 统一编程模型
```

## 性能考虑

| 协议 | 性能 | 适用场景 |
|------|------|----------|
| **HTTP RPC** | 中等 (JSON 序列化较慢) | 调试、跨语言、浏览器直接调用 |
| **gRPC** | 高 (Protobuf 更快,HTTP/2 多路复用) | 微服务内部通信、高性能要求 |

**建议**: 
- 开发/测试环境: 使用 HTTP RPC (易于调试)
- 生产环境: 使用 gRPC (性能优先)

## 总结

### 问题: nebula-example-api 如何同时支持 HTTP 和 gRPC?

### 答案: 实体只定义一次,框架自动适配两种协议

1. **接口和 DTO**: 只在 `nebula-example-api` 中定义一次 (Java)
2. **实现类**: 只在 `nebula-example` 中实现一次,加 `@RpcService`
3. **协议适配**: 
   - `HttpRpcServer` + `HttpRpcClient` 处理 HTTP + JSON
   - `GrpcRpcServer` + `GrpcRpcClient` 处理 gRPC + Protobuf
4. **自动注册**: `RpcServiceRegistrationProcessor` 同时注册到两个服务器
5. **客户端选择**: 通过配置或 `@Primary` 注解选择协议

### 不需要:
- ❌ 编写 `.proto` 文件
- ❌ 运行 `protoc` 生成代码
- ❌ 定义两份实体
- ❌ 实现两次服务

### 只需要:
- ✅ Java 接口 + 注解
- ✅ Java DTOs
- ✅ 一次实现

---

**这就是 Nebula RPC 框架的核心理念: 协议无关,专注业务逻辑。**


