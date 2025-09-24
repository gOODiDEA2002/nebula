package io.nebula.data.persistence.sharding.autoconfigure;

import io.nebula.data.persistence.datasource.DataSourceManager;
import io.nebula.data.persistence.sharding.ShardingConfig;
import io.nebula.data.persistence.sharding.ShardingSphereManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * ShardingSphere自动配置
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory")
@ConditionalOnProperty(prefix = "nebula.data.sharding", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ShardingConfig.class)
public class ShardingSphereAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public ShardingSphereManager shardingSphereManager(ShardingConfig shardingConfig) {
        log.info("Creating ShardingSphereManager");
        return new ShardingSphereManager(shardingConfig);
    }
    
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "nebula.data.sharding", name = "enabled", havingValue = "true")
    public DataSource shardingDataSource(ShardingSphereManager shardingSphereManager,
                                       DataSourceManager dataSourceManager,
                                       ShardingConfig shardingConfig) throws SQLException {
        log.info("Creating sharding data source");
        
        // 获取默认schema配置
        String defaultSchema = "default";
        ShardingConfig.SchemaConfig schemaConfig = shardingConfig.getSchemaConfig(defaultSchema);
        
        if (schemaConfig == null) {
            log.warn("No schema config found for: {}, creating empty sharding data source", defaultSchema);
            // 创建一个空的配置
            schemaConfig = new ShardingConfig.SchemaConfig();
        }
        
        // 准备数据源映射
        Map<String, DataSource> dataSources = prepareDataSources(schemaConfig, dataSourceManager);
        
        // 创建分片数据源
        return shardingSphereManager.createShardingDataSource(defaultSchema, dataSources);
    }
    
    /**
     * 准备数据源映射
     */
    private Map<String, DataSource> prepareDataSources(ShardingConfig.SchemaConfig schemaConfig, 
                                                      DataSourceManager dataSourceManager) {
        Map<String, DataSource> dataSources = new HashMap<>();
        
        // 添加配置的数据源
        for (String dataSourceName : schemaConfig.getDataSources()) {
            DataSource dataSource = dataSourceManager.getDataSource(dataSourceName);
            if (dataSource != null) {
                dataSources.put(dataSourceName, dataSource);
                log.debug("Added data source to sharding: {}", dataSourceName);
            } else {
                log.warn("Data source not found: {}", dataSourceName);
            }
        }
        
        // 如果没有配置数据源，使用主数据源
        if (dataSources.isEmpty()) {
            DataSource primaryDataSource = dataSourceManager.getPrimaryDataSource();
            if (primaryDataSource != null) {
                dataSources.put("ds0", primaryDataSource);
                log.info("Using primary data source as default sharding data source");
            } else {
                log.error("No data sources available for sharding");
            }
        }
        
        return dataSources;
    }
    
    /**
     * 分片数据源工厂Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "shardingDataSourceFactory")
    public ShardingDataSourceFactory shardingDataSourceFactory(ShardingSphereManager shardingSphereManager,
                                                              DataSourceManager dataSourceManager,
                                                              ShardingConfig shardingConfig) {
        return new ShardingDataSourceFactory(shardingSphereManager, dataSourceManager, shardingConfig);
    }
    
    /**
     * 分片数据源工厂
     */
    public static class ShardingDataSourceFactory {
        private final ShardingSphereManager shardingSphereManager;
        private final DataSourceManager dataSourceManager;
        private final ShardingConfig shardingConfig;
        
        public ShardingDataSourceFactory(ShardingSphereManager shardingSphereManager,
                                       DataSourceManager dataSourceManager,
                                       ShardingConfig shardingConfig) {
            this.shardingSphereManager = shardingSphereManager;
            this.dataSourceManager = dataSourceManager;
            this.shardingConfig = shardingConfig;
        }
        
        /**
         * 为指定schema创建分片数据源
         */
        public DataSource createDataSource(String schemaName) throws SQLException {
            log.info("Creating sharding data source for schema: {}", schemaName);
            
            ShardingConfig.SchemaConfig schemaConfig = shardingConfig.getSchemaConfig(schemaName);
            if (schemaConfig == null) {
                throw new IllegalArgumentException("No schema config found for: " + schemaName);
            }
            
            // 准备数据源映射
            Map<String, DataSource> dataSources = new HashMap<>();
            for (String dataSourceName : schemaConfig.getDataSources()) {
                DataSource dataSource = dataSourceManager.getDataSource(dataSourceName);
                if (dataSource != null) {
                    dataSources.put(dataSourceName, dataSource);
                } else {
                    log.warn("Data source not found: {}", dataSourceName);
                }
            }
            
            if (dataSources.isEmpty()) {
                throw new IllegalStateException("No valid data sources found for schema: " + schemaName);
            }
            
            return shardingSphereManager.createShardingDataSource(schemaName, dataSources);
        }
        
        /**
         * 获取已创建的分片数据源
         */
        public DataSource getDataSource(String schemaName) {
            return shardingSphereManager.getShardingDataSource(schemaName);
        }
        
        /**
         * 获取所有分片数据源
         */
        public Map<String, DataSource> getAllDataSources() {
            return shardingSphereManager.getAllShardingDataSources();
        }
    }
}
