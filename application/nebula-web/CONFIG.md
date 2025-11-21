# Nebula Web 配置指南

> Web应用模块配置说明

## 概述

`nebula-web` 提供 Web 应用的增强功能,包括统一异常处理、限流、缓存、性能监控等。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-web</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### Web配置

```yaml
nebula:
  web:
    enabled: true
    # 跨域配置
    cors:
      enabled: true
      allowed-origins: "*"
      allowed-methods: GET,POST,PUT,DELETE
      allowed-headers: "*"
```

## 票务系统场景

### 限流配置

```yaml
nebula:
  web:
    rate-limit:
      enabled: true
      # 全局限流: 每秒100个请求
      global-limit: 100
      # 接口级限流
      endpoints:
        - path: /api/orders
          limit: 50
        - path: /api/showtimes/*
          limit: 200
```

### 性能监控

```yaml
nebula:
  web:
    performance:
      enabled: true
      # 慢接口阈值(毫秒)
      slow-threshold: 1000
      # 是否记录请求参数
      log-request: true
      # 是否记录响应结果
      log-response: false
```

---

**最后更新**: 2025-11-20

