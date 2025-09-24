package io.nebula.integration.payment.core;

import io.nebula.integration.payment.core.model.*;

/**
 * 支付服务接口
 * 提供统一的支付抽象，支持多种支付提供商
 *
 * @author nebula
 */
public interface PaymentService {

    /**
     * 创建支付订单
     *
     * @param request 支付请求
     * @return 支付响应
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * 查询支付状态
     *
     * @param query 查询请求
     * @return 支付查询响应
     */
    PaymentQueryResponse queryPayment(PaymentQuery query);

    /**
     * 取消支付
     *
     * @param request 取消支付请求
     * @return 取消支付响应
     */
    PaymentCancelResponse cancelPayment(PaymentCancelRequest request);

    /**
     * 申请退款
     *
     * @param request 退款请求
     * @return 退款响应
     */
    RefundResponse refund(RefundRequest request);

    /**
     * 查询退款状态
     *
     * @param query 退款查询请求
     * @return 退款查询响应
     */
    RefundQueryResponse queryRefund(RefundQuery query);

    /**
     * 处理支付回调
     *
     * @param notification 支付回调通知
     * @return 回调处理结果
     */
    NotificationResult handleNotification(PaymentNotification notification);

    /**
     * 验证支付回调签名
     *
     * @param notification 支付回调通知
     * @return 验证结果
     */
    boolean verifyNotification(PaymentNotification notification);

    /**
     * 获取支付提供商类型
     *
     * @return 支付提供商类型
     */
    PaymentProvider getProvider();

    /**
     * 检查支付服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}
