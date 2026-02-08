# Nebula Example - Microservice

> Nebula Framework 微服务示例项目，演示多服务通信和服务发现

## 功能特性

- 服务注册与发现（Nacos）
- 服务间 RPC 调用
- 负载均衡
- REST API 与 RPC 接口并存

## 架构设计

```
                          ┌──────────────┐
                          │    Nacos     │
                          │ (服务发现)    │
                          └──────┬───────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
              ▼                  ▼                  ▼
    ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐
    │  user-service   │  │  order-service  │  │   其他服务   │
    │   (端口: 8001)   │◄─│   (端口: 8002)   │  │     ...     │
    └─────────────────┘  └─────────────────┘  └─────────────┘
           ▲                      │
           │                      │
           └──────── RPC 调用 ────┘
```

## 项目结构

```
nebula-example-microservice/
├── microservice-api/           # 共享 API 定义
│   └── src/main/java/.../api/
│       ├── user/
│       │   ├── UserRpcClient.java      # 用户服务 RPC 接口
│       │   ├── UserDto.java            # 用户 DTO
│       │   └── CreateUserRequest.java  # 创建用户请求
│       └── order/
│           ├── OrderRpcClient.java     # 订单服务 RPC 接口
│           ├── OrderDto.java           # 订单 DTO
│           └── CreateOrderRequest.java # 创建订单请求
│
├── user-service/               # 用户服务（端口 8001）
│   └── src/main/java/.../user/
│       ├── UserServiceApplication.java
│       ├── service/UserService.java
│       └── rpc/UserRpcClientImpl.java  # RPC 实现
│
├── order-service/              # 订单服务（端口 8002）
│   └── src/main/java/.../order/
│       ├── OrderServiceApplication.java
│       ├── service/OrderService.java   # 调用 user-service
│       ├── rpc/OrderRpcClientImpl.java # RPC 实现
│       └── controller/OrderController.java  # REST API
│
├── pom.xml
└── README.md
```

## 快速开始

### 前置条件

- JDK 21+
- Maven 3.8+
- Nacos 2.x（localhost:8848）
- Nebula Framework 2.0.1-SNAPSHOT

### 启动步骤

```bash
# 1. 确保 Nacos 已启动
# 如果使用 Docker:
# cd nebula-data && docker-compose up -d nacos

# 2. 编译 API 模块并安装到本地仓库
cd microservice-api && mvn clean install

# 3. 编译 user-service
cd ../user-service && mvn clean package -DskipTests

# 4. 编译 order-service
cd ../order-service && mvn clean package -DskipTests

# 5. 启动 user-service（端口 8001）
cd ../user-service
java -jar target/user-service-1.0.0-SNAPSHOT.jar

# 6. 新开终端，启动 order-service（端口 8002）
cd order-service
java -jar target/order-service-1.0.0-SNAPSHOT.jar
```

### 验证

```bash
# 1. 查看用户列表（通过 user-service）
curl http://localhost:8001/rpc/users

# 2. 创建订单（通过 order-service REST API，会调用 user-service 获取用户信息）
curl -X POST http://localhost:8002/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "productName": "iPhone 15", "quantity": 1, "amount": 8999}'

# 3. 查询订单（包含用户信息）
curl http://localhost:8002/api/orders/1

# 4. 查看用户的订单
curl http://localhost:8002/api/orders/user/1

# 5. 健康检查
curl http://localhost:8001/actuator/health
curl http://localhost:8002/actuator/health
```

## API 列表

### User Service (端口 8001)

| 类型 | 方法 | 路径 | 描述 |
|------|------|------|------|
| RPC | POST | /rpc/users | 创建用户 |
| RPC | GET | /rpc/users/{userId} | 获取用户 |
| RPC | GET | /rpc/users | 用户列表 |

### Order Service (端口 8002)

| 类型 | 方法 | 路径 | 描述 |
|------|------|------|------|
| REST | POST | /api/orders | 创建订单 |
| REST | GET | /api/orders/{orderId} | 获取订单 |
| REST | GET | /api/orders/user/{userId} | 用户订单列表 |
| REST | GET | /api/orders | 所有订单 |
| RPC | POST | /rpc/orders | 创建订单（RPC） |
| RPC | GET | /rpc/orders/{orderId} | 获取订单（RPC） |

## 核心代码说明

### 1. RPC 接口定义（API 模块）

```java
/**
 * RPC 接口设计原则：
 * - 参数使用具体类型
 * - 返回值使用业务对象
 * - 不使用 HTTP 路径注解（框架自动处理）
 */
@RpcClient("user-service")
public interface UserRpcClient {
    @RpcCall
    UserDto getUserById(Long userId);
}
```

### 2. API 模块自动配置

```java
// MicroserviceApiAutoConfiguration.java
@AutoConfiguration
@EnableRpcClients(basePackages = "io.nebula.example.microservice.api")
public class MicroserviceApiAutoConfiguration {
}
```

配合 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件自动注册。

### 3. RPC 接口实现（服务端）

```java
@RpcService
@RequiredArgsConstructor
public class UserRpcClientImpl implements UserRpcClient {
    private final UserService userService;
    
    @Override
    public UserDto getUserById(Long userId) {
        return userService.getUserById(userId);
    }
}
```

### 4. RPC 调用（客户端）

由于 API 模块包含自动配置，消费方启动类无需添加 `@EnableRpcClients` 注解：

```java
// OrderServiceApplication.java - 无需特殊注解
@SpringBootApplication
public class OrderServiceApplication {
    // ...
}

// OrderService.java - 直接注入使用
@Service
@RequiredArgsConstructor
public class OrderService {
    // 自动注入 RPC 客户端代理（由 API 模块自动配置）
    private final UserRpcClient userRpcClient;
    
    public OrderDto createOrder(CreateOrderRequest request) {
        // 调用用户服务
        UserDto user = userRpcClient.getUserById(request.getUserId());
        // ...
    }
}
```

## 配置说明

| 配置项 | user-service | order-service | 说明 |
|--------|-------------|---------------|------|
| server.port | 8001 | 8002 | 服务端口 |
| spring.application.name | user-service | order-service | 服务名 |
| nebula.discovery.nacos.server-addr | localhost:8848 | localhost:8848 | Nacos 地址 |
| nebula.rpc.discovery.enabled | true | true | 启用 RPC 服务发现 |

## 配置复杂度

| 服务 | 配置行数 | 必需配置项 |
|------|----------|------------|
| user-service | ~55 行 | 8 |
| order-service | ~55 行 | 8 |

## 相关文档

- [Nebula RPC 文档](../../infrastructure/rpc/nebula-rpc-core/README.md)
- [Nebula 服务发现文档](../../infrastructure/discovery/nebula-discovery-core/README.md)
- [nebula-starter-service 文档](../../starter/nebula-starter-service/README.md)

---

**版本**: 1.0.0-SNAPSHOT
