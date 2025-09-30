package io.nebula.data.persistence.readwrite.autoconfigure;

import io.nebula.data.persistence.datasource.DataSourceManager;
import io.nebula.data.persistence.readwrite.DynamicDataSource;
import io.nebula.data.persistence.readwrite.ReadWriteDataSourceManager;
import io.nebula.data.persistence.readwrite.aspect.ReadWriteDataSourceAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 读写分离自动配置
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "nebula.data.read-write-separation", name = "enabled", havingValue = "true")
@EnableConfigurationProperties
public class ReadWriteDataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public ReadWriteDataSourceManager readWriteDataSourceManager(DataSourceManager dataSourceManager) {
        log.info("Creating ReadWriteDataSourceManager");
        return new ReadWriteDataSourceManager(dataSourceManager);
    }
    
    /**
     * 创建动态数据源（读写分离）
     * 
     * 条件：
     * 1. nebula.data.read-write-separation.enabled = true
     * 2. nebula.data.read-write-separation.dynamic-routing = true
     * 3. 分片功能未启用（分片优先级更高）
     * 4. 没有其他 DataSource Bean
     */
    @Bean
    @Primary
    @ConditionalOnExpression("'${nebula.data.read-write-separation.dynamic-routing:false}' == 'true' && '${nebula.data.sharding.enabled:false}' != 'true'")
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dynamicDataSource(ReadWriteDataSourceManager readWriteManager) {
        log.info("创建动态数据源（读写分离模式）for default cluster");
        return new DynamicDataSource(readWriteManager, "default");
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nebula.data.read-write-separation", name = "aspect-enabled", havingValue = "true", matchIfMissing = false)
    public ReadWriteDataSourceAspect readWriteDataSourceAspect() {
        log.info("Creating ReadWriteDataSourceAspect");
        return new ReadWriteDataSourceAspect();
    }
}
