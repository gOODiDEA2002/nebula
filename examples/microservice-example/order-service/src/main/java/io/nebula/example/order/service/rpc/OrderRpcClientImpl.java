package io.nebula.example.order.service.rpc;

import io.nebula.example.user.api.dto.GetUserDto;
import io.nebula.example.user.api.rpc.UserRpcClient;
import io.nebula.example.user.api.rpc.AuthRpcClient;
import io.nebula.example.user.api.dto.AuthDto;
import io.nebula.example.order.api.dto.CreateOrderDto;
import io.nebula.example.order.api.dto.GetOrderDto;
import io.nebula.example.order.api.rpc.OrderRpcClient;
import io.nebula.example.order.service.business.OrderDemoService;
import io.nebula.rpc.core.annotation.RpcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderRpcService RPC 服务端实现
 * 
 * 作为 RPC Server 端的接口实现，负责接收并处理 RPC 调用
 * 同时作为 RPC Client 调用 UserRpcService（演示服务间交互）
 * 
 * 优化说明：
 * 1. @RpcService 注解无需指定接口类，框架会自动推导实现的 @RemoteService 接口
 * 2. RPC 客户端依赖注入无需 @Qualifier 注解，通过 UserApiAutoConfiguration 自动注册的 Bean 会自动匹配
 * 3. Bean 名称规则：接口简单类名首字母小写（UserRpcClient -> userRpcClient）
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RpcService 
@RequiredArgsConstructor
public class OrderRpcClientImpl implements OrderRpcClient {
    
    private final OrderDemoService orderDemoService;
    
    /**
     * 用户RPC服务客户端
     * 通过 UserApiAutoConfiguration 自动注册，Bean 名称为 userRpcClient
     * 无需 @Qualifier 注解，Lombok @RequiredArgsConstructor 会自动按名称注入
     */
    private final UserRpcClient userRpcClient;

    /**
     * 认证RPC服务客户端
     * 通过 UserApiAutoConfiguration 自动注册，Bean 名称为 authRpcClient
     * 无需 @Qualifier 注解，Lombok @RequiredArgsConstructor 会自动按名称注入
     */
    private final AuthRpcClient authRpcClient;
    
    @Override
    public CreateOrderDto.Response createOrder(CreateOrderDto.Request request) {
        log.info("RPC服务端: createOrder, userId={}, productName={}", 
            request.getUserId(), request.getProductName());
        
        log.info("→ 调用AuthRpcClient认证: username={}, password={}", "username", "password");
        AuthDto.Request authRequest = new AuthDto.Request();
        authRequest.setUsername("username");
        authRequest.setPassword("password");
        AuthDto.Response authResponse = authRpcClient.auth(authRequest);
        log.info("← AuthRpcClient返回认证信息: {}", authResponse.getToken());

        // 【服务间交互】调用UserService验证用户是否存在
        log.info("→ 调用UserService验证用户: userId={}", request.getUserId());
        GetUserDto.Response userResponse = userRpcClient.getUserById(request.getUserId());
        
        if (userResponse.getUser() == null) {
            log.error("用户不存在: userId={}", request.getUserId());
            throw new IllegalArgumentException("用户不存在: " + request.getUserId());
        }
        
        log.info("← UserService返回用户信息: {}", userResponse.getUser().getName());
        
        // 创建订单
        return orderDemoService.createOrder(request);
    }
    
    @Override
    public GetOrderDto.Response getOrderById(Long id) {
        log.info("RPC服务端: getOrderById, id={}", id);
        return orderDemoService.getOrderById(id);
    }
}

