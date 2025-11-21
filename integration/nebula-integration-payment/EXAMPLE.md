# Nebula Integration Payment - 使用示例

> 支付集成完整使用指南，以票务系统支付为例

## 目录

- [快速开始](#快速开始)
- [支付宝支付](#支付宝支付)
- [微信支付](#微信支付)
- [退款处理](#退款处理)
- [支付查询](#支付查询)
- [回调处理](#回调处理)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-integration-payment</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  payment:
    enabled: true
    
    # 支付宝配置
    alipay:
      enabled: true
      app-id: ${ALIPAY_APP_ID}
      private-key: ${ALIPAY_PRIVATE_KEY}
      public-key: ${ALIPAY_PUBLIC_KEY}
      gateway: https://openapi.alipay.com/gateway.do
      notify-url: ${APP_URL}/api/payment/alipay/notify
      return-url: ${APP_URL}/payment/success
    
    # 微信支付配置
    wechat:
      enabled: true
      app-id: ${WECHAT_APP_ID}
      mch-id: ${WECHAT_MCH_ID}
      api-key: ${WECHAT_API_KEY}
      notify-url: ${APP_URL}/api/payment/wechat/notify
```

---

## 支付宝支付

### 1. 电脑网站支付

```java
/**
 * 支付宝支付服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlipayService {
    
    private final PaymentService paymentService;
    
    /**
     * 创建电脑网站支付
     */
    public String createPagePay(Order order) {
        log.info("创建支付宝电脑网站支付：orderNo={}", order.getOrderNo());
        
        // 构建支付请求
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getOrderNo())
                .amount(order.getTotalAmount())
                .subject(String.format("购买演出门票 - %s", order.getShowtimeTitle()))
                .body(String.format("订单号：%s，座位：%s", order.getOrderNo(), order.getSeats()))
                .paymentType(PaymentType.ALIPAY_PAGE)  // 电脑网站支付
                .timeout(Duration.ofMinutes(30))
                .build();
        
        // 发起支付
        PaymentResponse response = paymentService.pay(request);
        
        if (response.isSuccess()) {
            log.info("支付宝订单创建成功：orderNo={}, tradeNo={}", 
                    order.getOrderNo(), response.getTradeNo());
            
            // 返回支付表单HTML
            return response.getBody();
        } else {
            log.error("支付宝订单创建失败：{}", response.getErrorMessage());
            throw new BusinessException("支付订单创建失败");
        }
    }
}
```

### 2. 手机网站支付

```java
/**
 * 手机网站支付
 */
public String createWapPay(Order order) {
    log.info("创建支付宝手机网站支付：orderNo={}", order.getOrderNo());
    
    PaymentRequest request = PaymentRequest.builder()
            .orderId(order.getOrderNo())
            .amount(order.getTotalAmount())
            .subject(String.format("购买演出门票 - %s", order.getShowtimeTitle()))
            .paymentType(PaymentType.ALIPAY_WAP)  // 手机网站支付
            .timeout(Duration.ofMinutes(30))
            .build();
    
    PaymentResponse response = paymentService.pay(request);
    
    if (response.isSuccess()) {
        log.info("手机支付宝订单创建成功：orderNo={}", order.getOrderNo());
        return response.getBody();  // 返回跳转URL
    } else {
        throw new BusinessException("支付订单创建失败");
    }
}
```

### 3. APP支付

```java
/**
 * APP支付
 */
public String createAppPay(Order order) {
    log.info("创建支付宝APP支付：orderNo={}", order.getOrderNo());
    
    PaymentRequest request = PaymentRequest.builder()
            .orderId(order.getOrderNo())
            .amount(order.getTotalAmount())
            .subject(String.format("购买演出门票 - %s", order.getShowtimeTitle()))
            .paymentType(PaymentType.ALIPAY_APP)  // APP支付
            .timeout(Duration.ofMinutes(30))
            .build();
    
    PaymentResponse response = paymentService.pay(request);
    
    if (response.isSuccess()) {
        log.info("APP支付宝订单创建成功：orderNo={}", order.getOrderNo());
        return response.getBody();  // 返回SDK调用参数
    } else {
        throw new BusinessException("支付订单创建失败");
    }
}
```

---

## 微信支付

### 1. Native扫码支付

```java
/**
 * 微信支付服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WechatPayService {
    
    private final PaymentService paymentService;
    
    /**
     * 创建Native扫码支付
     */
    public String createNativePay(Order order) {
        log.info("创建微信Native扫码支付：orderNo={}", order.getOrderNo());
        
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getOrderNo())
                .amount(order.getTotalAmount())
                .subject(String.format("购买演出门票 - %s", order.getShowtimeTitle()))
                .paymentType(PaymentType.WECHAT_NATIVE)  // Native扫码支付
                .timeout(Duration.ofMinutes(30))
                .build();
        
        PaymentResponse response = paymentService.pay(request);
        
        if (response.isSuccess()) {
            log.info("微信订单创建成功：orderNo={}, prepayId={}", 
                    order.getOrderNo(), response.getTradeNo());
            
            // 返回二维码URL
            return response.getBody();  // code_url
        } else {
            log.error("微信订单创建失败：{}", response.getErrorMessage());
            throw new BusinessException("支付订单创建失败");
        }
    }
}
```

### 2. JSAPI支付

```java
/**
 * JSAPI支付（公众号/小程序）
 */
public Map<String, String> createJsapiPay(Order order, String openid) {
    log.info("创建微信JSAPI支付：orderNo={}, openid={}", order.getOrderNo(), openid);
    
    PaymentRequest request = PaymentRequest.builder()
            .orderId(order.getOrderNo())
            .amount(order.getTotalAmount())
            .subject(String.format("购买演出门票 - %s", order.getShowtimeTitle()))
            .paymentType(PaymentType.WECHAT_JSAPI)  // JSAPI支付
            .buyerInfo(BuyerInfo.builder().openid(openid).build())
            .timeout(Duration.ofMinutes(30))
            .build();
    
    PaymentResponse response = paymentService.pay(request);
    
    if (response.isSuccess()) {
        log.info("JSAPI支付订单创建成功：orderNo={}", order.getOrderNo());
        
        // 返回前端调起支付所需的参数
        return JsonUtils.parseObject(response.getBody(), 
                new TypeReference<Map<String, String>>() {});
    } else {
        throw new BusinessException("支付订单创建失败");
    }
}
```

### 3. APP支付

```java
/**
 * 微信APP支付
 */
public Map<String, String> createAppPay(Order order) {
    log.info("创建微信APP支付：orderNo={}", order.getOrderNo());
    
    PaymentRequest request = PaymentRequest.builder()
            .orderId(order.getOrderNo())
            .amount(order.getTotalAmount())
            .subject(String.format("购买演出门票 - %s", order.getShowtimeTitle()))
            .paymentType(PaymentType.WECHAT_APP)  // APP支付
            .timeout(Duration.ofMinutes(30))
            .build();
    
    PaymentResponse response = paymentService.pay(request);
    
    if (response.isSuccess()) {
        log.info("微信APP支付订单创建成功：orderNo={}", order.getOrderNo());
        
        // 返回APP调起支付所需的参数
        return JsonUtils.parseObject(response.getBody(), 
                new TypeReference<Map<String, String>>() {});
    } else {
        throw new BusinessException("支付订单创建失败");
    }
}
```

---

## 退款处理

### 1. 全额退款

```java
/**
 * 退款服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    
    private final PaymentService paymentService;
    private final OrderService orderService;
    
    /**
     * 全额退款
     */
    public void fullRefund(String orderNo, String reason) {
        log.info("申请全额退款：orderNo={}, reason={}", orderNo, reason);
        
        // 查询订单
        Order order = orderService.getOrderByOrderNo(orderNo);
        
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确，无法退款");
        }
        
        // 构建退款请求
        RefundRequest request = RefundRequest.builder()
                .orderId(orderNo)
                .refundNo(generateRefundNo())  // 生成退款单号
                .refundAmount(order.getTotalAmount())  // 退款金额
                .reason(reason)
                .build();
        
        // 申请退款
        RefundResponse response = paymentService.refund(request);
        
        if (response.isSuccess()) {
            log.info("退款申请成功：orderNo={}, refundNo={}", 
                    orderNo, request.getRefundNo());
            
            // 更新订单状态
            orderService.updateOrderStatus(orderNo, "REFUNDING");
            
            // 记录退款信息
            saveRefundRecord(order, request, response);
        } else {
            log.error("退款申请失败：{}", response.getErrorMessage());
            throw new BusinessException("退款申请失败：" + response.getErrorMessage());
        }
    }
    
    /**
     * 部分退款
     */
    public void partialRefund(String orderNo, BigDecimal refundAmount, String reason) {
        log.info("申请部分退款：orderNo={}, amount={}, reason={}", 
                orderNo, refundAmount, reason);
        
        Order order = orderService.getOrderByOrderNo(orderNo);
        
        // 验证退款金额
        if (refundAmount.compareTo(order.getTotalAmount()) > 0) {
            throw new BusinessException("退款金额不能超过订单金额");
        }
        
        RefundRequest request = RefundRequest.builder()
                .orderId(orderNo)
                .refundNo(generateRefundNo())
                .refundAmount(refundAmount)
                .reason(reason)
                .build();
        
        RefundResponse response = paymentService.refund(request);
        
        if (response.isSuccess()) {
            log.info("部分退款申请成功：orderNo={}, refundNo={}, amount={}", 
                    orderNo, request.getRefundNo(), refundAmount);
            
            orderService.updateOrderStatus(orderNo, "PARTIAL_REFUNDED");
            saveRefundRecord(order, request, response);
        } else {
            throw new BusinessException("退款申请失败：" + response.getErrorMessage());
        }
    }
    
    private String generateRefundNo() {
        return "RFD" + System.currentTimeMillis();
    }
    
    private void saveRefundRecord(Order order, RefundRequest request, RefundResponse response) {
        RefundRecord record = new RefundRecord();
        record.setOrderNo(order.getOrderNo());
        record.setRefundNo(request.getRefundNo());
        record.setRefundAmount(request.getRefundAmount());
        record.setReason(request.getReason());
        record.setStatus(RefundStatus.SUCCESS.name());
        record.setRefundTime(LocalDateTime.now());
        
        refundRecordMapper.insert(record);
    }
}
```

---

## 支付查询

### 1. 查询支付状态

```java
/**
 * 支付查询服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentQueryService {
    
    private final PaymentService paymentService;
    private final OrderService orderService;
    
    /**
     * 查询支付状态
     */
    public PaymentStatus queryPaymentStatus(String orderNo) {
        log.info("查询支付状态：orderNo={}", orderNo);
        
        PaymentQuery query = PaymentQuery.builder()
                .orderId(orderNo)
                .build();
        
        PaymentQueryResponse response = paymentService.query(query);
        
        if (response.isSuccess()) {
            PaymentStatus status = response.getStatus();
            
            log.info("查询成功：orderNo={}, status={}", orderNo, status);
            
            // 根据支付状态更新订单
            if (status == PaymentStatus.SUCCESS) {
                orderService.markOrderPaid(orderNo, response.getTradeNo(), 
                        response.getPayTime());
            }
            
            return status;
        } else {
            log.error("查询支付状态失败：{}", response.getErrorMessage());
            return PaymentStatus.UNKNOWN;
        }
    }
    
    /**
     * 查询退款状态
     */
    public RefundStatus queryRefundStatus(String refundNo) {
        log.info("查询退款状态：refundNo={}", refundNo);
        
        RefundQuery query = RefundQuery.builder()
                .refundNo(refundNo)
                .build();
        
        RefundQueryResponse response = paymentService.queryRefund(query);
        
        if (response.isSuccess()) {
            RefundStatus status = response.getStatus();
            
            log.info("查询成功：refundNo={}, status={}", refundNo, status);
            
            return status;
        } else {
            log.error("查询退款状态失败：{}", response.getErrorMessage());
            return RefundStatus.UNKNOWN;
        }
    }
}
```

---

## 回调处理

### 1. 支付宝回调

```java
/**
 * 支付宝回调控制器
 */
