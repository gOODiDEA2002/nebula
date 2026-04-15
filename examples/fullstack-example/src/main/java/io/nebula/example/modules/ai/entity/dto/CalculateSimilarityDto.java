package io.nebula.example.modules.ai.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 计算文本相似度接口DTO
 */
public class CalculateSimilarityDto {
    
    /**
     * 计算文本相似度请求
     */
    @Data
    public static class Request {
        
        /** 第一个文本 */
        @NotBlank(message = "第一个文本不能为空")
        private String text1;
        
        /** 第二个文本 */
        @NotBlank(message = "第二个文本不能为空")
        private String text2;
    }
    
    /**
     * 计算文本相似度响应
     */
    @Data
    public static class Response {
        
        /** 相似度分数（0-1之间，1表示完全相似） */
        private Double similarity;
        
        /** 第一个文本 */
        private String text1;
        
        /** 第二个文本 */
        private String text2;
        
        /** 响应时间戳 */
        private String timestamp;
    }
}

