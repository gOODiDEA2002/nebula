package io.nebula.example.microservice.order.service;

import io.nebula.example.microservice.api.order.CreateOrderRequest;
import io.nebula.example.microservice.api.order.OrderDto;
import io.nebula.example.microservice.api.user.UserDto;
import io.nebula.example.microservice.api.user.UserRpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单服务实现
 * 
 * 演示服务间 RPC 调用
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final Map<Long, OrderDto> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 注入用户服务 RPC 客户端
     * 框架会自动创建代理，通过服务发现找到用户服务实例
     */
    private final UserRpcClient userRpcClient;

    /**
     * 创建订单
     * 演示调用用户服务获取用户信息
     */
    public OrderDto createOrder(CreateOrderRequest request) {
        log.info("创建订单: userId={}, product={}", request.getUserId(), request.getProductName());
        
        // 调用用户服务获取用户信息
        UserDto user = userRpcClient.getUserById(request.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在: " + request.getUserId());
        }
        log.info("获取用户信息成功: username={}", user.getUsername());

        Long id = idGenerator.getAndIncrement();
        OrderDto order = OrderDto.builder()
                .id(id)
                .orderNo(generateOrderNo())
                .userId(request.getUserId())
                .user(user)  // 包含用户信息
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .status(0)  // 待支付
                .createTime(LocalDateTime.now())
                .build();
        
        orderStore.put(id, order);
        log.info("订单创建成功: orderId={}, orderNo={}", id, order.getOrderNo());
        return order;
    }

    /**
     * 根据 ID 获取订单
     */
    public OrderDto getOrderById(Long orderId) {
        OrderDto order = orderStore.get(orderId);
        if (order != null) {
            // 获取最新的用户信息
            UserDto user = userRpcClient.getUserById(order.getUserId());
            order.setUser(user);
        }
        log.debug("查询订单: id={}, found={}", orderId, order != null);
        return order;
    }

    /**
     * 获取用户的订单列表
     */
    public List<OrderDto> listOrdersByUserId(Long userId) {
        // 先验证用户是否存在
        UserDto user = userRpcClient.getUserById(userId);
        if (user == null) {
            log.warn("用户不存在: {}", userId);
            return List.of();
        }

        List<OrderDto> orders = orderStore.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .peek(order -> order.setUser(user))
                .toList();
        
        log.debug("查询用户订单: userId={}, count={}", userId, orders.size());
        return orders;
    }

    /**
     * 获取所有订单
     */
    public List<OrderDto> listAllOrders() {
        return orderStore.values().stream()
                .peek(order -> {
                    UserDto user = userRpcClient.getUserById(order.getUserId());
                    order.setUser(user);
                })
                .toList();
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
