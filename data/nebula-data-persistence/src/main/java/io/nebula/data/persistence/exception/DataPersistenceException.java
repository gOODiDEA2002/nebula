package io.nebula.data.persistence.exception;

import io.nebula.core.common.exception.NebulaException;

/**
 * 数据持久层异常
 */
public class DataPersistenceException extends NebulaException {
    
    public DataPersistenceException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public DataPersistenceException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public DataPersistenceException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    public DataPersistenceException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
    
    // 常用的数据持久层错误码
    public static class ErrorCodes {
        public static final String DATA_ACCESS_ERROR = "DATA_ACCESS_ERROR";
        public static final String DUPLICATE_KEY = "DUPLICATE_KEY";
        public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
        public static final String OPTIMISTIC_LOCK_FAILURE = "OPTIMISTIC_LOCK_FAILURE";
        public static final String TRANSACTION_ROLLBACK = "TRANSACTION_ROLLBACK";
        public static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";
        public static final String SQL_SYNTAX_ERROR = "SQL_SYNTAX_ERROR";
        public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
        public static final String INVALID_QUERY = "INVALID_QUERY";
        public static final String BATCH_UPDATE_FAILURE = "BATCH_UPDATE_FAILURE";
    }
    
    // 便捷的静态方法
    public static DataPersistenceException dataAccessError(String message) {
        return new DataPersistenceException(ErrorCodes.DATA_ACCESS_ERROR, message);
    }
    
    public static DataPersistenceException dataAccessError(String message, Throwable cause) {
        return new DataPersistenceException(ErrorCodes.DATA_ACCESS_ERROR, message, cause);
    }
    
    public static DataPersistenceException duplicateKey(String message) {
        return new DataPersistenceException(ErrorCodes.DUPLICATE_KEY, message);
    }
    
    public static DataPersistenceException constraintViolation(String message) {
        return new DataPersistenceException(ErrorCodes.CONSTRAINT_VIOLATION, message);
    }
    
    public static DataPersistenceException optimisticLockFailure(String message) {
        return new DataPersistenceException(ErrorCodes.OPTIMISTIC_LOCK_FAILURE, message);
    }
    
    public static DataPersistenceException transactionRollback(String message) {
        return new DataPersistenceException(ErrorCodes.TRANSACTION_ROLLBACK, message);
    }
    
    public static DataPersistenceException connectionFailure(String message) {
        return new DataPersistenceException(ErrorCodes.CONNECTION_FAILURE, message);
    }
    
    public static DataPersistenceException sqlSyntaxError(String message) {
        return new DataPersistenceException(ErrorCodes.SQL_SYNTAX_ERROR, message);
    }
    
    public static DataPersistenceException dataIntegrityViolation(String message) {
        return new DataPersistenceException(ErrorCodes.DATA_INTEGRITY_VIOLATION, message);
    }
    
    public static DataPersistenceException invalidQuery(String message) {
        return new DataPersistenceException(ErrorCodes.INVALID_QUERY, message);
    }
    
    public static DataPersistenceException batchUpdateFailure(String message) {
        return new DataPersistenceException(ErrorCodes.BATCH_UPDATE_FAILURE, message);
    }
}
