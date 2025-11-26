# Nebula RPC HTTP 配置指南

> HTTP RPC配置说明

## 概述

`nebula-rpc-http` 提供基于 HTTP/REST 的 RPC 支持。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-rpc-http</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 客户端配置

```yaml
nebula:
  rpc:
    http:
      client:
        # 连接超时
        connect-timeout: 5s
        # 读取超时
        read-timeout: 30s
        # 最大连接数
        max-connections: 200
        # 重试配置
        retry:
          enabled: true
          max-attempts: 3
```

## 票务系统场景

### 服务调用

```yaml
nebula:
  rpc:
    http:
      client:
        services:
          user-service:
            base-url: http://user-service:8080
            timeout: 10s
          payment-service:
            base-url: http://payment-service:8080
            timeout: 60s  # 支付接口超时时间长
```

### 使用示例

```java
@FeignClient(name = "user-service", url = "${nebula.rpc.http.client.services.user-service.base-url}")
public interface UserApiClient {
    
    @GetMapping("/api/users/{id}")
    Result<UserVO> getUser(@PathVariable("id") Long id);
}

@Service
public class OrderService {
    
    @Autowired
    private UserApiClient userApiClient;
    
    public OrderVO createOrder(CreateOrderDTO dto) {
        // HTTP RPC调用
        Result<UserVO> userResult = userApiClient.getUser(dto.getUserId());
        UserVO user = userResult.getData();
        
        // 创建订单...
        return orderVO;
    }
}
```

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

