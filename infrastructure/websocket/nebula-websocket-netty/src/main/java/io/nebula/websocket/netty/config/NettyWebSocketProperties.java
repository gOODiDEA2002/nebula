package io.nebula.websocket.netty.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Netty WebSocket 配置属性
 */
@Data
@ConfigurationProperties(prefix = "nebula.websocket.netty")
public class NettyWebSocketProperties {

    /**
     * 是否启用 Netty WebSocket
     */
    private boolean enabled = true;

    /**
     * 服务端口
     */
    private int port = 9000;

    /**
     * WebSocket 路径
     */
    private String path = "/ws";

    /**
     * Boss 线程数（接收连接）
     */
    private int bossThreads = 1;

    /**
     * Worker 线程数（处理请求）
     */
    private int workerThreads = 0;  // 0 表示使用 CPU 核心数 * 2

    /**
     * 最大 HTTP 内容长度
     */
    private int maxContentLength = 65536;

    /**
     * 积压连接数
     */
    private int backlog = 1024;

    /**
     * 读空闲时间（秒），0 表示禁用
     */
    private int readerIdleTime = 60;

    /**
     * 写空闲时间（秒），0 表示禁用
     */
    private int writerIdleTime = 0;

    /**
     * 读写空闲时间（秒），0 表示禁用
     */
    private int allIdleTime = 0;

    /**
     * 集群配置
     */
    private ClusterConfig cluster = new ClusterConfig();

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
}

