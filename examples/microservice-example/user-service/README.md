# Nebula Example Service

##  模块说明

这是 Nebula RPC 的**服务提供者（Provider）**示例模块，展示如何实现和发布 RPC 服务

## ️ 架构角色

```
nebula-example-api           契约层（Contract）
     依赖
nebula-example-service       服务端（Provider） [当前模块]
     注册
Nacos 服务注册中心
```

##  模块内容

### 核心组件

1. **RPC服务实现**
   - `UserRpcServiceImpl`: 实现 `UserRpcService` 接口
   - 使用 `@RpcService` 注解标注

2. **业务逻辑层**
   - `RpcDemoService`: 业务服务接口
   - `RpcDemoServiceImpl`: 业务逻辑实现（内存存储示例）

3. **配置**
   - 端口: 8081
   - 服务名: nebula-example-service
   - 注册到 Nacos

##  快速启动

### 前置条件

1. **启动 Nacos**
   ```bash
   # 确保 Nacos 运行在 localhost:8848
   ```

2. **构建项目**
   ```bash
   cd nebula-example-service
   mvn clean install
   ```

### 启动服务

```bash
mvn spring-boot:run
```

或使用 IDE 直接运行 `NebulaExampleServiceApplication`

### 验证服务

1. **检查 Nacos 控制台**
   - 访问: http://localhost:8848/nacos
   - 查看服务列表，确认 `nebula-example-service` 已注册

2. **查看日志**
   ```
   初始化了 10 个测试用户
   HTTP RPC 服务器启动，端口: 8081
   ```

##  RPC 接口说明

### 提供的服务

实现了 `UserRpcService` 的所有方法：

| 方法 | 路径 | 说明 |
|------|------|------|
| createUser | POST /rpc/users | 创建用户 |
| getUserById | GET /rpc/users/{id} | 获取用户详情 |
| getUsers | GET /rpc/users | 获取用户列表 |
| updateUser | PUT /rpc/users/{id} | 更新用户 |
| deleteUser | DELETE /rpc/users/{id} | 删除用户 |

### 测试数据

服务启动时会自动初始化 10 个测试用户（ID: 1-10）

##  接口测试

### 三种测试方式

Nebula 服务支持三种访问方式：

| 方式 | 协议 | 端点 | 适用场景 |
|------|------|------|----------|
| **REST API** | HTTP/1.1 | `/rpc/users/*` | 外部调用前端手动测试 |
| **RPC 端点** | HTTP/1.1 | `/rpc` (统一) | 服务间 RPC 通信 |
| **gRPC** | HTTP/2 | `localhost:9081` | 高性能服务间通信 |

### 方式1：REST API 端点测试（推荐用于手动测试）

使用传统的 REST API 风格，简单直观：

#### 1. 创建用户

```bash
curl -X POST http://localhost:8081/rpc/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "name": "测试用户",
    "email": "test@example.com",
    "phone": "13800138000",
    "status": "ACTIVE"
  }'
```

**期待响应：**
```json
{
  "id": 11
}
```

#### 2. 获取用户详情

```bash
curl http://localhost:8081/rpc/users/1
```

**期待响应：**
```json
{
  "user": {
    "id": 1,
    "username": "user1",
    "name": "测试用户1",
    "email": "user1@example.com",
    "phone": "13800000001",
    "status": "ACTIVE",
    "createTime": "2025-01-15T10:00:00",
    "updateTime": "2025-01-15T10:00:00"
  }
}
```

#### 3. 获取用户列表

```bash
# 获取所有用户（分页）
curl "http://localhost:8081/rpc/users?page=1&size=10"

# 按用户名查询
curl "http://localhost:8081/rpc/users?username=user1"

# 按状态查询
curl "http://localhost:8081/rpc/users?status=ACTIVE"

# 组合查询
curl "http://localhost:8081/rpc/users?name=测试&status=ACTIVE&page=1&size=5"
```

**期待响应：**
```json
{
  "users": [
    {
      "id": 1,
      "username": "user1",
      "name": "测试用户1",
      ...
    }
  ],
  "total": 10,
  "page": 1,
  "size": 10
}
```

#### 4. 更新用户

```bash
curl -X PUT http://localhost:8081/rpc/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "更新后的用户名",
    "email": "newemail@example.com",
    "phone": "13900000001",
    "status": "INACTIVE"
  }'
```

**期待响应：**
```json
{
  "user": {
    "id": 1,
    "username": "user1",
    "name": "更新后的用户名",
    "email": "newemail@example.com",
    "phone": "13900000001",
    "status": "INACTIVE",
    ...
  }
}
```

#### 5. 删除用户

```bash
curl -X DELETE http://localhost:8081/rpc/users/1
```

**期待响应：**
```json
{
  "success": true
}
```

### 测试技巧

#### 使用 jq 格式化输出

```bash
# 安装 jq（macOS）
brew install jq

# 格式化 JSON 输出
curl http://localhost:8081/rpc/users/1 | jq
```

#### 查看响应头

