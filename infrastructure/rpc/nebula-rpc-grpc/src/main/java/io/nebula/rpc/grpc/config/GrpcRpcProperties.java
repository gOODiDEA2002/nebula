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
         * 服务器端口。
         * @deprecated 改用 {@code spring.grpc.server.port}, 桥接 EPP 仍会将本值转发到新键
         */
        @Deprecated
        private int port = 9090;

        /**
         * gRPC 端点访问 token（可选，默认空=不鉴权）。
         * 配置后，gRPC 请求须携带 metadata {@code x-nebula-rpc-token: <token>}，否则返回 UNAUTHENTICATED；
         * 默认关闭以不影响纯内网调用。
         */
        private String authToken = "";
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
         * 调用下游 gRPC 端点时携带的鉴权 token。
         * 下游配置 {@code nebula.rpc.grpc.server.auth-token} 后，本端必须配置相同值。
         */
        private String authToken = "";

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
         * 默认为0（不重试），因为大多数业务系统未实现幂等
         * 仅对幂等接口（如查询接口）启用重试
         */
        private int retryCount = 0;

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
