# Nebula AutoConfigure - 测试指南

> 统一自动配置模块的测试策略与实践

## 目录

- [测试策略](#测试策略)
- [自动配置测试](#自动配置测试)
- [条件测试](#条件测试)
- [配置属性测试](#配置属性测试)
- [集成测试](#集成测试)

---

## 测试策略

### 测试层次

1. **单元测试**：测试单个自动配置类
2. **条件测试**：测试条件注解逻辑
3. **集成测试**：测试完整的自动配置流程
4. **顺序测试**：测试配置加载顺序

### 测试覆盖目标

- 自动配置类：90%+
- 条件逻辑：100%
- 配置属性绑定：100%

---

## 自动配置测试

### 1. 基本自动配置测试

```java
/**
 * 缓存自动配置测试
 */
@SpringBootTest(classes = {
        CacheAutoConfiguration.class,
        RedisAutoConfiguration.class
})
@TestPropertySource(properties = {
        "nebula.cache.enabled=true",
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
class CacheAutoConfigurationTest {
    
    @Autowired(required = false)
    private CacheManager cacheManager;
    
    @Test
    void testCacheManagerAutoConfigured() {
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }
    
    @Test
    void testCacheOperations() {
        String key = "test:key";
        String value = "test value";
        
        cacheManager.set(key, value, Duration.ofMinutes(1));
        
        Optional<String> cached = cacheManager.get(key, String.class);
        assertThat(cached).isPresent();
        assertThat(cached.get()).isEqualTo(value);
    }
}
```

### 2. Bean存在性测试

```java
/**
 * 数据持久化自动配置测试
 */
@SpringBootTest
@TestPropertySource(properties = {
        "nebula.data.persistence.enabled=true"
})
class DataPersistenceAutoConfigurationTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void testRequiredBeansExist() {
        // 验证SqlSessionFactory存在
        assertThat(applicationContext.getBean(SqlSessionFactory.class)).isNotNull();
        
        // 验证TransactionManager存在
        assertThat(applicationContext.getBean(PlatformTransactionManager.class)).isNotNull();
        
        // 验证MybatisPlusInterceptor存在
        assertThat(applicationContext.getBean(MybatisPlusInterceptor.class)).isNotNull();
    }
}
```

---

## 条件测试

### 1. 条件属性测试

```java
/**
 * 条件属性测试
 */
class ConditionalPropertyTest {
    
    @Test
    void testConfigurationEnabledWhenPropertyTrue() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withPropertyValues("nebula.cache.enabled=true");
        
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
        });
    }
    
    @Test
    void testConfigurationDisabledWhenPropertyFalse() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withPropertyValues("nebula.cache.enabled=false");
        
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(CacheManager.class);
        });
    }
    
    @Test
    void testConfigurationDisabledWhenPropertyMissing() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class));
        
        // 没有配置时，默认不启用
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(CacheManager.class);
        });
    }
}
```

### 2. 条件类测试

```java
/**
 * 条件类测试
 */
class ConditionalClassTest {
    
    @Test
    void testConfigurationEnabledWhenClassPresent() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        CacheAutoConfiguration.class,
                        RedisAutoConfiguration.class
                ))
                .withPropertyValues("nebula.cache.enabled=true");
        
        // RedisTemplate类存在时，自动配置生效
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
        });
    }
}
```

### 3. 条件Bean测试

```java
/**
 * 条件Bean测试
 */
class ConditionalBeanTest {
    
    @Test
    void testDefaultBeanCreatedWhenMissing() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withPropertyValues("nebula.cache.enabled=true");
        
        // 没有自定义CacheManager时，使用默认实现
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class))
                    .isInstanceOf(DefaultCacheManager.class);
        });
    }
    
    @Test
    void testCustomBeanOverridesDefault() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withUserConfiguration(CustomCacheConfiguration.class)
                .withPropertyValues("nebula.cache.enabled=true");
        
        // 有自定义CacheManager时，使用自定义实现
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class))
                    .isInstanceOf(CustomCacheManager.class);
        });
    }
    
    @Configuration
    static class CustomCacheConfiguration {
        
        @Bean
        public CacheManager cacheManager() {
            return new CustomCacheManager();
        }
    }
}
```

---

## 配置属性测试

### 1. 属性绑定测试

```java
/**
 * 配置属性绑定测试
 */
@SpringBootTest
@TestPropertySource(properties = {
        "nebula.cache.ttl=3600",
        "nebula.cache.max-size=1000",
        "nebula.cache.enabled=true"
})
class CachePropertiesTest {
    
    @Autowired
    private CacheProperties properties;
    
    @Test
    void testPropertiesBinding() {
        assertThat(properties.getTtl()).isEqualTo(Duration.ofSeconds(3600));
        assertThat(properties.getMaxSize()).isEqualTo(1000);
        assertThat(properties.isEnabled()).isTrue();
    }
}
```

### 2. 默认值测试

```java
/**
 * 配置默认值测试
 */
@SpringBootTest
class DefaultPropertiesTest {
    
    @Autowired
    private CacheProperties properties;
    
    @Test
    void testDefaultValues() {
        // 验证默认值
        assertThat(properties.isEnabled()).isTrue();  // 默认启用
        assertThat(properties.getTtl()).isEqualTo(Duration.ofHours(1));  // 默认1小时
        assertThat(properties.getMaxSize()).isEqualTo(1000);  // 默认1000
    }
}
```

### 3. 属性验证测试

```java
/**
 * 配置属性验证测试
 */
@SpringBootTest
@TestPropertySource(properties = {
        "nebula.security.jwt.secret=",  // 空值，应该验证失败
        "nebula.security.jwt.expiration=100"  // 小于最小值，应该验证失败
})
class PropertyValidationTest {
    
    @Test
    void testInvalidPropertiesThrowException() {
        assertThatThrownBy(() -> {
            // 启动应用时应该抛出验证异常
            SpringApplication.run(Application.class);
        }).hasCauseInstanceOf(BindException.class);
    }
}
```

---

## 集成测试

### 1. 完整自动配置流程测试

```java
/**
 * 完整自动配置流程集成测试
 */
@SpringBootTest(classes = Application.class)
@TestPropertySource(locations = "classpath:test-application.yml")
class FullAutoConfigurationIntegrationTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void testAllRequiredBeansCreated() {
        // 验证核心Bean
        assertThat(applicationContext.getBean(IdGenerator.class)).isNotNull();
        assertThat(applicationContext.getBean(JwtUtils.class)).isNotNull();
        
        // 验证数据访问Bean
        assertThat(applicationContext.getBean(CacheManager.class)).isNotNull();
        assertThat(applicationContext.getBean(SqlSessionFactory.class)).isNotNull();
        
        // 验证RPC Bean
        assertThat(applicationContext.getBean(RpcClient.class)).isNotNull();
        
        // 验证Web Bean
        assertThat(applicationContext.getBean(GlobalExceptionHandler.class)).isNotNull();
    }
    
    @Test
    void testDependencyInjectionWorks() {
        // 测试依赖注入是否正常工作
        UserService userService = applicationContext.getBean(UserService.class);
        assertThat(userService).isNotNull();
        
        // 验证依赖已注入
        assertThat(userService.getCacheManager()).isNotNull();
        assertThat(userService.getUserMapper()).isNotNull();
    }
}
```

### 2. 配置顺序测试

```java
/**
 * 配置加载顺序测试
 */
@SpringBootTest
class ConfigurationOrderTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void testConfigurationLoadOrder() {
        // 获取所有自动配置类
        String[] beanNames = applicationContext.getBeanNamesForType(
                AnnotationAwareOrderComparator.class
        );
        
        List<String> configOrder = Arrays.asList(beanNames);
        
        // 验证配置顺序
        int dataSourceIndex = configOrder.indexOf("dataSourceAutoConfiguration");
        int cacheIndex = configOrder.indexOf("cacheAutoConfiguration");
        int securityIndex = configOrder.indexOf("securityAutoConfiguration");
        int webIndex = configOrder.indexOf("webAutoConfiguration");
        
        // 数据源应该在缓存之前
        assertThat(dataSourceIndex).isLessThan(cacheIndex);
        
        // 安全应该在Web之前
        assertThat(securityIndex).isLessThan(webIndex);
    }
}
```

### 3. 多模块集成测试

```java
/**
 * 多模块集成测试
 */
@SpringBootTest
@TestPropertySource(properties = {
        "nebula.cache.enabled=true",
        "nebula.data.persistence.enabled=true",
        "nebula.security.enabled=true",
        "nebula.web.enabled=true"
})
class MultiModuleIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Test
    void testMultiModuleInteraction() {
        // 创建用户
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        // 保存用户（使用持久化模块）
        userService.saveUser(user);
        
        // 查询用户（使用缓存模块）
        User retrieved = userService.getUserById(user.getId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getUsername()).isEqualTo("testuser");
        
        // 验证缓存生效
        Optional<User> cached = cacheManager.get("user:" + user.getId(), User.class);
        assertThat(cached).isPresent();
        assertThat(cached.get().getUsername()).isEqualTo("testuser");
    }
}
```

---

## 测试工具

### 1. ApplicationContextRunner

```java
/**
 * 使用ApplicationContextRunner进行测试
 */
class ApplicationContextRunnerTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CacheAutoConfiguration.class,
                    DataPersistenceAutoConfiguration.class
            ));
    
    @Test
    void testWithProperties() {
        this.contextRunner
                .withPropertyValues(
                        "nebula.cache.enabled=true",
                        "nebula.cache.ttl=7200"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheManager.class);
                    
                    CacheProperties properties = context.getBean(CacheProperties.class);
                    assertThat(properties.getTtl()).isEqualTo(Duration.ofSeconds(7200));
                });
    }
    
    @Test
    void testWithUserConfiguration() {
        this.contextRunner
                .withUserConfiguration(CustomConfiguration.class)
                .withPropertyValues("nebula.cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheManager.class);
                    assertThat(context.getBean(CacheManager.class))
                            .isInstanceOf(CustomCacheManager.class);
                });
    }
}
```

### 2. 测试配置类

```java
/**
 * 测试配置类
 */
@TestConfiguration
public class TestAutoConfiguration {
    
    @Bean
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
    
    @Bean
    public RedisConnectionFactory testRedisConnectionFactory() {
        // 使用嵌入式Redis进行测试
        return new LettuceConnectionFactory("localhost", 6379);
    }
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

