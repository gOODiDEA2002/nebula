package io.nebula.example.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nebula Web 应用示例
 * 
 * 演示 Nebula Framework 的 Web 开发能力：
 * - Thymeleaf 模板引擎
 * - 静态资源处理
 * - 统一异常处理
 * - 健康检查
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication
public class WebExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebExampleApplication.class, args);
        log.info("========================================");
        log.info("  Nebula Web Example 启动成功");
        log.info("  访问地址: http://localhost:8080");
        log.info("  健康检查: http://localhost:8080/actuator/health");
        log.info("========================================");
    }
}
