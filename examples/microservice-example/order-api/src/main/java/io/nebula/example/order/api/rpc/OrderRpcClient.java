package io.nebula.example.order.api.rpc;

import io.nebula.example.order.api.dto.CreateOrderDto;
import io.nebula.example.order.api.dto.GetOrderDto;
import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RemoteService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 订单RPC服务接口
 * 使用声明式注解方式定义RPC客户端
 * 
 * 此接口定义在独立的API模块中，可以被服务提供方和消费方共享
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@RemoteService
public interface OrderRpcClient {
    
    /**
     * 创建订单
     * 
     * 业务流程：
     * 1. 验证用户是否存在（调用UserService）
     * 2. 创建订单
     * 3. 返回订单信息
     */
    @RpcCall(value = "/rpc/orders", method = "POST")
    CreateOrderDto.Response createOrder(@RequestBody CreateOrderDto.Request request);
    
    /**
     * 获取订单详情
     * 
     * 业务流程：
     * 1. 查询订单信息
     * 2. 关联查询用户信息（调用UserService）
     * 3. 返回订单详情
     */
    @RpcCall(value = "/rpc/orders/{id}", method = "GET")
    GetOrderDto.Response getOrderById(@PathVariable("id") Long id);
}

