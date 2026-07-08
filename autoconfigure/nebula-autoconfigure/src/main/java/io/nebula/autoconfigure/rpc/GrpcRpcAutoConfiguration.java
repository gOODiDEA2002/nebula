package io.nebula.autoconfigure.rpc;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
import io.nebula.rpc.grpc.client.GrpcRpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import io.nebula.rpc.grpc.server.GrpcAuthTokenInterceptor;
import io.nebula.rpc.grpc.server.GrpcRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * gRPC RPC 自动配置类
 * 必须在 RpcDiscoveryAutoConfiguration 之前初始化，提供 grpcRpcClient Bean
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(RpcDiscoveryAutoConfiguration.class) // 关键：确保 grpcRpcClient 先创建
@ConditionalOnClass(name = { "io.nebula.rpc.grpc.client.GrpcRpcClient", "io.nebula.rpc.grpc.config.GrpcRpcProperties" })
@EnableConfigurationProperties(GrpcRpcProperties.class)
@ConditionalOnProperty(prefix = "nebula.rpc.grpc", name = "enabled", havingValue = "true", matchIfMissing = false)
public class GrpcRpcAutoConfiguration {

    /**
     * 配置 gRPC RPC 服务器
     */
    @Bean
    @ConditionalOnMissingBean(GrpcRpcServer.class)
    @ConditionalOnProperty(prefix = "nebula.rpc.grpc.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcRpcServer grpcRpcServer(GrpcRpcProperties properties) {
        log.info("配置 gRPC RPC 服务器: port={}", properties.getServer().getPort());
        return new GrpcRpcServer(rpcObjectMapper());
    }

    /**
     * 配置 gRPC RPC 客户端
     * 标记为 @Primary，优先使用 gRPC（如果启用）
     */
    @Bean(name = "grpcRpcClient")
    @Primary // 如果 gRPC 启用，优先使用它
    @ConditionalOnMissingBean(name = "grpcRpcClient")
    @ConditionalOnProperty(prefix = "nebula.rpc.grpc.client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcRpcClient grpcRpcClient(GrpcRpcProperties properties) {
        log.info("配置 gRPC RPC 客户端: target={}", properties.getClient().getTarget());
        return new GrpcRpcClient(rpcObjectMapper(), properties.getClient());
    }

    private static ObjectMapper rpcObjectMapper() {
        return JsonMapper.builder().build();
    }

    /**
     * gRPC token 鉴权拦截器（可选，配置 auth-token 后生效）
     */
    @Bean
    @Order(0)
    @GlobalServerInterceptor
    @ConditionalOnProperty(prefix = "nebula.rpc.grpc.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcAuthTokenInterceptor grpcAuthTokenInterceptor(GrpcRpcProperties properties) {
        String token = properties.getServer().getAuthToken();
        if (token != null && !token.isEmpty()) {
            log.info("gRPC RPC token 鉴权已启用");
        }
        return new GrpcAuthTokenInterceptor(token);
    }

    /**
     * 组件摘要: gRPC RPC
     */
    @Bean
    NebulaComponentSummary grpcRpcSummary(GrpcRpcProperties properties) {
        var details = new java.util.LinkedHashMap<String, String>();

        // Server（遗留线程/流控参数已删除，改用 spring.grpc.server.*）
        details.put("Server Port", String.valueOf(properties.getServer().getPort()));
        details.put("Auth Token", properties.getServer().getAuthToken().isEmpty() ? "disabled" : "enabled");

        // Client
        details.put("Target", properties.getClient().getTarget());
        details.put("Negotiation", properties.getClient().getNegotiationType());
        details.put("Load Balancing", properties.getClient().getLoadBalancingPolicy());
        details.put("Connect Timeout", properties.getClient().getConnectTimeout() + "ms");
        details.put("Request Timeout", properties.getClient().getRequestTimeout() + "ms");

        return new SimpleComponentSummary("RPC", "gRPC RPC", true, 210, details);
    }
}
