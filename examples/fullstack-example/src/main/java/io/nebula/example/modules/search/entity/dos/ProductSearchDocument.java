package io.nebula.example.modules.search.entity.dos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 产品搜索文档
 * 用于 Elasticsearch 索引的产品数据模型
 *
 * @author nebula
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDocument {

    /**
     * 产品ID
     */
    private Long id;

    /**
     * 产品名称
     */
    private String name;

    /**
     * 产品描述
     */
    private String description;

    /**
     * 产品分类
     */
    private String category;

    /**
     * 产品价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stockQuantity;

    /**
     * 产品状态
     */
    private String status;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 标签（用于搜索和过滤）
     */
    private String[] tags;

    /**
     * 评分
     */
    private Double rating;

    /**
     * 销量
     */
    private Integer salesCount;

    /**
     * 创建时间(字符串格式:yyyy-MM-dd HH:mm:ss)
     */
    private String createTime;

    /**
     * 更新时间(字符串格式:yyyy-MM-dd HH:mm:ss)
     */
    private String updateTime;

    /**
     * 搜索建议字段（用于自动补全）
     */
    private SuggestField suggest;

    /**
     * 搜索建议字段结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestField {
        /**
         * 输入文本
         */
        private String[] input;

        /**
         * 权重
         */
        private Integer weight;
    }
}

