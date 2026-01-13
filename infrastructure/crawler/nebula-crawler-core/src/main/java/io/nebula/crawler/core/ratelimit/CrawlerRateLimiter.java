package io.nebula.crawler.core.ratelimit;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 爬虫限流器
 * <p>
 * 基于Guava RateLimiter实现，支持按域名/数据源限流
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class CrawlerRateLimiter {
    
    /**
     * 默认QPS
     */
    private final double defaultQps;
    
    /**
     * 域名/数据源限流器缓存
     */
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    /**
     * 自定义限流规则
     */
    private final Map<String, Double> customRules = new ConcurrentHashMap<>();
    
    public CrawlerRateLimiter(double defaultQps) {
        this.defaultQps = defaultQps;
    }
    
    /**
     * 添加自定义限流规则
     *
     * @param key 限流键（域名或数据源名称）
     * @param qps 每秒请求数
     */
    public void addRule(String key, double qps) {
        customRules.put(key, qps);
        // 如果已存在限流器，需要重新创建
        rateLimiters.remove(key);
    }
    
    /**
     * 获取令牌（阻塞）
     *
     * @param key 限流键
     */
    public void acquire(String key) {
        getRateLimiter(key).acquire();
    }
    
    /**
     * 获取多个令牌（阻塞）
     *
     * @param key     限流键
     * @param permits 令牌数量
     */
    public void acquire(String key, int permits) {
        getRateLimiter(key).acquire(permits);
    }
    
    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param key 限流键
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key) {
        return getRateLimiter(key).tryAcquire();
    }
    
    /**
     * 尝试获取令牌（带超时）
     *
     * @param key     限流键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, long timeout, TimeUnit unit) {
        return getRateLimiter(key).tryAcquire(timeout, unit);
    }
    
    /**
     * 获取或创建限流器
     */
    private RateLimiter getRateLimiter(String key) {
        return rateLimiters.computeIfAbsent(key, k -> {
            double qps = customRules.getOrDefault(k, defaultQps);
            log.debug("创建限流器: key={}, qps={}", k, qps);
            return RateLimiter.create(qps);
        });
    }
    
    /**
     * 从URL提取域名
     *
     * @param url URL
     * @return 域名
     */
    public static String extractDomain(String url) {
        if (url == null || url.isEmpty()) {
            return "default";
        }
        
        try {
            String domain = url;
            // 移除协议
            if (domain.contains("://")) {
                domain = domain.substring(domain.indexOf("://") + 3);
            }
            // 移除路径
            if (domain.contains("/")) {
                domain = domain.substring(0, domain.indexOf("/"));
            }
            // 移除端口
            if (domain.contains(":")) {
                domain = domain.substring(0, domain.indexOf(":"));
            }
            return domain;
        } catch (Exception e) {
            return "default";
        }
    }
    
    /**
     * 获取当前限流器数量
     */
    public int getRateLimiterCount() {
        return rateLimiters.size();
    }
    
    /**
     * 清除限流器缓存
     */
    public void clear() {
        rateLimiters.clear();
    }
}

