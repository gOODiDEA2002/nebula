package io.nebula.autoconfigure.data;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import io.nebula.data.persistence.config.DefaultMetaObjectHandler;
import io.nebula.data.persistence.config.MyBatisPlusConfiguration;
import io.nebula.data.persistence.datasource.DataSourceManager;
import io.nebula.data.persistence.transaction.DefaultTransactionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 数据持久层自动配置类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration(before = org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class)
@ConditionalOnClass(name = "com.baomidou.mybatisplus.core.mapper.BaseMapper")
@ConditionalOnProperty(prefix = "nebula.data.persistence", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties
@Import({
    MyBatisPlusConfiguration.class,
    DataSourceManager.class,
    ReadWriteDataSourceAutoConfiguration.class,
    ShardingSphereAutoConfiguration.class
})
@MapperScan(basePackages = {
    "io.nebula.**.mapper"
}, markerInterface = io.nebula.data.persistence.mapper.BaseMapper.class)
public class DataPersistenceAutoConfiguration {
    
    @Autowired(required = false)
    private DataSourceManager dataSourceManager;
    
    /**
     * 将 DataSourceManager 的主数据源注册为 Spring 的 DataSource Bean (for MyBatis-Plus)
     * 
     * 条件：
     * 1. nebula.data.persistence.enabled = true
     * 2. 没有其他 dataSource Bean（通过类型检查）
     * 3. 分片功能未启用（分片优先级更高）
     * 4. 读写分离动态路由未启用（读写分离优先级更高）
     */
    @Bean("dataSource")
    @Primary
    @ConditionalOnExpression("'${nebula.data.persistence.enabled:false}' == 'true' && '${nebula.data.sharding.enabled:false}' != 'true'")
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource primaryDataSource() {
        if (dataSourceManager == null) {
            log.warn("DataSourceManager 未初始化，无法提供主数据源");
            return null;
        }
        
        try {
            DataSource primaryDataSource = dataSourceManager.getPrimaryDataSource();
            log.info("成功使用 Nebula DataSourceManager 的主数据源作为 Spring 的 DataSource Bean（普通数据访问模式）");
            return primaryDataSource;
        } catch (Exception e) {
            log.error("无法获取 Nebula 主数据源", e);
            return null;
        }
    }
    
    /**
     * 为 MyBatis-Plus 配置 SqlSessionFactory
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "nebula.data.persistence", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(SqlSessionFactory.class)
    public SqlSessionFactory sqlSessionFactory(
            DataSource dataSource,
            MybatisPlusInterceptor mybatisPlusInterceptor,
            com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler) throws Exception {
        log.info("配置 MyBatis-Plus SqlSessionFactory，使用 Nebula 数据源");
        
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // 设置 MyBatis-Plus 全局配置
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        factoryBean.setGlobalConfig(globalConfig);
        
        // 设置插件
        factoryBean.setPlugins(mybatisPlusInterceptor);
        
        // 设置类型别名包 - 只扫描 io.nebula 包下的实体类，避免扫描第三方库导致别名冲突
        factoryBean.setTypeAliasesPackage("io.nebula.**.entity.dos");
        
        // 设置 mapper XML 文件位置（如果有的话）
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            factoryBean.setMapperLocations(resolver.getResources("classpath*:/mapper/**/*.xml"));
        } catch (Exception e) {
            log.debug("No mapper XML files found, which is fine for annotation-based mappers");
        }
        
        return factoryBean.getObject();
    }
    
    /**
     * 默认事务管理器
     */
    @Bean
    @ConditionalOnMissingBean(io.nebula.data.persistence.transaction.TransactionManager.class)
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

