package io.nebula.starter;

import jakarta.validation.Validator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

// Data模块自动配置
import io.nebula.data.persistence.autoconfigure.DataPersistenceAutoConfiguration;

// Storage模块自动配置
import io.nebula.storage.minio.autoconfigure.MinIOAutoConfiguration;
import io.nebula.storage.aliyun.oss.autoconfigure.AliyunOSSAutoConfiguration;

// Search模块自动配置
import io.nebula.search.elasticsearch.autoconfigure.ElasticsearchAutoConfiguration;

// Integration模块自动配置
import io.nebula.integration.payment.autoconfigure.PaymentAutoConfiguration;

// AI模块自动配置
import io.nebula.ai.spring.autoconfigure.AIAutoConfiguration;

/**
 * Nebula 框架自动配置
 */
@AutoConfiguration
@Import({
    DataPersistenceAutoConfiguration.class,
    MinIOAutoConfiguration.class,
    AliyunOSSAutoConfiguration.class,
    ElasticsearchAutoConfiguration.class,
    PaymentAutoConfiguration.class,
    AIAutoConfiguration.class
})
public class NebulaAutoConfiguration {
    
}
