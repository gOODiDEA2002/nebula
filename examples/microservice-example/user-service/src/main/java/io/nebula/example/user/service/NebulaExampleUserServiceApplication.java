package io.nebula.example.user.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nebula Example User Service 启动类
 * RPC服务提供者 - 提供UserRpcService的服务端实现
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@SpringBootApplication
public class NebulaExampleUserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NebulaExampleUserServiceApplication.class, args);
    }
}

