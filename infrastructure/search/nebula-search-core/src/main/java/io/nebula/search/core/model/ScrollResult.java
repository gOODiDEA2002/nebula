package io.nebula.search.core.model;

import java.util.List;

public class ScrollResult<T> {
    private boolean success;
    private String scrollId;
    private List<SearchDocument<T>> documents;
    private long totalHits;
    private boolean hasNext;
    private String errorMessage;
    
    public ScrollResult() {}
    
    public static <T> ScrollResult<T> success(String scrollId, List<SearchDocument<T>> documents, 
                                             long totalHits, boolean hasNext) {
        ScrollResult<T> result = new ScrollResult<>();
        result.setSuccess(true);
        result.setScrollId(scrollId);
        result.setDocuments(documents);
        result.setTotalHits(totalHits);
        result.setHasNext(hasNext);
        return result;
    }
    
    public static <T> ScrollResult<T> error(String errorMessage) {
        ScrollResult<T> result = new ScrollResult<>();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getScrollId() { return scrollId; }
    public void setScrollId(String scrollId) { this.scrollId = scrollId; }
    
    public List<SearchDocument<T>> getDocuments() { return documents; }
    public void setDocuments(List<SearchDocument<T>> documents) { this.documents = documents; }
    
    public long getTotalHits() { return totalHits; }
    public void setTotalHits(long totalHits) { this.totalHits = totalHits; }
    
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
