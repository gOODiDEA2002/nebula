package io.nebula.rpc.grpc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * gRPC RPC 配置属性
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.rpc.grpc")
public class GrpcRpcProperties {

    /**
     * 是否启用 gRPC RPC
     */
    private boolean enabled = false;

    /**
     * 服务器配置
     */
    private ServerConfig server = new ServerConfig();

    /**
     * 客户端配置
     */
    private ClientConfig client = new ClientConfig();

    /**
     * 服务器配置
     */
    @Data
    public static class ServerConfig {
        /**
         * 是否启用服务器
         */
        private boolean enabled = true;

        /**
         * 服务器端口
         */
        private int port = 9090;

        /**
         * 最大入站消息大小(字节)
         */
        private int maxInboundMessageSize = 10 * 1024 * 1024; // 10MB

        /**
         * Keep-Alive 时间(秒)
         */
        private long keepAliveTime = 30;

        /**
         * Keep-Alive 超时时间(秒)
         */
        private long keepAliveTimeout = 10;

        /**
         * 是否允许无调用时的 Keep-Alive
         */
        private boolean permitKeepAliveWithoutCalls = true;

        /**
         * 最大并发调用数
         */
        private int maxConcurrentCalls = 1000;
    }

    /**
     * 客户端配置
     */
    @Data
    public static class ClientConfig {
        /**
         * 是否启用客户端
         */
        private boolean enabled = true;

        /**
         * 默认目标地址
         */
        private String target = "localhost:9090";

        /**
         * 协商类型(plaintext, tls)
         */
        private String negotiationType = "plaintext";

        /**
         * 负载均衡策略
         */
        private String loadBalancingPolicy = "round_robin";

        /**
         * 最大入站消息大小(字节)
         */
        private int maxInboundMessageSize = 10 * 1024 * 1024; // 10MB

        /**
         * 连接超时时间(毫秒)
         */
        private long connectTimeout = 30000;

        /**
         * 请求超时时间(毫秒)
         */
        private long requestTimeout = 60000;

        /**
         * 重试次数
         */
        private int retryCount = 3;

        /**
         * 重试间隔(毫秒)
         */
        private long retryInterval = 1000;

        /**
         * 是否启用压缩
         */
        private boolean compressionEnabled = false;

        /**
         * 是否启用日志
         */
        private boolean loggingEnabled = true;
    }
}

