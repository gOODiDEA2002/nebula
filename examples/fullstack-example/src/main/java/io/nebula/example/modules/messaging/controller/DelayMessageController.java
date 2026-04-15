package io.nebula.example.modules.messaging.controller;

import io.nebula.core.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 延时消息示例Controller
 * 
 * TODO: 延迟消息功能暂未实现，此类暂时提供占位接口
 * 待 nebula-messaging-rabbitmq 实现 DelayMessage 功能后重新启用
 * 
 * 原有功能说明：
 * - 订单超时取消（30分钟延时）
 * - 优惠券过期提醒（3天延时）
 * - 自定义延时通知
 *
 * @author Nebula Example
 */
@Slf4j
@RestController
@RequestMapping("/messaging/delay")
@ConditionalOnProperty(prefix = "nebula.messaging.rabbitmq", name = "enabled", havingValue = "true")
public class DelayMessageController {
    
    /**
     * 示例1: 订单超时取消（暂未实现）
     * 
     * GET /messaging/delay/order-timeout?orderId=1001
     */
    @GetMapping("/order-timeout")
    public Result<String> orderTimeout(@RequestParam Long orderId) {
        log.warn("延迟消息功能暂未实现");
        return Result.businessError("延迟消息功能暂未实现，敬请期待");
    }
    
    /**
     * 示例2: 优惠券过期提醒（暂未实现）
     * 
     * GET /messaging/delay/coupon-expire?couponId=2001&userId=1
     */
    @GetMapping("/coupon-expire")
    public Result<String> couponExpireReminder(
            @RequestParam Long couponId,
            @RequestParam Long userId) {
        log.warn("延迟消息功能暂未实现");
        return Result.businessError("延迟消息功能暂未实现，敬请期待");
    }
    
    /**
     * 示例3: 自定义延时通知（暂未实现）
     * 
     * POST /messaging/delay/custom
     */
    @PostMapping("/custom")
    public Result<String> customDelayMessage(@RequestBody CustomDelayRequest request) {
        log.warn("延迟消息功能暂未实现");
        return Result.businessError("延迟消息功能暂未实现，敬请期待");
    }
    
    /**
     * 自定义延时请求DTO（临时占位）
     */
    @lombok.Data
    public static class CustomDelayRequest {
        private String title;
        private String content;
        private Long delaySeconds;
    }
}
