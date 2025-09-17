package io.nebula.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api")
public class HealthController {
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "message", "Nebula Example Application is running"
        );
    }
    
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "application", "nebula-example",
            "version", "2.0.0-SNAPSHOT",
            "framework", "Spring Boot 3.2.0",
            "mybatis-plus", "3.5.9"
        );
    }
}
