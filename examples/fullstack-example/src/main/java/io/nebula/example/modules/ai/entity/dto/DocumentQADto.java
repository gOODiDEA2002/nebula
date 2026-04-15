package io.nebula.example.modules.ai.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 文档问答（RAG）接口DTO
 */
public class DocumentQADto {
    
    /**
     * 文档问答请求
     */
    @Data
    public static class Request {
        
        /** 用户问题 */
        @NotBlank(message = "问题不能为空")
        private String question;
        
        /** 检索的相关文档数量 */
        @Min(value = 1, message = "contextSize必须大于0")
        private Integer contextSize = 3;
        
        /** 相似度阈值（可选） */
        private Double similarityThreshold;
        
        /** 温度参数（可选） */
        private Double temperature;
    }
    
    /**
     * 文档问答响应
     */
    @Data
    public static class Response {
        
        /** AI回答 */
        private String answer;
        
        /** 用户问题 */
        private String question;
        
        /** 相关文档上下文 */
        private List<ContextDocument> contextDocuments;
        
        /** 使用的模型 */
        private String model;
        
        /** token使用统计 */
        private TokenUsage usage;
        
        /** 响应时间戳 */
        private String timestamp;
        
        @Data
        public static class ContextDocument {
            /** 文档ID */
            private String id;
            
            /** 文档内容片段 */
            private String content;
            
            /** 相似度分数 */
            private Double score;
        }
        
        @Data
        public static class TokenUsage {
            /** 提示词tokens */
            private Integer promptTokens;
            
            /** 完成tokens */
            private Integer completionTokens;
            
            /** 总tokens */
            private Integer totalTokens;
        }
    }
}

