package io.nebula.search.core.query;

import io.nebula.search.core.aggregation.Aggregation;
import io.nebula.search.core.query.builder.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合查询
 * 支持强类型的聚合配置
 * 
 * @author nebula
 */
public class AggregationQuery {
    
    /**
     * 索引名称
     */
    private String[] indices;
    
    /**
     * 查询条件（用于过滤聚合数据）
     */
    private QueryBuilder queryBuilder;
    
    /**
     * 聚合列表
     */
    private List<Aggregation> aggregations;
    
    public AggregationQuery() {
        this.aggregations = new ArrayList<>();
    }
    
    public AggregationQuery(String... indices) {
        this.indices = indices;
        this.aggregations = new ArrayList<>();
    }
    
    // Builder 模式
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private AggregationQuery query = new AggregationQuery();
        
        public Builder indices(String... indices) {
            query.indices = indices;
            return this;
        }
        
        public Builder index(String index) {
            query.indices = new String[]{index};
            return this;
        }
        
        public Builder query(QueryBuilder queryBuilder) {
            query.queryBuilder = queryBuilder;
            return this;
        }
        
        public Builder addAggregation(Aggregation aggregation) {
            query.aggregations.add(aggregation);
            return this;
        }
        
        public Builder aggregations(List<Aggregation> aggregations) {
            query.aggregations = aggregations;
            return this;
        }
        
        public AggregationQuery build() {
            return query;
        }
    }
    
    // Getters and Setters
    
    public String[] getIndices() { 
        return indices; 
    }
    
    public void setIndices(String[] indices) { 
        this.indices = indices; 
    }
    
    public QueryBuilder getQueryBuilder() { 
        return queryBuilder; 
    }
    
    public void setQueryBuilder(QueryBuilder queryBuilder) { 
        this.queryBuilder = queryBuilder; 
    }
    
    public List<Aggregation> getAggregations() { 
        return aggregations; 
    }
    
    public void setAggregations(List<Aggregation> aggregations) { 
        this.aggregations = aggregations; 
    }
    
    public void addAggregation(Aggregation aggregation) {
        this.aggregations.add(aggregation);
    }
}
