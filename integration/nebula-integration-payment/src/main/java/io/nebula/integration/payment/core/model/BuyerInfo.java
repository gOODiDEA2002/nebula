package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 买家信息模型
 *
 * @author nebula
 */
@Data
@Builder
public class BuyerInfo {

    /**
     * 买家用户ID
     */
    private String buyerId;

    /**
     * 买家账号
     */
    private String buyerAccount;

    /**
     * 买家姓名
     */
    private String buyerName;

    /**
     * 买家邮箱
     */
    private String buyerEmail;

    /**
     * 买家手机号
     */
    private String buyerPhone;

    /**
     * 买家身份证号
     */
    private String buyerIdCard;

    /**
     * 买家地址
     */
    private String buyerAddress;

    /**
     * 买家OpenID（微信）
     */
    private String openId;

    /**
     * 买家Sub OpenID（微信小程序）
     */
    private String subOpenId;
}
