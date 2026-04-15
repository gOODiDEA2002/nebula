# Nebula RPC 协议支持测试文档

## 概述

`nebula-example-api` 模块中定义的 `UserRpcService` 接口和 DTO,可以同时支持 **HTTP RPC** 和 **gRPC** 两种协议,**无需定义两份实体**

## 架构说明

### 1. API 定义 (只定义一次)

```java
// nebula-example-api/src/main/java/io/nebula/example/api/rpc/UserRpcService.java
@RemoteService(value = "nebula-example", name = "user-rpc-service")
public interface UserRpcService {
    @RpcCall(value = "/rpc/users", method = "POST")
    CreateUserDto.Response createUser(@RequestBody CreateUserDto.Request request);
    
    @RpcCall(value = "/rpc/users/{id}", method = "GET")
    GetUserDto.Response getUserById(@PathVariable("id") Long id);
    
    // ... 其他方法
}
```

### 2. 服务实现 (只实现一次)

```java
// nebula-example/src/main/java/io/nebula/example/modules/rpc/rpccontroller/UserRpcServiceImpl.java
@RpcService
@Component
public class UserRpcServiceImpl implements UserRpcService {
    @Autowired
    private RpcDemoService rpcDemoService;
    
    @Override
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        return rpcDemoService.createUser(request);
    }
    
    @Override
    public GetUserDto.Response getUserById(Long id) {
        GetUserDto.Request request = new GetUserDto.Request();
        request.setId(id);
        return rpcDemoService.getUserById(request);
    }
    
    // ... 其他方法实现
}
```

### 3. 自动协议适配

#### HTTP RPC 流程

```
客户端
  > HttpRpcClient.createProxy()
      > JSON 序列化
          > HTTP POST /rpc
              > HttpRpcController.handleRequest()
                  > JSON 反序列化
                      > UserRpcServiceImpl.createUser()
                          > RpcDemoService.createUser()
                              > JSON 序列化返回值
                                  > HTTP Response
```

#### gRPC 流程

```
客户端
  > GrpcRpcClient.call()
      > Protobuf 序列化 (Java对象  bytes)
          > gRPC call()
              > GrpcRpcServer.call()
                  > Protobuf 反序列化 (bytes  Java对象)
                      > UserRpcServiceImpl.createUser() (同一个实现!)
                          > RpcDemoService.createUser()
                              > Protobuf 序列化返回值 (Java对象  bytes)
                                  > gRPC Response
```

### 4. 关键组件

| 组件 | HTTP RPC | gRPC |
|------|----------|------|
| **服务端口** | 8000 (Tomcat) | 9090 (gRPC Server) |
| **协议** | HTTP/1.1 + JSON | HTTP/2 + Protobuf |
| **序列化** | Jackson (JSON) | Protobuf (bytes) |
| **服务器** | `HttpRpcServer` | `GrpcRpcServer` |
| **控制器** | `HttpRpcController` | `GrpcRpcServer.call()` |
| **客户端** | `HttpRpcClient` | `GrpcRpcClient` |
| **实现类** | `UserRpcServiceImpl` (共享) | `UserRpcServiceImpl` (共享) |

## 测试用例

### 前置条件

1. 启动 `nebula-example` 应用:
   ```bash
   cd /Users/andy/DevOps/SourceCode/nebula-projects/nebula-example
   mvn spring-boot:run
   ```

2. 确认应用启动成功:
   ```bash
   tail -f startup.log | grep "Started NebulaExampleApplication"
   ```

3. 确认 HTTP RPC 和 gRPC 服务器都已启动:
   ```bash
   # 查看 HTTP RPC 服务器 (端口 8080)
   # 查看 gRPC 服务器 (端口 9090)
   # 查看 Tomcat 服务器 (端口 8000)
   ```

### 测试 1: HTTP RPC - 创建用户

#### 1.1 直接调用 HTTP RPC 服务端

```bash
curl -X POST http://localhost:8000/rpc/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "name": "Test User",
    "email": "test@example.com",
    "phone": "13900139000",
    "status": "ACTIVE"
  }'
```

**预期响应**:
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 11
  },
  "timestamp": "2025-10-09T18:50:09.494633"
}
```

#### 1.2 通过 HTTP RPC 客户端调用 (需要 Nacos)

```bash
curl -X POST http://localhost:8000/rpc-client/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "rpcuser",
    "name": "RPC User",
    "email": "rpc@example.com",
    "phone": "13900139001",
    "status": "ACTIVE"
  }'
```

**注意**: 此测试需要 Nacos 服务发现如果 Nacos 未启动或未配置认证,会报错:`没有可用的服务实例: nebula-example`

### 测试 2: HTTP RPC - 查询用户

```bash
curl -X GET http://localhost:8000/rpc/users/1
```

**预期响应**:
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "name": "管理员",
    "email": "admin@example.com",
    "phone": "13800138000",
    "status": "ACTIVE",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  },
  "timestamp": "2025-10-09T19:00:00.000000"
}
```

### 测试 3: gRPC - 使用 grpcurl 测试 (可选)

