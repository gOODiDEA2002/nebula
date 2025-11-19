# nebula-integration-notification 模块示例

## 模块简介

`nebula-integration-notification` 模块提供了统一的通知服务接口，目前主要集成了阿里云短信服务 (Aliyun SMS)，用于发送验证码、通知短信等。

## 核心功能示例

### 1. 配置阿里云短信

在 `application.yml` 中配置阿里云 AccessKey 和短信签名信息。

**`application.yml`**:

```yaml
nebula:
  notification:
    enabled: true
    sms:
      aliyun:
        access-key-id: ${ALIYUN_ACCESS_KEY_ID}
        access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
        endpoint: dysmsapi.aliyuncs.com
        sign-name: "我的应用" # 短信签名
        template-code: "SMS_123456789" # 默认模版Code (可选)
```

### 2. 发送短信验证码

**`io.nebula.example.notification.service.AuthService`**:

```java
package io.nebula.example.notification.service;

import io.nebula.notification.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SmsService smsService;

    public void sendLoginCode(String phoneNumber) {
        // 生成6位验证码
        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        
        log.info("向手机号 {} 发送验证码: {}", phoneNumber, code);
        
        // 发送短信
        // 参数: 手机号, 验证码内容 (对应模版中的变量，如 ${code})
        smsService.sendVerificationCode(phoneNumber, code);
        
        // 实际项目中还需将 code 存入缓存用于后续验证
    }
}
```

### 3. 发送通用通知短信

如果需要发送包含多个变量的短信，可以使用通用的发送方法（假设 `SmsService` 提供了支持 Map 参数的方法，具体视接口定义而定）。

```java
public void sendOrderNotification(String phoneNumber, String orderId) {
    // Map<String, String> params = Map.of("orderId", orderId);
    // smsService.send(phoneNumber, "SMS_ORDER_NOTIFY", params);
}
```

## 总结

该模块封装了阿里云 SMS SDK 的底层细节，开发者只需注入 `SmsService` 即可轻松实现短信发送功能。

