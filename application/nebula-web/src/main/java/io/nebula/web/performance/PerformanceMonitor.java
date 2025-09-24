package io.nebula.web.performance;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 性能监控接口
 * 
 * @author nebula
 */
public interface PerformanceMonitor {
    
    /**
     * 记录请求开始
     * 
     * @param request HTTP 请求
     * @return 请求开始时间戳
     */
    long recordRequestStart(HttpServletRequest request);
    
    /**
     * 记录请求完成
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param startTime 请求开始时间戳
     */
    void recordRequestComplete(HttpServletRequest request, HttpServletResponse response, long startTime);
    
    /**
     * 记录请求异常
     * 
     * @param request HTTP 请求
     * @param throwable 异常
     * @param startTime 请求开始时间戳
     */
    void recordRequestError(HttpServletRequest request, Throwable throwable, long startTime);
    
    /**
     * 获取性能指标
     * 
     * @return 性能指标
     */
    PerformanceMetrics getMetrics();
    
    /**
     * 重置性能指标
     */
    void resetMetrics();
    
    /**
     * 获取系统指标
     * 
     * @return 系统指标
     */
    SystemMetrics getSystemMetrics();
}
