package io.nebula.web.health.checkers;

import io.nebula.web.health.HealthCheckResult;
import io.nebula.web.health.HealthChecker;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 应用程序健康检查器
 * 
 * @author nebula
 */
public class ApplicationHealthChecker implements HealthChecker {
    
    private final ApplicationContext applicationContext;
    private final LocalDateTime startTime;
    
    public ApplicationHealthChecker(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.startTime = LocalDateTime.now();
    }
    
    @Override
    public String getName() {
        return "application";
    }
    
    @Override
    public HealthCheckResult check() {
        long checkStartTime = System.currentTimeMillis();
        
        try {
            // 检查 Spring 应用上下文是否活跃
            boolean isActive = applicationContext != null && 
                (applicationContext instanceof ConfigurableApplicationContext ? 
                    ((ConfigurableApplicationContext) applicationContext).isActive() : true);
            
            if (!isActive) {
                return HealthCheckResult.down("应用程序上下文未激活")
                    .withResponseTime(System.currentTimeMillis() - checkStartTime);
            }
            
            // 获取应用信息
            long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            String uptime = formatUptime(uptimeMillis);
            
            // 获取线程信息
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            int activeThreads = rootGroup.activeCount();
            
            return HealthCheckResult.up()
                .withDetail("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("uptime", uptime)
                .withDetail("activeThreads", activeThreads)
                .withDetail("contextActive", isActive)
                .withDetail("javaVersion", System.getProperty("java.version"))
                .withDetail("javaVendor", System.getProperty("java.vendor"))
                .withDetail("osName", System.getProperty("os.name"))
                .withDetail("osVersion", System.getProperty("os.version"))
                .withDetail("osArch", System.getProperty("os.arch"))
                .withResponseTime(System.currentTimeMillis() - checkStartTime);
                
        } catch (Exception e) {
            return HealthCheckResult.down("应用程序检查失败: " + e.getMessage())
                .withResponseTime(System.currentTimeMillis() - checkStartTime);
        }
    }
    
    private String formatUptime(long uptimeMillis) {
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d天 %d小时 %d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟 %d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    @Override
    public int getOrder() {
        return 10; // 最高优先级
    }
}
