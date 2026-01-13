package io.nebula.autoconfigure.crawler;

import io.nebula.crawler.core.proxy.ProxyProvider;
import io.nebula.crawler.http.HttpCrawlerEngine;
import io.nebula.crawler.http.config.HttpCrawlerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * HTTP爬虫自动配置
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
@Configuration
@ConditionalOnClass(HttpCrawlerEngine.class)
@ConditionalOnProperty(prefix = "nebula.crawler.http", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(HttpCrawlerProperties.class)
public class HttpCrawlerAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public HttpCrawlerEngine httpCrawlerEngine(
            HttpCrawlerProperties httpCrawlerProperties,
            Optional<ProxyProvider> proxyProvider) {
        log.info("初始化HTTP爬虫引擎");
        return new HttpCrawlerEngine(httpCrawlerProperties, proxyProvider.orElse(null));
    }
}
