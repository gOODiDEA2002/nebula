package io.nebula.search.core.model;

import java.util.List;
import java.util.Map;

public class SuggestResult {
    private boolean success;
    private Map<String, List<String>> suggestions;
    private String errorMessage;
    
    public SuggestResult() {}
    
    public static SuggestResult success(Map<String, List<String>> suggestions) {
        SuggestResult result = new SuggestResult();
        result.setSuccess(true);
        result.setSuggestions(suggestions);
        return result;
    }
    
    public static SuggestResult error(String errorMessage) {
        SuggestResult result = new SuggestResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public Map<String, List<String>> getSuggestions() { return suggestions; }
    public void setSuggestions(Map<String, List<String>> suggestions) { this.suggestions = suggestions; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
