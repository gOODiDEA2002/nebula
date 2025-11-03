# Nebula RPC 优化设计文档

## 背景

当前 Nebula RPC 框架需要大量手动配置，增加了开发者的负担本文档设计了四项优化方案，旨在简化使用方式，提供更好的开发体验

## 优化需求

### 优化1: @RpcClient 自动推导配置

**当前问题：**
```java
@RpcClient(
    value = "nebula-example-user-service",  // 需要手动指定目标服务名
    contextId = "authRpcClient"              // 需要手动指定上下文ID
)
public interface AuthRpcClient {
    @RpcCall(value = "/rpc/auth", method = "POST")
    AuthDto.Response auth(@RequestBody AuthDto.Request request);
}
```

**期望效果：**
```java
@RpcClient  // 无需任何配置
public interface AuthRpcClient {
    @RpcCall(value = "/rpc/auth", method = "POST")
    AuthDto.Response auth(@RequestBody AuthDto.Request request);
}
```

**现状分析：**
-  `contextId` 已支持默认值（见 `RpcClientScannerRegistrar.generateBeanName()`）
-  当 `contextId` 为空时，自动使用接口简单类名首字母小写
- ️ `value` (目标服务名) 当前默认使用接口全限定名，不够灵活

**解决方案：**
1. 保持现有的 `contextId` 自动推导逻辑（已实现）
2. 对于 `value`，提供三种方式：
   - 方式A：手动指定（保持向后兼容）
   - 方式B：在 -api 模块的自动配置中统一指定
   - 方式C：为空时使用约定命名规则推导

### 优化2: 自动扫描无需 @EnableRpcClients

**当前问题：**
```java
@SpringBootApplication
@EnableRpcClients(basePackageClasses = {UserRpcClient.class, AuthRpcClient.class})  
public class NebulaExampleOrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NebulaExampleOrderServiceApplication.class, args);
    }
}
```

**期望效果：**
```java
@SpringBootApplication
public class NebulaExampleOrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NebulaExampleOrderServiceApplication.class, args);
    }
}
```

**解决方案：**
参考 `lark-example-service-contract` 模式，在 `nebula-example-user-api` 模块中：

1. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
2. 创建 `UserApiAutoConfiguration` 类
3. 使用 `ImportBeanDefinitionRegistrar` 自动扫描和注册所有 RPC 客户端

**架构设计：**
```
nebula-example-user-api/
 src/main/java/io/nebula/example/api/
    rpc/
       AuthRpcClient.java
       UserRpcClient.java
    config/
        UserApiAutoConfiguration.java  (新增)
 src/main/resources/
     META-INF/spring/
         org.springframework.boot.autoconfigure.AutoConfiguration.imports  (新增)
```

### 优化3: @RpcService 自动推导接口

**当前问题：**
```java
@RpcService(OrderRpcClient.class)  // 需要手动指定接口类
@RequiredArgsConstructor
public class OrderRpcClientImpl implements OrderRpcClient {
    // ...
}
```

**期望效果：**
```java
@RpcService  // 无需指定接口
@RequiredArgsConstructor
public class OrderRpcClientImpl implements OrderRpcClient {
    // ...
}
```

**现状分析：**
- 当前 `@RpcService.value()` 是必填的 `Class<?>` 类型
- `RpcServiceRegistrationProcessor` 依赖 `value()` 获取接口类

**解决方案：**
1. 修改 `@RpcService` 注解，使 `value()` 可选：
   ```java
   Class<?> value() default void.class;
   ```
2. 修改 `RpcServiceRegistrationProcessor`：
   - 如果 `value()` 为 `void.class`，通过反射查找实现的接口
   - 优先选择标注了 `@RpcClient` 的接口
   - 如果找不到或有多个，抛出清晰的错误信息

**实现逻辑：**
```java
private Class<?> findServiceInterface(Class<?> beanClass, RpcService rpcService) {
    // 1. 如果手动指定了接口，直接使用
    if (rpcService.value() != void.class) {
        return rpcService.value();
    }
    
    // 2. 自动查找标注了 @RpcClient 的接口
    Class<?>[] interfaces = beanClass.getInterfaces();
    List<Class<?>> rpcInterfaces = new ArrayList<>();
    
    for (Class<?> iface : interfaces) {
        if (iface.isAnnotationPresent(RpcClient.class)) {
            rpcInterfaces.add(iface);
        }
    }
    
    // 3. 验证结果
    if (rpcInterfaces.isEmpty()) {
        throw new IllegalStateException(
            "类 " + beanClass.getName() + " 没有实现任何标注了 @RpcClient 的接口");
    }
    
    if (rpcInterfaces.size() > 1) {
        throw new IllegalStateException(
            "类 " + beanClass.getName() + " 实现了多个 @RpcClient 接口，请手动指定");
    }
    
    return rpcInterfaces.get(0);
}
```

