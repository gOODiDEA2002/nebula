package io.nebula.discovery.nacos.config;

import io.nebula.discovery.nacos.NacosServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 服务发现自动配置类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(NacosProperties.class)
@ConditionalOnProperty(prefix = "nebula.discovery.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosDiscoveryAutoConfiguration {
    
    /**
     * 创建 NacosServiceDiscovery Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public NacosServiceDiscovery nacosServiceDiscovery(NacosProperties nacosProperties) {
        log.info("初始化 Nacos 服务发现, serverAddr={}, namespace={}", 
                nacosProperties.getServerAddr(), nacosProperties.getNamespace());
        return new NacosServiceDiscovery(nacosProperties);
    }
}


