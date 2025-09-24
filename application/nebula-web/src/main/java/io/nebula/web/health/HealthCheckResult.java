package io.nebula.web.health;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查结果
 * 
 * @author nebula
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthCheckResult {
    
    /**
     * 健康状态
     */
    private HealthStatus status;
    
    /**
     * 检查时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 详细信息
     */
    private Map<String, Object> details;
    
    /**
     * 建议信息
     */
    private String suggestion;
    
    public HealthCheckResult() {
        this.timestamp = LocalDateTime.now();
        this.details = new LinkedHashMap<>();
    }
    
    public HealthCheckResult(HealthStatus status) {
        this();
        this.status = status;
    }
    
    public static HealthCheckResult up() {
        return new HealthCheckResult(HealthStatus.UP);
    }
    
    public static HealthCheckResult down() {
        return new HealthCheckResult(HealthStatus.DOWN);
    }
    
    public static HealthCheckResult down(String error) {
        HealthCheckResult result = new HealthCheckResult(HealthStatus.DOWN);
        result.setError(error);
        return result;
    }
    
    public static HealthCheckResult unknown() {
        return new HealthCheckResult(HealthStatus.UNKNOWN);
    }
    
    public static HealthCheckResult outOfService() {
        return new HealthCheckResult(HealthStatus.OUT_OF_SERVICE);
    }
    
    public HealthCheckResult withDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
    
    public HealthCheckResult withResponseTime(long responseTime) {
        this.responseTime = responseTime;
        return this;
    }
    
    public HealthCheckResult withSuggestion(String suggestion) {
        this.suggestion = suggestion;
        return this;
    }
    
    // Getters and Setters
    public HealthStatus getStatus() { return status; }
    public void setStatus(HealthStatus status) { this.status = status; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}
