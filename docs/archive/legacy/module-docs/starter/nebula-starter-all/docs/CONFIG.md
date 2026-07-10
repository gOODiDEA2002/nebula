# Nebula Starter All - 配置参考

> 全功能Starter的完整配置说明，包括所有模块的配置选项。

## 配置概览

- [基础配置](#基础配置)
- [数据库配置](#数据库配置)
- [缓存配置](#缓存配置)
- [RPC配置](#rpc配置)
- [消息队列配置](#消息队列配置)
- [搜索配置](#搜索配置)
- [存储配置](#存储配置)
- [AI配置](#ai配置)
- [任务调度配置](#任务调度配置)
- [票务系统完整配置](#票务系统完整配置)

---

## 基础配置

### Maven依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-all</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 最小配置

`application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: monolith-app
  
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
```

---

## 数据库配置

### MySQL配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.example.entity
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

---

## 缓存配置

### Redis+本地缓存

```yaml
spring:
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
  
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
      key-prefix: "app:"

nebula:
  cache:
    multilevel:
      enabled: true
      l1:
        max-size: 1000
        expire-after-write: 300
      l2:
        enabled: true
        expire-after-write: 3600
```

---

## RPC配置

```yaml
nebula:
  rpc:
    http:
      enabled: true
      port: 8080
    grpc:
      enabled: false  # 单体应用可禁用
      port: 9090
```

---

## 消息队列配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 5
        max-concurrency: 10
```

---

## 搜索配置

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: ${ES_PASSWORD:}
    connection-timeout: 5s
    socket-timeout: 30s
```

---

## 存储配置

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: myapp
```

---

## AI配置

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.7
    vectorstore:
      chroma:
        client:
          host: localhost
          port: 8000
```

---

## 任务调度配置

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 5
      thread-name-prefix: task-
```

---

## 票务系统完整配置

`application.yml`:

```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: 200
    max-connections: 10000

spring:
  application:
    name: ticket-monolith
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  # 数据库
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_db?useUnicode=true&characterEncoding=utf8
    username: root
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50
  
  # Redis
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
  
  # Elasticsearch
  elasticsearch:
    uris: http://localhost:9200
  
  # RabbitMQ
  rabbitmq:
    host: localhost
    port: 5672
  
  # AI
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
    vectorstore:
      chroma:
        client:
          host: localhost
          port: 8000
  
  # 缓存
  cache:
    type: redis
    redis:
      time-to-live: 3600000

# MyBatis-Plus
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.ticketsystem.entity
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted

# MinIO
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin

# 业务配置
ticket:
  order:
    payment-timeout: 15
    seat-lock-timeout: 10
  ai:
    customer-service:
      enabled: true
    recommendation:
      enabled: true

# 监控
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}

# 日志
logging:
  level:
    root: INFO
    com.ticketsystem: DEBUG
  file:
    name: logs/app.log
    max-size: 100MB
    max-history: 30
```

### 环境配置

**开发环境** `application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_dev
  data:
    redis:
      host: localhost
  elasticsearch:
    uris: http://localhost:9200

logging:
  level:
    root: DEBUG
```

**生产环境** `application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/ticket_prod
    hikari:
      maximum-pool-size: 100
  data:
    redis:
      host: prod-redis
      password: ${REDIS_PASSWORD}
  elasticsearch:
    uris: http://prod-es:9200

logging:
  level:
    root: INFO
  file:
    name: /var/log/ticket/app.log
```

---

## 按需启用/禁用功能

```yaml
nebula:
  # 禁用不需要的功能
  discovery:
    nacos:
      enabled: false
  
  rpc:
    grpc:
      enabled: false
  
  messaging:
    rabbitmq:
      enabled: true  # 保留需要的
  
  ai:
    enabled: true
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

