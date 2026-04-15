package io.nebula.example.order.service.business;

import io.nebula.example.order.api.dto.CreateOrderDto;
import io.nebula.example.order.api.dto.GetOrderDto;

/**
 * 订单演示服务接口
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface OrderDemoService {
    
    /**
     * 创建订单
     */
    CreateOrderDto.Response createOrder(CreateOrderDto.Request request);
    
    /**
     * 获取订单详情
     */
    GetOrderDto.Response getOrderById(Long id);
}

