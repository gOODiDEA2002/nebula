package io.nebula.data.access.exception;

/**
 * 乐观锁异常
 * 当乐观锁检查失败时抛出
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class OptimisticLockException extends DataAccessException {
    
    private final Class<?> entityType;
    private final Object entityId;
    private final Object expectedVersion;
    private final Object actualVersion;
    
    public OptimisticLockException(String message) {
        super(ErrorCodes.OPTIMISTIC_LOCK_FAILURE, message);
        this.entityType = null;
        this.entityId = null;
        this.expectedVersion = null;
        this.actualVersion = null;
    }
    
    public OptimisticLockException(Class<?> entityType, Object entityId, 
                                  Object expectedVersion, Object actualVersion) {
        super(ErrorCodes.OPTIMISTIC_LOCK_FAILURE, 
              String.format("Optimistic lock failure for entity %s with id %s: expected version %s, actual version %s", 
                          entityType.getSimpleName(), entityId, expectedVersion, actualVersion));
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    public OptimisticLockException(String message, Throwable cause) {
        super(ErrorCodes.OPTIMISTIC_LOCK_FAILURE, message, cause);
        this.entityType = null;
        this.entityId = null;
        this.expectedVersion = null;
        this.actualVersion = null;
    }
    
    public OptimisticLockException(Class<?> entityType, Object entityId, 
                                  Object expectedVersion, Object actualVersion, Throwable cause) {
        super(ErrorCodes.OPTIMISTIC_LOCK_FAILURE, 
              String.format("Optimistic lock failure for entity %s with id %s: expected version %s, actual version %s", 
                          entityType.getSimpleName(), entityId, expectedVersion, actualVersion), 
              cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
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
     * 获取期望版本
     */
    public Object getExpectedVersion() {
        return expectedVersion;
    }
    
    /**
     * 获取实际版本
     */
    public Object getActualVersion() {
        return actualVersion;
    }
    
    /**
     * 便捷的创建方法
     */
    public static OptimisticLockException of(Class<?> entityType, Object entityId, 
                                           Object expectedVersion, Object actualVersion) {
        return new OptimisticLockException(entityType, entityId, expectedVersion, actualVersion);
    }
}
