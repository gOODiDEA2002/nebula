# Nebula Starter Service Example

> 使用 `nebula-starter-service` 的微服务 RPC 示例

## 功能特性

- 基于 `nebula-starter-service`，集成 RPC + 服务发现 + 分布式锁
- 使用 `@RemoteService` 定义 RPC 接口
- 使用 `@RpcService` 实现 RPC 服务
- 使用 `@RpcCall` 定义 HTTP 路由映射
- 同时支持 HTTP RPC 和 gRPC 两种协议

## 项目结构

```
starter-service-example/
├── pom.xml
└── src/main/
    ├── java/io/nebula/examples/service/
    │   ├── ServiceApplication.java          # 启动类
    │   ├── api/
    │   │   ├── HelloRpcClient.java          # RPC 接口定义
    │   │   └── ServiceInfoDto.java          # 服务信息 DTO
    │   └── rpc/
    │       └── HelloRpcClientImpl.java      # RPC 服务实现
    └── resources/
        └── application.yml                  # 应用配置
```

## 前置条件

- JDK 21+
- Maven 3.8+
- Nacos（可选，默认已禁用）

## 快速开始

```bash
# 1. 安装框架到本地仓库（首次需要）
cd /path/to/nebula
mvn install -DskipTests

# 2. 启动应用（端口 8082）
mvn -q -f examples/starter-service-example spring-boot:run
```

## 接口测试

```bash
# 简单问候（通过 RPC 路由）
curl http://localhost:8082/rpc/hello

# 带参数问候
curl "http://localhost:8082/rpc/hello/greet?name=Nebula"

# 获取服务信息
curl http://localhost:8082/rpc/hello/info
```

## RPC 接口定义方式

```java
// 1. 使用 @RemoteService 标记接口
@RemoteService
public interface HelloRpcClient {

    // 2. 使用 @RpcCall 定义 HTTP 路由
    @RpcCall(value = "/rpc/hello", method = "GET")
    String hello();

    @RpcCall(value = "/rpc/hello/greet", method = "GET")
    String greet(@RequestParam("name") String name);
}

// 3. 使用 @RpcService 标记实现类
@RpcService
public class HelloRpcClientImpl implements HelloRpcClient {
    @Override
    public String hello() {
        return "Hello, Nebula Service!";
    }
}
```

## 配置说明

```yaml
server:
  port: 8082

nebula:
  discovery:
    nacos:
      enabled: false    # 默认禁用，启用需配置 Nacos 地址
  rpc:
    http:
      enabled: false    # 默认禁用 HTTP RPC 客户端
    grpc:
      enabled: false    # 默认禁用 gRPC
  lock:
    enabled: true       # 启用分布式锁
    enable-aspect: true # 启用 @Locked 切面
```

> 提示：启用 Nacos 后，其他微服务可通过 `@RemoteService` 自动发现并调用本服务。

## 相关文档

- [Nebula Examples 总览](../README.md)
- [nebula-starter-service](../../starter/nebula-starter-service/pom.xml)
- [nebula-starter-api（RPC 契约定义）](../starter-api-example/README.md)
- [微服务示例（多模块拆分）](../microservice-example/README.md)
