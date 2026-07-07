package io.nebula.data.persistence.readwrite.aspect;

import io.nebula.data.persistence.readwrite.DataSourceContextHolder;
import io.nebula.data.persistence.readwrite.DataSourceType;
import io.nebula.data.persistence.readwrite.annotation.ReadDataSource;
import io.nebula.data.persistence.readwrite.annotation.WriteDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 读写分离切面测试（CD-9）
 *
 * <p>切面 @Order(1) 先于事务切面执行, 同方法 @ReadDataSource+@Transactional 时
 * isActualTransactionActive 检查失效(事务未开启)。修复: 静态检测目标声明的
 * @Transactional, 非 readOnly 拒绝切读库。</p>
 */
class ReadWriteDataSourceAspectTest {

    private DemoService proxy;

    @BeforeEach
    void setUp() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new DemoService());
        factory.addAspect(new ReadWriteDataSourceAspect());
        proxy = factory.getProxy();
    }

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clearDataSourceType();
    }

    @Test
    @DisplayName("纯读方法: 正常切换到 READ")
    void plainReadSwitchesToRead() {
        assertThat(proxy.plainRead()).isEqualTo(DataSourceType.READ);
    }

    @Test
    @DisplayName("CD-9: @ReadDataSource + 写事务 @Transactional, 拒绝切读库")
    void readWithWriteTransactionRefused() {
        assertThat(proxy.readWithWriteTx()).isNull();
    }

    @Test
    @DisplayName("CD-9: force=true 也不放行写事务(写入会打到从库)")
    void forcedReadWithWriteTransactionStillRefused() {
        assertThat(proxy.forcedReadWithWriteTx()).isNull();
    }

    @Test
    @DisplayName("@ReadDataSource + @Transactional(readOnly=true): 合法组合, 放行")
    void readWithReadOnlyTransactionAllowed() {
        assertThat(proxy.readWithReadOnlyTx()).isEqualTo(DataSourceType.READ);
    }

    @Test
    @DisplayName("写方法不受影响: 正常切换到 WRITE")
    void writeWithTransactionAllowed() {
        assertThat(proxy.writeWithTx()).isEqualTo(DataSourceType.WRITE);
    }

    @Test
    @DisplayName("方法退出后恢复原数据源上下文")
    void contextRestoredAfterMethod() {
        proxy.plainRead();
        assertThat(DataSourceContextHolder.getDataSourceType()).isNull();
    }

    /**
     * 被代理的演示服务: 方法内部记录切面设置的数据源类型
     */
    static class DemoService {

        @ReadDataSource
        public DataSourceType plainRead() {
            return DataSourceContextHolder.getDataSourceType();
        }

        @ReadDataSource
        @Transactional
        public DataSourceType readWithWriteTx() {
            return DataSourceContextHolder.getDataSourceType();
        }

        @ReadDataSource(force = true)
        @Transactional
        public DataSourceType forcedReadWithWriteTx() {
            return DataSourceContextHolder.getDataSourceType();
        }

        @ReadDataSource
        @Transactional(readOnly = true)
        public DataSourceType readWithReadOnlyTx() {
            return DataSourceContextHolder.getDataSourceType();
        }

        @WriteDataSource
        @Transactional
        public DataSourceType writeWithTx() {
            return DataSourceContextHolder.getDataSourceType();
        }
    }
}
