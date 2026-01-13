package io.nebula.autoconfigure.crawler;

import io.nebula.crawler.browser.BrowserCrawlerEngine;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties;
import io.nebula.crawler.core.proxy.ProxyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * 浏览器爬虫自动配置
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
@Configuration
@ConditionalOnClass(BrowserCrawlerEngine.class)
@ConditionalOnProperty(prefix = "nebula.crawler.browser", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(BrowserCrawlerProperties.class)
public class BrowserCrawlerAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public BrowserCrawlerEngine browserCrawlerEngine(
            BrowserCrawlerProperties browserCrawlerProperties,
            Optional<ProxyProvider> proxyProvider) {
        log.info("初始化浏览器爬虫引擎");
        return new BrowserCrawlerEngine(browserCrawlerProperties, proxyProvider.orElse(null));
    }
}
