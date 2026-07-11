package io.nebula.example.modules.payment.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.AssertTrue;

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

    @AssertTrue(message = "商户订单号和第三方交易号至少填写一项")
    public boolean isIdentifierPresent() {
        return (outTradeNo != null && !outTradeNo.isBlank()) || (tradeNo != null && !tradeNo.isBlank());
    }
}
