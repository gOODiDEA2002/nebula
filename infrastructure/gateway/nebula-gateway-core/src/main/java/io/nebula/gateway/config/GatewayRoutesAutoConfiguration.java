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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网关路由和CORS自动配置
 * <p>
 * 将 nebula.gateway 的配置转换为 Spring Cloud Gateway 配置
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
        GatewayProperties.RoutesConfig routesConfig = nebulaGatewayProperties.getRoutes();
        
        if (routesConfig.isAutoConfigGrpcRoutes()) {
            configureGrpcRoutes();
        }
        
        // 配置自定义路由
        configureCustomRoutes();
        
        // 配置默认过滤器
        configureDefaultFilters();
        
        log.info("Nebula Gateway 路由配置完成，共 {} 条路由", springGatewayProperties.getRoutes().size());
    }

    /**
     * 根据 gRPC 服务配置自动生成路由
     */
    private void configureGrpcRoutes() {
        GatewayProperties.GrpcConfig grpcConfig = nebulaGatewayProperties.getGrpc();
        if (!grpcConfig.isEnabled() || grpcConfig.getServices().isEmpty()) {
            return;
        }

        // 收集所有服务的 API 路径
        List<String> apiPaths = new ArrayList<>();
        for (Map.Entry<String, GatewayProperties.ServiceConfig> entry : grpcConfig.getServices().entrySet()) {
            GatewayProperties.ServiceConfig serviceConfig = entry.getValue();
            if (serviceConfig.isEnabled() && !serviceConfig.getApiPackages().isEmpty()) {
                // 根据包名推断 API 路径
                String serviceName = entry.getKey();
                String apiPath = inferApiPath(serviceName);
                if (apiPath != null) {
                    apiPaths.add(apiPath);
                }
            }
        }

        if (apiPaths.isEmpty()) {
            return;
        }

        // 创建 gRPC 路由
        RouteDefinition grpcRoute = new RouteDefinition();
        grpcRoute.setId("nebula-grpc-services");
        grpcRoute.setUri(URI.create("no://op"));
        grpcRoute.setOrder(0);

        // Path 谓词 - 使用多个 Path predicate 或者使用正确的参数格式
        // Path predicate 支持逗号分隔的多个模式
        List<PredicateDefinition> predicates = new ArrayList<>();
        for (String pathGroup : apiPaths) {
            // 每个 pathGroup 可能包含多个逗号分隔的路径
            for (String path : pathGroup.split(",")) {
                PredicateDefinition pathPredicate = new PredicateDefinition();
                pathPredicate.setName("Path");
                pathPredicate.addArg("pattern", path.trim());
                predicates.add(pathPredicate);
            }
        }
        // 使用 OR 组合所有路径（实际上多个 predicate 默认是 OR 关系）
        // 但 Spring Cloud Gateway 的路由匹配是 AND，所以我们需要用一个 Path predicate 包含所有路径
        PredicateDefinition combinedPathPredicate = new PredicateDefinition();
        combinedPathPredicate.setName("Path");
        // 使用 _genkey_ 前缀的参数名来添加多个路径
        int idx = 0;
        for (String pathGroup : apiPaths) {
            for (String path : pathGroup.split(",")) {
                combinedPathPredicate.addArg("_genkey_" + idx++, path.trim());
            }
        }
        grpcRoute.setPredicates(List.of(combinedPathPredicate));

        // 过滤器
        List<FilterDefinition> filters = new ArrayList<>();
        
        // JWT 认证过滤器
        if (nebulaGatewayProperties.getJwt().isEnabled()) {
            FilterDefinition jwtFilter = new FilterDefinition();
            jwtFilter.setName("JwtAuth");
            filters.add(jwtFilter);
        }
        
        // gRPC 桥接过滤器
        FilterDefinition grpcFilter = new FilterDefinition();
        grpcFilter.setName("Grpc");
        filters.add(grpcFilter);
        
        grpcRoute.setFilters(filters);

        // 添加到路由列表（如果不存在同ID的路由）
        boolean exists = springGatewayProperties.getRoutes().stream()
                .anyMatch(r -> "nebula-grpc-services".equals(r.getId()));
        if (!exists) {
            springGatewayProperties.getRoutes().add(grpcRoute);
            log.info("自动配置 gRPC 路由: paths={}", apiPaths);
        }
    }

    /**
     * 根据服务名推断 API 路径
     */
    private String inferApiPath(String serviceName) {
        String apiPrefix = nebulaGatewayProperties.getRoutes().getApiPathPrefix();
        
        // ticket-user -> /api/v1/users/**
        // ticket-cinema -> /api/v1/cinemas/**, /api/v1/movies/**, /api/v1/showtimes/**, /api/v1/seats/**
        // ticket-order -> /api/v1/orders/**
        // ticket-payment -> /api/v1/payments/**, /api/v1/refunds/**
        // ticket-notification -> /api/v1/notifications/**
        
        if (serviceName.contains("user")) {
            return apiPrefix + "/users/**";
        } else if (serviceName.contains("cinema")) {
            return apiPrefix + "/cinemas/**," + apiPrefix + "/movies/**," + apiPrefix + "/showtimes/**," + apiPrefix + "/seats/**";
        } else if (serviceName.contains("order")) {
            return apiPrefix + "/orders/**";
        } else if (serviceName.contains("payment")) {
            return apiPrefix + "/payments/**," + apiPrefix + "/refunds/**";
        } else if (serviceName.contains("notification")) {
            return apiPrefix + "/notifications/**";
        }
        
        // 默认使用服务名的最后一部分
        String[] parts = serviceName.split("-");
        String resourceName = parts[parts.length - 1] + "s";
        return apiPrefix + "/" + resourceName + "/**";
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
                pathPredicate.addArg("pattern", String.join(",", def.getPaths()));
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
