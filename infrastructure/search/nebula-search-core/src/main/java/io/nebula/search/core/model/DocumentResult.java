package io.nebula.search.core.model;

public class DocumentResult {
    private boolean success;
    private String id;
    private String index;
    private String errorMessage;
    
    public DocumentResult() {}
    
    public static DocumentResult success(String index, String id) {
        DocumentResult result = new DocumentResult();
        result.setSuccess(true);
        result.setIndex(index);
        result.setId(id);
        return result;
    }
    
    public static DocumentResult error(String index, String id, String errorMessage) {
        DocumentResult result = new DocumentResult();
        result.setSuccess(false);
        result.setIndex(index);
        result.setId(id);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getIndex() { return index; }
    public void setIndex(String index) { this.index = index; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
