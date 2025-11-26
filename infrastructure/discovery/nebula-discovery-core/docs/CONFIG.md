# Nebula Discovery Core 配置指南

> 服务发现核心抽象配置说明

## 概述

`nebula-discovery-core` 提供服务发现的核心抽象接口。

## 基本配置

```yaml
nebula:
  discovery:
    core:
      # 服务发现类型: nacos / consul / eureka
      type: nacos
      # 是否启用健康检查
      health-check-enabled: true
```

---

**最后更新**: 2025-11-20

