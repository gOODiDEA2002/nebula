package io.nebula.web.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 健康检查服务
 * 
 * @author nebula
 */
public class HealthCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    
    private final List<HealthChecker> healthCheckers;
    private final Map<String, HealthCheckResult> lastResults = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final boolean showDetails;
    
    public HealthCheckService(List<HealthChecker> healthCheckers, boolean showDetails) {
        this.healthCheckers = healthCheckers.stream()
            .filter(HealthChecker::isEnabled)
            .sorted(Comparator.comparingInt(HealthChecker::getOrder))
            .collect(Collectors.toList());
        this.showDetails = showDetails;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "health-checker");
            thread.setDaemon(true);
            return thread;
        });
        
        logger.info("Initialized HealthCheckService with {} checkers", this.healthCheckers.size());
    }
    
    /**
     * 执行所有健康检查
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now());
        
        // 并发执行所有检查
        List<CompletableFuture<Map.Entry<String, HealthCheckResult>>> futures = 
            healthCheckers.stream()
                .map(checker -> CompletableFuture.supplyAsync(() -> {
                    try {
                        HealthCheckResult checkResult = checker.check();
                        lastResults.put(checker.getName(), checkResult);
                        return Map.entry(checker.getName(), checkResult);
                    } catch (Exception e) {
                        logger.error("Health check failed for {}: {}", checker.getName(), e.getMessage());
                        HealthCheckResult errorResult = HealthCheckResult.down("检查过程中出现异常: " + e.getMessage());
                        lastResults.put(checker.getName(), errorResult);
                        return Map.entry(checker.getName(), errorResult);
                    }
                }, executorService))
                .collect(Collectors.toList());
        
        // 收集所有结果
        Map<String, HealthCheckResult> results = new LinkedHashMap<>();
        for (CompletableFuture<Map.Entry<String, HealthCheckResult>> future : futures) {
            try {
                Map.Entry<String, HealthCheckResult> entry = future.join();
                results.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                logger.error("Failed to get health check result: {}", e.getMessage());
            }
        }
        
        // 计算总体状态
        HealthStatus overallStatus = calculateOverallStatus(results.values());
        result.put("status", overallStatus.getCode());
        
        // 添加详细信息
        if (showDetails) {
            Map<String, Object> components = new LinkedHashMap<>();
            for (Map.Entry<String, HealthCheckResult> entry : results.entrySet()) {
                HealthCheckResult checkResult = entry.getValue();
                Map<String, Object> componentInfo = new LinkedHashMap<>();
                componentInfo.put("status", checkResult.getStatus().getCode());
                componentInfo.put("timestamp", checkResult.getTimestamp());
                
                if (checkResult.getResponseTime() != null) {
                    componentInfo.put("responseTime", checkResult.getResponseTime() + "ms");
                }
                
                if (checkResult.getError() != null) {
                    componentInfo.put("error", checkResult.getError());
                }
                
                if (checkResult.getDetails() != null && !checkResult.getDetails().isEmpty()) {
                    componentInfo.put("details", checkResult.getDetails());
                }
                
                if (checkResult.getSuggestion() != null) {
                    componentInfo.put("suggestion", checkResult.getSuggestion());
                }
                
                components.put(entry.getKey(), componentInfo);
            }
            result.put("components", components);
        }
        
        // 添加概要信息
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", results.size());
        summary.put("up", results.values().stream().mapToInt(r -> r.getStatus() == HealthStatus.UP ? 1 : 0).sum());
        summary.put("down", results.values().stream().mapToInt(r -> r.getStatus() == HealthStatus.DOWN ? 1 : 0).sum());
        summary.put("unknown", results.values().stream().mapToInt(r -> r.getStatus() == HealthStatus.UNKNOWN ? 1 : 0).sum());
        result.put("summary", summary);
        
        return result;
    }
    
    /**
     * 执行单个健康检查
     */
    public HealthCheckResult checkHealth(String checkerName) {
        return healthCheckers.stream()
            .filter(checker -> checker.getName().equals(checkerName))
            .findFirst()
            .map(checker -> {
                try {
                    HealthCheckResult result = checker.check();
                    lastResults.put(checkerName, result);
                    return result;
                } catch (Exception e) {
                    logger.error("Health check failed for {}: {}", checkerName, e.getMessage());
                    HealthCheckResult errorResult = HealthCheckResult.down("检查过程中出现异常: " + e.getMessage());
                    lastResults.put(checkerName, errorResult);
                    return errorResult;
                }
            })
            .orElse(HealthCheckResult.unknown().withDetail("error", "未找到检查器: " + checkerName));
    }
    
    /**
     * 获取最后一次检查结果
     */
    public Map<String, HealthCheckResult> getLastResults() {
        return new HashMap<>(lastResults);
    }
    
    /**
     * 获取所有注册的检查器名称
     */
    public List<String> getCheckerNames() {
        return healthCheckers.stream()
            .map(HealthChecker::getName)
            .collect(Collectors.toList());
    }
    
    private HealthStatus calculateOverallStatus(Collection<HealthCheckResult> results) {
        if (results.isEmpty()) {
            return HealthStatus.UNKNOWN;
        }
        
        // 如果有任何一个是 DOWN，整体状态就是 DOWN
        if (results.stream().anyMatch(r -> r.getStatus() == HealthStatus.DOWN)) {
            return HealthStatus.DOWN;
        }
        
        // 如果有任何一个是 OUT_OF_SERVICE，整体状态就是 OUT_OF_SERVICE
        if (results.stream().anyMatch(r -> r.getStatus() == HealthStatus.OUT_OF_SERVICE)) {
            return HealthStatus.OUT_OF_SERVICE;
        }
        
        // 如果有任何一个是 UNKNOWN，整体状态就是 UNKNOWN
        if (results.stream().anyMatch(r -> r.getStatus() == HealthStatus.UNKNOWN)) {
            return HealthStatus.UNKNOWN;
        }
        
        // 所有都是 UP
        return HealthStatus.UP;
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
