package io.nebula.search.core.query;

import java.util.Map;

public class AggregationQuery {
    private String[] indices;
    private Map<String, Object> query;
    private Map<String, Object> aggregations;
    
    public AggregationQuery() {}
    
    // Getters and Setters
    public String[] getIndices() { return indices; }
    public void setIndices(String[] indices) { this.indices = indices; }
    
    public Map<String, Object> getQuery() { return query; }
    public void setQuery(Map<String, Object> query) { this.query = query; }
    
    public Map<String, Object> getAggregations() { return aggregations; }
    public void setAggregations(Map<String, Object> aggregations) { this.aggregations = aggregations; }
}
