package io.nebula.data.persistence.autoconfigure;

import io.nebula.data.persistence.datasource.DataSourceManager;
import io.nebula.data.persistence.readwrite.DynamicDataSource;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源优先级集成测试
 * 测试三种场景并存时的数据源优先级策略
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@DisplayName("数据源优先级集成测试")
class DataSourcePriorityTest {
    
    /**
     * 测试1：只启用普通数据访问
     */
    @SpringBootTest(classes = DataSourcePriorityTest.TestApplication.class)
    @TestPropertySource(properties = {
        "nebula.data.persistence.enabled=true",
        "nebula.data.persistence.sources.primary.type=mysql",
        "nebula.data.persistence.sources.primary.url=jdbc:h2:mem:primary;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.primary.username=sa",
        "nebula.data.persistence.sources.primary.password=",
        "nebula.data.read-write-separation.enabled=false",
        "nebula.data.sharding.enabled=false"
    })
    @DisplayName("场景1：只启用普通数据访问")
    static class OnlyNormalDataAccessTest {
        
        @Autowired
        private ApplicationContext applicationContext;
        
        @Test
        @DisplayName("应该使用普通数据源")
        void shouldUseNormalDataSource() {
            DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
            assertNotNull(dataSource, "数据源不应为null");
            // 普通数据源类型断言（HikariDataSource）
            assertTrue(dataSource.getClass().getName().contains("Hikari"), 
                "应该是HikariCP数据源");
        }
    }
    
    /**
     * 测试2：启用读写分离（不启用分片）
     */
    @SpringBootTest(classes = DataSourcePriorityTest.TestApplication.class)
    @TestPropertySource(properties = {
        "nebula.data.persistence.enabled=true",
        "nebula.data.persistence.sources.primary.type=mysql",
        "nebula.data.persistence.sources.primary.url=jdbc:h2:mem:primary;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.primary.username=sa",
        "nebula.data.persistence.sources.primary.password=",
        "nebula.data.persistence.sources.master.type=mysql",
        "nebula.data.persistence.sources.master.url=jdbc:h2:mem:master;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.master.username=sa",
        "nebula.data.persistence.sources.master.password=",
        "nebula.data.persistence.sources.slave01.type=mysql",
        "nebula.data.persistence.sources.slave01.url=jdbc:h2:mem:slave01;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.slave01.username=sa",
        "nebula.data.persistence.sources.slave01.password=",
        "nebula.data.read-write-separation.enabled=true",
        "nebula.data.read-write-separation.dynamic-routing=true",
        "nebula.data.read-write-separation.aspect-enabled=true",
        "nebula.data.read-write-separation.clusters.default.enabled=true",
        "nebula.data.read-write-separation.clusters.default.master=master",
        "nebula.data.read-write-separation.clusters.default.slaves[0]=slave01",
        "nebula.data.sharding.enabled=false"
    })
    @DisplayName("场景2：启用读写分离（优先级高于普通数据源）")
    static class ReadWriteSeparationTest {
        
        @Autowired
        private ApplicationContext applicationContext;
        
        @Test
        @DisplayName("应该使用动态数据源")
        void shouldUseDynamicDataSource() {
            DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
            assertNotNull(dataSource, "数据源不应为null");
            assertTrue(dataSource instanceof DynamicDataSource, 
                "应该是DynamicDataSource（读写分离）");
        }
    }
    
    /**
     * 测试3：启用分片（优先级最高）
     */
    @SpringBootTest(classes = DataSourcePriorityTest.TestApplication.class)
    @TestPropertySource(properties = {
        "nebula.data.persistence.enabled=true",
        "nebula.data.persistence.sources.ds0.type=mysql",
        "nebula.data.persistence.sources.ds0.url=jdbc:h2:mem:ds0;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.ds0.username=sa",
        "nebula.data.persistence.sources.ds0.password=",
        "nebula.data.persistence.sources.ds1.type=mysql",
        "nebula.data.persistence.sources.ds1.url=jdbc:h2:mem:ds1;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.ds1.username=sa",
        "nebula.data.persistence.sources.ds1.password=",
        "nebula.data.sharding.enabled=true",
        "nebula.data.sharding.schemas.default.data-sources[0]=ds0",
        "nebula.data.sharding.schemas.default.data-sources[1]=ds1",
        "nebula.data.sharding.schemas.default.tables[0].logic-table=t_order",
        "nebula.data.sharding.schemas.default.tables[0].actual-data-nodes=ds$->{0..1}.t_order_$->{0..1}",
        "nebula.data.sharding.schemas.default.tables[0].database-sharding-config.sharding-column=user_id",
        "nebula.data.sharding.schemas.default.tables[0].database-sharding-config.algorithm-name=db-mod",
        "nebula.data.sharding.schemas.default.tables[0].database-sharding-config.algorithm-expression=ds$->{user_id % 2}",
        "nebula.data.sharding.schemas.default.tables[0].table-sharding-config.sharding-column=id",
        "nebula.data.sharding.schemas.default.tables[0].table-sharding-config.algorithm-name=table-mod",
        "nebula.data.sharding.schemas.default.tables[0].table-sharding-config.algorithm-expression=t_order_$->{id % 2}",
        "nebula.data.sharding.schemas.default.tables[0].key-generate-config.column=id",
        "nebula.data.sharding.schemas.default.tables[0].key-generate-config.algorithm-name=snowflake",
        "nebula.data.read-write-separation.enabled=false"
    })
    @DisplayName("场景3：启用分片（优先级最高）")
    static class ShardingTest {
        
        @Autowired
        private ApplicationContext applicationContext;
        
