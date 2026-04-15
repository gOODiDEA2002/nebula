# Nebula Example API

## 模块说明

这是一个独立的 API 契约模块（API Contract Module），包含 RPC 接口定义DTOVO 等，用于在服务提供方和消费方之间共享接口契约

## 设计原则

遵循微服务架构的最佳实践：
- **接口与实现分离**: RPC 接口定义独立于具体实现
- **契约共享**: 服务提供方和消费方依赖同一份接口定义
- **版本管理**: 接口变更可以独立版本控制
- **降低耦合**: 避免实现细节泄漏到接口定义中

## 模块结构

```
nebula-example-api/
 src/main/java/io/nebula/example/api/
    rpc/                    # RPC服务接口定义
       UserRpcService.java
    dto/                    # 数据传输对象（Request/Response）
       CreateUserDto.java
       GetUserDto.java
       UpdateUserDto.java
       DeleteUserDto.java
       GetUsersDto.java
    vo/                     # 视图对象
       UserVo.java
    entity/                 # 实体对象
        User.java
 pom.xml
```

## 依赖关系

### 架构图

```
nebula-example-api (契约层)
                  
    依赖           依赖
                  
nebula-example-  nebula-example
   service          (client)
  (提供者)         (消费者)
```

### 谁应该依赖这个模块

1. **服务提供方** (nebula-example-service): 
   - 依赖此模块获取接口定义
   - 实现接口中定义的服务
   - 使用 `@RpcService` 注解发布服务

2. **服务消费方** (nebula-example): 
   - 依赖此模块获取接口定义  
   - 通过 `@RemoteService` 注解自动生成客户端代理
   - 注入接口即可调用远程服务

### 依赖配置

在 nebula-example 的 pom.xml 中添加：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-example-rpc-api</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

## 使用示例

### 服务提供方

使用 `@RpcService` 注解实现 RPC 服务端:

```java
// 在 nebula-example-service 模块中实现接口
@RpcService(UserRpcService.class)
@RequiredArgsConstructor
public class UserRpcServiceImpl implements UserRpcService {
    
    private final RpcDemoService rpcDemoService;
    
    @Override
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        log.info("RPC服务端: createUser, username={}", request.getUsername());
        return rpcDemoService.createUser(request);
    }
    
    @Override
    public GetUserDto.Response getUserById(Long id) {
        log.info("RPC服务端: getUserById, id={}", id);
        GetUserDto.Request request = new GetUserDto.Request();
        request.setId(id);
        return rpcDemoService.getUserById(request);
    }
    
    // ... 其他方法实现
}
```

**说明**:
- `@RpcService` 注解会自动将服务实现注册到 RPC 服务器
- 服务实现类无需添加 `@Controller` 或 `@Service` 注解
- `@RequiredArgsConstructor` 用于依赖注入

### 服务消费方

首先在启动类上启用 RPC 客户端扫描:

```java
@SpringBootApplication
@EnableRpcClients(basePackages = "io.nebula.example.api.rpc")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

然后在服务中注入并使用 RPC 客户端:

```java
// 在另一个服务或控制器中使用
@Service
@RequiredArgsConstructor
public class UserClientService {
    
    private final UserRpcService userRpcService;  // 自动注入 RPC 客户端代理
    
    public void processUser(Long userId) {
        GetUserDto.Response response = userRpcService.getUserById(userId);
        // 处理用户信息
        UserVo user = response.getUser();
        log.info("获取到用户: {}", user.getName());
    }
}
```

**说明**:
- `@EnableRpcClients` 启用 RPC 客户端自动扫描和注册
- `UserRpcService` 会被自动创建为动态代理并注入到 Spring 容器
- 调用接口方法时,RPC 框架会自动处理序列化网络传输和反序列化

## 最佳实践

1. **只包含接口定义**: 不要包含具体实现
2. **DTO 规范**: 严格遵循 Request/Response 结构
3. **参数验证**: 在 DTO 中添加 Jakarta Validation 注解
4. **文档注释**: 为所有接口和 DTO 添加清晰的 JavaDoc
5. **版本控制**: 接口变更要考虑向后兼容性

## 注意事项

- 此模块应保持轻量，只依赖必要的框架（RPC CoreValidation 等）
- 不要引入业务逻辑或第三方业务库
- 接口变更需要同时更新服务提供方和消费方

## 架构演进说明

### v2.0.0 架构重构

在 v2.0.0 版本中，我们将原来混合了服务提供者和消费者的单体模块重构为清晰的三层架构：

**之前（单体架构）：**
```
nebula-example-api/          契约定义
nebula-example/              同时包含服务端实现和客户端调用（ 职责混淆）
```

**现在（三层架构）：**
```
nebula-example-api/          契约层：接口定义
nebula-example-service/      服务端层：接口实现
nebula-example/              客户端层：服务调用
```

**优势：**
-  职责清晰：每个模块职责单一
-  易于理解：学习者一目了然
-  便于扩展：可独立部署水平扩展
-  符合最佳实践：遵循业界标准架构模式

**相关文档：**
- [服务提供者文档](../nebula-example-service/README.md)
- [服务消费者文档](../nebula-example/README.md)
- [整体架构说明](../ARCHITECTURE.md)

