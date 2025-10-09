package io.nebula.rpc.core.autoconfigure;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.LoadBalancer;
import io.nebula.discovery.core.LoadBalancerFactory;
import io.nebula.discovery.core.LoadBalanceStrategy;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.core.discovery.ServiceDiscoveryRpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * RPC 与服务发现集成自动配置
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({ServiceDiscovery.class, RpcClient.class})
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
     */
    @Bean(name = "serviceDiscoveryRpcClient")
    @ConditionalOnMissingBean(name = "serviceDiscoveryRpcClient")
    public ServiceDiscoveryRpcClient serviceDiscoveryRpcClient(
            ServiceDiscovery serviceDiscovery,
            LoadBalancer loadBalancer,
            @org.springframework.beans.factory.annotation.Qualifier("httpRpcClient") 
            io.nebula.rpc.core.client.RpcClient delegateRpcClient) {
        
        ServiceDiscoveryRpcClient client = new ServiceDiscoveryRpcClient(
                serviceDiscovery, loadBalancer, delegateRpcClient);
        
        log.info("配置服务发现 RPC 客户端: serviceDiscovery={}, loadBalancer={}", 
                serviceDiscovery.getClass().getSimpleName(),
                loadBalancer.getClass().getSimpleName());
        
        return client;
    }
}

/**
 * RPC 服务发现配置属性
 */
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "nebula.rpc.discovery")
class RpcDiscoveryProperties {
    
    /**
     * 是否启用 RPC 服务发现集成
     */
    private boolean enabled = false;
    
    /**
     * 负载均衡策略
     */
    private String loadBalanceStrategy = "round_robin";
    
    /**
     * 服务实例缓存超时时间（秒）
     */
    private int cacheTimeout = 300;
    
    /**
     * 是否启用服务实例缓存
     */
    private boolean enableCache = true;
    
    /**
     * 重试次数
     */
    private int retryCount = 3;
    
    /**
     * 重试间隔（毫秒）
     */
    private long retryInterval = 1000;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getLoadBalanceStrategy() {
        return loadBalanceStrategy;
    }
    
    public void setLoadBalanceStrategy(String loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }
    
    public int getCacheTimeout() {
        return cacheTimeout;
    }
    
    public void setCacheTimeout(int cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }
    
    public boolean isEnableCache() {
        return enableCache;
    }
    
    public void setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public long getRetryInterval() {
        return retryInterval;
    }
    
    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }
}
