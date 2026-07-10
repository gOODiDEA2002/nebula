# Nebula RPC gRPC 配置指南

> gRPC远程调用配置说明

## 概述

`nebula-rpc-grpc` 提供基于 gRPC 的高性能 RPC 支持。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-rpc-grpc</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 服务端配置

```yaml
nebula:
  rpc:
    grpc:
      enabled: true
      server:
        port: 9090
        # 最大消息大小(MB)
        max-inbound-message-size: 10
        # 最大连接空闲时间
        max-connection-idle: 5m
        # 保活时间
        keep-alive-time: 2h
```

### 客户端配置

```yaml
nebula:
  rpc:
    grpc:
      client:
        # 服务发现
        discovery:
          enabled: true
        # 负载均衡: round_robin / pick_first
        load-balance: round_robin
        # 超时配置
        timeout: 30s
        # 重试配置
        retry:
          enabled: true
          max-attempts: 3
```

## 票务系统场景

### 服务间调用

```yaml
nebula:
  rpc:
    grpc:
      client:
        targets:
          user-service:
            address: user-service:9090
            timeout: 10s
          order-service:
            address: order-service:9090
            timeout: 30s
```

### 使用示例

```java
// 定义protobuf
syntax = "proto3";

package ticket;

service UserService {
  rpc GetUser(GetUserRequest) returns (GetUserResponse);
}

message GetUserRequest {
  int64 user_id = 1;
}

message GetUserResponse {
  int64 id = 1;
  string username = 2;
  string phone = 3;
}

// 服务端实现
@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        User user = userService.getById(request.getUserId());
        
        GetUserResponse response = GetUserResponse.newBuilder()
            .setId(user.getId())
            .setUsername(user.getUsername())
            .setPhone(user.getPhone())
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

// 客户端调用
@Service
public class OrderService {
    
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userService;
    
    public OrderVO createOrder(CreateOrderDTO dto) {
        // 调用用户服务
        GetUserResponse user = userService.getUser(
            GetUserRequest.newBuilder()
                .setUserId(dto.getUserId())
                .build()
        );
        
        // 创建订单...
        return orderVO;
    }
}
```

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

