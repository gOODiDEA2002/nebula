package io.nebula.example.microservice.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户服务启动类
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        log.info("========================================");
        log.info("  User Service 启动成功");
        log.info("  服务名: user-service");
        log.info("  端口: 8001");
        log.info("========================================");
    }
}
