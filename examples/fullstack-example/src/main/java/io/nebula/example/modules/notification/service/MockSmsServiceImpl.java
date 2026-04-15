package io.nebula.example.modules.notification.service;

import io.nebula.notification.sms.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Mock 短信服务实现
 * 仅打印日志模拟发送，用于演示和开发测试
 */
@Slf4j
@Service
public class MockSmsServiceImpl implements SmsService {

    @Override
    public boolean send(String phone, String template, String... params) {
        log.info("[Mock短信] 发送短信: phone={}, template={}, params={}, time={}",
                phone, template, Arrays.toString(params), LocalDateTime.now());
        return true;
    }

    @Override
    public boolean sendVerificationCode(String phone, String code) {
        log.info("[Mock短信] 发送验证码: phone={}, code={}, time={}",
                phone, code, LocalDateTime.now());
        return true;
    }
}
