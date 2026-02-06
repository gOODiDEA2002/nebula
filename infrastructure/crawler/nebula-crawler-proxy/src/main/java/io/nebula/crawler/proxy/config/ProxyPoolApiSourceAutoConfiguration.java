package io.nebula.crawler.proxy.config;

import io.nebula.crawler.proxy.provider.ProxyPoolApiSource;
import io.nebula.crawler.proxy.provider.ProxySource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * proxy-pool 代理源自动配置
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ProxyPoolProperties.class)
@ConditionalOnProperty(prefix = "nebula.crawler.proxy", name = "enabled", havingValue = "true")
public class ProxyPoolApiSourceAutoConfiguration {

    /**
     * 注册 proxy-pool 代理源
     */
    @Bean
    @ConditionalOnProperty(prefix = "nebula.crawler.proxy.proxy-pool-service", name = "enabled", havingValue = "true")
    public ProxySource proxyPoolApiSource(ProxyPoolProperties properties) {
        log.info("启用 proxy-pool 代理源: baseUrl={}",
                properties.getProxyPoolService().getBaseUrl());
        return new ProxyPoolApiSource(properties.getProxyPoolService());
    }
}
