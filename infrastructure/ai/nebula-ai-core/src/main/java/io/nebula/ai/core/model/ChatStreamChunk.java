package io.nebula.ai.core.model;

import java.util.Objects;

/**
 * 流式分片：文本增量或工具调用增量，二者其一
 * <p>
 * 一个分片只承载一种增量，避免消费方在同一对象上同时处理两条语义不同的流。
 * {@link #getFinishReason()} 只在整轮的最后一个分片上出现。
 * <p>
 * <b>能力边界：</b>流式场景下并行多工具调用的分片聚合不提供保证——上游供应商在流式
 * 协议里对多工具分片的切分方式并不统一，框架不做跨分片拼装。流式工具调用属实验能力，
 * 生产用途请走非流式的 {@code chat(ChatRequest)} 拿完整 {@link ToolCall} 列表。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ChatStreamChunk {

    private final String contentDelta;
    private final ToolCall toolCallDelta;
    private final String finishReason;

    public ChatStreamChunk(String contentDelta, ToolCall toolCallDelta, String finishReason) {
        this.contentDelta = contentDelta;
        this.toolCallDelta = toolCallDelta;
        this.finishReason = finishReason;
    }

    /**
     * 创建文本增量分片
     */
    public static ChatStreamChunk content(String contentDelta, String finishReason) {
        return new ChatStreamChunk(contentDelta, null, finishReason);
    }

    /**
     * 创建工具调用增量分片
     */
    public static ChatStreamChunk toolCall(ToolCall toolCallDelta, String finishReason) {
        return new ChatStreamChunk(null, toolCallDelta, finishReason);
    }

    public String getContentDelta() {
        return contentDelta;
    }

    public ToolCall getToolCallDelta() {
        return toolCallDelta;
    }

    public String getFinishReason() {
        return finishReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatStreamChunk that = (ChatStreamChunk) o;
        return Objects.equals(contentDelta, that.contentDelta) &&
               Objects.equals(toolCallDelta, that.toolCallDelta) &&
               Objects.equals(finishReason, that.finishReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentDelta, toolCallDelta, finishReason);
    }

    @Override
    public String toString() {
        return "ChatStreamChunk{" +
               "contentDelta='" + contentDelta + '\'' +
               ", toolCallDelta=" + toolCallDelta +
               ", finishReason='" + finishReason + '\'' +
               '}';
    }
}
