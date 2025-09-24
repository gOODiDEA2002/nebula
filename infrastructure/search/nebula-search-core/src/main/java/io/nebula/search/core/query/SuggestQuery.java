package io.nebula.search.core.query;

import java.util.Map;

public class SuggestQuery {
    private String[] indices;
    private Map<String, Object> suggest;
    
    public SuggestQuery() {}
    
    // Getters and Setters
    public String[] getIndices() { return indices; }
    public void setIndices(String[] indices) { this.indices = indices; }
    
    public Map<String, Object> getSuggest() { return suggest; }
    public void setSuggest(Map<String, Object> suggest) { this.suggest = suggest; }
}