@RestController
@RequestMapping("/api/payment/alipay")
@RequiredArgsConstructor
@Slf4j
public class AlipayNotifyController {
    
    private final PaymentService paymentService;
    private final OrderService orderService;
    
    /**
     * 支付宝异步通知
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        log.info("收到支付宝异步通知");
        
        try {
            // 获取所有参数
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> 
                    params.put(key, values[0]));
            
            // 验证签名并处理通知
            PaymentNotification notification = paymentService.parseNotification(params);
            
            if (notification.isValid()) {
                String orderNo = notification.getOrderId();
                
                log.info("支付宝通知验证成功：orderNo={}, tradeNo={}, status={}", 
                        orderNo, notification.getTradeNo(), notification.getStatus());
                
                // 处理支付结果
                handlePaymentResult(orderNo, notification);
                
                return "success";  // 返回success给支付宝
            } else {
                log.error("支付宝通知验证失败");
                return "fail";
            }
        } catch (Exception e) {
            log.error("处理支付宝通知异常", e);
            return "fail";
        }
    }
    
    private void handlePaymentResult(String orderNo, PaymentNotification notification) {
        PaymentStatus status = notification.getStatus();
        
        if (status == PaymentStatus.SUCCESS) {
            // 支付成功，更新订单状态
            orderService.markOrderPaid(orderNo, notification.getTradeNo(), 
                    notification.getPayTime());
            
            // 发送通知
            notificationService.sendPaymentSuccessNotification(orderNo);
            
            // 生成电子票
            ticketService.generateTickets(orderNo);
        } else {
            log.warn("订单支付未成功：orderNo={}, status={}", orderNo, status);
        }
    }
}
```

### 2. 微信回调

```java
/**
 * 微信支付回调控制器
 */
@RestController
@RequestMapping("/api/payment/wechat")
@RequiredArgsConstructor
@Slf4j
public class WechatPayNotifyController {
    
