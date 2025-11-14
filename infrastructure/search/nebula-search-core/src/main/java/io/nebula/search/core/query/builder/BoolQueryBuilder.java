package io.nebula.search.core.query.builder;

import java.util.ArrayList;
import java.util.List;

/**
 * 布尔组合查询构建器
 * 用于组合多个查询条件
 * 
 * 使用场景：
 * - 复杂的组合查询
 * - 多条件过滤
 * 
 * @author nebula
 */
public class BoolQueryBuilder implements QueryBuilder {
    
    /**
     * must 子句：必须匹配，参与评分
     */
    private List<QueryBuilder> must;
    
    /**
     * filter 子句：必须匹配，不参与评分
     */
    private List<QueryBuilder> filter;
    
    /**
     * should 子句：应该匹配，参与评分
     */
    private List<QueryBuilder> should;
    
    /**
     * must_not 子句：必须不匹配
     */
    private List<QueryBuilder> mustNot;
    
    /**
     * should 子句的最小匹配数
     */
    private String minimumShouldMatch;
    
    /**
     * 权重
     */
    private Float boost;
    
    public BoolQueryBuilder() {
        this.must = new ArrayList<>();
        this.filter = new ArrayList<>();
        this.should = new ArrayList<>();
        this.mustNot = new ArrayList<>();
    }
    
    @Override
    public QueryType getQueryType() {
        return QueryType.BOOL;
    }
    
    // Builder 模式
    
    /**
     * 添加 must 子句
     */
    public BoolQueryBuilder must(QueryBuilder query) {
        this.must.add(query);
        return this;
    }
    
    /**
     * 添加 filter 子句
     */
    public BoolQueryBuilder filter(QueryBuilder query) {
        this.filter.add(query);
        return this;
    }
    
    /**
     * 添加 should 子句
     */
    public BoolQueryBuilder should(QueryBuilder query) {
        this.should.add(query);
        return this;
    }
    
    /**
     * 添加 must_not 子句
     */
    public BoolQueryBuilder mustNot(QueryBuilder query) {
        this.mustNot.add(query);
        return this;
    }
    
    /**
     * 设置 should 子句的最小匹配数
     */
    public BoolQueryBuilder minimumShouldMatch(String minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }
    
    /**
     * 设置权重
     */
    public BoolQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }
    
    // Getters
    
    public List<QueryBuilder> getMust() {
        return must;
    }
    
    public List<QueryBuilder> getFilter() {
        return filter;
    }
    
    public List<QueryBuilder> getShould() {
        return should;
    }
    
    public List<QueryBuilder> getMustNot() {
        return mustNot;
    }
    
    public String getMinimumShouldMatch() {
        return minimumShouldMatch;
    }
    
    public Float getBoost() {
        return boost;
    }
    
    /**
     * 是否有任何子句
     */
    public boolean hasClauses() {
        return !must.isEmpty() || !filter.isEmpty() || !should.isEmpty() || !mustNot.isEmpty();
    }
}

