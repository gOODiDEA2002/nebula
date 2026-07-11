package io.nebula.example.modules.payment.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建支付请求DTO
 */
@Data
public class CreatePaymentDto {
    /**
     * 商户订单号
     */
    @NotBlank(message = "商户订单号不能为空")
    private String outTradeNo;

    /**
     * 交易金额
     */
    @NotNull(message = "交易金额不能为空")
    @DecimalMin(value = "0.01", message = "交易金额必须大于0")
    private BigDecimal amount;

    /**
     * 商品描述
     */
    @NotBlank(message = "商品描述不能为空")
    private String subject;

    /**
     * 支付类型: WEB / APP / QR_CODE / H5
     */
    private String paymentType;
}