    private final PaymentService paymentService;
    private final OrderService orderService;
    
    /**
     * 微信支付异步通知
     */
    @PostMapping("/notify")
    public String notify(@RequestBody String xmlData) {
        log.info("收到微信支付异步通知");
        
        try {
            // 解析并验证通知
            PaymentNotification notification = paymentService.parseNotification(xmlData);
            
            if (notification.isValid()) {
                String orderNo = notification.getOrderId();
                
                log.info("微信通知验证成功：orderNo={}, tradeNo={}, status={}", 
                        orderNo, notification.getTradeNo(), notification.getStatus());
                
                // 处理支付结果
                handlePaymentResult(orderNo, notification);
                
                // 返回成功响应给微信
                return buildSuccessResponse();
            } else {
                log.error("微信通知验证失败");
                return buildFailResponse();
            }
        } catch (Exception e) {
            log.error("处理微信通知异常", e);
            return buildFailResponse();
        }
    }
    
    private void handlePaymentResult(String orderNo, PaymentNotification notification) {
        if (notification.getStatus() == PaymentStatus.SUCCESS) {
            orderService.markOrderPaid(orderNo, notification.getTradeNo(), 
                    notification.getPayTime());
            
            notificationService.sendPaymentSuccessNotification(orderNo);
            ticketService.generateTickets(orderNo);
        }
    }
    
