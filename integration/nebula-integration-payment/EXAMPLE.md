# nebula-integration-payment 模块示例

## 模块简介

`nebula-integration-payment` 提供了一套统一的支付接口，使得业务代码可以以相同的方式调用不同的支付渠道（如支付宝、微信支付）。

## 核心功能示例

### 1. 发起支付

**`io.nebula.example.payment.service.CheckoutService`**:

```java
package io.nebula.example.payment.service;

import io.nebula.integration.payment.core.PaymentService;
import io.nebula.integration.payment.core.model.PaymentRequest;
import io.nebula.integration.payment.core.model.PaymentResponse;
import io.nebula.integration.payment.core.model.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final PaymentService paymentService;

    public String checkoutOrder(String orderId, BigDecimal amount, String paymentMethod) {
        // 构建支付请求
        PaymentRequest request = PaymentRequest.builder()
                .orderId(orderId)
                .amount(amount)
                .subject("订单支付: " + orderId)
                .paymentType(PaymentType.valueOf(paymentMethod)) // ALIPAY_APP, WECHAT_NATIVE 等
                .build();

        // 发起支付
        PaymentResponse response = paymentService.pay(request);
        
        if (response.isSuccess()) {
            log.info("支付下单成功: {}", response.getTradeNo());
            // 返回支付参数（如支付宝的 form 表单，或微信的 code_url）
            return response.getBody();
        } else {
            log.error("支付下单失败: {}", response.getErrorMessage());
            throw new RuntimeException("支付失败");
        }
    }
}
```

### 2. 查询支付状态

```java
public void checkStatus(String orderId) {
    PaymentQuery query = PaymentQuery.builder()
            .orderId(orderId)
            .build();
            
    PaymentQueryResponse response = paymentService.query(query);
    
    if (response.getStatus() == PaymentStatus.SUCCESS) {
        log.info("订单 {} 已支付", orderId);
    }
}
```

### 3. 申请退款

```java
public void refundOrder(String orderId, BigDecimal refundAmount) {
    RefundRequest request = RefundRequest.builder()
            .orderId(orderId)
            .refundAmount(refundAmount)
            .reason("用户申请退款")
            .build();
            
    RefundResponse response = paymentService.refund(request);
    
    if (response.isSuccess()) {
        log.info("退款申请成功");
    }
}
```

## 总结

该模块通过统一的模型对象（`PaymentRequest`, `PaymentResponse`）屏蔽了底层支付 SDK 的复杂性，开发者只需关注业务逻辑，无需深入了解支付宝或微信支付的具体 API 细节。

