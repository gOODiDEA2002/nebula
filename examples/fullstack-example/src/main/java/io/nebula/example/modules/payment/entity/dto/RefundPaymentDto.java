package io.nebula.example.modules.payment.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 退款请求DTO
 */
@Data
public class RefundPaymentDto {
    /**
     * 商户订单号
     */
    @NotBlank(message = "商户订单号不能为空")
    private String outTradeNo;

    /**
     * 商户退款单号
     */
    @NotBlank(message = "商户退款单号不能为空")
    private String outRefundNo;

    /**
     * 退款金额
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String reason;
}
