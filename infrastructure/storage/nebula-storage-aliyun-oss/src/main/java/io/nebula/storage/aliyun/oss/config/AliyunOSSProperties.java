package io.nebula.storage.aliyun.oss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 阿里云OSS配置属性
 */
@ConfigurationProperties(prefix = "nebula.storage.aliyun.oss")
@Validated
@Data
public class AliyunOSSProperties {
    
    /**
     * 是否启用阿里云OSS存储
     */
    private boolean enabled = true;
    
    /**
     * OSS服务端点
     */
    @NotBlank(message = "OSS endpoint cannot be blank")
    private String endpoint;
    
    /**
     * 访问密钥ID
     */
    @NotBlank(message = "OSS access key ID cannot be blank")
    private String accessKeyId;
    
    /**
     * 访问密钥Secret
     */
    @NotBlank(message = "OSS access key secret cannot be blank")
    private String accessKeySecret;
    
    /**
     * 角色ARN
     */
    private String roleArn;
    
    /**
     * STS令牌（可选）
     */
    private String securityToken;
    
    /**
     * 默认存储桶名称
     */
    private String defaultBucket = "default";
    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 50000;
    
    /**
     * Socket超时时间（毫秒）
     */
    private int socketTimeout = 50000;
    
    /**
     * 请求超时时间（毫秒）
     */
    private int requestTimeout = 300000;
    
    /**
     * 空闲连接时间（毫秒）
     */
    private long idleConnectionTime = 60000;
    
    /**
     * 最大连接数
     */
    private int maxConnections = 1024;
    
    /**
     * 最大错误重试次数
     */
    private int maxErrorRetry = 3;
    
    /**
     * 是否支持CNAME
     */
    private boolean supportCname = false;
    
    /**
     * 是否开启二级域名
     */
    private boolean sldEnabled = false;
    
    /**
     * 自动创建默认存储桶
     */
    private boolean autoCreateDefaultBucket = true;
    
    /**
     * 预签名URL默认过期时间（秒）
     */
    private int defaultExpiry = 3600;
    
    /**
     * 最大文件大小（字节）
     */
    private long maxFileSize = 100 * 1024 * 1024; // 100MB
    
    /**
     * 允许的文件类型
     */
    private String[] allowedContentTypes = {
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "text/plain", "application/octet-stream"
    };

}