```bash
curl -i http://localhost:8081/rpc/users/1
```

#### 调试模式

```bash
curl -v http://localhost:8081/rpc/users/1
```

#### 批量测试脚本

```bash
#!/bin/bash
# test-user-service.sh

echo "=== 测试创建用户 ==="
curl -X POST http://localhost:8081/rpc/users \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","name":"测试","email":"test@example.com","phone":"13800000000"}' \
  | jq

echo "\n=== 测试获取用户列表 ==="
curl "http://localhost:8081/rpc/users?page=1&size=5" | jq

echo "\n=== 测试获取用户详情 ==="
curl http://localhost:8081/rpc/users/1 | jq

echo "\n测试完成！"
```

### 方式2：RPC 统一端点测试（服务间通信）

使用统一的 `/rpc` 端点，适合自动化测试和服务间调用：

#### 1. 获取用户详情

```bash
curl -X POST http://localhost:8081/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "test-001",
    "serviceName": "io.nebula.example.api.rpc.UserRpcService",
    "methodName": "getUserById",
    "parameters": [1],
    "parameterTypes": ["java.lang.Long"]
  }' | jq
```

#### 2. 创建用户

```bash
curl -X POST http://localhost:8081/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "test-002",
    "serviceName": "io.nebula.example.api.rpc.UserRpcService",
    "methodName": "createUser",
    "parameters": [{
      "username": "rpcuser",
      "name": "RPC测试用户",
      "email": "rpc@example.com",
      "phone": "13900000000",
      "status": "ACTIVE"
    }],
    "parameterTypes": ["io.nebula.example.api.dto.CreateUserDto$Request"]
  }' | jq
```

#### 3. 获取用户列表

```bash
curl -X POST http://localhost:8081/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "test-003",
    "serviceName": "io.nebula.example.api.rpc.UserRpcService",
    "methodName": "getUsers",
    "parameters": [null, null, "ACTIVE", 1, 5],
    "parameterTypes": [
      "java.lang.String",
      "java.lang.String", 
      "java.lang.String",
      "java.lang.Integer",
      "java.lang.Integer"
    ]
  }' | jq
```

**更多 RPC 测试示例，请参考：** [RPC 端点测试指南](../docs/RPC_ENDPOINT_TESTING.md)

#### 快速测试脚本

使用提供的测试脚本快速验证 RPC 端点：

```bash
# 运行 RPC 端点测试脚本
./test-rpc-endpoint.sh
```

该脚本会自动执行：
- 获取用户详情
- 创建用户
- 获取用户列表
- 更新用户
- 删除用户

### 方式3：gRPC 测试（高性能通信）

gRPC 使用 HTTP/2 和 Protocol Buffers，不能用普通 curl 测试，需要使用专门工具

#### Nebula gRPC 架构

**重要：** Nebula 使用**通用 gRPC 服务**（GenericRpcService），所有 `@RpcService` 自动支持 gRPC 调用：

```
@RpcService 注解  自动注册到 HTTP RPC 和 gRPC
                      HTTP: POST /rpc (JSON)
                      gRPC: GenericRpcService/Call (Protobuf)
```

**优势：**
-  一次定义，同时支持 HTTP 和 gRPC
-  无需编写 `.proto` 文件
-  无需重复定义 DTOs

#### 安装 grpcurl

```bash
# macOS
brew install grpcurl

# Linux
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.8.9/grpcurl_1.8.9_linux_x86_64.tar.gz
tar -xvf grpcurl_1.8.9_linux_x86_64.tar.gz
sudo mv grpcurl /usr/local/bin/

# 验证安装
grpcurl --version
```

#### 基础测试命令

##### 1. 列出所有服务

```bash
grpcurl -plaintext localhost:9081 list
```

**期待输出：**
```
grpc.health.v1.Health
grpc.reflection.v1alpha.ServerReflection
io.nebula.rpc.grpc.GenericRpcService  # Nebula 通用 RPC 服务
```

##### 2. 健康检查

```bash
grpcurl -plaintext \
  -d '{"service": ""}' \
  localhost:9081 \
  grpc.health.v1.Health/Check
```

**期待响应：**
```json
{
  "status": "SERVING"
}
```

##### 3. 查看 GenericRpcService 方法

```bash
# 查看 Nebula RPC 服务的方法
grpcurl -plaintext localhost:9081 list io.nebula.rpc.grpc.GenericRpcService
```

**期待输出：**
```
io.nebula.rpc.grpc.GenericRpcService.Call
io.nebula.rpc.grpc.GenericRpcService.ServerStream
io.nebula.rpc.grpc.GenericRpcService.ClientStream
io.nebula.rpc.grpc.GenericRpcService.BidirectionalStream
```

##### 4. 调用业务服务 - 获取用户详情

```bash
grpcurl -plaintext \
  -d '{
    "request_id": "grpc-test-001",
    "service_name": "io.nebula.example.api.rpc.UserRpcService",
    "method_name": "getUserById",
    "parameters": ["{\"id\":1}"],
    "parameter_types": ["io.nebula.example.api.dto.GetUserDto$Request"],
    "timestamp": '$(date +%s000)'
  }' \
  localhost:9081 \
  io.nebula.rpc.grpc.GenericRpcService/Call
```

