# Nebula Framework 自动配置注册指南

本文档说明如何在 Nebula Framework 中正确注册自动配置类，避免常见的 `ClassNotFoundException` 问题。

## 问题背景

在 Spring Boot 3.x 中，自动配置类通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件注册。当一个模块被引入时，其中注册的所有配置类都会被尝试加载。

### 常见问题

当在 `nebula-autoconfigure` 中注册位于其他模块的配置类时，如果该模块未被引入，会出现：

```
java.lang.IllegalStateException: Unable to read meta-data for class xxx.AutoConfiguration
Caused by: java.io.FileNotFoundException: class path resource [xxx/AutoConfiguration.class] cannot be opened because it does not exist
```

## 解决方案对比

### 方案一：配置类定义在 autoconfigure 内部（推荐）

将配置类直接定义在 `nebula-autoconfigure` 模块内部，使用 `@ConditionalOnClass(name = "...")` 进行条件判断：

```java
package io.nebula.autoconfigure.security;

@AutoConfiguration
@ConditionalOnClass(name = {
    "io.nebula.security.jwt.JwtService",
    "io.nebula.security.config.SecurityProperties"
})
@ConditionalOnProperty(prefix = "nebula.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(JwtService.class)
    public JwtService jwtService(SecurityProperties properties) {
        return new DefaultJwtService(properties);
    }
}
```

**优点**：
- 集中管理所有自动配置
- 类不存在时优雅跳过
- 符合框架架构理念
- 无需额外的代理类

**缺点**：
- 配置类与实现类分离（但这是可接受的）

### 方案二：模块内部注册

在各模块内部创建自己的 `AutoConfiguration.imports` 文件：

```
# nebula-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.nebula.security.config.SecurityAutoConfiguration
```

**优点**：
- 实现简单
- 模块自包含

**缺点**：
- 配置分散在各模块
- 难以统一管理依赖顺序

### 方案三：条件配置类定义在 autoconfigure 内部

将配置类定义在 `nebula-autoconfigure` 模块内部，使用条件注解：

```java
@AutoConfiguration
@ConditionalOnClass(name = {"io.nebula.rpc.grpc.client.GrpcRpcClient"})
public class GrpcRpcAutoConfiguration {
    // ...
}
```

**优点**：
- 最佳实践
- 完全集中管理
- 条件加载不会抛异常

**缺点**：
- 需要将配置类从原模块移动到 autoconfigure

## 为什么其他模块可以正常工作？

以 `GrpcRpcAutoConfiguration` 为例：

```java
@AutoConfiguration
@ConditionalOnClass(name = {"io.nebula.rpc.grpc.client.GrpcRpcClient", ...})
public class GrpcRpcAutoConfiguration {
    // 配置类定义在 nebula-autoconfigure 内部
}
```

关键点：
1. 配置类定义在 `nebula-autoconfigure` 模块内部
2. 使用 `@ConditionalOnClass(name = "...")` 字符串形式
3. 当 `nebula-rpc-grpc` 未引入时，条件不满足，配置被跳过

而 `SecurityAutoConfiguration` 之前的问题：
1. 配置类定义在 `nebula-security` 模块中
2. `nebula-autoconfigure` 的 imports 文件直接引用该类的全限定名
3. Spring Boot 尝试加载类时，如果模块未引入，类不存在，抛出异常

## 最佳实践

### 1. 所有自动配置类统一放在 nebula-autoconfigure

无论是核心模块还是基础设施模块，所有自动配置类都应该定义在 `nebula-autoconfigure` 中：

```java
// 正确：配置类定义在 nebula-autoconfigure 中
package io.nebula.autoconfigure.security;

@AutoConfiguration
@ConditionalOnClass(name = "io.nebula.security.jwt.JwtService")
public class SecurityAutoConfiguration {
    // ...
}
```

```java
// 错误：配置类定义在原模块中，然后在 autoconfigure 中引用
// 这会导致类不存在时抛出 ClassNotFoundException
```

### 2. 使用字符串形式的 @ConditionalOnClass

```java
// 正确：使用 name = "..." 字符串形式
@ConditionalOnClass(name = "io.nebula.security.jwt.JwtService")

// 错误：使用 .class 引用形式（当类不存在时会抛出异常）
@ConditionalOnClass(JwtService.class)
```

### 3. 可选依赖配置

对于可选依赖模块：
- 在 `nebula-autoconfigure` 的 pom.xml 中使用 `<optional>true</optional>`
- 在配置类中使用 `@ConditionalOnClass(name = "...")` 条件判断

## 当前实现

```
nebula-autoconfigure/
  src/main/java/io/nebula/autoconfigure/
    security/
      SecurityAutoConfiguration.java      # 安全配置（直接定义在 autoconfigure 中）
    rpc/
      GrpcRpcAutoConfiguration.java       # gRPC 配置
      HttpRpcAutoConfiguration.java       # HTTP RPC 配置
    data/
      CacheAutoConfiguration.java         # 缓存配置
      DataPersistenceAutoConfiguration.java # 数据持久化配置
      
  src/main/resources/META-INF/spring/
    org.springframework.boot.autoconfigure.AutoConfiguration.imports
      # Security Layer - 配置类在 autoconfigure 内部
      io.nebula.autoconfigure.security.SecurityAutoConfiguration
      
      # RPC Layer - 配置类在 autoconfigure 内部
      io.nebula.autoconfigure.rpc.GrpcRpcAutoConfiguration
      io.nebula.autoconfigure.rpc.HttpRpcAutoConfiguration
      
      # Data Layer - 配置类在 autoconfigure 内部
      io.nebula.autoconfigure.data.CacheAutoConfiguration
      io.nebula.autoconfigure.data.DataPersistenceAutoConfiguration
```

## 验证方法

测试配置是否正确：

```bash
# 1. 不引入 nebula-security 的应用
# 应该正常启动，无 ClassNotFoundException

# 2. 引入 nebula-security 的应用
# 应该正常启动，SecurityAutoConfiguration 被加载
# 日志应显示: "初始化JWT服务"
```

