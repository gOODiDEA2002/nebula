package io.nebula.web.controller;

import io.nebula.web.performance.PerformanceMetrics;
import io.nebula.web.performance.PerformanceMonitor;
import io.nebula.web.performance.SystemMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 性能监控控制器
 * 提供性能指标查询接口
 * 
 * @author nebula
 */
@RestController
@RequestMapping("/performance")
@ConditionalOnBean(PerformanceMonitor.class)
@ConditionalOnProperty(name = "nebula.web.performance.enabled", havingValue = "true")
public class PerformanceController {
    
    @Autowired
    private PerformanceMonitor performanceMonitor;
    
    /**
     * 获取应用性能指标
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        PerformanceMetrics metrics = performanceMonitor.getMetrics();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalRequests", metrics.getTotalRequests());
        result.put("successfulRequests", metrics.getSuccessfulRequests());
        result.put("failedRequests", metrics.getFailedRequests());
        result.put("activeRequests", metrics.getActiveRequests());
        result.put("averageResponseTime", metrics.getAverageResponseTime());
        result.put("minResponseTime", metrics.getMinResponseTime());
        result.put("maxResponseTime", metrics.getMaxResponseTime());
        result.put("slowRequestCount", metrics.getSlowRequestCount());
        result.put("successRate", metrics.getSuccessRate());
        result.put("failureRate", metrics.getFailureRate());
        result.put("statusCounts", metrics.getStatusCounts());
        result.put("pathCounts", metrics.getPathCounts());
        result.put("lastUpdateTime", metrics.getLastUpdateTime());
        
        return result;
    }
    
    /**
     * 获取系统指标
     */
    @GetMapping("/system")
    public SystemMetrics getSystemMetrics() {
        return performanceMonitor.getSystemMetrics();
    }
    
    /**
     * 获取综合状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        PerformanceMetrics metrics = performanceMonitor.getMetrics();
        SystemMetrics systemMetrics = performanceMonitor.getSystemMetrics();
        
        Map<String, Object> result = new HashMap<>();
        
        // 应用状态
        Map<String, Object> appStatus = new HashMap<>();
        appStatus.put("status", getApplicationStatus(metrics, systemMetrics));
        appStatus.put("totalRequests", metrics.getTotalRequests());
        appStatus.put("activeRequests", metrics.getActiveRequests());
        appStatus.put("successRate", metrics.getSuccessRate());
        appStatus.put("averageResponseTime", metrics.getAverageResponseTime());
        result.put("application", appStatus);
        
        // 系统状态
        Map<String, Object> sysStatus = new HashMap<>();
        sysStatus.put("cpuUsage", systemMetrics.getCpuUsage());
        sysStatus.put("memoryUsage", systemMetrics.getMemoryUsagePercentage());
        sysStatus.put("heapMemoryUsage", systemMetrics.getHeapMemoryUsage());
        sysStatus.put("activeThreads", systemMetrics.getActiveThreadCount());
        sysStatus.put("systemLoadAverage", systemMetrics.getSystemLoadAverage());
        result.put("system", sysStatus);
        
        return result;
    }
    
    /**
     * 重置性能指标
     */
    @PostMapping("/reset")
    public Map<String, String> resetMetrics() {
        performanceMonitor.resetMetrics();
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "Performance metrics have been reset");
        return result;
    }
    
    private String getApplicationStatus(PerformanceMetrics metrics, SystemMetrics systemMetrics) {
        // 简单的健康状态判断逻辑
        if (metrics.getFailureRate() > 50) {
            return "CRITICAL";
        } else if (metrics.getFailureRate() > 20 || systemMetrics.getCpuUsage() > 80 || 
                   systemMetrics.getMemoryUsagePercentage() > 90) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }
}
