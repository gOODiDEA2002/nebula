package io.nebula.data.access.exception;

/**
 * 实体未找到异常
 * 当根据指定条件无法找到实体时抛出
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class EntityNotFoundException extends DataAccessException {
    
    private final Class<?> entityType;
    private final Object entityId;
    
    public EntityNotFoundException(String message) {
        super(ErrorCodes.ENTITY_NOT_FOUND, message);
        this.entityType = null;
        this.entityId = null;
    }
    
    public EntityNotFoundException(Class<?> entityType, Object entityId) {
        super(ErrorCodes.ENTITY_NOT_FOUND, 
              String.format("Entity of type %s with id %s not found", 
                          entityType.getSimpleName(), entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    public EntityNotFoundException(Class<?> entityType, Object entityId, String message) {
        super(ErrorCodes.ENTITY_NOT_FOUND, message);
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    public EntityNotFoundException(Class<?> entityType, Object entityId, Throwable cause) {
        super(ErrorCodes.ENTITY_NOT_FOUND, 
              String.format("Entity of type %s with id %s not found", 
                          entityType.getSimpleName(), entityId), 
              cause);
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    /**
     * 获取实体类型
     */
    public Class<?> getEntityType() {
        return entityType;
    }
    
    /**
     * 获取实体ID
     */
    public Object getEntityId() {
        return entityId;
    }
    
    /**
     * 便捷的创建方法
     */
    public static EntityNotFoundException of(Class<?> entityType, Object entityId) {
        return new EntityNotFoundException(entityType, entityId);
    }
    
    public static EntityNotFoundException of(Class<?> entityType, Object entityId, String message) {
        return new EntityNotFoundException(entityType, entityId, message);
    }
}
