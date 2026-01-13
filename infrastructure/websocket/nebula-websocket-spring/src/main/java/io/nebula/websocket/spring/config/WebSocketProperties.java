package io.nebula.websocket.spring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 配置属性
 */
@Data
@ConfigurationProperties(prefix = "nebula.websocket")
public class WebSocketProperties {

    /**
     * 是否启用 WebSocket
     */
    private boolean enabled = true;

    /**
     * WebSocket 端点路径
     */
    private String endpoint = "/ws";

    /**
     * 允许的来源（CORS）
     */
    private String[] allowedOrigins = {"*"};

    /**
     * 是否允许使用 SockJS
     */
    private boolean sockJsEnabled = false;

    /**
     * 心跳配置
     */
    private HeartbeatConfig heartbeat = new HeartbeatConfig();

    /**
     * 集群配置
     */
    private ClusterConfig cluster = new ClusterConfig();

    /**
     * 缓冲区配置
     */
    private BufferConfig buffer = new BufferConfig();

    /**
     * 心跳配置
     */
    @Data
    public static class HeartbeatConfig {
        /**
         * 是否启用心跳检测
         */
        private boolean enabled = true;

        /**
         * 心跳间隔（秒）
         */
        private int intervalSeconds = 30;

        /**
         * 超时时间（秒）
         */
        private int timeoutSeconds = 60;
    }

    /**
     * 集群配置
     */
    @Data
    public static class ClusterConfig {
        /**
         * 是否启用集群模式
         */
        private boolean enabled = false;

        /**
         * 集群消息频道前缀
         */
        private String channelPrefix = "websocket:cluster:";
    }

    /**
     * 缓冲区配置
     */
    @Data
    public static class BufferConfig {
        /**
         * 发送缓冲区大小（字节）
         */
        private int sendBufferSizeLimit = 512 * 1024;

        /**
         * 发送超时时间（毫秒）
         */
        private int sendTimeLimit = 10000;

        /**
         * 消息大小限制（字节）
         */
        private int messageSizeLimit = 64 * 1024;
    }
}

