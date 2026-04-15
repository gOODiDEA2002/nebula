package io.nebula.examples.service.api;

import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RemoteService;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Hello服务RPC接口
 * 
 * 演示标准的RPC接口定义方式：
 * 1. 使用 @RemoteService 标记接口
 * 2. 使用 @RpcCall 定义HTTP路由
 * 3. 接口同时支持HTTP和gRPC两种协议
 * 
 * 注意：在实际项目中，此接口应该定义在独立的 *-api 模块中
 * 
 * @author Nebula Framework
 */
@RemoteService
public interface HelloRpcClient {
    
    /**
     * 简单问候
     * 
     * @return 问候语
     */
    @RpcCall(value = "/rpc/hello", method = "GET")
    String hello();
    
    /**
     * 带参数的问候
     * 
     * @param name 姓名
     * @return 个性化问候语
     */
    @RpcCall(value = "/rpc/hello/greet", method = "GET")
    String greet(@RequestParam("name") String name);
    
    /**
     * 获取服务信息
     * 
     * @return 服务信息DTO
     */
    @RpcCall(value = "/rpc/hello/info", method = "GET")
    ServiceInfoDto getServiceInfo();
}

