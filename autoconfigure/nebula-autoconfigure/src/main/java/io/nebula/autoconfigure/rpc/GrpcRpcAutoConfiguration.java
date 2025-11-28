package io.nebula.autoconfigure.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.grpc.client.GrpcRpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
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

/**
 * gRPC RPC 自动配置类
 * 必须在 RpcDiscoveryAutoConfiguration 之前初始化，提供 grpcRpcClient Bean
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(RpcDiscoveryAutoConfiguration.class)  // 关键：确保 grpcRpcClient 先创建
@ConditionalOnClass(name = {"io.nebula.rpc.grpc.client.GrpcRpcClient", "io.nebula.rpc.grpc.config.GrpcRpcProperties"})
@EnableConfigurationProperties(GrpcRpcProperties.class)
@ConditionalOnProperty(prefix = "nebula.rpc.grpc", name = "enabled", havingValue = "true", matchIfMissing = false)
public class GrpcRpcAutoConfiguration {

    /**
     * 配置 gRPC RPC 服务器
     */
    @Bean
    @ConditionalOnMissingBean(GrpcRpcServer.class)
    @ConditionalOnProperty(prefix = "nebula.rpc.grpc.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcRpcServer grpcRpcServer(ObjectMapper objectMapper, GrpcRpcProperties properties) {
        log.info("配置 gRPC RPC 服务器: port={}", properties.getServer().getPort());
        return new GrpcRpcServer(objectMapper);
    }

    /**
     * 配置 gRPC RPC 客户端
     * 标记为 @Primary，优先使用 gRPC（如果启用）
     */
    @Bean(name = "grpcRpcClient")
    @Primary  // 如果 gRPC 启用，优先使用它
    @ConditionalOnMissingBean(name = "grpcRpcClient")
    @ConditionalOnProperty(prefix = "nebula.rpc.grpc.client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcRpcClient grpcRpcClient(ObjectMapper objectMapper, GrpcRpcProperties properties) {
        log.info("配置 gRPC RPC 客户端: target={}", properties.getClient().getTarget());
        return new GrpcRpcClient(objectMapper, properties.getClient());
    }
}

