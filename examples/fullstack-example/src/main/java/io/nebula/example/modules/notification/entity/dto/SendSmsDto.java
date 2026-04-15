package io.nebula.example.modules.notification.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 发送短信请求DTO
 */
@Data
public class SendSmsDto {
    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信模板
     */
    private String template;

    /**
     * 模板参数
     */
    private List<String> params;
}
