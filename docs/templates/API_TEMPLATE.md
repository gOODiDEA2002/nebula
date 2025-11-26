# [模块名称] - API参考

> 本文档详细说明 [模块名称] 提供的公共 API 接口。

## API 概览

### 设计原则

- **简单易用**：API 设计简洁直观
- **一致性**：命名和使用方式保持一致
- **可扩展**：易于扩展和定制
- **向后兼容**：保持 API 稳定性

### 版本信息

- **当前版本**：v1.0.0
- **最低兼容版本**：v1.0.0
- **API 稳定性**：Stable

---

## 核心接口

### 1. ModuleService

主要服务接口，提供核心功能。

#### 1.1 接口定义

```java
package io.nebula.[module];

import java.util.List;
import java.util.Optional;

/**
 * [模块名称] 核心服务接口
 * 
 * @author Nebula Team
 * @since 1.0.0
 */
public interface ModuleService {
    
    /**
     * 处理数据
     * 
     * @param input 输入数据
     * @return 处理结果
     * @throws IllegalArgumentException 如果输入为空
     */
    String process(String input);
    
    /**
     * 保存实体
     * 
     * @param entity 实体对象
     * @param <T> 实体类型
     * @return 保存后的实体
     * @throws IllegalArgumentException 如果实体为空
     */
    <T> T save(T entity);
    
    /**
     * 根据ID查找实体
     * 
     * @param id 实体ID
     * @param <T> 实体类型
     * @return 实体对象（可能为空）
     */
    <T> Optional<T> findById(Long id);
    
    /**
     * 查询所有实体
     * 
     * @param <T> 实体类型
     * @return 实体列表
     */
    <T> List<T> listAll();
    
    /**
     * 更新实体
     * 
     * @param entity 实体对象
     * @param <T> 实体类型
     * @return 更新后的实体
     * @throws IllegalArgumentException 如果实体为空或ID不存在
     */
    <T> T update(T entity);
    
    /**
     * 删除实体
     * 
     * @param id 实体ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
}
```

#### 1.2 使用示例

```java
// 注入服务
@Autowired
private ModuleService moduleService;

// 保存实体
Order order = new Order();
order.setUserId("user123");
Order saved = moduleService.save(order);

// 查询实体
Optional<Order> found = moduleService.findById(1L);

// 更新实体
order.setOrderStatus("paid");
Order updated = moduleService.update(order);

// 删除实体
boolean deleted = moduleService.deleteById(1L);
```

### 2. ModuleConfig

配置类，用于配置模块。

#### 2.1 类定义

```java
package io.nebula.[module].config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [模块名称] 配置类
 * 
 * @author Nebula Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.[module]")
public class ModuleConfig {
    
    /**
     * 是否启用模块
     */
    private boolean enabled = true;
    
    /**
     * 配置属性1
     */
    private String property1;
    
    /**
     * 配置属性2
     */
    private Integer property2 = 100;
    
    /**
     * 高级配置
     */
    private AdvancedConfig advanced = new AdvancedConfig();
    
    @Data
    public static class AdvancedConfig {
        /**
         * 子配置1
         */
        private String subProperty1;
        
        /**
         * 子配置2
         */
        private Integer subProperty2 = 50;
    }
}
```

#### 2.2 使用示例

**配置文件**：

```yaml
nebula:
  [module]:
    enabled: true
    property1: value1
    property2: 200
    advanced:
      sub-property1: value1
      sub-property2: 100
```

**代码使用**：

```java
@Autowired
private ModuleConfig config;

public void doSomething() {
    if (config.isEnabled()) {
        String value = config.getProperty1();
        // ...
    }
}
```

---

## 注解

### 1. @EnableModule

启用模块的注解。

#### 1.1 注解定义

```java
package io.nebula.[module].annotation;

import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

/**
 * 启用 [模块名称] 模块
 * 
 * @author Nebula Team
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ModuleConfiguration.class)
public @interface EnableModule {
    
    /**
     * 是否启用自动配置
     */
    boolean autoConfig() default true;
}
```

#### 1.2 使用示例

```java
@SpringBootApplication
@EnableModule
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. @ModuleOperation

标记模块操作的注解。

#### 2.1 注解定义

```java
package io.nebula.[module].annotation;

import java.lang.annotation.*;

/**
 * 标记模块操作方法
 * 
 * @author Nebula Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModuleOperation {
    
    /**
     * 操作名称
     */
    String value() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
}
```

#### 2.2 使用示例

```java
@ModuleOperation(value = "createOrder", description = "创建订单")
public Order createOrder(Order order) {
    return moduleService.save(order);
}
```

---

## 异常

### 1. ModuleException

模块基础异常。

#### 1.1 异常定义

```java
package io.nebula.[module].exception;

