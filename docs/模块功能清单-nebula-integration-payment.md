# nebula-integration-payment 模块功能清单

> **模块**: nebula-integration-payment  
> **版本**: 2.0.1-SNAPSHOT  
> **状态**: ✅ 基础完整 | ⚠️ 需扩展  
> **生成时间**: 2025-01-13

## 📊 模块概况

### 基本信息

| 项目 | 信息 |
|------|------|
| 模块名称 | nebula-integration-payment |
| Maven坐标 | io.nebula:nebula-integration-payment |
| 包名 | io.nebula.integration.payment |
| 文件数量 | 23个Java文件 |
| 代码行数 | 约1500行 |
| 依赖模块 | Spring Boot, Lombok |

### 完成度评估

| 维度 | 完成度 | 说明 |
|------|--------|------|
| 核心架构 | 90% | 接口设计完整 |
| Mock实现 | 95% | 可用于开发测试 |
| 生产实现 | 0% | 无真实支付渠道实现 |
| 文档完整性 | 5% | 无独立README |
| 测试覆盖 | 0% | 无测试代码 |
| 生产就绪 | ⚠️ | 需补充真实支付实现 |

## ✅ 已实现功能

### 1. 核心接口层

#### 1.1 支付服务接口

**类名**: `io.nebula.integration.payment.core.PaymentService`

**核心方法**:

| 方法名 | 参数 | 返回值 | 功能描述 |
|--------|------|--------|----------|
| `createPayment()` | PaymentRequest | PaymentResponse | 创建支付订单 |
| `queryPayment()` | PaymentQuery | PaymentQueryResponse | 查询支付状态 |
| `cancelPayment()` | PaymentCancelRequest | PaymentCancelResponse | 取消支付 |
| `refund()` | RefundRequest | RefundResponse | 申请退款 |
| `queryRefund()` | RefundQuery | RefundQueryResponse | 查询退款状态 |
| `handleNotification()` | PaymentNotification | NotificationResult | 处理支付回调 |
| `verifyNotification()` | PaymentNotification | boolean | 验证回调签名 |
| `getProvider()` | - | PaymentProvider | 获取支付提供商 |
| `isAvailable()` | - | boolean | 检查服务可用性 |

**完整接口示例**:
```java
public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest request);
    PaymentQueryResponse queryPayment(PaymentQuery query);
    PaymentCancelResponse cancelPayment(PaymentCancelRequest request);
    RefundResponse refund(RefundRequest request);
    RefundQueryResponse queryRefund(RefundQuery query);
    NotificationResult handleNotification(PaymentNotification notification);
    boolean verifyNotification(PaymentNotification notification);
    PaymentProvider getProvider();
    boolean isAvailable();
}
```

### 2. 数据模型层

#### 2.1 支付相关模型

| 类名 | 用途 | 主要字段 |
|------|------|----------|
| **PaymentRequest** | 支付请求 | outTradeNo, amount, currency, subject, paymentType, buyerInfo, timeExpire |
| **PaymentResponse** | 支付响应 | success, outTradeNo, tradeNo, status, payUrl, qrCode, payParams, prepayId |
| **PaymentQuery** | 支付查询 | outTradeNo, tradeNo |
| **PaymentQueryResponse** | 查询响应 | status, amount, paidAmount, payTime, payMethod |
| **PaymentCancelRequest** | 取消请求 | outTradeNo, reason |
| **PaymentCancelResponse** | 取消响应 | success, outTradeNo, tradeNo |

#### 2.2 退款相关模型

| 类名 | 用途 | 主要字段 |
|------|------|----------|
| **RefundRequest** | 退款请求 | outTradeNo, outRefundNo, refundAmount, refundReason |
| **RefundResponse** | 退款响应 | success, refundNo, refundAmount, refundTime |
| **RefundQuery** | 退款查询 | outRefundNo, refundNo |
| **RefundQueryResponse** | 退款查询响应 | status, refundAmount, refundTime |

#### 2.3 通知相关模型

| 类名 | 用途 | 主要字段 |
|------|------|----------|
| **PaymentNotification** | 支付通知 | type, outTradeNo, tradeNo, status, totalAmount, timestamp, signature |
| **NotificationResult** | 通知结果 | success, message |

#### 2.4 枚举类型

**PaymentProvider** (支付提供商):
- ALIPAY (支付宝)
- WECHAT_PAY (微信支付)
- UNION_PAY (银联支付)
- PAYPAL (PayPal)
- STRIPE (Stripe)
- MOCK (测试支付)

