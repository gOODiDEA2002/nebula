# Nebula RPC Core 配置指南

> RPC核心抽象配置说明

## 概述

`nebula-rpc-core` 提供 RPC 的核心抽象接口。

## 基本配置

```yaml
nebula:
  rpc:
    core:
      # RPC协议: http / grpc
      protocol: grpc
      # 序列化方式
      serializer: protobuf
```

---

**最后更新**: 2025-11-20

