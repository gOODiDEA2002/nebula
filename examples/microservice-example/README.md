# Nebula Microservice Example

> Nebula 框架微服务拆分示例，演示 API 模块分离、RPC 调用、服务发现

## 功能特性

- 标准微服务拆分架构：API 模块 + Service 模块
- User 服务提供用户 CRUD 和认证接口
- Order 服务提供订单管理，并跨服务调用 User 服务
- 使用 `@RemoteService` + `@RpcService` 实现声明式 RPC
- 同时支持 HTTP RPC 和 gRPC 两种协议
- 基于 Nacos 的服务注册与发现
- 内存存储（演示用，无需数据库）

## 架构设计

```
                    ┌─────────────────┐
                    │   Nacos (8848)  │
                    │   服务注册中心    │
                    └────────┬────────┘
                             │ 注册/发现
                ┌────────────┼────────────┐
                │                         │
    ┌───────────┴───────────┐ ┌───────────┴───────────┐
    │   User Service (1001) │ │  Order Service (1002) │
    │   gRPC: 2001          │ │  gRPC: 2002           │
    │                       │ │                       │
    │   UserRpcClient impl  │ │  OrderRpcClient impl  │
    │   AuthRpcClient impl  │◄┤  (调用 UserRpcClient) │
    │   UserController      │ │                       │
    └───────────────────────┘ └───────────────────────┘
                │                         │
                │     共享 API 模块        │
                │                         │
    ┌───────────┴──────┐   ┌──────────────┴──────┐
    │    user-api       │   │    order-api         │
    │  UserRpcClient    │   │  OrderRpcClient      │
    │  AuthRpcClient    │   │  DTO / VO            │
    │  DTO / VO / Entity│   │                      │
    └──────────────────┘   └─────────────────────┘
```

## 项目结构

```
microservice-example/
├── pom.xml                              # 聚合 POM
│
├── user-api/                            # 用户 API 契约
│   ├── pom.xml
│   └── src/main/java/.../user/api/
│       ├── rpc/
│       │   ├── UserRpcClient.java       # 用户 RPC 接口
│       │   └── AuthRpcClient.java       # 认证 RPC 接口
│       ├── dto/                         # 请求/响应 DTO
│       ├── vo/                          # 视图对象
│       └── entity/                      # 实体类
│
├── order-api/                           # 订单 API 契约
│   ├── pom.xml
│   └── src/main/java/.../order/api/
│       ├── rpc/
│       │   └── OrderRpcClient.java      # 订单 RPC 接口
│       ├── dto/                         # 请求/响应 DTO
│       ├── vo/                          # 视图对象
│       └── entity/                      # 实体类
│
├── user-service/                        # 用户服务（端口 1001, gRPC 2001）
│   ├── pom.xml
│   └── src/main/
│       ├── java/.../user/service/
│       │   ├── NebulaExampleUserServiceApplication.java
│       │   ├── controller/UserController.java
│       │   ├── rpc/
│       │   │   ├── UserRpcClientImpl.java
│       │   │   └── AuthRpcClientImpl.java
│       │   └── business/               # 业务服务层
│       └── resources/application.yml
│
└── order-service/                       # 订单服务（端口 1002, gRPC 2002）
    ├── pom.xml
    └── src/main/
        ├── java/.../order/service/
        │   ├── NebulaExampleOrderServiceApplication.java
        │   ├── rpc/OrderRpcClientImpl.java
        │   └── business/               # 业务服务层
        └── resources/application.yml
```

## 前置条件

- JDK 21+
- Maven 3.8+
- Nacos 2.x（localhost:8848）

## 快速开始

```bash
# 1. 安装框架和 API 模块
cd /path/to/nebula
mvn install -DskipTests

# 2. 启动 Nacos
# 确保 Nacos 运行在 localhost:8848

# 3. 启动 User 服务（端口 1001）
mvn -q -f examples/microservice-example/user-service spring-boot:run

# 4. 启动 Order 服务（端口 1002）
mvn -q -f examples/microservice-example/order-service spring-boot:run
```

## 接口测试

### User 服务接口（端口 1001）

```bash
# 创建用户
curl -X POST http://localhost:1001/rpc/users \
  -H "Content-Type: application/json" \
  -d '{"username":"nebula","name":"Nebula User","email":"user@nebula.io"}'

# 查询用户
curl http://localhost:1001/rpc/users/1

# 用户列表
curl "http://localhost:1001/rpc/users?page=1&size=10"

# 更新用户
curl -X PUT http://localhost:1001/rpc/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated User"}'

# 删除用户
curl -X DELETE http://localhost:1001/rpc/users/1
```

### Order 服务接口（端口 1002，跨服务调用 User 服务）

```bash
# 创建订单（内部调用 UserService 验证用户）
curl -X POST http://localhost:1002/rpc/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productName":"Nebula License","quantity":1,"price":99.00}'

# 查询订单
curl http://localhost:1002/rpc/orders/1
```

## RPC 接口设计模式

### 模式一：零注解自动映射

```java
@RemoteService
public interface UserRpcClient {
    CreateUserDto.Response createUser(CreateUserDto.Request request);
    GetUserDto.Response getUserById(Long id);
}
```

### 模式二：显式路由映射

```java
@RemoteService
public interface OrderRpcClient {
    @RpcCall(value = "/rpc/orders", method = "POST")
    CreateOrderDto.Response createOrder(@RequestBody CreateOrderDto.Request request);

    @RpcCall(value = "/rpc/orders/{id}", method = "GET")
    GetOrderDto.Response getOrderById(@PathVariable("id") Long id);
}
```

## 配置说明

### User 服务

```yaml
server:
  port: 1001
grpc:
  server:
    port: 2001

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      auto-register: true
  rpc:
    http:
      enabled: true
      server:
        port: ${server.port}
        context-path: /rpc
    grpc:
      enabled: true
      server:
        port: ${grpc.server.port}
```

### Order 服务

```yaml
server:
  port: 1002
grpc:
  server:
    port: 2002

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      auto-register: true
  rpc:
    http:
      enabled: true
      server:
        port: ${server.port}
        context-path: /rpc
      client:
        enabled: true
    grpc:
      enabled: true
    discovery:
      enabled: true
      load-balance-strategy: ROUND_ROBIN
```

## 相关文档

- [Nebula Examples 总览](../README.md)
- [API 契约定义示例](../starter-api-example/README.md)
- [异步 RPC 示例](../rpc-async-example/README.md)
- [API 网关示例](../gateway-example/README.md)
