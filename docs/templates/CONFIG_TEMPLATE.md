# [模块名称] - 配置参考

> 本文档提供 [模块名称] 的完整配置说明和示例。

## 配置概览

本模块支持通过 YAML 配置文件进行配置，所有配置项都在 `nebula.[module]` 命名空间下。

**配置优先级**（从高到低）：
1. 命令行参数（`--nebula.[module].xxx=value`）
2. 环境变量（`NEBULA_[MODULE]_XXX=value`）
3. 应用配置文件（`application.yml`）
4. Spring Profile配置（`application-{profile}.yml`）
5. 框架默认配置

## 快速配置

### 最小配置

最简单的配置，使用默认值：

```yaml
nebula:
  [module]:
    enabled: true
```

### 推荐配置

适合大多数场景的配置：

```yaml
nebula:
  [module]:
    enabled: true
    property1: recommended-value
    property2: recommended-value
    advanced:
      feature1: true
```

### 完整配置

包含所有配置项的完整示例：

```yaml
nebula:
  [module]:
    # 基础配置
    enabled: true
    property1: value1
    property2: value2
    
    # 高级配置
    advanced:
      feature1: true
      feature2: false
      option1: value
    
    # 性能配置
    performance:
      pool-size: 10
      timeout: 30s
    
    # 监控配置
    monitoring:
      enabled: true
      interval: 60s
```

---

## 配置项详解

### 1. 基础配置

#### 1.1 启用配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].enabled` | `Boolean` | `true` | 否 | 是否启用模块 |

**说明**：

控制模块是否启用。设置为 `false` 时，模块的所有自动配置将不生效。

**示例**：

```yaml
nebula:
  [module]:
    enabled: true
```

**环境变量**：

```bash
export NEBULA_[MODULE]_ENABLED=true
```

#### 1.2 核心配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].property1` | `String` | `default` | 是 | 配置项1说明 |
| `nebula.[module].property2` | `Integer` | `100` | 否 | 配置项2说明 |
| `nebula.[module].property3` | `Duration` | `30s` | 否 | 配置项3说明 |
| `nebula.[module].property4` | `List<String>` | `[]` | 否 | 配置项4说明 |

**property1 详解**：

- **作用**：详细说明配置项的作用
- **取值范围**：说明合法的取值范围
- **注意事项**：使用此配置需要注意的事项
- **示例**：
  ```yaml
  nebula:
    [module]:
      property1: example-value
  ```

**property2 详解**：

- **作用**：详细说明配置项的作用
- **取值范围**：1-1000
- **推荐值**：100（轻量级）、500（中等）、1000（重量级）
- **注意事项**：值过大可能导致资源消耗过多
- **示例**：
  ```yaml
  nebula:
    [module]:
      property2: 100
  ```

**property3 详解**：

- **作用**：超时时间配置
- **格式**：支持 `30s`、`5m`、`1h` 等格式
- **推荐值**：30s
- **示例**：
  ```yaml
  nebula:
    [module]:
      property3: 30s
  ```

**property4 详解**：

- **作用**：列表型配置说明
- **格式**：YAML数组
- **示例**：
  ```yaml
  nebula:
    [module]:
      property4:
        - item1
        - item2
        - item3
  ```

---

### 2. 高级配置

#### 2.1 高级特性

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].advanced.feature1` | `Boolean` | `false` | 否 | 高级特性1 |
| `nebula.[module].advanced.feature2` | `Boolean` | `false` | 否 | 高级特性2 |
| `nebula.[module].advanced.option1` | `String` | `auto` | 否 | 高级选项1 |

**feature1 详解**：

- **作用**：启用高级特性1
- **适用场景**：什么情况下需要启用
- **性能影响**：启用后的性能影响
- **示例**：
  ```yaml
  nebula:
    [module]:
      advanced:
        feature1: true
  ```

#### 2.2 自定义配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].custom.xxx` | `Any` | - | 否 | 自定义配置 |