### 优化4: 自动注入无需 @Qualifier

**当前问题：**
```java
@Qualifier("userRpcClient")
private final UserRpcClient userRpcClient;

@Qualifier("authRpcClient")
private final AuthRpcClient authRpcClient;
```

**期望效果：**
```java
private final UserRpcClient userRpcClient;
private final AuthRpcClient authRpcClient;
```

**现状分析：**
-  通过优化1，Bean 名称已经是接口简单类名首字母小写
-  `generateBeanName()` 方法已经实现了这个逻辑
-  只要 Bean 名称与字段名匹配，Lombok 的 `@RequiredArgsConstructor` 就可以正常注入

**验证：**
- `AuthRpcClient` -> Bean 名称: `authRpcClient`
- `UserRpcClient` -> Bean 名称: `userRpcClient`
- 字段名与 Bean 名称一致，可以直接注入

**结论：**
此优化无需额外工作，通过优化2（自动配置）自然实现

## 实现方案对比

### 方案A: 修改 Nebula 核心框架（推荐）

**优势：**
- 所有项目自动受益
- 框架级别的改进
- 统一的使用体验

**劣势：**
- 需要修改核心代码
- 需要充分测试兼容性

**实施范围：**
- 修改 `nebula-rpc-core` 模块
- 修改 `nebula-rpc-http` 和 `nebula-rpc-grpc` 模块
- 保持向后兼容

### 方案B: 仅在 -api 模块中实现（快速方案）

**优势：**
- 不修改框架核心
- 快速实施
- 风险较低

**劣势：**
- 每个 -api 模块都需要配置
- 不是通用解决方案

**实施范围：**
- 仅在 `nebula-example-user-api` 中添加自动配置
- 其他项目需要复制类似配置

## 推荐实施方案

**采用方案A + 方案B 的混合方式：**

1. **优化1和4：** 框架级别改进（方案A）
   - 已经基本实现，无需修改

2. **优化2：** -api 模块自动配置（方案B）
   - 在 `nebula-example-user-api` 中实现
   - 可以作为模板供其他项目复制

3. **优化3：** 框架级别改进（方案A）
   - 修改 `@RpcService` 注解
   - 修改 `RpcServiceRegistrationProcessor`
   - 保持向后兼容

## 实施计划

### 阶段一：优化3 (@RpcService 自动推导)
- [ ] 修改 `@RpcService` 注解定义
- [ ] 修改 `RpcServiceRegistrationProcessor` 添加接口自动查找逻辑
- [ ] 添加单元测试
- [ ] 在 `nebula-example-order-service` 中验证

### 阶段二：优化2 (自动配置)
- [ ] 在 `nebula-example-user-api` 中创建 `UserApiAutoConfiguration`
- [ ] 创建 `META-INF/spring/...AutoConfiguration.imports`
- [ ] 实现自动扫描和注册逻辑
- [ ] 移除 `nebula-example-order-service` 中的 `@EnableRpcClients`
- [ ] 测试自动注入功能

### 阶段三：文档和示例更新
- [ ] 更新 `nebula-example-user-api/README.md`
- [ ] 更新 `nebula-example-order-service/README.md`
- [ ] 添加使用示例
- [ ] 更新架构文档

## 兼容性考虑

1. **向后兼容：**
   - 所有现有的显式配置依然有效
   - 自动推导作为默认行为
   - 显式配置优先级更高

2. **迁移路径：**
   - 现有代码无需修改即可运行
   - 可以逐步移除显式配置
   - 提供迁移指南

## 风险评估

| 优化项 | 风险等级 | 主要风险 | 缓解措施 |
|--------|---------|---------|---------|
| 优化1 | 低 | 已实现，无风险 | - |
| 优化2 | 中 | 自动配置可能与现有配置冲突 | 使用 `@ConditionalOnMissingBean` |
| 优化3 | 中 | 接口推导可能出错 | 提供清晰的错误信息，保留手动指定方式 |
| 优化4 | 低 | 通过优化1和2自然实现 | - |

## 测试策略

1. **单元测试：**
   - 测试 `@RpcService` 自动推导逻辑
   - 测试边界情况（无接口多接口）

2. **集成测试：**
   - 测试自动配置的 Bean 注册
   - 测试 RPC 调用功能

3. **回归测试：**
   - 确保显式配置仍然有效
   - 测试向后兼容性

## 参考实现

- Lark Framework: `lark-example-service-contract`
- Spring Cloud OpenFeign: 自动配置机制
- Spring Data: Repository 自动扫描