    private String buildSuccessResponse() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code>" +
               "<return_msg><![CDATA[OK]]></return_msg></xml>";
    }
    
    private String buildFailResponse() {
        return "<xml><return_code><![CDATA[FAIL]]></return_code>" +
               "<return_msg><![CDATA[ERROR]]></return_msg></xml>";
    }
}
```

---

## 票务系统完整示例

### 完整的支付流程

```java
/**
 * 票务支付服务（完整流程）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPaymentService {
    
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    
    /**
     * 1. 创建支付订单
     */
    public PaymentOrderResponse createPaymentOrder(String orderNo, PaymentType paymentType) {
        log.info("创建支付订单：orderNo={}, paymentType={}", orderNo, paymentType);
        
        // 1.1 查询订单
        Order order = orderService.getOrderByOrderNo(orderNo);
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        
        // 1.2 检查订单是否过期
        if (order.getCreateTime().plusMinutes(30).isBefore(LocalDateTime.now())) {
            throw new BusinessException("订单已过期");
        }
        
        // 1.3 构建支付请求
        PaymentRequest request = buildPaymentRequest(order, paymentType);
        
        // 1.4 发起支付
        PaymentResponse response = paymentService.pay(request);
        
        if (response.isSuccess()) {
            log.info("支付订单创建成功：orderNo={}, tradeNo={}", 
                    orderNo, response.getTradeNo());
            
            // 1.5 保存支付记录
            savePaymentRecord(order, paymentType, response);
            
            // 1.6 构建响应
            PaymentOrderResponse result = new PaymentOrderResponse();
            result.setOrderNo(orderNo);
            result.setPaymentType(paymentType);
            result.setPaymentData(response.getBody());
            result.setExpireTime(LocalDateTime.now().plusMinutes(30));
            
            return result;
        } else {
            log.error("支付订单创建失败：{}", response.getErrorMessage());
            throw new BusinessException("支付订单创建失败");
        }
    }
    
    /**
     * 2. 处理支付回调
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentNotification(PaymentNotification notification) {
        String orderNo = notification.getOrderId();
        
        log.info("处理支付回调：orderNo={}, status={}", orderNo, notification.getStatus());
        
        // 2.1 幂等性检查
        if (isPaymentProcessed(orderNo)) {
            log.info("支付已处理，跳过：orderNo={}", orderNo);
            return;
        }
        
        if (notification.getStatus() == PaymentStatus.SUCCESS) {
            // 2.2 更新订单状态
            orderService.markOrderPaid(orderNo, notification.getTradeNo(), 
                    notification.getPayTime());
            
            // 2.3 生成电子票
            List<String> ticketNos = ticketService.generateTickets(orderNo);
            
            // 2.4 发送通知
            Order order = orderService.getOrderByOrderNo(orderNo);
            notificationService.sendPaymentSuccessNotification(
                    order.getUserId(), orderNo, ticketNos);
            
            // 2.5 标记已处理
            markPaymentProcessed(orderNo);
            
            log.info("支付回调处理完成：orderNo={}", orderNo);
        } else {
            log.warn("支付未成功：orderNo={}, status={}", orderNo, notification.getStatus());
        }
    }
    
    /**
     * 3. 处理退款
     */
    @Transactional(rollbackFor = Exception.class)
    public void processRefund(String orderNo, String reason) {
        log.info("处理退款：orderNo={}, reason={}", orderNo, reason);
        
        // 3.1 查询订单
        Order order = orderService.getOrderByOrderNo(orderNo);
        
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确，无法退款");
        }
        
        // 3.2 检查退款条件
        validateRefundConditions(order);
        
        // 3.3 申请退款
        RefundRequest request = RefundRequest.builder()
                .orderId(orderNo)
                .refundNo(generateRefundNo())
                .refundAmount(order.getTotalAmount())
                .reason(reason)
                .build();
        
        RefundResponse response = paymentService.refund(request);
        
        if (response.isSuccess()) {
            // 3.4 更新订单状态
            orderService.updateOrderStatus(orderNo, "REFUNDED");
            
            // 3.5 作废电子票
            ticketService.invalidateTickets(orderNo);
            
            // 3.6 恢复库存
            showtimeService.restoreStock(order.getShowtimeId(), order.getQuantity());
            
            // 3.7 发送通知
            notificationService.sendRefundSuccessNotification(
                    order.getUserId(), orderNo, order.getTotalAmount());
            
            log.info("退款处理完成：orderNo={}, refundNo={}", 
                    orderNo, request.getRefundNo());
        } else {
            throw new BusinessException("退款失败：" + response.getErrorMessage());
        }
    }
    
    // 辅助方法
    
    private PaymentRequest buildPaymentRequest(Order order, PaymentType paymentType) {
        return PaymentRequest.builder()
                .orderId(order.getOrderNo())
                .amount(order.getTotalAmount())
                .subject(String.format("购买演出门票 - %s", order.getShowtimeTitle()))
                .body(String.format("订单号：%s，座位：%s", order.getOrderNo(), order.getSeats()))
                .paymentType(paymentType)
                .timeout(Duration.ofMinutes(30))
                .build();
    }
    
    private void savePaymentRecord(Order order, PaymentType paymentType, 
                                   PaymentResponse response) {
        PaymentRecord record = new PaymentRecord();
        record.setOrderNo(order.getOrderNo());
        record.setPaymentType(paymentType.name());
        record.setTradeNo(response.getTradeNo());
        record.setAmount(order.getTotalAmount());
        record.setStatus("PENDING");
        record.setCreateTime(LocalDateTime.now());
        
        paymentRecordMapper.insert(record);
    }
    
    private boolean isPaymentProcessed(String orderNo) {
        String key = "payment:processed:" + orderNo;
        return redisTemplate.hasKey(key);
    }
    
    private void markPaymentProcessed(String orderNo) {
        String key = "payment:processed:" + orderNo;
        redisTemplate.opsForValue().set(key, "1", Duration.ofDays(7));
    }
    
    private String generateRefundNo() {
        return "RFD" + System.currentTimeMillis();
    }
    
    private void validateRefundConditions(Order order) {
        // 检查演出是否已开始
        Showtime showtime = showtimeService.getById(order.getShowtimeId());
        
        if (showtime.getShowTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("演出已开始，无法退款");
        }
        
        // 检查距离演出开始时间
        long hoursUntilShow = Duration.between(LocalDateTime.now(), 
                showtime.getShowTime()).toHours();
        
        if (hoursUntilShow < 2) {
            throw new BusinessException("距离演出开始不足2小时，无法退款");
        }
    }
}