**示例**：

```yaml
nebula:
  [module]:
    custom:
      key1: value1
      key2: value2
```

---

### 3. 性能配置

#### 3.1 线程池配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].performance.pool-size` | `Integer` | `10` | 否 | 线程池大小 |
| `nebula.[module].performance.queue-capacity` | `Integer` | `100` | 否 | 队列容量 |
| `nebula.[module].performance.keep-alive` | `Duration` | `60s` | 否 | 线程保活时间 |

**pool-size 选择建议**：

- **计算密集型**：`CPU核心数 + 1`
- **IO密集型**：`2 * CPU核心数`
- **混合型**：根据实际测试调整

**示例**：

```yaml
nebula:
  [module]:
    performance:
      pool-size: 20
      queue-capacity: 200
      keep-alive: 120s
```

#### 3.2 超时配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].performance.connect-timeout` | `Duration` | `10s` | 否 | 连接超时 |
| `nebula.[module].performance.read-timeout` | `Duration` | `30s` | 否 | 读取超时 |
| `nebula.[module].performance.write-timeout` | `Duration` | `30s` | 否 | 写入超时 |

**示例**：

```yaml
nebula:
  [module]:
    performance:
      connect-timeout: 10s
      read-timeout: 30s
      write-timeout: 30s
```

#### 3.3 缓存配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].performance.cache.enabled` | `Boolean` | `true` | 否 | 是否启用缓存 |
| `nebula.[module].performance.cache.max-size` | `Integer` | `1000` | 否 | 缓存最大条目数 |
| `nebula.[module].performance.cache.ttl` | `Duration` | `5m` | 否 | 缓存过期时间 |

**示例**：

```yaml
nebula:
  [module]:
    performance:
      cache:
        enabled: true
        max-size: 5000
        ttl: 10m
```

---

### 4. 监控配置

#### 4.1 监控开关

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].monitoring.enabled` | `Boolean` | `true` | 否 | 是否启用监控 |
| `nebula.[module].monitoring.interval` | `Duration` | `60s` | 否 | 监控采样间隔 |

**示例**：

```yaml
nebula:
  [module]:
    monitoring:
      enabled: true
      interval: 60s
```

#### 4.2 指标导出

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].monitoring.metrics.enabled` | `Boolean` | `true` | 否 | 是否导出指标 |
| `nebula.[module].monitoring.metrics.prefix` | `String` | `nebula.[module]` | 否 | 指标前缀 |

**示例**：

```yaml
nebula:
  [module]:
    monitoring:
      metrics:
        enabled: true
        prefix: "nebula.[module]"
```

---

### 5. 安全配置

#### 5.1 认证配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].security.auth.enabled` | `Boolean` | `false` | 否 | 是否启用认证 |
| `nebula.[module].security.auth.type` | `String` | `basic` | 否 | 认证类型 |
| `nebula.[module].security.auth.username` | `String` | - | 否 | 用户名 |
| `nebula.[module].security.auth.password` | `String` | - | 否 | 密码 |

**示例**：

```yaml
nebula:
  [module]:
    security:
      auth:
        enabled: true
        type: basic
        username: ${AUTH_USERNAME}  # 使用环境变量
        password: ${AUTH_PASSWORD}
```

#### 5.2 加密配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.[module].security.encryption.enabled` | `Boolean` | `false` | 否 | 是否启用加密 |
| `nebula.[module].security.encryption.algorithm` | `String` | `AES` | 否 | 加密算法 |
| `nebula.[module].security.encryption.key` | `String` | - | 否 | 加密密钥 |

**示例**：

```yaml
nebula:
  [module]:
    security:
      encryption:
        enabled: true
        algorithm: AES
        key: ${ENCRYPTION_KEY}  # 使用环境变量
```

---

## 配置示例

### 示例1：开发环境配置

**application-dev.yml**：

