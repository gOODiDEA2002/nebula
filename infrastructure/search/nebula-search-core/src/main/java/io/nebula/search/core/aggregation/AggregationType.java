package io.nebula.search.core.aggregation;

/**
 * 聚合类型枚举
 * 
 * @author nebula
 */
public enum AggregationType {
    
    /**
     * 词项聚合 - 按字段值分组
     */
    TERMS,
    
    /**
     * 直方图聚合 - 数值区间分组
     */
    HISTOGRAM,
    
    /**
     * 日期直方图聚合 - 时间区间分组
     */
    DATE_HISTOGRAM,
    
    /**
     * 平均值聚合
     */
    AVG,
    
    /**
     * 求和聚合
     */
    SUM,
    
    /**
     * 最小值聚合
     */
    MIN,
    
    /**
     * 最大值聚合
     */
    MAX,
    
    /**
     * 统计聚合 - 包含 count, min, max, avg, sum
     */
    STATS,
    
    /**
     * 基数聚合 - 唯一值数量
     */
    CARDINALITY,
    
    /**
     * 百分位聚合
     */
    PERCENTILES,
    
    /**
     * 范围聚合
     */
    RANGE,
    
    /**
     * 嵌套聚合
     */
    NESTED
}

