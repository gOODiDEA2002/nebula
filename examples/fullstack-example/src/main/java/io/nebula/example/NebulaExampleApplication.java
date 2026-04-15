package io.nebula.example;

import io.nebula.rpc.core.annotation.EnableRpcClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nebula 框架示例应用
 */
@SpringBootApplication
@EnableRpcClients(basePackages = "io.nebula.example.api.rpc")
public class NebulaExampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NebulaExampleApplication.class, args);
    }
}
