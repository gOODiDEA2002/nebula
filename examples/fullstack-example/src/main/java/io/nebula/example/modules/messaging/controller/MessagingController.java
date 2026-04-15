package io.nebula.example.modules.messaging.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.messaging.entity.dto.GetMessageStatsDto;
import io.nebula.example.modules.messaging.entity.dto.SendBatchNotificationDto;
import io.nebula.example.modules.messaging.entity.dto.SendOrderNotificationDto;
import io.nebula.example.modules.messaging.entity.dto.SendOrderStatusUpdateDto;
import io.nebula.example.modules.messaging.service.MessagingDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 消息传递演示控制器
 * 演示 Nebula 消息传递层的完整功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/messaging")
@RequiredArgsConstructor
@Validated
@Tag(name = "消息传递演示", description = "Nebula 消息传递层功能演示API")
@ConditionalOnProperty(prefix = "nebula.messaging.rabbitmq", name = "enabled", havingValue = "true")
public class MessagingController {
    
    private final MessagingDemoService messagingDemoService;
    
    @Operation(summary = "发送订单通知", description = "发送订单创建/更新/完成通知消息")
    @PostMapping("/order/notification")
    public Result<SendOrderNotificationDto.Response> sendOrderNotification(
            @Valid @RequestBody SendOrderNotificationDto.Request request) {
        log.info("接收发送订单通知请求: orderId={}, type={}", 
            request.getOrderId(), request.getNotificationType());
        
        SendOrderNotificationDto.Response response = messagingDemoService.sendOrderNotification(request);
        
        if (response.getSuccess()) {
            return Result.success(response, "订单通知发送成功");
        } else {
            return Result.error("MESSAGE_SEND_FAILED", "订单通知发送失败: " + response.getErrorMessage());
        }
    }
    
    @Operation(summary = "发送订单状态更新通知", description = "发送订单状态变更通知消息，支持同步/异步发送")
    @PostMapping("/order/status-update")
    public Result<SendOrderStatusUpdateDto.Response> sendOrderStatusUpdate(
            @Valid @RequestBody SendOrderStatusUpdateDto.Request request) {
        log.info("接收发送订单状态更新通知请求: orderId={}, {} -> {}, async={}", 
            request.getOrderId(), request.getOldStatus(), request.getNewStatus(), request.getAsync());
        
        SendOrderStatusUpdateDto.Response response = messagingDemoService.sendOrderStatusUpdate(request);
        
        if (response.getSuccess()) {
            return Result.success(response, "订单状态更新通知发送成功");
        } else {
            return Result.error("MESSAGE_SEND_FAILED", "订单状态更新通知发送失败: " + response.getErrorMessage());
        }
    }
    
    @Operation(summary = "发送批量通知", description = "批量发送订单通知消息")
    @PostMapping("/order/batch-notification")
    public Result<SendBatchNotificationDto.Response> sendBatchNotification(
            @Valid @RequestBody SendBatchNotificationDto.Request request) {
        log.info("接收发送批量通知请求: count={}", request.getNotifications().size());
        
        SendBatchNotificationDto.Response response = messagingDemoService.sendBatchNotification(request);
        
        return Result.success(response, String.format(
            "批量通知发送完成: 总数=%d, 成功=%d, 失败=%d", 
            response.getTotalCount(), response.getSuccessCount(), response.getFailedCount()));
    }
    
    @Operation(summary = "获取消息统计信息", description = "获取生产者和消费者的消息统计信息")
    @GetMapping("/stats")
    public Result<GetMessageStatsDto.Response> getMessageStats(
            @Valid GetMessageStatsDto.Request request) {
        log.info("接收获取消息统计信息请求: type={}", request.getStatsType());
        
        GetMessageStatsDto.Response response = messagingDemoService.getMessageStats(request);
        
        return Result.success(response, "获取消息统计信息成功");
    }
}

