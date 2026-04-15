package io.nebula.example.modules.messaging.service.impl;

import io.nebula.example.modules.messaging.entity.dto.GetMessageStatsDto;
import io.nebula.example.modules.messaging.entity.dto.SendBatchNotificationDto;
import io.nebula.example.modules.messaging.entity.dto.SendOrderNotificationDto;
import io.nebula.example.modules.messaging.entity.dto.SendOrderStatusUpdateDto;
import io.nebula.example.modules.messaging.event.OrderNotificationEvent;
import io.nebula.example.modules.messaging.event.OrderStatusUpdateEvent;
import io.nebula.example.modules.messaging.service.MessagingDemoService;
import io.nebula.messaging.core.manager.MessageManager;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 消息传递演示服务实现
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "nebula.messaging.rabbitmq", name = "enabled", havingValue = "true")
public class MessagingDemoServiceImpl implements MessagingDemoService {
    
    private final MessageManager messageManager;
    
    @Override
    public SendOrderNotificationDto.Response sendOrderNotification(SendOrderNotificationDto.Request request) {
        log.info("开始发送订单通知: orderId={}, type={}", request.getOrderId(), request.getNotificationType());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建事件对象
            OrderNotificationEvent event = OrderNotificationEvent.builder()
                .orderId(request.getOrderId())
                .orderNo(request.getOrderNo())
                .userId(request.getUserId())
                .productName(request.getProductName())
                .amount(request.getAmount())
                .status(request.getStatus())
                .notificationType(request.getNotificationType())
                .eventTime(LocalDateTime.now())
                .build();
            
            // 构建消息
            Message<OrderNotificationEvent> message = Message.<OrderNotificationEvent>builder()
                .topic("order.notification")
                .queue("order-notification-queue")
                .payload(event)
                .build();
            
            // 发送消息
            MessageProducer.SendResult result = messageManager.getProducer().send(message);
            
            // 构建响应
            SendOrderNotificationDto.Response response = new SendOrderNotificationDto.Response();
            response.setMessageId(result.getMessageId());
            response.setSuccess(result.isSuccess());
            response.setElapsedTime(System.currentTimeMillis() - startTime);
            response.setErrorMessage(result.getErrorMessage());
            
            log.info("订单通知发送完成: messageId={}, success={}, elapsedTime={}ms", 
                result.getMessageId(), result.isSuccess(), response.getElapsedTime());
            
            return response;
            
        } catch (Exception e) {
            log.error("发送订单通知失败: orderId={}", request.getOrderId(), e);
            
            SendOrderNotificationDto.Response response = new SendOrderNotificationDto.Response();
            response.setSuccess(false);
            response.setElapsedTime(System.currentTimeMillis() - startTime);
            response.setErrorMessage(e.getMessage());
            
            return response;
        }
    }
    
    @Override
    public SendOrderStatusUpdateDto.Response sendOrderStatusUpdate(SendOrderStatusUpdateDto.Request request) {
        log.info("开始发送订单状态更新通知: orderId={}, {} -> {}", 
            request.getOrderId(), request.getOldStatus(), request.getNewStatus());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建事件对象
            OrderStatusUpdateEvent event = OrderStatusUpdateEvent.builder()
                .orderId(request.getOrderId())
                .orderNo(request.getOrderNo())
                .oldStatus(request.getOldStatus())
                .newStatus(request.getNewStatus())
                .eventTime(LocalDateTime.now())
                .remark(request.getRemark())
                .build();
            
            SendOrderStatusUpdateDto.Response response = new SendOrderStatusUpdateDto.Response();
            
            if (Boolean.TRUE.equals(request.getAsync())) {
                // 异步发送
                CompletableFuture<MessageProducer.SendResult> future = messageManager.getProducer()
                    .sendAsync("order.status.update", event);
                
                future.thenAccept(result -> {
                    log.info("订单状态更新通知异步发送完成: messageId={}, success={}", 
                        result.getMessageId(), result.isSuccess());
                });
                
                response.setMessageId("ASYNC_" + System.currentTimeMillis());
                response.setSuccess(true);
                response.setElapsedTime(System.currentTimeMillis() - startTime);
                
            } else {
                // 同步发送
                MessageProducer.SendResult result = messageManager.getProducer()
                    .send("order.status.update", event);
                
                response.setMessageId(result.getMessageId());
                response.setSuccess(result.isSuccess());
                response.setElapsedTime(System.currentTimeMillis() - startTime);
                response.setErrorMessage(result.getErrorMessage());
                
                log.info("订单状态更新通知发送完成: messageId={}, success={}, elapsedTime={}ms", 
                    result.getMessageId(), result.isSuccess(), response.getElapsedTime());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("发送订单状态更新通知失败: orderId={}", request.getOrderId(), e);
            
            SendOrderStatusUpdateDto.Response response = new SendOrderStatusUpdateDto.Response();
            response.setSuccess(false);
            response.setElapsedTime(System.currentTimeMillis() - startTime);
            response.setErrorMessage(e.getMessage());
            
            return response;
        }
    }
    
    @Override
    public SendBatchNotificationDto.Response sendBatchNotification(SendBatchNotificationDto.Request request) {
        log.info("开始发送批量通知: count={}", request.getNotifications().size());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建消息列表
            List<Message<OrderNotificationEvent>> messages = request.getNotifications().stream()
                .map(item -> {
                    OrderNotificationEvent event = OrderNotificationEvent.builder()
                        .orderId(item.getOrderId())
                        .orderNo(item.getOrderNo())
                        .userId(item.getUserId())
                        .productName(item.getProductName())
                        .amount(item.getAmount())
                        .status(item.getStatus())
                        .eventTime(LocalDateTime.now())
                        .build();
                    
                    return Message.<OrderNotificationEvent>builder()
                        .topic("order.notification.batch")
                        .payload(event)
                        .build();
                })
                .collect(Collectors.toList());
            
            // 批量发送
            MessageProducer.BatchSendResult result = messageManager.getProducer().sendBatch(messages);
            
            // 构建响应
            SendBatchNotificationDto.Response response = new SendBatchNotificationDto.Response();
            response.setTotalCount(result.getTotalCount());
            response.setSuccessCount(result.getSuccessCount());
            response.setFailedCount(result.getFailedCount());
            response.setElapsedTime(System.currentTimeMillis() - startTime);
            
            // 收集失败的消息ID
            List<String> failedMessageIds = result.getFailedResults().stream()
                .map(MessageProducer.SendResult::getMessageId)
                .collect(Collectors.toList());
            response.setFailedMessageIds(failedMessageIds);
            
            log.info("批量通知发送完成: total={}, success={}, failed={}, elapsedTime={}ms", 
                result.getTotalCount(), result.getSuccessCount(), result.getFailedCount(), 
                response.getElapsedTime());
            
            return response;
            
        } catch (Exception e) {
            log.error("发送批量通知失败", e);
            
            SendBatchNotificationDto.Response response = new SendBatchNotificationDto.Response();
            response.setTotalCount(request.getNotifications().size());
            response.setSuccessCount(0);
            response.setFailedCount(request.getNotifications().size());
            response.setElapsedTime(System.currentTimeMillis() - startTime);
            response.setFailedMessageIds(new ArrayList<>());
            
            return response;
        }
    }
    
    @Override
    public GetMessageStatsDto.Response getMessageStats(GetMessageStatsDto.Request request) {
        log.info("获取消息统计信息: type={}", request.getStatsType());
        
        GetMessageStatsDto.Response response = new GetMessageStatsDto.Response();
        
        try {
            // 获取生产者统计
            if ("PRODUCER".equals(request.getStatsType()) || "ALL".equals(request.getStatsType())) {
                MessageProducer.ProducerStats producerStats = messageManager.getProducer().getStats();
                
                GetMessageStatsDto.ProducerStatsVo producerStatsVo = new GetMessageStatsDto.ProducerStatsVo();
                producerStatsVo.setSentCount(producerStats.getSentCount());
                producerStatsVo.setSuccessCount(producerStats.getSuccessCount());
                producerStatsVo.setFailedCount(producerStats.getFailedCount());
                producerStatsVo.setSuccessRate(producerStats.getSuccessRate());
                producerStatsVo.setAverageElapsedTime(producerStats.getAverageElapsedTime());
                producerStatsVo.setStartTime(producerStats.getStartTime());
                
                response.setProducerStats(producerStatsVo);
            }
            
            // 获取消费者统计
            if ("CONSUMER".equals(request.getStatsType()) || "ALL".equals(request.getStatsType())) {
                io.nebula.messaging.core.consumer.MessageConsumer.ConsumerStats consumerStats = 
                    messageManager.getConsumer().getStats();
                
                GetMessageStatsDto.ConsumerStatsVo consumerStatsVo = new GetMessageStatsDto.ConsumerStatsVo();
                consumerStatsVo.setConsumedCount(consumerStats.getConsumedCount());
                consumerStatsVo.setSuccessCount(consumerStats.getSuccessCount());
                consumerStatsVo.setFailedCount(consumerStats.getFailedCount());
                consumerStatsVo.setSuccessRate(consumerStats.getSuccessRate());
                consumerStatsVo.setAverageElapsedTime(consumerStats.getAverageElapsedTime());
                consumerStatsVo.setProcessingCount(consumerStats.getProcessingCount());
                consumerStatsVo.setStartTime(consumerStats.getStartTime());
                
                response.setConsumerStats(consumerStatsVo);
            }
            
        } catch (Exception e) {
            log.error("获取消息统计信息失败", e);
        }
        
        return response;
    }
}

