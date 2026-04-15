package io.nebula.example.user.api.rpc;

import io.nebula.example.user.api.dto.AuthDto;
import io.nebula.rpc.core.annotation.RemoteService;
import io.nebula.example.user.api.dto.*;

/**
 * 认证RPC服务接口
 * 使用声明式注解方式定义RPC客户端
 * 
 * 此接口定义在独立的API模块中，可以被服务提供方和消费方共享
 * 
 * 优化说明：
 * 1. 无需 @RpcCall 注解，框架会自动基于方法名调用
 * 2. 无需 @RequestBody 注解，参数自动作为请求体
 * 3. value 和 contextId 可省略，框架会自动推导
 */
@RemoteService
public interface AuthRpcClient {
    
    /**
     * 认证
     * RPC 路径自动映射为：{serviceName}/auth
     */
    AuthDto.Response auth(AuthDto.Request request);
}

