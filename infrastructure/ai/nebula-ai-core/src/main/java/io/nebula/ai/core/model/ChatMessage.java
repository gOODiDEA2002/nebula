package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
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
    private final String toolCallId;
    private final String toolName;
    private final List<ToolCall> toolCalls;

    /**
     * 兼容 2.1.0 及更早版本的构造函数（无 tool 字段）
     * <p>
     * 签名保持不变，行为等价于 toolCallId/toolName/toolCalls 为 null。
     */
    public ChatMessage(String id,
                      MessageRole role,
                      String content,
                      LocalDateTime timestamp,
                      Map<String, Object> metadata) {
        this(id, role, content, timestamp, metadata, null, null, null);
    }

    @JsonCreator
    public ChatMessage(@JsonProperty("id") String id,
                      @JsonProperty("role") MessageRole role,
                      @JsonProperty("content") String content,
                      @JsonProperty("timestamp") LocalDateTime timestamp,
                      @JsonProperty("metadata") Map<String, Object> metadata,
                      @JsonProperty("toolCallId") String toolCallId,
                      @JsonProperty("toolName") String toolName,
                      @JsonProperty("toolCalls") List<ToolCall> toolCalls) {
        this.id = id;
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.content = Objects.requireNonNull(content, "Content cannot be null");
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.metadata = metadata;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.toolCalls = toolCalls;
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

    /**
     * 创建携带工具调用的助手消息
     * <p>
     * 用于把上一轮模型返回的 tool_calls 原样放回下一轮上下文；模型据此才认得随后的
     * {@link #toolResponse} 消息属于哪次调用。content 可为空字符串。
     */
    public static ChatMessage assistantToolCalls(String content, List<ToolCall> toolCalls) {
        return new ChatMessage(null, MessageRole.ASSISTANT, content != null ? content : "",
                null, null, null, null, toolCalls);
    }

    /**
     * 创建工具执行结果消息
     *
     * @param toolCallId 对应 {@link ToolCall#getId()}，模型靠它把结果与调用配对
     * @param toolName   工具名
     * @param content    工具执行结果（文本形式）
     */
    public static ChatMessage toolResponse(String toolCallId, String toolName, String content) {
        return new ChatMessage(null, MessageRole.TOOL_RESPONSE, content, null, null,
                toolCallId, toolName, null);
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

    /**
     * TOOL / TOOL_RESPONSE 消息所对应的工具调用 ID
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * TOOL / TOOL_RESPONSE 消息所对应的工具名
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * ASSISTANT 消息携带的工具调用列表
     */
    public List<ToolCall> getToolCalls() {
        return toolCalls;
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
               Objects.equals(metadata, that.metadata) &&
               Objects.equals(toolCallId, that.toolCallId) &&
               Objects.equals(toolName, that.toolName) &&
               Objects.equals(toolCalls, that.toolCalls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role, content, timestamp, metadata, toolCallId, toolName, toolCalls);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
               "id='" + id + '\'' +
               ", role=" + role +
               ", content='" + content + '\'' +
               ", timestamp=" + timestamp +
               ", metadata=" + metadata +
               ", toolCallId='" + toolCallId + '\'' +
               ", toolName='" + toolName + '\'' +
               ", toolCalls=" + toolCalls +
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
