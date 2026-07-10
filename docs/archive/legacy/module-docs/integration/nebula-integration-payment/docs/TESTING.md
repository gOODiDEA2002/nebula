# nebula-integration-payment 模块单元测试清单

## 模块说明

支付集成模块，提供统一的支付、退款、查询接口，目前支持支付宝和微信支付。

## 核心功能

1. 统一下单（PaymentRequest -> PaymentResponse）
2. 支付查询（PaymentQuery）
3. 退款申请（RefundRequest）
4. 退款查询（RefundQuery）
5. 支付回调处理

## 测试类清单

### 1. PaymentServiceTest

**测试类路径**: `io.nebula.integration.payment.core.PaymentService` 实现类  
**测试目的**: 验证核心支付流程

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testPay() | pay(PaymentRequest) | 测试统一下单 | PaymentProvider |
| testQuery() | query(PaymentQuery) | 测试支付查询 | PaymentProvider |
| testRefund() | refund(RefundRequest) | 测试退款申请 | PaymentProvider |

### 2. ModelValidationTest

**测试类路径**: `io.nebula.integration.payment.core.model` 包下的模型类  
**测试目的**: 验证请求参数校验

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testPaymentRequestValidation() | - | 验证必填参数校验 |

### 3. AlipayProviderTest (如果包含实现)

**测试类路径**: 支付宝实现类  
**测试目的**: 验证支付宝 SDK 调用逻辑（需 Mock）

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testAlipayPay() | pay() | 测试支付宝下单 | AlipayClient |

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|---------|---------|
| PaymentProvider | Service测试 | Mock具体渠道的实现 |
| AlipayClient | 支付宝测试 | Mock SDK调用 |
| HttpClient | 微信支付测试 | Mock HTTP请求 |

### 严禁真实支付
**所有测试必须使用Mock对象或沙箱环境，严禁在单元测试中使用真实资金进行支付测试。**

## 测试执行

```bash
mvn test -pl nebula/integration/nebula-integration-payment
```

## 验收标准

- 支付流程逻辑测试通过
- 参数校验逻辑测试通过
- Mock对象使用正确