@Data
public class PaymentOrderResponse {
    private String orderNo;
    private PaymentType paymentType;
    private String paymentData;  // 支付参数（URL/表单/SDK参数等）
    private LocalDateTime expireTime;
}
```

---

## 最佳实践

### 1. 安全防护

- **签名验证**：验证回调签名的合法性
- **幂等性**：防止重复处理支付回调
- **金额校验**：验证支付金额与订单金额一致
- **状态检查**：检查订单状态是否允许支付

### 2. 异常处理

- **超时重试**：网络超时时自动重试
- **降级处理**：支付渠道故障时的降级方案
- **异常记录**：记录所有异常和错误
- **告警通知**：支付异常时及时告警

### 3. 对账和核对

- **支付记录**：记录所有支付请求和响应
- **定时对账**：每日对账确保数据一致
- **差异处理**：处理支付平台与系统差异
- **财务报表**：生成财务统计报表

### 4. 性能优化

- **异步回调**：回调处理异步执行
- **批量查询**：批量查询支付状态
- **缓存优化**：缓存支付配置信息
- **连接池**：复用HTTP连接

### 5. 测试

- **沙箱测试**：使用支付平台沙箱环境测试
- **Mock测试**：使用Mock支付服务测试
- **压力测试**：测试支付系统承载能力
- **回调测试**：测试回调处理的正确性

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [TESTING.md](./TESTING.md) - 测试指南

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0
