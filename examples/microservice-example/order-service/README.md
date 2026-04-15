# Nebula Example Order Service

订单服务示例，演示 Nebula RPC 框架的服务端实现和跨服务调用

##  快速开始

### 1. 启动依赖服务

确保以下服务已启动：
- **Nacos** - 服务注册与发现
- **User Service** - 用户服务（被调用方）

### 2. 启动 Order Service

```bash
mvn spring-boot:run
```

### 3. 测试 RPC 调用

```bash
# 测试 gRPC 协议
./test-grpc.sh

# 测试零配置
./test-zero-config.sh
```

##  项目结构

```
order-service/
 src/main/java/io/nebula/example/order/service/
    NebulaExampleOrderServiceApplication.java  # 主应用类
    rpc/
       OrderRpcClientImpl.java                # RPC 服务实现
    business/
        OrderDemoService.java                   # 业务逻辑
 src/main/resources/
    application.yml                             # 配置文件
 docs/
     INDEX.md                                    # 文档索引
     archive/                                    # 历史文档归档
```

## ️ 配置说明

### application.yml

```yaml
spring:
  application:
    name: nebula-example-order-service

# Nacos 配置
nebula:
  discovery:
    nacos:
      server-addr: localhost:8848
      namespace: dev

# RPC 配置
  rpc:
    grpc:
      enabled: true
      server:
        port: 9091
```

##  核心功能

### 1. RPC 服务实现

```java
@RpcService  // 无需指定接口类，自动推导
@RequiredArgsConstructor
public class OrderRpcClientImpl implements OrderRpcClient {
    
    private final UserRpcClient userRpcClient;  // 无需 @Qualifier
    private final AuthRpcClient authRpcClient;  // 无需 @Qualifier
    
    @Override
    public CreateOrderDto.Response createOrder(CreateOrderDto.Request request) {
        // 1. 认证
        AuthDto.Response authResponse = authRpcClient.auth(...);
        
        // 2. 获取用户信息
        GetUserDto.Response userResponse = userRpcClient.getUserById(...);
        
        // 3. 创建订单
        // ...
    }
}
```

### 2. 跨服务调用

Order Service 自动调用 User Service：
- 通过 Nacos 服务发现
- 支持 gRPC 和 HTTP 协议
- 自动负载均衡

##  关键特性

### 零配置 RPC 客户端注入

```java
//  无需任何配置，直接注入使用
@RequiredArgsConstructor
public class OrderRpcClientImpl {
    private final UserRpcClient userRpcClient;
    private final AuthRpcClient authRpcClient;
}
```

### 自动服务发现

```java
//  自动从 Nacos 发现 nebula-example-user-service
authRpcClient.auth(request);  
```

### 协议自动选择

- gRPC 优先（高性能）
- HTTP 降级（兼容性）

##  文档

- [文档索引](docs/INDEX.md) - 所有文档的索引和归档
- [历史文档](docs/archive/) - 开发过程中的问题修复和优化记录

##  常见问题

### 1. 服务注册失败

**问题**：`No such service: nebula-example-user-service`

**解决**：确保 User Service 已启动并成功注册到 Nacos

### 2. gRPC 调用失败

**问题**：`gRPC connection failed`

**解决**：检查 gRPC 端口配置和防火墙设置

### 3. 命名空间问题

**问题**：`Namespace not found`

**解决**：确保 Nacos 中已创建对应的命名空间，或使用默认命名空间（public）

##  性能监控

### 日志级别

```yaml
logging:
  level:
    io.nebula.rpc: DEBUG  # RPC 调用日志
    io.nebula.discovery: INFO  # 服务发现日志
```

### 关键日志

```
INFO  - 注册RPC服务: OrderRpcClient
INFO  - 执行RPC调用: service=nebula-example-user-service, method=auth
INFO  - RPC调用成功: duration=50ms
```

##  相关项目

- [Nebula Framework](../../../nebula/) - 核心框架
- [User Service](../../nebula-example-user-service/) - 用户服务
- [User API](../../nebula-example-user-api/) - 用户服务 API 定义

##  更新日志

- **2025-10** - 实现零配置 RPC 客户端注入
- **2025-10** - 支持 ThreadLocal 服务名传递
- **2025-10** - 完成 gRPC 协议集成
- **2025-09** - 完成 Nacos 服务注册与发现

---

**版本**: 2.0.0  
**许可**: Apache License 2.0
