# Nebula Lock Core 配置指南

> 分布式锁核心抽象配置说明

## 概述

`nebula-lock-core` 提供分布式锁的核心抽象接口。

## 基本配置

```yaml
nebula:
  lock:
    core:
      # 锁类型: redis / zookeeper / database
      type: redis
```

---

**最后更新**: 2025-11-20

