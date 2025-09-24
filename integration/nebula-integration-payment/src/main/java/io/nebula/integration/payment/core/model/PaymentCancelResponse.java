package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 取消支付响应模型
 *
 * @author nebula
 */
@Data
@Builder
public class PaymentCancelResponse {

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
     * 支付提供商
     */
    private PaymentProvider provider;
}
