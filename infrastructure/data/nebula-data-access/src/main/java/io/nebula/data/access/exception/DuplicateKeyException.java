package io.nebula.data.access.exception;

/**
 * 重复键异常
 * 当插入或更新操作违反唯一约束时抛出
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class DuplicateKeyException extends DataAccessException {
    
    private final String keyName;
    private final Object keyValue;
    
    public DuplicateKeyException(String message) {
        super(ErrorCodes.DUPLICATE_KEY, message);
        this.keyName = null;
        this.keyValue = null;
    }
    
    public DuplicateKeyException(String keyName, Object keyValue) {
        super(ErrorCodes.DUPLICATE_KEY, 
              String.format("Duplicate key '%s' with value '%s'", keyName, keyValue));
        this.keyName = keyName;
        this.keyValue = keyValue;
    }
    
    public DuplicateKeyException(String keyName, Object keyValue, String message) {
        super(ErrorCodes.DUPLICATE_KEY, message);
        this.keyName = keyName;
        this.keyValue = keyValue;
    }
    
    public DuplicateKeyException(String message, Throwable cause) {
        super(ErrorCodes.DUPLICATE_KEY, message, cause);
        this.keyName = null;
        this.keyValue = null;
    }
    
    public DuplicateKeyException(String keyName, Object keyValue, Throwable cause) {
        super(ErrorCodes.DUPLICATE_KEY, 
              String.format("Duplicate key '%s' with value '%s'", keyName, keyValue), 
              cause);
        this.keyName = keyName;
        this.keyValue = keyValue;
    }
    
    /**
     * 获取键名
     */
    public String getKeyName() {
        return keyName;
    }
    
    /**
     * 获取键值
     */
    public Object getKeyValue() {
        return keyValue;
    }
    
    /**
     * 便捷的创建方法
     */
    public static DuplicateKeyException of(String keyName, Object keyValue) {
        return new DuplicateKeyException(keyName, keyValue);
    }
    
    public static DuplicateKeyException of(String keyName, Object keyValue, String message) {
        return new DuplicateKeyException(keyName, keyValue, message);
    }
}
