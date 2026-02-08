package io.nebula.example.microservice.order.rpc;

import io.nebula.example.microservice.api.order.CreateOrderRequest;
import io.nebula.example.microservice.api.order.OrderDto;
import io.nebula.example.microservice.api.order.OrderRpcClient;
import io.nebula.example.microservice.order.service.OrderService;
import io.nebula.rpc.core.annotation.RpcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 订单 RPC 服务实现
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@RpcService
@RequiredArgsConstructor
public class OrderRpcClientImpl implements OrderRpcClient {

    private final OrderService orderService;

    @Override
    public OrderDto createOrder(CreateOrderRequest request) {
        log.info("RPC: 创建订单 userId={}", request.getUserId());
        return orderService.createOrder(request);
    }

    @Override
    public OrderDto getOrderById(Long orderId) {
        log.info("RPC: 获取订单 orderId={}", orderId);
        return orderService.getOrderById(orderId);
    }

    @Override
    public List<OrderDto> listOrdersByUserId(Long userId) {
        log.info("RPC: 获取用户订单 userId={}", userId);
        return orderService.listOrdersByUserId(userId);
    }
}
