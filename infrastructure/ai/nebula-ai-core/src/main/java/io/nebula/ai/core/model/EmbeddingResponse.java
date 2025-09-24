package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 嵌入响应模型
 */
public class EmbeddingResponse {

    private final String id;
    private final List<Embedding> embeddings;
    private final String model;
    private final LocalDateTime timestamp;
    private final Usage usage;
    private final Map<String, Object> metadata;

    @JsonCreator
    public EmbeddingResponse(@JsonProperty("id") String id,
                            @JsonProperty("embeddings") List<Embedding> embeddings,
                            @JsonProperty("model") String model,
                            @JsonProperty("timestamp") LocalDateTime timestamp,
                            @JsonProperty("usage") Usage usage,
                            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.id = id;
        this.embeddings = Objects.requireNonNull(embeddings, "Embeddings cannot be null");
        this.model = model;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.usage = usage;
        this.metadata = metadata;
    }

    /**
     * 创建简单的嵌入响应
     */
    public static EmbeddingResponse simple(List<Double> vector) {
        Embedding embedding = new Embedding(0, vector, null);
        return new EmbeddingResponse(null, List.of(embedding), null, null, null, null);
    }

    /**
     * 创建嵌入响应构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public List<Embedding> getEmbeddings() {
        return embeddings;
    }

    public String getModel() {
        return model;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Usage getUsage() {
        return usage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 获取第一个嵌入向量（用于单个文本的情况）
     */
    public List<Double> getFirstVector() {
        return embeddings.isEmpty() ? null : embeddings.get(0).getVector();
    }

    /**
     * 获取所有嵌入向量
     */
    public List<List<Double>> getAllVectors() {
        return embeddings.stream()
                .map(Embedding::getVector)
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingResponse that = (EmbeddingResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(embeddings, that.embeddings) &&
               Objects.equals(model, that.model) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(usage, that.usage) &&
               Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, embeddings, model, timestamp, usage, metadata);
    }

    @Override
    public String toString() {
        return "EmbeddingResponse{" +
               "id='" + id + '\'' +
               ", embeddings=" + embeddings +
               ", model='" + model + '\'' +
               ", timestamp=" + timestamp +
               ", usage=" + usage +
               ", metadata=" + metadata +
               '}';
    }

    /**
     * 单个嵌入结果
     */
    public static class Embedding {
        private final int index;
        private final List<Double> vector;
        private final String text;

        @JsonCreator
        public Embedding(@JsonProperty("index") int index,
                        @JsonProperty("vector") List<Double> vector,
                        @JsonProperty("text") String text) {
            this.index = index;
            this.vector = Objects.requireNonNull(vector, "Vector cannot be null");
            this.text = text;
        }

        public int getIndex() {
            return index;
        }

        public List<Double> getVector() {
            return vector;
        }

        public String getText() {
            return text;
        }

        public int getDimension() {
            return vector.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Embedding embedding = (Embedding) o;
            return index == embedding.index &&
                   Objects.equals(vector, embedding.vector) &&
                   Objects.equals(text, embedding.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, vector, text);
        }

        @Override
        public String toString() {
            return "Embedding{" +
                   "index=" + index +
                   ", vector=" + (vector != null ? vector.size() + "D" : "null") +
                   ", text='" + text + '\'' +
                   '}';
        }
    }

    /**
     * 使用量统计
     */
    public static class Usage {
        private final Integer promptTokens;
        private final Integer totalTokens;

        @JsonCreator
        public Usage(@JsonProperty("promptTokens") Integer promptTokens,
                    @JsonProperty("totalTokens") Integer totalTokens) {
            this.promptTokens = promptTokens;
            this.totalTokens = totalTokens;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Usage usage = (Usage) o;
            return Objects.equals(promptTokens, usage.promptTokens) &&
                   Objects.equals(totalTokens, usage.totalTokens);
        }

        @Override
        public int hashCode() {
            return Objects.hash(promptTokens, totalTokens);
        }

        @Override
        public String toString() {
            return "Usage{" +
                   "promptTokens=" + promptTokens +
                   ", totalTokens=" + totalTokens +
                   '}';
        }
    }

    /**
     * 嵌入响应构建器
     */
    public static class Builder {
        private String id;
        private List<Embedding> embeddings;
        private String model;
        private LocalDateTime timestamp;
        private Usage usage;
        private Map<String, Object> metadata;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder embeddings(List<Embedding> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        public Builder addEmbedding(Embedding embedding) {
            if (this.embeddings == null) {
                this.embeddings = new java.util.ArrayList<>();
            }
            this.embeddings.add(embedding);
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public EmbeddingResponse build() {
            return new EmbeddingResponse(id, embeddings, model, timestamp, usage, metadata);
        }
    }
}
