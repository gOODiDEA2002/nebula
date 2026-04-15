package io.nebula.example.modules.storage.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 下载文件DTO
 */
public class DownloadFileDto {
    
    /**
     * 下载文件请求
     */
    @Data
    public static class Request {
        /**
         * 存储桶名称
         */
        @NotBlank(message = "存储桶名称不能为空")
        private String bucket;
        
        /**
         * 文件键（对象路径）
         */
        @NotBlank(message = "文件键不能为空")
        private String key;
    }
    
    /**
     * 下载文件响应
     * 注意：实际文件内容通过流返回，此响应用于元数据
     */
    @Data
    public static class Response {
        /**
         * 文件名
         */
        private String fileName;
        
        /**
         * 文件大小（字节）
         */
        private Long fileSize;
        
        /**
         * 内容类型
         */
        private String contentType;
    }
}

