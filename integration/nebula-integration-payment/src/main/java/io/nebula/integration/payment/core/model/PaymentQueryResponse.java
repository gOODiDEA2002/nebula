package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付查询响应模型
 *
 * @author nebula
 */
@Data
@Builder
public class PaymentQueryResponse {

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
     * 支付状态
     */
    private PaymentStatus status;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 实际支付金额
     */
    private BigDecimal paidAmount;

    /**
     * 货币类型
     */
    private String currency;

    /**
     * 商品描述
     */
    private String subject;

    /**
     * 买家信息
     */
    private BuyerInfo buyer;

    /**
     * 支付创建时间
     */
    private LocalDateTime createTime;

    /**
     * 支付成功时间
     */
    private LocalDateTime payTime;

    /**
     * 支付方式
     */
    private String payMethod;

    /**
     * 支付提供商
     */
    private PaymentProvider provider;

    /**
     * 原始响应数据
     */
    private Map<String, Object> rawResponse;
}
