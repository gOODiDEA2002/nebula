package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 聊天消息模型
 */
public class ChatMessage {

    private final String id;
    private final MessageRole role;
    private final String content;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;

    @JsonCreator
    public ChatMessage(@JsonProperty("id") String id,
                      @JsonProperty("role") MessageRole role,
                      @JsonProperty("content") String content,
                      @JsonProperty("timestamp") LocalDateTime timestamp,
                      @JsonProperty("metadata") Map<String, Object> metadata) {
        this.id = id;
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.content = Objects.requireNonNull(content, "Content cannot be null");
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.metadata = metadata;
    }

    /**
     * 创建用户消息
     */
    public static ChatMessage user(String content) {
        return new ChatMessage(null, MessageRole.USER, content, null, null);
    }

    /**
     * 创建用户消息（带元数据）
     */
    public static ChatMessage user(String content, Map<String, Object> metadata) {
        return new ChatMessage(null, MessageRole.USER, content, null, metadata);
    }

    /**
     * 创建助手消息
     */
    public static ChatMessage assistant(String content) {
        return new ChatMessage(null, MessageRole.ASSISTANT, content, null, null);
    }

    /**
     * 创建助手消息（带元数据）
     */
    public static ChatMessage assistant(String content, Map<String, Object> metadata) {
        return new ChatMessage(null, MessageRole.ASSISTANT, content, null, metadata);
    }

    /**
     * 创建系统消息
     */
    public static ChatMessage system(String content) {
        return new ChatMessage(null, MessageRole.SYSTEM, content, null, null);
    }

    /**
     * 创建系统消息（带元数据）
     */
    public static ChatMessage system(String content, Map<String, Object> metadata) {
        return new ChatMessage(null, MessageRole.SYSTEM, content, null, metadata);
    }

    public String getId() {
        return id;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id) &&
               role == that.role &&
               Objects.equals(content, that.content) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role, content, timestamp, metadata);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
               "id='" + id + '\'' +
               ", role=" + role +
               ", content='" + content + '\'' +
               ", timestamp=" + timestamp +
               ", metadata=" + metadata +
               '}';
    }

    /**
     * 消息角色枚举
     */
    public enum MessageRole {
        /**
         * 用户消息
         */
        USER,

        /**
         * AI助手消息
         */
        ASSISTANT,

        /**
         * 系统消息
         */
        SYSTEM,

        /**
         * 工具调用消息
         */
        TOOL,

        /**
         * 工具响应消息
         */
        TOOL_RESPONSE
    }
}
