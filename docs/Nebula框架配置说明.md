# Nebula 框架配置说明

本文档详细说明 Nebula 框架 2.0.x 版本各模块的配置项，帮助开发者正确配置应用。

## 目录

1. [配置概述](#配置概述)
2. [数据持久化配置](#数据持久化配置)
3. [缓存配置](#缓存配置)
4. [消息队列配置](#消息队列配置)
5. [服务发现配置](#服务发现配置)
6. [RPC配置](#rpc配置)
7. [分布式锁配置](#分布式锁配置)
8. [对象存储配置](#对象存储配置)
9. [任务调度配置](#任务调度配置)
10. [Web配置](#web配置)
11. [安全配置](#安全配置)
12. [搜索配置](#搜索配置)
13. [AI配置](#ai配置)
14. [爬虫配置](#爬虫配置)

---

## 配置概述

### 配置前缀规范

Nebula 框架所有配置统一使用 `nebula.*` 前缀，与 Spring Boot 原生配置分开管理：

| 模块 | 配置前缀 |
|------|---------|
| 数据持久化 | `nebula.data.persistence.*` |
| 缓存 | `nebula.data.cache.*` |
| 消息队列 | `nebula.messaging.rabbitmq.*` |
| 服务发现 | `nebula.discovery.nacos.*` |
| RPC | `nebula.rpc.*` |
| 分布式锁 | `nebula.lock.*` |
| 对象存储 | `nebula.storage.minio.*` / `nebula.storage.aliyun.oss.*` |
| 任务调度 | `nebula.task.*` |
| Web | `nebula.web.*` |
| 安全 | `nebula.security.*` |
| 搜索 | `nebula.search.elasticsearch.*` |
| AI | `nebula.ai.*` |
| 爬虫 | `nebula.crawler.*` |

### 模块启用机制

每个模块都有 `enabled` 属性控制是否启用：

```yaml
nebula:
  data:
    persistence:
      enabled: true  # 启用数据持久化
    cache:
      enabled: true  # 启用缓存
  messaging:
    rabbitmq:
      enabled: true  # 启用消息队列
```

---

## 数据持久化配置

配置前缀: `nebula.data.persistence`

### 基础配置

```yaml
nebula:
  data:
    persistence:
      enabled: true          # 是否启用数据持久化（必须设为true才能生效）
      primary: primary       # 主数据源名称
      sources:               # 数据源列表
        primary:             # 数据源名称（与primary对应）
          type: mysql        # 数据库类型: mysql, postgresql, oracle, sqlserver
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/mydb?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
          username: root
          password: password
          validation-query: SELECT 1
          pool:              # 连接池配置
            min-size: 5              # 最小连接数
            max-size: 20             # 最大连接数
            connection-timeout: 30s  # 连接超时时间
            idle-timeout: 10m        # 空闲连接超时
            max-lifetime: 30m        # 连接最大生命周期
            validation-timeout: 5s   # 验证超时时间
```

### MyBatis-Plus 配置

```yaml
nebula:
  data:
    persistence:
      mybatis-plus:
        mapper-locations: classpath*:/mapper/**/*.xml  # Mapper XML文件位置
        type-aliases-package: com.example.entity       # 实体类别名包
        map-underscore-to-camel-case: true            # 下划线转驼峰
        log-impl: slf4j                                # 日志实现: slf4j, stdout, no
        global-config:
          db-config:
            id-type: auto                   # 主键策略: auto, none, input, assign_id, assign_uuid
            logic-delete-field: deleted     # 逻辑删除字段
            logic-delete-value: 1           # 逻辑删除值
            logic-not-delete-value: 0       # 逻辑未删除值
            table-underline: true           # 表名使用下划线
```

### 多数据源配置

```yaml
nebula:
  data:
    persistence:
      enabled: true
      primary: master
      sources:
        master:
          url: jdbc:mysql://master:3306/mydb
          username: root
          password: password
        slave:
          url: jdbc:mysql://slave:3306/mydb
          username: reader
          password: password
```

---

## 缓存配置

配置前缀: `nebula.data.cache`

### 本地缓存配置

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: local            # 缓存类型: local, redis, multi-level
      default-ttl: 1h        # 默认过期时间
      local:
        enabled: true
        max-size: 10000                # 最大缓存条目
        initial-capacity: 1000         # 初始容量
        expire-after-write: 30m        # 写入后过期时间
        expire-after-access: 1h        # 访问后过期时间
        cleanup-interval: 5m           # 清理间隔
        eviction-policy: LRU           # 驱逐策略: LRU, LFU, FIFO
        stats-enabled: true            # 是否启用统计
```

### Redis 缓存配置

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: redis
      default-ttl: 1h
      redis:
        enabled: true
        host: localhost
        port: 6379
        password: your-password
        database: 0
        timeout: 2s
        key-prefix: "myapp:cache:"
        serialization: JSON            # 序列化类型: JSON, JDK, KRYO
        pool:
          max-active: 8                # 最大活跃连接
          max-idle: 8                  # 最大空闲连接
          min-idle: 0                  # 最小空闲连接
          max-wait: -1ms               # 最大等待时间
          connect-timeout: 2s          # 连接超时
```

### 多级缓存配置

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: multi-level
      default-ttl: 1h
      multi-level:
        enabled: true
        local-cache-enabled: true       # 启用L1本地缓存
        remote-cache-enabled: true      # 启用L2远程缓存
        sync-on-update: true            # 更新时同步
        l1-write-back-enabled: true     # L1回写启用
        l1-default-ttl: 10m             # L1默认TTL
        l1-write-back-ttl: 5m           # L1回写TTL
        l1-ttl-ratio: 0.5               # L1 TTL比例
        l1-max-size: 10000              # L1最大条目数
```

---

## 消息队列配置

配置前缀: `nebula.messaging.rabbitmq`

### 基础配置

```yaml
nebula:
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      username: guest
      password: guest
      virtual-host: /
      connection-timeout: 60000        # 连接超时(ms)
      heartbeat: 60                    # 心跳间隔(秒)
      automatic-recovery: true         # 自动恢复
      network-recovery-interval: 5000  # 网络恢复间隔(ms)
```

### 消费者配置

```yaml
nebula:
  messaging:
    rabbitmq:
      consumer:
        prefetch-count: 1              # 预取数量
        auto-ack: false                # 自动确认
        retry-count: 3                 # 重试次数
        retry-interval: 1000           # 重试间隔(ms)
```

### 生产者配置

```yaml
nebula:
  messaging:
    rabbitmq:
      producer:
        publisher-confirms: true       # 发送确认
        confirm-timeout: 5000          # 确认超时(ms)
        publisher-returns: true        # 发送回调
```

### Exchange 配置

```yaml
nebula:
  messaging:
    rabbitmq:
      exchange:
        default-type: topic            # 默认类型: direct, topic, fanout, headers
        durable: true                  # 持久化
        auto-delete: false             # 自动删除
```

### 延时消息配置

```yaml
nebula:
  messaging:
    rabbitmq:
      delay-message:
        enabled: true
        default-max-retries: 3
        default-retry-interval: 1000
        max-delay-millis: 604800000    # 最大延时(7天)
        min-delay-millis: 1000         # 最小延时(1秒)
        auto-create-resources: true    # 自动创建资源
        enable-dead-letter-queue: true # 启用死信队列
        dead-letter-exchange: nebula.dlx.exchange
        dead-letter-queue: nebula.dlx.queue
```

---

## 服务发现配置

配置前缀: `nebula.discovery.nacos`

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848      # Nacos服务器地址
      namespace: ""                     # 命名空间ID
      group-name: DEFAULT_GROUP         # 分组名称
      cluster-name: DEFAULT             # 集群名称
      username: nacos                   # 用户名
      password: nacos                   # 密码
      auto-register: true               # 自动注册服务
      ip: ""                            # 指定注册IP（不填则自动检测）
      weight: 1.0                       # 服务权重
      healthy: true                     # 是否健康
      instance-enabled: true            # 实例是否启用
      heartbeat-interval: 5000          # 心跳间隔(ms)
      heartbeat-timeout: 15000          # 心跳超时(ms)
      ip-delete-timeout: 30000          # IP删除超时(ms)
      preferred-networks:               # 首选网络地址（用于多网卡环境）
        - 192.168.2
        - 10.0
      ignored-interfaces:               # 忽略的网络接口
        - docker0
        - veth
      metadata:                         # 元数据
        httpPort: ${server.port}
```

---

## RPC配置

### HTTP RPC 配置

配置前缀: `nebula.rpc.http`

```yaml
nebula:
  rpc:
    http:
      enabled: true
      server:
        enabled: true
      client:
        enabled: true
        connect-timeout: 30000         # 连接超时(ms)
        read-timeout: 60000            # 读取超时(ms)
```

### gRPC 配置

配置前缀: `nebula.rpc.grpc`

```yaml
nebula:
  rpc:
    grpc:
      enabled: true
      server:
        enabled: true
```

### RPC 服务发现配置

配置前缀: `nebula.rpc.discovery`

```yaml
nebula:
  rpc:
    discovery:
      enabled: true
      load-balance-strategy: ROUND_ROBIN  # 负载均衡策略: ROUND_ROBIN, RANDOM, WEIGHT
```

---

## 分布式锁配置

配置前缀: `nebula.lock`

```yaml
nebula:
  lock:
    enabled: true
    enable-aspect: true                # 启用@Locked注解切面
    default-wait-time: 30s             # 默认等待锁超时时间
    default-lease-time: 60s            # 默认锁租约时间
    enable-watchdog: true              # 启用看门狗自动续期
    watchdog-interval: 20s             # 看门狗续期间隔（默认租约时间的1/3）
    fair: false                        # 是否使用公平锁
    redlock:                           # Redlock配置（多Redis实例）
      enabled: false
      addresses:
        - redis://host1:6379
        - redis://host2:6379
        - redis://host3:6379
      quorum: 2                        # 最小获取锁的实例数
```

---

## 对象存储配置

### MinIO 配置

配置前缀: `nebula.storage.minio`

```yaml
nebula:
  storage:
    minio:
      enabled: true
      endpoint: http://localhost:9000     # MinIO服务端点
      domain: ""                           # 公开访问域名（可选，用于生成访问URL）
      access-key: minioadmin               # 访问密钥
      secret-key: minioadmin               # 秘密密钥
      default-bucket: default              # 默认存储桶
      secure: false                        # 是否使用SSL
      region: ""                           # 区域
      auto-create-default-bucket: true     # 自动创建默认存储桶
      connect-timeout: 10000               # 连接超时(ms)
      write-timeout: 10000                 # 写超时(ms)
      read-timeout: 10000                  # 读超时(ms)
      default-expiry: 3600                 # 预签名URL默认过期时间(秒)
      max-file-size: 104857600             # 最大文件大小(100MB)
      allowed-content-types:               # 允许的文件类型
        - image/jpeg
        - image/png
        - application/pdf
```

### 阿里云 OSS 配置

配置前缀: `nebula.storage.aliyun.oss`

```yaml
nebula:
  storage:
    aliyun:
      oss:
        enabled: true
        endpoint: https://oss-cn-hangzhou.aliyuncs.com
        access-key-id: ${ALIYUN_ACCESS_KEY}
        access-key-secret: ${ALIYUN_SECRET_KEY}
        default-bucket: my-bucket
```

---

## 任务调度配置

配置前缀: `nebula.task`

### 基础配置

```yaml
nebula:
  task:
    enabled: true
    executor:
      core-pool-size: 10               # 核心线程数
      max-pool-size: 200               # 最大线程数
      keep-alive-seconds: 60           # 线程空闲时间(秒)
      queue-capacity: 1000             # 队列容量
      thread-name-prefix: nebula-task- # 线程名前缀
```

### XXL-JOB 配置

配置前缀: `nebula.task.xxl-job`

```yaml
nebula:
  task:
    xxl-job:
      enabled: true
      admin-addresses: http://localhost:8080/xxl-job-admin  # 管理端地址
      executor-name: my-executor       # 执行器名称
      executor-ip: ""                  # 执行器IP（不填则自动检测）
      executor-port: 9999              # 执行器端口
      log-path: ./logs/xxl-job         # 日志路径
      log-retention-days: 30           # 日志保留天数
      access-token: xxl-job            # 访问令牌
      heartbeat-interval: 30           # 心跳间隔(秒)
      registry-retry-count: 3          # 注册重试次数
      registry-timeout: 10             # 注册超时(秒)
```

---

## Web配置

配置前缀: `nebula.web`

```yaml
nebula:
  web:
    enabled: true
    response:
      enabled: true                    # 统一响应封装
    auth:
      enabled: true                    # 启用认证
      jwt-secret: your-secret-key      # JWT密钥（至少32字节）
      jwt-expiration: 86400            # Token过期时间(秒)
      ignore-paths:                    # 忽略认证的路径
        - /health/**
        - /actuator/**
        - /api/v1/login
    rate-limit:
      enabled: true                    # 启用限流
      default-qps: 100                 # 默认QPS
    performance:
      enabled: true                    # 启用性能监控
      slow-request-threshold: 1000     # 慢请求阈值(ms)
```

---

## 安全配置

配置前缀: `nebula.security`

```yaml
nebula:
  security:
    enabled: true
    jwt:
      enabled: true
      secret: your-secret-key-at-least-32-bytes
      expiration: 24h                  # 访问Token过期时间
      refresh-expiration: 7d           # 刷新Token过期时间
```

---

## 搜索配置

配置前缀: `nebula.search.elasticsearch`

```yaml
nebula:
  search:
    elasticsearch:
      enabled: true
      uris:
        - http://localhost:9200
      username: elastic
      password: password
      connection-timeout: 5s
      read-timeout: 30s
```

---

## AI配置

配置前缀: `nebula.ai`

```yaml
nebula:
  ai:
    enabled: true
    chat:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: gpt-3.5-turbo
          options:
            temperature: 0.7
            max-tokens: 1000
    embedding:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: text-embedding-ada-002
    vector-store:
      default-provider: chroma
      providers:
        chroma:
          host: localhost
          port: 8000
          collection-name: my-docs
```

---

## 爬虫配置

配置前缀: `nebula.crawler`

### 基础配置

```yaml
nebula:
  crawler:
    enabled: true
```

### 代理池配置

配置前缀: `nebula.crawler.proxy`

```yaml
nebula:
  crawler:
    proxy:
      enabled: true
      min-available: 10                # 最小可用代理数
      check-url: https://www.baidu.com # 代理检测URL
      check-timeout: 5000              # 检测超时(ms)
      check-interval: 300000           # 检测间隔(ms)
      max-fail-count: 3                # 最大失败次数
      blacklist-expire-hours: 24       # 黑名单过期时间(小时)
      static-proxies:                  # 静态代理列表
        - http://proxy1:8080
        - http://proxy2:8080
      api-sources:                     # API代理源
        - name: provider1
          url: http://api.provider.com/get
          format: json
          priority: 100
```

### 验证码配置

配置前缀: `nebula.crawler.captcha`

```yaml
nebula:
  crawler:
    captcha:
      enabled: true
      local-ocr-enabled: true          # 启用本地OCR
      ocr-engine: ddddocr              # OCR引擎: tesseract, ddddocr
      ddddocr-urls:                    # ddddocr服务地址列表(支持负载均衡)
        - http://localhost:8866
      opencv-urls:                     # OpenCV服务地址列表(支持负载均衡)
        - http://localhost:8867
      local-slider-enabled: true       # 启用本地滑块检测
      local-rotate-enabled: true       # 启用本地旋转检测
      local-click-enabled: true        # 启用本地点击检测
      min-length: 4                    # 验证码最小长度
      max-length: 6                    # 验证码最大长度
      default-timeout: 60000           # 默认超时(ms)
      providers:                       # 第三方平台
        - name: provider1
          api-key: xxx
          enabled: true
          priority: 1
```

### HTTP爬虫配置

配置前缀: `nebula.crawler.http`

```yaml
nebula:
  crawler:
    http:
      enabled: true
      connect-timeout: 30000           # 连接超时(ms)
      read-timeout: 60000              # 读取超时(ms)
      write-timeout: 60000             # 写入超时(ms)
      max-connections: 200             # 最大连接数
      max-connections-per-host: 20     # 每主机最大连接数
      keep-alive-time: 300000          # 连接保活时间(ms)
      retry-count: 3                   # 默认重试次数
      retry-interval: 1000             # 重试间隔(ms)
      use-proxy: false                 # 是否使用代理
      default-qps: 5.0                 # 默认QPS限制
      follow-redirects: true           # 是否跟随重定向
      trust-all-certs: false           # 是否信任所有证书(仅测试环境)
      user-agents:                     # User-Agent池(随机轮换)
        - "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0"
        - "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0"
```

### 浏览器爬虫配置

配置前缀: `nebula.crawler.browser`

支持两种运行模式：LOCAL(本地启动浏览器) 和 REMOTE(连接远程 Playwright Server)

```yaml
nebula:
  crawler:
    browser:
      enabled: true
      mode: REMOTE                     # 运行模式: LOCAL 或 REMOTE
      browser-type: chromium           # 浏览器类型: chromium, firefox, webkit
      headless: true                   # 无头模式(仅LOCAL模式)
      pool-size: 5                     # 浏览器上下文池大小
      page-timeout: 30000              # 页面超时(ms)
      navigation-timeout: 30000        # 导航超时(ms)
      connect-timeout: 30000           # 连接超时(ms)(仅REMOTE模式)
      screenshot-on-error: true        # 错误时截图
      use-proxy: false                 # 是否使用代理
      viewport-width: 1920             # 视口宽度
      viewport-height: 1080            # 视口高度
      disable-images: false            # 禁用图片加载
      disable-css: false               # 禁用CSS加载
      slow-mo: 0                       # 慢速模式延迟(ms)(仅LOCAL模式)
      # 远程配置(REMOTE模式)
      remote:
        endpoints:                     # Playwright Server端点列表
          - ws://localhost:9222
        load-balance-strategy: ROUND_ROBIN  # 负载均衡: ROUND_ROBIN, RANDOM, LEAST_CONNECTIONS
        health-check-interval: 30000   # 健康检查间隔(ms)
        max-retries: 3                 # 连接失败重试次数
        retry-interval: 1000           # 重试间隔(ms)
```

#### 浏览器运行模式说明

| 模式 | 适用场景 | 说明 |
|------|---------|------|
| `LOCAL` | 开发环境、单机部署 | 在本地启动浏览器实例 |
| `REMOTE` | Docker/K8s部署 | 连接远程 Playwright Server，支持多端点负载均衡 |

---

## 完整配置示例

以下是一个完整的配置示例，展示如何组织 Nebula 框架的配置：

### application.yml (通用配置)

```yaml
spring:
  application:
    name: my-service
  profiles:
    active: dev

server:
  port: 8080

nebula:
  data:
    persistence:
      enabled: true
      primary: primary
      mybatis-plus:
        mapper-locations: classpath*:/mapper/**/*.xml
        type-aliases-package: com.example.entity
    cache:
      enabled: true
      type: redis
  
  web:
    enabled: true
    response:
      enabled: true
  
  task:
    enabled: true
```

### application-dev.yml (开发环境配置)

```yaml
nebula:
  data:
    persistence:
      sources:
        primary:
          url: jdbc:mysql://localhost:3306/mydb
          username: root
          password: password
    cache:
      redis:
        host: localhost
        port: 6379
  
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      username: guest
      password: guest
  
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  
  storage:
    minio:
      enabled: true
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
```

---

## 配置迁移指南

从 Spring Boot 原生配置迁移到 Nebula 框架配置的对照表：

| 原配置 | Nebula 配置 |
|--------|-------------|
| `spring.datasource.*` | `nebula.data.persistence.sources.*` |
| `spring.data.redis.*` | `nebula.data.cache.redis.*` |
| `spring.rabbitmq.*` | `nebula.messaging.rabbitmq.*` |
| `spring.cloud.nacos.discovery.*` | `nebula.discovery.nacos.*` |
| `mybatis-plus.*` | `nebula.data.persistence.mybatis-plus.*` |

---

## 注意事项

1. **启用开关**: 每个模块都需要显式设置 `enabled: true` 才能生效
2. **配置顺序**: 建议按照模块依赖顺序配置（服务发现 -> RPC -> 数据 -> 缓存 -> 消息）
3. **敏感信息**: 生产环境敏感配置应使用环境变量: `${ENV_VAR:default}`
4. **时间格式**: 支持 Duration 格式: `30s`, `5m`, `1h`, `1d`
5. **连接池**: 根据实际负载调整连接池参数，避免资源浪费

---

*文档版本: 2.0.1*
*更新日期: 2026-01-12*
