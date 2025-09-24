package io.nebula.data.persistence.readwrite;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 动态数据源路由器
 * 根据当前线程的数据源上下文动态选择数据源
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {
    
    private final ReadWriteDataSourceManager readWriteManager;
    private final String clusterName;
    
    public DynamicDataSource(ReadWriteDataSourceManager readWriteManager, String clusterName) {
        this.readWriteManager = readWriteManager;
        this.clusterName = clusterName;
        initializeDataSources();
    }
    
    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType dataSourceType = DataSourceContextHolder.getDataSourceType();
        
        if (dataSourceType == null) {
            // 默认使用写数据源
            dataSourceType = DataSourceType.WRITE;
        }
        
        String lookupKey = clusterName + "." + dataSourceType.name().toLowerCase();
        log.debug("Determining data source lookup key: {}", lookupKey);
        
        return lookupKey;
    }
    
    @Override
    protected DataSource determineTargetDataSource() {
        DataSourceType dataSourceType = DataSourceContextHolder.getDataSourceType();
        
        if (dataSourceType == null) {
            dataSourceType = DataSourceType.WRITE;
        }
        
        try {
            DataSource targetDataSource;
            
            switch (dataSourceType) {
                case READ:
                    targetDataSource = readWriteManager.getReadDataSource(clusterName);
                    break;
                case WRITE:
                default:
                    targetDataSource = readWriteManager.getWriteDataSource(clusterName);
                    break;
            }
            
            log.debug("Selected {} data source for cluster: {}", dataSourceType, clusterName);
            return targetDataSource;
            
        } catch (Exception e) {
            log.error("Error determining target data source, using write data source", e);
            return readWriteManager.getWriteDataSource(clusterName);
        }
    }
    
    /**
     * 初始化数据源映射
     */
    private void initializeDataSources() {
        try {
            Map<Object, Object> targetDataSources = new java.util.HashMap<>();
            
            // 写数据源
            String writeKey = clusterName + ".write";
            DataSource writeDataSource = readWriteManager.getWriteDataSource(clusterName);
            targetDataSources.put(writeKey, writeDataSource);
            
            // 读数据源
            String readKey = clusterName + ".read";
            DataSource readDataSource = readWriteManager.getReadDataSource(clusterName);
            targetDataSources.put(readKey, readDataSource);
            
            // 设置目标数据源
            setTargetDataSources(targetDataSources);
            
            // 设置默认数据源为写数据源
            setDefaultTargetDataSource(writeDataSource);
            
            // 初始化
            afterPropertiesSet();
            
            log.info("Dynamic data source initialized for cluster: {}", clusterName);
            
        } catch (Exception e) {
            log.error("Failed to initialize dynamic data source for cluster: {}", clusterName, e);
            throw new RuntimeException("Dynamic data source initialization failed", e);
        }
    }
    
    /**
     * 刷新数据源映射
     */
    public void refreshDataSources() {
        log.info("Refreshing data sources for cluster: {}", clusterName);
        initializeDataSources();
    }
    
    /**
     * 获取集群名称
     */
    public String getClusterName() {
        return clusterName;
    }
}
