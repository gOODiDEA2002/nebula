package io.nebula.autoconfigure.gateway;

import io.nebula.gateway.config.GatewayProperties;
import io.nebula.gateway.config.GatewayRedisAutoConfiguration;
import io.nebula.gateway.config.GatewayRoutesAutoConfiguration;
import io.nebula.gateway.config.RateLimitKeyResolverConfig;
import io.nebula.gateway.controller.GatewayHealthController;
import io.nebula.gateway.filter.LoggingGlobalFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 网关自动配置
 * <p>
 * 当检测到Spring Cloud Gateway在类路径中时自动配置网关组件
 * <p>
 * 基于微服务三原则优化：
 * - 前端接口通过 Controller 暴露（HTTP 代理）
 * - 服务间接口通过 RpcClient 暴露（纯 RPC，不经过 Gateway）
 * - Gateway 职责简化为：HTTP 反向代理、认证、限流、日志
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GatewayFilter")
@ConditionalOnProperty(prefix = "nebula.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GatewayProperties.class)
@Import({RateLimitKeyResolverConfig.class, GatewayRoutesAutoConfiguration.class, GatewayRedisAutoConfiguration.class, GatewayHealthController.class})
public class GatewayAutoConfiguration {
    
    // 注意：JWT 认证过滤器已移至应用层实现（如 ticket-gateway）
    // 框架不再内置 JWT 认证，由各应用根据业务需求自行实现
    
    /**
     * 全局日志过滤器
     * <p>
     * 功能：
     * - 为每个请求生成唯一的RequestId
     * - 记录请求开始和结束日志
     * - 统计请求耗时
     * - 标记慢请求
     */
    @Bean
    @ConditionalOnMissingBean(name = "loggingGlobalFilter")
    @ConditionalOnProperty(prefix = "nebula.gateway.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GlobalFilter loggingGlobalFilter(GatewayProperties gatewayProperties) {
        log.info("初始化Nebula Gateway日志过滤器");
        return new LoggingGlobalFilter(gatewayProperties);
    }
}
