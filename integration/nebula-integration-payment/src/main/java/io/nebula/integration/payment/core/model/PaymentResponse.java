package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付响应模型
 *
 * @author nebula
 */
@Data
@Builder
public class PaymentResponse {

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
     * 支付链接（网页支付）
     */
    private String payUrl;

    /**
     * 二维码内容（扫码支付）
     */
    private String qrCode;

    /**
     * 支付表单（H5支付）
     */
    private String payForm;

    /**
     * 支付参数（APP支付、小程序支付等）
     */
    private Map<String, Object> payParams;

    /**
     * 预支付ID
     */
    private String prepayId;

    /**
     * 支付创建时间
     */
    private LocalDateTime createTime;

    /**
     * 支付过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付提供商
     */
    private PaymentProvider provider;

    /**
     * 原始响应数据
     */
    private Map<String, Object> rawResponse;
}
