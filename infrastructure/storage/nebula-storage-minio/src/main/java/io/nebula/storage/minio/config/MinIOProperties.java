package io.nebula.storage.minio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

/**
 * MinIO配置属性
 */
@ConfigurationProperties(prefix = "nebula.storage.minio")
@Validated
public class MinIOProperties {
    
    /**
     * 是否启用MinIO存储
     */
    private boolean enabled = true;
    
    /**
     * MinIO服务器端点
     */
    @NotBlank(message = "MinIO endpoint cannot be blank")
    private String endpoint = "http://localhost:9000";
    
    /**
     * 访问密钥
     */
    @NotBlank(message = "MinIO access key cannot be blank")
    private String accessKey = "minioadmin";
    
    /**
     * 秘密密钥
     */
    @NotBlank(message = "MinIO secret key cannot be blank")
    private String secretKey = "minioadmin";
    
    /**
     * 默认存储桶名称
     */
    private String defaultBucket = "default";
    
    /**
     * 是否使用SSL
     */
    private boolean secure = false;
    
    /**
     * 连接超时时间（毫秒）
     */
    @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
    private long connectTimeout = 10000;
    
    /**
     * 写超时时间（毫秒）
     */
    @Min(value = 1000, message = "Write timeout must be at least 1000ms")
    private long writeTimeout = 10000;
    
    /**
     * 读超时时间（毫秒）
     */
    @Min(value = 1000, message = "Read timeout must be at least 1000ms")
    private long readTimeout = 10000;
    
    /**
     * 区域
     */
    private String region;
    
    /**
     * 自动创建默认存储桶
     */
    private boolean autoCreateDefaultBucket = true;
    
    /**
     * 预签名URL默认过期时间（秒）
     */
    @Min(value = 60, message = "Default expiry must be at least 60 seconds")
    private int defaultExpiry = 3600;
    
    /**
     * 最大文件大小（字节）
     */
    @Min(value = 1024, message = "Max file size must be at least 1024 bytes")
    private long maxFileSize = 100 * 1024 * 1024; // 100MB
    
    /**
     * 允许的文件类型
     */
    private String[] allowedContentTypes = {
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "text/plain", "application/octet-stream"
    };
    
    // Getter and Setter methods
    
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
    
    public String getAccessKey() {
        return accessKey;
    }
    
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getDefaultBucket() {
        return defaultBucket;
    }
    
    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }
    
    public boolean isSecure() {
        return secure;
    }
    
    public void setSecure(boolean secure) {
        this.secure = secure;
    }
    
    public long getConnectTimeout() {
        return connectTimeout;
    }
    
    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    public long getWriteTimeout() {
        return writeTimeout;
    }
    
    public void setWriteTimeout(long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
    
    public long getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public boolean isAutoCreateDefaultBucket() {
        return autoCreateDefaultBucket;
    }
    
    public void setAutoCreateDefaultBucket(boolean autoCreateDefaultBucket) {
        this.autoCreateDefaultBucket = autoCreateDefaultBucket;
    }
    
    public int getDefaultExpiry() {
        return defaultExpiry;
    }
    
    public void setDefaultExpiry(int defaultExpiry) {
        this.defaultExpiry = defaultExpiry;
    }
    
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    public String[] getAllowedContentTypes() {
        return allowedContentTypes;
    }
    
    public void setAllowedContentTypes(String[] allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}

