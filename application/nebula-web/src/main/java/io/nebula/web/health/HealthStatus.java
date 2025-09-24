package io.nebula.web.health;

/**
 * 健康状态枚举
 * 
 * @author nebula
 */
public enum HealthStatus {
    
    /**
     * 健康状态
     */
    UP("UP", "系统运行正常"),
    
    /**
     * 不健康状态
     */
    DOWN("DOWN", "系统异常"),
    
    /**
     * 未知状态
     */
    UNKNOWN("UNKNOWN", "状态未知"),
    
    /**
     * 部分服务不可用
     */
    OUT_OF_SERVICE("OUT_OF_SERVICE", "服务不可用");
    
    private final String code;
    private final String description;
    
    HealthStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}
