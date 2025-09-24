package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 文档模型
 */
public class Document {

    private final String id;
    private final String content;
    private final List<Double> vector;
    private final Map<String, Object> metadata;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @JsonCreator
    public Document(@JsonProperty("id") String id,
                   @JsonProperty("content") String content,
                   @JsonProperty("vector") List<Double> vector,
                   @JsonProperty("metadata") Map<String, Object> metadata,
                   @JsonProperty("createdAt") LocalDateTime createdAt,
                   @JsonProperty("updatedAt") LocalDateTime updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.content = Objects.requireNonNull(content, "Content cannot be null");
        this.vector = vector;
        this.metadata = metadata;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    /**
     * 创建简单文档（不含向量）
     */
    public static Document simple(String content) {
        return new Document(null, content, null, null, null, null);
    }

    /**
     * 创建简单文档（指定ID）
     */
    public static Document simple(String id, String content) {
        return new Document(id, content, null, null, null, null);
    }

    /**
     * 创建带元数据的文档
     */
    public static Document withMetadata(String content, Map<String, Object> metadata) {
        return new Document(null, content, null, metadata, null, null);
    }

    /**
     * 创建带向量的文档
     */
    public static Document withVector(String content, List<Double> vector) {
        return new Document(null, content, vector, null, null, null);
    }

    /**
     * 创建文档构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建文档副本（更新向量）
     */
    public Document withVector(List<Double> vector) {
        return new Document(this.id, this.content, vector, this.metadata, 
                          this.createdAt, LocalDateTime.now());
    }

    /**
     * 创建文档副本（更新元数据）
     */
    public Document withMetadata(Map<String, Object> metadata) {
        return new Document(this.id, this.content, this.vector, metadata, 
                          this.createdAt, LocalDateTime.now());
    }

    /**
     * 创建文档副本（更新内容）
     */
    public Document withContent(String content) {
        return new Document(this.id, content, this.vector, this.metadata, 
                          this.createdAt, LocalDateTime.now());
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public List<Double> getVector() {
        return vector;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 检查是否有向量
     */
    public boolean hasVector() {
        return vector != null && !vector.isEmpty();
    }

    /**
     * 获取向量维度
     */
    public int getVectorDimension() {
        return hasVector() ? vector.size() : 0;
    }

    /**
     * 获取元数据值
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * 获取元数据值（带默认值）
     */
    public Object getMetadataValue(String key, Object defaultValue) {
        if (metadata == null) {
            return defaultValue;
        }
        return metadata.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id) &&
               Objects.equals(content, document.content) &&
               Objects.equals(vector, document.vector) &&
               Objects.equals(metadata, document.metadata) &&
               Objects.equals(createdAt, document.createdAt) &&
               Objects.equals(updatedAt, document.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, vector, metadata, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Document{" +
               "id='" + id + '\'' +
               ", content='" + (content.length() > 100 ? content.substring(0, 100) + "..." : content) + '\'' +
               ", vector=" + (vector != null ? vector.size() + "D" : "null") +
               ", metadata=" + metadata +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }

    /**
     * 文档构建器
     */
    public static class Builder {
        private String id;
        private String content;
        private List<Double> vector;
        private Map<String, Object> metadata;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder vector(List<Double> vector) {
            this.vector = vector;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new java.util.HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Document build() {
            return new Document(id, content, vector, metadata, createdAt, updatedAt);
        }
    }
}
