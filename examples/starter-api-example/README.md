# Nebula Starter API Example

> 使用 `nebula-starter-api` 的 RPC 契约定义示例

## 功能特性

- 基于 `nebula-starter-api`，仅用于定义 RPC 接口契约
- 使用 `@RemoteService` 声明远程服务接口
- 输出为 JAR 包，供服务提供方和消费方共同依赖
- **非可运行项目**，不包含启动类

## 项目结构

```
starter-api-example/
├── pom.xml
└── src/main/java/io/nebula/examples/api/
    └── UserApi.java               # RPC 接口定义
```

## 前置条件

- JDK 21+
- Maven 3.8+
- 无外部依赖

## 使用方式

```bash
# 编译并安装到本地仓库
mvn install -f examples/starter-api-example
```

## 接口定义示例

```java
@RemoteService(name = "example-service")
public interface UserApi {
    String hello(String name);
}
```

## 在微服务中使用

### 服务提供方（实现接口）

```java
@RpcService
public class UserApiImpl implements UserApi {
    @Override
    public String hello(String name) {
        return "Hello, " + name;
    }
}
```

### 服务消费方（注入调用）

```java
@Service
public class OrderService {
    @Autowired
    private UserApi userApi;   // 框架自动代理

    public String greetUser(String name) {
        return userApi.hello(name);
    }
}
```

### pom.xml 依赖

```xml
<!-- 服务提供方和消费方都添加此依赖 -->
<dependency>
    <groupId>io.nebula.examples</groupId>
    <artifactId>starter-api-example</artifactId>
    <version>${revision}</version>
</dependency>
```

## 设计原则

```
                    starter-api-example (契约 JAR)
                    ┌────────────────────┐
                    │  @RemoteService    │
                    │  UserApi.java      │
                    │  DTO / VO          │
                    └────────┬───────────┘
                             │
                ┌────────────┼────────────┐
                │                         │
        服务提供方                    服务消费方
   ┌──────────────────┐      ┌──────────────────┐
   │  @RpcService     │      │  @Autowired      │
   │  UserApiImpl     │      │  UserApi userApi  │
   │  (实现接口)       │      │  (框架代理注入)    │
   └──────────────────┘      └──────────────────┘
```

> API 模块只包含接口定义和 DTO，不包含任何业务实现。
> 这种模式确保服务间通过契约而非实现耦合。

## 相关文档

- [Nebula Examples 总览](../README.md)
- [nebula-starter-api](../../starter/nebula-starter-api/pom.xml)
- [微服务示例（多模块拆分实践）](../microservice-example/README.md)
- [微服务 RPC 示例](../starter-service-example/README.md)
