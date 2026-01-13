package io.nebula.crawler.proxy.provider;

import io.nebula.crawler.core.proxy.Proxy;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 静态代理来源
 * <p>
 * 从配置文件中读取代理列表
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class StaticProxySource implements ProxySource {
    
    private final String name;
    private final List<String> proxyStrings;
    private final int priority;
    
    /**
     * 构造函数
     *
     * @param name         来源名称
     * @param proxyStrings 代理字符串列表，格式: host:port 或 host:port:user:pass
     * @param priority     优先级
     */
    public StaticProxySource(String name, List<String> proxyStrings, int priority) {
        this.name = name;
        this.proxyStrings = proxyStrings != null ? proxyStrings : new ArrayList<>();
        this.priority = priority;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getType() {
        return "static";
    }
    
    @Override
    public List<Proxy> fetch() {
        List<Proxy> proxies = new ArrayList<>();
        
        for (String proxyString : proxyStrings) {
            try {
                Proxy proxy = Proxy.parse(proxyString);
                if (proxy != null) {
                    proxies.add(proxy);
                }
            } catch (Exception e) {
                log.warn("解析代理失败: {}, error={}", proxyString, e.getMessage());
            }
        }
        
        log.debug("StaticProxySource[{}] 返回 {} 个代理", name, proxies.size());
        return proxies;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
}

