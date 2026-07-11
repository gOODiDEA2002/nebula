package io.nebula.data.persistence.sharding;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ShardingSphere管理器单元测试
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@DisplayName("ShardingSphere管理器单元测试")
class ShardingSphereManagerTest {
    
    private ShardingConfig shardingConfig;
    private ShardingSphereManager shardingSphereManager;
    private Map<String, DataSource> testDataSources;
    
    @BeforeEach
    void setUp() {
        // 初始化配置
        shardingConfig = new ShardingConfig();
        shardingConfig.setEnabled(true);
        
        // 配置schema
        ShardingConfig.SchemaConfig schemaConfig = new ShardingConfig.SchemaConfig();
        schemaConfig.setDataSources(List.of("ds0", "ds1"));
        
        // 配置订单表分片
        List<TableShardingConfig> tables = new ArrayList<>();
        TableShardingConfig orderTable = new TableShardingConfig();
        orderTable.setLogicTable("t_order");
        orderTable.setActualDataNodes("ds${0..1}.t_order_${0..1}");
        
        // 数据库分片配置
        DatabaseShardingConfig dbConfig = new DatabaseShardingConfig();
        dbConfig.setShardingColumn("user_id");
        dbConfig.setAlgorithmName("database-user-mod");
        dbConfig.setAlgorithmExpression("ds${user_id % 2}");
        orderTable.setDatabaseShardingConfig(dbConfig);
        
        // 表分片配置
        TableShardingConfigInternal tableConfig = new TableShardingConfigInternal();
        tableConfig.setShardingColumn("id");
        tableConfig.setAlgorithmName("table-order-mod");
        tableConfig.setAlgorithmExpression("t_order_${id % 2}");
        orderTable.setTableShardingConfig(tableConfig);
        
        // 主键生成配置
        KeyGenerateConfig keyConfig = new KeyGenerateConfig();
        keyConfig.setColumn("id");
        keyConfig.setAlgorithmName("snowflake");
        orderTable.setKeyGenerateConfig(keyConfig);
        
        tables.add(orderTable);
        schemaConfig.setTables(tables);
        
        Map<String, ShardingConfig.SchemaConfig> schemas = new HashMap<>();
        schemas.put("default", schemaConfig);
        shardingConfig.setSchemas(schemas);
        
        // 创建管理器
        shardingSphereManager = new ShardingSphereManager(shardingConfig);
        
        // 准备测试数据源（内存数据库）
        testDataSources = new HashMap<>();
        testDataSources.put("ds0", createTestDataSource("mem:ds0"));
        testDataSources.put("ds1", createTestDataSource("mem:ds1"));
    }
    
    @Test
    @DisplayName("测试创建分片数据源")
    void testCreateShardingDataSource() throws SQLException {
        // 执行
        DataSource shardingDataSource = shardingSphereManager.createShardingDataSource("default", testDataSources);
        
        // 验证
        assertNotNull(shardingDataSource, "分片数据源不应为null");
        assertTrue(shardingDataSource instanceof ShardingSphereDataSource, "应该是ShardingSphere数据源");
    }
    
    @Test
    @DisplayName("测试获取分片数据源")
    void testGetShardingDataSource() throws SQLException {
        // 准备
        shardingSphereManager.createShardingDataSource("default", testDataSources);
        
        // 执行
        DataSource dataSource = shardingSphereManager.getShardingDataSource("default");
        
        // 验证
        assertNotNull(dataSource, "应该能获取到分片数据源");
    }
    
    @Test
    @DisplayName("测试分片数据源健康检查")
    void testHealthCheck() throws SQLException {
        // 准备
        shardingSphereManager.createShardingDataSource("default", testDataSources);
        
        // 执行
        Map<String, Boolean> healthStatus = shardingSphereManager.healthCheck();
        
        // 验证
        assertNotNull(healthStatus, "健康状态不应为null");
        assertTrue(healthStatus.containsKey("default"), "应该包含default schema的健康状态");
    }
    
    @Test
    @DisplayName("测试获取分片统计信息")
    void testGetShardingStats() throws SQLException {
        // 准备
        shardingSphereManager.createShardingDataSource("default", testDataSources);
        
        // 执行
        Map<String, Object> stats = shardingSphereManager.getShardingStats();
        
        // 验证
        assertNotNull(stats, "统计信息不应为null");
        assertTrue(stats.containsKey("default"), "应该包含default schema的统计信息");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> defaultStats = (Map<String, Object>) stats.get("default");
        assertEquals(1, defaultStats.get("tableCount"), "应该有1个分片表");
    }
    
