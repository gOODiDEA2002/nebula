package io.nebula.data.persistence.sharding;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * ShardingSphere分片管理器
 * 提供分库分表的配置和管理功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Component
public class ShardingSphereManager {
    
    private final Map<String, DataSource> shardingDataSources = new HashMap<>();
    private final ShardingConfig shardingConfig;
    
    public ShardingSphereManager(ShardingConfig shardingConfig) {
        this.shardingConfig = shardingConfig;
    }
    
    /**
     * 创建分片数据源
     */
    public DataSource createShardingDataSource(String schemaName, Map<String, DataSource> dataSources) throws SQLException {
        log.info("Creating sharding data source for schema: {}", schemaName);
        
        // 构建分片规则配置
        ShardingRuleConfiguration shardingRuleConfig = createShardingRuleConfiguration(schemaName);
        
        // 创建ShardingSphere数据源
        DataSource shardingDataSource = ShardingSphereDataSourceFactory.createDataSource(
            schemaName, dataSources, Arrays.asList(shardingRuleConfig), new Properties()
        );
        
        // 缓存数据源
        shardingDataSources.put(schemaName, shardingDataSource);
        
        log.info("Successfully created sharding data source for schema: {}", schemaName);
        return shardingDataSource;
    }
    
    /**
     * 获取分片数据源
     */
    public DataSource getShardingDataSource(String schemaName) {
        return shardingDataSources.get(schemaName);
    }
    
    /**
     * 创建分片规则配置
     */
    private ShardingRuleConfiguration createShardingRuleConfiguration(String schemaName) {
        ShardingRuleConfiguration ruleConfig = new ShardingRuleConfiguration();
        
        // 获取该schema的配置
        List<TableShardingConfig> tableConfigs = shardingConfig.getTableConfigs(schemaName);
        
        for (TableShardingConfig tableConfig : tableConfigs) {
            // 创建表分片规则
            ShardingTableRuleConfiguration tableRuleConfig = createTableRuleConfiguration(tableConfig);
            ruleConfig.getTables().add(tableRuleConfig);
        }
        
        // 配置分片算法
        configureShardingAlgorithms(ruleConfig);
        
        // 配置默认分片策略
        configureDefaultShardingStrategy(ruleConfig);
        
        return ruleConfig;
    }
    
    /**
     * 创建表分片规则配置
     */
    private ShardingTableRuleConfiguration createTableRuleConfiguration(TableShardingConfig tableConfig) {
        ShardingTableRuleConfiguration tableRuleConfig = new ShardingTableRuleConfiguration(
            tableConfig.getLogicTable(), 
            tableConfig.getActualDataNodes()
        );
        
        // 配置分库策略
        if (tableConfig.getDatabaseShardingConfig() != null) {
            DatabaseShardingConfig dbConfig = tableConfig.getDatabaseShardingConfig();
            tableRuleConfig.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration(
                    dbConfig.getShardingColumn(), 
                    dbConfig.getAlgorithmName()
                )
            );
        }
        
