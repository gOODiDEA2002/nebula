package io.nebula.autoconfigure.crawler;

import io.nebula.crawler.core.CrawlerEngine;
import io.nebula.crawler.core.CrawlerEngineType;
import io.nebula.crawler.core.proxy.ProxyProvider;
import lombok.Getter;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 爬虫引擎管理器
 * <p>
 * 管理多个爬虫引擎，提供统一的引擎获取接口
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public class CrawlerEngineManager {
    
    private final Map<CrawlerEngineType, CrawlerEngine> engineMap;
    
    @Getter
    private final ProxyProvider proxyProvider;
    
    /**
     * 构造函数
     *
     * @param engines       爬虫引擎列表
     * @param proxyProvider 代理提供者
     */
    public CrawlerEngineManager(List<CrawlerEngine> engines, ProxyProvider proxyProvider) {
        this.engineMap = new EnumMap<>(CrawlerEngineType.class);
        this.proxyProvider = proxyProvider;
        
        for (CrawlerEngine engine : engines) {
            engineMap.put(engine.getType(), engine);
        }
    }
    
    /**
     * 获取指定类型的爬虫引擎
     *
     * @param type 引擎类型
     * @return 爬虫引擎
     */
    public Optional<CrawlerEngine> getEngine(CrawlerEngineType type) {
        return Optional.ofNullable(engineMap.get(type));
    }
    
    /**
     * 获取HTTP爬虫引擎
     *
     * @return HTTP爬虫引擎
     */
    public Optional<CrawlerEngine> getHttpEngine() {
        return getEngine(CrawlerEngineType.HTTP);
    }
    
    /**
     * 获取浏览器爬虫引擎
     *
     * @return 浏览器爬虫引擎
     */
    public Optional<CrawlerEngine> getBrowserEngine() {
        return getEngine(CrawlerEngineType.BROWSER);
    }
    
    /**
     * 获取默认爬虫引擎（优先HTTP）
     *
     * @return 默认爬虫引擎
     */
    public CrawlerEngine getDefaultEngine() {
        return getHttpEngine()
            .or(this::getBrowserEngine)
            .orElseThrow(() -> new IllegalStateException("没有可用的爬虫引擎"));
    }
    
    /**
     * 检查指定类型的引擎是否可用
     *
     * @param type 引擎类型
     * @return true表示可用
     */
    public boolean hasEngine(CrawlerEngineType type) {
        return engineMap.containsKey(type);
    }
    
    /**
     * 获取所有可用引擎类型
     *
     * @return 引擎类型列表
     */
    public List<CrawlerEngineType> getAvailableEngineTypes() {
        return List.copyOf(engineMap.keySet());
    }
}

