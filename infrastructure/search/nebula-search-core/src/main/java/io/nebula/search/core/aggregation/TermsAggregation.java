package io.nebula.search.core.aggregation;

/**
 * 词项聚合
 * 按字段值分组，统计每个值的文档数量
 * 
 * 使用场景：
 * - 按类别统计商品数量
 * - 按标签统计文章数量
 * - 按用户统计行为数量
 * 
 * @author nebula
 */
public class TermsAggregation extends AbstractAggregation {
    
    /**
     * 返回桶数量，默认10
     */
    private Integer size;
    
    /**
     * 排序方式：_count（按文档数）, _key（按词项值）
     */
    private String order;
    
    /**
     * 最小文档数阈值
     */
    private Integer minDocCount;
    
    /**
     * 包含的词项模式（正则表达式）
     */
    private String include;
    
    /**
     * 排除的词项模式（正则表达式）
     */
    private String exclude;
    
    public TermsAggregation(String name, String field) {
        super(name, field);
        this.size = 10;
        this.minDocCount = 1;
    }
    
    @Override
    public AggregationType getType() {
        return AggregationType.TERMS;
    }
    
    // Builder 模式
    
    public TermsAggregation size(int size) {
        this.size = size;
        return this;
    }
    
    public TermsAggregation order(String order) {
        this.order = order;
        return this;
    }
    
    public TermsAggregation minDocCount(int minDocCount) {
        this.minDocCount = minDocCount;
        return this;
    }
    
    public TermsAggregation include(String include) {
        this.include = include;
        return this;
    }
    
    public TermsAggregation exclude(String exclude) {
        this.exclude = exclude;
        return this;
    }
    
    // Getters
    
    public Integer getSize() {
        return size;
    }
    
    public String getOrder() {
        return order;
    }
    
    public Integer getMinDocCount() {
        return minDocCount;
    }
    
    public String getInclude() {
        return include;
    }
    
    public String getExclude() {
        return exclude;
    }
}

