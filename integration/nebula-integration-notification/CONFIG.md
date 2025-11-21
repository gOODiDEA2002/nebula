# Nebula Integration Notification - 配置示例

> 通知集成模块的详细配置指南

## 目录

- [基础配置](#基础配置)
- [短信配置](#短信配置)
- [邮件配置](#邮件配置)
- [推送配置](#推送配置)
- [站内消息配置](#站内消息配置)
- [多环境配置](#多环境配置)
- [高级配置](#高级配置)

---

## 基础配置

### 最小配置

```yaml
nebula:
  notification:
    enabled: true
```

### 完整基础配置

```yaml
nebula:
  notification:
    # 是否启用通知模块
    enabled: true
    
    # 是否启用异步发送
    async-enabled: true
    
    # 是否记录通知历史
    record-enabled: true
    
    # 默认通知渠道优先级
    default-channels:
      - SMS
      - EMAIL
      - PUSH
      - INTERNAL
```

---

## 短信配置

### 1. 阿里云短信

```yaml
nebula:
  notification:
    sms:
      # 短信服务提供商（aliyun, tencent, huawei）
      provider: aliyun
      
      # 阿里云配置
      aliyun:
        # Access Key ID
        access-key-id: ${ALIYUN_ACCESS_KEY_ID}
        
        # Access Key Secret
        access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
        
        # 短信服务端点
        endpoint: dysmsapi.aliyuncs.com
        
        # 短信签名
        sign-name: "票务系统"
        
        # 短信模板
        templates:
          # 验证码模板
          verify-code: SMS_123456789
          
          # 订单确认模板
          order-confirm: SMS_234567890
          
          # 订单取消模板
          order-cancel: SMS_345678901
          
          # 退款成功模板
          refund-success: SMS_456789012
          
          # 演出提醒模板
          show-reminder: SMS_567890123
```

### 2. 腾讯云短信

```yaml
nebula:
  notification:
    sms:
      provider: tencent
      
      tencent:
        # SecretId
        secret-id: ${TENCENT_SECRET_ID}
        
        # SecretKey
        secret-key: ${TENCENT_SECRET_KEY}
        
        # SMS应用ID
        app-id: ${TENCENT_SMS_APP_ID}
        
        # 短信签名
        sign-name: "票务系统"
        
        # 短信模板
        templates:
          verify-code: "123456"
          order-confirm: "234567"
```

### 3. 华为云短信

```yaml
nebula:
  notification:
    sms:
      provider: huawei
      
      huawei:
        # App Key
        app-key: ${HUAWEI_APP_KEY}
        
        # App Secret
        app-secret: ${HUAWEI_APP_SECRET}
        
        # 短信通道号
        sender: ${HUAWEI_SENDER}
        
        # 短信签名
        signature: "票务系统"
        
        # 短信模板
        templates:
          verify-code: "template_001"
          order-confirm: "template_002"
```

### 4. 短信发送配置

```yaml
nebula:
  notification:
    sms:
      # 发送频率限制
      rate-limit:
        # 是否启用限流
        enabled: true
        
        # 每个手机号每分钟最多发送次数
        max-per-minute: 1
        
        # 每个手机号每小时最多发送次数
        max-per-hour: 5
        
        # 每个手机号每天最多发送次数
        max-per-day: 10
      
      # 重试配置
      retry:
        # 是否启用重试
        enabled: true
        
        # 最大重试次数
        max-attempts: 3
        
        # 重试间隔（毫秒）
        backoff-interval: 1000
```

---

## 邮件配置

### 1. SMTP配置

```yaml
nebula:
  notification:
    email:
      # 是否启用邮件
      enabled: true
      
      # SMTP服务器
      host: smtp.example.com
      
      # SMTP端口
      port: 465
      
      # 用户名
      username: ${MAIL_USERNAME}
      
      # 密码
      password: ${MAIL_PASSWORD}
      
      # 发件人邮箱
      from: no-reply@ticket-system.com
      
      # 发件人名称
      from-name: "票务系统"
      
      # 是否启用SSL
      ssl: true
      
      # 是否启用TLS
      tls: false
      
      # 连接超时（毫秒）
      timeout: 5000
```

### 2. 邮件模板配置

```yaml
nebula:
  notification:
    email:
      # 邮件模板路径
      templates-path: classpath:mail-templates/
      
      # 模板引擎（thymeleaf, freemarker, velocity）
      template-engine: thymeleaf
      
      # 模板缓存
      template-cache: true
      
      # 模板编码
      template-encoding: UTF-8
```

### 3. 邮件内容配置

```yaml
nebula:
  notification:
    email:
      # 默认主题前缀
      subject-prefix: "[票务系统] "
      
      # 是否启用HTML邮件
      html-enabled: true
      
      # 是否启用附件
      attachment-enabled: true
      
      # 附件最大大小（MB）
      max-attachment-size: 10
```

### 4. 主流邮箱配置示例

#### Gmail

```yaml
nebula:
  notification:
    email:
      host: smtp.gmail.com
      port: 587
      username: ${GMAIL_USERNAME}
      password: ${GMAIL_APP_PASSWORD}  # 应用专用密码
      from: ${GMAIL_USERNAME}
      from-name: "Ticket System"
      ssl: false
      tls: true
```

#### QQ邮箱

```yaml
nebula:
  notification:
    email:
      host: smtp.qq.com
      port: 465
      username: ${QQ_EMAIL}
      password: ${QQ_AUTH_CODE}  # 授权码
      from: ${QQ_EMAIL}
      from-name: "票务系统"
      ssl: true
      tls: false
```

#### 163邮箱

```yaml
nebula:
  notification:
    email:
      host: smtp.163.com
      port: 465
      username: ${163_EMAIL}
      password: ${163_AUTH_CODE}  # 授权码
      from: ${163_EMAIL}
      from-name: "票务系统"
      ssl: true
      tls: false
```

---

## 推送配置

### 1. Firebase Cloud Messaging (FCM)

```yaml
nebula:
  notification:
    push:
      enabled: true
      
      providers:
        # Firebase Cloud Messaging
        - type: fcm
          enabled: true
          
          # FCM服务器密钥
          server-key: ${FCM_SERVER_KEY}
          
          # FCM项目ID
          project-id: ${FCM_PROJECT_ID}
          
          # 服务账号凭据文件
          credentials-path: classpath:firebase-credentials.json
```

### 2. Apple Push Notification Service (APNS)

```yaml
nebula:
  notification:
    push:
      providers:
        # Apple Push Notification Service
        - type: apns
          enabled: true
          
          # APNS证书路径
          certificate-path: ${APNS_CERT_PATH}
          
          # APNS证书密码
          certificate-password: ${APNS_CERT_PASSWORD}
          
          # Bundle ID
          bundle-id: com.example.ticket
          
          # 是否使用生产环境
          production: true
```

### 3. 华为推送服务 (HMS)

```yaml
nebula:
  notification:
    push:
      providers:
        # Huawei Mobile Services
        - type: hms
          enabled: true
          
          # HMS App ID
          app-id: ${HMS_APP_ID}
          
          # HMS App Secret
          app-secret: ${HMS_APP_SECRET}
```

### 4. 小米推送

```yaml
nebula:
  notification:
    push:
      providers:
        # Xiaomi Push
        - type: xiaomi
          enabled: true
          
          # 小米App Secret
          app-secret: ${XIAOMI_APP_SECRET}
          
          # Android包名
          package-name: com.example.ticket
```

### 5. 推送通用配置

```yaml
nebula:
  notification:
    push:
      # 默认推送优先级（high, normal, low）
      default-priority: high
      
      # 默认推送声音
      default-sound: default
      
      # 默认震动
      default-vibration: true
      
      # 推送有效期（秒）
      time-to-live: 86400
      
      # 批量推送配置
      batch:
        # 是否启用批量推送
        enabled: true
        
        # 批量大小
        size: 1000
        
        # 批量间隔（毫秒）
        interval: 1000
```

---

## 站内消息配置

### 1. 基础配置

```yaml
nebula:
  notification:
    internal:
      # 是否启用站内消息
      enabled: true
      
      # 最大未读消息数
      max-unread: 100
      
      # 消息保留天数
      retention-days: 90
      
      # 是否自动标记已读
      auto-mark-read: false
      
      # 自动标记已读延迟（秒）
      auto-mark-read-delay: 5
```

### 2. WebSocket配置

```yaml
nebula:
  notification:
    internal:
      # WebSocket配置
      websocket:
        # 是否启用WebSocket实时推送
        enabled: true
        
        # WebSocket端点
        endpoint: /ws/notifications
        
        # 心跳间隔（秒）
        heartbeat-interval: 30
        
        # 连接超时（秒）
        connection-timeout: 60
```

### 3. 消息类型配置

```yaml
nebula:
  notification:
    internal:
      # 消息类型配置
      types:
        # 系统消息
        system:
          enabled: true
          icon: system
          color: blue
        
        # 订单消息
        order:
          enabled: true
          icon: order
          color: green
        
        # 活动消息
        activity:
          enabled: true
          icon: activity
          color: orange
```

---

## 多环境配置

### 开发环境（application-dev.yml）

```yaml
nebula:
  notification:
    enabled: true
    
    # 开发环境使用Mock服务
    sms:
      provider: mock
      mock:
        # Mock模式下自动返回成功
        auto-success: true
        
        # Mock延迟（毫秒）
        delay: 100
    
    email:
      enabled: true
      host: smtp.mailtrap.io  # 使用Mailtrap测试
      port: 2525
      username: ${MAILTRAP_USERNAME}
      password: ${MAILTRAP_PASSWORD}
      from: dev@ticket-system.com
    
    push:
      enabled: false  # 开发环境关闭推送
    
    internal:
      enabled: true
      websocket:
        enabled: true
        endpoint: /ws/notifications
```

### 测试环境（application-test.yml）

```yaml
nebula:
  notification:
    enabled: true
    
    sms:
      provider: aliyun
      aliyun:
        access-key-id: ${TEST_ALIYUN_ACCESS_KEY_ID}
        access-key-secret: ${TEST_ALIYUN_ACCESS_KEY_SECRET}
        endpoint: dysmsapi.aliyuncs.com
        sign-name: "测试签名"
      
      # 测试环境限制发送频率
      rate-limit:
        enabled: true
        max-per-minute: 1
        max-per-hour: 3
        max-per-day: 5
    
    email:
      enabled: true
      host: smtp.example.com
      port: 465
      username: ${TEST_MAIL_USERNAME}
      password: ${TEST_MAIL_PASSWORD}
      from: test@ticket-system.com
      ssl: true
    
    push:
      enabled: true
      providers:
        - type: fcm
          enabled: true
          server-key: ${TEST_FCM_SERVER_KEY}
    
    internal:
      enabled: true
```

### 生产环境（application-prod.yml）

```yaml
nebula:
  notification:
    enabled: true
    async-enabled: true
    record-enabled: true
    
    sms:
      provider: aliyun
      aliyun:
        access-key-id: ${PROD_ALIYUN_ACCESS_KEY_ID}
        access-key-secret: ${PROD_ALIYUN_ACCESS_KEY_SECRET}
        endpoint: dysmsapi.aliyuncs.com
        sign-name: "票务系统"
        templates:
          verify-code: SMS_123456789
          order-confirm: SMS_234567890
          order-cancel: SMS_345678901
          refund-success: SMS_456789012
          show-reminder: SMS_567890123
      
      rate-limit:
        enabled: true
        max-per-minute: 1
        max-per-hour: 5
        max-per-day: 10
      
      retry:
        enabled: true
        max-attempts: 3
        backoff-interval: 1000
    
    email:
      enabled: true
      host: smtp.exmail.qq.com  # 企业邮箱
      port: 465
      username: ${PROD_MAIL_USERNAME}
      password: ${PROD_MAIL_PASSWORD}
      from: no-reply@ticket-system.com
      from-name: "票务系统"
      ssl: true
      templates-path: classpath:mail-templates/
      template-engine: thymeleaf
    
    push:
      enabled: true
      providers:
        - type: fcm
          enabled: true
          server-key: ${PROD_FCM_SERVER_KEY}
          project-id: ${PROD_FCM_PROJECT_ID}
          credentials-path: file:${PROD_FCM_CREDENTIALS_PATH}
        
        - type: apns
          enabled: true
          certificate-path: file:${PROD_APNS_CERT_PATH}
          certificate-password: ${PROD_APNS_CERT_PASSWORD}
          bundle-id: com.example.ticket
          production: true
      
      default-priority: high
      time-to-live: 86400
      batch:
        enabled: true
        size: 1000
        interval: 1000
    
    internal:
      enabled: true
      max-unread: 100
      retention-days: 90
      websocket:
        enabled: true
        endpoint: /ws/notifications
        heartbeat-interval: 30
        connection-timeout: 60
```

---

## 高级配置

### 1. 线程池配置

```yaml
nebula:
  notification:
    # 异步执行线程池配置
    async:
      # 核心线程数
      core-pool-size: 10
      
      # 最大线程数
      max-pool-size: 50
      
      # 队列容量
      queue-capacity: 1000
      
      # 线程空闲时间（秒）
      keep-alive-seconds: 60
      
      # 线程名称前缀
      thread-name-prefix: notification-
```

### 2. 消息队列配置

```yaml
nebula:
  notification:
    # 是否使用消息队列
    mq-enabled: true
    
    # 消息队列类型（rabbitmq, kafka, rocketmq）
    mq-type: rabbitmq
    
    rabbitmq:
      # 交换机
      exchange: notification.exchange
      
      # 路由键
      routing-key: notification.send
      
      # 队列名称
      queue: notification.queue
      
      # 是否持久化
      durable: true
      
      # 是否自动删除
      auto-delete: false
```

### 3. 监控配置

```yaml
nebula:
  notification:
    # 监控配置
    monitoring:
      # 是否启用监控
      enabled: true
      
      # 监控指标
      metrics:
        # 发送成功率
        success-rate: true
        
        # 发送耗时
        latency: true
        
        # 发送失败率
        failure-rate: true
      
      # 告警配置
      alert:
        # 是否启用告警
        enabled: true
        
        # 失败率阈值（百分比）
        failure-threshold: 10
        
        # 告警接收人
        recipients:
          - admin@example.com
```

### 4. 日志配置

```yaml
nebula:
  notification:
    # 日志配置
    logging:
      # 是否记录请求日志
      log-requests: true
      
      # 是否记录响应日志
      log-responses: true
      
      # 是否记录敏感信息（生产环境应设为false）
      log-sensitive-data: false
      
      # 日志级别
      level: INFO
```

---

## 票务系统配置示例

### 完整配置

```yaml
spring:
  application:
    name: ticket-system
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

nebula:
  notification:
    enabled: true
    async-enabled: true
    record-enabled: true
    
    # 短信配置
    sms:
      provider: aliyun
      aliyun:
        access-key-id: ${ALIYUN_ACCESS_KEY_ID}
        access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
        endpoint: dysmsapi.aliyuncs.com
        sign-name: "票务系统"
        templates:
          verify-code: SMS_123456789
          order-confirm: SMS_234567890
          order-cancel: SMS_345678901
          refund-success: SMS_456789012
          show-reminder: SMS_567890123
      rate-limit:
        enabled: true
        max-per-minute: 1
        max-per-hour: 5
        max-per-day: 10
    
    # 邮件配置
    email:
      enabled: true
      host: ${MAIL_HOST}
      port: ${MAIL_PORT}
      username: ${MAIL_USERNAME}
      password: ${MAIL_PASSWORD}
      from: ${MAIL_FROM}
      from-name: "票务系统"
      ssl: true
      templates-path: classpath:mail-templates/
      template-engine: thymeleaf
    
    # 推送配置
    push:
      enabled: true
      providers:
        - type: fcm
          enabled: true
          server-key: ${FCM_SERVER_KEY}
          credentials-path: ${FCM_CREDENTIALS_PATH}
        - type: apns
          enabled: true
          certificate-path: ${APNS_CERT_PATH}
          certificate-password: ${APNS_CERT_PASSWORD}
          bundle-id: com.example.ticket
          production: ${APNS_PRODUCTION:false}
      batch:
        enabled: true
        size: 1000
    
    # 站内消息配置
    internal:
      enabled: true
      max-unread: 100
      retention-days: 90
      websocket:
        enabled: true
        endpoint: /ws/notifications
    
    # 线程池配置
    async:
      core-pool-size: 10
      max-pool-size: 50
      queue-capacity: 1000
    
    # 监控配置
    monitoring:
      enabled: true
      alert:
        enabled: true
        failure-threshold: 10
```

### 环境变量

```bash
# 短信配置
export ALIYUN_ACCESS_KEY_ID=your_access_key_id
export ALIYUN_ACCESS_KEY_SECRET=your_access_key_secret

# 邮件配置
export MAIL_HOST=smtp.example.com
export MAIL_PORT=465
export MAIL_USERNAME=your_username
export MAIL_PASSWORD=your_password
export MAIL_FROM=no-reply@ticket-system.com

# 推送配置
export FCM_SERVER_KEY=your_fcm_server_key
export FCM_CREDENTIALS_PATH=/path/to/firebase-credentials.json
export APNS_CERT_PATH=/path/to/apns_cert.p12
export APNS_CERT_PASSWORD=your_apns_cert_password
export APNS_PRODUCTION=true

# 应用配置
export SPRING_PROFILES_ACTIVE=prod
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

