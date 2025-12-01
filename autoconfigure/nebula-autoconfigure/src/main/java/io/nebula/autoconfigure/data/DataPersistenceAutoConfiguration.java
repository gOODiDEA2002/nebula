package io.nebula.autoconfigure.data;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import io.nebula.data.persistence.config.DefaultMetaObjectHandler;
import io.nebula.data.persistence.config.MyBatisPlusConfiguration;
import io.nebula.data.persistence.config.MybatisPlusProperties;
import io.nebula.data.persistence.datasource.DataSourceManager;
import io.nebula.data.persistence.transaction.DefaultTransactionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

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
@EnableConfigurationProperties(MybatisPlusProperties.class)
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
    
    @Autowired(required = false)
    private MybatisPlusProperties mybatisPlusProperties;
    
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
     * 从 nebula.data.persistence.mybatis-plus 读取配置
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
        
        // 获取配置属性（使用默认值如果未配置）
        MybatisPlusProperties props = mybatisPlusProperties != null ? mybatisPlusProperties : new MybatisPlusProperties();
        
        // 配置 MyBatis Configuration
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(props.isMapUnderscoreToCamelCase());
        
        // 设置日志实现
        String logImplClass = props.getLogImplClass();
        if (StringUtils.hasText(logImplClass)) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Log> logClass = (Class<? extends Log>) Class.forName(logImplClass);
                configuration.setLogImpl(logClass);
                log.info("设置 MyBatis 日志实现: {}", logImplClass);
            } catch (ClassNotFoundException e) {
                log.warn("无法加载日志实现类: {}", logImplClass);
            }
        }
        factoryBean.setConfiguration(configuration);
        
        // 设置 MyBatis-Plus 全局配置
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        
        // 设置数据库配置
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        MybatisPlusProperties.DbConfig propsDbConfig = props.getGlobalConfig().getDbConfig();
        
        // 设置主键类型
        try {
            IdType idType = IdType.valueOf(propsDbConfig.getIdType().toUpperCase());
            dbConfig.setIdType(idType);
        } catch (IllegalArgumentException e) {
            log.warn("无效的主键类型: {}，使用默认值 AUTO", propsDbConfig.getIdType());
            dbConfig.setIdType(IdType.AUTO);
        }
        
        // 设置逻辑删除配置
        dbConfig.setLogicDeleteField(propsDbConfig.getLogicDeleteField());
        dbConfig.setLogicDeleteValue(String.valueOf(propsDbConfig.getLogicDeleteValue()));
        dbConfig.setLogicNotDeleteValue(String.valueOf(propsDbConfig.getLogicNotDeleteValue()));
        dbConfig.setTableUnderline(propsDbConfig.isTableUnderline());
        
        globalConfig.setDbConfig(dbConfig);
        factoryBean.setGlobalConfig(globalConfig);
        
        // 设置插件
        factoryBean.setPlugins(mybatisPlusInterceptor);
        
        // 设置类型别名包
        String typeAliasesPackage = props.getTypeAliasesPackage();
        if (StringUtils.hasText(typeAliasesPackage)) {
            factoryBean.setTypeAliasesPackage(typeAliasesPackage);
            log.info("设置 MyBatis 类型别名包: {}", typeAliasesPackage);
        }
        
        // 设置 mapper XML 文件位置
        String mapperLocations = props.getMapperLocations();
        if (StringUtils.hasText(mapperLocations)) {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            try {
                Resource[] resources = resolver.getResources(mapperLocations);
                if (resources.length > 0) {
                    factoryBean.setMapperLocations(resources);
                    log.info("设置 MyBatis Mapper 位置: {}，找到 {} 个文件", mapperLocations, resources.length);
                }
            } catch (Exception e) {
                log.debug("No mapper XML files found at {}, which is fine for annotation-based mappers", mapperLocations);
            }
        }
        
        return factoryBean.getObject();
    }
    
    /**
     * 默认事务管理器
     * 使用 @Qualifier 指定 applicationTaskExecutor，避免与其他 Executor bean 冲突
     */
    @Bean
    @ConditionalOnMissingBean(io.nebula.data.persistence.transaction.TransactionManager.class)
    public DefaultTransactionManager nebulaTransactionManager(
            PlatformTransactionManager platformTransactionManager,
            @Qualifier("applicationTaskExecutor") Executor taskExecutor) {
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

