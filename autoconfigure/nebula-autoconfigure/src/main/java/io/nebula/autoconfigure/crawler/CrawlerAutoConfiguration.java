package io.nebula.autoconfigure.crawler;

import io.nebula.crawler.core.CrawlerEngine;
import io.nebula.crawler.core.config.CrawlerProperties;
import io.nebula.crawler.core.proxy.ProxyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 爬虫模块自动配置
 * <p>
 * 统一管理爬虫相关的自动配置
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(CrawlerEngine.class)
@ConditionalOnProperty(prefix = "nebula.crawler", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CrawlerProperties.class)
@Import({
    HttpCrawlerAutoConfiguration.class,
    BrowserCrawlerAutoConfiguration.class,
    ProxyCrawlerAutoConfiguration.class,
    CaptchaCrawlerAutoConfiguration.class
})
public class CrawlerAutoConfiguration {
    
    /**
     * 爬虫引擎管理器
     */
    @Bean
    public CrawlerEngineManager crawlerEngineManager(
            java.util.List<CrawlerEngine> engines,
            java.util.Optional<ProxyProvider> proxyProvider) {
        log.info("初始化爬虫引擎管理器，可用引擎: {}", 
            engines.stream().map(e -> e.getType().name()).toList());
        return new CrawlerEngineManager(engines, proxyProvider.orElse(null));
    }
}

