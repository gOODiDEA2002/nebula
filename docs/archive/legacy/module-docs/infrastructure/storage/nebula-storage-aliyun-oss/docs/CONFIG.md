# Nebula Storage Aliyun OSS 配置指南

> 阿里云OSS对象存储配置说明

## 概述

`nebula-storage-aliyun-oss` 提供阿里云 OSS 对象存储支持。

## 基本配置

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-storage-aliyun-oss</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

```yaml
nebula:
  storage:
    aliyun-oss:
      enabled: true
      endpoint: oss-cn-hangzhou.aliyuncs.com
      access-key-id: ${ALIYUN_ACCESS_KEY_ID}
      access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
      bucket: ticket-prod
```

---

**最后更新**: 2025-11-20

