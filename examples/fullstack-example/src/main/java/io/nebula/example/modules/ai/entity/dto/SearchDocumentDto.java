package io.nebula.example.modules.ai.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 搜索文档接口DTO
 */
public class SearchDocumentDto {
    
    /**
     * 搜索文档请求
     */
    @Data
    public static class Request {
        
        /** 查询文本 */
        @NotBlank(message = "查询文本不能为空")
        private String query;
        
        /** 返回的最大结果数 */
        @Min(value = 1, message = "topK必须大于0")
        private Integer topK = 5;
        
        /** 相似度阈值（可选，0-1之间） */
        private Double similarityThreshold;
        
        /** 过滤条件（可选） */
        private Map<String, Object> filter;
    }
    
    /**
     * 搜索文档响应
     */
    @Data
    public static class Response {
        
        /** 搜索结果列表 */
        private List<DocumentResult> documents;
        
        /** 查询文本 */
        private String query;
        
        /** 找到的总数 */
        private Integer totalFound;
        
        /** 最高分数 */
        private Double maxScore;
        
        /** 最低分数 */
        private Double minScore;
        
        /** 响应时间戳 */
        private String timestamp;
        
        @Data
        public static class DocumentResult {
            /** 文档ID */
            private String id;
            
            /** 文档内容 */
            private String content;
            
            /** 相似度分数 */
            private Double score;
            
            /** 元数据 */
            private Map<String, Object> metadata;
        }
    }
}

