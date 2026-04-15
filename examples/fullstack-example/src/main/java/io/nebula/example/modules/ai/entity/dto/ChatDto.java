package io.nebula.example.modules.ai.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能聊天接口DTO
 */
public class ChatDto {
    
    /**
     * 智能聊天请求
     */
    @Data
    public static class Request {
        
        /** 用户消息 */
        @NotBlank(message = "消息内容不能为空")
        private String message;
        
        /** 模型名称（可选） */
        private String model;
        
        /** 温度参数（可选，0.0-2.0） */
        private Double temperature;
        
        /** 最大token数（可选） */
        private Integer maxTokens;
        
        /** 是否流式输出（可选，默认false） */
        private Boolean stream = false;
    }
    
    /**
     * 智能聊天响应
     */
    @Data
    public static class Response {
        
        /** 响应ID */
        private String id;
        
        /** AI回复内容 */
        private String content;
        
        /** 使用的模型 */
        private String model;
        
        /** token使用统计 */
        private TokenUsage usage;
        
        /** 完成原因 */
        private String finishReason;
        
        /** 响应时间戳 */
        private String timestamp;
        
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