        // 配置分表策略
        if (tableConfig.getTableShardingConfig() != null) {
            TableShardingConfigInternal tbConfig = tableConfig.getTableShardingConfig();
            tableRuleConfig.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration(
                    tbConfig.getShardingColumn(), 
                    tbConfig.getAlgorithmName()
                )
            );
        }
        
        // 配置主键生成策略
        if (tableConfig.getKeyGenerateConfig() != null) {
            KeyGenerateConfig keyConfig = tableConfig.getKeyGenerateConfig();
            tableRuleConfig.setKeyGenerateStrategy(
                new org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration(
                    keyConfig.getColumn(), 
                    keyConfig.getAlgorithmName()
                )
            );
        }
        
        return tableRuleConfig;
    }
    
    /**
     * 配置分片算法
     */
    private void configureShardingAlgorithms(ShardingRuleConfiguration ruleConfig) {
        Map<String, AlgorithmConfiguration> algorithms = ruleConfig.getShardingAlgorithms();
        
        // 配置内置分片算法
        
        // 取模算法
        algorithms.put("mod-algorithm", new AlgorithmConfiguration("MOD", createModProperties()));
        
        // 哈希取模算法
        algorithms.put("hash-mod-algorithm", new AlgorithmConfiguration("HASH_MOD", createHashModProperties()));
        
        // 范围分片算法
        algorithms.put("range-algorithm", new AlgorithmConfiguration("RANGE", createRangeProperties()));
        
        // 时间范围分片算法
        algorithms.put("datetime-range-algorithm", new AlgorithmConfiguration("DATETIME_RANGE", createDatetimeRangeProperties()));
        
        // 内联分片算法（自定义表达式）
        algorithms.put("inline-algorithm", new AlgorithmConfiguration("INLINE", createInlineProperties()));
        
        // 配置主键生成算法
        Map<String, AlgorithmConfiguration> keyGenerators = ruleConfig.getKeyGenerators();
        
        // 雪花算法
        keyGenerators.put("snowflake", new AlgorithmConfiguration("SNOWFLAKE", new Properties()));
        
        // UUID算法
        keyGenerators.put("uuid", new AlgorithmConfiguration("UUID", new Properties()));
    }
    
    /**
     * 配置默认分片策略
     */
    private void configureDefaultShardingStrategy(ShardingRuleConfiguration ruleConfig) {
        // 默认数据库分片策略
        if (shardingConfig.getDefaultDatabaseStrategy() != null) {
            DatabaseShardingConfig defaultDbStrategy = shardingConfig.getDefaultDatabaseStrategy();
            ruleConfig.setDefaultDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration(
                    defaultDbStrategy.getShardingColumn(),
                    defaultDbStrategy.getAlgorithmName()
                )
            );
        }
        
        // 默认表分片策略
        if (shardingConfig.getDefaultTableStrategy() != null) {
            TableShardingConfigInternal defaultTableStrategy = shardingConfig.getDefaultTableStrategy();
            ruleConfig.setDefaultTableShardingStrategy(
                new StandardShardingStrategyConfiguration(
                    defaultTableStrategy.getShardingColumn(),
                    defaultTableStrategy.getAlgorithmName()
                )
            );
        }
    }
    
    /**
     * 创建取模算法属性
     */
    private Properties createModProperties() {
        Properties props = new Properties();
        props.setProperty("sharding-count", "4"); // 默认分4个片
        return props;
    }
    
    /**
     * 创建哈希取模算法属性
     */
    private Properties createHashModProperties() {
        Properties props = new Properties();
        props.setProperty("sharding-count", "4");
        return props;
    }
    
    /**
     * 创建范围分片算法属性
     */
    private Properties createRangeProperties() {
        Properties props = new Properties();
        props.setProperty("range-lower", "1");
        props.setProperty("range-upper", "1000000");
        props.setProperty("sharding-count", "4");
        return props;
    }
    
    /**
     * 创建时间范围分片算法属性
     */
    private Properties createDatetimeRangeProperties() {
        Properties props = new Properties();
        props.setProperty("datetime-pattern", "yyyy-MM-dd HH:mm:ss");
        props.setProperty("datetime-lower", "2024-01-01 00:00:00");
        props.setProperty("datetime-upper", "2024-12-31 23:59:59");
        props.setProperty("sharding-suffix-pattern", "yyyyMM");
        return props;
    }
    
    /**
     * 创建内联分片算法属性
     */
    private Properties createInlineProperties() {
        Properties props = new Properties();
        props.setProperty("algorithm-expression", "ds${user_id % 2}");
        return props;
    }
    
    /**
     * 获取所有分片数据源
     */
    public Map<String, DataSource> getAllShardingDataSources() {
        return new HashMap<>(shardingDataSources);
    }
    
    /**
     * 检查分片数据源健康状态
     */
    public Map<String, Boolean> healthCheck() {
        Map<String, Boolean> healthStatus = new HashMap<>();
        
        for (Map.Entry<String, DataSource> entry : shardingDataSources.entrySet()) {
            String schemaName = entry.getKey();
            DataSource dataSource = entry.getValue();
            
            try {
                // 简单的连接测试
                try (var connection = dataSource.getConnection()) {
                    boolean isValid = connection.isValid(5); // 5秒超时
                    healthStatus.put(schemaName, isValid);
                }
            } catch (Exception e) {
                log.error("Health check failed for sharding data source: {}", schemaName, e);
                healthStatus.put(schemaName, false);
            }
        }
        
        return healthStatus;
    }
    
    /**
     * 获取分片统计信息
     */
    public Map<String, Object> getShardingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        for (String schemaName : shardingDataSources.keySet()) {
            Map<String, Object> schemaStats = new HashMap<>();
            
            List<TableShardingConfig> tableConfigs = shardingConfig.getTableConfigs(schemaName);
            schemaStats.put("tableCount", tableConfigs.size());
            schemaStats.put("tables", tableConfigs.stream()
                .map(TableShardingConfig::getLogicTable)
                .toArray(String[]::new));
            
            stats.put(schemaName, schemaStats);
        }
        
        return stats;
    }
    
    /**
     * 刷新分片配置
     */
    public void refreshShardingConfiguration(String schemaName) {
        log.info("Refreshing sharding configuration for schema: {}", schemaName);
        
        // 移除旧的数据源
        DataSource oldDataSource = shardingDataSources.remove(schemaName);
        if (oldDataSource != null) {
            // 这里可以添加数据源关闭逻辑
            log.info("Removed old sharding data source for schema: {}", schemaName);
        }
        
        // 重新创建数据源需要外部提供数据源映射
        log.info("Sharding configuration refreshed for schema: {}", schemaName);
    }
}
