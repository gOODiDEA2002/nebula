package io.nebula.example.modules.storage.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息VO
 */
@Data
public class FileInfoVo {
    
    /**
     * 文件键（对象路径）
     */
    private String key;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 格式化的文件大小
     */
    private String formattedSize;
    
    /**
     * 文件类型
     */
    private String contentType;
    
    /**
     * ETag
     */
    private String etag;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 存储桶名称
     */
    private String bucket;
    
    /**
     * 文件扩展名
     */
    private String extension;
    
    /**
     * 文件URL（如果有）
     */
    private String url;
}

