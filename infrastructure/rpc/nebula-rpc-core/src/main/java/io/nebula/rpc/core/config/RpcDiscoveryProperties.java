package io.nebula.rpc.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RPC 服务发现配置属性
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.rpc.discovery")
public class RpcDiscoveryProperties {
    
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
}

