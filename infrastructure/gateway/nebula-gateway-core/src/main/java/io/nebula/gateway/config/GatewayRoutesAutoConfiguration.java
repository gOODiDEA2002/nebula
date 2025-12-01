package io.nebula.gateway.config;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网关路由和CORS自动配置
 * <p>
 * 将 nebula.gateway 的配置转换为 Spring Cloud Gateway 配置
 * <p>
 * 基于微服务三原则优化：
 * - 前端接口通过 Controller 暴露（HTTP 代理转发）
 * - 服务间调用通过 RpcClient（不经过 Gateway）
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.gateway.config.GatewayProperties")
@ConditionalOnProperty(prefix = "nebula.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRoutesAutoConfiguration {

    private final GatewayProperties nebulaGatewayProperties;
    private final org.springframework.cloud.gateway.config.GatewayProperties springGatewayProperties;
    
    @Autowired(required = false)
    private ServiceDiscovery serviceDiscovery;

    public GatewayRoutesAutoConfiguration(GatewayProperties nebulaGatewayProperties,
                                           org.springframework.cloud.gateway.config.GatewayProperties springGatewayProperties) {
        this.nebulaGatewayProperties = nebulaGatewayProperties;
        this.springGatewayProperties = springGatewayProperties;
    }

    @PostConstruct
    public void configureRoutes() {
        // 配置 HTTP 代理路由
        configureHttpProxyRoutes();
        
        // 配置自定义路由
        configureCustomRoutes();
        
        // 配置默认过滤器
        configureDefaultFilters();
        
        log.info("Nebula Gateway 路由配置完成，共 {} 条路由", springGatewayProperties.getRoutes().size());
    }

    /**
     * 根据 HTTP 代理服务配置自动生成路由
     * <p>
     * 为每个配置的服务生成路由，将请求代理到后端服务的 Controller
     */
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
            
            // 获取 API 路径
            List<String> apiPaths = serviceConfig.getApiPaths();
            if (apiPaths == null || apiPaths.isEmpty()) {
                log.warn("服务 {} 未配置 API 路径，跳过", serviceName);
                continue;
            }
            
            // 确定目标 URI
            String targetUri = determineTargetUri(serviceName, serviceConfig, httpConfig.isUseDiscovery());
            
            // 为每组 API 路径创建路由
            RouteDefinition route = new RouteDefinition();
            route.setId("nebula-http-" + serviceName);
            route.setUri(URI.create(targetUri));
            route.setOrder(0);
            
            // 配置 Path 谓词
            PredicateDefinition pathPredicate = new PredicateDefinition();
            pathPredicate.setName("Path");
            int idx = 0;
            for (String apiPath : apiPaths) {
                pathPredicate.addArg("_genkey_" + idx++, apiPath.trim());
            }
            route.setPredicates(List.of(pathPredicate));
            
            // 添加到路由列表
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
     * 1. 静态地址配置
     * 2. 服务发现（从 Nacos 获取实例地址）
     * 3. lb:// 前缀（需要 Spring Cloud LoadBalancer 支持）
     */
    private String determineTargetUri(String serviceName, 
                                       GatewayProperties.HttpServiceConfig serviceConfig,
                                       boolean useDiscovery) {
        // 优先使用静态地址
        if (serviceConfig.getAddress() != null && !serviceConfig.getAddress().isEmpty()) {
            return serviceConfig.getAddress();
        }
        
        // 使用服务发现
        if (useDiscovery) {
            String targetServiceName = serviceConfig.getServiceName() != null 
                    ? serviceConfig.getServiceName() 
                    : serviceName;
            
            // 尝试从 Nebula ServiceDiscovery 获取服务实例
            if (serviceDiscovery != null) {
                try {
                    List<ServiceInstance> instances = serviceDiscovery.getInstances(targetServiceName);
                    if (instances != null && !instances.isEmpty()) {
                        // 获取第一个健康实例（实际生产中应该使用负载均衡）
                        ServiceInstance instance = instances.stream()
                                .filter(ServiceInstance::isHealthy)
                                .findFirst()
                                .orElse(instances.get(0));
                        String uri = String.format("http://%s:%d", instance.getIp(), instance.getPort());
                        log.info("从服务发现获取 {} 地址: {}", targetServiceName, uri);
                        return uri;
                    } else {
                        log.warn("服务 {} 在服务发现中无可用实例", targetServiceName);
                    }
                } catch (Exception e) {
                    log.warn("从服务发现获取 {} 地址失败: {}", targetServiceName, e.getMessage());
                }
            }
            
            // 如果 Nebula ServiceDiscovery 不可用，回退到 lb:// 前缀
            // 这需要 Spring Cloud LoadBalancer 支持
            return "lb://" + targetServiceName;
        }
        
        log.warn("服务 {} 无法确定目标 URI", serviceName);
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
