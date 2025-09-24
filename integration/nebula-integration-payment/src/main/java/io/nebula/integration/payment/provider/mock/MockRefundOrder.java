package io.nebula.integration.payment.provider.mock;

import io.nebula.integration.payment.core.model.RefundStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mock 退款订单模型
 *
 * @author nebula
 */
@Data
@Builder
public class MockRefundOrder {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 商户退款单号
     */
    private String outRefundNo;

    /**
     * Mock退款单号
     */
    private String refundNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款状态
     */
    private RefundStatus status;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;
}
