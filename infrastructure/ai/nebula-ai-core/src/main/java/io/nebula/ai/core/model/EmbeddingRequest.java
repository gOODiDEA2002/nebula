package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 嵌入请求模型
 */
public class EmbeddingRequest {

    private final List<String> texts;
    private final String model;
    private final Map<String, Object> options;

    @JsonCreator
    public EmbeddingRequest(@JsonProperty("texts") List<String> texts,
                           @JsonProperty("model") String model,
                           @JsonProperty("options") Map<String, Object> options) {
        this.texts = Objects.requireNonNull(texts, "Texts cannot be null");
        this.model = model;
        this.options = options;
    }

    /**
     * 创建简单的嵌入请求
     */
    public static EmbeddingRequest simple(String text) {
        return new EmbeddingRequest(List.of(text), null, null);
    }

    /**
     * 创建简单的嵌入请求（指定模型）
     */
    public static EmbeddingRequest simple(String text, String model) {
        return new EmbeddingRequest(List.of(text), model, null);
    }

    /**
     * 创建批量嵌入请求
     */
    public static EmbeddingRequest batch(List<String> texts) {
        return new EmbeddingRequest(texts, null, null);
    }

    /**
     * 创建批量嵌入请求（指定模型）
     */
    public static EmbeddingRequest batch(List<String> texts, String model) {
        return new EmbeddingRequest(texts, model, null);
    }

    /**
     * 创建嵌入请求构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public List<String> getTexts() {
        return texts;
    }

    public String getModel() {
        return model;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingRequest that = (EmbeddingRequest) o;
        return Objects.equals(texts, that.texts) &&
               Objects.equals(model, that.model) &&
               Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texts, model, options);
    }

    @Override
    public String toString() {
        return "EmbeddingRequest{" +
               "texts=" + texts +
               ", model='" + model + '\'' +
               ", options=" + options +
               '}';
    }

    /**
     * 嵌入请求构建器
     */
    public static class Builder {
        private List<String> texts;
        private String model;
        private Map<String, Object> options;

        public Builder texts(List<String> texts) {
            this.texts = texts;
            return this;
        }

        public Builder addText(String text) {
            if (this.texts == null) {
                this.texts = new java.util.ArrayList<>();
            }
            this.texts.add(text);
            return this;
        }

        public Builder model(String model) {
            this.model = model;
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

        public EmbeddingRequest build() {
            return new EmbeddingRequest(texts, model, options);
        }
    }
}
