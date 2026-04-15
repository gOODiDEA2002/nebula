package io.nebula.example.modules.messaging.service;

import io.nebula.example.modules.messaging.entity.dto.SendOrderNotificationDto;
import io.nebula.example.modules.messaging.entity.dto.SendOrderStatusUpdateDto;
import io.nebula.example.modules.messaging.entity.dto.GetMessageStatsDto;
import io.nebula.example.modules.messaging.entity.dto.SendBatchNotificationDto;

/**
 * 消息传递演示服务接口
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface MessagingDemoService {
    
    /**
     * 发送订单通知
     * 
     * @param request 请求参数
     * @return 发送结果
     */
    SendOrderNotificationDto.Response sendOrderNotification(SendOrderNotificationDto.Request request);
    
    /**
     * 发送订单状态更新通知
     * 
     * @param request 请求参数
     * @return 发送结果
     */
    SendOrderStatusUpdateDto.Response sendOrderStatusUpdate(SendOrderStatusUpdateDto.Request request);
    
    /**
     * 发送批量通知
     * 
     * @param request 请求参数
     * @return 发送结果
     */
    SendBatchNotificationDto.Response sendBatchNotification(SendBatchNotificationDto.Request request);
    
    /**
     * 获取消息统计信息
     * 
     * @param request 请求参数
     * @return 统计信息
     */
    GetMessageStatsDto.Response getMessageStats(GetMessageStatsDto.Request request);
}

