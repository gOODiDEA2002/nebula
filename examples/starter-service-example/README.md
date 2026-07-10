# Nebula Starter Service Example

> 使用 `nebula-starter-service` 的微服务 RPC 示例

## 功能特性

- 基于 `nebula-starter-service`，集成 RPC + 服务发现 + 分布式锁
- 使用 `@RemoteService` 定义 RPC 接口
- 使用 `@RpcService` 实现 RPC 服务
- 使用应用层 Controller 暴露三个示例 GET 接口
- 启用 HTTP RPC Server，保留通用 `POST /rpc` 服务间调用入口
- 使用 Redis Lock 执行真实受锁回调

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
    │   ├── rpc/
    │   │   └── HelloRpcClientImpl.java      # RPC 服务实现
    │   └── controller/
    │       ├── HelloController.java          # HTTP 示例接口
    │       └── LockController.java           # 分布式锁示例接口
    └── resources/
        └── application.yml                  # 应用配置
```

## 前置条件

- JDK 21+
- Maven 3.8+
- Redis（默认 `localhost:6379`，用于分布式锁）
- Nacos（可选，默认已禁用）

Redis 地址可通过 `REDIS_HOST`、`REDIS_PORT` 和 `REDIS_PASSWORD` 环境变量覆盖。

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
# 简单问候（应用层 HTTP 接口）
curl http://localhost:8082/rpc/hello

# 带参数问候
curl "http://localhost:8082/rpc/hello/greet?name=Nebula"

# 获取服务信息
curl http://localhost:8082/rpc/hello/info

# 执行一次真实 Redis 分布式锁回调
curl "http://localhost:8082/lock/execute?key=starter-service-demo"
```

## RPC 接口定义方式

```java
// 1. 使用 @RemoteService 标记接口
@RemoteService
public interface HelloRpcClient {

    // 2. 使用 @RpcCall 描述 RPC 调用元数据
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
    discovery:
      enabled: false    # 单应用示例不启用服务发现客户端
    http:
      enabled: true     # 启用 HTTP RPC Server
      client:
        enabled: false  # 本示例只演示服务端
    grpc:
      enabled: false    # 默认禁用 gRPC
  lock:
    enabled: true       # 启用分布式锁
    enable-aspect: true # 启用 @Locked 切面
```

> 提示：启用 Nacos 后，其他微服务可通过 `@RemoteService` 自动发现并调用本服务。

## E2E 验证

```bash
E2E_MODE=full E2E_WITH_MIDDLEWARE=false \
  E2E_ONLY=starter-service-example examples/e2e-all.sh
```

完整验证会调用三个 GET 接口和通用 `POST /rpc`，执行一次真实 Redis Lock，并确认 Redis 不可达时
应用快速失败。

## 相关文档

- [Nebula Examples 总览](../README.md)
- [nebula-starter-service](../../starter/nebula-starter-service/pom.xml)
- [nebula-starter-api（RPC 契约定义）](../starter-api-example/README.md)
- [微服务示例（多模块拆分）](../microservice-example/README.md)
