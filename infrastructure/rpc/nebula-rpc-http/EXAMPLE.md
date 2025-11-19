# nebula-rpc-http 模块示例

## 模块简介

`nebula-rpc-http` 是 Nebula 框架基于 HTTP 协议（通常使用 OkHttp 或 HttpClient）实现的 RPC 模块。它提供了轻量级、基于标准 HTTP/REST 的远程调用能力，易于调试和跨语言集成。

## 核心功能示例

### 1. 配置 HTTP RPC

在 `application.yml` 中配置 HTTP 服务器和客户端参数。

**`application.yml`**:

```yaml
nebula:
  rpc:
    http:
      enabled: true
      
      # 服务端配置 (暴露服务)
      server:
        enabled: true
        port: 8080            # 复用 Spring Boot Web 端口或指定独立端口
        context-path: /rpc    # RPC 调用的基础路径
        max-request-size: 10485760 # 10MB
        request-timeout: 60000     # 60s
      
      # 客户端配置 (调用服务)
      client:
        enabled: true
        connect-timeout: 5000  # 连接超时 5s
        read-timeout: 10000    # 读取超时 10s
        max-connections: 200   # 连接池大小
        retry-count: 3         # 失败重试次数
        logging-enabled: true  # 打印请求日志
```

### 2. 暴露 HTTP RPC 服务

实现 `@RpcService` 接口。`nebula-rpc-http` 会自动将这些服务映射为 HTTP Controller 接口。

**`io.nebula.example.http.provider.EchoServiceImpl`**:

```java
package io.nebula.example.http.provider;

import io.nebula.rpc.core.annotation.RpcService;
import org.springframework.stereotype.Service;

@Service
@RpcService(serviceName = "echo-service")
public class EchoServiceImpl implements EchoService {

    @Override
    public String echo(String message) {
        return "Echo: " + message;
    }
}
```

假设 `EchoService` 定义如下：
```java
@RpcClient("echo-service")
public interface EchoService {
    @RpcCall(path = "/echo")
    String echo(String message);
}
```

该服务将暴露在 `http://localhost:8080/rpc/echo-service/echo` (取决于具体实现映射规则，通常是 `/{context-path}/{service-name}/{method-path}` 或类名/方法名映射)。

### 3. 调用 HTTP RPC 服务

使用 `@RpcClient` 定义的接口进行调用。客户端底层会发送 HTTP 请求。

**`io.nebula.example.http.consumer.ConsumerService`**:

```java
package io.nebula.example.http.consumer;

import io.nebula.example.http.provider.EchoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerService {

    private final EchoService echoService;

    public void doEcho() {
        String result = echoService.echo("Hello HTTP");
        log.info("RPC Result: {}", result);
    }
}
```

## 进阶特性

### 1. 自定义 HTTP 头

可以在 `@RpcCall` 中指定 headers，用于传递认证 Token 等信息。

```java
@RpcCall(path = "/secure/data", headers = {"Authorization: Bearer ${token}"})
String getSecureData();
```

### 2. 负载均衡

如果集成了 `nebula-discovery`，HTTP 客户端会自动解析服务名（如 `http://echo-service/rpc/...`），并根据负载均衡策略选择具体的 IP 和端口进行请求。

### 3. 与 Spring MVC 互操作

由于 `nebula-rpc-http` 基于标准 HTTP，你可以使用任何 HTTP 客户端（如 Postman, curl, 浏览器）直接调用 RPC 接口，方便调试。

```bash
curl -X POST http://localhost:8080/rpc/echo-service/echo \
     -H "Content-Type: application/json" \
     -d '["Hello World"]'
```
*(注：请求体格式取决于具体的序列化协议，默认为 JSON)*

## 总结

`nebula-rpc-http` 是一种简单、通用的 RPC 实现，特别适合对性能要求不是极致苛刻，但对互操作性和调试便利性有较高要求的场景。

