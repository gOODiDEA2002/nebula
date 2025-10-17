# Nebula RPC 优化任务清单

## 任务概述

本文档列出了 Nebula RPC 框架优化的详细任务清单，包括具体的实施步骤、文件修改和验证方法。

## 任务分解

### ✅ 优化1: @RpcClient contextId 自动推导

**状态：已完成**

当前 `RpcClientScannerRegistrar.generateBeanName()` 已经实现了自动推导：
- 如果 contextId 为空，使用接口简单类名首字母小写
- 例如：`AuthRpcClient` -> `authRpcClient`

**无需额外工作。**

---

### 🔨 优化2: 创建自动配置类

**目标：** 在 `nebula-example-user-api` 模块中添加自动配置，无需 `@EnableRpcClients`

#### 任务 2.1: 创建自动配置类

- [ ] 文件：`nebula-example-user-api/src/main/java/io/nebula/example/api/config/UserApiAutoConfiguration.java`
- [ ] 功能：
  - 实现 `ImportBeanDefinitionRegistrar` 接口
  - 扫描 `io.nebula.example.api.rpc` 包下所有 `@RpcClient` 接口
  - 为每个接口注册 `RpcClientFactoryBean`
  - 使用 `@ConditionalOnMissingBean` 避免重复注册

**参考代码：**
```java
package io.nebula.example.api.config;

import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.rpc.core.scan.RpcClientFactoryBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import java.util.Set;

@Slf4j
@AutoConfiguration
public class UserApiAutoConfiguration implements ImportBeanDefinitionRegistrar {
    
    private static final String BASE_PACKAGE = "io.nebula.example.api.rpc";
    private static final String TARGET_SERVICE_NAME = "nebula-example-user-service";
    
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        log.info("开始自动注册 User API RPC 客户端...");
        
        ClassPathScanningCandidateComponentProvider scanner = 
            new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    return beanDefinition.getMetadata().isInterface();
                }
            };
        
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcClient.class));
        
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(BASE_PACKAGE);
        
        for (BeanDefinition candidate : candidates) {
            if (candidate instanceof AnnotatedBeanDefinition) {
                registerRpcClient((AnnotatedBeanDefinition) candidate, registry);
            }
        }
        
        log.info("User API RPC 客户端自动注册完成，共注册 {} 个客户端", candidates.size());
    }
    
    private void registerRpcClient(AnnotatedBeanDefinition definition, BeanDefinitionRegistry registry) {
        String className = definition.getMetadata().getClassName();
        
        try {
            Class<?> clientClass = Class.forName(className);
            String beanName = generateBeanName(clientClass);
            
            // 检查是否已经注册
            if (registry.containsBeanDefinition(beanName)) {
                log.debug("RPC 客户端 {} 已存在，跳过注册", beanName);
                return;
            }
            
            // 构建 Bean 定义
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(RpcClientFactoryBean.class);
            builder.addPropertyValue("type", clientClass);
            
            // 注册 Bean
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
            
            log.info("自动注册 RPC 客户端: {} -> {}", className, beanName);
            
        } catch (ClassNotFoundException e) {
            log.error("无法加载 RPC 客户端类: {}", className, e);
        }
    }
    
    private String generateBeanName(Class<?> clientClass) {
        RpcClient annotation = clientClass.getAnnotation(RpcClient.class);
        
        // 优先使用 contextId
        if (annotation != null && StringUtils.hasText(annotation.contextId())) {
            return annotation.contextId();
        }
        
        // 使用类名（首字母小写）
        String className = clientClass.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
}
```

#### 任务 2.2: 创建 AutoConfiguration.imports 文件

