package io.nebula.data.persistence.autoconfigure;

import io.nebula.data.persistence.config.DefaultMetaObjectHandler;
import io.nebula.data.persistence.config.MyBatisPlusConfiguration;
import io.nebula.data.persistence.datasource.DataSourceManager;
import io.nebula.data.persistence.transaction.DefaultTransactionManager;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 数据持久层自动配置类
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "com.baomidou.mybatisplus.core.mapper.BaseMapper")
@ConditionalOnProperty(prefix = "nebula.data.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties
@Import({
    MyBatisPlusConfiguration.class,
    DataSourceManager.class,
    io.nebula.data.persistence.readwrite.autoconfigure.ReadWriteDataSourceAutoConfiguration.class,
    io.nebula.data.persistence.sharding.autoconfigure.ShardingSphereAutoConfiguration.class
})
@MapperScan(basePackages = {
    "io.nebula.**.mapper",
    "**.mapper"
}, markerInterface = io.nebula.data.persistence.mapper.BaseMapper.class)
public class DataPersistenceAutoConfiguration {
    
    /**
     * 默认事务管理器
     */
    @Bean
    @ConditionalOnMissingBean(io.nebula.data.access.transaction.TransactionManager.class)
    public DefaultTransactionManager nebulaTransactionManager(
            PlatformTransactionManager platformTransactionManager,
            Executor taskExecutor) {
        log.info("配置Nebula事务管理器");
        return new DefaultTransactionManager(platformTransactionManager, taskExecutor);
    }
    
    /**
     * 任务执行器（如果没有定义的话）
     */
    @Bean
    @ConditionalOnMissingBean(Executor.class)
    public Executor taskExecutor() {
        log.info("配置默认任务执行器");
        return Executors.newFixedThreadPool(10);
    }
    
    /**
     * 元数据处理器（如果没有定义的话）
     */
    @Bean
    @ConditionalOnMissingBean(com.baomidou.mybatisplus.core.handlers.MetaObjectHandler.class)
    public DefaultMetaObjectHandler defaultMetaObjectHandler() {
        log.info("配置默认元数据处理器");
        return new DefaultMetaObjectHandler();
    }
}
