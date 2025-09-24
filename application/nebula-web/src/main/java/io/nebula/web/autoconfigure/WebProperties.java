package io.nebula.web.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nebula Web 配置属性
 */
@ConfigurationProperties(prefix = "nebula.web")
public class WebProperties {
    
    /**
     * 异常处理器配置
     */
    private ExceptionHandler exceptionHandler = new ExceptionHandler();
    
    /**
     * API 文档配置
     */
    private ApiDoc apiDoc = new ApiDoc();
    
    /**
     * CORS 配置
     */
    private Cors cors = new Cors();
    
    /**
     * 请求日志配置
     */
    private RequestLogging requestLogging = new RequestLogging();
    
    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();
    
    /**
     * 缓存配置
     */
    private Cache cache = new Cache();
    
    /**
     * 认证配置
     */
    private Auth auth = new Auth();
    
    /**
     * 数据脱敏配置
     */
    private DataMasking dataMasking = new DataMasking();
    
    /**
     * 性能监控配置
     */
    private Performance performance = new Performance();
    
    /**
     * 健康检查配置
     */
    private Health health = new Health();
    
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }
    
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }
    
    public ApiDoc getApiDoc() {
        return apiDoc;
    }
    
    public void setApiDoc(ApiDoc apiDoc) {
        this.apiDoc = apiDoc;
    }
    
    public Cors getCors() {
        return cors;
    }
    
    public void setCors(Cors cors) {
        this.cors = cors;
    }
    
    public RequestLogging getRequestLogging() {
        return requestLogging;
    }
    
    public void setRequestLogging(RequestLogging requestLogging) {
        this.requestLogging = requestLogging;
    }
    
    public RateLimit getRateLimit() {
        return rateLimit;
    }
    
    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }
    
    public Cache getCache() {
        return cache;
    }
    
    public void setCache(Cache cache) {
        this.cache = cache;
    }
    
    public Auth getAuth() {
        return auth;
    }
    
    public void setAuth(Auth auth) {
        this.auth = auth;
    }
    
    public DataMasking getDataMasking() {
        return dataMasking;
    }
    
    public void setDataMasking(DataMasking dataMasking) {
        this.dataMasking = dataMasking;
    }
    
    public Performance getPerformance() {
        return performance;
    }
    
    public void setPerformance(Performance performance) {
        this.performance = performance;
    }
    
    public Health getHealth() {
        return health;
    }
    
    public void setHealth(Health health) {
        this.health = health;
    }
    
    /**
     * 异常处理器配置
     */
    public static class ExceptionHandler {
        
        /**
         * 是否启用全局异常处理器
         */
        private boolean enabled = true;
        
        /**
         * 是否记录异常堆栈信息到日志
         */
        private boolean logStackTrace = true;
        
        /**
         * 是否在响应中包含异常详情（仅开发环境建议启用）
         */
        private boolean includeExceptionDetails = false;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isLogStackTrace() {
            return logStackTrace;
        }
        
        public void setLogStackTrace(boolean logStackTrace) {
            this.logStackTrace = logStackTrace;
        }
        
        public boolean isIncludeExceptionDetails() {
            return includeExceptionDetails;
        }
        
        public void setIncludeExceptionDetails(boolean includeExceptionDetails) {
            this.includeExceptionDetails = includeExceptionDetails;
        }
    }
    
    /**
     * API 文档配置
     */
    public static class ApiDoc {
        
        /**
         * 是否启用 API 文档
         */
        private boolean enabled = true;
        
        /**
         * API 文档标题
         */
        private String title = "Nebula API Documentation";
        
        /**
         * API 文档描述
         */
        private String description = "Nebula Framework REST API Documentation";
        
        /**
         * API 版本
         */
        private String version = "2.0.0";
        
        /**
         * 联系人信息
         */
        private String contactName = "Nebula Team";
        
        /**
         * 联系邮箱
         */
        private String contactEmail = "nebula@example.com";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getContactName() {
            return contactName;
        }
        
        public void setContactName(String contactName) {
            this.contactName = contactName;
        }
        
        public String getContactEmail() {
            return contactEmail;
        }
        
        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }
    }
    
    /**
     * CORS 配置
     */
    public static class Cors {
        
        /**
         * 是否启用 CORS
         */
        private boolean enabled = false;
        
        /**
         * 允许的源地址
         */
        private String[] allowedOrigins = {"*"};
        
        /**
         * 允许的请求方法
         */
        private String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
        
        /**
         * 允许的请求头
         */
        private String[] allowedHeaders = {"*"};
        
        /**
         * 是否允许发送凭据
         */
        private boolean allowCredentials = true;
        
        /**
         * 预检请求的缓存时间（秒）
         */
        private long maxAge = 3600;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String[] getAllowedOrigins() {
            return allowedOrigins;
        }
        
        public void setAllowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
        
        public String[] getAllowedMethods() {
            return allowedMethods;
        }
        
        public void setAllowedMethods(String[] allowedMethods) {
            this.allowedMethods = allowedMethods;
        }
        
        public String[] getAllowedHeaders() {
            return allowedHeaders;
        }
        
        public void setAllowedHeaders(String[] allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }
        
        public boolean isAllowCredentials() {
            return allowCredentials;
        }
        
        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }
        
        public long getMaxAge() {
            return maxAge;
        }
        
        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }
    
    /**
     * 请求日志配置
     */
    public static class RequestLogging {
        
        /**
         * 是否启用请求日志
         */
        private boolean enabled = false;
        
        /**
         * 是否记录请求头
         */
        private boolean includeHeaders = true;
        
        /**
         * 是否记录请求体（注意：可能影响性能）
         */
        private boolean includeRequestBody = false;
        
        /**
         * 是否记录响应体（注意：可能影响性能）
         */
        private boolean includeResponseBody = false;
        
        /**
         * 忽略的路径模式
         */
        private String[] ignorePaths = {"/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"};
        
        /**
         * 最大请求体记录长度
         */
        private int maxRequestBodyLength = 1024;
        
        /**
         * 最大响应体记录长度
         */
        private int maxResponseBodyLength = 1024;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isIncludeHeaders() {
            return includeHeaders;
        }
        
        public void setIncludeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
        }
        
        public boolean isIncludeRequestBody() {
            return includeRequestBody;
        }
        
        public void setIncludeRequestBody(boolean includeRequestBody) {
            this.includeRequestBody = includeRequestBody;
        }
        
        public boolean isIncludeResponseBody() {
            return includeResponseBody;
        }
        
        public void setIncludeResponseBody(boolean includeResponseBody) {
            this.includeResponseBody = includeResponseBody;
        }
        
        public String[] getIgnorePaths() {
            return ignorePaths;
        }
        
        public void setIgnorePaths(String[] ignorePaths) {
            this.ignorePaths = ignorePaths;
        }
        
        public int getMaxRequestBodyLength() {
            return maxRequestBodyLength;
        }
        
        public void setMaxRequestBodyLength(int maxRequestBodyLength) {
            this.maxRequestBodyLength = maxRequestBodyLength;
        }
        
        public int getMaxResponseBodyLength() {
            return maxResponseBodyLength;
        }
        
        public void setMaxResponseBodyLength(int maxResponseBodyLength) {
            this.maxResponseBodyLength = maxResponseBodyLength;
        }
    }
    
    /**
     * 限流配置
     */
    public static class RateLimit {
        
        /**
         * 是否启用限流
         */
        private boolean enabled = false;
        
        /**
         * 默认限流策略：每秒允许的请求数
         */
        private int defaultRequestsPerSecond = 100;
        
        /**
         * 限流时间窗口（秒）
         */
        private int timeWindow = 60;
        
        /**
         * 限流键生成策略：IP, USER, API, CUSTOM
         */
        private String keyStrategy = "IP";
        
        /**
         * 限流后的响应消息
         */
        private String limitExceededMessage = "请求过于频繁，请稍后再试";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getDefaultRequestsPerSecond() {
            return defaultRequestsPerSecond;
        }
        
        public void setDefaultRequestsPerSecond(int defaultRequestsPerSecond) {
            this.defaultRequestsPerSecond = defaultRequestsPerSecond;
        }
        
        public int getTimeWindow() {
            return timeWindow;
        }
        
        public void setTimeWindow(int timeWindow) {
            this.timeWindow = timeWindow;
        }
        
        public String getKeyStrategy() {
            return keyStrategy;
        }
        
        public void setKeyStrategy(String keyStrategy) {
            this.keyStrategy = keyStrategy;
        }
        
        public String getLimitExceededMessage() {
            return limitExceededMessage;
        }
        
        public void setLimitExceededMessage(String limitExceededMessage) {
            this.limitExceededMessage = limitExceededMessage;
        }
    }
    
    /**
     * 缓存配置
     */
    public static class Cache {
        
        /**
         * 是否启用响应缓存
         */
        private boolean enabled = false;
        
        /**
         * 默认缓存过期时间（秒）
         */
        private int defaultTtl = 300;
        
        /**
         * 最大缓存大小
         */
        private int maxSize = 1000;
        
        /**
         * 缓存键前缀
         */
        private String keyPrefix = "nebula:web:cache:";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getDefaultTtl() {
            return defaultTtl;
        }
        
        public void setDefaultTtl(int defaultTtl) {
            this.defaultTtl = defaultTtl;
        }
        
        public int getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public String getKeyPrefix() {
            return keyPrefix;
        }
        
        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
    
    /**
     * 认证配置
     */
    public static class Auth {
        
        /**
         * 是否启用认证
         */
        private boolean enabled = false;
        
        /**
         * JWT 密钥
         */
        private String jwtSecret = "nebula-default-secret-key";
        
        /**
         * JWT 过期时间（秒）
         */
        private int jwtExpiration = 86400;
        
        /**
         * 忽略认证的路径
         */
        private String[] ignorePaths = {"/public/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"};
        
        /**
         * 认证头名称
         */
        private String authHeader = "Authorization";
        
        /**
         * 认证头前缀
         */
        private String authHeaderPrefix = "Bearer ";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getJwtSecret() {
            return jwtSecret;
        }
        
        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }
        
        public int getJwtExpiration() {
            return jwtExpiration;
        }
        
        public void setJwtExpiration(int jwtExpiration) {
            this.jwtExpiration = jwtExpiration;
        }
        
        public String[] getIgnorePaths() {
            return ignorePaths;
        }
        
        public void setIgnorePaths(String[] ignorePaths) {
            this.ignorePaths = ignorePaths;
        }
        
        public String getAuthHeader() {
            return authHeader;
        }
        
        public void setAuthHeader(String authHeader) {
            this.authHeader = authHeader;
        }
        
        public String getAuthHeaderPrefix() {
            return authHeaderPrefix;
        }
        
        public void setAuthHeaderPrefix(String authHeaderPrefix) {
            this.authHeaderPrefix = authHeaderPrefix;
        }
    }
    
    /**
     * 数据脱敏配置
     */
    public static class DataMasking {
        
        /**
         * 是否启用数据脱敏
         */
        private boolean enabled = false;
        
        /**
         * 需要脱敏的字段名称模式
         */
        private String[] sensitiveFields = {"password", "mobile", "email", "idCard", "bankCard"};
        
        /**
         * 脱敏策略：MASK, HIDE, HASH
         */
        private String strategy = "MASK";
        
        /**
         * 掩码字符
         */
        private String maskChar = "*";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String[] getSensitiveFields() {
            return sensitiveFields;
        }
        
        public void setSensitiveFields(String[] sensitiveFields) {
            this.sensitiveFields = sensitiveFields;
        }
        
        public String getStrategy() {
            return strategy;
        }
        
        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
        
        public String getMaskChar() {
            return maskChar;
        }
        
        public void setMaskChar(String maskChar) {
            this.maskChar = maskChar;
        }
    }
    
    /**
     * 性能监控配置
     */
    public static class Performance {
        
        /**
         * 是否启用性能监控
         */
        private boolean enabled = false;
        
        /**
         * 慢查询阈值（毫秒）
         */
        private long slowRequestThreshold = 1000;
        
        /**
         * 是否记录详细指标
         */
        private boolean enableDetailedMetrics = true;
        
        /**
         * 指标收集间隔（秒）
         */
        private int metricsInterval = 60;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public long getSlowRequestThreshold() {
            return slowRequestThreshold;
        }
        
        public void setSlowRequestThreshold(long slowRequestThreshold) {
            this.slowRequestThreshold = slowRequestThreshold;
        }
        
        public boolean isEnableDetailedMetrics() {
            return enableDetailedMetrics;
        }
        
        public void setEnableDetailedMetrics(boolean enableDetailedMetrics) {
            this.enableDetailedMetrics = enableDetailedMetrics;
        }
        
        public int getMetricsInterval() {
            return metricsInterval;
        }
        
        public void setMetricsInterval(int metricsInterval) {
            this.metricsInterval = metricsInterval;
        }
    }
    
    /**
     * 健康检查配置
     */
    public static class Health {
        
        /**
         * 是否启用健康检查
         */
        private boolean enabled = true;
        
        /**
         * 健康检查端点路径
         */
        private String endpoint = "/health";
        
        /**
         * 是否显示详细信息
         */
        private boolean showDetails = false;
        
        /**
         * 检查间隔（秒）
         */
        private int checkInterval = 30;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public boolean isShowDetails() {
            return showDetails;
        }
        
        public void setShowDetails(boolean showDetails) {
            this.showDetails = showDetails;
        }
        
        public int getCheckInterval() {
            return checkInterval;
        }
        
        public void setCheckInterval(int checkInterval) {
            this.checkInterval = checkInterval;
        }
    }
}