**PaymentType** (支付类型):
```java
public enum PaymentType {
    QR_CODE,    // 扫码支付
    WEB,        // 网页支付
    WAP,        // 手机网页支付
    APP,        // APP支付
    MINI_PROGRAM, // 小程序支付
    JSAPI       // 公众号支付
}
```

**PaymentStatus** (支付状态):
```java
public enum PaymentStatus {
    PENDING,    // 待支付
    SUCCESS,    // 支付成功
    FAILED,     // 支付失败
    CANCELLED,  // 已取消
    REFUNDED,   // 已退款
    PARTIAL_REFUNDED, // 部分退款
    CLOSED      // 已关闭
}
```

**RefundStatus** (退款状态):
```java
public enum RefundStatus {
    PENDING,    // 退款中
    SUCCESS,    // 退款成功
    FAILED      // 退款失败
}
```

**BuyerInfo** (买家信息):
```java
@Data
@Builder
public class BuyerInfo {
    private String buyerId;      // 买家ID
    private String buyerName;    // 买家姓名
    private String buyerEmail;   // 买家邮箱
    private String buyerPhone;   // 买家手机号
}
```

### 3. Mock实现层

#### 3.1 MockPaymentService

**类名**: `io.nebula.integration.payment.provider.mock.MockPaymentService`

**功能**: 完整的Mock支付实现,用于开发和测试

**特性**:
- ✅ 支持所有支付类型(QR_CODE, WEB, APP等)
- ✅ 自动支付成功机制(创建1分钟后自动成功)
- ✅ 支持支付查询
- ✅ 支持支付取消
- ✅ 支持退款申请
- ✅ 支持退款查询
- ✅ 支持回调处理
- ✅ 内存存储订单数据
- ✅ 提供测试辅助方法

**测试辅助方法**:
```java
// 模拟支付成功
public void mockPaymentSuccess(String outTradeNo)

// 模拟支付失败
public void mockPaymentFailed(String outTradeNo)
```

**使用示例**:
```java
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService;
    
    public PaymentResponse createPayment(Order order) {
        PaymentRequest request = PaymentRequest.builder()
            .outTradeNo(order.getOrderNo())
            .amount(order.getTotalAmount())
            .currency("CNY")
            .subject(order.getTitle())
            .paymentType(PaymentType.QR_CODE)
            .buyerInfo(BuyerInfo.builder()
                .buyerId(order.getUserId().toString())
                .buyerName(order.getUserName())
                .build())
            .build();
            
        return paymentService.createPayment(request);
    }
}
```

#### 3.2 Mock存储模型

| 类名 | 用途 | 存储内容 |
|------|------|----------|
| **MockPaymentOrder** | Mock支付订单 | 商户订单号、支付订单号、金额、状态、时间等 |
| **MockRefundOrder** | Mock退款订单 | 退款单号、退款金额、退款状态、时间等 |

### 4. 配置层

#### 4.1 PaymentProperties

**配置前缀**: `nebula.payment`

**支持的配置**:

```yaml
nebula:
  payment:
    # Mock支付配置
    mock:
      enabled: true                  # 是否启用Mock支付
      auto-success-delay: 60         # 自动支付成功延迟(秒)
    
    # 支付宝配置
    alipay:
      enabled: false                 # 是否启用支付宝
      app-id: ""                     # 应用ID
      private-key: ""                # 商户私钥
      public-key: ""                 # 支付宝公钥
      server-url: "https://openapi.alipay.com/gateway.do"
      sign-type: "RSA2"              # 签名类型
      charset: "UTF-8"               # 字符编码
      format: "json"                 # 数据格式
    
    # 微信支付配置
    wechat-pay:
      enabled: false                 # 是否启用微信支付
      app-id: ""                     # 应用ID
      mch-id: ""                     # 商户号
      mch-key: ""                    # 商户密钥
      cert-path: ""                  # 证书路径
      server-url: "https://api.mch.weixin.qq.com"
```

#### 4.2 自动配置

**类名**: `io.nebula.integration.payment.autoconfigure.PaymentAutoConfiguration`

**功能**: 
- 自动注册Mock支付服务
- 条件化配置(基于配置文件)
- Spring Boot自动配置集成

## ❌ 缺失功能

### 1. 真实支付渠道实现 (P0)

#### 1.1 支付宝实现

**缺失**: AlipayPaymentServiceImpl

**应包含**:
- ✅ 配置类已定义
- ❌ 服务实现类
- ❌ SDK集成
- ❌ 签名验证
- ❌ 回调处理

**建议依赖**:
```xml
<dependency>
    <groupId>com.alipay.sdk</groupId>
    <artifactId>alipay-sdk-java</artifactId>
    <version>4.38.157.ALL</version>
</dependency>
```

