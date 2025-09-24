package io.nebula.storage.core.model;

import java.time.LocalDateTime;

/**
 * 对象摘要信息
 * 用于列表操作返回的简化对象信息
 */
public class ObjectSummary {
    
    /**
     * 存储桶名称
     */
    private String bucket;
    
    /**
     * 对象键
     */
    private String key;
    
    /**
     * 对象大小（字节）
     */
    private Long size;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * ETag
     */
    private String etag;
    
    /**
     * 存储类型
     */
    private String storageClass;
    
    /**
     * 拥有者信息
     */
    private String owner;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 版本ID
     */
    private String versionId;
    
    /**
     * 是否为目录
     */
    private boolean directory;
    
    // 构造函数
    public ObjectSummary() {}
    
    public ObjectSummary(String bucket, String key) {
        this.bucket = bucket;
        this.key = key;
    }
    
    public ObjectSummary(String bucket, String key, Long size, LocalDateTime lastModified) {
        this.bucket = bucket;
        this.key = key;
        this.size = size;
        this.lastModified = lastModified;
    }
    
    // Builder模式
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ObjectSummary summary = new ObjectSummary();
        
        public Builder bucket(String bucket) {
            summary.bucket = bucket;
            return this;
        }
        
        public Builder key(String key) {
            summary.key = key;
            return this;
        }
        
        public Builder size(Long size) {
            summary.size = size;
            return this;
        }
        
        public Builder lastModified(LocalDateTime lastModified) {
            summary.lastModified = lastModified;
            return this;
        }
        
        public Builder etag(String etag) {
            summary.etag = etag;
            return this;
        }
        
        public Builder storageClass(String storageClass) {
            summary.storageClass = storageClass;
            return this;
        }
        
        public Builder owner(String owner) {
            summary.owner = owner;
            return this;
        }
        
        public Builder contentType(String contentType) {
            summary.contentType = contentType;
            return this;
        }
        
        public Builder versionId(String versionId) {
            summary.versionId = versionId;
            return this;
        }
        
        public Builder directory(boolean directory) {
            summary.directory = directory;
            return this;
        }
        
        public ObjectSummary build() {
            return summary;
        }
    }
    
    // Getter and Setter methods
    
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
    
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    public String getEtag() {
        return etag;
    }
    
    public void setEtag(String etag) {
        this.etag = etag;
    }
    
    public String getStorageClass() {
        return storageClass;
    }
    
    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getVersionId() {
        return versionId;
    }
    
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
    
    public boolean isDirectory() {
        return directory;
    }
    
    public void setDirectory(boolean directory) {
        this.directory = directory;
    }
    
    /**
     * 获取文件名（从key中提取）
     */
    public String getFileName() {
        if (key == null) {
            return null;
        }
        int lastSlash = key.lastIndexOf('/');
        return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
    }
    
    /**
     * 获取文件扩展名
     */
    public String getFileExtension() {
        String fileName = getFileName();
        if (fileName == null) {
            return null;
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : null;
    }
    
    /**
     * 获取文件路径（不包含文件名）
     */
    public String getFilePath() {
        if (key == null) {
            return null;
        }
        int lastSlash = key.lastIndexOf('/');
        return lastSlash > 0 ? key.substring(0, lastSlash) : "";
    }
    
    /**
     * 格式化文件大小
     */
    public String getFormattedSize() {
        if (size == null) {
            return "0 B";
        }
        
        long bytes = size;
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        int unit = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        
        return String.format("%.1f %s", 
                bytes / Math.pow(1024, unit), 
                units[unit]);
    }
    
    @Override
    public String toString() {
        return "ObjectSummary{" +
                "bucket='" + bucket + '\'' +
                ", key='" + key + '\'' +
                ", size=" + size +
                ", lastModified=" + lastModified +
                ", etag='" + etag + '\'' +
                ", storageClass='" + storageClass + '\'' +
                ", owner='" + owner + '\'' +
                ", contentType='" + contentType + '\'' +
                ", versionId='" + versionId + '\'' +
                ", directory=" + directory +
                '}';
    }
}
