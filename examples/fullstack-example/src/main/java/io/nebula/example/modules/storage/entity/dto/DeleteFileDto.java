package io.nebula.example.modules.storage.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 删除文件DTO
 */
public class DeleteFileDto {
    
    /**
     * 删除文件请求
     */
    @Data
    public static class Request {
        /**
         * 存储桶名称
         */
        @NotBlank(message = "存储桶名称不能为空")
        private String bucket;
        
        /**
         * 文件键列表（对象路径）
         */
        @NotEmpty(message = "文件键列表不能为空")
        private List<String> keys;
    }
    
    /**
     * 删除文件响应
     */
    @Data
    public static class Response {
        /**
         * 成功删除的文件数量
         */
        private Integer deletedCount;
        
        /**
         * 失败的文件键列表
         */
        private List<String> failedKeys;
    }
}

