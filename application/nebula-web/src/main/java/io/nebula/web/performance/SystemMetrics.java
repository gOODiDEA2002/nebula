package io.nebula.web.performance;

import java.time.LocalDateTime;

/**
 * 系统指标数据类
 * 
 * @author nebula
 */
public class SystemMetrics {
    
    /**
     * CPU 使用率（百分比）
     */
    private double cpuUsage;
    
    /**
     * 总内存（字节）
     */
    private long totalMemory;
    
    /**
     * 已使用内存（字节）
     */
    private long usedMemory;
    
    /**
     * 空闲内存（字节）
     */
    private long freeMemory;
    
    /**
     * 最大可用内存（字节）
     */
    private long maxMemory;
    
    /**
     * JVM 堆内存使用率（百分比）
     */
    private double heapMemoryUsage;
    
    /**
     * JVM 非堆内存使用（字节）
     */
    private long nonHeapMemoryUsed;
    
    /**
     * 活跃线程数
     */
    private int activeThreadCount;
    
    /**
     * 峰值线程数
     */
    private int peakThreadCount;
    
    /**
     * 守护线程数
     */
    private int daemonThreadCount;
    
    /**
     * 已加载类数量
     */
    private long loadedClassCount;
    
    /**
     * GC 次数
     */
    private long gcCount;
    
    /**
     * GC 总耗时（毫秒）
     */
    private long gcTime;
    
    /**
     * 系统负载平均值
     */
    private double systemLoadAverage;
    
    /**
     * 采集时间
     */
    private LocalDateTime timestamp;
    
    public SystemMetrics() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 计算内存使用率
     */
    public double getMemoryUsagePercentage() {
        return totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0;
    }
    
    // Getters and Setters
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    
    public long getTotalMemory() { return totalMemory; }
    public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }
    
    public long getUsedMemory() { return usedMemory; }
    public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }
    
    public long getFreeMemory() { return freeMemory; }
    public void setFreeMemory(long freeMemory) { this.freeMemory = freeMemory; }
    
    public long getMaxMemory() { return maxMemory; }
    public void setMaxMemory(long maxMemory) { this.maxMemory = maxMemory; }
    
    public double getHeapMemoryUsage() { return heapMemoryUsage; }
    public void setHeapMemoryUsage(double heapMemoryUsage) { this.heapMemoryUsage = heapMemoryUsage; }
    
    public long getNonHeapMemoryUsed() { return nonHeapMemoryUsed; }
    public void setNonHeapMemoryUsed(long nonHeapMemoryUsed) { this.nonHeapMemoryUsed = nonHeapMemoryUsed; }
    
    public int getActiveThreadCount() { return activeThreadCount; }
    public void setActiveThreadCount(int activeThreadCount) { this.activeThreadCount = activeThreadCount; }
    
    public int getPeakThreadCount() { return peakThreadCount; }
    public void setPeakThreadCount(int peakThreadCount) { this.peakThreadCount = peakThreadCount; }
    
    public int getDaemonThreadCount() { return daemonThreadCount; }
    public void setDaemonThreadCount(int daemonThreadCount) { this.daemonThreadCount = daemonThreadCount; }
    
    public long getLoadedClassCount() { return loadedClassCount; }
    public void setLoadedClassCount(long loadedClassCount) { this.loadedClassCount = loadedClassCount; }
    
    public long getGcCount() { return gcCount; }
    public void setGcCount(long gcCount) { this.gcCount = gcCount; }
    
    public long getGcTime() { return gcTime; }
    public void setGcTime(long gcTime) { this.gcTime = gcTime; }
    
    public double getSystemLoadAverage() { return systemLoadAverage; }
    public void setSystemLoadAverage(double systemLoadAverage) { this.systemLoadAverage = systemLoadAverage; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
