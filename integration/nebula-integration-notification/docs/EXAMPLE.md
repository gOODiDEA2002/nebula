# Nebula Integration Notification - 使用示例

> 通知集成完整使用指南，以票务系统通知为例

## 目录

- [快速开始](#快速开始)
- [短信通知](#短信通知)
- [邮件通知](#邮件通知)
- [推送通知](#推送通知)
- [站内消息](#站内消息)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-integration-notification</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  notification:
    enabled: true
    
    # 短信配置
    sms:
      provider: aliyun  # 支持: aliyun, tencent
      aliyun:
        access-key-id: ${ALIYUN_ACCESS_KEY_ID}
        access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
        endpoint: dysmsapi.aliyuncs.com
        sign-name: "票务系统"
        templates:
          verify-code: SMS_123456789       # 验证码模板
          order-confirm: SMS_234567890     # 订单确认模板
          order-cancel: SMS_345678901      # 订单取消模板
          refund-success: SMS_456789012    # 退款成功模板
    
    # 邮件配置
    email:
      enabled: true
      host: smtp.example.com
      port: 465
      username: ${MAIL_USERNAME}
      password: ${MAIL_PASSWORD}
      from: no-reply@ticket-system.com
      from-name: "票务系统"
      ssl: true
      templates-path: classpath:mail-templates/
    
    # 推送配置
    push:
      enabled: true
      providers:
        - type: fcm  # Firebase Cloud Messaging
          server-key: ${FCM_SERVER_KEY}
        - type: apns  # Apple Push Notification Service
          certificate: ${APNS_CERTIFICATE}
          password: ${APNS_PASSWORD}
    
    # 站内消息配置
    internal:
      enabled: true
      max-unread: 100
      retention-days: 90
```

---

## 短信通知

### 1. 发送验证码

```java
/**
 * 短信通知服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationService {
    
    private final SmsService smsService;
    private final CacheManager cacheManager;
    
    /**
     * 发送登录验证码
     */
    public void sendLoginVerificationCode(String phoneNumber) {
        log.info("发送登录验证码：phone={}", phoneNumber);
        
        // 1. 检查发送频率
        String rateLimitKey = "sms:rate:" + phoneNumber;
        if (cacheManager.hasKey(rateLimitKey)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }
        
        // 2. 生成6位验证码
        String code = generateVerificationCode();
        
        // 3. 发送短信
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        
        SmsRequest request = SmsRequest.builder()
                .phoneNumber(phoneNumber)
                .templateCode("verify-code")
                .params(params)
                .build();
        
        SmsResponse response = smsService.send(request);
        
        if (response.isSuccess()) {
            log.info("验证码发送成功：phone={}", phoneNumber);
            
            // 4. 存储验证码（5分钟有效）
            String codeKey = "sms:code:" + phoneNumber;
            cacheManager.set(codeKey, code, Duration.ofMinutes(5));
            
            // 5. 设置发送频率限制（60秒）
            cacheManager.set(rateLimitKey, "1", Duration.ofSeconds(60));
        } else {
            log.error("验证码发送失败：{}", response.getErrorMessage());
            throw new BusinessException("验证码发送失败");
        }
    }
    
    /**
     * 验证验证码
     */
    public boolean verifyCode(String phoneNumber, String code) {
        String codeKey = "sms:code:" + phoneNumber;
        String storedCode = cacheManager.get(codeKey, String.class);
        
        if (storedCode == null) {
            return false;
        }
        
        boolean valid = storedCode.equals(code);
        
        if (valid) {
            // 验证成功后删除验证码
            cacheManager.delete(codeKey);
        }
        
        return valid;
    }
    
    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
}
```

### 2. 订单确认短信

```java
/**
 * 发送订单确认短信
 */
public void sendOrderConfirmation(Order order) {
    log.info("发送订单确认短信：orderNo={}", order.getOrderNo());
    
    Map<String, String> params = new HashMap<>();
    params.put("orderNo", order.getOrderNo());
    params.put("eventName", order.getShowtimeTitle());
    params.put("showTime", formatShowTime(order.getShowTime()));
    params.put("quantity", String.valueOf(order.getQuantity()));
    params.put("amount", order.getTotalAmount().toString());
    
    SmsRequest request = SmsRequest.builder()
            .phoneNumber(order.getUserPhone())
            .templateCode("order-confirm")
            .params(params)
            .build();
    
    SmsResponse response = smsService.send(request);
    
    if (response.isSuccess()) {
        log.info("订单确认短信发送成功：orderNo={}", order.getOrderNo());
        
        // 记录通知历史
        saveNotificationRecord(order.getUserId(), NotificationType.SMS, 
                "订单确认", NotificationStatus.SUCCESS);
    } else {
        log.error("订单确认短信发送失败：{}", response.getErrorMessage());
        
        saveNotificationRecord(order.getUserId(), NotificationType.SMS, 
                "订单确认", NotificationStatus.FAILED);
    }
}
```

### 3. 退款通知短信

```java
/**
 * 发送退款成功短信
 */
public void sendRefundSuccessNotification(String userId, String orderNo, 
                                         BigDecimal refundAmount) {
    log.info("发送退款成功短信：orderNo={}, amount={}", orderNo, refundAmount);
    
    // 查询用户手机号
    User user = userService.getById(userId);
    
    Map<String, String> params = new HashMap<>();
    params.put("orderNo", orderNo);
    params.put("amount", refundAmount.toString());
    
    SmsRequest request = SmsRequest.builder()
            .phoneNumber(user.getPhoneNumber())
            .templateCode("refund-success")
            .params(params)
            .build();
    
    smsService.send(request);
}
```

---

## 邮件通知

### 1. 订单详情邮件

```java
/**
 * 邮件通知服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {
    
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    
    /**
     * 发送订单详情邮件
     */
    public void sendOrderDetailsEmail(Order order) {
        log.info("发送订单详情邮件：orderNo={}", order.getOrderNo());
        
        // 1. 构建邮件内容
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", order.getUserName());
        variables.put("orderNo", order.getOrderNo());
        variables.put("eventName", order.getShowtimeTitle());
        variables.put("showTime", order.getShowTime());
        variables.put("venue", order.getVenue());
        variables.put("seats", order.getSeats());
        variables.put("quantity", order.getQuantity());
        variables.put("totalAmount", order.getTotalAmount());
        variables.put("qrCodeUrl", generateQRCodeUrl(order.getOrderNo()));
        
        // 2. 渲染邮件模板
        String htmlContent = templateEngine.process("order-details", variables);
        
        // 3. 发送邮件
        EmailRequest request = EmailRequest.builder()
                .to(order.getUserEmail())
                .subject(String.format("订单确认 - %s", order.getShowtimeTitle()))
                .htmlContent(htmlContent)
                .build();
        
        EmailResponse response = emailService.send(request);
        
        if (response.isSuccess()) {
            log.info("订单详情邮件发送成功：orderNo={}", order.getOrderNo());
        } else {
            log.error("订单详情邮件发送失败：{}", response.getErrorMessage());
        }
    }
    
    /**
     * 发送电子票邮件
     */
    public void sendElectronicTicketsEmail(Order order, List<Ticket> tickets) {
        log.info("发送电子票邮件：orderNo={}, ticketCount={}", 
                order.getOrderNo(), tickets.size());
        
        // 1. 构建邮件内容
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", order.getUserName());
        variables.put("eventName", order.getShowtimeTitle());
        variables.put("showTime", order.getShowTime());
        variables.put("venue", order.getVenue());
        variables.put("tickets", tickets);
        
        String htmlContent = templateEngine.process("electronic-tickets", variables);
        
        // 2. 准备附件（电子票PDF）
        List<EmailAttachment> attachments = new ArrayList<>();
        
        for (Ticket ticket : tickets) {
            byte[] pdfData = ticketService.generateTicketPdf(ticket);
            
            EmailAttachment attachment = EmailAttachment.builder()
                    .filename(String.format("ticket-%s.pdf", ticket.getTicketNo()))
                    .content(pdfData)
                    .contentType("application/pdf")
                    .build();
            
            attachments.add(attachment);
        }
        
        // 3. 发送邮件
        EmailRequest request = EmailRequest.builder()
                .to(order.getUserEmail())
                .subject(String.format("电子票 - %s", order.getShowtimeTitle()))
                .htmlContent(htmlContent)
                .attachments(attachments)
                .build();
        
        emailService.send(request);
    }
    
    private String generateQRCodeUrl(String orderNo) {
        return String.format("https://ticket-system.com/qrcode/%s", orderNo);
    }
}
```

### 2. 演出提醒邮件

```java
/**
 * 发送演出提醒邮件
 */
public void sendShowReminderEmail(Order order) {
    log.info("发送演出提醒邮件：orderNo={}", order.getOrderNo());
    
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", order.getUserName());
    variables.put("eventName", order.getShowtimeTitle());
    variables.put("showTime", order.getShowTime());
    variables.put("venue", order.getVenue());
    variables.put("seats", order.getSeats());
    variables.put("notice", "请提前1小时到场，凭电子票二维码入场");
    
    String htmlContent = templateEngine.process("show-reminder", variables);
    
    EmailRequest request = EmailRequest.builder()
            .to(order.getUserEmail())
            .subject(String.format("演出提醒 - %s", order.getShowtimeTitle()))
            .htmlContent(htmlContent)
            .build();
    
    emailService.send(request);
}
```

---

## 推送通知

### 1. APP推送通知

```java
/**
 * 推送通知服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    
    private final PushService pushService;
    private final UserDeviceRepository userDeviceRepository;
    
    /**
     * 推送订单支付成功通知
     */
    public void pushOrderPaidNotification(String userId, Order order) {
        log.info("推送订单支付成功通知：userId={}, orderNo={}", userId, order.getOrderNo());
        
        // 1. 查询用户设备Token
        List<UserDevice> devices = userDeviceRepository.findByUserId(userId);
        
        if (devices.isEmpty()) {
            log.info("用户没有注册设备：userId={}", userId);
            return;
        }
        
        // 2. 构建推送消息
        PushMessage message = PushMessage.builder()
                .title("支付成功")
                .body(String.format("您的订单已支付成功，演出：%s", order.getShowtimeTitle()))
                .data(Map.of(
                        "type", "ORDER_PAID",
                        "orderNo", order.getOrderNo(),
                        "orderId", order.getId()
                ))
                .build();
        
        // 3. 批量推送
        for (UserDevice device : devices) {
            PushRequest request = PushRequest.builder()
                    .deviceToken(device.getDeviceToken())
                    .platform(device.getPlatform())
                    .message(message)
                    .build();
            
            try {
                PushResponse response = pushService.push(request);
                
                if (response.isSuccess()) {
                    log.info("推送成功：userId={}, deviceId={}", userId, device.getId());
                } else {
                    log.error("推送失败：{}", response.getErrorMessage());
                }
            } catch (Exception e) {
                log.error("推送异常：userId={}, deviceId={}", userId, device.getId(), e);
            }
        }
    }
    
    /**
     * 推送演出开始提醒
     */
    public void pushShowStartReminder(String userId, Order order) {
        log.info("推送演出开始提醒：userId={}, orderNo={}", userId, order.getOrderNo());
        
        List<UserDevice> devices = userDeviceRepository.findByUserId(userId);
        
        PushMessage message = PushMessage.builder()
                .title("演出即将开始")
                .body(String.format("您预订的演出《%s》将在1小时后开始，请提前入场", 
                        order.getShowtimeTitle()))
                .data(Map.of(
                        "type", "SHOW_REMINDER",
                        "orderNo", order.getOrderNo(),
                        "showTime", order.getShowTime().toString()
                ))
                .sound("reminder.wav")
                .badge(1)
                .build();
        
        for (UserDevice device : devices) {
            PushRequest request = PushRequest.builder()
                    .deviceToken(device.getDeviceToken())
                    .platform(device.getPlatform())
                    .message(message)
                    .build();
            
            pushService.push(request);
        }
    }
    
    /**
     * 推送退款成功通知
     */
    public void pushRefundSuccessNotification(String userId, String orderNo, 
                                             BigDecimal refundAmount) {
        log.info("推送退款成功通知：userId={}, orderNo={}", userId, orderNo);
        
        List<UserDevice> devices = userDeviceRepository.findByUserId(userId);
        
        PushMessage message = PushMessage.builder()
                .title("退款成功")
                .body(String.format("您的订单已退款成功，金额：¥%.2f", refundAmount))
                .data(Map.of(
                        "type", "REFUND_SUCCESS",
                        "orderNo", orderNo,
                        "amount", refundAmount.toString()
                ))
                .build();
        
        for (UserDevice device : devices) {
            PushRequest request = PushRequest.builder()
                    .deviceToken(device.getDeviceToken())
                    .platform(device.getPlatform())
                    .message(message)
                    .build();
            
            pushService.push(request);
        }
    }
}
```

---

## 站内消息

### 1. 系统消息

```java
/**
 * 站内消息服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InternalMessageService {
    
    private final MessageRepository messageRepository;
    private final MessageNotifier messageNotifier;
    
    /**
     * 发送系统消息
     */
    public void sendSystemMessage(String userId, String title, String content) {
        log.info("发送系统消息：userId={}, title={}", userId, title);
        
        InternalMessage message = new InternalMessage();
        message.setUserId(userId);
        message.setType(MessageType.SYSTEM);
        message.setTitle(title);
        message.setContent(content);
        message.setStatus(MessageStatus.UNREAD);
        message.setCreateTime(LocalDateTime.now());
        
        messageRepository.save(message);
        
        // 实时通知（WebSocket）
        messageNotifier.notifyUser(userId, message);
    }
    
    /**
     * 发送订单消息
     */
    public void sendOrderMessage(String userId, Order order, String content) {
        log.info("发送订单消息：userId={}, orderNo={}", userId, order.getOrderNo());
        
        InternalMessage message = new InternalMessage();
        message.setUserId(userId);
        message.setType(MessageType.ORDER);
        message.setTitle("订单通知");
        message.setContent(content);
        message.setRelatedId(order.getId());
        message.setRelatedType("ORDER");
        message.setStatus(MessageStatus.UNREAD);
        message.setCreateTime(LocalDateTime.now());
        
        messageRepository.save(message);
        messageNotifier.notifyUser(userId, message);
    }
    
    /**
     * 查询未读消息数量
     */
    public long getUnreadCount(String userId) {
        return messageRepository.countByUserIdAndStatus(userId, MessageStatus.UNREAD);
    }
    
    /**
     * 标记消息为已读
     */
    public void markAsRead(String userId, String messageId) {
        InternalMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此消息");
        }
        
        message.setStatus(MessageStatus.READ);
        message.setReadTime(LocalDateTime.now());
        
        messageRepository.save(message);
    }
    
    /**
     * 标记所有消息为已读
     */
    public void markAllAsRead(String userId) {
        List<InternalMessage> messages = messageRepository
                .findByUserIdAndStatus(userId, MessageStatus.UNREAD);
        
        for (InternalMessage message : messages) {
            message.setStatus(MessageStatus.READ);
            message.setReadTime(LocalDateTime.now());
        }
        
        messageRepository.saveAll(messages);
    }
}
```

---

## 票务系统完整示例

### 完整的通知流程

```java
/**
 * 票务通知服务（完整流程）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketNotificationService {
    
    private final SmsNotificationService smsService;
    private final EmailNotificationService emailService;
    private final PushNotificationService pushService;
    private final InternalMessageService messageService;
    
    /**
     * 1. 订单创建通知
     */
    public void notifyOrderCreated(Order order) {
        log.info("发送订单创建通知：orderNo={}", order.getOrderNo());
        
        try {
            // 站内消息
            messageService.sendOrderMessage(
                    order.getUserId(),
                    order,
                    String.format("订单已创建，请在30分钟内完成支付。订单号：%s", order.getOrderNo())
            );
            
            // 短信通知（可选，根据用户设置）
            if (order.getUserSettings().isSmsEnabled()) {
                // 发送订单创建短信
            }
            
        } catch (Exception e) {
            log.error("订单创建通知失败", e);
        }
    }
    
    /**
     * 2. 支付成功通知
     */
    public void notifyPaymentSuccess(Order order) {
        log.info("发送支付成功通知：orderNo={}", order.getOrderNo());
        
        try {
            // 1. 短信通知
            CompletableFuture.runAsync(() -> 
                    smsService.sendOrderConfirmation(order));
            
            // 2. 邮件通知
            CompletableFuture.runAsync(() -> 
                    emailService.sendOrderDetailsEmail(order));
            
            // 3. APP推送
            CompletableFuture.runAsync(() -> 
                    pushService.pushOrderPaidNotification(order.getUserId(), order));
            
            // 4. 站内消息
            messageService.sendOrderMessage(
                    order.getUserId(),
                    order,
                    String.format("支付成功！您已成功购买《%s》的门票。", order.getShowtimeTitle())
            );
            
            log.info("支付成功通知发送完成：orderNo={}", order.getOrderNo());
            
        } catch (Exception e) {
            log.error("支付成功通知失败", e);
        }
    }
    
    /**
     * 3. 电子票生成通知
     */
    public void notifyElectronicTicketsGenerated(Order order, List<Ticket> tickets) {
        log.info("发送电子票生成通知：orderNo={}, ticketCount={}", 
                order.getOrderNo(), tickets.size());
        
        try {
            // 1. 邮件发送电子票
            CompletableFuture.runAsync(() -> 
                    emailService.sendElectronicTicketsEmail(order, tickets));
            
            // 2. APP推送
            CompletableFuture.runAsync(() -> {
                PushMessage message = PushMessage.builder()
                        .title("电子票已生成")
                        .body(String.format("您的电子票已生成，共%d张", tickets.size()))
                        .data(Map.of(
                                "type", "TICKETS_GENERATED",
                                "orderNo", order.getOrderNo(),
                                "ticketCount", tickets.size()
                        ))
                        .build();
                
                List<UserDevice> devices = userDeviceRepository
                        .findByUserId(order.getUserId());
                
                for (UserDevice device : devices) {
                    PushRequest request = PushRequest.builder()
                            .deviceToken(device.getDeviceToken())
                            .platform(device.getPlatform())
                            .message(message)
                            .build();
                    
                    pushService.push(request);
                }
            });
            
            // 3. 站内消息
            messageService.sendOrderMessage(
                    order.getUserId(),
                    order,
                    String.format("您的电子票已生成，请在"我的订单"中查看。")
            );
            
        } catch (Exception e) {
            log.error("电子票生成通知失败", e);
        }
    }
    
    /**
     * 4. 演出提醒通知
     */
    public void notifyShowReminder(Order order) {
        log.info("发送演出提醒通知：orderNo={}", order.getOrderNo());
        
        try {
            // 1. 短信提醒
            CompletableFuture.runAsync(() -> {
                Map<String, String> params = new HashMap<>();
                params.put("eventName", order.getShowtimeTitle());
                params.put("showTime", formatShowTime(order.getShowTime()));
                params.put("venue", order.getVenue());
                
                SmsRequest request = SmsRequest.builder()
                        .phoneNumber(order.getUserPhone())
                        .templateCode("show-reminder")
                        .params(params)
                        .build();
                
                smsService.send(request);
            });
            
            // 2. 邮件提醒
            CompletableFuture.runAsync(() -> 
                    emailService.sendShowReminderEmail(order));
            
            // 3. APP推送
            CompletableFuture.runAsync(() -> 
                    pushService.pushShowStartReminder(order.getUserId(), order));
            
            // 4. 站内消息
            messageService.sendOrderMessage(
                    order.getUserId(),
                    order,
                    String.format("您预订的演出《%s》将在1小时后开始，请提前入场。", 
                            order.getShowtimeTitle())
            );
            
        } catch (Exception e) {
            log.error("演出提醒通知失败", e);
        }
    }
    
    /**
     * 5. 退款成功通知
     */
    public void notifyRefundSuccess(Order order, BigDecimal refundAmount) {
        log.info("发送退款成功通知：orderNo={}, amount={}", 
                order.getOrderNo(), refundAmount);
        
        try {
            // 1. 短信通知
            CompletableFuture.runAsync(() -> 
                    smsService.sendRefundSuccessNotification(
                            order.getUserId(), order.getOrderNo(), refundAmount));
            
            // 2. 邮件通知
            CompletableFuture.runAsync(() -> {
                Map<String, Object> variables = new HashMap<>();
                variables.put("userName", order.getUserName());
                variables.put("orderNo", order.getOrderNo());
                variables.put("refundAmount", refundAmount);
                variables.put("refundTime", LocalDateTime.now());
                
                String htmlContent = templateEngine.process("refund-success", variables);
                
                EmailRequest request = EmailRequest.builder()
                        .to(order.getUserEmail())
                        .subject("退款成功通知")
                        .htmlContent(htmlContent)
                        .build();
                
                emailService.send(request);
            });
            
            // 3. APP推送
            CompletableFuture.runAsync(() -> 
                    pushService.pushRefundSuccessNotification(
                            order.getUserId(), order.getOrderNo(), refundAmount));
            
            // 4. 站内消息
            messageService.sendOrderMessage(
                    order.getUserId(),
                    order,
                    String.format("退款成功！金额¥%.2f已原路退回，请注意查收。", refundAmount)
            );
            
        } catch (Exception e) {
            log.error("退款成功通知失败", e);
        }
    }
    
    /**
     * 6. 订单取消通知
     */
    public void notifyOrderCancelled(Order order, String reason) {
        log.info("发送订单取消通知：orderNo={}, reason={}", order.getOrderNo(), reason);
        
        try {
            // 1. 短信通知
            CompletableFuture.runAsync(() -> {
                Map<String, String> params = new HashMap<>();
                params.put("orderNo", order.getOrderNo());
                params.put("reason", reason);
                
                SmsRequest request = SmsRequest.builder()
                        .phoneNumber(order.getUserPhone())
                        .templateCode("order-cancel")
                        .params(params)
                        .build();
                
                smsService.send(request);
            });
            
            // 2. APP推送
            CompletableFuture.runAsync(() -> {
                PushMessage message = PushMessage.builder()
                        .title("订单已取消")
                        .body(String.format("您的订单已取消。原因：%s", reason))
                        .data(Map.of(
                                "type", "ORDER_CANCELLED",
                                "orderNo", order.getOrderNo(),
                                "reason", reason
                        ))
                        .build();
                
                List<UserDevice> devices = userDeviceRepository
                        .findByUserId(order.getUserId());
                
                for (UserDevice device : devices) {
                    PushRequest request = PushRequest.builder()
                            .deviceToken(device.getDeviceToken())
                            .platform(device.getPlatform())
                            .message(message)
                            .build();
                    
                    pushService.push(request);
                }
            });
            
            // 3. 站内消息
            messageService.sendOrderMessage(
                    order.getUserId(),
                    order,
                    String.format("您的订单已取消。原因：%s", reason)
            );
            
        } catch (Exception e) {
            log.error("订单取消通知失败", e);
        }
    }
    
    // 辅助方法
    
    private String formatShowTime(LocalDateTime showTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
        return showTime.format(formatter);
    }
}
```

### 定时任务：演出提醒

```java
/**
 * 演出提醒定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShowReminderTask {
    
    private final OrderRepository orderRepository;
    private final TicketNotificationService notificationService;
    
    /**
     * 每小时检查即将开始的演出
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void sendShowReminders() {
        log.info("开始执行演出提醒任务");
        
        // 查询未来1-2小时内的订单
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(2);
        
        List<Order> orders = orderRepository.findOrdersWithShowTimeBetween(
                startTime, endTime, OrderStatus.PAID);
        
        log.info("找到{}个需要提醒的订单", orders.size());
        
        for (Order order : orders) {
            try {
                // 检查是否已发送提醒
                if (!isReminderSent(order.getId())) {
                    notificationService.notifyShowReminder(order);
                    markReminderSent(order.getId());
                }
            } catch (Exception e) {
                log.error("发送演出提醒失败：orderNo={}", order.getOrderNo(), e);
            }
        }
        
        log.info("演出提醒任务执行完成");
    }
    
    private boolean isReminderSent(String orderId) {
        String key = "reminder:sent:" + orderId;
        return redisTemplate.hasKey(key);
    }
    
    private void markReminderSent(String orderId) {
        String key = "reminder:sent:" + orderId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofDays(1));
    }
}
```

---

## 最佳实践

### 1. 通知策略

- **多渠道通知**：重要通知通过多个渠道发送（短信+邮件+推送）
- **用户偏好**：尊重用户的通知设置和偏好
- **频率控制**：避免频繁发送通知，设置合理的频率限制
- **时段控制**：避免在深夜发送非紧急通知

### 2. 异步处理

- **异步发送**：通知发送使用异步处理，不阻塞主流程
- **重试机制**：失败自动重试，最多重试3次
- **降级处理**：某个渠道失败不影响其他渠道
- **超时控制**：设置合理的超时时间

### 3. 性能优化

- **批量发送**：批量发送通知，提高效率
- **连接池**：复用HTTP连接，减少开销
- **缓存模板**：缓存邮件和短信模板，减少IO
- **限流保护**：防止通知发送过载

### 4. 监控告警

- **发送统计**：统计各渠道发送成功率
- **失败告警**：发送失败率超过阈值时告警
- **延迟监控**：监控通知发送延迟
- **成本监控**：监控短信和推送成本

### 5. 安全合规

- **用户同意**：发送营销类通知需获得用户同意
- **退订机制**：提供通知退订功能
- **隐私保护**：不在通知中泄露敏感信息
- **审计日志**：记录所有通知发送日志

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [TESTING.md](./TESTING.md) - 测试指南

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0
