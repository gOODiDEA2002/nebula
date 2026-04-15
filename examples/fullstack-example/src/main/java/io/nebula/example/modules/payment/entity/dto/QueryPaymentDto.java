package io.nebula.example.modules.payment.entity.dto;

import lombok.Data;

/**
 * 查询支付请求DTO
 */
@Data
public class QueryPaymentDto {
    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 第三方交易号
     */
    private String tradeNo;
}
