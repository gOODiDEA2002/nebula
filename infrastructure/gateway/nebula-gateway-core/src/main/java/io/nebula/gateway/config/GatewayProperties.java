package io.nebula.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关配置属性
 * <p>
 * 基于微服务三原则优化后的简化配置：
 * - 前端接口通过 Controller 暴露（HTTP 代理）
 * - 服务间接口通过 RpcClient 暴露（纯 RPC，不经过 Gateway）
 * - Gateway 职责：HTTP 反向代理、认证、限流、日志
 */
@Data
@ConfigurationProperties(prefix = "nebula.gateway")
public class GatewayProperties {
    
    /**
     * 是否启用网关功能
     */
    private boolean enabled = true;
    
    /**
     * 日志配置
     */
    private LoggingConfig logging = new LoggingConfig();
    
    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();
    
    /**
     * 路由配置
     */
    private RoutesConfig routes = new RoutesConfig();
    
    /**
     * CORS配置
     */
    private CorsConfig cors = new CorsConfig();
    
    /**
     * HTTP 代理配置
     */
    private HttpProxyConfig http = new HttpProxyConfig();
    
    /**
     * 认证配置
     */
    private AuthConfig auth = new AuthConfig();
    
    /**
     * 认证配置
     */
    @Data
    public static class AuthConfig {
        /**
         * JWT 认证配置
         */
        private JwtConfig jwt = new JwtConfig();
    }
    
    /**
     * JWT 认证配置
     */
    @Data
    public static class JwtConfig {
        /**
         * 是否启用 JWT 认证
         */
        private boolean enabled = false;
        
        /**
         * JWT 密钥（至少 32 字符）
         */
        private String secret;
        
        /**
         * JWT 请求头名称
         */
        private String header = "Authorization";
        
        /**
         * Token 前缀
         */
        private String prefix = "Bearer ";
        
        /**
         * 白名单路径（不需要认证）
         */
        private List<String> whitelist = new ArrayList<>();
    }
    
    /**
     * 日志配置
     */
    @Data
    public static class LoggingConfig {
        /**
         * 是否启用请求日志
         */
        private boolean enabled = true;
        
        /**
         * 请求ID请求头名称
         */
        private String requestIdHeader = "X-Request-Id";
        
        /**
         * 是否记录请求体
         */
        private boolean logRequestBody = false;
        
        /**
         * 是否记录响应体
         */
        private boolean logResponseBody = false;
        
        /**
         * 慢请求阈值(毫秒)
         */
        private long slowRequestThreshold = 3000;
    }
    
    /**
     * 限流配置
     * <p>
     * 框架层提供 ip 和 path 两种通用策略。
     * 如需基于用户或其他业务维度限流，请在应用层自定义 KeyResolver。
     */
    @Data
    public static class RateLimitConfig {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;
        
        /**
         * 限流策略: ip, path
         * - ip: 基于客户端IP限流（默认）
         * - path: 基于请求路径限流
         */
        private String strategy = "ip";
        
        /**
         * 每秒补充令牌数
         */
        private int replenishRate = 100;
        
        /**
         * 桶容量
         */
        private int burstCapacity = 200;
        
        /**
         * 每次请求消耗令牌数
         */
        private int requestedTokens = 1;
        
        /**
         * Redis 配置（用于分布式限流）
         */
        private RedisConfig redis = new RedisConfig();
    }
    
    /**
     * Redis 配置（用于限流）
     */
    @Data
    public static class RedisConfig {
        /**
         * 是否启用 Redis 限流
         */
        private boolean enabled = true;
        
        /**
         * Redis 主机
         */
        private String host = "localhost";
        
        /**
         * Redis 端口
         */
        private int port = 6379;
        
        /**
         * Redis 密码
         */
        private String password;
        
        /**
         * 数据库索引
         */
        private int database = 0;
        
        /**
         * 连接超时(毫秒)
         */
        private long timeout = 2000;
    }
    
    /**
     * 路由配置
     */
    @Data
    public static class RoutesConfig {
        /**
         * API路径前缀（用于匹配）
         * 示例: /api/v1
         */
        private String apiPathPrefix = "/api/v1";
        
        /**
         * 默认过滤器
         */
        private List<String> defaultFilters = new ArrayList<>();
        
        /**
         * 自定义路由定义
         */
        private List<RouteDefinition> definitions = new ArrayList<>();
    }
    
    /**
     * 路由定义
     */
    @Data
    public static class RouteDefinition {
        /**
         * 路由ID
         */
        private String id;
        
        /**
         * 目标URI
         */
        private String uri;
        
        /**
         * 路径匹配模式
         */
        private List<String> paths = new ArrayList<>();
        
        /**
         * 过滤器列表
         */
        private List<String> filters = new ArrayList<>();
        
        /**
         * 排序顺序
         */
        private int order = 0;
    }
    
    /**
     * CORS配置
     */
    @Data
    public static class CorsConfig {
        /**
         * 是否启用CORS
         */
        private boolean enabled = true;
        
        /**
         * 允许的来源
         */
        private List<String> allowedOrigins = List.of("*");
        
        /**
         * 允许的方法
         */
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        
        /**
         * 允许的请求头
         */
        private List<String> allowedHeaders = List.of("*");
        
        /**
         * 暴露的响应头
         */
        private List<String> exposedHeaders = List.of("Authorization");
        
        /**
         * 是否允许凭证
         */
        private boolean allowCredentials = false;
        
        /**
         * 预检请求缓存时间(秒)
         */
        private long maxAge = 3600;
    }
    
    /**
     * HTTP 代理配置
     * <p>
     * 用于配置 HTTP 反向代理到后端服务
     */
    @Data
    public static class HttpProxyConfig {
        /**
         * 是否启用 HTTP 代理
         */
        private boolean enabled = true;
        
        /**
         * 是否使用服务发现
         */
        private boolean useDiscovery = true;
        
        /**
         * 服务配置映射
         * key: 服务名称
         * value: 服务配置
         */
        private Map<String, HttpServiceConfig> services = new HashMap<>();
        
        /**
         * 默认超时配置
         */
        private TimeoutConfig timeout = new TimeoutConfig();
    }
    
    /**
     * HTTP 服务配置
     */
    @Data
    public static class HttpServiceConfig {
        /**
         * 是否启用该服务
         */
        private boolean enabled = true;
        
        /**
         * 服务发现中的服务名
         * 如果不指定，默认使用配置的 key
         */
        private String serviceName;
        
        /**
         * 静态服务地址（不使用服务发现时）
         * 示例: http://localhost:8080
         */
        private String address;
        
        /**
         * API 路径列表
         * 示例: ["/api/v1/users/**", "/api/v1/profiles/**"]
         */
        private List<String> apiPaths = new ArrayList<>();
        
        /**
         * 自定义超时配置
         */
        private TimeoutConfig timeout;
    }
    
    /**
     * 超时配置
     */
    @Data
    public static class TimeoutConfig {
        /**
         * 连接超时时间(毫秒)
         */
        private long connectTimeout = 30000;
        
        /**
         * 读取超时时间(毫秒)
         */
        private long readTimeout = 60000;
        
        /**
         * 写入超时时间(毫秒)
         */
        private long writeTimeout = 60000;
    }
}
