package io.nebula.data.persistence.readwrite;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据源上下文持有者
 * 使用ThreadLocal管理当前线程的数据源类型
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class DataSourceContextHolder {
    
    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置数据源类型
     */
    public static void setDataSourceType(DataSourceType dataSourceType) {
        if (dataSourceType == null) {
            log.warn("Trying to set null data source type, ignoring");
            return;
        }
        
        log.debug("Setting data source type to: {} for thread: {}", 
                 dataSourceType, Thread.currentThread().getName());
        CONTEXT.set(dataSourceType);
    }
    
    /**
     * 获取数据源类型
     */
    public static DataSourceType getDataSourceType() {
        DataSourceType dataSourceType = CONTEXT.get();
        log.debug("Getting data source type: {} for thread: {}", 
                 dataSourceType, Thread.currentThread().getName());
        return dataSourceType;
    }
    
    /**
     * 清除数据源类型
     */
    public static void clearDataSourceType() {
        log.debug("Clearing data source type for thread: {}", Thread.currentThread().getName());
        CONTEXT.remove();
    }
    
    /**
     * 设置为读数据源
     */
    public static void setRead() {
        setDataSourceType(DataSourceType.READ);
    }
    
    /**
     * 设置为写数据源
     */
    public static void setWrite() {
        setDataSourceType(DataSourceType.WRITE);
    }
    
    /**
     * 检查是否为读数据源
     */
    public static boolean isRead() {
        return DataSourceType.READ.equals(getDataSourceType());
    }
    
    /**
     * 检查是否为写数据源
     */
    public static boolean isWrite() {
        return DataSourceType.WRITE.equals(getDataSourceType());
    }
    
    /**
     * 执行读操作（自动设置和清理上下文）
     */
    public static <T> T executeRead(java.util.function.Supplier<T> supplier) {
        DataSourceType previousType = getDataSourceType();
        try {
            setRead();
            return supplier.get();
        } finally {
            if (previousType != null) {
                setDataSourceType(previousType);
            } else {
                clearDataSourceType();
            }
        }
    }
    
    /**
     * 执行写操作（自动设置和清理上下文）
     */
    public static <T> T executeWrite(java.util.function.Supplier<T> supplier) {
        DataSourceType previousType = getDataSourceType();
        try {
            setWrite();
            return supplier.get();
        } finally {
            if (previousType != null) {
                setDataSourceType(previousType);
            } else {
                clearDataSourceType();
            }
        }
    }
    
    /**
     * 执行读操作（无返回值）
     */
    public static void executeRead(Runnable runnable) {
        executeRead(() -> {
            runnable.run();
            return null;
        });
    }
    
    /**
     * 执行写操作（无返回值）
     */
    public static void executeWrite(Runnable runnable) {
        executeWrite(() -> {
            runnable.run();
            return null;
        });
    }
    
    /**
     * 获取当前上下文信息（用于调试）
     */
    public static String getContextInfo() {
        DataSourceType type = getDataSourceType();
        return String.format("Thread: %s, DataSourceType: %s", 
                           Thread.currentThread().getName(), 
                           type != null ? type : "null");
    }
}
