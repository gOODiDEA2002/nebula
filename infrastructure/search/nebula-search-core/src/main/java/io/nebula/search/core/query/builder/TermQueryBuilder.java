package io.nebula.search.core.query.builder;

/**
 * 精确匹配查询构建器
 * 用于精确值匹配，不进行分词
 * 
 * 使用场景：
 * - 精确匹配 ID、状态码、标签等
 * - 枚举值查询
 * 
 * @author nebula
 */
public class TermQueryBuilder implements QueryBuilder {
    
    /**
     * 字段名
     */
    private final String field;
    
    /**
     * 查询值
     */
    private final Object value;
    
    /**
     * 权重
     */
    private Float boost;
    
    public TermQueryBuilder(String field, Object value) {
        this.field = field;
        this.value = value;
    }
    
    @Override
    public QueryType getQueryType() {
        return QueryType.TERM;
    }
    
    // Builder 模式
    
    public TermQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }
    
    // Getters
    
    public String getField() {
        return field;
    }
    
    public Object getValue() {
        return value;
    }
    
    public Float getBoost() {
        return boost;
    }
}

