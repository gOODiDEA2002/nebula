package io.nebula.autoconfigure.data;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证主数据源缺失时 fail-fast：
 * 修复前 primaryDataSource() 返回 null(NullBean)，下游 sqlSessionFactory 注入时报出与根因脱节的错误；
 * 修复后直接抛异常并给出明确原因，启动即终止。
 */
class DataPersistenceFailFastTest {

    @Test
    void primaryDataSourceThrowsWhenManagerMissing() {
        // dataSourceManager 为 @Autowired(required=false) 字段，未设置时为 null
        DataPersistenceAutoConfiguration config = new DataPersistenceAutoConfiguration();

        assertThatThrownBy(config::primaryDataSource)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DataSourceManager 未初始化");
    }

    @Test
    void primaryDataSourceReturnTypeIsDataSource() {
        // 编译期约束：返回类型仍是 DataSource(不因 fail-fast 改动破坏 Bean 契约)
        assertThatThrownBy(() -> {
            DataSource ds = new DataPersistenceAutoConfiguration().primaryDataSource();
            ds.getConnection();
        }).isInstanceOf(IllegalStateException.class);
    }
}
