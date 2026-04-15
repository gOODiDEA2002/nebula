package io.nebula.example.modules.ai.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 添加文档接口DTO
 */
public class AddDocumentDto {
    
    /**
     * 添加文档请求
     */
    @Data
    public static class Request {
        
        /** 文档内容 */
        @NotBlank(message = "文档内容不能为空")
        private String content;
        
        /** 文档元数据（可选） */
        private Map<String, Object> metadata;
    }
    
    /**
     * 添加文档响应
     */
    @Data
    public static class Response {
        
        /** 文档ID */
        private String documentId;
        
        /** 是否添加成功 */
        private Boolean success;
        
        /** 响应时间戳 */
        private String timestamp;
    }
}

