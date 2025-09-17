package io.nebula.core.config.properties;

import io.nebula.core.common.exception.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置
 */
@Data
@ConfigurationProperties(prefix = "nebula.data.sources")
public class DataSourcesConfiguration implements io.nebula.core.config.ConfigurationProperties {
    
    /**
     * 数据源配置映射
     */
    @Valid
    @NotEmpty(message = "至少需要配置一个数据源")
    private Map<String, DataSourceConfig> dataSources = new HashMap<>();
    
    @Override
    public String prefix() {
        return "nebula.data.sources";
    }
    
    @Override
    public void validate() {
        // 检查是否配置了主数据源
        if (!dataSources.containsKey("primary")) {
            throw ValidationException.of("dataSources", "必须配置名为 'primary' 的主数据源");
        }
        
        // 检查所有数据源配置
        for (Map.Entry<String, DataSourceConfig> entry : dataSources.entrySet()) {
            String name = entry.getKey();
            DataSourceConfig config = entry.getValue();
            
            if (config == null) {
                throw ValidationException.of("dataSources." + name, "数据源配置不能为空");
            }
            
            // 验证URL格式
            if (config.getUrl() != null && !isValidJdbcUrl(config.getUrl())) {
                throw ValidationException.of("dataSources." + name + ".url", 
                        "JDBC URL格式无效: " + config.getUrl());
            }
        }
    }
    
    @Override
    public String description() {
        return "数据源配置，支持多数据源";
    }
    
    /**
     * 获取主数据源配置
     * 
     * @return 主数据源配置
     */
    public DataSourceConfig getPrimaryDataSource() {
        return dataSources.get("primary");
    }
    
    /**
     * 获取指定名称的数据源配置
     * 
     * @param name 数据源名称
     * @return 数据源配置
     */
    public DataSourceConfig getDataSource(String name) {
        return dataSources.get(name);
    }
    
    /**
     * 验证JDBC URL格式
     * 
     * @param url JDBC URL
     * @return 是否有效
     */
    private boolean isValidJdbcUrl(String url) {
        return url.startsWith("jdbc:");
    }
    
    /**
     * 单个数据源配置
     */
    @Data
    public static class DataSourceConfig {
        
        /**
         * 数据源类型
         */
        @NotBlank(message = "数据源类型不能为空")
        private String type = "mysql";
        
        /**
         * JDBC URL
         */
        @NotBlank(message = "JDBC URL不能为空")
        private String url;
        
        /**
         * 认证信息
         */
        @Valid
        private CredentialsConfig credentials = new CredentialsConfig();
        
        /**
         * 连接池配置
         */
        @Valid
        private PoolConfig pool = new PoolConfig();
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * 描述
         */
        private String description;
    }
    
    /**
     * 认证信息配置
     */
    @Data
    public static class CredentialsConfig {
        
        /**
         * 用户名
         */
        @NotBlank(message = "数据库用户名不能为空")
        private String username;
        
        /**
         * 密码
         */
        @NotBlank(message = "数据库密码不能为空")
        private String password;
        
        /**
         * 驱动类名
         */
        private String driverClassName;
    }
    
    /**
     * 连接池配置
     */
    @Data
    public static class PoolConfig {
        
        /**
         * 最小连接数
         */
        @Min(value = 1, message = "最小连接数不能小于1")
        private int minSize = 5;
        
        /**
         * 最大连接数
         */
        @Min(value = 1, message = "最大连接数不能小于1")
        @Max(value = 100, message = "最大连接数不能超过100")
        private int maxSize = 20;
        
        /**
         * 连接超时时间
         */
        @Min(value = 1000, message = "连接超时时间不能小于1000毫秒")
        private Duration connectionTimeout = Duration.ofSeconds(30);
        
        /**
         * 空闲超时时间
         */
        private Duration idleTimeout = Duration.ofMinutes(10);
        
        /**
         * 最大生存时间
         */
        private Duration maxLifetime = Duration.ofMinutes(30);
        
        /**
         * 连接验证查询
         */
        private String validationQuery = "SELECT 1";
        
        /**
         * 是否自动提交
         */
        private boolean autoCommit = true;
        
        /**
         * 是否只读
         */
        private boolean readOnly = false;
        
        /**
         * 事务隔离级别
         */
        private String transactionIsolation;
        
        /**
         * 连接初始化SQL
         */
        private String connectionInitSql;
        
        /**
         * 连接测试查询
         */
        private String connectionTestQuery;
        
        /**
         * 验证连接配置
         */
        public void validate() {
            if (minSize > maxSize) {
                throw ValidationException.of("pool.minSize", 
                        "最小连接数不能大于最大连接数: min=" + minSize + ", max=" + maxSize);
            }
            
            if (connectionTimeout.toMillis() > maxLifetime.toMillis()) {
                throw ValidationException.of("pool.connectionTimeout", 
                        "连接超时时间不能大于最大生存时间");
            }
        }
    }
}
