# Nebula Integration Notification 模块

## 模块简介

提供短信、邮件、推送等通知服务的统一接口和实现。

## 功能特性

- 短信发送(阿里云SMS)
- 验证码发送
- 短信限流

## 快速开始

```yaml
nebula:
  notification:
    enabled: true
    sms:
      access-key-id: your-key-id
      access-key-secret: your-key-secret
      sign-name: your-sign-name
```

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final SmsService smsService;
    
    public void sendCode(String phone) {
        String code = generateCode();
        smsService.sendVerificationCode(phone, code);
    }
}
```

## 许可证

Apache 2.0

