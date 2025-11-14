package io.nebula.search.core.aggregation;

/**
 * 日期直方图聚合
 * 按时间区间分组
 * 
 * 使用场景：
 * - 按天/周/月统计订单数量
 * - 时间序列数据分析
 * - 趋势分析
 * 
 * @author nebula
 */
public class DateHistogramAggregation extends AbstractAggregation {
    
    /**
     * 日历间隔：year, month, week, day, hour, minute, second
     */
    private String calendarInterval;
    
    /**
     * 固定间隔（如：1h, 30m, 1d）
     */
    private String fixedInterval;
    
    /**
     * 时区
     */
    private String timeZone;
    
    /**
     * 格式化模式
     */
    private String format;
    
    /**
     * 最小文档数阈值
     */
    private Integer minDocCount;
    
    /**
     * 扩展边界 - 最小值
     */
    private String extendedBoundsMin;
    
    /**
     * 扩展边界 - 最大值
     */
    private String extendedBoundsMax;
    
    public DateHistogramAggregation(String name, String field) {
        super(name, field);
        this.minDocCount = 0;
    }
    
    @Override
    public AggregationType getType() {
        return AggregationType.DATE_HISTOGRAM;
    }
    
    // Builder 模式
    
    public DateHistogramAggregation calendarInterval(String calendarInterval) {
        this.calendarInterval = calendarInterval;
        return this;
    }
    
    public DateHistogramAggregation fixedInterval(String fixedInterval) {
        this.fixedInterval = fixedInterval;
        return this;
    }
    
    public DateHistogramAggregation timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }
    
    public DateHistogramAggregation format(String format) {
        this.format = format;
        return this;
    }
    
    public DateHistogramAggregation minDocCount(int minDocCount) {
        this.minDocCount = minDocCount;
        return this;
    }
    
    public DateHistogramAggregation extendedBounds(String min, String max) {
        this.extendedBoundsMin = min;
        this.extendedBoundsMax = max;
        return this;
    }
    
    // Getters
    
    public String getCalendarInterval() {
        return calendarInterval;
    }
    
    public String getFixedInterval() {
        return fixedInterval;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
    
    public String getFormat() {
        return format;
    }
    
    public Integer getMinDocCount() {
        return minDocCount;
    }
    
    public String getExtendedBoundsMin() {
        return extendedBoundsMin;
    }
    
    public String getExtendedBoundsMax() {
        return extendedBoundsMax;
    }
}

