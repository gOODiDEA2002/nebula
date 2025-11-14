package io.nebula.search.core.query.builder;

/**
 * 全文匹配查询构建器
 * 用于全文搜索，支持分词和相关性评分
 * 
 * 使用场景：
 * - 搜索文本内容
 * - 模糊匹配
 * 
 * @author nebula
 */
public class MatchQueryBuilder implements QueryBuilder {
    
    /**
     * 字段名
     */
    private final String field;
    
    /**
     * 查询值
     */
    private final Object query;
    
    /**
     * 操作符（AND/OR）
     */
    private String operator;
    
    /**
     * 最小匹配度
     */
    private String minimumShouldMatch;
    
    /**
     * 模糊度（AUTO, 0, 1, 2）
     */
    private String fuzziness;
    
    /**
     * 前缀长度
     */
    private Integer prefixLength;
    
    /**
     * 最大扩展数
     */
    private Integer maxExpansions;
    
    public MatchQueryBuilder(String field, Object query) {
        this.field = field;
        this.query = query;
    }
    
    @Override
    public QueryType getQueryType() {
        return QueryType.MATCH;
    }
    
    // Builder 模式
    
    public MatchQueryBuilder operator(String operator) {
        this.operator = operator;
        return this;
    }
    
    public MatchQueryBuilder minimumShouldMatch(String minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }
    
    public MatchQueryBuilder fuzziness(String fuzziness) {
        this.fuzziness = fuzziness;
        return this;
    }
    
    public MatchQueryBuilder prefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
        return this;
    }
    
    public MatchQueryBuilder maxExpansions(int maxExpansions) {
        this.maxExpansions = maxExpansions;
        return this;
    }
    
    // Getters
    
    public String getField() {
        return field;
    }
    
    public Object getQuery() {
        return query;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public String getMinimumShouldMatch() {
        return minimumShouldMatch;
    }
    
    public String getFuzziness() {
        return fuzziness;
    }
    
    public Integer getPrefixLength() {
        return prefixLength;
    }
    
    public Integer getMaxExpansions() {
        return maxExpansions;
    }
}

