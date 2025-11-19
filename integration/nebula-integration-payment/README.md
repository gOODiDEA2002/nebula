# Nebula Integration Payment 模块

## 模块简介

`nebula-integration-payment` 是 Nebula 框架的支付集成模块，提供统一的支付接口，支持支付宝（Alipay）和微信支付（WeChat Pay）等多种支付渠道。

该模块旨在简化支付对接流程，提供支付、退款、查询、回调处理等标准功能。

## 核心特性

- **统一接口**：`PaymentService` 屏蔽了不同支付渠道的 API 差异。
- **多渠道支持**：内置支付宝、微信支付支持。
- **支付流程管理**：支持统一下单、支付回调、退款申请、退款查询。
- **配置灵活**：通过 YAML 配置即可启用和切换支付渠道。

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-integration-payment</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 2. 配置支付参数

**`application.yml`**:

```yaml
nebula:
  payment:
    enabled: true
    # 支付宝配置
    alipay:
      app-id: your-app-id
      private-key: your-private-key
      alipay-public-key: alipay-public-key
      notify-url: http://your-domain/api/payment/alipay/notify
      return-url: http://your-domain/api/payment/alipay/return
      sandbox: true # 是否开启沙箱环境
      
    # 微信支付配置
    wechat:
      app-id: your-wx-app-id
      mch-id: your-mch-id
      api-v3-key: your-api-v3-key
      private-key-path: classpath:cert/apiclient_key.pem
      certificate-path: classpath:cert/apiclient_cert.pem
      notify-url: http://your-domain/api/payment/wechat/notify
```

### 3. 使用示例

详见 [EXAMPLE.md](./EXAMPLE.md)。

## 依赖说明

- JDK 21+
- Spring Boot 3.x
- Alipay SDK
- WeChat Pay SDK

