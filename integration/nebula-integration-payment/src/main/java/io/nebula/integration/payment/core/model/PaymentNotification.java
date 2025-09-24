package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 支付通知模型
 *
 * @author nebula
 */
@Data
@Builder
public class PaymentNotification {

    /**
     * 通知类型
     */
    private NotificationType type;

    /**
     * 支付提供商
     */
    private PaymentProvider provider;

    /**
     * 原始通知数据
     */
    private Map<String, Object> rawData;

    /**
     * 签名
     */
    private String signature;

    /**
     * 签名类型
     */
    private String signType;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * 通知主体
     */
    private String body;

    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        REFUND_SUCCESS,
        REFUND_FAILED
    }
}
