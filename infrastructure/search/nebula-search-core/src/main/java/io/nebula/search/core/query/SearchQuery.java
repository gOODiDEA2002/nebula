package io.nebula.search.core.query;

import java.util.List;
import java.util.Map;

/**
 * 搜索查询
 */
public class SearchQuery {
    
    /**
     * 索引名称
     */
    private String[] indices;
    
    /**
     * 查询条件
     */
    private Map<String, Object> query;
    
    /**
     * 过滤条件
     */
    private Map<String, Object> filter;
    
    /**
     * 排序
     */
    private List<Map<String, Object>> sort;
    
    /**
     * 分页-起始位置
     */
    private Integer from = 0;
    
    /**
     * 分页-数量
     */
    private Integer size = 10;
    
    /**
     * 高亮字段
     */
    private Map<String, Object> highlight;
    
    /**
     * 聚合
     */
    private Map<String, Object> aggregations;
    
    /**
     * 源字段过滤
     */
    private Object source;
    
    /**
     * 超时时间
     */
    private String timeout;
    
    /**
     * 搜索类型
     */
    private String searchType;
    
    /**
     * 滚动时间
     */
    private String scroll;
    
    /**
     * 最小分数
     */
    private Double minScore;
    
    /**
     * 是否返回版本
     */
    private Boolean version;
    
    /**
     * 是否解释评分
     */
    private Boolean explain;
    
    /**
     * 路由
     */
    private String routing;
    
    /**
     * 偏好设置
     */
    private String preference;
    
    // 构造函数
    public SearchQuery() {}
    
    public SearchQuery(String index) {
        this.indices = new String[]{index};
    }
    
    public SearchQuery(String[] indices) {
        this.indices = indices;
    }
    
    // Builder模式
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private SearchQuery query = new SearchQuery();
        
        public Builder indices(String... indices) {
            query.indices = indices;
            return this;
        }
        
        public Builder index(String index) {
            query.indices = new String[]{index};
            return this;
        }
        
        public Builder query(Map<String, Object> queryMap) {
            query.query = queryMap;
            return this;
        }
        
        public Builder filter(Map<String, Object> filter) {
            query.filter = filter;
            return this;
        }
        
        public Builder sort(List<Map<String, Object>> sort) {
            query.sort = sort;
            return this;
        }
        
        public Builder from(int from) {
            query.from = from;
            return this;
        }
        
        public Builder size(int size) {
            query.size = size;
            return this;
        }
        
        public Builder highlight(Map<String, Object> highlight) {
            query.highlight = highlight;
            return this;
        }
        
        public Builder aggregations(Map<String, Object> aggregations) {
            query.aggregations = aggregations;
            return this;
        }
        
        public Builder source(Object source) {
            query.source = source;
            return this;
        }
        
        public Builder timeout(String timeout) {
            query.timeout = timeout;
            return this;
        }
        
        public Builder searchType(String searchType) {
            query.searchType = searchType;
            return this;
        }
        
        public Builder scroll(String scroll) {
            query.scroll = scroll;
            return this;
        }
        
        public Builder minScore(Double minScore) {
            query.minScore = minScore;
            return this;
        }
        
        public Builder version(Boolean version) {
            query.version = version;
            return this;
        }
        
        public Builder explain(Boolean explain) {
            query.explain = explain;
            return this;
        }
        
        public Builder routing(String routing) {
            query.routing = routing;
            return this;
        }
        
        public Builder preference(String preference) {
            query.preference = preference;
            return this;
        }
        
        public SearchQuery build() {
            return query;
        }
    }
    
    // Getter and Setter methods
    
    public String[] getIndices() {
        return indices;
    }
    
    public void setIndices(String[] indices) {
        this.indices = indices;
    }
    
    public Map<String, Object> getQuery() {
        return query;
    }
    
    public void setQuery(Map<String, Object> query) {
        this.query = query;
    }
    
    public Map<String, Object> getFilter() {
        return filter;
    }
    
    public void setFilter(Map<String, Object> filter) {
        this.filter = filter;
    }
    
    public List<Map<String, Object>> getSort() {
        return sort;
    }
    
    public void setSort(List<Map<String, Object>> sort) {
        this.sort = sort;
    }
    
    public Integer getFrom() {
        return from;
    }
    
    public void setFrom(Integer from) {
        this.from = from;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
    
    public Map<String, Object> getHighlight() {
        return highlight;
    }
    
    public void setHighlight(Map<String, Object> highlight) {
        this.highlight = highlight;
    }
    
    public Map<String, Object> getAggregations() {
        return aggregations;
    }
    
    public void setAggregations(Map<String, Object> aggregations) {
        this.aggregations = aggregations;
    }
    
    public Object getSource() {
        return source;
    }
    
    public void setSource(Object source) {
        this.source = source;
    }
    
    public String getTimeout() {
        return timeout;
    }
    
    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
    
    public String getSearchType() {
        return searchType;
    }
    
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
    
    public String getScroll() {
        return scroll;
    }
    
    public void setScroll(String scroll) {
        this.scroll = scroll;
    }
    
    public Double getMinScore() {
        return minScore;
    }
    
    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }
    
    public Boolean getVersion() {
        return version;
    }
    
    public void setVersion(Boolean version) {
        this.version = version;
    }
    
    public Boolean getExplain() {
        return explain;
    }
    
    public void setExplain(Boolean explain) {
        this.explain = explain;
    }
    
    public String getRouting() {
        return routing;
    }
    
    public void setRouting(String routing) {
        this.routing = routing;
    }
    
    public String getPreference() {
        return preference;
    }
    
    public void setPreference(String preference) {
        this.preference = preference;
    }
}
