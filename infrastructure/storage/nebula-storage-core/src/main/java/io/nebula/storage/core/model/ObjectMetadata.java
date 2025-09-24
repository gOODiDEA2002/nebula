package io.nebula.storage.core.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 对象元数据
 */
public class ObjectMetadata {
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 内容长度
     */
    private Long contentLength;
    
    /**
     * 内容编码
     */
    private String contentEncoding;
    
    /**
     * 内容MD5
     */
    private String contentMd5;
    
    /**
     * 缓存控制
     */
    private String cacheControl;
    
    /**
     * 内容处置
     */
    private String contentDisposition;
    
    /**
     * 过期时间
     */
    private LocalDateTime expirationTime;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * ETag
     */
    private String etag;
    
    /**
     * 用户自定义元数据
     */
    private Map<String, String> userMetadata = new HashMap<>();
    
    /**
     * 系统元数据
     */
    private Map<String, Object> systemMetadata = new HashMap<>();
    
    // 构造函数
    public ObjectMetadata() {}
    
    public ObjectMetadata(String contentType, Long contentLength) {
        this.contentType = contentType;
        this.contentLength = contentLength;
    }
    
    // Getter and Setter methods
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public Long getContentLength() {
        return contentLength;
    }
    
    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }
    
    public String getContentEncoding() {
        return contentEncoding;
    }
    
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }
    
    public String getContentMd5() {
        return contentMd5;
    }
    
    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }
    
    public String getCacheControl() {
        return cacheControl;
    }
    
    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }
    
    public String getContentDisposition() {
        return contentDisposition;
    }
    
    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }
    
    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }
    
    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
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
    
    public Map<String, String> getUserMetadata() {
        return userMetadata;
    }
    
    public void setUserMetadata(Map<String, String> userMetadata) {
        this.userMetadata = userMetadata;
    }
    
    public Map<String, Object> getSystemMetadata() {
        return systemMetadata;
    }
    
    public void setSystemMetadata(Map<String, Object> systemMetadata) {
        this.systemMetadata = systemMetadata;
    }
    
    // 便利方法
    
    /**
     * 添加用户元数据
     */
    public void addUserMetadata(String key, String value) {
        if (userMetadata == null) {
            userMetadata = new HashMap<>();
        }
        userMetadata.put(key, value);
    }
    
    /**
     * 获取用户元数据
     */
    public String getUserMetadata(String key) {
        return userMetadata != null ? userMetadata.get(key) : null;
    }
    
    /**
     * 添加系统元数据
     */
    public void addSystemMetadata(String key, Object value) {
        if (systemMetadata == null) {
            systemMetadata = new HashMap<>();
        }
        systemMetadata.put(key, value);
    }
    
    /**
     * 获取系统元数据
     */
    public Object getSystemMetadata(String key) {
        return systemMetadata != null ? systemMetadata.get(key) : null;
    }
    
    /**
     * 复制元数据
     */
    public ObjectMetadata copy() {
        ObjectMetadata copy = new ObjectMetadata();
        copy.contentType = this.contentType;
        copy.contentLength = this.contentLength;
        copy.contentEncoding = this.contentEncoding;
        copy.contentMd5 = this.contentMd5;
        copy.cacheControl = this.cacheControl;
        copy.contentDisposition = this.contentDisposition;
        copy.expirationTime = this.expirationTime;
        copy.lastModified = this.lastModified;
        copy.etag = this.etag;
        
        if (this.userMetadata != null) {
            copy.userMetadata = new HashMap<>(this.userMetadata);
        }
        if (this.systemMetadata != null) {
            copy.systemMetadata = new HashMap<>(this.systemMetadata);
        }
        
        return copy;
    }
    
    @Override
    public String toString() {
        return "ObjectMetadata{" +
                "contentType='" + contentType + '\'' +
                ", contentLength=" + contentLength +
                ", contentEncoding='" + contentEncoding + '\'' +
                ", contentMd5='" + contentMd5 + '\'' +
                ", cacheControl='" + cacheControl + '\'' +
                ", contentDisposition='" + contentDisposition + '\'' +
                ", expirationTime=" + expirationTime +
                ", lastModified=" + lastModified +
                ", etag='" + etag + '\'' +
                ", userMetadata=" + userMetadata +
                ", systemMetadata=" + systemMetadata +
                '}';
    }
}
