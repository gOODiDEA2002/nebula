package io.nebula.example.order.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nebula Example Order Service 启动类
 * 
 * 订单服务 - 演示微服务间交互
 * - 作为Provider：提供OrderRpcService服务（自动扫描 @RpcService）
 * - 作为Consumer：调用UserRpcService服务（通过 UserApiAutoConfiguration 自动注册）
 * 
 * 优化说明：
 * 1. 无需 @EnableRpcClients 注解，UserApiAutoConfiguration 会自动注册所有 RPC 客户端
 * 2. 无需指定 basePackageClasses，自动扫描 io.nebula.example.api.rpc 包
 * 3. RPC 客户端 Bean 自动注入，无需 @Qualifier 注解
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@SpringBootApplication
public class NebulaExampleOrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NebulaExampleOrderServiceApplication.class, args);
    }
}

