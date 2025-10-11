package io.nebula.autoconfigure.storage;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.STSAssumeRoleSessionCredentialsProvider;
import io.nebula.storage.aliyun.oss.config.AliyunOSSProperties;
import io.nebula.storage.aliyun.oss.AliyunOSSStorageService;
import io.nebula.storage.core.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 阿里云OSS自动配置
 */
@Configuration
@ConditionalOnClass({OSS.class})
@ConditionalOnProperty(prefix = "nebula.storage.aliyun.oss", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(AliyunOSSProperties.class)
public class AliyunOSSAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(AliyunOSSAutoConfiguration.class);
    
    private final AliyunOSSProperties properties;
    private OSS ossClient;
    
    public AliyunOSSAutoConfiguration(AliyunOSSProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void init() {
        log.info("阿里云OSS存储服务已启用: endpoint={}, defaultBucket={}", 
                properties.getEndpoint(), properties.getDefaultBucket());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ClientBuilderConfiguration ossClientConfiguration() {
        ClientBuilderConfiguration config = new ClientBuilderConfiguration();
        config.setConnectionTimeout(properties.getConnectionTimeout());
        config.setSocketTimeout(properties.getSocketTimeout());
        config.setRequestTimeout(properties.getRequestTimeout());
        config.setIdleConnectionTime(properties.getIdleConnectionTime());
        config.setMaxConnections(properties.getMaxConnections());
        config.setMaxErrorRetry(properties.getMaxErrorRetry());
        config.setSupportCname(properties.isSupportCname());
        config.setSLDEnabled(properties.isSldEnabled());
        
        return config;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public OSS ossClient(ClientBuilderConfiguration config) {
        try {
            OSSClientBuilder builder = new OSSClientBuilder();
            
            // 根据是否有STS令牌选择认证方式
            if (StringUtils.hasText(properties.getSecurityToken())) {
                // 使用STS临时凭证
                STSAssumeRoleSessionCredentialsProvider credentialsProvider = 
                    CredentialsProviderFactory.newSTSAssumeRoleSessionCredentialsProvider(
                        properties.getAccessKeyId(),
                        properties.getAccessKeySecret(),
                        properties.getSecurityToken(),
                        properties.getRoleArn());
                        
                this.ossClient = builder.build(properties.getEndpoint(), credentialsProvider, config);
            } else {
                // 使用普通AccessKey
                DefaultCredentialProvider credentialsProvider = 
                    CredentialsProviderFactory.newDefaultCredentialProvider(
                        properties.getAccessKeyId(),
                        properties.getAccessKeySecret());
                        
                this.ossClient = builder.build(properties.getEndpoint(), credentialsProvider, config);
            }
            
            // 测试连接
            ossClient.listBuckets();
            log.info("阿里云OSS客户端连接成功: endpoint={}", properties.getEndpoint());
            
            return this.ossClient;
            
        } catch (Exception e) {
            log.error("阿里云OSS客户端初始化失败: endpoint={}", properties.getEndpoint(), e);
            throw new RuntimeException("阿里云OSS客户端初始化失败", e);
        }
    }
    
    @Bean
    @ConditionalOnMissingBean
    public StorageService aliyunOSSStorageService(OSS ossClient) {
        AliyunOSSStorageService service = new AliyunOSSStorageService(ossClient);
        
        // 自动创建默认存储桶
        if (properties.isAutoCreateDefaultBucket()) {
            try {
                String defaultBucket = properties.getDefaultBucket();
                if (!service.bucketExists(defaultBucket)) {
                    service.createBucket(defaultBucket);
                    log.info("自动创建默认存储桶: {}", defaultBucket);
                }
            } catch (Exception e) {
                log.warn("自动创建默认存储桶失败: {}", properties.getDefaultBucket(), e);
            }
        }
        
        return service;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public AliyunOSSStorageService aliyunOSSStorageServiceImpl(OSS ossClient) {
        return new AliyunOSSStorageService(ossClient);
    }
    
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
                log.info("阿里云OSS客户端已关闭");
            } catch (Exception e) {
                log.warn("关闭阿里云OSS客户端失败", e);
            }
        }
    }
}
