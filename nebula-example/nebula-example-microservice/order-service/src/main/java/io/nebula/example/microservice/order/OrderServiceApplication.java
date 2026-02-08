package io.nebula.example.microservice.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单服务启动类
 * 
 * 依赖的 microservice-api 模块包含 MicroserviceApiAutoConfiguration，
 * 会自动扫描并注册 RPC 客户端接口，无需显式添加 @EnableRpcClients
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        log.info("========================================");
        log.info("  Order Service 启动成功");
        log.info("  服务名: order-service");
        log.info("  端口: 8002");
        log.info("  REST API: http://localhost:8002/api/orders");
        log.info("========================================");
    }
}
