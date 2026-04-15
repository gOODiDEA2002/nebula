package io.nebula.example.order.service.business.impl;

import io.nebula.example.order.api.dto.CreateOrderDto;
import io.nebula.example.order.api.dto.GetOrderDto;
import io.nebula.example.order.api.entity.Order;
import io.nebula.example.order.api.vo.OrderVo;
import io.nebula.example.order.service.business.OrderDemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单演示服务实现
 * 使用内存存储演示订单功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Service
public class OrderDemoServiceImpl implements OrderDemoService {
    
    // 使用内存存储订单数据（演示用）
    private final Map<Long, Order> orderStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public CreateOrderDto.Response createOrder(CreateOrderDto.Request request) {
        log.info("创建订单: userId={}, productName={}, quantity={}", 
            request.getUserId(), request.getProductName(), request.getQuantity());
        
        Order order = new Order();
        order.setId(idGenerator.getAndIncrement());
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setProductName(request.getProductName());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setTotalAmount(request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setStatus("CREATED");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        orderStorage.put(order.getId(), order);
        
        CreateOrderDto.Response response = new CreateOrderDto.Response();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setTotalAmount(order.getTotalAmount());
        
        log.info("创建订单成功: orderId={}, orderNo={}, totalAmount={}", 
            order.getId(), order.getOrderNo(), order.getTotalAmount());
        
        return response;
    }
    
    @Override
    public GetOrderDto.Response getOrderById(Long id) {
        log.info("获取订单详情: orderId={}", id);
        
        Order order = orderStorage.get(id);
        
        GetOrderDto.Response response = new GetOrderDto.Response();
        if (order != null) {
            OrderVo orderVo = new OrderVo();
            BeanUtils.copyProperties(order, orderVo);
            response.setOrder(orderVo);
        }
        
        return response;
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) 
            + String.format("%04d", idGenerator.get());
    }
}

