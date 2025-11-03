package io.nebula.notification.sms;

/**
 * 短信服务接口
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface SmsService {
    
    /**
     * 发送短信
     */
    boolean send(String phone, String template, String... params);
    
    /**
     * 发送验证码
     */
    boolean sendVerificationCode(String phone, String code);
}

