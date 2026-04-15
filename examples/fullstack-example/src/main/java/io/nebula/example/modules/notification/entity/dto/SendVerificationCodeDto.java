package io.nebula.example.modules.notification.entity.dto;

import lombok.Data;

/**
 * 发送验证码请求DTO
 */
@Data
public class SendVerificationCodeDto {
    /**
     * 手机号
     */
    private String phone;

    /**
     * 验证码
     */
    private String code;
}