    @Test
    @DisplayName("测试INLINE算法自动创建")
    void testInlineAlgorithmAutoCreation() throws SQLException {
        // 执行
        DataSource shardingDataSource = shardingSphereManager.createShardingDataSource("default", testDataSources);
        
        // 验证 - 如果没有异常，说明INLINE算法已成功创建
        assertNotNull(shardingDataSource, "分片数据源应该成功创建");
    }
    
    @Test
    @DisplayName("测试雪花算法配置")
    void testSnowflakeKeyGeneration() throws SQLException {
        // 执行
        DataSource shardingDataSource = shardingSphereManager.createShardingDataSource("default", testDataSources);
        
        // 验证 - 如果没有异常，说明雪花算法已成功配置
        assertNotNull(shardingDataSource, "分片数据源应该成功创建，并包含雪花算法配置");
    }
    
    @Test
    @DisplayName("测试获取所有分片数据源")
    void testGetAllShardingDataSources() throws SQLException {
        // 准备
        shardingSphereManager.createShardingDataSource("default", testDataSources);
        
        // 执行
        Map<String, DataSource> allDataSources = shardingSphereManager.getAllShardingDataSources();
        
        // 验证
        assertNotNull(allDataSources, "所有数据源不应为null");
        assertEquals(1, allDataSources.size(), "应该有1个分片数据源");
        assertTrue(allDataSources.containsKey("default"), "应该包含default数据源");
    }

    @Test
    @DisplayName("测试分片与读写分离组合路由")
    void testShardingWithReadWriteSplitting() throws SQLException {
        DataSource writeDataSource = createTestDataSource("mem:write");
        DataSource readDataSource = createTestDataSource("mem:read");
        initializeProductTable(writeDataSource, "write-row");
        initializeProductTable(readDataSource, "read-row");
        testDataSources.put("write", writeDataSource);
        testDataSources.put("read", readDataSource);

        ShardingConfig.SchemaConfig schemaConfig = shardingConfig.getSchemaConfig("default");
        schemaConfig.setDataSources(List.of("ds0", "ds1", "write", "read"));
        schemaConfig.setReadWriteSeparationEnabled(true);
        ShardingConfig.ReadWriteDataSourceConfig group = new ShardingConfig.ReadWriteDataSourceConfig();
        group.setWriteDataSource("write");
        group.setReadDataSources(List.of("read"));
        group.setLoadBalanceAlgorithm("ROUND_ROBIN");
        ShardingConfig.ReadWriteSeparationConfig readWriteConfig =
                new ShardingConfig.ReadWriteSeparationConfig();
        readWriteConfig.setDataSources(Map.of("product_rw", group));
        schemaConfig.setReadWriteSeparation(readWriteConfig);

        TableShardingConfig productTable = new TableShardingConfig();
        productTable.setLogicTable("t_product");
        productTable.setActualDataNodes("product_rw.t_product");
        schemaConfig.getTables().add(productTable);

        DataSource combinedDataSource = shardingSphereManager.createShardingDataSource(
                "default", testDataSources);
        try (var connection = combinedDataSource.getConnection();
                var insert = connection.prepareStatement("INSERT INTO t_product (id, name) VALUES (?, ?)");
                var query = connection.prepareStatement("SELECT name FROM t_product WHERE id = ?")) {
            insert.setLong(1, 2L);
            insert.setString(2, "inserted");
            assertEquals(1, insert.executeUpdate());
            query.setLong(1, 1L);
            try (var result = query.executeQuery()) {
                assertTrue(result.next());
                assertEquals("read-row", result.getString(1));
            }
        }

        assertEquals(1, countRows(writeDataSource, 2L));
        assertEquals(0, countRows(readDataSource, 2L));
    }

    private void initializeProductTable(DataSource dataSource, String name) throws SQLException {
        try (var connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE t_product (id BIGINT PRIMARY KEY, name VARCHAR(64))");
            statement.execute("INSERT INTO t_product (id, name) VALUES (1, '" + name + "')");
        }
    }

    private int countRows(DataSource dataSource, long id) throws SQLException {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("SELECT COUNT(*) FROM t_product WHERE id = ?")) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
    
    /**
     * 创建测试用的数据源
     */
    private DataSource createTestDataSource(String jdbcUrl) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setJdbcUrl("jdbc:h2:" + jdbcUrl + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setMaximumPoolSize(2);
        return dataSource;
    }
}
