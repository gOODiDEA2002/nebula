package io.nebula.data.persistence.sharding;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
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
        
        // 配置分片算法（根据表配置动态创建）
        configureShardingAlgorithms(ruleConfig, tableConfigs);
        
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
     * 配置分片算法（根据表配置动态创建）
     */
    private void configureShardingAlgorithms(ShardingRuleConfiguration ruleConfig, List<TableShardingConfig> tableConfigs) {
        Map<String, AlgorithmConfiguration> algorithms = ruleConfig.getShardingAlgorithms();
        Map<String, AlgorithmConfiguration> keyGenerators = ruleConfig.getKeyGenerators();
        
        // 1. 为每个表的分片策略创建对应的算法
        for (TableShardingConfig tableConfig : tableConfigs) {
            // 1.1 数据库分片算法
            if (tableConfig.getDatabaseShardingConfig() != null) {
                DatabaseShardingConfig dbConfig = tableConfig.getDatabaseShardingConfig();
                createShardingAlgorithm(algorithms, dbConfig.getAlgorithmName(), dbConfig.getAlgorithmExpression());
            }
            
            // 1.2 表分片算法
            if (tableConfig.getTableShardingConfig() != null) {
                TableShardingConfigInternal tbConfig = tableConfig.getTableShardingConfig();
                createShardingAlgorithm(algorithms, tbConfig.getAlgorithmName(), tbConfig.getAlgorithmExpression());
            }
            
            // 1.3 主键生成算法
            if (tableConfig.getKeyGenerateConfig() != null) {
                KeyGenerateConfig keyConfig = tableConfig.getKeyGenerateConfig();
                String algorithmName = keyConfig.getAlgorithmName();
                if (!keyGenerators.containsKey(algorithmName)) {
                    keyGenerators.put(algorithmName, createKeyGenerateAlgorithm(algorithmName));
                }
            }
        }
        
        // 2. 配置默认算法（如果需要）
        if (shardingConfig.getDefaultDatabaseStrategy() != null) {
            DatabaseShardingConfig defaultDbStrategy = shardingConfig.getDefaultDatabaseStrategy();
            if (defaultDbStrategy.getAlgorithmExpression() != null) {
                createShardingAlgorithm(algorithms, defaultDbStrategy.getAlgorithmName(), 
                    defaultDbStrategy.getAlgorithmExpression());
            }
        }
        
        if (shardingConfig.getDefaultTableStrategy() != null) {
            TableShardingConfigInternal defaultTableStrategy = shardingConfig.getDefaultTableStrategy();
            if (defaultTableStrategy.getAlgorithmExpression() != null) {
                createShardingAlgorithm(algorithms, defaultTableStrategy.getAlgorithmName(), 
                    defaultTableStrategy.getAlgorithmExpression());
            }
        }
        
        // 3. 确保雪花算法和UUID算法总是可用
        keyGenerators.putIfAbsent("snowflake", new AlgorithmConfiguration("SNOWFLAKE", new Properties()));
        keyGenerators.putIfAbsent("uuid", new AlgorithmConfiguration("UUID", new Properties()));
        
        log.info("已配置 {} 个分片算法, {} 个主键生成算法", algorithms.size(), keyGenerators.size());
    }
    
    /**
     * 创建分片算法
     */
    private void createShardingAlgorithm(Map<String, AlgorithmConfiguration> algorithms, 
                                        String algorithmName, String algorithmExpression) {
        if (algorithmName == null || algorithms.containsKey(algorithmName)) {
            return;
        }
        
        if (algorithmExpression != null && !algorithmExpression.isEmpty()) {
            // 使用 INLINE 算法
            Properties props = new Properties();
            props.setProperty("algorithm-expression", algorithmExpression);
            algorithms.put(algorithmName, new AlgorithmConfiguration("INLINE", props));
            log.debug("创建 INLINE 算法: {} -> {}", algorithmName, algorithmExpression);
        } else {
            // 默认使用 MOD 算法
            Properties props = new Properties();
            props.setProperty("sharding-count", "2");
            algorithms.put(algorithmName, new AlgorithmConfiguration("MOD", props));
            log.debug("创建 MOD 算法: {} (默认)", algorithmName);
        }
    }
    
    /**
     * 创建主键生成算法
     */
    private AlgorithmConfiguration createKeyGenerateAlgorithm(String algorithmName) {
        switch (algorithmName.toLowerCase()) {
            case "snowflake":
                return new AlgorithmConfiguration("SNOWFLAKE", new Properties());
            case "uuid":
                return new AlgorithmConfiguration("UUID", new Properties());
            default:
                log.warn("未知的主键生成算法: {}, 使用默认的 SNOWFLAKE", algorithmName);
                return new AlgorithmConfiguration("SNOWFLAKE", new Properties());
        }
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
