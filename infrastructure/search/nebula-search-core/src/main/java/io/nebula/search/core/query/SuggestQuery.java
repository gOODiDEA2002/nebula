package io.nebula.search.core.query;

import io.nebula.search.core.suggestion.Suggester;

import java.util.ArrayList;
import java.util.List;

/**
 * 建议查询
 * 支持强类型的建议配置
 * 
 * @author nebula
 */
public class SuggestQuery {
    
    /**
     * 索引名称
     */
    private String[] indices;
    
    /**
     * 建议器列表
     */
    private List<Suggester> suggesters;
    
    public SuggestQuery() {
        this.suggesters = new ArrayList<>();
    }
    
    public SuggestQuery(String... indices) {
        this.indices = indices;
        this.suggesters = new ArrayList<>();
    }
    
    // Builder 模式
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private SuggestQuery query = new SuggestQuery();
        
        public Builder indices(String... indices) {
            query.indices = indices;
            return this;
        }
        
        public Builder index(String index) {
            query.indices = new String[]{index};
            return this;
        }
        
        public Builder addSuggester(Suggester suggester) {
            query.suggesters.add(suggester);
            return this;
        }
        
        public Builder suggesters(List<Suggester> suggesters) {
            query.suggesters = suggesters;
            return this;
        }
        
        public SuggestQuery build() {
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
    
    public List<Suggester> getSuggesters() { 
        return suggesters; 
    }
    
    public void setSuggesters(List<Suggester> suggesters) { 
        this.suggesters = suggesters; 
    }
    
    public void addSuggester(Suggester suggester) {
        this.suggesters.add(suggester);
    }
}
