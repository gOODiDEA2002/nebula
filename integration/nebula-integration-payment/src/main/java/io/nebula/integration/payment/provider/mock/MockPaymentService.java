package io.nebula.integration.payment.provider.mock;

import io.nebula.integration.payment.core.PaymentService;
import io.nebula.integration.payment.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock 支付服务实现
 * 用于开发和测试环境
 *
 * @author nebula
 */
@Service
public class MockPaymentService implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentService.class);

    // 模拟存储支付订单
    private final Map<String, MockPaymentOrder> payments = new ConcurrentHashMap<>();
    private final Map<String, MockRefundOrder> refunds = new ConcurrentHashMap<>();

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        logger.info("创建Mock支付订单: {}", request.getOutTradeNo());

        String tradeNo = "mock_" + UUID.randomUUID().toString().replace("-", "");
        
        // 存储支付订单
        MockPaymentOrder order = MockPaymentOrder.builder()
            .outTradeNo(request.getOutTradeNo())
            .tradeNo(tradeNo)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .subject(request.getSubject())
            .status(PaymentStatus.PENDING)
            .createTime(LocalDateTime.now())
            .expireTime(request.getTimeExpire() != null ? request.getTimeExpire() : LocalDateTime.now().plusHours(2))
            .build();
        
        payments.put(request.getOutTradeNo(), order);

        // 根据支付类型返回不同的响应
        Map<String, Object> payParams = new HashMap<>();
        String qrCode = null;
        String payUrl = null;

        switch (request.getPaymentType()) {
            case QR_CODE:
                qrCode = "mock://pay/" + tradeNo;
                break;
            case WEB:
                payUrl = "http://mock.payment.com/pay/" + tradeNo;
                break;
            case APP:
                payParams.put("partnerId", "mock_partner");
                payParams.put("prepayId", tradeNo);
                payParams.put("nonceStr", UUID.randomUUID().toString());
                payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
                payParams.put("sign", "mock_sign_" + UUID.randomUUID().toString());
                break;
            default:
                payUrl = "http://mock.payment.com/pay/" + tradeNo;
        }

        return PaymentResponse.builder()
            .success(true)
            .outTradeNo(request.getOutTradeNo())
            .tradeNo(tradeNo)
            .status(PaymentStatus.PENDING)
            .payUrl(payUrl)
            .qrCode(qrCode)
            .payParams(payParams)
            .prepayId(tradeNo)
            .createTime(LocalDateTime.now())
            .expireTime(order.getExpireTime())
            .provider(PaymentProvider.MOCK)
            .build();
    }

    @Override
    public PaymentQueryResponse queryPayment(PaymentQuery query) {
        logger.info("查询Mock支付订单: {}", query.getOutTradeNo());

        MockPaymentOrder order = payments.get(query.getOutTradeNo());
        if (order == null) {
            return PaymentQueryResponse.builder()
                .success(false)
                .errorCode("ORDER_NOT_FOUND")
                .errorMessage("订单不存在")
                .outTradeNo(query.getOutTradeNo())
                .provider(PaymentProvider.MOCK)
                .build();
        }

        // 模拟支付状态变化（简单规则：创建超过1分钟自动成功）
        if (order.getStatus() == PaymentStatus.PENDING && 
            order.getCreateTime().isBefore(LocalDateTime.now().minusMinutes(1))) {
            order.setStatus(PaymentStatus.SUCCESS);
            order.setPayTime(LocalDateTime.now());
        }

        return PaymentQueryResponse.builder()
            .success(true)
            .outTradeNo(order.getOutTradeNo())
            .tradeNo(order.getTradeNo())
            .status(order.getStatus())
            .amount(order.getAmount())
            .paidAmount(order.getStatus() == PaymentStatus.SUCCESS ? order.getAmount() : BigDecimal.ZERO)
            .currency(order.getCurrency())
            .subject(order.getSubject())
            .createTime(order.getCreateTime())
            .payTime(order.getPayTime())
            .payMethod("mock_pay")
            .provider(PaymentProvider.MOCK)
            .build();
    }

    @Override
    public PaymentCancelResponse cancelPayment(PaymentCancelRequest request) {
        logger.info("取消Mock支付订单: {}", request.getOutTradeNo());

        MockPaymentOrder order = payments.get(request.getOutTradeNo());
        if (order == null) {
            return PaymentCancelResponse.builder()
                .success(false)
                .errorCode("ORDER_NOT_FOUND")
                .errorMessage("订单不存在")
                .outTradeNo(request.getOutTradeNo())
                .provider(PaymentProvider.MOCK)
                .build();
        }

        if (order.getStatus() != PaymentStatus.PENDING) {
            return PaymentCancelResponse.builder()
                .success(false)
                .errorCode("ORDER_STATUS_ERROR")
                .errorMessage("订单状态不允许取消")
                .outTradeNo(request.getOutTradeNo())
                .tradeNo(order.getTradeNo())
                .provider(PaymentProvider.MOCK)
                .build();
        }

        order.setStatus(PaymentStatus.CANCELLED);

        return PaymentCancelResponse.builder()
            .success(true)
            .outTradeNo(order.getOutTradeNo())
            .tradeNo(order.getTradeNo())
            .provider(PaymentProvider.MOCK)
            .build();
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        logger.info("创建Mock退款: {}", request.getOutRefundNo());

        MockPaymentOrder order = payments.get(request.getOutTradeNo());
        if (order == null || order.getStatus() != PaymentStatus.SUCCESS) {
            return RefundResponse.builder()
                .success(false)
                .errorCode("ORDER_NOT_FOUND_OR_NOT_PAID")
                .errorMessage("订单不存在或未支付")
                .outTradeNo(request.getOutTradeNo())
                .outRefundNo(request.getOutRefundNo())
                .provider(PaymentProvider.MOCK)
                .build();
        }

        String refundNo = "mock_refund_" + UUID.randomUUID().toString().replace("-", "");
        
        MockRefundOrder refundOrder = MockRefundOrder.builder()
            .outTradeNo(request.getOutTradeNo())
            .outRefundNo(request.getOutRefundNo())
            .refundNo(refundNo)
            .refundAmount(request.getRefundAmount())
            .status(RefundStatus.SUCCESS) // Mock 退款直接成功
            .refundTime(LocalDateTime.now())
            .build();
        
        refunds.put(request.getOutRefundNo(), refundOrder);

        return RefundResponse.builder()
            .success(true)
            .outTradeNo(request.getOutTradeNo())
            .tradeNo(order.getTradeNo())
            .outRefundNo(request.getOutRefundNo())
            .refundNo(refundNo)
            .refundAmount(request.getRefundAmount())
            .refundTime(LocalDateTime.now())
            .provider(PaymentProvider.MOCK)
            .build();
    }

    @Override
    public RefundQueryResponse queryRefund(RefundQuery query) {
        logger.info("查询Mock退款: {}", query.getOutRefundNo());

        MockRefundOrder refundOrder = refunds.get(query.getOutRefundNo());
        if (refundOrder == null) {
            return RefundQueryResponse.builder()
                .success(false)
                .errorCode("REFUND_NOT_FOUND")
                .errorMessage("退款记录不存在")
                .outRefundNo(query.getOutRefundNo())
                .provider(PaymentProvider.MOCK)
                .build();
        }

        return RefundQueryResponse.builder()
            .success(true)
            .outRefundNo(refundOrder.getOutRefundNo())
            .refundNo(refundOrder.getRefundNo())
            .refundAmount(refundOrder.getRefundAmount())
            .status(refundOrder.getStatus())
            .refundTime(refundOrder.getRefundTime())
            .provider(PaymentProvider.MOCK)
            .build();
    }

    @Override
    public NotificationResult handleNotification(PaymentNotification notification) {
        logger.info("处理Mock支付通知: {}", notification.getType());
        
        // Mock 实现直接返回成功
        return NotificationResult.success("success");
    }

    @Override
    public boolean verifyNotification(PaymentNotification notification) {
        logger.info("验证Mock支付通知签名");
        
        // Mock 实现直接返回验证成功
        return true;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MOCK;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 模拟触发支付成功（用于测试）
     */
    public void mockPaymentSuccess(String outTradeNo) {
        MockPaymentOrder order = payments.get(outTradeNo);
        if (order != null && order.getStatus() == PaymentStatus.PENDING) {
            order.setStatus(PaymentStatus.SUCCESS);
            order.setPayTime(LocalDateTime.now());
            logger.info("Mock支付成功: {}", outTradeNo);
        }
    }

    /**
     * 模拟触发支付失败（用于测试）
     */
    public void mockPaymentFailed(String outTradeNo) {
        MockPaymentOrder order = payments.get(outTradeNo);
        if (order != null && order.getStatus() == PaymentStatus.PENDING) {
            order.setStatus(PaymentStatus.FAILED);
            logger.info("Mock支付失败: {}", outTradeNo);
        }
    }
}
