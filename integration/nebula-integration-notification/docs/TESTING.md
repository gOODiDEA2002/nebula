# nebula-integration-notification 模块单元测试清单

## 模块说明

集成通知服务模块，提供短信（SMS）发送功能，目前主要实现了阿里云短信服务。

## 核心功能

1. 发送短信验证码
2. 短信服务配置
3. 阿里云短信客户端集成

## 测试类清单

### 1. SmsServiceTest

**测试类路径**: `io.nebula.notification.sms.SmsService`  
**测试目的**: 验证短信发送逻辑

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSendVerificationCode() | sendVerificationCode() | 测试发送验证码 | Client (Aliyun SDK) |
| testSendSmsFailed() | sendSms() | 测试发送失败处理 | Client |

### 2. NotificationPropertiesTest

**测试类路径**: `io.nebula.notification.config.NotificationProperties`  
**测试目的**: 验证配置属性加载

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testPropertiesBinding() | - | 验证access-key等属性绑定 |

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|---------|---------|
| com.aliyun.dysmsapi20170525.Client | 发送短信 | Mock sendSmsWithOptions() |

### 避免真实发送
**测试必须Mock阿里云客户端，严禁在单元测试中真实发送短信，以免产生费用或被封禁。**

## 测试执行

```bash
mvn test -pl nebula/integration/nebula-integration-notification
```

## 验收标准

- 短信发送逻辑测试通过
- 异常处理逻辑测试通过
- Mock对象使用正确

