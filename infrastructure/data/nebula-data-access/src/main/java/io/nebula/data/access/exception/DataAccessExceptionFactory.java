package io.nebula.data.access.exception;

/**
 * 数据访问异常工厂
 * 提供便捷的方法来创建各种数据访问异常
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public final class DataAccessExceptionFactory {
    
    private DataAccessExceptionFactory() {
        // 防止实例化
    }
    
    // ========== 实体未找到异常 ==========
    
    public static EntityNotFoundException entityNotFound(Class<?> entityType, Object entityId) {
        return EntityNotFoundException.of(entityType, entityId);
    }
    
    public static EntityNotFoundException entityNotFound(Class<?> entityType, Object entityId, String message) {
        return EntityNotFoundException.of(entityType, entityId, message);
    }
    
    public static EntityNotFoundException entityNotFound(String message) {
        return new EntityNotFoundException(message);
    }
    
    // ========== 重复键异常 ==========
    
    public static DuplicateKeyException duplicateKey(String keyName, Object keyValue) {
        return DuplicateKeyException.of(keyName, keyValue);
    }
    
    public static DuplicateKeyException duplicateKey(String keyName, Object keyValue, String message) {
        return DuplicateKeyException.of(keyName, keyValue, message);
    }
    
    public static DuplicateKeyException duplicateKey(String message) {
        return new DuplicateKeyException(message);
    }
    
    public static DuplicateKeyException duplicateKey(String message, Throwable cause) {
        return new DuplicateKeyException(message, cause);
    }
    
    // ========== 乐观锁异常 ==========
    
    public static OptimisticLockException optimisticLockFailure(Class<?> entityType, Object entityId, 
                                                               Object expectedVersion, Object actualVersion) {
        return OptimisticLockException.of(entityType, entityId, expectedVersion, actualVersion);
    }
    
    public static OptimisticLockException optimisticLockFailure(String message) {
        return new OptimisticLockException(message);
    }
    
    public static OptimisticLockException optimisticLockFailure(String message, Throwable cause) {
        return new OptimisticLockException(message, cause);
    }
    
    // ========== 查询异常 ==========
    
    public static QueryException queryError(String message) {
        return QueryException.of(message);
    }
    
    public static QueryException queryError(String queryType, String queryString, String message) {
        return QueryException.of(queryType, queryString, message);
    }
    
    public static QueryException queryError(String message, Throwable cause) {
        return QueryException.of(message, cause);
    }
    
    // ========== 约束违反异常 ==========
    
    public static DataAccessException constraintViolation(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.CONSTRAINT_VIOLATION, message);
    }
    
    public static DataAccessException constraintViolation(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.CONSTRAINT_VIOLATION, message, cause);
    }
    
    // ========== 事务异常 ==========
    
    public static DataAccessException transactionFailure(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.TRANSACTION_FAILURE, message);
    }
    
    public static DataAccessException transactionFailure(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.TRANSACTION_FAILURE, message, cause);
    }
    
    // ========== 连接异常 ==========
    
    public static DataAccessException connectionFailure(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.CONNECTION_FAILURE, message);
    }
    
    public static DataAccessException connectionFailure(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.CONNECTION_FAILURE, message, cause);
    }
    
    // ========== 查询超时异常 ==========
    
    public static DataAccessException queryTimeout(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.QUERY_TIMEOUT, message);
    }
    
    public static DataAccessException queryTimeout(long timeoutMillis) {
        return new DataAccessException(DataAccessException.ErrorCodes.QUERY_TIMEOUT, 
                                     "Query timed out after " + timeoutMillis + " ms");
    }
    
    public static DataAccessException queryTimeout(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.QUERY_TIMEOUT, message, cause);
    }
    
    // ========== 批量更新异常 ==========
    
    public static DataAccessException batchUpdateFailure(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.BATCH_UPDATE_FAILURE, message);
    }
    
    public static DataAccessException batchUpdateFailure(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.BATCH_UPDATE_FAILURE, message, cause);
    }
    
    public static DataAccessException batchUpdateFailure(int expectedCount, int actualCount) {
        return new DataAccessException(DataAccessException.ErrorCodes.BATCH_UPDATE_FAILURE, 
                                     String.format("Batch update failed: expected %d, actual %d", 
                                                   expectedCount, actualCount));
    }
    
    // ========== 数据完整性违反异常 ==========
    
    public static DataAccessException dataIntegrityViolation(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.DATA_INTEGRITY_VIOLATION, message);
    }
    
    public static DataAccessException dataIntegrityViolation(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.DATA_INTEGRITY_VIOLATION, message, cause);
    }
    
    // ========== 数据格式异常 ==========
    
    public static DataAccessException invalidDataFormat(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.INVALID_DATA_FORMAT, message);
    }
    
    public static DataAccessException invalidDataFormat(String field, Object value, String expectedFormat) {
        return new DataAccessException(DataAccessException.ErrorCodes.INVALID_DATA_FORMAT, 
                                     String.format("Invalid data format for field '%s': value '%s', expected format '%s'", 
                                                   field, value, expectedFormat));
    }
    
    public static DataAccessException invalidDataFormat(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.INVALID_DATA_FORMAT, message, cause);
    }
    
    // ========== Repository异常 ==========
    
    public static DataAccessException repositoryError(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.REPOSITORY_ERROR, message);
    }
    
    public static DataAccessException repositoryError(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.REPOSITORY_ERROR, message, cause);
    }
    
    // ========== QueryBuilder异常 ==========
    
    public static DataAccessException queryBuilderError(String message) {
        return new DataAccessException(DataAccessException.ErrorCodes.QUERY_BUILDER_ERROR, message);
    }
    
    public static DataAccessException queryBuilderError(String message, Throwable cause) {
        return new DataAccessException(DataAccessException.ErrorCodes.QUERY_BUILDER_ERROR, message, cause);
    }
    
    // ========== 通用数据访问异常 ==========
    
    public static DataAccessException general(String message) {
        return new DataAccessException(message);
    }
    
    public static DataAccessException general(String message, Throwable cause) {
        return new DataAccessException(message, cause);
    }
    
    public static DataAccessException general(String errorCode, String message) {
        return new DataAccessException(errorCode, message);
    }
    
    public static DataAccessException general(String errorCode, String message, Throwable cause) {
        return new DataAccessException(errorCode, message, cause);
    }
}
