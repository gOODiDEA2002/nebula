package io.nebula.autoconfigure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.gateway.config.GatewayProperties;
import io.nebula.gateway.config.GatewayRedisAutoConfiguration;
import io.nebula.gateway.config.GatewayRoutesAutoConfiguration;
import io.nebula.gateway.config.RateLimitKeyResolverConfig;
import io.nebula.gateway.filter.LoggingGlobalFilter;
import io.nebula.gateway.grpc.AbstractGrpcServiceRouter;
import io.nebula.gateway.grpc.AutoDiscoveryGrpcServiceRouter;
import io.nebula.gateway.grpc.GrpcClientAutoRegistrar;
import io.nebula.gateway.grpc.GrpcGatewayFilterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import jakarta.annotation.PreDestroy;

/**
 * 网关自动配置
 * <p>
 * 当检测到Spring Cloud Gateway在类路径中时自动配置网关组件
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GatewayFilter")
@ConditionalOnProperty(prefix = "nebula.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GatewayProperties.class)
@Import({RateLimitKeyResolverConfig.class, GatewayRoutesAutoConfiguration.class, GatewayRedisAutoConfiguration.class})
public class GatewayAutoConfiguration {
    
    private GrpcClientAutoRegistrar grpcClientAutoRegistrar;
    
    // 注意：JWT 认证过滤器已移至应用层实现（如 ticket-gateway）
    // 框架不再内置 JWT 认证，由各应用根据业务需求自行实现
    
    /**
     * 全局日志过滤器
     */
    @Bean
    @ConditionalOnMissingBean(name = "loggingGlobalFilter")
    @ConditionalOnProperty(prefix = "nebula.gateway.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GlobalFilter loggingGlobalFilter(GatewayProperties gatewayProperties) {
        log.info("初始化Nebula Gateway日志过滤器");
        return new LoggingGlobalFilter(gatewayProperties);
    }
    
    /**
     * gRPC客户端自动注册器
     * <p>
     * 根据配置自动扫描@RpcClient接口并创建gRPC客户端代理
     * 支持从 Nacos 等注册中心动态获取服务地址
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nebula.gateway.grpc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcClientAutoRegistrar grpcClientAutoRegistrar(GatewayProperties gatewayProperties,
                                                            ObjectMapper objectMapper,
                                                            DefaultListableBeanFactory beanFactory,
                                                            ObjectProvider<ServiceDiscovery> serviceDiscoveryProvider) {
        ServiceDiscovery serviceDiscovery = serviceDiscoveryProvider.getIfAvailable();
        if (serviceDiscovery != null) {
            log.info("初始化Nebula Gateway gRPC客户端自动注册器 (启用服务发现)");
        } else {
            log.info("初始化Nebula Gateway gRPC客户端自动注册器 (使用静态地址)");
        }
        this.grpcClientAutoRegistrar = new GrpcClientAutoRegistrar(gatewayProperties, objectMapper, beanFactory, serviceDiscovery);
        this.grpcClientAutoRegistrar.scanAndRegister();
        return this.grpcClientAutoRegistrar;
    }
    
    /**
     * 自动发现gRPC服务路由器
     * <p>
     * 当没有自定义路由器时，根据@RpcCall注解自动注册路由
     */
    @Bean
    @ConditionalOnMissingBean(AbstractGrpcServiceRouter.class)
    @ConditionalOnProperty(prefix = "nebula.gateway.grpc", name = "auto-scan", havingValue = "true", matchIfMissing = true)
    public AutoDiscoveryGrpcServiceRouter autoDiscoveryGrpcServiceRouter(ObjectMapper objectMapper,
                                                                          GatewayProperties gatewayProperties,
                                                                          GrpcClientAutoRegistrar clientRegistrar) {
        log.info("初始化Nebula Gateway 自动发现路由器");
        AutoDiscoveryGrpcServiceRouter router = new AutoDiscoveryGrpcServiceRouter(objectMapper, gatewayProperties, clientRegistrar);
        router.init();
        return router;
    }
    
    /**
     * gRPC网关过滤器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public GrpcGatewayFilterFactory grpcGatewayFilterFactory(ObjectMapper objectMapper,
                                                              AbstractGrpcServiceRouter serviceRouter) {
        log.info("初始化Nebula Gateway gRPC过滤器");
        return new GrpcGatewayFilterFactory(objectMapper, serviceRouter);
    }
    
    @PreDestroy
    public void shutdown() {
        if (grpcClientAutoRegistrar != null) {
            grpcClientAutoRegistrar.shutdown();
        }
    }
}

