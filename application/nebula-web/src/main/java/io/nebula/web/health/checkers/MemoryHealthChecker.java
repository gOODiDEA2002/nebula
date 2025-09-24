package io.nebula.web.health.checkers;

import io.nebula.web.health.HealthCheckResult;
import io.nebula.web.health.HealthChecker;
import io.nebula.web.health.HealthStatus;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * 内存健康检查器
 * 
 * @author nebula
 */
public class MemoryHealthChecker implements HealthChecker {
    
    private final double threshold; // 内存使用率阈值（百分比）
    
    public MemoryHealthChecker() {
        this(85.0); // 默认85%阈值
    }
    
    public MemoryHealthChecker(double threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public String getName() {
        return "memory";
    }
    
    @Override
    public HealthCheckResult check() {
        long startTime = System.currentTimeMillis();
        
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
            
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double usagePercentage = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;
            double heapUsagePercentage = heapMemory.getMax() > 0 ? 
                (double) heapMemory.getUsed() / heapMemory.getMax() * 100 : 0;
            
            HealthCheckResult result = usagePercentage <= threshold ? 
                HealthCheckResult.up() : HealthCheckResult.down("内存使用率过高");
            
            return result
                .withDetail("totalMemory", formatBytes(totalMemory))
                .withDetail("freeMemory", formatBytes(freeMemory))
                .withDetail("usedMemory", formatBytes(usedMemory))
                .withDetail("maxMemory", formatBytes(maxMemory))
                .withDetail("usagePercentage", String.format("%.2f%%", usagePercentage))
                .withDetail("heapUsed", formatBytes(heapMemory.getUsed()))
                .withDetail("heapMax", formatBytes(heapMemory.getMax()))
                .withDetail("heapUsagePercentage", String.format("%.2f%%", heapUsagePercentage))
                .withDetail("nonHeapUsed", formatBytes(nonHeapMemory.getUsed()))
                .withDetail("threshold", threshold + "%")
                .withResponseTime(System.currentTimeMillis() - startTime)
                .withSuggestion(usagePercentage > threshold ? 
                    "内存使用率过高，建议调整JVM参数或优化应用程序" : null);
                
        } catch (Exception e) {
            return HealthCheckResult.down("内存检查失败: " + e.getMessage())
                .withResponseTime(System.currentTimeMillis() - startTime);
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    @Override
    public int getOrder() {
        return 50;
    }
}
