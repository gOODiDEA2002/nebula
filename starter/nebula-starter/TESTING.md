# Nebula Starter - 测试指南

> Nebula框架Starter模块的测试策略

## 目录

- [测试策略](#测试策略)
- [依赖测试](#依赖测试)
- [集成测试](#集成测试)
- [模块冲突测试](#模块冲突测试)

---

## 测试策略

### 测试重点

由于 `nebula-starter` 是依赖管理模块，测试重点在于：

1. **依赖完整性**：验证所有必需依赖都已包含
2. **依赖冲突**：验证没有依赖冲突
3. **版本一致性**：验证所有模块版本一致
4. **功能可用性**：验证各个Starter能正常启动和使用

---

## 依赖测试

### 1. 依赖完整性测试

```java
/**
 * Starter依赖完整性测试
 */
@SpringBootTest(classes = TestApplication.class)
class StarterDependencyTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void testMinimalStarterDependencies() {
        // nebula-starter-minimal应该包含的Bean
        assertThat(applicationContext.getBean(IdGenerator.class)).isNotNull();
        assertThat(applicationContext.getBean(JsonUtils.class)).isNotNull();
        assertThat(applicationContext.getBean(CryptoUtils.class)).isNotNull();
    }
    
    @Test
    void testWebStarterDependencies() {
        // nebula-starter-web应该包含的Bean
        assertThat(applicationContext.getBean(GlobalExceptionHandler.class)).isNotNull();
        assertThat(applicationContext.getBean(CacheManager.class)).isNotNull();
    }
    
    @Test
    void testServiceStarterDependencies() {
        // nebula-starter-service应该包含的Bean
        assertThat(applicationContext.getBean(RpcClient.class)).isNotNull();
        assertThat(applicationContext.getBean(MessageProducer.class)).isNotNull();
        assertThat(applicationContext.getBean(DistributedLock.class)).isNotNull();
    }
}
```

### 2. 版本一致性测试

```java
/**
 * 版本一致性测试
 */
class VersionConsistencyTest {
    
    @Test
    void testAllModulesUseSameVersion() throws Exception {
        // 读取pom.xml
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document doc = builder.parse(new File("pom.xml"));
        
        // 获取所有nebula依赖
        NodeList dependencies = doc.getElementsByTagName("dependency");
        
        String expectedVersion = null;
        
        for (int i = 0; i < dependencies.getLength(); i++) {
            Node dependency = dependencies.item(i);
            
            if (dependency.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) dependency;
                
                String groupId = element.getElementsByTagName("groupId")
                        .item(0).getTextContent();
                
                if ("io.nebula".equals(groupId)) {
                    String version = element.getElementsByTagName("version")
                            .item(0).getTextContent();
                    
                    if (expectedVersion == null) {
                        expectedVersion = version;
                    } else {
                        assertThat(version).isEqualTo(expectedVersion);
                    }
                }
            }
        }
    }
}
```

---

## 集成测试

### 1. Minimal Starter集成测试

```java
/**
 * Minimal Starter集成测试
 */
@SpringBootTest(classes = MinimalTestApplication.class)
@TestPropertySource(properties = {
        "spring.main.web-application-type=none"
})
class MinimalStarterIntegrationTest {
    
    @Autowired
    private IdGenerator idGenerator;
    
    @Autowired
    private JsonUtils jsonUtils;
    
    @Test
    void testIdGeneration() {
        String id = idGenerator.generateSnowflakeId();
        assertThat(id).isNotBlank();
        assertThat(id.length()).isGreaterThan(10);
    }
    
    @Test
    void testJsonUtils() {
        Map<String, Object> data = Map.of("key", "value");
        String json = jsonUtils.toJson(data);
        
        assertThat(json).contains("key");
        assertThat(json).contains("value");
    }
}

@SpringBootApplication
class MinimalTestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MinimalTestApplication.class, args);
    }
}
```

### 2. Web Starter集成测试

```java
/**
 * Web Starter集成测试
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = WebTestApplication.class
)
@TestPropertySource(properties = {
        "nebula.web.enabled=true",
        "nebula.security.enabled=true",
        "nebula.cache.enabled=true"
})
class WebStarterIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testWebEndpoint() {
        String url = "http://localhost:" + port + "/api/test";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    void testGlobalExceptionHandler() {
        String url = "http://localhost:" + port + "/api/error";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("error");
    }
}

@SpringBootApplication
class WebTestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WebTestApplication.class, args);
    }
}

@RestController
@RequestMapping("/api")
class TestController {
    
    @GetMapping("/test")
    public String test() {
        return "OK";
    }
    
    @GetMapping("/error")
    public String error() {
        throw new BusinessException("Test error");
    }
}
```

### 3. Service Starter集成测试

```java
/**
 * Service Starter集成测试
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ServiceTestApplication.class
)
@TestPropertySource(properties = {
        "nebula.rpc.http.enabled=true",
        "nebula.discovery.nacos.enabled=false",  // 测试环境禁用
        "nebula.messaging.rabbitmq.enabled=false",  // 测试环境禁用
        "nebula.lock.redis.enabled=true"
})
class ServiceStarterIntegrationTest {
    
    @Autowired(required = false)
    private RpcClient rpcClient;
    
    @Autowired(required = false)
    private DistributedLock distributedLock;
    
    @Test
    void testRpcClientAvailable() {
        assertThat(rpcClient).isNotNull();
    }
    
    @Test
    void testDistributedLockAvailable() {
        assertThat(distributedLock).isNotNull();
    }
    
    @Test
    void testDistributedLockOperation() {
        String lockKey = "test:lock";
        
        boolean locked = distributedLock.tryLock(lockKey, Duration.ofSeconds(10));
        assertThat(locked).isTrue();
        
        distributedLock.unlock(lockKey);
    }
}

@SpringBootApplication
class ServiceTestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ServiceTestApplication.class, args);
    }
}
```

---

## 模块冲突测试

### 1. 依赖冲突检测

```bash
# 使用Maven检查依赖冲突
mvn dependency:tree -Dverbose
mvn dependency:analyze
```

### 2. 类冲突测试

```java
/**
 * 类冲突测试
 */
class ClassConflictTest {
    
    @Test
    void testNoClassConflicts() {
        // 检查关键类是否唯一
        Set<String> classNames = new HashSet<>();
        
        // 获取所有Nebula类
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        // 验证没有重复的类
        assertNoDuplicateClasses(classNames, "io.nebula");
    }
    
    private void assertNoDuplicateClasses(Set<String> classNames, String packageName) {
        // 实现类重复检测逻辑
    }
}
```

---

## 启动测试

### 1. 快速启动测试

```java
/**
 * 启动性能测试
 */
class StartupPerformanceTest {
    
    @Test
    void testMinimalStarterStartupTime() {
        long startTime = System.currentTimeMillis();
        
        SpringApplication app = new SpringApplication(MinimalTestApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        
        ConfigurableApplicationContext context = app.run();
        
        long startupTime = System.currentTimeMillis() - startTime;
        
        assertThat(startupTime).isLessThan(5000);  // 应该在5秒内启动
        
        context.close();
    }
    
    @Test
    void testWebStarterStartupTime() {
        long startTime = System.currentTimeMillis();
        
        SpringApplication app = new SpringApplication(WebTestApplication.class);
        ConfigurableApplicationContext context = app.run("--server.port=0");
        
        long startupTime = System.currentTimeMillis() - startTime;
        
        assertThat(startupTime).isLessThan(10000);  // 应该在10秒内启动
        
        context.close();
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

