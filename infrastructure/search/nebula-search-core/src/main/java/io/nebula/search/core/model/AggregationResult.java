package io.nebula.search.core.model;

import java.util.Map;

public class AggregationResult {
    private boolean success;
    private Map<String, Object> aggregations;
    private String errorMessage;
    
    public AggregationResult() {}
    
    public static AggregationResult success(Map<String, Object> aggregations) {
        AggregationResult result = new AggregationResult();
        result.setSuccess(true);
        result.setAggregations(aggregations);
        return result;
    }
    
    public static AggregationResult error(String errorMessage) {
        AggregationResult result = new AggregationResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public Map<String, Object> getAggregations() { return aggregations; }
    public void setAggregations(Map<String, Object> aggregations) { this.aggregations = aggregations; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
