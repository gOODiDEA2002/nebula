package io.nebula.example.modules.notification.entity.dto;

import lombok.Data;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 发送短信请求DTO
 */
@Data
public class SendSmsDto {
    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 短信模板
     */
    @NotBlank(message = "短信模板不能为空")
    private String template;

    /**
     * 模板参数
     */
    private List<String> params;
}
