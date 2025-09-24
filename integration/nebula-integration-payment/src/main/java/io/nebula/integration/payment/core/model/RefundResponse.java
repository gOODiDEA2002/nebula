package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款响应模型
 *
 * @author nebula
 */
@Data
@Builder
public class RefundResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 商户退款单号
     */
    private String outRefundNo;

    /**
     * 第三方退款单号
     */
    private String refundNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 支付提供商
     */
    private PaymentProvider provider;
}
