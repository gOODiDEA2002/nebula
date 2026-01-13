package io.nebula.autoconfigure.crawler;

import io.nebula.crawler.proxy.ProxyPool;
import io.nebula.crawler.proxy.config.ProxyPoolProperties;
import io.nebula.crawler.proxy.provider.ApiProxySource;
import io.nebula.crawler.proxy.provider.ProxySource;
import io.nebula.crawler.proxy.provider.StaticProxySource;
import io.nebula.data.cache.manager.CacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理池自动配置
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
@Configuration
@ConditionalOnClass(ProxyPool.class)
@ConditionalOnProperty(prefix = "nebula.crawler.proxy", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ProxyPoolProperties.class)
public class ProxyCrawlerAutoConfiguration {
    
    /**
     * 代理来源列表
     */
    @Bean
    @ConditionalOnMissingBean
    public List<ProxySource> proxySources(ProxyPoolProperties properties) {
        List<ProxySource> sources = new ArrayList<>();
        
        // 静态代理来源
        if (properties.getStaticProxies() != null && !properties.getStaticProxies().isEmpty()) {
            sources.add(new StaticProxySource("static", properties.getStaticProxies(), 1));
            log.info("注册静态代理来源，代理数量: {}", properties.getStaticProxies().size());
        }
        
        // API代理来源
        if (properties.getApiSources() != null) {
            for (ProxyPoolProperties.ApiSource apiSource : properties.getApiSources()) {
                ApiProxySource.ResponseParser parser;
                if ("json".equalsIgnoreCase(apiSource.getFormat())) {
                    parser = new ApiProxySource.JsonArrayParser(
                        apiSource.getHostField(),
                        apiSource.getPortField(),
                        apiSource.getUsernameField(),
                        apiSource.getPasswordField()
                    );
                } else {
                    parser = new ApiProxySource.SimpleTextParser();
                }
                
                sources.add(new ApiProxySource(
                    apiSource.getName(),
                    apiSource.getUrl(),
                    parser,
                    apiSource.getPriority()
                ));
                log.info("注册API代理来源: {}", apiSource.getName());
            }
        }
        
        return sources;
    }
    
    /**
     * 代理池
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CacheManager.class)
    public ProxyPool proxyPool(
            ProxyPoolProperties properties,
            CacheManager cacheManager,
            List<ProxySource> proxySources) {
        log.info("初始化代理池，来源数量: {}", proxySources.size());
        return new ProxyPool(properties, cacheManager, proxySources);
    }
}

