package io.nebula.search.core.model;

/**
 * 索引操作结果
 */
public class IndexResult {
    private boolean success;
    private String errorMessage;
    private String indexName;
    private boolean acknowledged;
    
    public IndexResult() {}
    
    public IndexResult(boolean success, String indexName) {
        this.success = success;
        this.indexName = indexName;
    }
    
    public static IndexResult success(String indexName) {
        IndexResult result = new IndexResult(true, indexName);
        result.setAcknowledged(true);
        return result;
    }
    
    public static IndexResult error(String indexName, String errorMessage) {
        IndexResult result = new IndexResult(false, indexName);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
}
