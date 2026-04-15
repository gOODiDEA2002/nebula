package io.nebula.example.modules.notification.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.notification.entity.dto.SendSmsDto;
import io.nebula.example.modules.notification.entity.dto.SendVerificationCodeDto;
import io.nebula.notification.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final SmsService smsService;

    /**
     * 发送短信
     */
    @PostMapping("/sms/send")
    public Result<Map<String, Object>> sendSms(@RequestBody SendSmsDto dto) {
        boolean success = smsService.send(dto.getPhone(), dto.getTemplate(),
                dto.getParams() != null ? dto.getParams().toArray(new String[0]) : new String[0]);

        return Result.success(Map.of(
                "success", success,
                "phone", dto.getPhone(),
                "template", dto.getTemplate()
        ));
    }

    /**
     * 发送验证码
     */
    @PostMapping("/sms/verification-code")
    public Result<Map<String, Object>> sendVerificationCode(@RequestBody SendVerificationCodeDto dto) {
        boolean success = smsService.sendVerificationCode(dto.getPhone(), dto.getCode());

        return Result.success(Map.of(
                "success", success,
                "phone", dto.getPhone()
        ));
    }
}
