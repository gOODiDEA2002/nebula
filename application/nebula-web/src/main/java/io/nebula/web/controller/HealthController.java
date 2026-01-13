package io.nebula.web.controller;

import io.nebula.web.health.HealthCheckResult;
import io.nebula.web.health.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.nebula.core.common.result.Result;
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
@ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
public class HealthController {
    
    @Autowired(required = false)
    private HealthCheckService healthCheckService;
    
    /**
     * 获取整体健康状态
     */
    @GetMapping
    public Result<Map<String, Object>> health() {
        if (healthCheckService == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "UP");
            result.put("message", "健康检查服务未配置，默认状态正常");
            result.put("timestamp", java.time.LocalDateTime.now());
            return Result.success(result);
        }
        
        Map<String, Object> result = healthCheckService.checkHealth();
        
        // 根据状态设置 HTTP 状态码
        String status = (String) result.get("status");
        if ("DOWN".equals(status) || "OUT_OF_SERVICE".equals(status)) {
            return Result.error("503", "服务不可用");
        } else if ("UNKNOWN".equals(status)) {
            return Result.error("500", "未知错误");
        } else {
            return Result.success(result);
        }
    }
    
    /**
     * 获取简化的健康状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> result = healthCheckService.checkHealth();
        
        Map<String, Object> simplified = new HashMap<>();
        simplified.put("status", result.get("status"));
        simplified.put("timestamp", result.get("timestamp"));
        simplified.put("summary", result.get("summary"));
        
        return Result.success(simplified);
    }
    
    /**
     * 获取指定组件的健康状态
     */
    @GetMapping("/component/{name}")
    public Result<HealthCheckResult> checkComponent(@PathVariable String name) {
        HealthCheckResult result = healthCheckService.checkHealth(name);
        
        if (result.getStatus().getCode().equals("DOWN") || 
            result.getStatus().getCode().equals("OUT_OF_SERVICE")) {
            return Result.error("503", "服务不可用", result);
        } else if (result.getStatus().getCode().equals("UNKNOWN")) {
            return Result.error("500", "未知错误", result);
        } else {
            return Result.success(result);
        }
    }
    
    /**
     * 获取所有可用的检查器
     */
    @GetMapping("/checkers")
    public Result<Map<String, Object>> getCheckers() {
        List<String> checkerNames = healthCheckService.getCheckerNames();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", checkerNames.size());
        result.put("checkers", checkerNames);
        
        return Result.success(result);
    }
    
    /**
     * 获取最后一次检查的结果
     */
    @GetMapping("/last-results")
    public Result<Map<String, HealthCheckResult>> getLastResults() {
        return Result.success(healthCheckService.getLastResults());
    }
    
    /**
     * 健康检查探针
     */
    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "pong");
        result.put("message", "应用程序正在运行");
        
        return Result.success(result);
    }
    
    /**
     * 存活探针（Kubernetes liveness probe）
     */
    @GetMapping("/liveness")
    public Result<Map<String, String>> liveness() {
        // 简单的存活检查，只要应用程序能响应就认为是存活的
        Map<String, String> result = new HashMap<>();
        result.put("status", "alive");
        result.put("message", "应用程序存活");
        
        return Result.success(result);
    }
    
    /**
     * 就绪探针（Kubernetes readiness probe）
     */
    @GetMapping("/readiness")
    public Result<Map<String, Object>> readiness() {
        Map<String, Object> result = healthCheckService.checkHealth();
        String status = (String) result.get("status");
        
        Map<String, Object> readinessResult = new HashMap<>();
        readinessResult.put("status", "UP".equals(status) ? "ready" : "not-ready");
        readinessResult.put("details", result);
        
        if ("UP".equals(status)) {
            return Result.success(readinessResult);
        } else {
            return Result.error("503", "服务不可用", readinessResult);
        }
    }
}
