package io.nebula.search.core.aggregation;

/**
 * 指标聚合基类
 * 用于计算数值型字段的统计指标（avg, sum, min, max 等）
 * 
 * @author nebula
 */
public class MetricAggregation extends AbstractAggregation {
    
    /**
     * 指标类型
     */
    private final AggregationType metricType;
    
    /**
     * 缺失值处理
     */
    private Object missing;
    
    public MetricAggregation(String name, String field, AggregationType metricType) {
        super(name, field);
        this.metricType = metricType;
    }
    
    @Override
    public AggregationType getType() {
        return metricType;
    }
    
    /**
     * 设置缺失值的默认值
     * 
     * @param missing 默认值
     * @return 当前对象
     */
    public MetricAggregation missing(Object missing) {
        this.missing = missing;
        return this;
    }
    
    public Object getMissing() {
        return missing;
    }
    
    // 静态工厂方法
    
    /**
     * 创建平均值聚合
     */
    public static MetricAggregation avg(String name, String field) {
        return new MetricAggregation(name, field, AggregationType.AVG);
    }
    
    /**
     * 创建求和聚合
     */
    public static MetricAggregation sum(String name, String field) {
        return new MetricAggregation(name, field, AggregationType.SUM);
    }
    
    /**
     * 创建最小值聚合
     */
    public static MetricAggregation min(String name, String field) {
        return new MetricAggregation(name, field, AggregationType.MIN);
    }
    
    /**
     * 创建最大值聚合
     */
    public static MetricAggregation max(String name, String field) {
        return new MetricAggregation(name, field, AggregationType.MAX);
    }
    
    /**
     * 创建统计聚合（包含 count, min, max, avg, sum）
     */
    public static MetricAggregation stats(String name, String field) {
        return new MetricAggregation(name, field, AggregationType.STATS);
    }
    
    /**
     * 创建基数聚合（唯一值数量）
     */
    public static MetricAggregation cardinality(String name, String field) {
        return new MetricAggregation(name, field, AggregationType.CARDINALITY);
    }
}