```yaml
spring:
  profiles:
    active: dev

nebula:
  [module]:
    enabled: true
    property1: dev-value
    property2: 50
    
    # 开发环境关闭性能优化，方便调试
    performance:
      cache:
        enabled: false
      pool-size: 5
    
    # 开启详细监控
    monitoring:
      enabled: true
      interval: 30s
    
    # 开发环境不需要认证
    security:
      auth:
        enabled: false

# 日志配置
logging:
  level:
    io.nebula.[module]: DEBUG
```

### 示例2：测试环境配置

**application-test.yml**：

```yaml
spring:
  profiles:
    active: test

nebula:
  [module]:
    enabled: true
    property1: test-value
    property2: 100
    
    # 测试环境使用中等性能配置
    performance:
      cache:
        enabled: true
        max-size: 1000
      pool-size: 10
      
    # 启用监控
    monitoring:
      enabled: true
      interval: 60s
    
    # 启用基础认证
    security:
      auth:
        enabled: true
        username: test
        password: test123

logging:
  level:
    io.nebula.[module]: INFO
```

### 示例3：生产环境配置

**application-prod.yml**：

```yaml
spring:
  profiles:
    active: prod

nebula:
  [module]:
    enabled: true
    property1: ${MODULE_PROPERTY1}  # 从环境变量读取
    property2: ${MODULE_PROPERTY2:200}  # 默认值200
    
    # 生产环境高性能配置
    performance:
      cache:
        enabled: true
        max-size: 10000
        ttl: 30m
      pool-size: 50
      queue-capacity: 1000
      connect-timeout: 5s
      read-timeout: 60s
      
    # 生产环境监控
    monitoring:
      enabled: true
      interval: 300s
      metrics:
        enabled: true
    
    # 生产环境安全配置
    security:
      auth:
        enabled: true
        username: ${AUTH_USERNAME}
        password: ${AUTH_PASSWORD}
      encryption:
        enabled: true
        key: ${ENCRYPTION_KEY}

logging:
  level:
    io.nebula.[module]: WARN
  file:
    name: /var/log/app/application.log
```

### 示例4：Docker环境配置

**docker-compose.yml**：

```yaml
version: '3.8'
services:
  app:
    image: your-app:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - NEBULA_[MODULE]_ENABLED=true
      - NEBULA_[MODULE]_PROPERTY1=prod-value
      - NEBULA_[MODULE]_PROPERTY2=200
      - AUTH_USERNAME=admin
      - AUTH_PASSWORD=secure-password
      - ENCRYPTION_KEY=your-encryption-key
    ports:
      - "8080:8080"
```

### 示例5：Kubernetes ConfigMap

**configmap.yaml**：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  application.yml: |
    nebula:
      [module]:
        enabled: true
        property1: k8s-value
        property2: 200
        performance:
          pool-size: 50
          cache:
            enabled: true
            max-size: 10000
        monitoring:
          enabled: true
```

---

## 多环境配置管理

### 方案1：Spring Profile

使用不同的配置文件：

```
src/main/resources/
├── application.yml          # 通用配置
├── application-dev.yml      # 开发环境
├── application-test.yml     # 测试环境
└── application-prod.yml     # 生产环境
```

启动时指定 Profile：

```bash
# 开发环境
java -jar app.jar --spring.profiles.active=dev

# 生产环境
java -jar app.jar --spring.profiles.active=prod
```

### 方案2：环境变量

使用环境变量覆盖配置：

```bash
export NEBULA_[MODULE]_ENABLED=true
export NEBULA_[MODULE]_PROPERTY1=value
export NEBULA_[MODULE]_PROPERTY2=200
```

### 方案3：外部配置文件

使用外部配置文件：

```bash
java -jar app.jar --spring.config.location=file:/etc/app/application.yml
```

### 方案4：配置中心

使用Nacos等配置中心：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        namespace: prod
        group: DEFAULT_GROUP
        file-extension: yaml
```

---

## 配置验证

### 启动时验证

模块启动时会自动验证配置的合法性。

