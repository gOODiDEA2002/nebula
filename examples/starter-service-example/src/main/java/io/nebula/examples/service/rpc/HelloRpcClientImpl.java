package io.nebula.examples.service.rpc;

import io.nebula.examples.service.api.HelloRpcClient;
import io.nebula.examples.service.api.ServiceInfoDto;
import io.nebula.rpc.core.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello服务RPC实现
 * 
 * 演示标准的RPC服务实现方式：
 * 1. 使用 @RpcService 标记实现类（无需指定接口类）
 * 2. 实现 @RemoteService 标记的接口
 * 3. 不使用 @RestController，所有路由由 @RpcCall 定义
 * 
 * 此实现类同时支持HTTP和gRPC两种协议访问
 * 
 * @author Nebula Framework
 */
@Slf4j
@RpcService
public class HelloRpcClientImpl implements HelloRpcClient {
    
    @Override
    public String hello() {
        log.info("RPC调用: hello");
        return "Hello, Nebula Service!";
    }
    
    @Override
    public String greet(String name) {
        log.info("RPC调用: greet, name={}", name);
        return String.format("Hello, %s! Welcome to Nebula Framework.", name);
    }
    
    @Override
    public ServiceInfoDto getServiceInfo() {
        log.info("RPC调用: getServiceInfo");
        return ServiceInfoDto.create(
            "starter-service-example",
            "2.0.1-SNAPSHOT",
            "Nebula Starter Service Example - 演示RPC服务实现"
        );
    }
}

