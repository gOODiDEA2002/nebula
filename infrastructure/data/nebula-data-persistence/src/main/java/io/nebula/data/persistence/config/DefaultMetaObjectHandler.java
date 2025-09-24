package io.nebula.data.persistence.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 默认元数据处理器
 * 自动填充创建时间、更新时间等字段
 */
@Slf4j
public class DefaultMetaObjectHandler implements MetaObjectHandler {
    
    // 常用字段名
    private static final String CREATE_TIME = "createTime";
    private static final String CREATED_AT = "createdAt";
    private static final String UPDATE_TIME = "updateTime";
    private static final String UPDATED_AT = "updatedAt";
    private static final String CREATE_BY = "createBy";
    private static final String UPDATE_BY = "updateBy";
    private static final String VERSION = "version";
    private static final String DELETED = "deleted";
    
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        
        // 填充创建时间
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, CREATED_AT, LocalDateTime.class, LocalDateTime.now());
        
        // 填充更新时间
        this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, UPDATED_AT, LocalDateTime.class, LocalDateTime.now());
        
        // 填充创建者（如果有用户上下文的话）
        this.fillValIfNullByName(CREATE_BY, getCurrentUserId(), metaObject, false);
        
        // 填充版本号
        this.strictInsertFill(metaObject, VERSION, Integer.class, 1);
        
        // 填充删除标记
        this.strictInsertFill(metaObject, DELETED, Integer.class, 0);
        
        log.debug("插入填充完成");
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        
        // 填充更新时间
        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, UPDATED_AT, LocalDateTime.class, LocalDateTime.now());
        
        // 填充更新者（如果有用户上下文的话）
        this.fillValIfNullByName(UPDATE_BY, getCurrentUserId(), metaObject, true);
        
        log.debug("更新填充完成");
    }
    
    /**
     * 填充值，如果字段值为null
     * 
     * @param fieldName  字段名
     * @param fieldVal   字段值
     * @param metaObject 元对象
     * @param isFill     是否填充
     */
    private void fillValIfNullByName(String fieldName, Object fieldVal, MetaObject metaObject, boolean isFill) {
        // 1. 没有 get 方法
        if (!metaObject.hasSetter(fieldName)) {
            return;
        }
        // 2. 如果用户有手动设置的值
        Object userSetValue = metaObject.getValue(fieldName);
        if (Objects.nonNull(userSetValue)) {
            return;
        }
        // 3. field 类型相同时设置
        if (isFill) {
            metaObject.setValue(fieldName, fieldVal);
        } else {
            this.setFieldValByName(fieldName, fieldVal, metaObject);
        }
    }
    
    /**
     * 获取当前用户ID
     * 这里返回默认值，实际项目中应该从用户上下文中获取
     * 
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        // TODO: 从Spring Security上下文或其他用户上下文中获取当前用户ID
        // 这里返回系统默认值
        return 1L;
    }
    
    /**
     * 获取当前用户名
     * 这里返回默认值，实际项目中应该从用户上下文中获取
     * 
     * @return 用户名
     */
    private String getCurrentUsername() {
        // TODO: 从Spring Security上下文或其他用户上下文中获取当前用户名
        // 这里返回系统默认值
        return "system";
    }
    
    /**
     * 安全地获取值
     * 
     * @param supplier 值供应器
     * @param <T>      值类型
     * @return 值
     */
    private <T> T safeGet(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("获取值失败", e);
            return null;
        }
    }
}
