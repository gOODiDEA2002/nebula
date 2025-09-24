package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 聊天响应模型
 */
public class ChatResponse {

    private final String id;
    private final String content;
    private final List<ChatMessage> messages;
    private final String model;
    private final LocalDateTime timestamp;
    private final Usage usage;
    private final String finishReason;
    private final Map<String, Object> metadata;

    @JsonCreator
    public ChatResponse(@JsonProperty("id") String id,
                       @JsonProperty("content") String content,
                       @JsonProperty("messages") List<ChatMessage> messages,
                       @JsonProperty("model") String model,
                       @JsonProperty("timestamp") LocalDateTime timestamp,
                       @JsonProperty("usage") Usage usage,
                       @JsonProperty("finishReason") String finishReason,
                       @JsonProperty("metadata") Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.messages = messages;
        this.model = model;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.usage = usage;
        this.finishReason = finishReason;
        this.metadata = metadata;
    }

    /**
     * 创建简单的聊天响应
     */
    public static ChatResponse simple(String content) {
        return new ChatResponse(null, content, null, null, null, null, null, null);
    }

    /**
     * 创建聊天响应构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public List<ChatMessage> getMessages() {
        return messages;
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

    public String getFinishReason() {
        return finishReason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatResponse that = (ChatResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(content, that.content) &&
               Objects.equals(messages, that.messages) &&
               Objects.equals(model, that.model) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(usage, that.usage) &&
               Objects.equals(finishReason, that.finishReason) &&
               Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, messages, model, timestamp, usage, finishReason, metadata);
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
               "id='" + id + '\'' +
               ", content='" + content + '\'' +
               ", messages=" + messages +
               ", model='" + model + '\'' +
               ", timestamp=" + timestamp +
               ", usage=" + usage +
               ", finishReason='" + finishReason + '\'' +
               ", metadata=" + metadata +
               '}';
    }

    /**
     * 使用量统计
     */
    public static class Usage {
        private final Integer promptTokens;
        private final Integer completionTokens;
        private final Integer totalTokens;

        @JsonCreator
        public Usage(@JsonProperty("promptTokens") Integer promptTokens,
                    @JsonProperty("completionTokens") Integer completionTokens,
                    @JsonProperty("totalTokens") Integer totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
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
                   Objects.equals(completionTokens, usage.completionTokens) &&
                   Objects.equals(totalTokens, usage.totalTokens);
        }

        @Override
        public int hashCode() {
            return Objects.hash(promptTokens, completionTokens, totalTokens);
        }

        @Override
        public String toString() {
            return "Usage{" +
                   "promptTokens=" + promptTokens +
                   ", completionTokens=" + completionTokens +
                   ", totalTokens=" + totalTokens +
                   '}';
        }
    }

    /**
     * 聊天响应构建器
     */
    public static class Builder {
        private String id;
        private String content;
        private List<ChatMessage> messages;
        private String model;
        private LocalDateTime timestamp;
        private Usage usage;
        private String finishReason;
        private Map<String, Object> metadata;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
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

        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ChatResponse build() {
            return new ChatResponse(id, content, messages, model, timestamp, 
                                  usage, finishReason, metadata);
        }
    }
}
