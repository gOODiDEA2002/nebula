package io.nebula.web.mask;

/**
 * 脱敏类型枚举
 * 
 * @author nebula
 */
public enum MaskType {
    
    /**
     * 手机号脱敏：138****8888
     */
    PHONE,
    
    /**
     * 身份证号脱敏：110***********1234
     */
    ID_CARD,
    
    /**
     * 邮箱脱敏：test***@example.com
     */
    EMAIL,
    
    /**
     * 姓名脱敏：张*
     */
    NAME,
    
    /**
     * 银行卡号脱敏：6222****1234
     */
    BANK_CARD,
    
    /**
     * 地址脱敏：北京市***区
     */
    ADDRESS,
    
    /**
     * 密码脱敏：******
     */
    PASSWORD,
    
    /**
     * IP地址脱敏：192.168.*.*
     */
    IP_ADDRESS,
    
    /**
     * 自定义脱敏策略
     */
    CUSTOM
}
