package io.nebula.web.health;

/**
 * 健康检查器接口
 * 
 * @author nebula
 */
public interface HealthChecker {
    
    /**
     * 获取检查器名称
     * 
     * @return 检查器名称
     */
    String getName();
    
    /**
     * 执行健康检查
     * 
     * @return 健康检查结果
     */
    HealthCheckResult check();
    
    /**
     * 获取检查器优先级（数值越小优先级越高）
     * 
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }
    
    /**
     * 是否启用该检查器
     * 
     * @return true 启用，false 禁用
     */
    default boolean isEnabled() {
        return true;
    }
}
