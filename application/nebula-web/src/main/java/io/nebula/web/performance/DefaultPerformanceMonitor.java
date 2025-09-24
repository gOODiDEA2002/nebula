package io.nebula.web.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.management.*;
import java.util.List;

/**
 * 默认性能监控实现
 * 
 * @author nebula
 */
public class DefaultPerformanceMonitor implements PerformanceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultPerformanceMonitor.class);
    
    private final PerformanceMetrics metrics = new PerformanceMetrics();
    private final long slowRequestThreshold;
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();
    
    // JMX Beans
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    
    public DefaultPerformanceMonitor(long slowRequestThreshold) {
        this.slowRequestThreshold = slowRequestThreshold;
    }
    
    @Override
    public long recordRequestStart(HttpServletRequest request) {
        metrics.incrementTotalRequests();
        metrics.incrementActiveRequests();
        
        String path = getRequestPath(request);
        metrics.incrementPathCount(path);
        
        return System.currentTimeMillis();
    }
    
    @Override
    public void recordRequestComplete(HttpServletRequest request, HttpServletResponse response, long startTime) {
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        metrics.decrementActiveRequests();
        metrics.addResponseTime(responseTime);
        metrics.incrementStatusCount(response.getStatus());
        metrics.updateLastUpdateTime();
        
        // 判断是否为成功请求
        if (response.getStatus() >= 200 && response.getStatus() < 400) {
            metrics.incrementSuccessfulRequests();
        } else {
            metrics.incrementFailedRequests();
        }
        
        // 判断是否为慢请求
        if (responseTime > slowRequestThreshold) {
            metrics.incrementSlowRequestCount();
            logger.warn("Slow request detected: {} {} - {}ms", 
                       request.getMethod(), getRequestPath(request), responseTime);
        }
        
        logger.debug("Request completed: {} {} - {}ms", 
                    request.getMethod(), getRequestPath(request), responseTime);
    }
    
    @Override
    public void recordRequestError(HttpServletRequest request, Throwable throwable, long startTime) {
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        metrics.decrementActiveRequests();
        metrics.addResponseTime(responseTime);
        metrics.incrementFailedRequests();
        metrics.incrementStatusCount(500); // 默认为服务器错误
        metrics.updateLastUpdateTime();
        
        logger.error("Request error: {} {} - {}ms, error: {}", 
                    request.getMethod(), getRequestPath(request), responseTime, throwable.getMessage());
    }
    
    @Override
    public PerformanceMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public void resetMetrics() {
        synchronized (metrics) {
            // 这里可以实现指标重置逻辑
            logger.info("Performance metrics reset");
        }
    }
    
    @Override
    public SystemMetrics getSystemMetrics() {
        SystemMetrics systemMetrics = new SystemMetrics();
        
        try {
            // 内存指标
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
            
            Runtime runtime = Runtime.getRuntime();
            systemMetrics.setTotalMemory(runtime.totalMemory());
            systemMetrics.setFreeMemory(runtime.freeMemory());
            systemMetrics.setMaxMemory(runtime.maxMemory());
            systemMetrics.setUsedMemory(runtime.totalMemory() - runtime.freeMemory());
            
            if (heapMemory != null) {
                long used = heapMemory.getUsed();
                long max = heapMemory.getMax();
                systemMetrics.setHeapMemoryUsage(max > 0 ? (double) used / max * 100 : 0);
            }
            
            if (nonHeapMemory != null) {
                systemMetrics.setNonHeapMemoryUsed(nonHeapMemory.getUsed());
            }
            
            // 线程指标
            systemMetrics.setActiveThreadCount(threadBean.getThreadCount());
            systemMetrics.setPeakThreadCount(threadBean.getPeakThreadCount());
            systemMetrics.setDaemonThreadCount(threadBean.getDaemonThreadCount());
            
            // 类加载指标
            systemMetrics.setLoadedClassCount(classLoadingBean.getLoadedClassCount());
            
            // 操作系统指标
            systemMetrics.setSystemLoadAverage(osBean.getSystemLoadAverage());
            
            // CPU 使用率（如果支持）
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                systemMetrics.setCpuUsage(sunOsBean.getProcessCpuLoad() * 100);
            }
            
            // GC 指标
            long totalGcCount = 0;
            long totalGcTime = 0;
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                totalGcCount += gcBean.getCollectionCount();
                totalGcTime += gcBean.getCollectionTime();
            }
            systemMetrics.setGcCount(totalGcCount);
            systemMetrics.setGcTime(totalGcTime);
            
        } catch (Exception e) {
            logger.warn("Error collecting system metrics: {}", e.getMessage());
        }
        
        return systemMetrics;
    }
    
    private String getRequestPath(HttpServletRequest request) {
        try {
            return urlPathHelper.getPathWithinApplication(request);
        } catch (Exception e) {
            return request.getRequestURI();
        }
    }
}
