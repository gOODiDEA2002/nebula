package io.nebula.autoconfigure.gateway;

import io.nebula.discovery.core.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Gateway LoadBalancer 自动配置
 * 
 * 当同时存在 Nebula ServiceDiscovery 和 Spring Cloud LoadBalancer 时，
 * 自动配置适配器，使得 Gateway 可以使用 lb:// URI 进行负载均衡。
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = {
    "org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier",
    "io.nebula.discovery.core.ServiceDiscovery"
})
@ConditionalOnBean(ServiceDiscovery.class)
@ConditionalOnProperty(prefix = "nebula.gateway.http", name = "use-discovery", havingValue = "true", matchIfMissing = false)
@LoadBalancerClients(defaultConfiguration = GatewayLoadBalancerConfiguration.class)
public class GatewayLoadBalancerAutoConfiguration {
    
    public GatewayLoadBalancerAutoConfiguration() {
        log.info("Nebula Gateway LoadBalancer 适配器已启用，将使用 Nebula ServiceDiscovery 进行服务发现");
    }
}

/**
 * Gateway LoadBalancer 配置类
 * 
 * 为每个服务提供自定义的 ServiceInstanceListSupplier
 */
@Slf4j
class GatewayLoadBalancerConfiguration {
    
    @Bean
    public ServiceInstanceListSupplier serviceInstanceListSupplier(
            ConfigurableApplicationContext context,
            ServiceDiscovery serviceDiscovery) {
        
        // 从环境中获取当前服务ID
        Environment env = context.getEnvironment();
        String serviceId = env.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        
        log.info("为服务 {} 创建 Nebula ServiceInstanceListSupplier", serviceId);
        
        return new NebulaServiceInstanceListSupplier(serviceId, serviceDiscovery);
    }
}

