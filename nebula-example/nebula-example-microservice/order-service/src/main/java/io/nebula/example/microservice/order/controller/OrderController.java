package io.nebula.example.microservice.order.controller;

import io.nebula.example.microservice.api.order.CreateOrderRequest;
import io.nebula.example.microservice.api.order.OrderDto;
import io.nebula.example.microservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单 REST API 控制器
 * 
 * 提供面向前端的 REST API（与 RPC 接口并存）
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("REST API: 创建订单");
        OrderDto order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long orderId) {
        log.info("REST API: 获取订单 orderId={}", orderId);
        OrderDto order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    /**
     * 获取用户的订单列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> listOrdersByUserId(@PathVariable Long userId) {
        log.info("REST API: 获取用户订单 userId={}", userId);
        List<OrderDto> orders = orderService.listOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * 获取所有订单
     */
    @GetMapping
    public ResponseEntity<List<OrderDto>> listAllOrders() {
        log.info("REST API: 获取所有订单");
        List<OrderDto> orders = orderService.listAllOrders();
        return ResponseEntity.ok(orders);
    }
}