/**
 * [模块名称] 基础异常
 * 
 * @author Nebula Team
 * @since 1.0.0
 */
public class ModuleException extends RuntimeException {
    
    /**
     * 错误码
     */
    private String errorCode;
    
    public ModuleException(String message) {
        super(message);
    }
    
    public ModuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
```

### 2. 具体异常

| 异常类 | 错误码 | 说明 |
|--------|--------|------|
| `ConfigurationException` | `CONFIG_ERROR` | 配置相关错误 |
| `ValidationException` | `VALIDATION_ERROR` | 参数验证错误 |
| `OperationException` | `OPERATION_ERROR` | 业务操作错误 |

#### 2.1 定义示例

```java
/**
 * 配置异常
 */
public class ConfigurationException extends ModuleException {
    public ConfigurationException(String message) {
        super("CONFIG_ERROR", message);
    }
}
```

#### 2.2 捕获示例

```java
try {
    moduleService.process(input);
} catch (ValidationException e) {
    // 处理验证异常
    logger.error("验证失败: {}", e.getMessage());
} catch (OperationException e) {
    // 处理操作异常
    logger.error("操作失败: {}", e.getMessage());
} catch (ModuleException e) {
    // 处理其他模块异常
    logger.error("模块异常: {}", e.getMessage());
}
```

---

## 事件

### 1. ModuleEvent

模块事件基类。

#### 1.1 事件定义

```java
package io.nebula.[module].event;

import org.springframework.context.ApplicationEvent;

/**
 * [模块名称] 事件基类
 * 
 * @author Nebula Team
 * @since 1.0.0
 */
public abstract class ModuleEvent extends ApplicationEvent {
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 事件时间戳
     */
    private long timestamp;
    
    public ModuleEvent(Object source, String eventType) {
        super(source);
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}
```

### 2. 具体事件

| 事件类 | 事件类型 | 说明 |
|--------|----------|------|
| `DataChangedEvent` | `DATA_CHANGED` | 数据发生变更时触发 |
| `OperationCompletedEvent` | `OPERATION_COMPLETED` | 操作完成时触发 |

#### 2.1 发布事件

```java
@Autowired
private ApplicationEventPublisher eventPublisher;

// 发布事件
DataChangedEvent event = new DataChangedEvent(this, order);
eventPublisher.publishEvent(event);
```

#### 2.2 监听事件

```java
@Component
public class ModuleEventListener {
    
    @EventListener
    public void handleDataChanged(DataChangedEvent event) {
        Object data = event.getData();
        // 处理数据变更
    }
    
    @EventListener
    public void handleOperationCompleted(OperationCompletedEvent event) {
        String operation = event.getOperationName();
        boolean success = event.isSuccess();
        // 处理操作完成
    }
}
```

---

## 扩展点

### 1. ModuleExtension

模块扩展接口。

#### 1.1 接口定义

```java
package io.nebula.[module].extension;

/**
 * [模块名称] 扩展接口
 * 
 * @author Nebula Team
 * @since 1.0.0
 */
public interface ModuleExtension {
    
    /**
     * 扩展点名称
     */
    String getName();
    
    /**
     * 执行扩展逻辑
     * 
     * @param context 扩展上下文
     * @return 执行结果
     */
    Object execute(ExtensionContext context);
}
```

#### 1.2 实现扩展

```java
@Component
public class CustomExtension implements ModuleExtension {
    
    @Override
    public String getName() {
        return "customExtension";
    }
    
    @Override
    public Object execute(ExtensionContext context) {
        // 自定义扩展逻辑
        return "result";
    }
}
```

---

## 工具类

### 1. ModuleUtils

模块工具类。

#### 1.1 工具类定义

```java
package io.nebula.[module].util;

/**
 * [模块名称] 工具类
 * 
 * @author Nebula Team
 * @since 1.0.0
 */
public final class ModuleUtils {
    
    private ModuleUtils() {
    }
    
    /**
     * 工具方法1
     * 
     * @param input 输入参数
     * @return 处理结果
     */
    public static String formatValue(String input) {
        // 实现逻辑
        return input;
    }
    
    /**
     * 工具方法2
     * 
     * @param value 值
     * @return 验证结果
     */
    public static boolean validateValue(String value) {
        // 实现逻辑
        return value != null && !value.isEmpty();
    }
}
```

#### 1.2 使用示例

```java
// 使用工具类方法
String formatted = ModuleUtils.formatValue(input);
boolean isValid = ModuleUtils.validateValue(value);
```

---

## API 变更记录

### v1.0.0

- 初始版本发布
- 提供核心 API

### v0.9.0

- Beta 版本
- API 基本稳定

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例

---

> 本文档由 Nebula 框架团队维护，最后更新：[日期]
