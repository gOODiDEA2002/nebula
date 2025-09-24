package io.nebula.data.access.repository.impl;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存Repository实现
 * 用于测试和演示目的
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class InMemoryRepository<T, ID extends Serializable> extends AbstractRepository<T, ID> {
    
    private final Map<ID, T> dataStore = new ConcurrentHashMap<>();
    private final Class<T> entityType;
    private final Class<ID> idType;
    private final String idFieldName;
    
    public InMemoryRepository(Class<T> entityType, Class<ID> idType) {
        this(entityType, idType, "id");
    }
    
    public InMemoryRepository(Class<T> entityType, Class<ID> idType, String idFieldName) {
        this.entityType = entityType;
        this.idType = idType;
        this.idFieldName = idFieldName;
    }
    
    @Override
    protected ID getEntityId(T entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            Field idField = findIdField(entity.getClass());
            idField.setAccessible(true);
            return (ID) idField.get(entity);
        } catch (Exception e) {
            log.error("Error getting entity id from field: {}", idFieldName, e);
            throw new RuntimeException("Failed to get entity id", e);
        }
    }
    
    @Override
    protected void setEntityId(T entity, ID id) {
        if (entity == null) {
            return;
        }
        
        try {
            Field idField = findIdField(entity.getClass());
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            log.error("Error setting entity id to field: {}", idFieldName, e);
            throw new RuntimeException("Failed to set entity id", e);
        }
    }
    
    @Override
    protected ID generateId() {
        if (idType == String.class) {
            return (ID) UUID.randomUUID().toString();
        } else if (idType == Long.class) {
            return (ID) Long.valueOf(System.currentTimeMillis());
        } else if (idType == Integer.class) {
            return (ID) Integer.valueOf((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
        } else {
            // 对于其他类型，尝试使用UUID字符串
            return (ID) UUID.randomUUID().toString();
        }
    }
    
    @Override
    protected Map<ID, T> getDataStore() {
        return dataStore;
    }
    
    @Override
    protected T cloneEntity(T entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            // 简单的克隆实现，实际应用中可能需要更复杂的深拷贝
            T cloned = entityType.getDeclaredConstructor().newInstance();
            copyFields(entity, cloned);
            return cloned;
        } catch (Exception e) {
            log.warn("Error cloning entity, returning original: {}", e.getMessage());
            // 如果克隆失败，返回原对象（注意：这可能不安全）
            return entity;
        }
    }
    
    @Override
    protected Class<T> getEntityType() {
        return entityType;
    }
    
    @Override
    protected Class<ID> getIdType() {
        return idType;
    }
    
    /**
     * 查找ID字段
     */
    private Field findIdField(Class<?> clazz) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(idFieldName);
        } catch (NoSuchFieldException e) {
            // 如果在当前类中找不到，尝试在父类中查找
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findIdField(superClass);
            }
            throw e;
        }
    }
    
    /**
     * 复制字段值
     */
    private void copyFields(T source, T target) {
        Field[] fields = source.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(source);
                field.set(target, value);
            } catch (Exception e) {
                log.debug("Error copying field {}: {}", field.getName(), e.getMessage());
                // 忽略字段复制错误，继续处理其他字段
            }
        }
    }
    
    /**
     * 获取数据存储统计信息
     */
    public String getStats() {
        return String.format("InMemoryRepository Stats: entityType=%s, idType=%s, size=%d", 
                           entityType.getSimpleName(), idType.getSimpleName(), dataStore.size());
    }
    
    /**
     * 清空数据存储
     */
    public void clear() {
        dataStore.clear();
        log.debug("Cleared all data from repository");
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return dataStore.isEmpty();
    }
}
