package io.nebula.example.modules.payment.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款请求DTO
 */
@Data
public class RefundPaymentDto {
    /**
     * 商户订单号
     */
    private String outTradeNo;

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
}
