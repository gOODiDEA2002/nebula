package io.nebula.rpc.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RPC 服务发现配置属性
 * 
 * 智能默认值设计：
 * - 默认启用 RPC 服务发现集成
 * - 当有服务发现模块（如 Nacos）时自动生效
 * - 可通过 enabled=false 显式禁用
 * - 配置校验确保参数有效
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "nebula.rpc.discovery")
public class RpcDiscoveryProperties {
    
    /**
     * 是否启用 RPC 服务发现集成
     * 默认启用，有服务发现模块时自动生效
     */
    private boolean enabled = true;
    
    /**
     * 负载均衡策略
     * 可选值: round_robin, random, weighted
     */
    @NotBlank(message = "负载均衡策略不能为空")
    @Pattern(regexp = "^(round_robin|random|weighted)$", 
             message = "负载均衡策略必须是: round_robin, random, weighted 之一")
    private String loadBalanceStrategy = "round_robin";
    
    /**
     * 服务实例缓存超时时间（秒）
     * 范围: 10 - 3600 秒
     */
    @Min(value = 10, message = "缓存超时时间不能小于 10 秒")
    @Max(value = 3600, message = "缓存超时时间不能大于 3600 秒")
    private int cacheTimeout = 300;
    
    /**
     * 是否启用服务实例缓存
     */
    private boolean enableCache = true;
    
    /**
     * 重试次数
     * 范围: 0 - 10
     */
    @Min(value = 0, message = "重试次数不能小于 0")
    @Max(value = 10, message = "重试次数不能大于 10")
    private int retryCount = 3;
    
    /**
     * 重试间隔（毫秒）
     * 范围: 100 - 60000 毫秒
     */
    @Min(value = 100, message = "重试间隔不能小于 100 毫秒")
    @Max(value = 60000, message = "重试间隔不能大于 60000 毫秒")
    private long retryInterval = 1000;
}

