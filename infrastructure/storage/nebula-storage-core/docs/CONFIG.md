# Nebula Storage Core 配置指南

> 对象存储核心抽象配置说明

## 概述

`nebula-storage-core` 提供对象存储的核心抽象接口。

## 基本配置

```yaml
nebula:
  storage:
    core:
      # 存储类型: minio / aliyun-oss / aws-s3
      type: minio
      # 默认bucket
      default-bucket: default
```

---

**最后更新**: 2025-11-20

