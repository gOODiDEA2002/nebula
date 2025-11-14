package io.nebula.search.core.aggregation;

/**
 * 直方图聚合
 * 按数值区间分组
 * 
 * 使用场景：
 * - 价格区间分布统计
 * - 评分区间分布
 * - 数值范围分析
 * 
 * @author nebula
 */
public class HistogramAggregation extends AbstractAggregation {
    
    /**
     * 区间间隔
     */
    private Double interval;
    
    /**
     * 最小文档数阈值
     */
    private Integer minDocCount;
    
    /**
     * 扩展边界 - 最小值
     */
    private Double extendedBoundsMin;
    
    /**
     * 扩展边界 - 最大值
     */
    private Double extendedBoundsMax;
    
    /**
     * 偏移量
     */
    private Double offset;
    
    public HistogramAggregation(String name, String field) {
        super(name, field);
        this.minDocCount = 0;
    }
    
    @Override
    public AggregationType getType() {
        return AggregationType.HISTOGRAM;
    }
    
    // Builder 模式
    
    public HistogramAggregation interval(double interval) {
        this.interval = interval;
        return this;
    }
    
    public HistogramAggregation minDocCount(int minDocCount) {
        this.minDocCount = minDocCount;
        return this;
    }
    
    public HistogramAggregation extendedBounds(double min, double max) {
        this.extendedBoundsMin = min;
        this.extendedBoundsMax = max;
        return this;
    }
    
    public HistogramAggregation offset(double offset) {
        this.offset = offset;
        return this;
    }
    
    // Getters
    
    public Double getInterval() {
        return interval;
    }
    
    public Integer getMinDocCount() {
        return minDocCount;
    }
    
    public Double getExtendedBoundsMin() {
        return extendedBoundsMin;
    }
    
    public Double getExtendedBoundsMax() {
        return extendedBoundsMax;
    }
    
    public Double getOffset() {
        return offset;
    }
}

