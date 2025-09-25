package io.nebula.data.persistence.readwrite.autoconfigure;

import io.nebula.data.persistence.datasource.DataSourceManager;
import io.nebula.data.persistence.readwrite.DynamicDataSource;
import io.nebula.data.persistence.readwrite.ReadWriteDataSourceManager;
import io.nebula.data.persistence.readwrite.aspect.ReadWriteDataSourceAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
    
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "nebula.data.read-write-separation", name = "dynamic-routing", havingValue = "true", matchIfMissing = false)
    public DataSource dynamicDataSource(ReadWriteDataSourceManager readWriteManager) {
        log.info("Creating DynamicDataSource for default cluster");
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
