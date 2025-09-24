package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 支付查询请求模型
 *
 * @author nebula
 */
@Data
@Builder
public class PaymentQuery {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 支付提供商
     */
    private PaymentProvider provider;
}
