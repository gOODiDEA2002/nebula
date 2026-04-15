package io.nebula.example.modules.search.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 产品搜索结果 VO
 *
 * @author nebula
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchVo {

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
     * 标签
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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 搜索匹配分数
     */
    private Double score;

    /**
     * 高亮内容
     */
    private Map<String, String[]> highlight;
}

