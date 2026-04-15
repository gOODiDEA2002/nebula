package io.nebula.example.modules.payment.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付请求DTO
 */
@Data
public class CreatePaymentDto {
    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 商品描述
     */
    private String subject;

    /**
     * 支付类型: WEB / APP / QR_CODE / H5
     */
    private String paymentType;
}
