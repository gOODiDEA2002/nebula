package io.nebula.example.modules.messaging.handler;

import io.nebula.example.modules.messaging.event.OrderNotificationEvent;
import io.nebula.example.modules.messaging.event.OrderStatusUpdateEvent;
import io.nebula.messaging.core.annotation.MessageListener;
import io.nebula.messaging.core.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单通知消息处理器
 * 演示如何使用 @MessageListener 注解自动注册消息处理器
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "nebula.messaging.rabbitmq", name = "enabled", havingValue = "true")
public class OrderNotificationHandler {
    
    /**
     * 处理订单通知消息
     * 
     * @param message 消息对象
     */
    @MessageListener(topic = "order.notification", queue = "order-notification-queue")
    public void handleOrderNotification(Message<OrderNotificationEvent> message) {
        OrderNotificationEvent event = message.getPayload();
        
        log.info("收到订单通知: orderId={}, orderNo={}, type={}, status={}", 
            event.getOrderId(), 
            event.getOrderNo(), 
            event.getNotificationType(), 
            event.getStatus());
        
        // 处理订单通知逻辑
        // 例如：发送邮件、短信通知、推送通知等
        
        try {
            // 模拟业务处理
            Thread.sleep(100);
            
            log.info("订单通知处理完成: orderId={}", event.getOrderId());
            
        } catch (InterruptedException e) {
            log.error("订单通知处理失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("订单通知处理失败", e);
        }
    }
    
    /**
     * 处理订单状态更新消息
     * 
     * @param message 消息对象
     */
    @MessageListener(topic = "order.status.update", concurrency = 3, maxRetries = 5)
    public void handleOrderStatusUpdate(Message<OrderStatusUpdateEvent> message) {
        OrderStatusUpdateEvent event = message.getPayload();
        
        log.info("收到订单状态更新: orderId={}, orderNo={}, {} -> {}", 
            event.getOrderId(), 
            event.getOrderNo(), 
            event.getOldStatus(), 
            event.getNewStatus());
        
        // 处理订单状态更新逻辑
        // 例如：更新订单缓存、通知相关服务、记录日志等
        
        try {
            // 模拟业务处理
            Thread.sleep(150);
            
            log.info("订单状态更新处理完成: orderId={}, newStatus={}", 
                event.getOrderId(), event.getNewStatus());
            
        } catch (InterruptedException e) {
            log.error("订单状态更新处理失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("订单状态更新处理失败", e);
        }
    }
    
    /**
     * 处理批量订单通知
     * 
     * @param message 消息对象
     */
    @MessageListener(topic = "order.notification.batch", queue = "order-batch-queue", concurrency = 5)
    public void handleBatchOrderNotification(Message<OrderNotificationEvent> message) {
        OrderNotificationEvent event = message.getPayload();
        
        log.debug("收到批量订单通知: orderId={}", event.getOrderId());
        
        // 处理批量通知逻辑
        try {
            // 模拟快速处理
            Thread.sleep(50);
            
        } catch (InterruptedException e) {
            log.error("批量订单通知处理失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量订单通知处理失败", e);
        }
    }
}

