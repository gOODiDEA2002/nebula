package io.nebula.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的Hello控制器，用于验证基本功能
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class HelloController {
    
    @GetMapping("/hello")
    public Map<String, Object> hello() {
        log.info("收到hello请求");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Nebula Framework!");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "success");
        response.put("framework", "Nebula 2.0.0-SNAPSHOT");
        
        return response;
    }
    
}