**验证规则**：

1. 必填项是否配置
2. 配置值是否在合法范围内
3. 配置组合是否冲突

**验证失败示例**：

```
Caused by: IllegalArgumentException: 
  Property 'nebula.[module].property1' is required but not set.
```

### 手动验证

使用配置验证工具：

```java
@SpringBootTest
class ConfigValidationTest {
    
    @Autowired
    private ModuleProperties properties;
    
    @Test
    void validateConfig() {
        assertNotNull(properties.getProperty1());
        assertTrue(properties.getProperty2() > 0);
    }
}
```

---

## 配置最佳实践

### 实践1：使用环境变量管理敏感信息

**推荐**：

```yaml
nebula:
  [module]:
    security:
      password: ${DB_PASSWORD}
```

**不推荐**：

```yaml
nebula:
  [module]:
    security:
      password: plain-text-password  # 不要硬编码密码
```

### 实践2：使用合理的默认值

**推荐**：

```yaml
nebula:
  [module]:
    property2: ${CONFIG_VALUE:100}  # 提供默认值
```

### 实践3：配置分层

**推荐**：

```
application.yml           # 通用配置
application-{profile}.yml # 环境特定配置
```

### 实践4：配置注释

**推荐**：

```yaml
nebula:
  [module]:
    # 连接池大小，根据并发量调整
    # 建议值：轻量级10，中等50，重量级200
    pool-size: 50
```

### 实践5：配置验证

**推荐**：

```java
@ConfigurationProperties(prefix = "nebula.[module]")
@Validated
public class ModuleProperties {
    
    @NotNull
    @Min(1)
    @Max(1000)
    private Integer poolSize = 10;
}
```

---

## 配置迁移指南

### 从1.x迁移到2.x

#### 配置项变更

| 1.x配置项 | 2.x配置项 | 说明 |
|-----------|-----------|------|
| `old.property` | `nebula.[module].property` | 命名空间变更 |
| `old.feature` | `nebula.[module].advanced.feature` | 移至advanced |

#### 迁移步骤

1. **备份现有配置**

```bash
cp application.yml application.yml.backup
```

2. **更新配置命名空间**

```yaml
# 1.x
old:
  property: value

# 2.x
nebula:
  [module]:
    property: value
```

3. **测试验证**

```bash
mvn test
```

---

## 常见配置问题

### 问题1：配置不生效

**原因**：

- 配置项拼写错误
- 配置优先级问题
- 模块未启用

**解决方案**：

1. 检查配置项拼写
2. 确认模块已启用：`nebula.[module].enabled=true`
3. 查看启动日志确认配置加载情况

### 问题2：环境变量不生效

**原因**：

环境变量命名不正确。

**解决方案**：

```bash
# 正确的环境变量格式
# 配置项：nebula.[module].property-name
# 环境变量：NEBULA_[MODULE]_PROPERTY_NAME

export NEBULA_[MODULE]_PROPERTY_NAME=value
```

### 问题3：配置值类型错误

**原因**：

配置值类型与期望类型不匹配。

**解决方案**：

```yaml
# 错误：字符串值给Integer类型
nebula:
  [module]:
    property2: "100"  # 错误

# 正确
nebula:
  [module]:
    property2: 100  # 正确
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

## 附录

### 附录A：完整配置项索引

按字母顺序排列的所有配置项：

- `nebula.[module].enabled`
- `nebula.[module].property1`
- `nebula.[module].property2`
- ...

### 附录B：配置属性类

**ModuleProperties.java**：

```java
@ConfigurationProperties(prefix = "nebula.[module]")
@Data
public class ModuleProperties {
    
    private Boolean enabled = true;
    private String property1;
    private Integer property2 = 100;
    
    @Data
    public static class Advanced {
        private Boolean feature1 = false;
    }
    
    @Data
    public static class Performance {
        private Integer poolSize = 10;
    }
}
```

---

> 如有配置相关问题，请查阅文档或提Issue。

