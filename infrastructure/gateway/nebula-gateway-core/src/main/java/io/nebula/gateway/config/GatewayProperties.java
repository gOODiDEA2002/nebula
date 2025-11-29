package io.nebula.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关配置属性
 */
@Data
@ConfigurationProperties(prefix = "nebula.gateway")
public class GatewayProperties {
    
    /**
     * 是否启用网关功能
     */
    private boolean enabled = true;
    
    // 注意：JWT 认证已移至应用层实现，框架不再内置 JWT 配置
    // 各应用（如 ticket-gateway）应自行定义认证配置和实现
    
    /**
     * 日志配置
     */
    private LoggingConfig logging = new LoggingConfig();
    
    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();
    
    /**
     * gRPC服务配置
     */
    private GrpcConfig grpc = new GrpcConfig();
    
    /**
     * 路由配置
     */
    private RoutesConfig routes = new RoutesConfig();
    
    /**
     * CORS配置
     */
    private CorsConfig cors = new CorsConfig();
    
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
     * gRPC服务配置
     */
    @Data
    public static class GrpcConfig {
        /**
         * 是否启用gRPC网关功能
         */
        private boolean enabled = true;
        
        /**
         * 是否自动扫描@RpcClient接口并注册路由
         */
        private boolean autoScan = true;
        
        /**
         * 路径映射配置
         */
        private PathMapping pathMapping = new PathMapping();
        
        /**
         * Header 传递配置
         */
        private HeaderPropagation headerPropagation = new HeaderPropagation();
        
        /**
         * gRPC服务配置映射
         * key: 服务名称
         * value: 服务配置
         */
        private Map<String, ServiceConfig> services = new HashMap<>();
        
        /**
         * gRPC客户端默认配置
         */
        private ClientDefaults clientDefaults = new ClientDefaults();
    }
    
    /**
     * 路径映射配置
     */
    @Data
    public static class PathMapping {
        /**
         * 是否启用路径映射
         */
        private boolean enabled = true;
        
        /**
         * Gateway 对外暴露的 API 前缀
         */
        private String apiPrefix = "/api/v1";
        
        /**
         * RPC 服务内部路径前缀
         */
        private String rpcPrefix = "/rpc";
    }
    
    /**
     * Header 传递配置
     * <p>
     * 用于配置哪些 HTTP Headers 需要通过 RpcContext 传递到后端服务。
     * 框架只负责传递，不关心业务含义（如 userId）。
     */
    @Data
    public static class HeaderPropagation {
        /**
         * 是否启用 Header 传递
         */
        private boolean enabled = true;
        
        /**
         * 需要传递的 Header 列表（支持通配符）
         * 默认传递所有 X- 开头的 Header 和 Authorization
         * 示例: ["X-*", "Authorization"]
         */
        private List<String> includes = List.of("X-*", "Authorization");
        
        /**
         * 需要排除的 Header 列表
         */
        private List<String> excludes = new ArrayList<>();
    }
    
    /**
     * gRPC服务配置
     */
    @Data
    public static class ServiceConfig {
        /**
         * 是否启用该服务
         */
        private boolean enabled = true;
        
        /**
         * 是否使用服务发现获取地址
         * 如果为 true，将从 Nacos 等注册中心获取服务地址
         * 如果为 false，使用 address 配置的静态地址
         */
        private boolean useDiscovery = true;
        
        /**
         * 服务发现中的服务名
         * 如果不指定，默认使用配置的 key（如 ticket-user）
         */
        private String serviceName;
        
        /**
         * gRPC服务地址 (如: localhost:5001)
         * 仅当 useDiscovery=false 时使用
         */
        private String address;
        
        /**
         * gRPC端口号
         * 服务发现时，如果服务注册的是 HTTP 端口，可通过此配置指定 gRPC 端口
         * 例如：服务注册 8080，gRPC 端口为 5001，设置 grpcPort=5001
         */
        private Integer grpcPort;
        
        /**
         * API接口包路径列表 (用于扫描@RpcClient接口)
         */
        private List<String> apiPackages = new ArrayList<>();
        
        /**
         * 自定义 API 路径列表
         * 如果不配置，将根据服务名自动推断
         * 示例: ["/api/v1/users/**", "/api/v1/profiles/**"]
         */
        private List<String> apiPaths = new ArrayList<>();
        
        /**
         * 连接超时时间(毫秒)
         */
        private Long connectTimeout;
        
        /**
         * 请求超时时间(毫秒)
         */
        private Long requestTimeout;
        
        /**
         * 重试次数
         */
        private Integer retryCount;
    }
    
    /**
     * gRPC客户端默认配置
     */
    @Data
    public static class ClientDefaults {
        /**
         * 默认连接超时时间(毫秒)
         */
        private long connectTimeout = 30000;
        
        /**
         * 默认请求超时时间(毫秒)
         */
        private long requestTimeout = 60000;
        
        /**
         * 默认重试次数
         */
        private int retryCount = 3;
        
        /**
         * 默认重试间隔(毫秒)
         */
        private long retryInterval = 1000;
        
        /**
         * 协商类型 (plaintext/tls)
         */
        private String negotiationType = "plaintext";
        
        /**
         * 最大入站消息大小(字节)
         */
        private int maxInboundMessageSize = 10485760;
    }
    
    /**
     * 路由配置
     */
    @Data
    public static class RoutesConfig {
        /**
         * 是否自动配置gRPC路由
         * 当启用时，会根据 grpc.services 配置自动生成路由
         */
        private boolean autoConfigGrpcRoutes = true;
        
        /**
         * gRPC路由的API路径前缀（用于匹配）
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
        private String uri = "no://op";
        
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
}

