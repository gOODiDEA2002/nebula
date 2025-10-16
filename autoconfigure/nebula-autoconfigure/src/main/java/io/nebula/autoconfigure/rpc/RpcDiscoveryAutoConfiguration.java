package io.nebula.autoconfigure.rpc;

import io.nebula.autoconfigure.discovery.NacosDiscoveryAutoConfiguration;
import io.nebula.discovery.core.LoadBalanceStrategy;
import io.nebula.discovery.core.LoadBalancer;
import io.nebula.discovery.core.LoadBalancerFactory;
import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.core.config.RpcDiscoveryProperties;
import io.nebula.rpc.core.discovery.ServiceDiscoveryRpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * RPC 与服务发现集成自动配置
 * 必须在 NacosDiscoveryAutoConfiguration 之后初始化
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(NacosDiscoveryAutoConfiguration.class)  // 关键：确保服务发现先初始化
@ConditionalOnClass({ServiceDiscovery.class, RpcClient.class})
@ConditionalOnBean(ServiceDiscovery.class)  // 确保 ServiceDiscovery Bean 存在
@EnableConfigurationProperties(RpcDiscoveryProperties.class)
@ConditionalOnProperty(prefix = "nebula.rpc.discovery", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RpcDiscoveryAutoConfiguration {
    
    /**
     * 负载均衡器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer loadBalancer(RpcDiscoveryProperties properties) {
        LoadBalanceStrategy strategy = LoadBalanceStrategy.valueOf(
                properties.getLoadBalanceStrategy().toUpperCase());
        
        LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(strategy);
        
        log.info("配置负载均衡器: strategy={}", strategy);
        return loadBalancer;
    }
    
    /**
     * 服务发现 RPC 客户端配置
     * 自动注入 @Primary 标记的 RpcClient Bean
     * - 如果 gRPC 启用（nebula.rpc.grpc.enabled=true），则使用 GrpcRpcClient
     * - 否则使用 HttpRpcClient
     */
    @Bean(name = "serviceDiscoveryRpcClient")
    @ConditionalOnMissingBean(name = "serviceDiscoveryRpcClient")
    public ServiceDiscoveryRpcClient serviceDiscoveryRpcClient(
            ServiceDiscovery serviceDiscovery,
            LoadBalancer loadBalancer,
            RpcClient delegateRpcClient,  // 移除 @Qualifier，让 @Primary 生效
            Environment environment) {
        
        ServiceDiscoveryRpcClient client = new ServiceDiscoveryRpcClient(
                serviceDiscovery, loadBalancer, delegateRpcClient, environment);
        
        log.info("配置服务发现 RPC 客户端: serviceDiscovery={}, loadBalancer={}, delegateClient={}", 
                serviceDiscovery.getClass().getSimpleName(),
                loadBalancer.getClass().getSimpleName(),
                delegateRpcClient.getClass().getSimpleName());  // 新增：记录委托客户端类型
        
        return client;
    }
}

