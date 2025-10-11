package io.nebula.autoconfigure.discovery;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.nacos.NacosServiceAutoRegistrar;
import io.nebula.discovery.nacos.NacosServiceDiscovery;
import io.nebula.discovery.nacos.config.NacosProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Nacos 服务发现自动配置类
 * 集中管理 Nacos 相关的 Bean 配置
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({ServiceDiscovery.class, NacosServiceDiscovery.class})
@ConditionalOnProperty(prefix = "nebula.discovery.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosDiscoveryAutoConfiguration {
    
    /**
     * 创建 NacosServiceDiscovery Bean
     * 使用Binder手动绑定属性，避免@ConfigurationProperties绑定时机问题
     * 
     * @param environment Spring环境
     */
    @Bean
    @ConditionalOnMissingBean
    public NacosServiceDiscovery nacosServiceDiscovery(Environment environment) {
        log.info("创建 NacosServiceDiscovery Bean");
        
        // 使用 Binder 手动绑定属性
        NacosProperties nacosProperties = Binder.get(environment)
            .bind("nebula.discovery.nacos", NacosProperties.class)
            .orElseGet(NacosProperties::new);
        
        log.info("  - serverAddr: {}", nacosProperties.getServerAddr());
        log.info("  - namespace: {}", nacosProperties.getNamespace());
        log.info("  - username: {}", nacosProperties.getUsername());
        log.info("  - password: {}", nacosProperties.getPassword() != null && !nacosProperties.getPassword().isEmpty() ? "****" : "null");
        log.info("  - groupName: {}", nacosProperties.getGroupName());
        log.info("  - clusterName: {}", nacosProperties.getClusterName());
        log.info("  - autoRegister: {}", nacosProperties.isAutoRegister());
        
        return new NacosServiceDiscovery(nacosProperties);
    }
    
    /**
     * 创建 Nacos 服务自动注册器 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "nebula.discovery.nacos", name = "auto-register", havingValue = "true", matchIfMissing = true)
    public NacosServiceAutoRegistrar nacosServiceAutoRegistrar(ServiceDiscovery serviceDiscovery,
                                                              Environment environment) {
        log.info("配置 Nacos 服务自动注册器");
        
        // 使用 Binder 手动绑定属性
        NacosProperties nacosProperties = Binder.get(environment)
            .bind("nebula.discovery.nacos", NacosProperties.class)
            .orElseGet(NacosProperties::new);
        
        return new NacosServiceAutoRegistrar(serviceDiscovery, nacosProperties, environment);
    }
}

