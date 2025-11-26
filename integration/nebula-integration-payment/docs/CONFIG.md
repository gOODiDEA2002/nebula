# Nebula Integration Payment - 配置示例

> 支付集成模块的详细配置指南

## 目录

- [基础配置](#基础配置)
- [支付宝配置](#支付宝配置)
- [微信支付配置](#微信支付配置)
- [多环境配置](#多环境配置)
- [高级配置](#高级配置)
- [安全配置](#安全配置)

---

## 基础配置

### 最小配置

```yaml
nebula:
  payment:
    enabled: true
```

### 完整基础配置

```yaml
nebula:
  payment:
    # 是否启用支付模块
    enabled: true
    
    # 默认支付方式
    default-payment-type: ALIPAY_PAGE
    
    # 默认超时时间（分钟）
    default-timeout: 30
    
    # 是否启用支付记录
    record-enabled: true
    
    # 是否启用签名验证
    signature-verification: true
```

---

## 支付宝配置

### 1. 电脑网站支付

```yaml
nebula:
  payment:
    alipay:
      enabled: true
      
      # 应用ID
      app-id: ${ALIPAY_APP_ID}
      
      # 应用私钥（PKCS8格式）
      private-key: ${ALIPAY_PRIVATE_KEY}
      
      # 支付宝公钥
      public-key: ${ALIPAY_PUBLIC_KEY}
      
      # 支付宝网关
      gateway: https://openapi.alipay.com/gateway.do
      
      # 签名类型（RSA2推荐）
      sign-type: RSA2
      
      # 字符编码
      charset: UTF-8
      
      # 数据格式
      format: json
      
      # 异步通知地址
      notify-url: https://your-domain.com/api/payment/alipay/notify
      
      # 同步返回地址
      return-url: https://your-domain.com/payment/success
```

### 2. 手机网站支付

```yaml
nebula:
  payment:
    alipay:
      enabled: true
      app-id: ${ALIPAY_APP_ID}
      private-key: ${ALIPAY_PRIVATE_KEY}
      public-key: ${ALIPAY_PUBLIC_KEY}
      gateway: https://openapi.alipay.com/gateway.do
      
      # 手机网站支付配置
      wap:
        quit-url: https://your-domain.com  # 用户付款中途退出返回地址
        
      notify-url: https://your-domain.com/api/payment/alipay/notify
      return-url: https://your-domain.com/payment/success
```

### 3. APP支付

```yaml
nebula:
  payment:
    alipay:
      enabled: true
      app-id: ${ALIPAY_APP_ID}
      private-key: ${ALIPAY_PRIVATE_KEY}
      public-key: ${ALIPAY_PUBLIC_KEY}
      gateway: https://openapi.alipay.com/gateway.do
      
      # APP支付配置
      app:
        # iOS应用scheme
        ios-scheme: your-app-scheme://
        
        # Android应用包名
        android-package: com.yourcompany.app
      
      notify-url: https://your-domain.com/api/payment/alipay/notify
```

### 4. 扫码支付

```yaml
nebula:
  payment:
    alipay:
      enabled: true
      app-id: ${ALIPAY_APP_ID}
      private-key: ${ALIPAY_PRIVATE_KEY}
      public-key: ${ALIPAY_PUBLIC_KEY}
      gateway: https://openapi.alipay.com/gateway.do
      
      # 扫码支付配置
      qr:
        # 二维码有效时间（分钟）
        qr-code-timeout: 30
      
      notify-url: https://your-domain.com/api/payment/alipay/notify
```

---

## 微信支付配置

### 1. Native扫码支付

```yaml
nebula:
  payment:
    wechat:
      enabled: true
      
      # 应用ID（公众号、小程序、APP等）
      app-id: ${WECHAT_APP_ID}
      
      # 商户号
      mch-id: ${WECHAT_MCH_ID}
      
      # API密钥
      api-key: ${WECHAT_API_KEY}
      
      # API证书路径（用于退款等操作）
      cert-path: classpath:certs/apiclient_cert.p12
      
      # API证书密码（默认为商户号）
      cert-password: ${WECHAT_MCH_ID}
      
      # 异步通知地址
      notify-url: https://your-domain.com/api/payment/wechat/notify
```

### 2. JSAPI支付（公众号/小程序）

```yaml
nebula:
  payment:
    wechat:
      enabled: true
      app-id: ${WECHAT_APP_ID}
      mch-id: ${WECHAT_MCH_ID}
      api-key: ${WECHAT_API_KEY}
      
      # JSAPI支付配置
      jsapi:
        # 公众号APPID
        mp-app-id: ${WECHAT_MP_APP_ID}
        
        # 小程序APPID
        mini-app-id: ${WECHAT_MINI_APP_ID}
      
      notify-url: https://your-domain.com/api/payment/wechat/notify
```

### 3. APP支付

```yaml
nebula:
  payment:
    wechat:
      enabled: true
      
      # APP应用ID
      app-id: ${WECHAT_APP_ID}
      
      mch-id: ${WECHAT_MCH_ID}
      api-key: ${WECHAT_API_KEY}
      cert-path: classpath:certs/apiclient_cert.p12
      
      notify-url: https://your-domain.com/api/payment/wechat/notify
```

### 4. H5支付

```yaml
nebula:
  payment:
    wechat:
      enabled: true
      app-id: ${WECHAT_APP_ID}
      mch-id: ${WECHAT_MCH_ID}
      api-key: ${WECHAT_API_KEY}
      
      # H5支付配置
      h5:
        # 场景信息
        scene-info:
          h5_info:
            type: Wap
            wap_url: https://your-domain.com
            wap_name: "票务系统"
      
      notify-url: https://your-domain.com/api/payment/wechat/notify
```

---

## 多环境配置

### 开发环境（application-dev.yml）

```yaml
nebula:
  payment:
    enabled: true
    
    # 使用支付宝沙箱环境
    alipay:
      enabled: true
      app-id: ${DEV_ALIPAY_APP_ID}
      private-key: ${DEV_ALIPAY_PRIVATE_KEY}
      public-key: ${DEV_ALIPAY_PUBLIC_KEY}
      gateway: https://openapi.alipaydev.com/gateway.do  # 沙箱网关
      notify-url: http://localhost:8080/api/payment/alipay/notify
      return-url: http://localhost:8080/payment/success
    
    # 微信支付测试环境
    wechat:
      enabled: true
      app-id: ${DEV_WECHAT_APP_ID}
      mch-id: ${DEV_WECHAT_MCH_ID}
      api-key: ${DEV_WECHAT_API_KEY}
      notify-url: http://localhost:8080/api/payment/wechat/notify
      
      # 开发环境可以使用Mock支付
      mock-enabled: true
```

### 测试环境（application-test.yml）

```yaml
nebula:
  payment:
    enabled: true
    
    alipay:
      enabled: true
      app-id: ${TEST_ALIPAY_APP_ID}
      private-key: ${TEST_ALIPAY_PRIVATE_KEY}
      public-key: ${TEST_ALIPAY_PUBLIC_KEY}
      gateway: https://openapi.alipaydev.com/gateway.do  # 继续使用沙箱
      notify-url: https://test.your-domain.com/api/payment/alipay/notify
      return-url: https://test.your-domain.com/payment/success
    
    wechat:
      enabled: true
      app-id: ${TEST_WECHAT_APP_ID}
      mch-id: ${TEST_WECHAT_MCH_ID}
      api-key: ${TEST_WECHAT_API_KEY}
      notify-url: https://test.your-domain.com/api/payment/wechat/notify
```

### 生产环境（application-prod.yml）

```yaml
nebula:
  payment:
    enabled: true
    
    alipay:
      enabled: true
      app-id: ${PROD_ALIPAY_APP_ID}
      private-key: ${PROD_ALIPAY_PRIVATE_KEY}
      public-key: ${PROD_ALIPAY_PUBLIC_KEY}
      gateway: https://openapi.alipay.com/gateway.do  # 生产网关
      notify-url: https://api.your-domain.com/api/payment/alipay/notify
      return-url: https://www.your-domain.com/payment/success
      
      # 生产环境启用签名验证
      signature-verification: true
    
    wechat:
      enabled: true
      app-id: ${PROD_WECHAT_APP_ID}
      mch-id: ${PROD_WECHAT_MCH_ID}
      api-key: ${PROD_WECHAT_API_KEY}
      cert-path: file:/path/to/secure/certs/apiclient_cert.p12
      notify-url: https://api.your-domain.com/api/payment/wechat/notify
      
      # 生产环境启用SSL证书验证
      ssl-verification: true
```

---

## 高级配置

### 1. 连接池配置

```yaml
nebula:
  payment:
    # HTTP客户端配置
    http-client:
      # 连接超时（毫秒）
      connect-timeout: 5000
      
      # 读取超时（毫秒）
      read-timeout: 10000
      
      # 写入超时（毫秒）
      write-timeout: 10000
      
      # 连接池最大连接数
      max-connections: 100
      
      # 每个路由的最大连接数
      max-connections-per-route: 20
```

### 2. 重试配置

```yaml
nebula:
  payment:
    # 重试配置
    retry:
      # 是否启用重试
      enabled: true
      
      # 最大重试次数
      max-attempts: 3
      
      # 重试间隔（毫秒）
      backoff-interval: 1000
      
      # 重试策略（fixed, exponential）
      backoff-strategy: exponential
```

### 3. 日志配置

```yaml
nebula:
  payment:
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

### 4. 缓存配置

```yaml
nebula:
  payment:
    # 缓存配置
    cache:
      # 是否启用缓存
      enabled: true
      
      # 支付订单缓存时间（秒）
      order-ttl: 1800
      
      # 退款记录缓存时间（秒）
      refund-ttl: 3600
```

### 5. 限流配置

```yaml
nebula:
  payment:
    # 限流配置
    rate-limit:
      # 是否启用限流
      enabled: true
      
      # 每秒最大请求数
      requests-per-second: 100
      
      # 每个IP每秒最大请求数
      requests-per-ip-per-second: 10
```

---

## 安全配置

### 1. 签名验证

```yaml
nebula:
  payment:
    security:
      # 是否启用签名验证
      signature-verification: true
      
      # 签名算法
      signature-algorithm: RSA2
      
      # 是否验证回调来源IP
      verify-callback-ip: true
      
      # 允许的回调IP列表（支付宝/微信的服务器IP）
      allowed-callback-ips:
        - 110.75.36.0/24
        - 110.75.37.0/24
```

### 2. 证书管理

```yaml
nebula:
  payment:
    security:
      # 证书存储路径
      cert-store-path: /secure/certs
      
      # 证书加密密码
      cert-password: ${CERT_PASSWORD}
      
      # 证书有效期检查
      cert-expiry-check: true
```

### 3. 数据加密

```yaml
nebula:
  payment:
    security:
      # 敏感数据加密
      encrypt-sensitive-data: true
      
      # 加密算法
      encryption-algorithm: AES
      
      # 加密密钥
      encryption-key: ${ENCRYPTION_KEY}
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
  payment:
    enabled: true
    default-timeout: 30
    record-enabled: true
    
    # 支付宝配置
    alipay:
      enabled: true
      app-id: ${ALIPAY_APP_ID}
      private-key: ${ALIPAY_PRIVATE_KEY}
      public-key: ${ALIPAY_PUBLIC_KEY}
      gateway: ${ALIPAY_GATEWAY:https://openapi.alipay.com/gateway.do}
      sign-type: RSA2
      charset: UTF-8
      format: json
      notify-url: ${APP_URL}/api/payment/alipay/notify
      return-url: ${APP_URL}/payment/success
    
    # 微信支付配置
    wechat:
      enabled: true
      app-id: ${WECHAT_APP_ID}
      mch-id: ${WECHAT_MCH_ID}
      api-key: ${WECHAT_API_KEY}
      cert-path: file:${WECHAT_CERT_PATH}
      notify-url: ${APP_URL}/api/payment/wechat/notify
    
    # HTTP客户端配置
    http-client:
      connect-timeout: 5000
      read-timeout: 10000
      max-connections: 100
    
    # 重试配置
    retry:
      enabled: true
      max-attempts: 3
      backoff-interval: 1000
      backoff-strategy: exponential
    
    # 日志配置
    logging:
      log-requests: true
      log-responses: true
      log-sensitive-data: false
      level: INFO
    
    # 安全配置
    security:
      signature-verification: true
      verify-callback-ip: true
```

### 环境变量

```bash
# 支付宝配置
export ALIPAY_APP_ID=your_alipay_app_id
export ALIPAY_PRIVATE_KEY=your_alipay_private_key
export ALIPAY_PUBLIC_KEY=your_alipay_public_key

# 微信支付配置
export WECHAT_APP_ID=your_wechat_app_id
export WECHAT_MCH_ID=your_wechat_mch_id
export WECHAT_API_KEY=your_wechat_api_key
export WECHAT_CERT_PATH=/path/to/apiclient_cert.p12

# 应用配置
export APP_URL=https://api.your-domain.com
export SPRING_PROFILES_ACTIVE=prod
```

---

## 配置验证

### 启动时验证配置

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConfigValidator {
    
    private final PaymentProperties paymentProperties;
    
    @PostConstruct
    public void validate() {
        log.info("开始验证支付配置");
        
        if (paymentProperties.isEnabled()) {
            // 验证支付宝配置
            if (paymentProperties.getAlipay().isEnabled()) {
                validateAlipayConfig();
            }
            
            // 验证微信支付配置
            if (paymentProperties.getWechat().isEnabled()) {
                validateWechatConfig();
            }
        }
        
        log.info("支付配置验证完成");
    }
    
    private void validateAlipayConfig() {
        AlipayProperties alipay = paymentProperties.getAlipay();
        
        Assert.hasText(alipay.getAppId(), "支付宝App ID不能为空");
        Assert.hasText(alipay.getPrivateKey(), "支付宝私钥不能为空");
        Assert.hasText(alipay.getPublicKey(), "支付宝公钥不能为空");
        Assert.hasText(alipay.getNotifyUrl(), "支付宝回调地址不能为空");
        
        log.info("支付宝配置验证通过");
    }
    
    private void validateWechatConfig() {
        WechatProperties wechat = paymentProperties.getWechat();
        
        Assert.hasText(wechat.getAppId(), "微信App ID不能为空");
        Assert.hasText(wechat.getMchId(), "微信商户号不能为空");
        Assert.hasText(wechat.getApiKey(), "微信API密钥不能为空");
        Assert.hasText(wechat.getNotifyUrl(), "微信回调地址不能为空");
        
        log.info("微信支付配置验证通过");
    }
}
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

