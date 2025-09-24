package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款请求模型
 *
 * @author nebula
 */
@Data
@Builder
public class RefundRequest {

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
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String reason;

    /**
     * 支付提供商
     */
    private PaymentProvider provider;
}
