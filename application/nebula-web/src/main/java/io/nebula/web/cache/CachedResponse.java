package io.nebula.web.cache;

import java.io.Serializable;
import java.util.Map;

/**
 * 缓存的响应数据
 */
public class CachedResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应状态码
     */
    private int status;
    
    /**
     * 响应头
     */
    private Map<String, String> headers;
    
    /**
     * 响应体
     */
    private byte[] body;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 字符编码
     */
    private String characterEncoding;
    
    /**
     * 创建时间
     */
    private long createdTime;
    
    public CachedResponse() {
        this.createdTime = System.currentTimeMillis();
    }
    
    public CachedResponse(int status, Map<String, String> headers, byte[] body, 
                         String contentType, String characterEncoding) {
        this();
        this.status = status;
        this.headers = headers;
        this.body = body;
        this.contentType = contentType;
        this.characterEncoding = characterEncoding;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public byte[] getBody() {
        return body;
    }
    
    public void setBody(byte[] body) {
        this.body = body;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getCharacterEncoding() {
        return characterEncoding;
    }
    
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }
    
    /**
     * 获取响应大小（字节）
     */
    public int getSize() {
        return body != null ? body.length : 0;
    }
    
    /**
     * 判断是否已过期
     */
    public boolean isExpired(int ttlSeconds) {
        long now = System.currentTimeMillis();
        return (now - createdTime) > (ttlSeconds * 1000L);
    }
    
    @Override
    public String toString() {
        return "CachedResponse{" +
                "status=" + status +
                ", contentType='" + contentType + '\'' +
                ", size=" + getSize() +
                ", createdTime=" + createdTime +
                '}';
    }
}
