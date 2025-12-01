package io.nebula.gateway.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gateway 健康检查控制器（WebFlux 版本）
 * 
 * 提供与 nebula-web 模块类似的健康检查端点，但使用 WebFlux 响应式编程模型
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@RestController
@RequestMapping("/health")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "nebula.gateway.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayHealthController {
    
    /**
     * 简单的 ping 端点
     * 
     * @return pong 响应
     */
    @GetMapping("/ping")
    public Mono<Map<String, String>> ping() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "pong");
        response.put("message", "Gateway is running");
        return Mono.just(response);
    }
    
    /**
     * 健康状态端点
     * 
     * @return 健康状态信息
     */
    @GetMapping
    public Mono<Map<String, Object>> health() {
        return status();
    }
    
    /**
     * 详细状态端点
     * 
     * @return 详细状态信息
     */
    @GetMapping("/status")
    public Mono<Map<String, Object>> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("component", "gateway");
        response.put("timestamp", Instant.now().toString());
        
        // 添加运行时信息
        Map<String, Object> runtime = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        runtime.put("availableProcessors", rt.availableProcessors());
        runtime.put("freeMemory", formatBytes(rt.freeMemory()));
        runtime.put("totalMemory", formatBytes(rt.totalMemory()));
        runtime.put("maxMemory", formatBytes(rt.maxMemory()));
        response.put("runtime", runtime);
        
        return Mono.just(response);
    }
    
    /**
     * 存活探针端点（Kubernetes）
     * 
     * @return 存活状态
     */
    @GetMapping("/liveness")
    public Mono<Map<String, String>> liveness() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "UP");
        return Mono.just(response);
    }
    
    /**
     * 就绪探针端点（Kubernetes）
     * 
     * @return 就绪状态
     */
    @GetMapping("/readiness")
    public Mono<Map<String, String>> readiness() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "UP");
        return Mono.just(response);
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}

