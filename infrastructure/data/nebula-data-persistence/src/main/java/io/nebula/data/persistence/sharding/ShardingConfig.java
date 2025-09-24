package io.nebula.data.persistence.sharding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 分片配置类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "nebula.data.sharding")
public class ShardingConfig {
    
    /**
     * 是否启用分片
     */
    private boolean enabled = false;
    
    /**
     * 默认数据库分片策略
     */
    private DatabaseShardingConfig defaultDatabaseStrategy;
    
    /**
     * 默认表分片策略
     */
    private TableShardingConfigInternal defaultTableStrategy;
    
    /**
     * Schema配置映射
     */
    private Map<String, SchemaConfig> schemas = new HashMap<>();
    
    /**
     * 获取指定schema的表配置
     */
    public List<TableShardingConfig> getTableConfigs(String schemaName) {
        SchemaConfig schemaConfig = schemas.get(schemaName);
        return schemaConfig != null ? schemaConfig.getTables() : new ArrayList<>();
    }
    
    /**
     * 获取schema配置
     */
    public SchemaConfig getSchemaConfig(String schemaName) {
        return schemas.get(schemaName);
    }
    
    /**
     * Schema配置
     */
    @Data
    public static class SchemaConfig {
        /**
         * 数据源名称列表
         */
        private List<String> dataSources = new ArrayList<>();
        
        /**
         * 表分片配置列表
         */
        private List<TableShardingConfig> tables = new ArrayList<>();
        
        /**
         * 是否启用读写分离
         */
        private boolean readWriteSeparationEnabled = false;
        
        /**
         * 读写分离配置
         */
        private ReadWriteSeparationConfig readWriteSeparation;
    }
    
    /**
     * 读写分离配置（在分片环境中的配置）
     */
    @Data
    public static class ReadWriteSeparationConfig {
        /**
         * 读写分离数据源映射
         * key: 逻辑数据源名称
         * value: 读写分离配置
         */
        private Map<String, ReadWriteDataSourceConfig> dataSources = new HashMap<>();
    }
    
    /**
     * 读写数据源配置
     */
    @Data
    public static class ReadWriteDataSourceConfig {
        /**
         * 主库数据源名称
         */
        private String writeDataSource;
        
        /**
         * 从库数据源名称列表
         */
        private List<String> readDataSources = new ArrayList<>();
        
        /**
         * 负载均衡策略
         */
        private String loadBalanceAlgorithm = "ROUND_ROBIN";
    }
}

/**
 * 表分片配置
 */
@Data
class TableShardingConfig {
    /**
     * 逻辑表名
     */
    private String logicTable;
    
    /**
     * 实际数据节点
     * 例如: ds${0..1}.t_order${0..1}
     */
    private String actualDataNodes;
    
    /**
     * 数据库分片配置
     */
    private DatabaseShardingConfig databaseShardingConfig;
    
    /**
     * 表分片配置
     */
    private TableShardingConfigInternal tableShardingConfig;
    
    /**
     * 主键生成配置
     */
    private KeyGenerateConfig keyGenerateConfig;
}

/**
 * 数据库分片配置
 */
@Data
class DatabaseShardingConfig {
    /**
     * 分片列名
     */
    private String shardingColumn;
    
    /**
     * 分片算法名称
     */
    private String algorithmName;
    
    /**
     * 分片算法表达式（用于内联算法）
     */
    private String algorithmExpression;
    
    /**
     * 自定义算法属性
     */
    private Properties algorithmProperties = new Properties();
}

/**
 * 表分片配置（内部使用，避免类名冲突）
 */
@Data
class TableShardingConfigInternal {
    /**
     * 分片列名
     */
    private String shardingColumn;
    
    /**
     * 分片算法名称
     */
    private String algorithmName;
    
    /**
     * 分片算法表达式（用于内联算法）
     */
    private String algorithmExpression;
    
    /**
     * 自定义算法属性
     */
    private Properties algorithmProperties = new Properties();
}

/**
 * 主键生成配置
 */
@Data
class KeyGenerateConfig {
    /**
     * 主键列名
     */
    private String column;
    
    /**
     * 主键生成算法名称
     */
    private String algorithmName = "snowflake";
    
    /**
     * 算法属性
     */
    private Properties algorithmProperties = new Properties();
}
