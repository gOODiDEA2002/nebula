package io.nebula.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网关路由和 CORS 自动配置
 * <p>
 * 将 nebula.gateway 的配置转换为 Spring Cloud Gateway 配置。
 * 服务发现场景统一使用 lb:// 协议交由 Spring Cloud LoadBalancer 动态路由，
 * 不再在启动时解析固定地址（避免路由钉死到单实例）。
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.gateway.config.GatewayProperties")
@ConditionalOnProperty(prefix = "nebula.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRoutesAutoConfiguration {

    private final GatewayProperties nebulaGatewayProperties;
    private final org.springframework.cloud.gateway.config.GatewayProperties springGatewayProperties;

    public GatewayRoutesAutoConfiguration(GatewayProperties nebulaGatewayProperties,
                                           org.springframework.cloud.gateway.config.GatewayProperties springGatewayProperties) {
        this.nebulaGatewayProperties = nebulaGatewayProperties;
        this.springGatewayProperties = springGatewayProperties;
    }

    @PostConstruct
    public void configureRoutes() {
        configureHttpProxyRoutes();
        configureCustomRoutes();
        configureDefaultFilters();
        
        log.info("Nebula Gateway 路由配置完成，共 {} 条路由", springGatewayProperties.getRoutes().size());
    }

    private void configureHttpProxyRoutes() {
        GatewayProperties.HttpProxyConfig httpConfig = nebulaGatewayProperties.getHttp();
        if (!httpConfig.isEnabled() || httpConfig.getServices().isEmpty()) {
            log.info("HTTP 代理未启用或无服务配置");
            return;
        }

        for (Map.Entry<String, GatewayProperties.HttpServiceConfig> entry : httpConfig.getServices().entrySet()) {
            String serviceName = entry.getKey();
            GatewayProperties.HttpServiceConfig serviceConfig = entry.getValue();
            
            if (!serviceConfig.isEnabled()) {
                continue;
            }
            
            List<String> apiPaths = serviceConfig.getApiPaths();
            if (apiPaths == null || apiPaths.isEmpty()) {
                log.warn("服务 {} 未配置 API 路径，跳过", serviceName);
                continue;
            }
            
            String targetUri = determineTargetUri(serviceName, serviceConfig, httpConfig.isUseDiscovery());
            
            RouteDefinition route = new RouteDefinition();
            route.setId("nebula-http-" + serviceName);
            route.setUri(URI.create(targetUri));
            route.setOrder(0);
            
            PredicateDefinition pathPredicate = new PredicateDefinition();
            pathPredicate.setName("Path");
            int idx = 0;
            for (String apiPath : apiPaths) {
                pathPredicate.addArg("_genkey_" + idx++, apiPath.trim());
            }
            route.setPredicates(List.of(pathPredicate));
            
            boolean exists = springGatewayProperties.getRoutes().stream()
                    .anyMatch(r -> route.getId().equals(r.getId()));
            if (!exists) {
                springGatewayProperties.getRoutes().add(route);
                log.info("配置 HTTP 代理路由: {} -> {}, paths={}", serviceName, targetUri, apiPaths);
            }
        }
    }
    
    /**
     * 确定目标 URI
     * <p>
     * 优先级：
     * 1. 静态地址配置（直连）
     * 2. lb:// 协议（通过 Spring Cloud LoadBalancer 动态路由 + Nebula ServiceDiscovery 适配）
     */
    private String determineTargetUri(String serviceName, 
                                       GatewayProperties.HttpServiceConfig serviceConfig,
                                       boolean useDiscovery) {
        if (serviceConfig.getAddress() != null && !serviceConfig.getAddress().isEmpty()) {
            return serviceConfig.getAddress();
        }
        
        if (useDiscovery) {
            String targetServiceName = serviceConfig.getServiceName() != null 
                    ? serviceConfig.getServiceName() 
                    : serviceName;
            return "lb://" + targetServiceName;
        }
        
        log.warn("服务 {} 无法确定目标 URI（未配置静态地址且未启用服务发现）", serviceName);
        return "no://op";
    }

    /**
     * 配置自定义路由
     */
    private void configureCustomRoutes() {
        List<GatewayProperties.RouteDefinition> definitions = nebulaGatewayProperties.getRoutes().getDefinitions();
        if (definitions == null || definitions.isEmpty()) {
            return;
        }

        for (GatewayProperties.RouteDefinition def : definitions) {
            RouteDefinition route = new RouteDefinition();
            route.setId(def.getId());
            route.setUri(URI.create(def.getUri()));
            route.setOrder(def.getOrder());

            // Path 谓词
            if (!def.getPaths().isEmpty()) {
                PredicateDefinition pathPredicate = new PredicateDefinition();
                pathPredicate.setName("Path");
                int idx = 0;
                for (String path : def.getPaths()) {
                    pathPredicate.addArg("_genkey_" + idx++, path.trim());
                }
                route.setPredicates(List.of(pathPredicate));
            }

            // 过滤器
            if (!def.getFilters().isEmpty()) {
                List<FilterDefinition> filters = def.getFilters().stream()
                        .map(filterName -> {
                            FilterDefinition filter = new FilterDefinition();
                            filter.setName(filterName);
                            return filter;
                        })
                        .collect(Collectors.toList());
                route.setFilters(filters);
            }

            springGatewayProperties.getRoutes().add(route);
            log.debug("添加自定义路由: id={}, paths={}", def.getId(), def.getPaths());
        }
    }

    /**
     * 配置默认过滤器
     */
    private void configureDefaultFilters() {
        List<String> defaultFilters = nebulaGatewayProperties.getRoutes().getDefaultFilters();
        if (defaultFilters == null || defaultFilters.isEmpty()) {
            return;
        }

        for (String filterName : defaultFilters) {
            FilterDefinition filter = new FilterDefinition();
            filter.setName(filterName);
            
            // 为 RequestRateLimiter 配置参数
            if ("RequestRateLimiter".equals(filterName)) {
                GatewayProperties.RateLimitConfig rateLimitConfig = nebulaGatewayProperties.getRateLimit();
                filter.addArg("redis-rate-limiter.replenishRate", String.valueOf(rateLimitConfig.getReplenishRate()));
                filter.addArg("redis-rate-limiter.burstCapacity", String.valueOf(rateLimitConfig.getBurstCapacity()));
                filter.addArg("redis-rate-limiter.requestedTokens", String.valueOf(rateLimitConfig.getRequestedTokens()));
                filter.addArg("key-resolver", "#{@defaultKeyResolver}");
                log.info("配置 RequestRateLimiter: replenishRate={}, burstCapacity={}", 
                        rateLimitConfig.getReplenishRate(), rateLimitConfig.getBurstCapacity());
            }
            
            springGatewayProperties.getDefaultFilters().add(filter);
        }
    }

    /**
     * CORS 配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "nebula.gateway.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CorsWebFilter corsWebFilter() {
        GatewayProperties.CorsConfig corsConfig = nebulaGatewayProperties.getCors();
        
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsConfig.getAllowedOrigins());
        config.setAllowedMethods(corsConfig.getAllowedMethods());
        config.setAllowedHeaders(corsConfig.getAllowedHeaders());
        config.setExposedHeaders(corsConfig.getExposedHeaders());
        config.setAllowCredentials(corsConfig.isAllowCredentials());
        config.setMaxAge(corsConfig.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        log.info("配置 CORS: allowedOrigins={}, allowedMethods={}", 
                corsConfig.getAllowedOrigins(), corsConfig.getAllowedMethods());
        
        return new CorsWebFilter(source);
    }
}
