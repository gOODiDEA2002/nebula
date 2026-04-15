package io.nebula.example.modules.storage.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成预签名URLDTO
 */
public class GenerateUrlDto {
    
    /**
     * 生成预签名URL请求
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
        
        /**
         * 过期时间（秒）
         */
        @Min(value = 60, message = "过期时间不能小于60秒")
        @Max(value = 604800, message = "过期时间不能大于7天")
        private Integer expirySeconds = 3600;  // 默认1小时
    }
    
    /**
     * 生成预签名URL响应
     */
    @Data
    public static class Response {
        /**
         * 预签名URL
         */
        private String url;
        
        /**
         * 过期时间（秒）
         */
        private Integer expirySeconds;
        
        /**
         * 过期时间戳（ISO 8601格式）
         */
        private String expiryTime;
    }
}