- [ ] 文件：`nebula-example-user-api/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- [ ] 内容：
```
io.nebula.example.api.config.UserApiAutoConfiguration
```

#### 任务 2.3: 更新 pom.xml（如需要）

- [ ] 确保 `spring-boot-autoconfigure-processor` 依赖

#### 任务 2.4: 移除显式配置

- [ ] 文件：`nebula-example-order-service/src/main/java/.../NebulaExampleOrderServiceApplication.java`
- [ ] 修改：移除 `@EnableRpcClients(basePackageClasses = {...})`
- [ ] 改为：`@EnableRpcClients` 或完全移除（如果自动配置生效）

#### 任务 2.5: 测试自动配置

- [ ] 启动 `nebula-example-order-service`
- [ ] 验证 RPC 客户端自动注册成功
- [ ] 验证 RPC 调用功能正常

---

### 🔨 优化3: @RpcService 自动推导接口

**目标：** `@RpcService` 注解无需指定接口类

#### 任务 3.1: 修改 @RpcService 注解

- [ ] 文件：`nebula/infrastructure/rpc/nebula-rpc-core/src/main/java/io/nebula/rpc/core/annotation/RpcService.java`
- [ ] 修改：
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RpcService {
    
    /**
     * 实现的RPC服务接口类
     * 如果为 void.class，则自动查找标注了 @RpcClient 的接口
     */
    Class<?> value() default void.class;  // 修改：添加默认值
    
    /**
     * 服务名称，默认使用接口的全限定名
     */
    String serviceName() default "";
}
```

#### 任务 3.2: 修改 RpcServiceRegistrationProcessor (HTTP)

- [ ] 文件：`nebula/infrastructure/rpc/nebula-rpc-http/src/main/java/.../RpcServiceRegistrationProcessor.java`
- [ ] 添加方法：
```java
/**
 * 查找服务接口
 * 如果 @RpcService 没有指定接口，自动查找标注了 @RpcClient 的接口
 */
private Class<?> findServiceInterface(Class<?> beanClass, RpcService rpcService) {
    // 1. 如果手动指定了接口，直接使用
    Class<?> specifiedInterface = rpcService.value();
    if (specifiedInterface != null && specifiedInterface != void.class) {
        return specifiedInterface;
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
        throw new IllegalStateException(String.format(
            "类 %s 没有实现任何标注了 @RpcClient 的接口，请在 @RpcService 中手动指定接口类",
            beanClass.getName()));
    }
    
    if (rpcInterfaces.size() > 1) {
        throw new IllegalStateException(String.format(
            "类 %s 实现了多个 @RpcClient 接口 %s，请在 @RpcService 中手动指定接口类",
            beanClass.getName(), rpcInterfaces));
    }
    
    log.info("自动推导 RPC 服务接口: {} -> {}", 
        beanClass.getSimpleName(), rpcInterfaces.get(0).getSimpleName());
    
    return rpcInterfaces.get(0);
}
```
- [ ] 修改 `postProcessAfterInitialization` 方法：
```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    Class<?> beanClass = bean.getClass();
    
    // 检查是否有 @RpcService 注解
    RpcService rpcService = beanClass.getAnnotation(RpcService.class);
    if (rpcService == null) {
        return bean;
    }
    
    // 获取RPC接口类（自动推导或手动指定）
    Class<?> serviceInterface = findServiceInterface(beanClass, rpcService);
    
    // 确定服务名称
    String serviceName;
    if (StringUtils.hasText(rpcService.serviceName())) {
        serviceName = rpcService.serviceName();
    } else {
        serviceName = serviceInterface.getName();
    }
    
    // 注册服务
    @SuppressWarnings("unchecked")
    Class<Object> interfaceClass = (Class<Object>) serviceInterface;
    rpcServer.registerService(serviceName, interfaceClass, bean);
    log.info("自动注册RPC服务: serviceName={}, interface={}, implementation={}", 
            serviceName, serviceInterface.getSimpleName(), beanClass.getSimpleName());
    
    return bean;
}
```

#### 任务 3.3: 修改 gRPC 的 RpcServiceRegistrationProcessor

- [ ] 文件：`nebula/infrastructure/rpc/nebula-rpc-grpc/src/main/java/.../GrpcServiceRegistrationProcessor.java`
- [ ] 应用相同的修改逻辑

#### 任务 3.4: 更新实现类

