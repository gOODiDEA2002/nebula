package io.nebula.rpc.grpc.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.grpc.client.GrpcRpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import io.nebula.rpc.grpc.server.GrpcRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * gRPC RPC 自动配置类
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({GrpcRpcClient.class, GrpcRpcServer.class})
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
     */
    @Bean(name = "grpcRpcClient")
    @ConditionalOnMissingBean(name = "grpcRpcClient")
    @ConditionalOnProperty(prefix = "nebula.rpc.grpc.client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcRpcClient grpcRpcClient(ObjectMapper objectMapper, GrpcRpcProperties properties) {
        log.info("配置 gRPC RPC 客户端: target={}", properties.getClient().getTarget());
        return new GrpcRpcClient(objectMapper, properties.getClient());
    }
}

