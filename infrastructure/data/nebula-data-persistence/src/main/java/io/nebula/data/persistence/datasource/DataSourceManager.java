package io.nebula.data.persistence.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源管理器
 * 支持多数据源的动态配置和管理
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "nebula.data.persistence", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConfigurationProperties(prefix = "nebula.data.persistence")
public class DataSourceManager implements InitializingBean, DisposableBean {
    
    /**
     * 数据源缓存
     */
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    
    /**
     * 数据源配置
     */
    private Map<String, DataSourceConfig> sources;
    
    /**
     * 默认数据源名称
     */
    private String primary = "primary";
    
    public void setSources(Map<String, DataSourceConfig> sources) {
        this.sources = sources;
    }
    
    public void setPrimary(String primary) {
        this.primary = primary;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("DataSourceManager 开始初始化，配置的数据源: {}", sources != null ? sources.keySet() : "null");
        if (sources != null && !sources.isEmpty()) {
            initializeDataSources();
        } else {
            log.warn("没有配置数据源，DataSourceManager 将不会初始化任何数据源");
        }
    }
    
    @Override
    public void destroy() throws Exception {
        closeAllDataSources();
    }
    
    /**
     * 初始化数据源
     */
    private void initializeDataSources() {
        sources.forEach((name, config) -> {
            try {
                DataSource dataSource = createDataSource(config);
                dataSources.put(name, dataSource);
                log.info("数据源初始化成功: {}", name);
            } catch (Exception e) {
                log.error("数据源初始化失败: {}", name, e);
            }
        });
        
        // 验证主数据源是否存在
        if (!dataSources.containsKey(primary)) {
            log.warn("主数据源 '{}' 不存在，可能会导致应用启动失败", primary);
        }
    }
    
    /**
     * 创建数据源
     */
    private DataSource createDataSource(DataSourceConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        // 基本配置
        hikariConfig.setDriverClassName(config.getDriverClassName());
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        
        // 连接池配置
        if (config.getPool() != null) {
            PoolConfig pool = config.getPool();
            hikariConfig.setMinimumIdle(pool.getMinSize());
            hikariConfig.setMaximumPoolSize(pool.getMaxSize());
            hikariConfig.setConnectionTimeout(pool.getConnectionTimeout().toMillis());
            hikariConfig.setIdleTimeout(pool.getIdleTimeout().toMillis());
            hikariConfig.setMaxLifetime(pool.getMaxLifetime().toMillis());
            hikariConfig.setValidationTimeout(pool.getValidationTimeout().toMillis());
        } else {
            // 默认连接池配置
            hikariConfig.setMinimumIdle(5);
            hikariConfig.setMaximumPoolSize(20);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
        }
        
        // 其他配置
        hikariConfig.setConnectionTestQuery(config.getValidationQuery());
        hikariConfig.setAutoCommit(true);
        hikariConfig.setPoolName("NebulaPool-" + System.currentTimeMillis());
        
        return new HikariDataSource(hikariConfig);
    }
    
    /**
     * 获取数据源
     */
    public DataSource getDataSource(String name) {
        DataSource dataSource = dataSources.get(name);
        if (dataSource == null) {
            throw new IllegalArgumentException("数据源不存在: " + name);
        }
        return dataSource;
    }
    
    /**
     * 获取主数据源
     */
    public DataSource getPrimaryDataSource() {
        return getDataSource(primary);
    }
    
    /**
     * 检查数据源是否存在
     */
    public boolean containsDataSource(String name) {
        return dataSources.containsKey(name);
    }
    
    /**
     * 获取所有数据源名称
     */
    public java.util.Set<String> getDataSourceNames() {
        return dataSources.keySet();
    }
    
    /**
     * 测试数据源连接
     */
    public boolean testConnection(String name) {
        try {
            DataSource dataSource = getDataSource(name);
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(5);
            }
        } catch (Exception e) {
            log.error("测试数据源连接失败: {}", name, e);
            return false;
        }
    }
    
    /**
     * 关闭所有数据源
     */
    private void closeAllDataSources() {
        dataSources.forEach((name, dataSource) -> {
            try {
                if (dataSource instanceof HikariDataSource) {
                    ((HikariDataSource) dataSource).close();
                    log.info("数据源已关闭: {}", name);
                }
            } catch (Exception e) {
                log.error("关闭数据源失败: {}", name, e);
            }
        });
        dataSources.clear();
    }
    
    /**
     * 数据源配置类
     */
    public static class DataSourceConfig {
        private String type = "mysql";
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private String url;
        private String username;
        private String password;
        private String validationQuery = "SELECT 1";
        private PoolConfig pool;
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getValidationQuery() { return validationQuery; }
        public void setValidationQuery(String validationQuery) { this.validationQuery = validationQuery; }
        
        public PoolConfig getPool() { return pool; }
        public void setPool(PoolConfig pool) { this.pool = pool; }
    }
    
    /**
     * 连接池配置类
     */
    public static class PoolConfig {
        private int minSize = 5;
        private int maxSize = 20;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
        private Duration validationTimeout = Duration.ofSeconds(5);
        
        // Getters and Setters
        public int getMinSize() { return minSize; }
        public void setMinSize(int minSize) { this.minSize = minSize; }
        
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
        
        public Duration getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(Duration connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        
        public Duration getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(Duration idleTimeout) { this.idleTimeout = idleTimeout; }
        
        public Duration getMaxLifetime() { return maxLifetime; }
        public void setMaxLifetime(Duration maxLifetime) { this.maxLifetime = maxLifetime; }
        
        public Duration getValidationTimeout() { return validationTimeout; }
        public void setValidationTimeout(Duration validationTimeout) { this.validationTimeout = validationTimeout; }
    }
}
