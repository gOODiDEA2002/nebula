package io.nebula.web.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的限流器实现
 * 使用滑动窗口算法
 */
public class MemoryRateLimiter implements RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryRateLimiter.class);
    
    private final int maxRequestsPerWindow;
    private final long windowSizeInMillis;
    private final ConcurrentHashMap<String, WindowData> windows = new ConcurrentHashMap<>();
    
    public MemoryRateLimiter(int maxRequestsPerWindow, long windowSizeInMillis) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowSizeInMillis = windowSizeInMillis;
    }
    
    @Override
    public boolean tryAcquire(String key, int permits) {
        if (permits <= 0) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        WindowData window = windows.computeIfAbsent(key, k -> new WindowData());
        
        synchronized (window) {
            // 清理过期的窗口数据
            if (currentTime - window.getWindowStart() >= windowSizeInMillis) {
                window.reset(currentTime);
            }
            
            // 检查是否超过限制
            if (window.getRequestCount() + permits > maxRequestsPerWindow) {
                logger.debug("Rate limit exceeded for key: {}, current count: {}, requested: {}, limit: {}", 
                    key, window.getRequestCount(), permits, maxRequestsPerWindow);
                return false;
            }
            
            // 增加请求计数
            window.addRequests(permits);
            return true;
        }
    }
    
    @Override
    public int getAvailablePermits(String key) {
        long currentTime = System.currentTimeMillis();
        WindowData window = windows.get(key);
        
        if (window == null) {
            return maxRequestsPerWindow;
        }
        
        synchronized (window) {
            // 检查窗口是否过期
            if (currentTime - window.getWindowStart() >= windowSizeInMillis) {
                return maxRequestsPerWindow;
            }
            
            return Math.max(0, maxRequestsPerWindow - window.getRequestCount());
        }
    }
    
    @Override
    public void reset(String key) {
        windows.remove(key);
        logger.debug("Rate limiter reset for key: {}", key);
    }
    
    /**
     * 清理过期的窗口数据
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> {
            WindowData window = entry.getValue();
            synchronized (window) {
                return currentTime - window.getWindowStart() >= windowSizeInMillis * 2;
            }
        });
    }
    
    /**
     * 获取当前活跃的限流键数量
     */
    public int getActiveKeysCount() {
        return windows.size();
    }
    
    /**
     * 窗口数据
     */
    private static class WindowData {
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger requestCount = new AtomicInteger(0);
        
        public long getWindowStart() {
            return windowStart.get();
        }
        
        public int getRequestCount() {
            return requestCount.get();
        }
        
        public void addRequests(int count) {
            requestCount.addAndGet(count);
        }
        
        public void reset(long newWindowStart) {
            windowStart.set(newWindowStart);
            requestCount.set(0);
        }
    }
}
