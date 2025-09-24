package io.nebula.search.core.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 搜索结果
 */
public class SearchResult<T> {
    
    /**
     * 搜索是否成功
     */
    private boolean success;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 总命中数
     */
    private long totalHits;
    
    /**
     * 命中关系（精确或估算）
     */
    private String totalHitsRelation;
    
    /**
     * 最大分数
     */
    private Double maxScore;
    
    /**
     * 文档列表
     */
    private List<SearchDocument<T>> documents;
    
    /**
     * 聚合结果
     */
    private Map<String, Object> aggregations;
    
    /**
     * 搜索耗时（毫秒）
     */
    private long took;
    
    /**
     * 是否超时
     */
    private boolean timedOut;
    
    /**
     * 分片信息
     */
    private ShardInfo shards;
    
    /**
     * 搜索时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 滚动ID（用于滚动搜索）
     */
    private String scrollId;
    
    /**
     * 搜索建议
     */
    private Map<String, List<String>> suggestions;
    
    // 构造函数
    public SearchResult() {
        this.timestamp = LocalDateTime.now();
    }
    
    public SearchResult(boolean success) {
        this();
        this.success = success;
    }
    
    // 静态工厂方法
    public static <T> SearchResult<T> success() {
        return new SearchResult<>(true);
    }
    
    public static <T> SearchResult<T> success(List<SearchDocument<T>> documents, long totalHits) {
        SearchResult<T> result = new SearchResult<>(true);
        result.setDocuments(documents);
        result.setTotalHits(totalHits);
        return result;
    }
    
    public static <T> SearchResult<T> error(String errorMessage) {
        SearchResult<T> result = new SearchResult<>(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Builder模式
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    public static class Builder<T> {
        private SearchResult<T> result = new SearchResult<>();
        
        public Builder<T> success(boolean success) {
            result.success = success;
            return this;
        }
        
        public Builder<T> totalHits(long totalHits) {
            result.totalHits = totalHits;
            return this;
        }
        
        public Builder<T> maxScore(Double maxScore) {
            result.maxScore = maxScore;
            return this;
        }
        
        public Builder<T> documents(List<SearchDocument<T>> documents) {
            result.documents = documents;
            return this;
        }
        
        public Builder<T> aggregations(Map<String, Object> aggregations) {
            result.aggregations = aggregations;
            return this;
        }
        
        public Builder<T> took(long took) {
            result.took = took;
            return this;
        }
        
        public Builder<T> timedOut(boolean timedOut) {
            result.timedOut = timedOut;
            return this;
        }
        
        public Builder<T> shards(ShardInfo shards) {
            result.shards = shards;
            return this;
        }
        
        public Builder<T> scrollId(String scrollId) {
            result.scrollId = scrollId;
            return this;
        }
        
        public Builder<T> suggestions(Map<String, List<String>> suggestions) {
            result.suggestions = suggestions;
            return this;
        }
        
        public Builder<T> errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }
        
        public SearchResult<T> build() {
            return result;
        }
    }
    
    // Getter and Setter methods
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public long getTotalHits() {
        return totalHits;
    }
    
    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }
    
    public String getTotalHitsRelation() {
        return totalHitsRelation;
    }
    
    public void setTotalHitsRelation(String totalHitsRelation) {
        this.totalHitsRelation = totalHitsRelation;
    }
    
    public Double getMaxScore() {
        return maxScore;
    }
    
    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }
    
    public List<SearchDocument<T>> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<SearchDocument<T>> documents) {
        this.documents = documents;
    }
    
    public Map<String, Object> getAggregations() {
        return aggregations;
    }
    
    public void setAggregations(Map<String, Object> aggregations) {
        this.aggregations = aggregations;
    }
    
    public long getTook() {
        return took;
    }
    
    public void setTook(long took) {
        this.took = took;
    }
    
    public boolean isTimedOut() {
        return timedOut;
    }
    
    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }
    
    public ShardInfo getShards() {
        return shards;
    }
    
    public void setShards(ShardInfo shards) {
        this.shards = shards;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getScrollId() {
        return scrollId;
    }
    
    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }
    
    public Map<String, List<String>> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(Map<String, List<String>> suggestions) {
        this.suggestions = suggestions;
    }
    
    /**
     * 是否有结果
     */
    public boolean hasHits() {
        return documents != null && !documents.isEmpty();
    }
    
    /**
     * 获取结果数量
     */
    public int getHitCount() {
        return documents != null ? documents.size() : 0;
    }
    
    /**
     * 获取第一个文档
     */
    public SearchDocument<T> getFirstDocument() {
        return hasHits() ? documents.get(0) : null;
    }
    
    /**
     * 分片信息
     */
    public static class ShardInfo {
        private int total;
        private int successful;
        private int skipped;
        private int failed;
        
        public ShardInfo() {}
        
        public ShardInfo(int total, int successful, int skipped, int failed) {
            this.total = total;
            this.successful = successful;
            this.skipped = skipped;
            this.failed = failed;
        }
        
        // Getter and Setter methods
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public int getSuccessful() { return successful; }
        public void setSuccessful(int successful) { this.successful = successful; }
        
        public int getSkipped() { return skipped; }
        public void setSkipped(int skipped) { this.skipped = skipped; }
        
        public int getFailed() { return failed; }
        public void setFailed(int failed) { this.failed = failed; }
    }
    
    @Override
    public String toString() {
        return "SearchResult{" +
                "success=" + success +
                ", totalHits=" + totalHits +
                ", maxScore=" + maxScore +
                ", documentCount=" + getHitCount() +
                ", took=" + took +
                ", timedOut=" + timedOut +
                ", timestamp=" + timestamp +
                '}';
    }
}
