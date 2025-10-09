package io.nebula.rpc.http.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP RPC配置属性
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.rpc.http")
public class HttpRpcProperties {
    
    /**
     * 是否启用HTTP RPC
     */
    private boolean enabled = true;
    
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
        private int port = 8080;
        
        /**
         * 上下文路径
         */
        private String contextPath = "/rpc";
        
        /**
         * 最大请求大小（字节）
         */
        private long maxRequestSize = 10485760; // 10MB
        
        /**
         * 最大响应大小（字节）
         */
        private long maxResponseSize = 10485760; // 10MB
        
        /**
         * 请求处理超时时间（毫秒）
         */
        private long requestTimeout = 60000; // 60s
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
         * 默认服务地址
         */
        private String baseUrl;
        
        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 30000; // 30s
        
        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeout = 60000; // 60s
        
        /**
         * 写入超时时间（毫秒）
         */
        private int writeTimeout = 60000; // 60s
        
        /**
         * 最大连接数
         */
        private int maxConnections = 200;
        
        /**
         * 每个路由的最大连接数
         */
        private int maxConnectionsPerRoute = 100;
        
        /**
         * 连接保持时间（毫秒）
         */
        private long keepAliveTime = 60000; // 60s
        
        /**
         * 重试次数
         */
        private int retryCount = 3;
        
        /**
         * 重试间隔（毫秒）
         */
        private long retryInterval = 1000; // 1s
        
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

