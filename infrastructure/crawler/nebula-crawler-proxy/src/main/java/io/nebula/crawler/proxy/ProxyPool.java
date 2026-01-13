package io.nebula.crawler.proxy;

import io.nebula.crawler.core.proxy.Proxy;
import io.nebula.crawler.core.proxy.ProxyProvider;
import io.nebula.crawler.core.proxy.ProxyValidator;
import io.nebula.crawler.proxy.config.ProxyPoolProperties;
import io.nebula.crawler.proxy.provider.ProxySource;
import io.nebula.data.cache.manager.CacheManager;
import io.nebula.core.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * 代理池管理
 * <p>
 * 提供代理获取、验证、轮换功能，使用Redis缓存代理列表
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class ProxyPool implements ProxyProvider {
    
    private static final String PROXY_LIST_KEY = "nebula:crawler:proxy:list";
    private static final String PROXY_BLACKLIST_KEY = "nebula:crawler:proxy:blacklist";
    
    private final ProxyPoolProperties properties;
    private final CacheManager cacheManager;
    private final List<ProxySource> proxySources;
    private final ProxyChecker proxyChecker;
    private final ScheduledExecutorService scheduler;
    private final Random random = new Random();
    
    /**
     * 构造函数
     *
     * @param properties   代理池配置
     * @param cacheManager 缓存服务
     * @param proxySources 代理源列表
     */
    public ProxyPool(ProxyPoolProperties properties,
                     CacheManager cacheManager,
                     List<ProxySource> proxySources) {
        this.properties = properties;
        this.cacheManager = cacheManager;
        this.proxySources = proxySources != null ? proxySources : new ArrayList<>();
        this.proxyChecker = new ProxyChecker(properties.getCheckUrl(), properties.getCheckTimeout());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "proxy-pool-checker");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 首次刷新代理池
        if (getAvailableCount() < properties.getMinAvailable()) {
            refresh();
        }
        
        // 启动定期检测任务
        startCheckTask();
        
        log.info("ProxyPool初始化完成: sources={}, available={}",
            proxySources.size(), getAvailableCount());
    }
    
    /**
     * 启动检测任务
     */
    private void startCheckTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.debug("开始检测代理可用性...");
                List<Proxy> proxies = getAvailableProxies();
                
                for (Proxy proxy : proxies) {
                    ProxyValidator.ValidationResult result = proxyChecker.validate(proxy);
                    if (!result.isValid()) {
                        reportFailure(proxy, "检测失败: " + result.message());
                    } else {
                        proxy.updateResponseTime(result.responseTime());
                        updateProxy(proxy);
                    }
                }
                
                // 如果代理数量不足，触发刷新
                if (getAvailableCount() < properties.getMinAvailable()) {
                    log.info("代理数量不足，触发刷新");
                    refresh();
                }
                
                log.debug("代理检测完成，当前可用: {}", getAvailableCount());
                
            } catch (Exception e) {
                log.error("代理检测任务异常: {}", e.getMessage());
            }
        }, properties.getCheckInterval(), properties.getCheckInterval(), TimeUnit.MILLISECONDS);
    }
    
    @Override
    public Proxy getProxy() {
        List<Proxy> proxies = getAvailableProxies();
        if (proxies.isEmpty()) {
            log.warn("没有可用代理，触发刷新");
            refresh();
            proxies = getAvailableProxies();
        }
        
        if (proxies.isEmpty()) {
            return null;
        }
        
        // 随机选择一个代理
        Proxy proxy = proxies.get(random.nextInt(proxies.size()));
        proxy.setLastUseTime(System.currentTimeMillis());
        return proxy;
    }
    
    @Override
    public List<Proxy> getProxies(int count) {
        List<Proxy> proxies = getAvailableProxies();
        if (proxies.size() <= count) {
            return new ArrayList<>(proxies);
        }
        
        // 随机打乱后返回指定数量
        List<Proxy> shuffled = new ArrayList<>(proxies);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, count);
    }
    
    @Override
    public void reportSuccess(Proxy proxy) {
        if (proxy == null) return;
        
        proxy.incrementSuccess();
        updateProxy(proxy);
    }
    
    @Override
    public void reportFailure(Proxy proxy, String reason) {
        if (proxy == null) return;
        
        proxy.incrementFail();
        
        if (proxy.getFailCount() >= properties.getMaxFailCount()) {
            // 加入黑名单
            addToBlacklist(proxy);
            removeProxy(proxy);
            log.info("代理加入黑名单: {}, reason={}", proxy.toAddress(), reason);
        } else {
            updateProxy(proxy);
            log.debug("代理失败计数增加: {}, failCount={}", proxy.toAddress(), proxy.getFailCount());
        }
    }
    
    @Override
    public int getAvailableCount() {
        return getAvailableProxies().size();
    }
    
    @Override
    public void refresh() {
        log.info("刷新代理池...");
        
        int addedCount = 0;
        
        for (ProxySource source : proxySources) {
            try {
                List<Proxy> newProxies = source.fetch();
                log.info("从 {} 获取到 {} 个代理", source.getName(), newProxies.size());
                
                // 验证并添加
                for (Proxy proxy : newProxies) {
                    if (!isBlacklisted(proxy)) {
                        ProxyValidator.ValidationResult result = proxyChecker.validate(proxy);
                        if (result.isValid()) {
                            proxy.updateResponseTime(result.responseTime());
                            addProxy(proxy);
                            addedCount++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("从 {} 获取代理失败: {}", source.getName(), e.getMessage());
            }
        }
        
        log.info("代理池刷新完成，新增: {}，当前可用: {}", addedCount, getAvailableCount());
    }
    
    @Override
    public void clear() {
        cacheManager.delete(PROXY_LIST_KEY);
        log.info("代理池已清空");
    }
    
    /**
     * 获取可用代理列表
     */
    private List<Proxy> getAvailableProxies() {
        return cacheManager.get(PROXY_LIST_KEY, String.class)
            .map(json -> {
                try {
                    return JsonUtils.toList(json, Proxy.class);
                } catch (Exception e) {
                    log.warn("解析代理列表失败: {}", e.getMessage());
                    return new ArrayList<Proxy>();
                }
            })
            .orElse(new ArrayList<>());
    }
    
    /**
     * 添加代理
     */
    private void addProxy(Proxy proxy) {
        List<Proxy> proxies = getAvailableProxies();
        
        // 检查是否已存在
        boolean exists = proxies.stream()
            .anyMatch(p -> p.getHost().equals(proxy.getHost()) && p.getPort() == proxy.getPort());
        
        if (!exists) {
            proxies.add(proxy);
            saveProxies(proxies);
        }
    }
    
    /**
     * 更新代理
     */
    private void updateProxy(Proxy proxy) {
        List<Proxy> proxies = getAvailableProxies();
        proxies.removeIf(p -> p.getHost().equals(proxy.getHost()) && p.getPort() == proxy.getPort());
        proxies.add(proxy);
        saveProxies(proxies);
    }
    
    /**
     * 移除代理
     */
    private void removeProxy(Proxy proxy) {
        List<Proxy> proxies = getAvailableProxies();
        proxies.removeIf(p -> p.getHost().equals(proxy.getHost()) && p.getPort() == proxy.getPort());
        saveProxies(proxies);
    }
    
    /**
     * 保存代理列表
     */
    private void saveProxies(List<Proxy> proxies) {
        cacheManager.set(PROXY_LIST_KEY, JsonUtils.toJson(proxies));
    }
    
    /**
     * 添加到黑名单
     */
    private void addToBlacklist(Proxy proxy) {
        cacheManager.sAdd(PROXY_BLACKLIST_KEY, proxy.toAddress());
        cacheManager.expire(PROXY_BLACKLIST_KEY, Duration.ofHours(properties.getBlacklistExpireHours()));
    }
    
    /**
     * 检查是否在黑名单中
     */
    private boolean isBlacklisted(Proxy proxy) {
        return cacheManager.sIsMember(PROXY_BLACKLIST_KEY, proxy.toAddress());
    }
    
    /**
     * 关闭
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("ProxyPool已关闭");
    }
    
    /**
     * 获取统计信息
     */
    public ProxyPoolStats getStats() {
        List<Proxy> proxies = getAvailableProxies();
        long blacklistSize = cacheManager.sCard(PROXY_BLACKLIST_KEY);
        
        double avgResponseTime = proxies.stream()
            .mapToLong(Proxy::getResponseTime)
            .average()
            .orElse(0);
        
        double avgSuccessRate = proxies.stream()
            .mapToDouble(Proxy::getSuccessRate)
            .average()
            .orElse(0);
        
        return new ProxyPoolStats(
            proxies.size(),
            (int) blacklistSize,
            avgResponseTime,
            avgSuccessRate
        );
    }
    
    /**
     * 代理池统计信息
     */
    public record ProxyPoolStats(
        int availableCount,
        int blacklistCount,
        double avgResponseTime,
        double avgSuccessRate
    ) {}
}

