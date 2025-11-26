# [模块名称] - 使用示例

> 本文档提供 [模块名称] 的完整使用示例，所有代码均可运行。

## 示例概览

本文档包含以下示例：

- [示例1：基础用法](#示例1基础用法) - 最简单的使用方式
- [示例2：常见场景](#示例2常见场景) - 常见业务场景应用
- [示例3：高级用法](#示例3高级用法) - 高级特性和技巧
- [示例4：集成示例](#示例4集成示例) - 与其他模块集成
- [示例5：实战案例](#示例5实战案例) - 完整的实战案例
- [最佳实践](#最佳实践) - 推荐的使用方式
- [常见错误](#常见错误) - 常见错误和解决方案

## 前提条件

### 环境要求

- **Java**：21+
- **Spring Boot**：3.2+
- **Maven**：3.8+
- **[其他中间件]**：版本要求

### 依赖配置

在 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- Nebula Starter（推荐）-->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-[type]</artifactId>
        <version>2.0.0-SNAPSHOT</version>
    </dependency>
    
    <!-- 或者单独引入此模块 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>[artifact-id]</artifactId>
        <version>2.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 基础配置

在 `application.yml` 中添加：

```yaml
nebula:
  [module]:
    enabled: true
    # 基础配置项
```

---

## 示例1：基础用法

### 场景说明

描述这个示例解决什么问题，适用于什么场景。

### 实现步骤

#### 步骤1：添加依赖

参考[依赖配置](#依赖配置)。

#### 步骤2：配置

在 `application.yml` 中配置：

```yaml
nebula:
  [module]:
    enabled: true
    property1: value1
    property2: value2
```

#### 步骤3：创建实体类

```java
package com.example.demo.entity;

import lombok.Data;

/**
 * 示例实体类
 */
@Data
public class ExampleEntity {
    private Long id;
    private String name;
    private String description;
}
```

#### 步骤4：创建服务类

```java
package com.example.demo.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 示例服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExampleService {
    
    // 注入模块组件
    private final ModuleComponent component;
    
    /**
     * 示例方法
     */
    public void doSomething() {
        log.info("开始执行示例操作");
        
        // 使用模块功能
        component.execute();
        
        log.info("示例操作执行完成");
    }
}
```

#### 步骤5：创建控制器类

```java
package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

/**
 * 示例控制器
 */
@RestController
@RequestMapping("/api/example")
@RequiredArgsConstructor
public class ExampleController {
    
    private final ExampleService exampleService;
    
    @GetMapping("/test")
    public String test() {
        exampleService.doSomething();
        return "Success";
    }
}
```

#### 步骤6：运行和测试

启动应用：

```bash
mvn spring-boot:run
```

访问接口：

```bash
curl http://localhost:8080/api/example/test
```

### 运行结果

**请求**：

```http
GET http://localhost:8080/api/example/test
```

**响应**：

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

**日志输出**：

```
2025-11-20 10:00:00.000  INFO [main] c.e.d.service.ExampleService: 开始执行示例操作
2025-11-20 10:00:00.001  INFO [main] c.e.d.service.ExampleService: 示例操作执行完成
```

### 关键点说明

1. **配置项说明**：解释重要配置项的作用
2. **依赖注入**：说明如何正确注入组件
3. **生命周期**：说明组件的生命周期管理

---

## 示例2：常见场景

### 场景2.1：[具体场景名称]

#### 业务背景

描述业务场景和需求。

#### 解决方案

```java
/**
 * 场景2.1解决方案
 */
@Service
@Slf4j
public class Scenario1Service {
    
    @Autowired
    private ModuleComponent component;
    
    public void handleScenario1() {
        // 场景处理逻辑
        try {
            // 步骤1
            component.step1();
            
            // 步骤2
            component.step2();
            
            // 步骤3
            component.step3();
            
            log.info("场景处理成功");
        } catch (Exception e) {
            log.error("场景处理失败", e);
            throw new BusinessException("处理失败: " + e.getMessage());
        }
    }
}
```

#### 测试验证

```java
@SpringBootTest
class Scenario1ServiceTest {
    
    @Autowired
    private Scenario1Service service;
    
    @Test
    void testHandleScenario1() {
        service.handleScenario1();
        // 验证结果
    }
}
```

### 场景2.2：[具体场景名称]

#### 业务背景

描述业务场景和需求。

#### 解决方案

```java
/**
 * 场景2.2解决方案
 */
@Service
@Slf4j
public class Scenario2Service {
    
    // 实现代码
}
```

### 场景2.3：[具体场景名称]

#### 业务背景

描述业务场景和需求。

#### 解决方案

```java
/**
 * 场景2.3解决方案
 */
@Service
@Slf4j
public class Scenario3Service {
    
    // 实现代码
}
```

---

## 示例3：高级用法

### 示例3.1：自定义配置

#### 场景说明

需要自定义模块行为以满足特殊需求。

#### 实现步骤

**1. 创建自定义配置类**

```java
package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * 自定义配置
 */
@Configuration
public class CustomModuleConfig {
    
    @Bean
    public CustomComponent customComponent() {
        return new CustomComponent();
    }
}
```

**2. 在配置文件中启用**

```yaml
nebula:
  [module]:
    enabled: true
    custom:
      enabled: true
      property: custom-value
```

**3. 使用自定义配置**

```java
@Service
@RequiredArgsConstructor
public class CustomService {
    
    private final CustomComponent customComponent;
    
    public void useCustomFeature() {
        customComponent.customMethod();
    }
}
```

### 示例3.2：扩展功能

#### 场景说明

通过扩展点实现自定义功能。

#### 实现步骤

**1. 实现扩展接口**

```java
package com.example.demo.extension;

import io.nebula.[module].extension.Extension;

/**
 * 自定义扩展实现
 */
@Component
public class CustomExtension implements Extension {
    
    @Override
    public void execute() {
        // 自定义逻辑
    }
}
```

**2. 注册扩展**

```java
@Configuration
public class ExtensionConfig {
    
    @Bean
    public ExtensionRegistry extensionRegistry(List<Extension> extensions) {
        ExtensionRegistry registry = new ExtensionRegistry();
        extensions.forEach(registry::register);
        return registry;
    }
}
```

### 示例3.3：性能优化

#### 场景说明

针对高并发场景的性能优化。

#### 优化方案

```java
/**
 * 性能优化示例
 */
@Service
@Slf4j
public class OptimizedService {
    
    // 使用缓存
    @Cacheable(value = "example-cache", key = "#id")
    public ExampleEntity getById(Long id) {
        // 数据库查询
        return repository.findById(id).orElse(null);
    }
    
    // 批量处理
    public void batchProcess(List<ExampleEntity> entities) {
        // 批量处理逻辑
        entities.parallelStream()
               .forEach(this::process);
    }
    
    // 异步处理
    @Async
    public CompletableFuture<Void> asyncProcess(ExampleEntity entity) {
        // 异步处理逻辑
        return CompletableFuture.completedFuture(null);
    }
}
```

---

## 示例4：集成示例

### 示例4.1：与数据库集成

#### 配置

```yaml
nebula:
  [module]:
    enabled: true
  data:
    persistence:
      enabled: true

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo
    username: root
    password: password
```

#### 代码实现

```java
/**
 * 数据库集成示例
 */
@Service
@RequiredArgsConstructor
public class DatabaseIntegrationService {
    
    private final ModuleComponent moduleComponent;
    private final ExampleMapper exampleMapper;
    
    @Transactional
    public void processWithDatabase() {
        // 使用模块功能
        moduleComponent.prepare();
        
        // 数据库操作
        ExampleEntity entity = new ExampleEntity();
        exampleMapper.insert(entity);
        
        // 后续处理
        moduleComponent.afterSave(entity);
    }
}
```

### 示例4.2：与缓存集成

#### 配置

```yaml
nebula:
  [module]:
    enabled: true
  data:
    cache:
      enabled: true

spring:
  redis:
    host: localhost
    port: 6379
```

#### 代码实现

```java
/**
 * 缓存集成示例
 */
@Service
@RequiredArgsConstructor
public class CacheIntegrationService {
    
    private final ModuleComponent moduleComponent;
    private final CacheManager cacheManager;
    
    public ExampleEntity getWithCache(Long id) {
        // 先从缓存获取
        Cache cache = cacheManager.getCache("example");
        ExampleEntity cached = cache.get(id, ExampleEntity.class);
        
        if (cached != null) {
            return cached;
        }
        
        // 缓存未命中，使用模块功能获取
        ExampleEntity entity = moduleComponent.fetchById(id);
        
        // 存入缓存
        cache.put(id, entity);
        
        return entity;
    }
}
```

### 示例4.3：与消息队列集成

#### 配置

```yaml
nebula:
  [module]:
    enabled: true
  messaging:
    rabbitmq:
      enabled: true
```

#### 代码实现

```java
/**
 * 消息队列集成示例
 */
@Service
@RequiredArgsConstructor
public class MessageIntegrationService {
    
    private final ModuleComponent moduleComponent;
    private final MessageProducer messageProducer;
    
    public void processAndSendMessage(ExampleEntity entity) {
        // 使用模块功能处理
        moduleComponent.process(entity);
        
        // 发送消息
        messageProducer.send("example.topic", entity);
    }
}

/**
 * 消息消费者
 */
@Component
@Slf4j
public class ExampleMessageConsumer {
    
    @Autowired
    private ModuleComponent moduleComponent;
    
    @RabbitListener(queues = "example.queue")
    public void handleMessage(ExampleEntity entity) {
        log.info("收到消息: {}", entity);
        
        // 使用模块功能处理消息
        moduleComponent.handleMessage(entity);
    }
}
```

---

## 示例5：实战案例

### 案例：票务系统中的应用

#### 业务场景

在票务系统中使用本模块实现[具体功能]。

#### 系统架构

```
用户请求 -> Controller -> Service -> [模块组件] -> 数据库/缓存/消息队列
```

#### 完整实现

**1. 实体类**

```java
/**
 * 票务实体
 */
@Data
@TableName("t_ticket")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ticketNo;
    private String eventName;
    private BigDecimal price;
    private Integer status;
    private LocalDateTime createTime;
}
```

**2. Mapper接口**

```java
/**
 * 票务Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
    
    List<Ticket> selectAvailableTickets(@Param("eventId") Long eventId);
}
```

**3. Service实现**

```java
/**
 * 票务服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    
    private final TicketMapper ticketMapper;
    private final ModuleComponent moduleComponent;
    private final CacheManager cacheManager;
    private final LockManager lockManager;
    
    /**
     * 购票
     */
    @Transactional(rollbackFor = Exception.class)
    public Ticket purchaseTicket(Long ticketId, Long userId) {
        // 使用分布式锁防止超卖
        return lockManager.execute("ticket:" + ticketId, () -> {
            // 查询票务
            Ticket ticket = ticketMapper.selectById(ticketId);
            if (ticket == null || ticket.getStatus() != 0) {
                throw new BusinessException("票已售出或不存在");
            }
            
            // 使用模块功能处理业务逻辑
            moduleComponent.beforePurchase(ticket, userId);
            
            // 更新票务状态
            ticket.setStatus(1);
            ticketMapper.updateById(ticket);
            
            // 清除缓存
            cacheManager.getCache("tickets").evict(ticketId);
            
            // 后续处理
            moduleComponent.afterPurchase(ticket, userId);
            
            log.info("用户 {} 成功购买票务 {}", userId, ticketId);
            return ticket;
        });
    }
    
    /**
     * 查询可用票务（带缓存）
     */
    @Cacheable(value = "tickets", key = "'available:' + #eventId")
    public List<Ticket> getAvailableTickets(Long eventId) {
        // 使用模块功能获取数据
        return moduleComponent.fetchAvailableTickets(eventId);
    }
}
```

**4. Controller实现**

```java
/**
 * 票务控制器
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    
    private final TicketService ticketService;
    private final CurrentUser currentUser;
    
    /**
     * 购买票务
     */
    @PostMapping("/{id}/purchase")
    public Result<Ticket> purchase(@PathVariable Long id) {
        Long userId = currentUser.getUserId();
        Ticket ticket = ticketService.purchaseTicket(id, userId);
        return Result.success(ticket);
    }
    
    /**
     * 查询可用票务
     */
    @GetMapping("/available")
    public Result<List<Ticket>> available(@RequestParam Long eventId) {
        List<Ticket> tickets = ticketService.getAvailableTickets(eventId);
        return Result.success(tickets);
    }
}
```

**5. 配置文件**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_system
    username: root
    password: password
  redis:
    host: localhost
    port: 6379

nebula:
  [module]:
    enabled: true
    # 模块特定配置
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
  lock:
    redis:
      enabled: true
```

#### 测试验证

```java
@SpringBootTest
class TicketServiceTest {
    
    @Autowired
    private TicketService ticketService;
    
    @Test
    void testPurchaseTicket() {
        Long ticketId = 1L;
        Long userId = 100L;
        
        Ticket ticket = ticketService.purchaseTicket(ticketId, userId);
        
        assertNotNull(ticket);
        assertEquals(1, ticket.getStatus());
    }
    
    @Test
    void testGetAvailableTickets() {
        Long eventId = 1L;
        
        List<Ticket> tickets = ticketService.getAvailableTickets(eventId);
        
        assertNotNull(tickets);
        assertFalse(tickets.isEmpty());
    }
}
```

---

## 最佳实践

### 实践1：合理使用配置

**说明**：根据不同环境使用不同配置，避免硬编码。

**推荐做法**：

```yaml
# application.yml - 默认配置
nebula:
  [module]:
    enabled: true
    property: default-value

# application-dev.yml - 开发环境
nebula:
  [module]:
    property: dev-value
    debug: true

# application-prod.yml - 生产环境
nebula:
  [module]:
    property: ${ENV_PROPERTY}
    debug: false
```

**不推荐做法**：

```java
// 硬编码配置
public class BadExample {
    private static final String PROPERTY = "hard-coded-value";
}
```

### 实践2：正确处理异常

**说明**：使用模块提供的异常类型，提供有意义的错误信息。

**推荐做法**：

```java
@Service
public class GoodExample {
    
    public void process() {
        try {
            component.execute();
        } catch (ModuleException e) {
            log.error("模块处理失败: {}", e.getMessage(), e);
            throw new BusinessException("业务处理失败", e);
        }
    }
}
```

**不推荐做法**：

```java
@Service
public class BadExample {
    
    public void process() {
        try {
            component.execute();
        } catch (Exception e) {
            // 吞掉异常
            e.printStackTrace();
        }
    }
}
```

### 实践3：使用依赖注入

**说明**：使用Spring的依赖注入，避免手动创建对象。

**推荐做法**：

```java
@Service
@RequiredArgsConstructor  // Lombok生成构造函数
public class GoodExample {
    
    private final ModuleComponent component;  // 构造函数注入
    
    public void process() {
        component.execute();
    }
}
```

**不推荐做法**：

```java
@Service
public class BadExample {
    
    @Autowired  // 字段注入，不推荐
    private ModuleComponent component;
    
    public void process() {
        ModuleComponent newComponent = new ModuleComponent();  // 手动创建，错误
        newComponent.execute();
    }
}
```

### 实践4：资源管理

**说明**：正确管理资源生命周期，避免资源泄漏。

**推荐做法**：

```java
@Service
public class GoodExample {
    
    public void processWithResource() {
        try (Resource resource = component.createResource()) {
            resource.use();
        }  // 自动关闭资源
    }
}
```

### 实践5：性能优化

**说明**：合理使用缓存、批量操作等提升性能。

**推荐做法**：

```java
@Service
public class GoodExample {
    
    // 使用缓存
    @Cacheable("example")
    public Data getData(Long id) {
        return component.query(id);
    }
    
    // 批量处理
    public void batchProcess(List<Data> dataList) {
        component.batchExecute(dataList);  // 批量操作
    }
}
```

---

## 常见错误

### 错误1：未启用模块

**错误现象**：

```
NoSuchBeanDefinitionException: No qualifying bean of type 'ModuleComponent'
```

**原因分析**：

模块未启用或自动配置未生效。

**解决方案**：

```yaml
# 在application.yml中启用模块
nebula:
  [module]:
    enabled: true  # 确保设置为true
```

### 错误2：配置项缺失

**错误现象**：

```
IllegalArgumentException: Required property 'xxx' is not set
```

**原因分析**：

必填配置项未配置。

**解决方案**：

```yaml
nebula:
  [module]:
    enabled: true
    required-property: value  # 添加必填配置
```

### 错误3：依赖冲突

**错误现象**：

```
NoSuchMethodError: xxx.method()
```

**原因分析**：

依赖版本冲突。

**解决方案**：

```xml
<!-- 排除冲突的依赖 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>[artifact-id]</artifactId>
    <exclusions>
        <exclusion>
            <groupId>conflict-group</groupId>
            <artifactId>conflict-artifact</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### 错误4：线程安全问题

**错误现象**：

数据不一致或并发异常。

**原因分析**：

组件使用不当导致线程安全问题。

**解决方案**：

```java
@Service
public class ThreadSafeExample {
    
    // 使用ThreadLocal
    private ThreadLocal<Context> contextHolder = new ThreadLocal<>();
    
    // 或使用分布式锁
    @Autowired
    private LockManager lockManager;
    
    public void safeProcess(Long id) {
        lockManager.execute("key:" + id, () -> {
            // 临界区代码
        });
    }
}
```

### 错误5：资源泄漏

**错误现象**：

内存占用持续增长，最终OOM。

**原因分析**：

资源未正确释放。

**解决方案**：

```java
@Service
public class ResourceManagementExample {
    
    public void correctUsage() {
        // 使用try-with-resources
        try (AutoCloseable resource = component.createResource()) {
            // 使用资源
        } catch (Exception e) {
            log.error("资源使用失败", e);
        }
        // 资源自动关闭
    }
}
```

---

## 完整示例项目

### 项目结构

```
example-project/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       ├── DemoApplication.java
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── entity/
│   │   │       └── mapper/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── mapper/
│   └── test/
│       └── java/
│           └── com/example/demo/
├── pom.xml
└── README.md
```

### 运行示例

```bash
# 克隆示例项目
git clone https://github.com/nebula-framework/examples.git
cd examples/[module-example]

# 启动依赖服务
docker-compose up -d

# 运行示例
mvn spring-boot:run

# 测试接口
curl http://localhost:8080/api/example/test
```

### 示例源码

完整示例源码请参考：`examples/[module-example]`

---

## 参考资源

### 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

### 示例项目

- `examples/[example-basic]` - 基础示例
- `examples/[example-advanced]` - 高级示例
- `examples/[example-integration]` - 集成示例

### 外部资源

- [Spring Boot文档](https://spring.io/projects/spring-boot)
- [相关技术文档]

---

> 如有问题或建议，欢迎提Issue或PR。

