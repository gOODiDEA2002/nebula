# Nebula Starter Service - 配置参考

> 微服务应用专用Starter的完整配置说明，包括RPC、服务发现、消息队列、分布式锁等配置。

## 配置概览

- [基础配置](#基础配置)
- [RPC配置](#rpc配置)
- [服务发现配置](#服务发现配置)
- [消息队列配置](#消息队列配置)
- [分布式锁配置](#分布式锁配置)
- [任务调度配置](#任务调度配置)
- [票务系统配置示例](#票务系统配置示例)

---

## 基础配置

### Maven依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 最小配置

`application.yml`:

```yaml
spring:
  application:
    name: my-service

server:
  port: 8080

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  rpc:
    http:
      enabled: true
  messaging:
    rabbitmq:
      enabled: true
```

---

## RPC配置

### HTTP RPC配置

`application.yml`:

```yaml
nebula:
  rpc:
    http:
      # 启用HTTP RPC
      enabled: true
      # 服务端口
      port: 8080
      # 连接超时（毫秒）
      connect-timeout: 5000
      # 读取超时（毫秒）
      read-timeout: 30000
      # 最大连接数
      max-connections: 200
      # 服务发现
      discovery:
        enabled: true
      # RPC服务配置
      services:
        user-service:
          url: http://localhost:8081
          connect-timeout: 3000
          read-timeout: 10000
        order-service:
          url: http://localhost:8082
```

### gRPC配置

```yaml
nebula:
  rpc:
    grpc:
      # 启用gRPC
      enabled: true
      # gRPC端口
      port: 9090
      # 最大消息大小（字节）
      max-inbound-message-size: 4194304  # 4MB
      # 最大Header大小（字节）
      max-inbound-metadata-size: 8192
      # 启用TLS
      tls:
        enabled: false
        cert-chain: classpath:cert.pem
        private-key: classpath:key.pem
```

---

## 服务发现配置

### Nacos配置

`application.yml`:

```yaml
nebula:
  discovery:
    nacos:
      # 启用Nacos
      enabled: true
      # Nacos服务器地址
      server-addr: localhost:8848
      # 命名空间
      namespace: dev
      # 分组
      group: DEFAULT_GROUP
      # 集群名称
      cluster-name: DEFAULT
      # 服务元数据
      metadata:
        version: 1.0.0
        region: cn-hangzhou
      # 心跳间隔（秒）
      heart-beat-interval: 5
      # 心跳超时（秒）
      heart-beat-timeout: 15
      # IP选择
      ip-delete-timeout: 30
```

### Nacos配置中心

```yaml
spring:
  cloud:
    nacos:
      config:
        enabled: true
        server-addr: localhost:8848
        namespace: dev
        group: DEFAULT_GROUP
        file-extension: yml
        shared-configs:
          - dataId: common-config.yml
            group: DEFAULT_GROUP
            refresh: true
```

---

## 消息队列配置

### RabbitMQ配置

`application.yml`:

```yaml
nebula:
  messaging:
    rabbitmq:
      # 启用RabbitMQ
      enabled: true
      # 服务器地址
      host: localhost
      # 端口
      port: 5672
      # 用户名
      username: guest
      # 密码
      password: guest
      # 虚拟主机
      virtual-host: /
      # 连接超时（毫秒）
      connection-timeout: 60000
      # 发布确认
      publisher-confirms: true
      # 发布返回
      publisher-returns: true
      # 消费者配置
      listener:
        # 手动确认
        acknowledge-mode: manual
        # 最大并发消费者数
        max-concurrency: 10
        # 预取数量
        prefetch: 1
```

### 交换机和队列配置

```java
@Configuration
public class RabbitMQConfig {
    
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
            .ttl(60000)  // 消息TTL 60秒
            .maxLength(10000)  // 最大长度
            .build();
    }
    
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange");
    }
    
    @Bean
    public Binding orderBinding() {
        return BindingBuilder
            .bind(orderQueue())
            .to(orderExchange())
            .with("order.*");
    }
}
```

---

## 分布式锁配置

### Redis分布式锁配置

`application.yml`:

```yaml
nebula:
  lock:
    redis:
      # 启用Redis分布式锁
      enabled: true
      # Redis连接（继承spring.data.redis配置）
      # 默认锁过期时间（毫秒）
      lease-time: 30000
      # 默认等待时间（毫秒）
      wait-time: 3000
      # 看门狗超时（毫秒）
      watchdog-timeout: 30000
```

---

## 任务调度配置

### XXL-JOB配置

`application.yml`:

```yaml
xxl:
  job:
    # 管理端地址
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    # 访问令牌
    access-token: default_token
    # 执行器配置
    executor:
      # 执行器名称
      appname: ${spring.application.name}
      # 执行器端口
      port: 9999
      # 日志路径
      logpath: ./logs/xxl-job
      # 日志保留天数
      logretentiondays: 30
```

---

## 票务系统配置示例

### 用户服务配置

`user-service/application.yml`:

```yaml
# ==================== 基础配置 ====================
spring:
  application:
    name: user-service

server:
  port: 8081

# ==================== 数据库配置 ====================
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_user?useUnicode=true&characterEncoding=utf8
    username: root
    password: ${DB_PASSWORD}

# ==================== Redis配置 ====================
spring:
  data:
    redis:
      host: localhost
      port: 6379

# ==================== Nacos配置 ====================
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: ticket-system
      group: SERVICE_GROUP
      metadata:
        version: 1.0.0
        service-type: user

# ==================== RPC配置 ====================
nebula:
  rpc:
    http:
      enabled: true
      port: 8081

# ==================== 消息队列配置 ====================
nebula:
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
```

### 订单服务配置

`order-service/application.yml`:

```yaml
# ==================== 基础配置 ====================
spring:
  application:
    name: order-service

server:
  port: 8082

# ==================== 数据库配置 ====================
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_order?useUnicode=true&characterEncoding=utf8
    username: root
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50

# ==================== Redis配置 ====================
spring:
  data:
    redis:
      host: localhost
      port: 6379

# ==================== Nacos配置 ====================
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: ticket-system
      group: SERVICE_GROUP

# ==================== RPC配置 ====================
nebula:
  rpc:
    http:
      enabled: true
      discovery:
        enabled: true
      services:
        user-service:
          connect-timeout: 3000
          read-timeout: 10000
        showtime-service:
          connect-timeout: 3000
          read-timeout: 10000

# ==================== 消息队列配置 ====================
nebula:
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      listener:
        acknowledge-mode: manual
        max-concurrency: 20

# ==================== 分布式锁配置 ====================
nebula:
  lock:
    redis:
      enabled: true
      lease-time: 10000  # 10秒
      wait-time: 3000    # 3秒

# ==================== 任务调度配置 ====================
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      appname: order-service
      port: 9999

# ==================== 业务配置 ====================
ticket:
  order:
    payment-timeout: 15  # 支付超时（分钟）
    seat-lock-timeout: 10  # 座位锁定超时（分钟）
```

### 通知服务配置

`notification-service/application.yml`:

```yaml
spring:
  application:
    name: notification-service

server:
  port: 8083

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: ticket-system
  
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      listener:
        acknowledge-mode: manual
        max-concurrency: 10

# 通知配置
notification:
  email:
    enabled: true
    host: smtp.example.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
  sms:
    enabled: true
    provider: aliyun
    access-key: ${SMS_ACCESS_KEY}
    secret-key: ${SMS_SECRET_KEY}
```

### 完整微服务配置示例

**docker-compose.yml**（基础设施）:

```yaml
version: '3.8'

services:
  nacos:
    image: nacos/nacos-server:v2.2.3
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      - MODE=standalone

  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=root123

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=guest
      - RABBITMQ_DEFAULT_PASS=guest
```

**启动脚本** `start-services.sh`:

```bash
#!/bin/bash

# 启动基础设施
docker-compose up -d

# 等待服务就绪
sleep 30

# 启动微服务
cd user-service && mvn spring-boot:run &
cd order-service && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &
```

---

## 配置最佳实践

### 实践1：环境变量管理

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}
  data:
    redis:
      password: ${REDIS_PASSWORD}

nebula:
  discovery:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
```

### 实践2：配置中心化

使用Nacos配置中心管理所有配置：

```bash
# 上传配置到Nacos
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=order-service.yml" \
  -d "group=SERVICE_GROUP" \
  -d "content=$(cat application.yml)"
```

### 实践3：配置验证

```java
@Configuration
@ConfigurationProperties(prefix = "ticket.order")
@Validated
public class OrderConfig {
    @Min(1)
    private int paymentTimeout = 15;
    
    @Min(1)
    private int seatLockTimeout = 10;
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

