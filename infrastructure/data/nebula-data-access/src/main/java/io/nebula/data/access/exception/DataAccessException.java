package io.nebula.data.access.exception;

/**
 * 数据访问异常基类
 * 所有数据访问相关异常的基础类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class DataAccessException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] args;
    
    public DataAccessException(String message) {
        super(message);
        this.errorCode = "DATA_ACCESS_ERROR";
        this.args = new Object[0];
    }
    
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DATA_ACCESS_ERROR";
        this.args = new Object[0];
    }
    
    public DataAccessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }
    
    public DataAccessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }
    
    public DataAccessException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args != null ? args : new Object[0];
    }
    
    public DataAccessException(String errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args != null ? args : new Object[0];
    }
    
    /**
     * 获取错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 获取错误参数
     */
    public Object[] getArgs() {
        return args.clone();
    }
    
    /**
     * 错误代码常量
     */
    public static class ErrorCodes {
        public static final String DATA_ACCESS_ERROR = "DATA_ACCESS_ERROR";
        public static final String ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";
        public static final String DUPLICATE_KEY = "DUPLICATE_KEY";
        public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
        public static final String OPTIMISTIC_LOCK_FAILURE = "OPTIMISTIC_LOCK_FAILURE";
        public static final String PESSIMISTIC_LOCK_FAILURE = "PESSIMISTIC_LOCK_FAILURE";
        public static final String TRANSACTION_FAILURE = "TRANSACTION_FAILURE";
        public static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";
        public static final String QUERY_TIMEOUT = "QUERY_TIMEOUT";
        public static final String INVALID_QUERY = "INVALID_QUERY";
        public static final String BATCH_UPDATE_FAILURE = "BATCH_UPDATE_FAILURE";
        public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
        public static final String INVALID_DATA_FORMAT = "INVALID_DATA_FORMAT";
        public static final String REPOSITORY_ERROR = "REPOSITORY_ERROR";
        public static final String QUERY_BUILDER_ERROR = "QUERY_BUILDER_ERROR";
    }
}
