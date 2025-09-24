package io.nebula.web.controller;

import io.nebula.web.health.HealthCheckResult;
import io.nebula.web.health.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * @author nebula
 */
@RestController
@RequestMapping("/health")
@ConditionalOnBean(HealthCheckService.class)
@ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
public class HealthController {
    
    @Autowired
    private HealthCheckService healthCheckService;
    
    /**
     * 获取整体健康状态
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = healthCheckService.checkHealth();
        
        // 根据状态设置 HTTP 状态码
        String status = (String) result.get("status");
        if ("DOWN".equals(status) || "OUT_OF_SERVICE".equals(status)) {
            return ResponseEntity.status(503).body(result);
        } else if ("UNKNOWN".equals(status)) {
            return ResponseEntity.status(500).body(result);
        } else {
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 获取简化的健康状态
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = healthCheckService.checkHealth();
        
        Map<String, Object> simplified = new HashMap<>();
        simplified.put("status", result.get("status"));
        simplified.put("timestamp", result.get("timestamp"));
        simplified.put("summary", result.get("summary"));
        
        return simplified;
    }
    
    /**
     * 获取指定组件的健康状态
     */
    @GetMapping("/component/{name}")
    public ResponseEntity<HealthCheckResult> checkComponent(@PathVariable String name) {
        HealthCheckResult result = healthCheckService.checkHealth(name);
        
        if (result.getStatus().getCode().equals("DOWN") || 
            result.getStatus().getCode().equals("OUT_OF_SERVICE")) {
            return ResponseEntity.status(503).body(result);
        } else if (result.getStatus().getCode().equals("UNKNOWN")) {
            return ResponseEntity.status(500).body(result);
        } else {
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 获取所有可用的检查器
     */
    @GetMapping("/checkers")
    public Map<String, Object> getCheckers() {
        List<String> checkerNames = healthCheckService.getCheckerNames();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", checkerNames.size());
        result.put("checkers", checkerNames);
        
        return result;
    }
    
    /**
     * 获取最后一次检查的结果
     */
    @GetMapping("/last-results")
    public Map<String, HealthCheckResult> getLastResults() {
        return healthCheckService.getLastResults();
    }
    
    /**
     * 健康检查探针（简单版本）
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "pong");
        result.put("message", "应用程序正在运行");
        
        return result;
    }
    
    /**
     * 存活探针（Kubernetes liveness probe）
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        // 简单的存活检查，只要应用程序能响应就认为是存活的
        Map<String, String> result = new HashMap<>();
        result.put("status", "alive");
        result.put("message", "应用程序存活");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 就绪探针（Kubernetes readiness probe）
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> result = healthCheckService.checkHealth();
        String status = (String) result.get("status");
        
        Map<String, Object> readinessResult = new HashMap<>();
        readinessResult.put("status", "UP".equals(status) ? "ready" : "not-ready");
        readinessResult.put("details", result);
        
        if ("UP".equals(status)) {
            return ResponseEntity.ok(readinessResult);
        } else {
            return ResponseEntity.status(503).body(readinessResult);
        }
    }
}
