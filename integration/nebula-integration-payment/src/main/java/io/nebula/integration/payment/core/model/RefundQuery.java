package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 退款查询请求模型
 *
 * @author nebula
 */
@Data
@Builder
public class RefundQuery {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 商户退款单号
     */
    private String outRefundNo;

    /**
     * 支付提供商
     */
    private PaymentProvider provider;
}
