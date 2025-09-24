package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付请求模型
 *
 * @author nebula
 */
@Data
@Builder
public class PaymentRequest {

    /**
     * 商户订单号（必填）
     */
    private String outTradeNo;

    /**
     * 交易金额（必填）
     */
    private BigDecimal amount;

    /**
     * 货币类型（默认：CNY）
     */
    @Builder.Default
    private String currency = "CNY";

    /**
     * 商品描述（必填）
     */
    private String subject;

    /**
     * 商品详细描述
     */
    private String body;

    /**
     * 支付类型
     */
    private PaymentType paymentType;

    /**
     * 买家信息
     */
    private BuyerInfo buyer;

    /**
     * 支付超时时间
     */
    private LocalDateTime timeExpire;

    /**
     * 异步通知地址
     */
    private String notifyUrl;

    /**
     * 同步返回地址
     */
    private String returnUrl;

    /**
     * 取消支付返回地址
     */
    private String cancelUrl;

    /**
     * 附加数据（业务自定义）
     */
    private String attach;

    /**
     * 扩展参数
     */
    private Map<String, Object> extras;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 设备信息
     */
    private String deviceInfo;
}
