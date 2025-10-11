package io.nebula.autoconfigure.storage;

import io.minio.MinioClient;
import io.nebula.storage.core.StorageService;
import io.nebula.storage.minio.config.MinIOProperties;
import io.nebula.storage.minio.MinIOStorageService;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * MinIO自动配置
 */
@Configuration
@ConditionalOnClass({MinioClient.class})
@ConditionalOnProperty(prefix = "nebula.storage.minio", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(MinIOProperties.class)
public class MinIOAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(MinIOAutoConfiguration.class);
    
    private final MinIOProperties properties;
    
    public MinIOAutoConfiguration(MinIOProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void init() {
        log.info("MinIO存储服务已启用: endpoint={}, defaultBucket={}", 
                properties.getEndpoint(), properties.getDefaultBucket());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(OkHttpClient okHttpClient) {
        try {
            MinioClient.Builder builder = MinioClient.builder()
                    .endpoint(properties.getEndpoint())
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .httpClient(okHttpClient);
            
            if (properties.getRegion() != null) {
                builder.region(properties.getRegion());
            }
            
            MinioClient client = builder.build();
            
            // 测试连接
            client.listBuckets();
            log.info("MinIO客户端连接成功: endpoint={}", properties.getEndpoint());
            
            return client;
            
        } catch (Exception e) {
            log.error("MinIO客户端初始化失败: endpoint={}", properties.getEndpoint(), e);
            throw new RuntimeException("MinIO客户端初始化失败", e);
        }
    }
    
    @Bean
    @ConditionalOnMissingBean
    @Primary
    public StorageService minioStorageService(MinioClient minioClient) {
        MinIOStorageService service = new MinIOStorageService(minioClient);
        
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
    public MinIOStorageService minIOStorageService(MinioClient minioClient) {
        return new MinIOStorageService(minioClient);
    }
}
