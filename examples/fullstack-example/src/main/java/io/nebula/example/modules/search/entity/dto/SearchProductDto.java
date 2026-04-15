package io.nebula.example.modules.search.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 搜索产品 DTO
 *
 * @author nebula
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchProductDto {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 产品分类
     */
    private String category;

    /**
     * 最低价格
     */
    @Min(value = 0, message = "最低价格不能小于0")
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    @Min(value = 0, message = "最高价格不能小于0")
    private BigDecimal maxPrice;

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
    private String tag;

    /**
     * 最低评分
     */
    @Min(value = 0, message = "最低评分不能小于0")
    @Max(value = 5, message = "最高评分不能大于5")
    private Double minRating;

    /**
     * 排序字段
     */
    private String sortField = "createTime";

    /**
     * 排序方向 (asc/desc)
     */
    private String sortOrder = "desc";

    /**
     * 当前页码
     */
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能大于100")
    private Integer size = 20;

    /**
     * 是否高亮
     */
    private Boolean highlight = true;
}

