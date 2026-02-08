package io.nebula.example.rpc.async.client;

import io.nebula.rpc.core.annotation.EnableRpcClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 异步RPC客户端启动类
 * 
 * <p>作为RPC服务消费方，演示异步RPC调用能力。
 * 
 * @author Nebula Framework
 */
@Slf4j
@SpringBootApplication
@EnableRpcClients(basePackages = "io.nebula.example.rpc.async.api")
public class AsyncRpcClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncRpcClientApplication.class, args);
        log.info("========================================");
        log.info("  Async RPC Client 启动成功");
        log.info("  服务名: async-rpc-client");
        log.info("  端口: 8082");
        log.info("  API文档: http://localhost:8082/swagger-ui.html");
        log.info("========================================");
    }
}
