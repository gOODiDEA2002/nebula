package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 取消支付请求模型
 *
 * @author nebula
 */
@Data
@Builder
public class PaymentCancelRequest {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 取消原因
     */
    private String reason;

    /**
     * 支付提供商
     */
    private PaymentProvider provider;
}