**建议实现结构**:
```java
@Service
@ConditionalOnProperty(prefix = "nebula.payment.alipay", name = "enabled", havingValue = "true")
public class AlipayPaymentServiceImpl implements PaymentService {
    
    @Autowired
    private PaymentProperties properties;
    
    private AlipayClient alipayClient;
    
    @PostConstruct
    public void init() {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl(properties.getAlipay().getServerUrl());
        config.setAppId(properties.getAlipay().getAppId());
        config.setPrivateKey(properties.getAlipay().getPrivateKey());
        config.setAlipayPublicKey(properties.getAlipay().getPublicKey());
        
        alipayClient = new DefaultAlipayClient(config);
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // 实现支付宝支付
    }
    
    // 其他方法实现...
}
```

#### 1.2 微信支付实现

**缺失**: WechatPayPaymentServiceImpl

**应包含**:
- ✅ 配置类已定义
- ❌ 服务实现类
- ❌ SDK集成
- ❌ 证书管理
- ❌ 回调处理

**建议依赖**:
```xml
<dependency>
    <groupId>com.github.wechatpay-apiv3</groupId>
    <artifactId>wechatpay-java</artifactId>
    <version>0.2.12</version>
</dependency>
```

#### 1.3 银联支付实现

**缺失**: UnionPayPaymentServiceImpl

#### 1.4 国际支付实现

- ❌ PayPal实现
- ❌ Stripe实现

### 2. 高级功能

#### 2.1 支付路由

**缺失**: PaymentRouter

**功能**:
- 根据金额/地区/用户自动选择支付渠道
- 支付渠道优先级配置
- 失败自动切换备用渠道

**建议实现**:
```java
@Service
public class PaymentRouter {
    
    @Autowired
    private List<PaymentService> paymentServices;
    
    public PaymentService route(PaymentRequest request) {
        // 路由逻辑
        // 1. 检查用户偏好
        // 2. 检查金额范围
        // 3. 检查地区限制
        // 4. 检查渠道可用性
        return selectedPaymentService;
    }
}
```

#### 2.2 支付订单管理

**缺失**: PaymentOrderService

**功能**:
- 支付订单持久化
- 订单状态同步
- 订单查询接口
- 对账功能

#### 2.3 异步回调处理

**缺失**: NotificationHandler

**功能**:
- 异步回调接收
- 签名验证
- 幂等性处理
- 重试机制
- 业务通知

**建议实现**:
```java
@RestController
@RequestMapping("/payment/notify")
public class PaymentNotificationController {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private NotificationHandler notificationHandler;
    
    @PostMapping("/alipay")
    public String alipayNotify(HttpServletRequest request) {
        // 1. 解析回调参数
        // 2. 验证签名
        // 3. 处理业务逻辑
        // 4. 返回确认
        return "success";
    }
    
    @PostMapping("/wechat")
    public String wechatNotify(@RequestBody String xmlData) {
        // 微信回调处理
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>";
    }
}
```

#### 2.4 对账功能

**缺失**: ReconciliationService

**功能**:
- 下载对账文件
- 对账差异检测
- 差异处理
- 对账报告生成

### 3. 安全功能

#### 3.1 签名管理

**缺失**: SignatureManager

**功能**:
- 请求签名生成
- 响应签名验证
- 证书管理
- 密钥轮换

#### 3.2 数据加密

**缺失**: 敏感信息加密

**应加密字段**:
- 买家手机号
- 买家邮箱
- 买家身份证号

### 4. 监控与统计

#### 4.1 支付监控

**缺失**: PaymentMetrics

**功能**:
- 支付成功率统计
- 支付时长监控
- 支付渠道性能对比
- 异常告警

#### 4.2 财务报表

**缺失**: FinancialReportService

**功能**:
- 日/月/年报表
- 收入统计
- 退款统计
- 渠道费用统计

## 🎯 功能补充优先级

### P0 (立即补充) 🔴

1. **支付宝实现**
   - AlipayPaymentServiceImpl
   - 支付/查询/退款
   - 回调处理

2. **微信支付实现**
   - WechatPayPaymentServiceImpl
   - 支付/查询/退款
   - 回调处理

3. **支付订单管理**
   - 数据库表设计
   - 订单持久化
   - 状态同步

4. **回调处理**
   - Controller实现
   - 签名验证
   - 幂等性

### P1 (重要) ⚠️

5. **支付路由**
   - 多渠道路由
   - 失败切换

6. **对账功能**
   - 对账文件下载
   - 差异检测

