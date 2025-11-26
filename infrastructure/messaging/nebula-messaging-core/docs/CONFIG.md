# Nebula Messaging Core 配置指南

> 消息队列核心抽象配置说明

## 概述

`nebula-messaging-core` 提供消息队列的核心抽象接口,定义统一的消息发送和消费API。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-messaging-core</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 消息序列化配置

```yaml
nebula:
  messaging:
    core:
      # 序列化方式: json / protobuf / kryo
      serializer: json
      # JSON序列化配置
      json:
        # 日期格式
        date-format: yyyy-MM-dd HH:mm:ss
        # 空值处理
        include-null: false
```

### 消息路由配置

```yaml
nebula:
  messaging:
    core:
      router:
        # 路由策略: direct / topic / fanout
        strategy: topic
        # 默认交换机
        default-exchange: ticket.default
```

## 相关文档

- [README](README.md) - 模块介绍
- [EXAMPLE](EXAMPLE.md) - 使用示例
- [ROADMAP](ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

