# Nebula AutoConfigure - 使用示例

> 统一自动配置模块的使用指南

## 目录

- [快速开始](#快速开始)
- [自动配置原理](#自动配置原理)
- [条件配置](#条件配置)
- [配置顺序](#配置顺序)
- [自定义配置](#自定义配置)
- [故障排查](#故障排查)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-autoconfigure</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 零配置启动

创建Spring Boot应用：

```java
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

添加配置文件 `application.yml`：

```yaml
spring:
  application:
    name: my-app

# Nebula框架会根据配置自动启用相应功能
nebula:
  # 数据持久化
  data:
    persistence:
      enabled: true
  
  # 缓存
  cache:
    enabled: true
  
  # 安全
  security:
    enabled: true
```

启动应用，相关Bean会自动注入：

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    // 自动注入的Bean
    private final CacheManager cacheManager;
    private final UserMapper userMapper;  // MyBatis Mapper
    
    public User getUserById(Long id) {
        // 使用缓存
        return cacheManager.get("user:" + id, User.class)
                .orElseGet(() -> {
                    User user = userMapper.selectById(id);
                    cacheManager.set("user:" + id, user, Duration.ofHours(1));
                    return user;
                });
    }
}
```

---

## 自动配置原理

### 1. 自动配置类结构

```java
/**
 * 数据持久化自动配置
 */
@Configuration
@ConditionalOnProperty(name = "nebula.data.persistence.enabled", havingValue = "true")
@EnableConfigurationProperties(DataPersistenceProperties.class)
@Import({
        MyBatisPlusConfiguration.class,
        DataSourceConfiguration.class,
        TransactionConfiguration.class
})
public class DataPersistenceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
        // 创建SqlSessionFactory
        return createSqlSessionFactory(dataSource);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

### 2. 配置属性绑定

```java
/**
 * 数据持久化配置属性
 */
@Data
@ConfigurationProperties(prefix = "nebula.data.persistence")
public class DataPersistenceProperties {
    
    /**
     * 是否启用数据持久化
     */
    private boolean enabled = true;
    
    /**
     * MyBatis-Plus配置
     */
    private MyBatisPlusConfig mybatisPlus = new MyBatisPlusConfig();
    
    /**
     * 分页配置
     */
    private PaginationConfig pagination = new PaginationConfig();
    
    @Data
    public static class MyBatisPlusConfig {
        /**
         * 是否启用逻辑删除
         */
        private boolean logicDelete = true;
        
        /**
         * 是否启用乐观锁
         */
        private boolean optimisticLock = true;
    }
    
    @Data
    public static class PaginationConfig {
        /**
         * 默认每页大小
         */
        private int defaultPageSize = 10;
        
        /**
         * 最大每页大小
         */
        private int maxPageSize = 100;
    }
}
```

---

## 条件配置

### 1. 基于配置属性的条件

```java
/**
 * 只有当配置启用时才加载
 */
@Configuration
@ConditionalOnProperty(
        name = "nebula.security.enabled", 
        havingValue = "true", 
        matchIfMissing = false
)
public class SecurityAutoConfiguration {
    // 配置内容
}
```

### 2. 基于类存在的条件

```java
/**
 * 只有当Redis存在时才加载缓存配置
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(name = "nebula.cache.enabled", havingValue = "true")
public class CacheAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheManager(redisTemplate);
    }
}
```

### 3. 基于Bean存在的条件

```java
/**
 * 只有当DataSource存在时才加载
 */
@Configuration
@ConditionalOnBean(DataSource.class)
public class TransactionAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

---

## 配置顺序

### 1. 使用@AutoConfigureAfter

```java
/**
 * 缓存配置必须在数据源配置之后
 */
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class CacheAutoConfiguration {
    // 可以使用DataSource相关Bean
}
```

### 2. 使用@AutoConfigureBefore

```java
/**
 * 安全配置必须在Web配置之前
 */
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
public class SecurityAutoConfiguration {
    // 优先配置安全相关内容
}
```

### 3. 配置顺序示例

```java
/**
 * 完整的配置顺序示例
 */
@Configuration
@ConditionalOnProperty(name = "nebula.rpc.http.enabled", havingValue = "true")
@AutoConfigureAfter({
        DiscoveryAutoConfiguration.class,  // 服务发现
        LoadBalancerAutoConfiguration.class  // 负载均衡
})
@AutoConfigureBefore({
        WebMvcAutoConfiguration.class  // Web MVC
})
public class RpcHttpAutoConfiguration {
    // RPC HTTP配置
}
```

---

## 自定义配置

### 1. 覆盖自动配置的Bean

```java
/**
 * 自定义配置类
 */
@Configuration
public class CustomConfiguration {
    
    /**
     * 覆盖默认的CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        // 自定义CacheManager实现
        return new CustomCacheManager();
    }
}
```

### 2. 排除自动配置

```java
/**
 * 排除特定的自动配置
 */
@SpringBootApplication(exclude = {
        CacheAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

或在配置文件中排除：

```yaml
spring:
  autoconfigure:
    exclude:
      - io.nebula.autoconfigure.cache.CacheAutoConfiguration
      - io.nebula.autoconfigure.security.SecurityAutoConfiguration
```

### 3. 自定义自动配置类

```java
/**
 * 自定义自动配置类
 */
@Configuration
@ConditionalOnProperty(name = "myapp.custom.enabled", havingValue = "true")
@AutoConfigureAfter(NebulaAutoConfiguration.class)
public class CustomAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public CustomService customService() {
        return new CustomServiceImpl();
    }
}
```

注册自动配置类（`META-INF/spring.factories`）：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.config.CustomAutoConfiguration
```

---

## 故障排查

### 1. 查看自动配置报告

启动时添加 `--debug` 参数：

```bash
java -jar application.jar --debug
```

或在配置文件中开启：

```yaml
debug: true
```

输出示例：

```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------

   DataPersistenceAutoConfiguration matched:
      - @ConditionalOnProperty (nebula.data.persistence.enabled=true) matched (OnPropertyCondition)
      - @ConditionalOnClass found required class 'org.apache.ibatis.session.SqlSessionFactory' (OnClassCondition)

   CacheAutoConfiguration matched:
      - @ConditionalOnProperty (nebula.cache.enabled=true) matched (OnPropertyCondition)
      - @ConditionalOnClass found required class 'org.springframework.data.redis.core.RedisTemplate' (OnClassCondition)

Negative matches:
-----------------

   SecurityAutoConfiguration:
      Did not match:
         - @ConditionalOnProperty (nebula.security.enabled=false) did not match (OnPropertyCondition)

   MessagingAutoConfiguration:
      Did not match:
         - @ConditionalOnClass did not find required class 'org.springframework.amqp.rabbit.core.RabbitTemplate' (OnClassCondition)
```

### 2. 常见问题排查

#### Bean未注入

**问题**：自动配置的Bean无法注入

**排查步骤**：

1. 检查依赖是否正确引入：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-cache</artifactId>
</dependency>
```

2. 检查配置是否启用：

```yaml
nebula:
  cache:
    enabled: true  # 确保启用
```

3. 查看自动配置报告（使用 `--debug`）

#### 配置不生效

**问题**：配置了属性但不生效

**排查步骤**：

1. 检查配置前缀是否正确：

```yaml
# 正确
nebula:
  data:
    persistence:
      enabled: true

# 错误（前缀不对）
nebula-data-persistence:
  enabled: true
```

2. 检查配置属性绑定：

```java
@ConfigurationProperties(prefix = "nebula.data.persistence")
public class DataPersistenceProperties {
    // ...
}
```

#### Bean循环依赖

**问题**：启动时报循环依赖错误

**解决方案**：

1. 使用 `@Lazy` 注解：

```java
@Bean
public ServiceA serviceA(@Lazy ServiceB serviceB) {
    return new ServiceA(serviceB);
}
```

2. 调整配置顺序（使用 `@AutoConfigureAfter` / `@AutoConfigureBefore`）

---

## 配置优先级

### 配置加载顺序

1. **命令行参数**
2. **Java系统属性**
3. **操作系统环境变量**
4. **application-{profile}.yml**
5. **application.yml**
6. **@PropertySource**
7. **默认配置**

### 示例

```yaml
# application.yml（默认配置）
nebula:
  cache:
    ttl: 3600

# application-dev.yml（开发环境）
nebula:
  cache:
    ttl: 600  # 覆盖默认值

# application-prod.yml（生产环境）
nebula:
  cache:
    ttl: 7200  # 覆盖默认值
```

命令行参数：

```bash
# 覆盖配置文件中的值
java -jar application.jar --nebula.cache.ttl=1800
```

---

## 最佳实践

### 1. 合理使用条件注解

```java
@Configuration
@ConditionalOnClass(RabbitTemplate.class)  // 类存在
@ConditionalOnProperty(  // 配置启用
        name = "nebula.messaging.enabled", 
        havingValue = "true",
        matchIfMissing = false  // 默认不启用
)
public class MessagingAutoConfiguration {
    // ...
}
```

### 2. 提供默认配置

```java
@Data
@ConfigurationProperties(prefix = "nebula.cache")
public class CacheProperties {
    
    /**
     * 是否启用缓存（默认启用）
     */
    private boolean enabled = true;
    
    /**
     * 默认TTL（默认1小时）
     */
    private Duration ttl = Duration.ofHours(1);
    
    /**
     * 最大缓存大小（默认1000）
     */
    private int maxSize = 1000;
}
```

### 3. 支持自定义扩展

```java
@Configuration
public class CacheAutoConfiguration {
    
    /**
     * 提供默认实现
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        return new DefaultCacheManager();
    }
    
    /**
     * 允许用户自定义策略
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheEvictionStrategy evictionStrategy() {
        return new LRUEvictionStrategy();
    }
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

