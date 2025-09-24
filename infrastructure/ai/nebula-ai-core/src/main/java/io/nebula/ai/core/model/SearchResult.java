package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 搜索结果模型
 */
public class SearchResult {

    private final String id;
    private final List<DocumentResult> documents;
    private final String query;
    private final int totalFound;
    private final double maxScore;
    private final double minScore;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;

    @JsonCreator
    public SearchResult(@JsonProperty("id") String id,
                       @JsonProperty("documents") List<DocumentResult> documents,
                       @JsonProperty("query") String query,
                       @JsonProperty("totalFound") Integer totalFound,
                       @JsonProperty("maxScore") Double maxScore,
                       @JsonProperty("minScore") Double minScore,
                       @JsonProperty("timestamp") LocalDateTime timestamp,
                       @JsonProperty("metadata") Map<String, Object> metadata) {
        this.id = id;
        this.documents = Objects.requireNonNull(documents, "Documents cannot be null");
        this.query = query;
        this.totalFound = totalFound != null ? totalFound : documents.size();
        this.maxScore = maxScore != null ? maxScore : calculateMaxScore(documents);
        this.minScore = minScore != null ? minScore : calculateMinScore(documents);
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.metadata = metadata;
    }

    /**
     * 创建简单的搜索结果
     */
    public static SearchResult simple(List<DocumentResult> documents) {
        return new SearchResult(null, documents, null, null, null, null, null, null);
    }

    /**
     * 创建搜索结果构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建空的搜索结果
     */
    public static SearchResult empty() {
        return new SearchResult(null, List.of(), null, 0, 0.0, 0.0, null, null);
    }

    public String getId() {
        return id;
    }

    public List<DocumentResult> getDocuments() {
        return documents;
    }

    public String getQuery() {
        return query;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public double getMinScore() {
        return minScore;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 检查是否为空结果
     */
    public boolean isEmpty() {
        return documents.isEmpty();
    }

    /**
     * 获取结果数量
     */
    public int size() {
        return documents.size();
    }

    /**
     * 获取第一个结果
     */
    public DocumentResult getFirst() {
        return documents.isEmpty() ? null : documents.get(0);
    }

    /**
     * 获取分数大于阈值的结果
     */
    public List<DocumentResult> getDocumentsAboveScore(double threshold) {
        return documents.stream()
                .filter(doc -> doc.getScore() >= threshold)
                .toList();
    }

    /**
     * 获取所有文档内容
     */
    public List<String> getContents() {
        return documents.stream()
                .map(DocumentResult::getContent)
                .toList();
    }

    /**
     * 获取所有文档ID
     */
    public List<String> getDocumentIds() {
        return documents.stream()
                .map(DocumentResult::getId)
                .toList();
    }

    private static double calculateMaxScore(List<DocumentResult> documents) {
        return documents.stream()
                .mapToDouble(DocumentResult::getScore)
                .max()
                .orElse(0.0);
    }

    private static double calculateMinScore(List<DocumentResult> documents) {
        return documents.stream()
                .mapToDouble(DocumentResult::getScore)
                .min()
                .orElse(0.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return totalFound == that.totalFound &&
               Double.compare(that.maxScore, maxScore) == 0 &&
               Double.compare(that.minScore, minScore) == 0 &&
               Objects.equals(id, that.id) &&
               Objects.equals(documents, that.documents) &&
               Objects.equals(query, that.query) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, documents, query, totalFound, maxScore, minScore, timestamp, metadata);
    }

    @Override
    public String toString() {
        return "SearchResult{" +
               "id='" + id + '\'' +
               ", documents=" + documents.size() +
               ", query='" + query + '\'' +
               ", totalFound=" + totalFound +
               ", maxScore=" + maxScore +
               ", minScore=" + minScore +
               ", timestamp=" + timestamp +
               ", metadata=" + metadata +
               '}';
    }

    /**
     * 文档搜索结果
     */
    public static class DocumentResult {
        private final String id;
        private final String content;
        private final double score;
        private final Map<String, Object> metadata;
        private final List<Double> vector;

        @JsonCreator
        public DocumentResult(@JsonProperty("id") String id,
                             @JsonProperty("content") String content,
                             @JsonProperty("score") double score,
                             @JsonProperty("metadata") Map<String, Object> metadata,
                             @JsonProperty("vector") List<Double> vector) {
            this.id = Objects.requireNonNull(id, "ID cannot be null");
            this.content = Objects.requireNonNull(content, "Content cannot be null");
            this.score = score;
            this.metadata = metadata;
            this.vector = vector;
        }

        /**
         * 从文档创建结果
         */
        public static DocumentResult fromDocument(Document document, double score) {
            return new DocumentResult(document.getId(), document.getContent(), 
                                    score, document.getMetadata(), document.getVector());
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public double getScore() {
            return score;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public List<Double> getVector() {
            return vector;
        }

        /**
         * 转换为文档对象
         */
        public Document toDocument() {
            return Document.builder()
                    .id(id)
                    .content(content)
                    .vector(vector)
                    .metadata(metadata)
                    .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DocumentResult that = (DocumentResult) o;
            return Double.compare(that.score, score) == 0 &&
                   Objects.equals(id, that.id) &&
                   Objects.equals(content, that.content) &&
                   Objects.equals(metadata, that.metadata) &&
                   Objects.equals(vector, that.vector);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, content, score, metadata, vector);
        }

        @Override
        public String toString() {
            return "DocumentResult{" +
                   "id='" + id + '\'' +
                   ", content='" + (content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                   ", score=" + score +
                   ", metadata=" + metadata +
                   ", vector=" + (vector != null ? vector.size() + "D" : "null") +
                   '}';
        }
    }

    /**
     * 搜索结果构建器
     */
    public static class Builder {
        private String id;
        private List<DocumentResult> documents;
        private String query;
        private Integer totalFound;
        private Double maxScore;
        private Double minScore;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder documents(List<DocumentResult> documents) {
            this.documents = documents;
            return this;
        }

        public Builder addDocument(DocumentResult document) {
            if (this.documents == null) {
                this.documents = new java.util.ArrayList<>();
            }
            this.documents.add(document);
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder totalFound(int totalFound) {
            this.totalFound = totalFound;
            return this;
        }

        public Builder maxScore(double maxScore) {
            this.maxScore = maxScore;
            return this;
        }

        public Builder minScore(double minScore) {
            this.minScore = minScore;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SearchResult build() {
            return new SearchResult(id, documents, query, totalFound, maxScore, minScore, timestamp, metadata);
        }
    }
}