7. **监控统计**
   - 成功率统计
   - 性能监控

### P2 (可选) 💡

8. **国际支付**
   - PayPal
   - Stripe

9. **高级功能**
   - 分账
   - 延迟结算
   - 资金冻结

## 📐 建议的目录结构扩展

```
nebula-integration-payment/
├── src/main/java/io/nebula/integration/payment/
│   ├── core/                          # 核心层 ✅
│   │   ├── PaymentService.java
│   │   └── model/                     # 数据模型 ✅
│   ├── provider/                      # 提供商实现
│   │   ├── alipay/                    # 支付宝 ❌
│   │   │   ├── AlipayPaymentServiceImpl.java
│   │   │   └── AlipaySignature.java
│   │   ├── wechat/                    # 微信支付 ❌
│   │   │   ├── WechatPayPaymentServiceImpl.java
│   │   │   └── WechatPaySignature.java
│   │   ├── unionpay/                  # 银联 ❌
│   │   ├── paypal/                    # PayPal ❌
│   │   ├── stripe/                    # Stripe ❌
│   │   └── mock/                      # Mock实现 ✅
│   ├── router/                        # 支付路由 ❌
│   │   ├── PaymentRouter.java
│   │   └── RouteStrategy.java
│   ├── order/                         # 订单管理 ❌
│   │   ├── PaymentOrderService.java
│   │   ├── PaymentOrder.java
│   │   └── PaymentOrderRepository.java
│   ├── notification/                  # 回调处理 ❌
│   │   ├── NotificationHandler.java
│   │   ├── NotificationController.java
│   │   └── IdempotentManager.java
│   ├── reconciliation/                # 对账 ❌
│   │   ├── ReconciliationService.java
│   │   └── ReconciliationReport.java
│   ├── security/                      # 安全 ❌
│   │   ├── SignatureManager.java
│   │   └── CertificateManager.java
│   ├── metrics/                       # 监控 ❌
│   │   ├── PaymentMetrics.java
│   │   └── FinancialReportService.java
│   └── autoconfigure/                 # 自动配置 ✅
│       ├── PaymentAutoConfiguration.java
│       └── PaymentProperties.java
└── src/main/resources/
    ├── db/migration/                  # 数据库迁移 ❌
    │   └── V1__init_payment_tables.sql
    └── META-INF/
        └── spring.factories
```

## 💡 实施建议

### Phase 1: 真实支付渠道 (2-3周)

**任务**:
1. 集成支付宝SDK
2. 实现AlipayPaymentServiceImpl
3. 集成微信支付SDK
4. 实现WechatPayPaymentServiceImpl
5. 实现回调Controller
6. 编写集成测试

**交付物**:
- 可用的支付宝/微信支付功能
- 完整的回调处理
- 测试用例覆盖率≥80%

### Phase 2: 订单管理 (1-2周)

**任务**:
1. 设计数据库表
2. 实现PaymentOrderService
3. 实现订单状态同步
4. 实现订单查询API

**交付物**:
- 完整的订单管理系统
- RESTful API

### Phase 3: 高级功能 (2-3周)

**任务**:
1. 实现支付路由
2. 实现对账功能
3. 实现监控统计
4. 性能优化

**交付物**:
- 智能路由系统
- 对账报表
- 监控dashboard

## 📝 文档补充建议

当前无独立README,建议创建:

### 必需文档

1. **README.md** (参考模板)
   - 快速开始
   - 配置说明
   - 使用示例
   - API文档

2. **支付渠道接入指南**
   - 支付宝接入
   - 微信接入
   - 测试环境配置

3. **回调处理指南**
   - 回调URL配置
   - 签名验证
   - 幂等性处理

4. **对账指南**
   - 对账文件格式
   - 对账流程
   - 差异处理

### 参考模板

使用: [MODULE_README_TEMPLATE.md](../templates/MODULE_README_TEMPLATE.md)

## 📚 参考资源

- [支付宝开放平台](https://opendocs.alipay.com/open)
- [微信支付文档](https://pay.weixin.qq.com/wiki/doc/api/index.html)
- [PayPal Developer](https://developer.paypal.com/)
- [Stripe API Reference](https://stripe.com/docs/api)

## 🔗 相关模块

- `nebula-foundation`: 基础工具类
- `nebula-data-persistence`: 订单数据持久化
- `nebula-messaging-rabbitmq`: 异步通知
- `nebula-web`: Web回调接口

---

**总结**: nebula-integration-payment模块架构设计优秀，Mock实现完整，但缺少真实支付渠道实现。建议优先实现P0级别功能后方可用于生产环境。

