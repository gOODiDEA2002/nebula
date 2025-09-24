package io.nebula.web.health.checkers;

import io.nebula.web.health.HealthCheckResult;
import io.nebula.web.health.HealthChecker;
import io.nebula.web.health.HealthStatus;

import java.io.File;

/**
 * 磁盘空间健康检查器
 * 
 * @author nebula
 */
public class DiskSpaceHealthChecker implements HealthChecker {
    
    private final long threshold;
    private final String path;
    
    public DiskSpaceHealthChecker() {
        this(1024 * 1024 * 1024L, System.getProperty("user.dir")); // 默认1GB阈值
    }
    
    public DiskSpaceHealthChecker(long threshold, String path) {
        this.threshold = threshold;
        this.path = path;
    }
    
    @Override
    public String getName() {
        return "diskSpace";
    }
    
    @Override
    public HealthCheckResult check() {
        long startTime = System.currentTimeMillis();
        
        try {
            File file = new File(path);
            long freeSpace = file.getFreeSpace();
            long totalSpace = file.getTotalSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double usagePercentage = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
            
            HealthCheckResult result = freeSpace >= threshold ? 
                HealthCheckResult.up() : HealthCheckResult.down("磁盘空间不足");
            
            return result
                .withDetail("path", path)
                .withDetail("total", formatBytes(totalSpace))
                .withDetail("free", formatBytes(freeSpace))
                .withDetail("used", formatBytes(usedSpace))
                .withDetail("usagePercentage", String.format("%.2f%%", usagePercentage))
                .withDetail("threshold", formatBytes(threshold))
                .withResponseTime(System.currentTimeMillis() - startTime)
                .withSuggestion(freeSpace < threshold ? "请清理磁盘空间或增加存储容量" : null);
                
        } catch (Exception e) {
            return HealthCheckResult.down("磁盘空间检查失败: " + e.getMessage())
                .withDetail("path", path)
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
        return 100;
    }
}