        @Test
        @DisplayName("应该使用分片数据源")
        void shouldUseShardingDataSource() {
            DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
            assertNotNull(dataSource, "数据源不应为null");
            assertTrue(dataSource instanceof ShardingSphereDataSource, 
                "应该是ShardingSphereDataSource（分片）");
        }
    }
    
    /**
     * 测试4：三种场景并存
     */
    @SpringBootTest(classes = DataSourcePriorityTest.TestApplication.class)
    @TestPropertySource(properties = {
        // 普通数据源
        "nebula.data.persistence.enabled=true",
        "nebula.data.persistence.sources.primary.type=mysql",
        "nebula.data.persistence.sources.primary.url=jdbc:h2:mem:primary;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.primary.username=sa",
        "nebula.data.persistence.sources.primary.password=",
        // 读写分离数据源
        "nebula.data.persistence.sources.master.type=mysql",
        "nebula.data.persistence.sources.master.url=jdbc:h2:mem:master;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.master.username=sa",
        "nebula.data.persistence.sources.master.password=",
        "nebula.data.persistence.sources.slave01.type=mysql",
        "nebula.data.persistence.sources.slave01.url=jdbc:h2:mem:slave01;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.slave01.username=sa",
        "nebula.data.persistence.sources.slave01.password=",
        // 分片数据源
        "nebula.data.persistence.sources.ds0.type=mysql",
        "nebula.data.persistence.sources.ds0.url=jdbc:h2:mem:ds0;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.ds0.username=sa",
        "nebula.data.persistence.sources.ds0.password=",
        "nebula.data.persistence.sources.ds1.type=mysql",
        "nebula.data.persistence.sources.ds1.url=jdbc:h2:mem:ds1;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "nebula.data.persistence.sources.ds1.username=sa",
        "nebula.data.persistence.sources.ds1.password=",
        // 读写分离配置（dynamic-routing=false，不作为主数据源）
        "nebula.data.read-write-separation.enabled=true",
        "nebula.data.read-write-separation.dynamic-routing=false",
        "nebula.data.read-write-separation.aspect-enabled=true",
        "nebula.data.read-write-separation.clusters.product-cluster.enabled=true",
        "nebula.data.read-write-separation.clusters.product-cluster.master=master",
        "nebula.data.read-write-separation.clusters.product-cluster.slaves[0]=slave01",
        // 分片配置（优先级最高）
        "nebula.data.sharding.enabled=true",
        "nebula.data.sharding.schemas.default.data-sources[0]=ds0",
        "nebula.data.sharding.schemas.default.data-sources[1]=ds1",
        "nebula.data.sharding.schemas.default.tables[0].logic-table=t_order",
        "nebula.data.sharding.schemas.default.tables[0].actual-data-nodes=ds$->{0..1}.t_order_$->{0..1}",
        "nebula.data.sharding.schemas.default.tables[0].database-sharding-config.sharding-column=user_id",
        "nebula.data.sharding.schemas.default.tables[0].database-sharding-config.algorithm-name=db-mod",
        "nebula.data.sharding.schemas.default.tables[0].database-sharding-config.algorithm-expression=ds$->{user_id % 2}",
        "nebula.data.sharding.schemas.default.tables[0].table-sharding-config.sharding-column=id",
        "nebula.data.sharding.schemas.default.tables[0].table-sharding-config.algorithm-name=table-mod",
        "nebula.data.sharding.schemas.default.tables[0].table-sharding-config.algorithm-expression=t_order_$->{id % 2}",
        "nebula.data.sharding.schemas.default.tables[0].key-generate-config.column=id",
        "nebula.data.sharding.schemas.default.tables[0].key-generate-config.algorithm-name=snowflake"
    })
    @DisplayName("场景4：三种场景并存（分片优先）")
    static class CombinedTest {
        
        @Autowired
        private ApplicationContext applicationContext;
        
        @Test
        @DisplayName("主数据源应该是分片数据源")
        void primaryDataSourceShouldBeSharding() {
            DataSource dataSource = applicationContext.getBean("dataSource", DataSource.class);
            assertNotNull(dataSource, "数据源不应为null");
            assertTrue(dataSource instanceof ShardingSphereDataSource, 
                "主数据源应该是ShardingSphereDataSource（分片优先）");
        }
        
        @Test
        @DisplayName("应该存在ReadWriteDataSourceManager")
        void shouldHaveReadWriteDataSourceManager() {
            boolean hasBean = applicationContext.containsBean("readWriteDataSourceManager");
            assertTrue(hasBean, "应该存在ReadWriteDataSourceManager（用于注解驱动的读写分离）");
        }
        
        @Test
        @DisplayName("应该存在DataSourceManager")
        void shouldHaveDataSourceManager() {
            DataSourceManager dataSourceManager = applicationContext.getBean(DataSourceManager.class);
            assertNotNull(dataSourceManager, "应该存在DataSourceManager");
            
            // 验证所有数据源都已配置
            assertTrue(dataSourceManager.containsDataSource("primary"), "应该包含primary数据源");
            assertTrue(dataSourceManager.containsDataSource("master"), "应该包含master数据源");
            assertTrue(dataSourceManager.containsDataSource("slave01"), "应该包含slave01数据源");
            assertTrue(dataSourceManager.containsDataSource("ds0"), "应该包含ds0数据源");
            assertTrue(dataSourceManager.containsDataSource("ds1"), "应该包含ds1数据源");
        }
    }
    
    /**
     * 测试应用配置
     */
    @SpringBootApplication
    static class TestApplication {
        // 最小化的Spring Boot应用配置
    }
}
