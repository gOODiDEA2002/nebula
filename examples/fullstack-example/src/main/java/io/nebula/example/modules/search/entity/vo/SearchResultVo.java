package io.nebula.example.modules.search.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索结果 VO
 *
 * @author nebula
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultVo {

    /**
     * 总命中数
     */
    private Long totalHits;

    /**
     * 最大分数
     */
    private Double maxScore;

    /**
     * 搜索耗时（毫秒）
     */
    private Long took;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 搜索结果列表
     */
    private List<ProductSearchVo> products;
}

