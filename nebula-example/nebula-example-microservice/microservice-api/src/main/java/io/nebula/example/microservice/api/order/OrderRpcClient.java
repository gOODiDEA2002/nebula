package io.nebula.example.microservice.api.order;

import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RpcClient;

import java.util.List;

/**
 * 订单服务 RPC 接口
 * 
 * RPC 接口设计原则：
 * - 参数使用具体类型
 * - 返回值使用业务对象
 * - 不使用 HTTP 路径注解（框架自动处理）
 * - 错误通过 BusinessException 抛出
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@RpcClient("order-service")
public interface OrderRpcClient {

    /**
     * 创建订单
     * 
     * @param request 创建订单请求
     * @return 创建的订单信息（包含用户信息）
     */
    @RpcCall
    OrderDto createOrder(CreateOrderRequest request);

    /**
     * 根据 ID 获取订单
     * 
     * @param orderId 订单 ID
     * @return 订单信息，不存在返回 null
     */
    @RpcCall
    OrderDto getOrderById(Long orderId);

    /**
     * 获取用户的订单列表
     * 
     * @param userId 用户 ID
     * @return 订单列表
     */
    @RpcCall
    List<OrderDto> listOrdersByUserId(Long userId);
}
