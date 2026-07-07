package io.nebula.autoconfigure.gateway;

import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
import io.nebula.gateway.config.GatewayProperties;
import io.nebula.gateway.config.GatewayRedisAutoConfiguration;
import io.nebula.gateway.config.GatewayRoutesAutoConfiguration;
import io.nebula.gateway.config.RateLimitKeyResolverConfig;
import io.nebula.gateway.controller.GatewayHealthController;
import io.nebula.gateway.filter.LoggingGlobalFilter;
import io.nebula.gateway.util.ReactiveClientIpResolver;
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
 * 当检测到 Spring Cloud Gateway 在类路径中时自动配置网关组件。
 * IP 解析遵循可信代理策略（与 nebula-web 一致），默认不信任 XFF。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GatewayFilter")
@ConditionalOnProperty(prefix = "nebula.gateway", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(GatewayProperties.class)
@Import({ RateLimitKeyResolverConfig.class, GatewayRoutesAutoConfiguration.class, GatewayRedisAutoConfiguration.class,
        GatewayHealthController.class })
public class GatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReactiveClientIpResolver reactiveClientIpResolver(GatewayProperties gatewayProperties) {
        return new ReactiveClientIpResolver(gatewayProperties.getLogging().getTrustedProxies());
    }

    @Bean
    @ConditionalOnMissingBean(name = "loggingGlobalFilter")
    @ConditionalOnProperty(prefix = "nebula.gateway.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GlobalFilter loggingGlobalFilter(GatewayProperties gatewayProperties,
                                             ReactiveClientIpResolver clientIpResolver) {
        log.info("初始化 Nebula Gateway 日志过滤器");
        return new LoggingGlobalFilter(gatewayProperties, clientIpResolver);
    }

    @Bean
    NebulaComponentSummary gatewaySummary(GatewayProperties gatewayProperties) {
        var details = new java.util.LinkedHashMap<String, String>();

        if (gatewayProperties.getRateLimit().isEnabled()) {
            details.put("Rate Limit", "ENABLED (" + gatewayProperties.getRateLimit().getStrategy() + ")");
            details.put("Replenish Rate", String.valueOf(gatewayProperties.getRateLimit().getReplenishRate()));
            details.put("Burst Capacity", String.valueOf(gatewayProperties.getRateLimit().getBurstCapacity()));
        } else {
            details.put("Rate Limit", "DISABLED");
        }

        details.put("JWT Auth", gatewayProperties.getAuth().getJwt().isEnabled() ? "ENABLED" : "DISABLED");
        details.put("API Prefix", gatewayProperties.getRoutes().getApiPathPrefix());
        details.put("Route Count", String.valueOf(gatewayProperties.getRoutes().getDefinitions().size()));
        details.put("CORS", gatewayProperties.getCors().isEnabled() ? "ENABLED" : "DISABLED");

        var trustedProxies = gatewayProperties.getLogging().getTrustedProxies();
        details.put("Trusted Proxies", trustedProxies.isEmpty() ? "NONE (XFF ignored)" : String.join(", ", trustedProxies));

        return new SimpleComponentSummary("Infrastructure", "Gateway", true, 1100, details);
    }
}
