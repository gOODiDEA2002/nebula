package io.nebula.web.performance;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 性能指标数据类
 * 
 * @author nebula
 */
public class PerformanceMetrics {
    
    /**
     * 请求总数
     */
    private volatile long totalRequests = 0;
    
    /**
     * 成功请求数
     */
    private volatile long successfulRequests = 0;
    
    /**
     * 失败请求数
     */
    private volatile long failedRequests = 0;
    
    /**
     * 总响应时间（毫秒）
     */
    private volatile long totalResponseTime = 0;
    
    /**
     * 最小响应时间（毫秒）
     */
    private volatile long minResponseTime = Long.MAX_VALUE;
    
    /**
     * 最大响应时间（毫秒）
     */
    private volatile long maxResponseTime = 0;
    
    /**
     * 慢请求数量
     */
    private volatile long slowRequestCount = 0;
    
    /**
     * 活跃请求数
     */
    private volatile long activeRequests = 0;
    
    /**
     * 各状态码的请求数量
     */
    private final Map<Integer, Long> statusCounts = new ConcurrentHashMap<>();
    
    /**
     * 各 API 路径的请求数量
     */
    private final Map<String, Long> pathCounts = new ConcurrentHashMap<>();
    
    /**
     * 最后更新时间
     */
    private volatile LocalDateTime lastUpdateTime = LocalDateTime.now();
    
    public synchronized void incrementTotalRequests() {
        totalRequests++;
    }
    
    public synchronized void incrementSuccessfulRequests() {
        successfulRequests++;
    }
    
    public synchronized void incrementFailedRequests() {
        failedRequests++;
    }
    
    public synchronized void addResponseTime(long responseTime) {
        totalResponseTime += responseTime;
        if (responseTime < minResponseTime) {
            minResponseTime = responseTime;
        }
        if (responseTime > maxResponseTime) {
            maxResponseTime = responseTime;
        }
    }
    
    public synchronized void incrementSlowRequestCount() {
        slowRequestCount++;
    }
    
    public synchronized void incrementActiveRequests() {
        activeRequests++;
    }
    
    public synchronized void decrementActiveRequests() {
        if (activeRequests > 0) {
            activeRequests--;
        }
    }
    
    public synchronized void incrementStatusCount(int statusCode) {
        statusCounts.merge(statusCode, 1L, Long::sum);
    }
    
    public synchronized void incrementPathCount(String path) {
        pathCounts.merge(path, 1L, Long::sum);
    }
    
    public synchronized void updateLastUpdateTime() {
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 获取平均响应时间
     */
    public double getAverageResponseTime() {
        return totalRequests > 0 ? (double) totalResponseTime / totalRequests : 0;
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        return totalRequests > 0 ? (double) successfulRequests / totalRequests * 100 : 0;
    }
    
    /**
     * 获取失败率
     */
    public double getFailureRate() {
        return totalRequests > 0 ? (double) failedRequests / totalRequests * 100 : 0;
    }
    
    // Getters
    public long getTotalRequests() { return totalRequests; }
    public long getSuccessfulRequests() { return successfulRequests; }
    public long getFailedRequests() { return failedRequests; }
    public long getTotalResponseTime() { return totalResponseTime; }
    public long getMinResponseTime() { return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime; }
    public long getMaxResponseTime() { return maxResponseTime; }
    public long getSlowRequestCount() { return slowRequestCount; }
    public long getActiveRequests() { return activeRequests; }
    public Map<Integer, Long> getStatusCounts() { return new ConcurrentHashMap<>(statusCounts); }
    public Map<String, Long> getPathCounts() { return new ConcurrentHashMap<>(pathCounts); }
    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
}
