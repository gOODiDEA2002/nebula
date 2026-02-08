package io.nebula.rpc.http.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * HTTP RPC 配置属性
 * 
 * 包含完整的配置校验，确保参数有效性
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "nebula.rpc.http")
public class HttpRpcProperties {
    
    /**
     * 是否启用HTTP RPC
     */
    private boolean enabled = true;
    
    /**
     * 服务器配置
     */
    @Valid
    private ServerConfig server = new ServerConfig();
    
    /**
     * 客户端配置
     */
    @Valid
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
         * 范围: 1 - 65535
         */
        @Min(value = 1, message = "端口号不能小于 1")
        @Max(value = 65535, message = "端口号不能大于 65535")
        private int port = 8080;
        
        /**
         * 上下文路径
         */
        @NotBlank(message = "RPC 上下文路径不能为空")
        private String contextPath = "/rpc";
        
        /**
         * 最大请求大小（字节）
         * 范围: 1KB - 100MB
         */
        @Min(value = 1024, message = "最大请求大小不能小于 1KB")
        @Max(value = 104857600, message = "最大请求大小不能大于 100MB")
        private long maxRequestSize = 10485760; // 10MB
        
        /**
         * 最大响应大小（字节）
         * 范围: 1KB - 100MB
         */
        @Min(value = 1024, message = "最大响应大小不能小于 1KB")
        @Max(value = 104857600, message = "最大响应大小不能大于 100MB")
        private long maxResponseSize = 10485760; // 10MB
        
        /**
         * 请求处理超时时间（毫秒）
         * 范围: 1000ms - 600000ms (10分钟)
         */
        @Min(value = 1000, message = "请求超时时间不能小于 1000 毫秒")
        @Max(value = 600000, message = "请求超时时间不能大于 600000 毫秒")
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
         * 范围: 1000ms - 120000ms
         */
        @Min(value = 1000, message = "连接超时时间不能小于 1000 毫秒")
        @Max(value = 120000, message = "连接超时时间不能大于 120000 毫秒")
        private int connectTimeout = 30000; // 30s
        
        /**
         * 读取超时时间（毫秒）
         * 范围: 1000ms - 600000ms
         */
        @Min(value = 1000, message = "读取超时时间不能小于 1000 毫秒")
        @Max(value = 600000, message = "读取超时时间不能大于 600000 毫秒")
        private int readTimeout = 60000; // 60s
        
        /**
         * 写入超时时间（毫秒）
         * 范围: 1000ms - 600000ms
         */
        @Min(value = 1000, message = "写入超时时间不能小于 1000 毫秒")
        @Max(value = 600000, message = "写入超时时间不能大于 600000 毫秒")
        private int writeTimeout = 60000; // 60s
        
        /**
         * 最大连接数
         * 范围: 1 - 10000
         */
        @Min(value = 1, message = "最大连接数不能小于 1")
        @Max(value = 10000, message = "最大连接数不能大于 10000")
        private int maxConnections = 200;
        
        /**
         * 每个路由的最大连接数
         * 范围: 1 - 1000
         */
        @Min(value = 1, message = "每个路由的最大连接数不能小于 1")
        @Max(value = 1000, message = "每个路由的最大连接数不能大于 1000")
        private int maxConnectionsPerRoute = 100;
        
        /**
         * 连接保持时间（毫秒）
         * 范围: 10000ms - 3600000ms
         */
        @Min(value = 10000, message = "连接保持时间不能小于 10000 毫秒")
        @Max(value = 3600000, message = "连接保持时间不能大于 3600000 毫秒")
        private long keepAliveTime = 60000; // 60s
        
        /**
         * 重试次数
         * 范围: 0 - 10
         */
        @Min(value = 0, message = "重试次数不能小于 0")
        @Max(value = 10, message = "重试次数不能大于 10")
        private int retryCount = 3;
        
        /**
         * 重试间隔（毫秒）
         * 范围: 100ms - 60000ms
         */
        @Min(value = 100, message = "重试间隔不能小于 100 毫秒")
        @Max(value = 60000, message = "重试间隔不能大于 60000 毫秒")
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

