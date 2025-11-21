# Nebula Starter Web - 配置参考

> Web应用专用Starter的完整配置说明，包括Web服务器、数据库、缓存、安全等配置。

## 配置概览

本文档包含以下配置内容：

- [基础配置](#基础配置)
- [Web服务器配置](#web服务器配置)
- [数据库配置](#数据库配置)
- [缓存配置](#缓存配置)
- [安全配置](#安全配置)
- [监控配置](#监控配置)
- [跨域配置](#跨域配置)
- [文件上传配置](#文件上传配置)
- [票务系统配置示例](#票务系统配置示例)

---

## 基础配置

### Maven依赖

在 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- Nebula Web Starter -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-web</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- 数据库驱动（如需要） -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
</dependencies>
```

### 最小配置文件

`application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: web-app
```

---

## Web服务器配置

### Tomcat服务器配置

`application.yml`:

```yaml
server:
  # 端口配置
  port: 8080
  
  # Servlet配置
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
      enabled: true
      force: true
  
  # Tomcat配置
  tomcat:
    # 最大线程数
    threads:
      max: 200
      min-spare: 10
    
    # 最大连接数
    max-connections: 10000
    accept-count: 100
    
    # 连接超时
    connection-timeout: 20000
    
    # URI编码
    uri-encoding: UTF-8
    
    # 访问日志
    accesslog:
      enabled: true
      directory: logs
      pattern: common
      file-date-format: .yyyy-MM-dd
  
  # 压缩配置
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml
    min-response-size: 1024
  
  # HTTP2配置
  http2:
    enabled: true
  
  # 错误页面配置
  error:
    include-message: always
    include-binding-errors: always
    include-stacktrace: never
    include-exception: false
```

### SSL/HTTPS配置

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: tomcat
```

---

## 数据库配置

### MySQL配置

`application.yml`:

```yaml
spring:
  datasource:
    # 数据库连接
    url: jdbc:mysql://localhost:3306/ticket_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    # HikariCP连接池配置
    hikari:
      # 最小空闲连接数
      minimum-idle: 5
      # 最大连接数
      maximum-pool-size: 20
      # 连接超时时间（毫秒）
      connection-timeout: 30000
      # 空闲超时时间（毫秒）
      idle-timeout: 600000
      # 最大生命周期（毫秒）
      max-lifetime: 1800000
      # 连接测试查询
      connection-test-query: SELECT 1

# MyBatis-Plus配置
mybatis-plus:
  # Mapper XML文件位置
  mapper-locations: classpath*:mapper/**/*.xml
  # 类型别名包路径
  type-aliases-package: com.example.entity
  # 全局配置
  global-config:
    db-config:
      # 主键类型
      id-type: assign_id
      # 逻辑删除字段
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  # 配置
  configuration:
    # 驼峰转下划线
    map-underscore-to-camel-case: true
    # 日志
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
    # 缓存
    cache-enabled: true
```

### 多数据源配置

```yaml
spring:
  datasource:
    # 主数据源
    master:
      url: jdbc:mysql://localhost:3306/ticket_system
      username: root
      password: password
      
    # 从数据源（读）
    slave:
      url: jdbc:mysql://localhost:3307/ticket_system
      username: readonly
      password: password
```

---

## 缓存配置

### Redis缓存配置

`application.yml`:

```yaml
spring:
  # Redis配置
  data:
    redis:
      # 单机模式
      host: localhost
      port: 6379
      password:
      database: 0
      
      # 连接池配置
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
      
      # 超时配置
      timeout: 3000ms
      connect-timeout: 3000ms
  
  # 缓存配置
  cache:
    # 缓存类型
    type: redis
    # 缓存名称
    cache-names:
      - movies
      - users
      - products
    # Redis缓存配置
    redis:
      # 缓存过期时间（毫秒）
      time-to-live: 600000
      # 缓存key前缀
      key-prefix: "cache:"
      # 是否缓存null值
      cache-null-values: true
      # 是否使用前缀
      use-key-prefix: true
```

### 多级缓存配置

```yaml
nebula:
  cache:
    # 启用多级缓存
    multilevel:
      enabled: true
      # L1缓存（本地缓存）
      l1:
        max-size: 1000
        expire-after-write: 300
      # L2缓存（Redis）
      l2:
        enabled: true
        expire-after-write: 600
```

---

## 安全配置

### JWT配置

`application.yml`:

```yaml
nebula:
  security:
    # JWT配置
    jwt:
      # 密钥
      secret: ${JWT_SECRET:your-secret-key-at-least-256-bits}
      # Token过期时间（秒）
      expiration: 86400  # 24小时
      # 刷新Token过期时间（秒）
      refresh-expiration: 604800  # 7天
      # Token前缀
      token-prefix: Bearer
      # Header名称
      header: Authorization
    
    # 密码加密配置
    password:
      # 盐长度
      salt-length: 16
      # 强制强密码
      enforce-strong: true
      # 最小长度
      min-length: 8
```

### CORS跨域配置

```yaml
spring:
  web:
    cors:
      # 允许的来源
      allowed-origins:
        - http://localhost:3000
        - https://example.com
      # 允许的方法
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      # 允许的请求头
      allowed-headers:
        - "*"
      # 是否允许携带凭证
      allow-credentials: true
      # 预检请求有效期（秒）
      max-age: 3600
```

或通过代码配置：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 监控配置

### Actuator监控

`application.yml`:

```yaml
management:
  # 端点配置
  endpoints:
    web:
      # 基础路径
      base-path: /actuator
      # 暴露的端点
      exposure:
        include: health,info,metrics,prometheus
  
  # 健康检查
  health:
    # 显示详细信息
    show-details: always
    # Redis健康检查
    redis:
      enabled: true
    # 数据库健康检查
    db:
      enabled: true
  
  # Metrics配置
  metrics:
    # 标签
    tags:
      application: ${spring.application.name}
    # 导出配置
    export:
      prometheus:
        enabled: true
```

### 日志配置

`application.yml`:

```yaml
logging:
  level:
    root: INFO
    io.nebula: DEBUG
    com.example: DEBUG
    # SQL日志
    com.example.mapper: DEBUG
    # HTTP日志
    org.springframework.web: DEBUG
  
  # 日志文件
  file:
    name: logs/app.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 1GB
  
  # 日志格式
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
```

---

## 跨域配置

### 全局CORS配置

创建配置类：

```java
@Configuration
public class GlobalCorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的来源
        config.addAllowedOriginPattern("*");
        
        // 允许的方法
        config.addAllowedMethod("*");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 是否允许携带凭证
        config.setAllowCredentials(true);
        
        // 预检请求有效期
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        return new CorsFilter(source);
    }
}
```

---

## 文件上传配置

### 文件上传限制

`application.yml`:

```yaml
spring:
  servlet:
    multipart:
      # 是否启用
      enabled: true
      # 单个文件最大大小
      max-file-size: 10MB
      # 请求最大大小
      max-request-size: 100MB
      # 文件大小阈值（超过则写入磁盘）
      file-size-threshold: 0
      # 临时目录
      location: /tmp
```

### 文件存储配置

```yaml
# 自定义文件存储配置
file:
  # 上传路径
  upload-path: /uploads
  # 允许的文件类型
  allowed-types:
    - image/jpeg
    - image/png
    - image/gif
    - application/pdf
  # 文件大小限制（字节）
  max-size: 10485760  # 10MB
```

**配置类**:

```java
@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class FileConfig {
    private String uploadPath = "/uploads";
    private List<String> allowedTypes = new ArrayList<>();
    private long maxSize = 10485760;
}
```

---

## 票务系统配置示例

### 完整配置文件

`application.yml`:

```yaml
# ==================== 服务器配置 ====================
server:
  port: 8080
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
      enabled: true
  tomcat:
    threads:
      max: 200
    max-connections: 10000
    uri-encoding: UTF-8
  compression:
    enabled: true

# ==================== Spring配置 ====================
spring:
  application:
    name: ticket-system
  
  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  # Redis配置
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
  
  # 缓存配置
  cache:
    type: redis
    cache-names:
      - movies
      - showtimes
      - cinemas
      - users
    redis:
      time-to-live: 600000  # 10分钟
      key-prefix: "ticket:"
      cache-null-values: false
  
  # 文件上传配置
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB

# ==================== MyBatis-Plus配置 ====================
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.ticketsystem.entity
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

# ==================== Nebula安全配置 ====================
nebula:
  security:
    jwt:
      secret: ${JWT_SECRET:ticket-system-secret-key-256-bits-minimum}
      expiration: 86400  # 24小时
      refresh-expiration: 604800  # 7天
    password:
      salt-length: 16
      enforce-strong: true
      min-length: 8

# ==================== 业务配置 ====================
ticket:
  # 订单配置
  order:
    # 订单支付超时（分钟）
    payment-timeout: 15
    # 座位锁定超时（分钟）
    seat-lock-timeout: 10
  
  # 座位配置
  seat:
    # 座位锁定时间（秒）
    lock-duration: 600  # 10分钟
    # 最多选座数量
    max-selection: 5
  
  # 票价配置
  price:
    # 服务费率
    service-fee-rate: 0.05  # 5%
    # 最低服务费
    min-service-fee: 1.0

# ==================== 监控配置 ====================
management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,metrics,prometheus
  health:
    show-details: always
    redis:
      enabled: true
    db:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}

# ==================== 日志配置 ====================
logging:
  level:
    root: INFO
    io.nebula: DEBUG
    com.ticketsystem: DEBUG
    com.ticketsystem.mapper: DEBUG
  file:
    name: logs/ticket-system.log
    max-size: 100MB
    max-history: 30
```

### 环境配置

**开发环境** `application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_system_dev
  data:
    redis:
      host: localhost

logging:
  level:
    root: DEBUG
    com.ticketsystem: DEBUG

ticket:
  order:
    payment-timeout: 30  # 开发环境延长超时时间
```

**测试环境** `application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://test-db:3306/ticket_system_test
  data:
    redis:
      host: test-redis

logging:
  level:
    root: INFO
    com.ticketsystem: DEBUG
```

**生产环境** `application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/ticket_system
    hikari:
      maximum-pool-size: 100
  data:
    redis:
      host: prod-redis

logging:
  level:
    root: INFO
    com.ticketsystem: INFO
  file:
    name: /var/log/ticket-system/app.log

server:
  tomcat:
    threads:
      max: 500
    max-connections: 20000
```

---

## 配置最佳实践

### 实践1：使用环境变量

敏感信息通过环境变量配置：

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}
  data:
    redis:
      password: ${REDIS_PASSWORD}

nebula:
  security:
    jwt:
      secret: ${JWT_SECRET}
```

### 实践2：配置分层

```
config/
├── application.yml           # 通用配置
├── application-dev.yml       # 开发环境
├── application-test.yml      # 测试环境
└── application-prod.yml      # 生产环境
```

### 实践3：配置验证

```java
@Configuration
@ConfigurationProperties(prefix = "ticket")
@Validated
@Data
public class TicketConfig {
    
    @Valid
    private OrderConfig order;
    
    @Valid
    private SeatConfig seat;
    
    @Data
    public static class OrderConfig {
        @Min(value = 1, message = "支付超时时间至少为1分钟")
        private int paymentTimeout = 15;
        
        @Min(value = 1, message = "座位锁定超时时间至少为1分钟")
        private int seatLockTimeout = 10;
    }
    
    @Data
    public static class SeatConfig {
        @Min(value = 1, message = "最多选座数量至少为1")
        @Max(value = 10, message = "最多选座数量不能超过10")
        private int maxSelection = 5;
    }
}
```

### 实践4：配置加密

使用Jasypt加密敏感配置：

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

```yaml
jasypt:
  encryptor:
    password: ${JASYPT_PASSWORD}
    algorithm: PBEWithMD5AndDES

spring:
  datasource:
    password: ENC(encrypted_password)
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划
- [Nebula Web文档](../../application/nebula-web/README.md) - Web模块详细文档

---

> 如有问题或建议，欢迎提Issue。

