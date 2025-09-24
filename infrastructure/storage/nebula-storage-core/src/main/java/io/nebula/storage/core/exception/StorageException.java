package io.nebula.storage.core.exception;

import io.nebula.core.common.exception.NebulaException;

/**
 * 存储异常基类
 * 所有存储相关异常的基类
 */
public class StorageException extends NebulaException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 存储错误代码
     */
    public static final String STORAGE_ERROR = "STORAGE_ERROR";
    public static final String BUCKET_NOT_FOUND = "BUCKET_NOT_FOUND";
    public static final String OBJECT_NOT_FOUND = "OBJECT_NOT_FOUND";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String INVALID_BUCKET_NAME = "INVALID_BUCKET_NAME";
    public static final String INVALID_OBJECT_KEY = "INVALID_OBJECT_KEY";
    public static final String STORAGE_QUOTA_EXCEEDED = "STORAGE_QUOTA_EXCEEDED";
    public static final String NETWORK_ERROR = "NETWORK_ERROR";
    public static final String AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR";
    public static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";
    public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
    public static final String DOWNLOAD_FAILED = "DOWNLOAD_FAILED";
    public static final String DELETE_FAILED = "DELETE_FAILED";
    
    private final String bucket;
    private final String key;
    
    public StorageException(String message) {
        super(STORAGE_ERROR, message);
        this.bucket = null;
        this.key = null;
    }
    
    public StorageException(String message, Throwable cause) {
        super(STORAGE_ERROR, message, cause);
        this.bucket = null;
        this.key = null;
    }
    
    public StorageException(String errorCode, String message) {
        super(errorCode, message);
        this.bucket = null;
        this.key = null;
    }
    
    public StorageException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.bucket = null;
        this.key = null;
    }
    
    public StorageException(String bucket, String key, String errorCode, String message) {
        super(errorCode, message);
        this.bucket = bucket;
        this.key = key;
    }
    
    public StorageException(String bucket, String key, String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.bucket = bucket;
        this.key = key;
    }
    
    /**
     * 获取存储桶名称
     * 
     * @return 存储桶名称
     */
    public String getBucket() {
        return bucket;
    }
    
    /**
     * 获取对象键
     * 
     * @return 对象键
     */
    public String getKey() {
        return key;
    }
    
    /**
     * 工厂方法：创建桶不存在异常
     */
    public static StorageException bucketNotFound(String bucket) {
        return new StorageException(bucket, null, BUCKET_NOT_FOUND, 
                String.format("Bucket '%s' not found", bucket));
    }
    
    /**
     * 工厂方法：创建对象不存在异常
     */
    public static StorageException objectNotFound(String bucket, String key) {
        return new StorageException(bucket, key, OBJECT_NOT_FOUND, 
                String.format("Object '%s' not found in bucket '%s'", key, bucket));
    }
    
    /**
     * 工厂方法：创建访问拒绝异常
     */
    public static StorageException accessDenied(String bucket, String key) {
        return new StorageException(bucket, key, ACCESS_DENIED, 
                String.format("Access denied for object '%s' in bucket '%s'", key, bucket));
    }
    
    /**
     * 工厂方法：创建无效桶名异常
     */
    public static StorageException invalidBucketName(String bucket) {
        return new StorageException(bucket, null, INVALID_BUCKET_NAME, 
                String.format("Invalid bucket name: '%s'", bucket));
    }
    
    /**
     * 工厂方法：创建无效对象键异常
     */
    public static StorageException invalidObjectKey(String bucket, String key) {
        return new StorageException(bucket, key, INVALID_OBJECT_KEY, 
                String.format("Invalid object key: '%s' in bucket '%s'", key, bucket));
    }
    
    /**
     * 工厂方法：创建存储配额超限异常
     */
    public static StorageException quotaExceeded(String bucket) {
        return new StorageException(bucket, null, STORAGE_QUOTA_EXCEEDED, 
                String.format("Storage quota exceeded for bucket '%s'", bucket));
    }
    
    /**
     * 工厂方法：创建网络错误异常
     */
    public static StorageException networkError(String message, Throwable cause) {
        return new StorageException(NETWORK_ERROR, "Network error: " + message, cause);
    }
    
    /**
     * 工厂方法：创建认证错误异常
     */
    public static StorageException authenticationError(String message) {
        return new StorageException(AUTHENTICATION_ERROR, "Authentication error: " + message);
    }
    
    /**
     * 工厂方法：创建配置错误异常
     */
    public static StorageException configurationError(String message) {
        return new StorageException(CONFIGURATION_ERROR, "Configuration error: " + message);
    }
    
    /**
     * 工厂方法：创建上传失败异常
     */
    public static StorageException uploadFailed(String bucket, String key, String message, Throwable cause) {
        return new StorageException(bucket, key, UPLOAD_FAILED, 
                String.format("Upload failed for object '%s' in bucket '%s': %s", key, bucket, message), cause);
    }
    
    /**
     * 工厂方法：创建下载失败异常
     */
    public static StorageException downloadFailed(String bucket, String key, String message, Throwable cause) {
        return new StorageException(bucket, key, DOWNLOAD_FAILED, 
                String.format("Download failed for object '%s' in bucket '%s': %s", key, bucket, message), cause);
    }
    
    /**
     * 工厂方法：创建删除失败异常
     */
    public static StorageException deleteFailed(String bucket, String key, String message, Throwable cause) {
        return new StorageException(bucket, key, DELETE_FAILED, 
                String.format("Delete failed for object '%s' in bucket '%s': %s", key, bucket, message), cause);
    }
}
