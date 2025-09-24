package io.nebula.search.core.model;

import java.util.Map;

/**
 * 搜索文档
 */
public class SearchDocument<T> {
    
    /**
     * 文档ID
     */
    private String id;
    
    /**
     * 索引名称
     */
    private String index;
    
    /**
     * 文档类型
     */
    private String type;
    
    /**
     * 匹配分数
     */
    private Double score;
    
    /**
     * 文档版本
     */
    private Long version;
    
    /**
     * 文档源数据
     */
    private T source;
    
    /**
     * 高亮结果
     */
    private Map<String, String[]> highlight;
    
    /**
     * 排序值
     */
    private Object[] sort;
    
    /**
     * 字段值
     */
    private Map<String, Object> fields;
    
    // 构造函数
    public SearchDocument() {}
    
    public SearchDocument(String id, T source) {
        this.id = id;
        this.source = source;
    }
    
    public SearchDocument(String id, T source, Double score) {
        this.id = id;
        this.source = source;
        this.score = score;
    }
    
    // Builder模式
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    public static class Builder<T> {
        private SearchDocument<T> document = new SearchDocument<>();
        
        public Builder<T> id(String id) {
            document.id = id;
            return this;
        }
        
        public Builder<T> index(String index) {
            document.index = index;
            return this;
        }
        
        public Builder<T> type(String type) {
            document.type = type;
            return this;
        }
        
        public Builder<T> score(Double score) {
            document.score = score;
            return this;
        }
        
        public Builder<T> version(Long version) {
            document.version = version;
            return this;
        }
        
        public Builder<T> source(T source) {
            document.source = source;
            return this;
        }
        
        public Builder<T> highlight(Map<String, String[]> highlight) {
            document.highlight = highlight;
            return this;
        }
        
        public Builder<T> sort(Object[] sort) {
            document.sort = sort;
            return this;
        }
        
        public Builder<T> fields(Map<String, Object> fields) {
            document.fields = fields;
            return this;
        }
        
        public SearchDocument<T> build() {
            return document;
        }
    }
    
    // Getter and Setter methods
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getIndex() {
        return index;
    }
    
    public void setIndex(String index) {
        this.index = index;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public T getSource() {
        return source;
    }
    
    public void setSource(T source) {
        this.source = source;
    }
    
    public Map<String, String[]> getHighlight() {
        return highlight;
    }
    
    public void setHighlight(Map<String, String[]> highlight) {
        this.highlight = highlight;
    }
    
    public Object[] getSort() {
        return sort;
    }
    
    public void setSort(Object[] sort) {
        this.sort = sort;
    }
    
    public Map<String, Object> getFields() {
        return fields;
    }
    
    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
    
    @Override
    public String toString() {
        return "SearchDocument{" +
                "id='" + id + '\'' +
                ", index='" + index + '\'' +
                ", score=" + score +
                ", source=" + source +
                '}';
    }
}
