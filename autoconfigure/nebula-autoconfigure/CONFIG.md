# Nebula AutoConfigure - 配置指南

> 统一自动配置模块的配置说明

## 目录

- [基础配置](#基础配置)
- [模块配置](#模块配置)
- [配置优先级](#配置优先级)
- [环境配置](#环境配置)

---

## 基础配置

### 最小配置

```yaml
spring:
  application:
    name: my-app
```

Nebula框架的所有自动配置都是可选的，只需配置需要使用的模块即可。

---

## 模块配置

### 1. 核心层配置

#### Foundation基础配置

```yaml
nebula:
  foundation:
    # ID生成器配置
    id-generator:
      type: snowflake  # snowflake / uuid / business
      worker-id: 1
      datacenter-id: 1
    
    # JSON配置
    json:
      date-format: yyyy-MM-dd HH:mm:ss
      null-value-handling: ignore  # ignore / include
```

#### Security安全配置

```yaml
nebula:
  security:
    enabled: true
    
    # JWT配置
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400  # 24小时
      refresh-expiration: 604800  # 7天
    
    # RBAC配置
    rbac:
      enabled: true
      super-admin-role: SUPER_ADMIN
```

### 2. 数据访问层配置

#### 数据持久化

```yaml
nebula:
  data:
    persistence:
      enabled: true
      
      # MyBatis-Plus配置
      mybatis-plus:
        logic-delete: true
        optimistic-lock: true
      
      # 分页配置
      pagination:
        default-page-size: 10
        max-page-size: 100
```

#### 缓存配置

```yaml
nebula:
  cache:
    enabled: true
    
    # Redis配置
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
    
    # Caffeine配置
    caffeine:
      max-size: 1000
      expire-after-write: 3600
```

#### MongoDB配置

```yaml
nebula:
  data:
    mongodb:
      enabled: true
      uri: ${MONGODB_URI:mongodb://localhost:27017/mydb}
```

### 3. 消息传递配置

```yaml
nebula:
  messaging:
    enabled: true
    
    # RabbitMQ配置
    rabbitmq:
      host: ${RABBITMQ_HOST:localhost}
      port: ${RABBITMQ_PORT:5672}
      username: ${RABBITMQ_USERNAME:guest}
      password: ${RABBITMQ_PASSWORD:guest}
      virtual-host: /
```

### 4. RPC通信配置

#### HTTP RPC

```yaml
nebula:
  rpc:
    http:
      enabled: true
      
      # 连接池配置
      connection:
        max-total: 200
        max-per-route: 50
        connect-timeout: 5000
        socket-timeout: 30000
      
      # 重试配置
      retry:
        max-attempts: 3
        backoff-interval: 1000
```

#### gRPC

```yaml
nebula:
  rpc:
    grpc:
      enabled: true
      
      # 服务器配置
      server:
        port: 9090
        max-inbound-message-size: 4194304  # 4MB
      
      # 客户端配置
      client:
        negotiation-type: plaintext
```

### 5. 服务发现配置

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      
      # Nacos服务器地址
      server-addr: ${NACOS_SERVER:localhost:8848}
      
      # 命名空间
      namespace: ${NACOS_NAMESPACE:public}
      
      # 分组
      group: ${NACOS_GROUP:DEFAULT_GROUP}
      
      # 服务注册
      service:
        name: ${spring.application.name}
        ip: ${SERVER_IP:}
        port: ${server.port:8080}
```

### 6. 存储服务配置

#### MinIO

```yaml
nebula:
  storage:
    minio:
      enabled: true
      endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      default-bucket: ${MINIO_BUCKET:default}
```

#### 阿里云OSS

```yaml
nebula:
  storage:
    aliyun-oss:
      enabled: true
      endpoint: ${OSS_ENDPOINT:oss-cn-hangzhou.aliyuncs.com}
      access-key-id: ${OSS_ACCESS_KEY_ID}
      access-key-secret: ${OSS_ACCESS_KEY_SECRET}
      default-bucket: ${OSS_BUCKET:default}
```

### 7. 搜索服务配置

```yaml
nebula:
  search:
    elasticsearch:
      enabled: true
      uris: ${ES_URIS:http://localhost:9200}
      username: ${ES_USERNAME:}
      password: ${ES_PASSWORD:}
```

### 8. AI服务配置

```yaml
nebula:
  ai:
    spring:
      enabled: true
      
      # OpenAI配置
      openai:
        api-key: ${OPENAI_API_KEY}
        model: gpt-4
```

### 9. 分布式锁配置

```yaml
nebula:
  lock:
    redis:
      enabled: true
      
      # 锁配置
      default-lease-time: 30
      default-wait-time: 10
```

### 10. Web框架配置

```yaml
nebula:
  web:
    enabled: true
    
    # CORS配置
    cors:
      enabled: true
      allowed-origins: "*"
      allowed-methods: "*"
      allowed-headers: "*"
    
    # 全局异常处理
    exception-handler:
      enabled: true
      include-stack-trace: false
```

### 11. 任务调度配置

```yaml
nebula:
  task:
    enabled: true
    
    # XXL-Job配置
    xxl-job:
      admin-addresses: ${XXL_JOB_ADMIN:http://localhost:8080/xxl-job-admin}
      access-token: ${XXL_JOB_TOKEN:}
      app-name: ${spring.application.name}
      executor-port: 9999
```

### 12. 集成层配置

#### 支付集成

```yaml
nebula:
  integration:
    payment:
      enabled: true
      
      # 支付宝
      alipay:
        enabled: true
        app-id: ${ALIPAY_APP_ID}
        private-key: ${ALIPAY_PRIVATE_KEY}
        public-key: ${ALIPAY_PUBLIC_KEY}
      
      # 微信支付
      wechat:
        enabled: true
        app-id: ${WECHAT_APP_ID}
        mch-id: ${WECHAT_MCH_ID}
        api-key: ${WECHAT_API_KEY}
```

#### 通知集成

```yaml
nebula:
  integration:
    notification:
      enabled: true
      
      # 短信
      sms:
        provider: aliyun
        aliyun:
          access-key-id: ${ALIYUN_ACCESS_KEY_ID}
          access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
      
      # 邮件
      email:
        enabled: true
        host: ${MAIL_HOST}
        port: ${MAIL_PORT}
        username: ${MAIL_USERNAME}
        password: ${MAIL_PASSWORD}
```

---

## 配置优先级

### 1. 配置来源优先级（从高到低）

1. 命令行参数
2. Java系统属性（-D参数）
3. 操作系统环境变量
4. application-{profile}.yml
5. application.yml
6. @PropertySource
7. 默认配置

### 2. Profile配置

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

---
# application-dev.yml
nebula:
  cache:
    redis:
      host: localhost
      port: 6379

---
# application-prod.yml
nebula:
  cache:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
```

---

## 环境配置

### 开发环境

```yaml
spring:
  profiles:
    active: dev

nebula:
  # 启用所有调试功能
  foundation:
    debug: true
  
  # 使用本地服务
  cache:
    redis:
      host: localhost
  
  messaging:
    rabbitmq:
      host: localhost
  
  discovery:
    nacos:
      server-addr: localhost:8848
  
  # 开发环境放宽限制
  security:
    jwt:
      expiration: 86400  # 24小时
```

### 测试环境

```yaml
spring:
  profiles:
    active: test

nebula:
  # 使用测试服务
  cache:
    redis:
      host: test-redis
  
  messaging:
    rabbitmq:
      host: test-rabbitmq
  
  discovery:
    nacos:
      server-addr: test-nacos:8848
      namespace: test
```

### 生产环境

```yaml
spring:
  profiles:
    active: prod

nebula:
  # 使用环境变量
  cache:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
  
  messaging:
    rabbitmq:
      host: ${RABBITMQ_HOST}
      port: ${RABBITMQ_PORT}
      username: ${RABBITMQ_USERNAME}
      password: ${RABBITMQ_PASSWORD}
  
  discovery:
    nacos:
      server-addr: ${NACOS_SERVER}
      namespace: ${NACOS_NAMESPACE}
  
  # 生产环境安全配置
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 7200  # 2小时
  
  # 生产环境性能配置
  rpc:
    http:
      connection:
        max-total: 500
        max-per-route: 100
```

---

## 配置验证

### 使用@Validated注解

```java
@Data
@Validated
@ConfigurationProperties(prefix = "nebula.security.jwt")
public class JwtProperties {
    
    @NotBlank(message = "JWT secret不能为空")
    private String secret;
    
    @Min(value = 3600, message = "JWT过期时间不能少于1小时")
    private int expiration = 86400;
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

