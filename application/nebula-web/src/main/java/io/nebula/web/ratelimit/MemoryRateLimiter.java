package io.nebula.web.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的限流器实现
 * 使用滑动窗口算法，CAS 无锁化设计
 */
public class MemoryRateLimiter implements RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryRateLimiter.class);
    
    private final int maxRequestsPerWindow;
    private final long windowSizeInMillis;
    private final ConcurrentHashMap<String, WindowData> windows = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;
    
    public MemoryRateLimiter(int maxRequestsPerWindow, long windowSizeInMillis) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowSizeInMillis = windowSizeInMillis;
        
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-cleanup");
            t.setDaemon(true);
            return t;
        });
        long cleanupIntervalMs = Math.max(windowSizeInMillis * 2, 60_000);
        this.cleanupScheduler.scheduleAtFixedRate(this::cleanup, cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean tryAcquire(String key, int permits) {
        if (permits <= 0) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        WindowData window = windows.computeIfAbsent(key, k -> new WindowData(currentTime));
        
        // CAS 方式重置过期窗口
        long windowStart = window.getWindowStart();
        if (currentTime - windowStart >= windowSizeInMillis) {
            if (window.compareAndResetWindow(windowStart, currentTime)) {
                // 当前线程成功重置窗口
            }
            // 无论是否重置成功，都继续使用最新状态
        }
        
        // CAS 方式尝试增加计数
        while (true) {
            int current = window.getRequestCount();
            if (current + permits > maxRequestsPerWindow) {
                logger.debug("Rate limit exceeded for key: {}, current: {}, requested: {}, limit: {}",
                        key, current, permits, maxRequestsPerWindow);
                return false;
            }
            if (window.compareAndAddRequests(current, current + permits)) {
                return true;
            }
            // CAS 失败，自旋重试
        }
    }
    
    @Override
    public int getAvailablePermits(String key) {
        long currentTime = System.currentTimeMillis();
        WindowData window = windows.get(key);
        
        if (window == null) {
            return maxRequestsPerWindow;
        }
        
        if (currentTime - window.getWindowStart() >= windowSizeInMillis) {
            return maxRequestsPerWindow;
        }
        
        return Math.max(0, maxRequestsPerWindow - window.getRequestCount());
    }
    
    @Override
    public void reset(String key) {
        windows.remove(key);
        logger.debug("Rate limiter reset for key: {}", key);
    }
    
    /**
     * 清理过期的窗口数据，由定时任务自动调用
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        long threshold = windowSizeInMillis * 2;
        int removed = 0;
        var it = windows.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (currentTime - entry.getValue().getWindowStart() >= threshold) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            logger.debug("Rate limiter cleanup: removed {} expired entries, active: {}", removed, windows.size());
        }
    }
    
    public int getActiveKeysCount() {
        return windows.size();
    }
    
    public void shutdown() {
        cleanupScheduler.shutdown();
    }
    
    private static class WindowData {
        private final AtomicLong windowStart;
        private final AtomicInteger requestCount = new AtomicInteger(0);
        
        WindowData(long startTime) {
            this.windowStart = new AtomicLong(startTime);
        }
        
        long getWindowStart() {
            return windowStart.get();
        }
        
        int getRequestCount() {
            return requestCount.get();
        }
        
        boolean compareAndAddRequests(int expect, int update) {
            return requestCount.compareAndSet(expect, update);
        }
        
        boolean compareAndResetWindow(long expectStart, long newStart) {
            if (windowStart.compareAndSet(expectStart, newStart)) {
                requestCount.set(0);
                return true;
            }
            return false;
        }
    }
}
