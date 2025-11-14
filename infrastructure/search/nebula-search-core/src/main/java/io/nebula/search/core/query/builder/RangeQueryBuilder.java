package io.nebula.search.core.query.builder;

/**
 * 范围查询构建器
 * 用于数值、日期范围查询
 * 
 * 使用场景：
 * - 价格区间查询
 * - 日期范围查询
 * - 年龄范围查询
 * 
 * @author nebula
 */
public class RangeQueryBuilder implements QueryBuilder {
    
    /**
     * 字段名
     */
    private final String field;
    
    /**
     * 大于等于（>=）
     */
    private Object gte;
    
    /**
     * 大于（>）
     */
    private Object gt;
    
    /**
     * 小于等于（<=）
     */
    private Object lte;
    
    /**
     * 小于（<）
     */
    private Object lt;
    
    /**
     * 时区（用于日期查询）
     */
    private String timeZone;
    
    /**
     * 日期格式
     */
    private String format;
    
    /**
     * 权重
     */
    private Float boost;
    
    public RangeQueryBuilder(String field) {
        this.field = field;
    }
    
    @Override
    public QueryType getQueryType() {
        return QueryType.RANGE;
    }
    
    // Builder 模式
    
    public RangeQueryBuilder gte(Object gte) {
        this.gte = gte;
        return this;
    }
    
    public RangeQueryBuilder gt(Object gt) {
        this.gt = gt;
        return this;
    }
    
    public RangeQueryBuilder lte(Object lte) {
        this.lte = lte;
        return this;
    }
    
    public RangeQueryBuilder lt(Object lt) {
        this.lt = lt;
        return this;
    }
    
    public RangeQueryBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }
    
    public RangeQueryBuilder format(String format) {
        this.format = format;
        return this;
    }
    
    public RangeQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }
    
    // Getters
    
    public String getField() {
        return field;
    }
    
    public Object getGte() {
        return gte;
    }
    
    public Object getGt() {
        return gt;
    }
    
    public Object getLte() {
        return lte;
    }
    
    public Object getLt() {
        return lt;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
    
    public String getFormat() {
        return format;
    }
    
    public Float getBoost() {
        return boost;
    }
}