- [ ] 文件：`nebula-example-order-service/src/main/java/.../OrderRpcClientImpl.java`
- [ ] 修改：
```java
// 修改前
@RpcService(OrderRpcClient.class)
@RequiredArgsConstructor
public class OrderRpcClientImpl implements OrderRpcClient {
    // ...
}

// 修改后
@RpcService  // 移除接口类参数
@RequiredArgsConstructor
public class OrderRpcClientImpl implements OrderRpcClient {
    // ...
}
```

#### 任务 3.5: 测试接口推导

- [ ] 测试正常情况：实现单个 @RpcClient 接口
- [ ] 测试异常情况：不实现任何接口
- [ ] 测试异常情况：实现多个 @RpcClient 接口
- [ ] 验证错误信息清晰准确

---

### ✅ 优化4: 自动注入无需 @Qualifier

**状态：通过优化1和2自然实现**

#### 验证步骤

- [ ] 文件：`nebula-example-order-service/src/main/java/.../OrderRpcClientImpl.java`
- [ ] 修改：
```java
// 修改前
@Qualifier("userRpcClient")
private final UserRpcClient userRpcClient;

@Qualifier("authRpcClient")
private final AuthRpcClient authRpcClient;

// 修改后
private final UserRpcClient userRpcClient;
private final AuthRpcClient authRpcClient;
```
- [ ] 验证：依赖注入正常工作

---

## 文档更新任务

### 任务 D.1: 更新 nebula-example-user-api/README.md

- [ ] 添加自动配置说明
- [ ] 更新使用示例
- [ ] 说明无需 @EnableRpcClients

### 任务 D.2: 更新 nebula-example-order-service/README.md

- [ ] 更新启动类示例
- [ ] 更新服务实现示例
- [ ] 更新依赖注入示例

### 任务 D.3: 创建迁移指南

- [ ] 文件：`nebula/docs/RPC_MIGRATION_GUIDE.md`
- [ ] 内容：
  - 从旧版本到新版本的迁移步骤
  - 向后兼容性说明
  - 常见问题解答

---

## 测试任务

### 单元测试

- [ ] 测试 `UserApiAutoConfiguration` 注册逻辑
- [ ] 测试 `findServiceInterface` 接口推导逻辑
- [ ] 测试边界情况

### 集成测试

- [ ] 测试自动配置的 Bean 注册
- [ ] 测试 HTTP RPC 调用
- [ ] 测试 gRPC 调用（如果启用）

### 回归测试

- [ ] 验证显式配置仍然有效
- [ ] 验证向后兼容性
- [ ] 测试混合使用场景

---

## 实施顺序建议

1. **阶段一：** 优化3 (@RpcService 自动推导)
   - 修改注解和处理器
   - 在 order-service 中验证

2. **阶段二：** 优化2 (自动配置)
   - 创建自动配置类
   - 移除 @EnableRpcClients
   - 验证自动注入

3. **阶段三：** 文档和测试
   - 更新文档
   - 完善测试
   - 迁移指南

---

## 验收标准

### 功能验收

- [ ] 无需在 @RpcClient 中指定 contextId
- [ ] 无需在启动类中添加 @EnableRpcClients
- [ ] 无需在 @RpcService 中指定接口类
- [ ] 无需使用 @Qualifier 注入 RPC 客户端

### 兼容性验收

- [ ] 显式配置仍然有效（向后兼容）
- [ ] 现有代码无需修改即可运行
- [ ] 可以混合使用自动和手动配置

### 文档验收

- [ ] README 文档准确完整
- [ ] 使用示例清晰易懂
- [ ] 迁移指南详细可操作

---

## 注意事项

1. **向后兼容：** 所有修改必须保持向后兼容
2. **错误处理：** 提供清晰的错误信息
3. **日志记录：** 添加适当的调试日志
4. **代码质量：** 保持代码整洁，添加必要的注释
5. **测试覆盖：** 确保关键逻辑有单元测试

---

## 当前状态

- ✅ 优化1: 已完成（框架已支持）
- 🔨 优化2: 待实施
- 🔨 优化3: 待实施
- ✅ 优化4: 通过优化1和2自然实现

