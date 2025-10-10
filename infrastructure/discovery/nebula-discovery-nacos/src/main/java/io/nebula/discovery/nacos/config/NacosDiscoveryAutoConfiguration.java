package io.nebula.discovery.nacos.config;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.nacos.NacosServiceAutoRegistrar;
import io.nebula.discovery.nacos.NacosServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.AutoConfiguration;
/**
 * Nacos 服务发现自动配置类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "nebula.discovery.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NacosProperties.class)
public class NacosDiscoveryAutoConfiguration {
    
    private final NacosProperties nacosProperties;
    
    public NacosDiscoveryAutoConfiguration(NacosProperties nacosProperties) {
        this.nacosProperties = nacosProperties;
        log.info("NacosDiscoveryAutoConfiguration 初始化, 配置: serverAddr={}, namespace={}, username={}", 
                nacosProperties.getServerAddr(), nacosProperties.getNamespace(), nacosProperties.getUsername());
    }
    
    /**
     * 创建 NacosServiceDiscovery Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public NacosServiceDiscovery nacosServiceDiscovery() {
        log.info("创建 NacosServiceDiscovery Bean, serverAddr={}, namespace={}, username={}, password={}", 
                nacosProperties.getServerAddr(), nacosProperties.getNamespace(),
                nacosProperties.getUsername(), nacosProperties.getPassword() != null ? "****" : "null");
        return new NacosServiceDiscovery(nacosProperties);
    }
    
    /**
     * 创建 Nacos 服务自动注册器 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "nebula.discovery.nacos", name = "auto-register", havingValue = "true", matchIfMissing = true)
    public NacosServiceAutoRegistrar nacosServiceAutoRegistrar(ServiceDiscovery serviceDiscovery,
                                                              NacosProperties nacosProperties,
                                                              Environment environment) {
        log.info("配置 Nacos 服务自动注册器");
        return new NacosServiceAutoRegistrar(serviceDiscovery, nacosProperties, environment);
    }
}


