package io.nebula.example.modules.search.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 搜索建议DTO
 *
 * @author nebula
 */
@Data
public class SuggestProductsDto {

    /**
     * 搜索建议请求
     */
    @Data
    public static class Request {
        /**
         * 搜索文本
         */
        @NotBlank(message = "搜索文本不能为空")
        private String text;
        
        /**
         * 建议数量
         */
        @Min(value = 1, message = "建议数量不能小于1")
        private Integer size = 5;
    }

    /**
     * 搜索建议响应
     */
    @Data
    public static class Response {
        /**
         * 是否成功
         */
        private Boolean success;
        
        /**
         * 错误信息
         */
        private String errorMessage;
        
        /**
         * 建议列表
         */
        private List<Suggestion> suggestions;
    }

    /**
     * 单个建议
     */
    @Data
    public static class Suggestion {
        /**
         * 建议文本
         */
        private String text;
        
        /**
         * 得分
         */
        private Double score;
    }
}

