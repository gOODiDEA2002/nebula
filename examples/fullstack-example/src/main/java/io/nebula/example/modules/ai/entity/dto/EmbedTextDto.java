package io.nebula.example.modules.ai.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 文本嵌入接口DTO
 */
public class EmbedTextDto {
    
    /**
     * 文本嵌入请求
     */
    @Data
    public static class Request {
        
        /** 待向量化的文本列表 */
        @NotEmpty(message = "文本列表不能为空")
        private List<String> texts;
        
        /** 模型名称（可选） */
        private String model;
    }
    
    /**
     * 文本嵌入响应
     */
    @Data
    public static class Response {
        
        /** 响应ID */
        private String id;
        
        /** 嵌入结果列表 */
        private List<EmbeddingResult> embeddings;
        
        /** 使用的模型 */
        private String model;
        
        /** 向量维度 */
        private Integer dimension;
        
        /** token使用统计 */
        private TokenUsage usage;
        
        /** 响应时间戳 */
        private String timestamp;
        
        @Data
        public static class EmbeddingResult {
            /** 索引 */
            private Integer index;
            
            /** 向量 */
            private List<Double> vector;
            
            /** 原始文本 */
            private String text;
        }
        
        @Data
        public static class TokenUsage {
            /** 提示词tokens */
            private Integer promptTokens;
            
            /** 总tokens */
            private Integer totalTokens;
        }
    }
}