**期待响应：**
```json
{
  "requestId": "grpc-test-001",
  "success": true,
  "result": "{\"user\":{\"id\":1,\"username\":\"user1\",\"name\":\"测试用户1\"...}}",
  "timestamp": "1705234567890"
}
```

##### 5. 创建用户

```bash
grpcurl -plaintext \
  -d '{
    "request_id": "grpc-test-002",
    "service_name": "io.nebula.example.api.rpc.UserRpcService",
    "method_name": "createUser",
    "parameters": ["{\"username\":\"grpcuser\",\"name\":\"gRPC用户\",\"email\":\"grpc@test.com\",\"phone\":\"13900000000\",\"status\":\"ACTIVE\"}"],
    "parameter_types": ["io.nebula.example.api.dto.CreateUserDto$Request"],
    "timestamp": '$(date +%s000)'
  }' \
  localhost:9081 \
  io.nebula.rpc.grpc.GenericRpcService/Call
```

#### 快速测试脚本

使用提供的 gRPC 测试脚本：

```bash
# 运行 gRPC 测试脚本
./test-grpc.sh

# 如果端口不是 9081，可以指定端口
GRPC_PORT=9090 ./test-grpc.sh
```

该脚本会自动执行：
- 连接测试
- 列出所有服务
- 健康检查
- 查看服务方法
- 检查自定义业务服务

#### 注意事项

**端口确认：**
- 配置端口：9081
- 如果日志显示 9090，说明配置未生效，需要重启服务

**检查实际端口：**
```bash
# 查看日志中的 gRPC 端口
grep "gRPC Server started" logs/*.log

# 或检查端口占用
lsof -i :9081
```

**更多 gRPC 测试方法，请参考：** [gRPC 测试指南](../docs/GRPC_TESTING_GUIDE.md)

##  配置说明

### 端口配置

| 协议 | 端口 | 说明 |
|------|------|------|
| HTTP REST API | 8081 | REST API 和 Actuator 端点 |
| gRPC | 9081 | gRPC 服务端口 |

**重要：** gRPC 和 HTTP 不能共享端口，必须分别配置

### application.yml

```yaml
server:
  port: 8081  # HTTP REST API 端口

spring:
  application:
    name: nebula-example-service  # 服务名（用于Nacos注册）

# gRPC 配置
grpc:
  server:
    port: 9081  # gRPC 端口（独立于HTTP端口）

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      
  rpc:
    http:
      server:
        enabled: true  # 启用RPC服务端
      client:
        enabled: true  # 也可以调用其他服务
    discovery:
      enabled: true   # 启用服务发现
```

##  目录结构

```
nebula-example-service/
 src/main/java/io/nebula/example/service/
    NebulaExampleServiceApplication.java   # 启动类
    rpc/
       UserRpcServiceImpl.java            # RPC服务实现
    business/
        RpcDemoService.java                # 业务接口
        impl/
            RpcDemoServiceImpl.java        # 业务实现
 src/main/resources/
    application.yml                        # 配置文件
 pom.xml
```

##  依赖关系

```xml
<!-- RPC契约 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-example-api</artifactId>
</dependency>

<!-- RPC实现 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-http</artifactId>
</dependency>

<!-- 服务发现 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
</dependency>
```

##  使用示例

### 实现RPC服务

```java
@Slf4j
@RpcService(UserRpcService.class)  // 标注为RPC服务
@RequiredArgsConstructor
public class UserRpcServiceImpl implements UserRpcService {
    
    private final RpcDemoService rpcDemoService;
    
    @Override
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        log.info("RPC服务端: createUser, username={}", request.getUsername());
        return rpcDemoService.createUser(request);
    }
    
    // ... 其他方法实现
}
```

##  常见问题

### 1. 服务无法注册到Nacos

**解决方案：**
- 确认 Nacos 服务运行正常
- 检查 `server-addr` 配置是否正确
- 查看日志中的错误信息

### 2. RPC调用失败

**解决方案：**
- 确认服务已成功启动
- 检查 RPC 端口是否被占用
- 验证防火墙设置

### 3. 找不到服务实现类

**解决方案：**
- 确认 `@RpcService` 注解正确使用
- 检查 Spring 组件扫描路径
- 验证依赖注入配置

##  相关文档

- [Nebula RPC 文档](../nebula/infrastructure/rpc/nebula-rpc-http/README.md)
- [服务契约定义](../nebula-example-api/README.md)
- [客户端使用示例](../nebula-example/docs/nebula-rpc-test.md)

##  与其他模块的关系

```
nebula-example-api (契约)
    
    | 实现
    |
nebula-example-service (提供者)
    
    | 注册
    |
Nacos (注册中心)
    
    | 发现
    |
nebula-example (消费者)
```

##  许可证

本项目基于 Apache 2.0 许可证开源