如果安装了 `grpcurl`,可以直接测试 gRPC 服务:

```bash
# 列出所有 gRPC 服务
grpcurl -plaintext localhost:9090 list

# 调用 RPC 方法
grpcurl -plaintext -d '{
  "service_name": "io.nebula.example.api.rpc.UserRpcService",
  "method_name": "createUser",
  "args": ["eyJ1c2VybmFtZSI6InRlc3R1c2VyIiwibmFtZSI6IlRlc3QgVXNlciIsImVtYWlsIjoidGVzdEBleGFtcGxlLmNvbSIsInBob25lIjoiMTM5MDAxMzkwMDAiLCJzdGF0dXMiOiJBQ1RJVkUifQ=="],
  "arg_types": ["io.nebula.example.api.dto.CreateUserDto$Request"]
}' localhost:9090 io.nebula.rpc.grpc.RpcService/call
```

### 测试 4: 验证 HTTP 和 gRPC 使用同一实现

1. 在 `UserRpcServiceImpl` 中添加日志:
   ```java
   @Override
   public CreateUserDto.Response createUser(CreateUserDto.Request request) {
       System.out.println("[UserRpcServiceImpl] createUser called: " + request.getUsername());
       return rpcDemoService.createUser(request);
   }
   ```

2. 分别调用 HTTP RPC 和 gRPC 接口

3. 查看日志,确认都调用了同一个 `UserRpcServiceImpl` 实现

## 问题排查

### 问题 1: `没有可用的服务实例: nebula-example`

**原因**: Nacos 服务发现未配置或服务未注册

**解决方案**:
1. 启动 Nacos (默认端口 8848)
2. 在 `application.yml` 中配置 Nacos:
   ```yaml
   nebula:
     discovery:
       nacos:
         enabled: true
         server-addr: localhost:8848
         username:   # 如果不需要认证,留空
         password:
         auto-register: true  # 启用自动注册
   ```
3. 或者,直接使用 URL 配置,绕过服务发现:
   ```yaml
   nebula:
     rpc:
       services:
         user-rpc-service:
           url: http://localhost:8000  # 直接指定服务地址
   ```

### 问题 2: `user not found!` (Nacos 认证失败)

**原因**: Nacos 服务器开启了认证,但用户名/密码不正确

**解决方案**:
1. 确认 Nacos 用户名/密码 (默认: `nacos/nacos`)
2. 更新 `application.yml`:
   ```yaml
   nebula:
     discovery:
       nacos:
         username: nacos
         password: nacos
   ```
3. 或者关闭 Nacos 认证 (仅用于本地开发)

### 问题 3: gRPC 端口冲突

**原因**: 端口 9090 被占用

**解决方案**:
在 `application.yml` 中修改 gRPC 端口:
```yaml
nebula:
  rpc:
    grpc:
      server:
        port: 9091  # 修改为其他端口
```

## 核心优势总结

###  一次定义,多种协议

- **接口定义**: `UserRpcService` 只定义一次
- **DTO 定义**: `CreateUserDto` 等只定义一次
- **实现类**: `UserRpcServiceImpl` 只实现一次

###  自动协议适配

- **HTTP RPC**: JSON 序列化/反序列化 (Jackson)
- **gRPC**: Protobuf 序列化/反序列化 (通用 `rpc_common.proto`)

###  透明协议切换

客户端可以通过配置选择使用 HTTP 或 gRPC:

```yaml
nebula:
  rpc:
    http:
      enabled: true   # 启用 HTTP RPC
    grpc:
      enabled: true   # 启用 gRPC (优先使用,标记为 @Primary)
```

###  无需 .proto 文件定义

不需要为每个服务编写 `.proto` 文件,使用通用的 `rpc_common.proto`:

```protobuf
message RpcRequest {
  string service_name = 1;
  string method_name = 2;
  repeated bytes args = 3;         // 序列化后的参数
  repeated string arg_types = 4;   // 参数的完整类名
}

message RpcResponse {
  bool success = 1;
  bytes result = 2;                // 序列化后的结果
  string error_message = 3;
  string error_code = 4;
}
```

## 未来扩展

Nebula RPC 框架还可以扩展支持:

- **WebSocket RPC**: 实时双向通信
- **Netty-based RPC**: 高性能自定义协议
- **消息队列 RPC**: 异步解耦
- **Service Mesh 集成**: Istio, Linkerd 等

查看 `ROADMAP.md` 了解更多计划

## 相关文档

- [Nebula RPC HTTP 模块文档](../infrastructure/rpc/nebula-rpc-http/README.md)
- [Nebula RPC gRPC 模块文档](../infrastructure/rpc/nebula-rpc-grpc/README.md)
- [Nebula RPC HTTP 测试文档](./nebula-rpc-test.md)
- [Nebula RPC gRPC 测试文档](./nebula-grpc-test.md)
- [Nebula RPC Roadmap](../infrastructure/rpc/ROADMAP.md)

---

**文档版本**: 2.0.0  
**最后更新**: 2025-10-09  
**维护者**: Nebula Framework Team


