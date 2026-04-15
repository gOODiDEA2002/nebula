package io.nebula.example.modules.search.entity.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 搜索产品DTO
 *
 * @author nebula
 */
@Data
public class SearchProductsDto {

    /**
     * 搜索产品请求
     */
    @Data
    public static class Request {
        /**
         * 关键词搜索(名称、描述)
         */
        private String keyword;
        
        /**
         * 分类筛选
         */
        private String category;
        
        /**
         * 状态筛选
         */
        private String status;
        
        /**
         * 最低价格
         */
        private BigDecimal minPrice;
        
        /**
         * 最高价格
         */
        private BigDecimal maxPrice;
        
        /**
         * 品牌筛选
         */
        private String brand;
        
        /**
         * 标签筛选
         */
        private List<String> tags;
        
        /**
         * 最低评分
         */
        private Double minRating;
        
        /**
         * 最高评分
         */
        private Double maxRating;
        
        /**
         * 最低销量
         */
        private Integer minSalesCount;
        
        /**
         * 最高销量
         */
        private Integer maxSalesCount;
        
        /**
         * 页码
         */
        @Min(value = 1, message = "页码不能小于1")
        private Integer page = 1;
        
        /**
         * 每页大小
         */
        @Min(value = 1, message = "每页大小不能小于1")
        private Integer size = 10;
        
        /**
         * 排序字段
         */
        private List<String> sortFields = List.of("_score");
        
        /**
         * 排序顺序(asc/desc)
         */
        private String sortOrder = "desc";
        
        /**
         * 是否高亮
         */
        private Boolean highlight = false;
    }

    /**
     * 搜索产品响应
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
         * 总命中数
         */
        private Long totalHits;
        
        /**
         * 最大得分
         */
        private Double maxScore;
        
        /**
         * 产品列表
         */
        private List<ProductSearchResult> products;
        
        /**
         * 聚合结果
         */
        private Map<String, Object> aggregations;
        
        /**
         * 查询耗时(毫秒)
         */
        private Long took;
        
        /**
         * 是否超时
         */
        private Boolean timedOut;
    }

    /**
     * 产品搜索结果
     */
    @Data
    public static class ProductSearchResult {
        /**
         * 文档ID
         */
        private String id;
        
        /**
         * 产品名称
         */
        private String name;
        
        /**
         * 产品描述
         */
        private String description;
        
        /**
         * 分类
         */
        private String category;
        
        /**
         * 价格
         */
        private BigDecimal price;
        
        /**
         * 库存数量
         */
        private Integer stockQuantity;
        
        /**
         * 状态
         */
        private String status;
        
        /**
         * 品牌
         */
        private String brand;
        
        /**
         * 评分
         */
        private Double rating;
        
        /**
         * 销量
         */
        private Integer salesCount;
        
        /**
         * 相关性得分
         */
        private Double score;
        
        /**
         * 高亮字段
         */
        private Map<String, String[]> highlight;
    }
}

