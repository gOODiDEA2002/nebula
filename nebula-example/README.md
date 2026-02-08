# Nebula Framework 示例项目

> 本目录包含 Nebula Framework 的完整示例项目，演示框架各模块的使用方法

## 项目列表

| 示例 | 端口 | 说明 | 依赖 |
|------|------|------|------|
| [nebula-example-web](./nebula-example-web/) | 8080 | Web 应用示例 | - |
| [nebula-rpc-async](./nebula-rpc-async/) | 8010/8011 | 异步 RPC 示例 | Nacos |
| [nebula-example-microservice](./nebula-example-microservice/) | 8001/8002 | 微服务示例 | Nacos |
| [nebula-example-gateway](./nebula-example-gateway/) | 8000 | API 网关示例 | Nacos, Redis |

## 架构概览

```
                          ┌──────────────────┐
                          │   HTTP Client    │
                          │  (Browser/App)   │
                          └────────┬─────────┘
                                   │
                          ┌────────▼─────────┐
                          │   API Gateway    │
                          │  (端口: 8000)    │
                          └────────┬─────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
     ┌────────▼───────┐   ┌───────▼────────┐   ┌──────▼──────┐
     │  user-service  │   │  order-service │   │  其他服务   │
     │   (端口: 8001)  │◄──│   (端口: 8002)  │   │    ...     │
     └────────────────┘   └────────────────┘   └─────────────┘
              │                    │
              │     RPC 调用       │
              └────────────────────┘

                          ┌──────────────────┐
                          │      Nacos       │
                          │   (服务发现)      │
                          └──────────────────┘
```

## 快速开始

### 前置条件

- JDK 21+
- Maven 3.8+
- Nacos 2.x（localhost:8848）
- Redis（localhost:6379，网关限流需要）

### 方式一：使用启动脚本

```bash
# 一键启动所有服务
./scripts/start-all.sh

# 一键停止所有服务
./scripts/stop-all.sh
```

### 方式二：手动启动

```bash
# 1. 编译所有示例
mvn clean package -DskipTests

# 2. 启动 Web 示例
cd nebula-example-web
java -jar target/nebula-example-web-1.0.0-SNAPSHOT.jar

# 3. 启动微服务示例
cd nebula-example-microservice/user-service
java -jar target/user-service-1.0.0-SNAPSHOT.jar

cd nebula-example-microservice/order-service
java -jar target/order-service-1.0.0-SNAPSHOT.jar

# 4. 启动网关示例
cd nebula-example-gateway
java -jar target/nebula-example-gateway-1.0.0-SNAPSHOT.jar
```

## 示例详解

### 1. Web 应用示例 (nebula-example-web)

最简单的 Web 应用，演示 Thymeleaf 模板和静态资源处理。

**特点：**
- 无需外部服务
- Thymeleaf 模板渲染
- REST API
- 静态资源

**访问地址：**
- 首页: http://localhost:8080
- API: http://localhost:8080/api/info

### 2. 异步 RPC 示例 (nebula-rpc-async)

演示长时间运行任务的异步执行。

**特点：**
- 异步任务提交
- 任务状态查询
- 结果获取
- Nacos 存储

**访问地址：**
- 服务端: http://localhost:8010
- 客户端: http://localhost:8011

### 3. 微服务示例 (nebula-example-microservice)

演示多服务通信和服务发现。

**子模块：**
- `microservice-api` - 共享 API 定义
- `user-service` - 用户服务（端口 8001）
- `order-service` - 订单服务（端口 8002）

**特点：**
- 服务注册发现（Nacos）
- 服务间 RPC 调用
- REST API 与 RPC 并存
- API 模块自动配置

**测试：**
```bash
# 通过 order-service 创建订单（会调用 user-service 获取用户信息）
curl -X POST http://localhost:8002/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "productName": "iPhone", "quantity": 1, "amount": 8999}'
```

### 4. API 网关示例 (nebula-example-gateway)

演示 API 网关的核心功能。

**特点：**
- HTTP 反向代理
- JWT 认证
- 请求限流
- 熔断降级
- CORS 处理

**路由规则：**
- `/api/users/**` -> user-service
- `/api/orders/**` -> order-service

**测试：**
```bash
# 通过网关访问
curl http://localhost:8000/api/users
curl http://localhost:8000/api/orders
```

## 配置复杂度对比

| 示例 | 配置行数 | 说明 |
|------|----------|------|
| nebula-example-web | 69 行 | 无需外部服务 |
| nebula-rpc-async (Service) | 62 行 | 需要 Nacos |
| nebula-rpc-async (Client) | 88 行 | 需要 Nacos |
| user-service | 67 行 | 需要 Nacos |
| order-service | 67 行 | 需要 Nacos |
| nebula-example-gateway | ~100 行 | 需要 Nacos + Redis |

## 最佳实践

### 1. RPC 接口设计

```java
// 不使用 HTTP 路径注解
@RpcClient("user-service")
public interface UserRpcClient {
    @RpcCall
    UserDto getUserById(Long userId);
}
```

### 2. API 模块自动配置

```java
@AutoConfiguration
@EnableRpcClients(basePackages = "io.nebula.example.api")
public class ExampleApiAutoConfiguration {}
```

### 3. 服务间通信

- 前端请求 -> API Gateway -> 后端 Controller
- 服务间调用 -> RPC Client（不经过 Gateway）

## 目录结构

```
nebula-example/
├── pom.xml                          # 父 POM
├── README.md                        # 本文档
├── scripts/                         # 启动脚本
│   ├── start-all.sh                 # 一键启动
│   └── stop-all.sh                  # 一键停止
├── nebula-example-web/              # Web 应用示例
├── nebula-rpc-async/                # 异步 RPC 示例
├── nebula-example-microservice/     # 微服务示例
│   ├── microservice-api/            # 共享 API
│   ├── user-service/                # 用户服务
│   └── order-service/               # 订单服务
└── nebula-example-gateway/          # API 网关示例
```

## 相关文档

- [Nebula 框架使用指南](../docs/Nebula框架使用指南.md)
- [Nebula 框架配置说明](../docs/Nebula框架配置说明.md)
- [RPC 架构说明](../docs/rpc/ARCHITECTURE.md)

---

**版本**: 1.0.0-SNAPSHOT  
**更新日期**: 2026-01-21
