package io.nebula.storage.core.model;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * 存储操作结果
 */
public class StorageResult {
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 存储桶名称
     */
    private String bucket;
    
    /**
     * 对象键
     */
    private String key;
    
    /**
     * ETag
     */
    private String etag;
    
    /**
     * 版本ID
     */
    private String versionId;
    
    /**
     * 文件输入流（用于下载操作）
     */
    private InputStream inputStream;
    
    /**
     * 对象元数据
     */
    private ObjectMetadata metadata;
    
    /**
     * 操作时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 服务器端加密信息
     */
    private String serverSideEncryption;
    
    /**
     * 文件URL（如果支持）
     */
    private String url;
    
    // 构造函数
    public StorageResult() {
        this.timestamp = LocalDateTime.now();
    }
    
    public StorageResult(boolean success) {
        this();
        this.success = success;
    }
    
    public StorageResult(boolean success, String bucket, String key) {
        this(success);
        this.bucket = bucket;
        this.key = key;
    }
    
    // 静态工厂方法
    public static StorageResult success() {
        return new StorageResult(true);
    }
    
    public static StorageResult success(String bucket, String key) {
        return new StorageResult(true, bucket, key);
    }
    
    public static StorageResult success(String bucket, String key, String etag) {
        StorageResult result = new StorageResult(true, bucket, key);
        result.setEtag(etag);
        return result;
    }
    
    public static StorageResult error(String errorCode, String errorMessage) {
        StorageResult result = new StorageResult(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    public static StorageResult error(String bucket, String key, String errorCode, String errorMessage) {
        StorageResult result = new StorageResult(false, bucket, key);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Builder模式
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private StorageResult result = new StorageResult();
        
        public Builder success(boolean success) {
            result.success = success;
            return this;
        }
        
        public Builder bucket(String bucket) {
            result.bucket = bucket;
            return this;
        }
        
        public Builder key(String key) {
            result.key = key;
            return this;
        }
        
        public Builder etag(String etag) {
            result.etag = etag;
            return this;
        }
        
        public Builder versionId(String versionId) {
            result.versionId = versionId;
            return this;
        }
        
        public Builder inputStream(InputStream inputStream) {
            result.inputStream = inputStream;
            return this;
        }
        
        public Builder metadata(ObjectMetadata metadata) {
            result.metadata = metadata;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            result.errorCode = errorCode;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }
        
        public Builder requestId(String requestId) {
            result.requestId = requestId;
            return this;
        }
        
        public Builder serverSideEncryption(String serverSideEncryption) {
            result.serverSideEncryption = serverSideEncryption;
            return this;
        }
        
        public Builder url(String url) {
            result.url = url;
            return this;
        }
        
        public StorageResult build() {
            return result;
        }
    }
    
    // Getter and Setter methods
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getBucket() {
        return bucket;
    }
    
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getEtag() {
        return etag;
    }
    
    public void setEtag(String etag) {
        this.etag = etag;
    }
    
    public String getVersionId() {
        return versionId;
    }
    
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
    
    public InputStream getInputStream() {
        return inputStream;
    }
    
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    public ObjectMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getServerSideEncryption() {
        return serverSideEncryption;
    }
    
    public void setServerSideEncryption(String serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    @Override
    public String toString() {
        return "StorageResult{" +
                "success=" + success +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", bucket='" + bucket + '\'' +
                ", key='" + key + '\'' +
                ", etag='" + etag + '\'' +
                ", versionId='" + versionId + '\'' +
                ", metadata=" + metadata +
                ", timestamp=" + timestamp +
                ", requestId='" + requestId + '\'' +
                ", serverSideEncryption='" + serverSideEncryption + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
