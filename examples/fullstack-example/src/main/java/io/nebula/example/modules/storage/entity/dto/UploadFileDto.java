package io.nebula.example.modules.storage.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 上传文件DTO
 */
public class UploadFileDto {
    
    /**
     * 上传文件请求
     */
    @Data
    public static class Request {
        /**
         * 存储桶名称
         */
        @NotBlank(message = "存储桶名称不能为空")
        private String bucket;
        
        /**
         * 文件分类（用于生成路径）
         */
        private String category = "uploads";
        
        /**
         * 文件
         */
        @NotNull(message = "文件不能为空")
        private MultipartFile file;
        
        /**
         * 自定义元数据
         */
        private Map<String, String> metadata;
    }
    
    /**
     * 上传文件响应
     */
    @Data
    public static class Response {
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
         * ETag
         */
        private String etag;
        
        /**
         * 存储桶名称
         */
        private String bucket;
        
        /**
         * 上传时间
         */
        private String uploadTime;
    }
}

