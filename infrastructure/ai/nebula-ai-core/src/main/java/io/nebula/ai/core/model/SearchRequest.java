package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 搜索请求模型
 */
public class SearchRequest {

    private final String query;
    private final List<Double> vector;
    private final int topK;
    private final double similarityThreshold;
    private final Map<String, Object> filter;
    private final Map<String, Object> options;

    @JsonCreator
    public SearchRequest(@JsonProperty("query") String query,
                        @JsonProperty("vector") List<Double> vector,
                        @JsonProperty("topK") Integer topK,
                        @JsonProperty("similarityThreshold") Double similarityThreshold,
                        @JsonProperty("filter") Map<String, Object> filter,
                        @JsonProperty("options") Map<String, Object> options) {
        this.query = query;
        this.vector = vector;
        this.topK = topK != null ? topK : 10;
        this.similarityThreshold = similarityThreshold != null ? similarityThreshold : 0.0;
        this.filter = filter;
        this.options = options;
        
        // 验证参数
        if (query == null && vector == null) {
            throw new IllegalArgumentException("Either query or vector must be provided");
        }
    }

    /**
     * 创建文本查询请求
     */
    public static SearchRequest query(String query) {
        return new SearchRequest(query, null, 10, 0.0, null, null);
    }

    /**
     * 创建文本查询请求（指定TopK）
     */
    public static SearchRequest query(String query, int topK) {
        return new SearchRequest(query, null, topK, 0.0, null, null);
    }

    /**
     * 创建向量查询请求
     */
    public static SearchRequest vector(List<Double> vector) {
        return new SearchRequest(null, vector, 10, 0.0, null, null);
    }

    /**
     * 创建向量查询请求（指定TopK）
     */
    public static SearchRequest vector(List<Double> vector, int topK) {
        return new SearchRequest(null, vector, topK, 0.0, null, null);
    }

    /**
     * 创建搜索请求构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getQuery() {
        return query;
    }

    public List<Double> getVector() {
        return vector;
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public Map<String, Object> getFilter() {
        return filter;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    /**
     * 检查是否为文本查询
     */
    public boolean isTextQuery() {
        return query != null;
    }

    /**
     * 检查是否为向量查询
     */
    public boolean isVectorQuery() {
        return vector != null;
    }

    /**
     * 检查是否有过滤条件
     */
    public boolean hasFilter() {
        return filter != null && !filter.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchRequest that = (SearchRequest) o;
        return topK == that.topK &&
               Double.compare(that.similarityThreshold, similarityThreshold) == 0 &&
               Objects.equals(query, that.query) &&
               Objects.equals(vector, that.vector) &&
               Objects.equals(filter, that.filter) &&
               Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, vector, topK, similarityThreshold, filter, options);
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
               "query='" + query + '\'' +
               ", vector=" + (vector != null ? vector.size() + "D" : "null") +
               ", topK=" + topK +
               ", similarityThreshold=" + similarityThreshold +
               ", filter=" + filter +
               ", options=" + options +
               '}';
    }

    /**
     * 搜索请求构建器
     */
    public static class Builder {
        private String query;
        private List<Double> vector;
        private Integer topK;
        private Double similarityThreshold;
        private Map<String, Object> filter;
        private Map<String, Object> options;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder vector(List<Double> vector) {
            this.vector = vector;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder similarityThreshold(double threshold) {
            this.similarityThreshold = threshold;
            return this;
        }

        public Builder filter(Map<String, Object> filter) {
            this.filter = filter;
            return this;
        }

        public Builder addFilter(String key, Object value) {
            if (this.filter == null) {
                this.filter = new java.util.HashMap<>();
            }
            this.filter.put(key, value);
            return this;
        }

        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        public Builder addOption(String key, Object value) {
            if (this.options == null) {
                this.options = new java.util.HashMap<>();
            }
            this.options.put(key, value);
            return this;
        }

        public SearchRequest build() {
            return new SearchRequest(query, vector, topK, similarityThreshold, filter, options);
        }
    }
}
