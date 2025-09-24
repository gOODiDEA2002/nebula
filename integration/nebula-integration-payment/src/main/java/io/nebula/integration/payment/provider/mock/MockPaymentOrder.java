package io.nebula.integration.payment.provider.mock;

import io.nebula.integration.payment.core.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mock 支付订单模型
 *
 * @author nebula
 */
@Data
@Builder
public class MockPaymentOrder {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * Mock交易号
     */
    private String tradeNo;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 货币类型
     */
    private String currency;

    /**
     * 商品描述
     */
    private String subject;

    /**
     * 支付状态
     */
    private PaymentStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}
