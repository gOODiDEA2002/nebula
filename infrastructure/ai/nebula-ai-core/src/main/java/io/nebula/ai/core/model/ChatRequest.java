package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 聊天请求模型
 */
public class ChatRequest {

    private final List<ChatMessage> messages;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final Double topP;
    private final Double frequencyPenalty;
    private final Double presencePenalty;
    private final List<String> stop;
    private final boolean stream;
    private final Map<String, Object> options;
    private final List<ToolDefinition> tools;
    private final String toolChoice;

    /**
     * 兼容 2.1.0 及更早版本的构造函数（无 tool 字段）
     * <p>
     * 签名保持不变，行为等价于 tools/toolChoice 为 null。
     */
    public ChatRequest(List<ChatMessage> messages,
                      String model,
                      Double temperature,
                      Integer maxTokens,
                      Double topP,
                      Double frequencyPenalty,
                      Double presencePenalty,
                      List<String> stop,
                      Boolean stream,
                      Map<String, Object> options) {
        this(messages, model, temperature, maxTokens, topP, frequencyPenalty,
             presencePenalty, stop, stream, options, null, null);
    }

    @JsonCreator
    public ChatRequest(@JsonProperty("messages") List<ChatMessage> messages,
                      @JsonProperty("model") String model,
                      @JsonProperty("temperature") Double temperature,
                      @JsonProperty("maxTokens") Integer maxTokens,
                      @JsonProperty("topP") Double topP,
                      @JsonProperty("frequencyPenalty") Double frequencyPenalty,
                      @JsonProperty("presencePenalty") Double presencePenalty,
                      @JsonProperty("stop") List<String> stop,
                      @JsonProperty("stream") Boolean stream,
                      @JsonProperty("options") Map<String, Object> options,
                      @JsonProperty("tools") List<ToolDefinition> tools,
                      @JsonProperty("toolChoice") String toolChoice) {
        this.messages = Objects.requireNonNull(messages, "Messages cannot be null");
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.frequencyPenalty = frequencyPenalty;
        this.presencePenalty = presencePenalty;
        this.stop = stop;
        this.stream = stream != null ? stream : false;
        this.options = options;
        this.tools = tools;
        this.toolChoice = toolChoice;
    }

    /**
     * 创建简单的聊天请求
     */
    public static ChatRequest simple(String message) {
        return new ChatRequest(
            List.of(ChatMessage.user(message)),
            null, null, null, null, null, null, null, null, null
        );
    }

    /**
     * 创建简单的聊天请求（指定模型）
     */
    public static ChatRequest simple(String message, String model) {
        return new ChatRequest(
            List.of(ChatMessage.user(message)),
            model, null, null, null, null, null, null, null, null
        );
    }

    /**
     * 创建聊天请求构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public String getModel() {
        return model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public List<String> getStop() {
        return stop;
    }

    public boolean isStream() {
        return stream;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    /**
     * 本轮可供模型调用的工具声明；为空表示普通对话，行为与未引入 tool 能力时完全一致
     */
    public List<ToolDefinition> getTools() {
        return tools;
    }

    /**
     * 工具选择策略，透传给底层模型（如 auto / none / required 或供应商自定义 JSON）
     */
    public String getToolChoice() {
        return toolChoice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRequest that = (ChatRequest) o;
        return stream == that.stream &&
               Objects.equals(messages, that.messages) &&
               Objects.equals(model, that.model) &&
               Objects.equals(temperature, that.temperature) &&
               Objects.equals(maxTokens, that.maxTokens) &&
               Objects.equals(topP, that.topP) &&
               Objects.equals(frequencyPenalty, that.frequencyPenalty) &&
               Objects.equals(presencePenalty, that.presencePenalty) &&
               Objects.equals(stop, that.stop) &&
               Objects.equals(options, that.options) &&
               Objects.equals(tools, that.tools) &&
               Objects.equals(toolChoice, that.toolChoice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messages, model, temperature, maxTokens, topP,
                          frequencyPenalty, presencePenalty, stop, stream, options,
                          tools, toolChoice);
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
               "messages=" + messages +
               ", model='" + model + '\'' +
               ", temperature=" + temperature +
               ", maxTokens=" + maxTokens +
               ", topP=" + topP +
               ", frequencyPenalty=" + frequencyPenalty +
               ", presencePenalty=" + presencePenalty +
               ", stop=" + stop +
               ", stream=" + stream +
               ", options=" + options +
               ", tools=" + tools +
               ", toolChoice='" + toolChoice + '\'' +
               '}';
    }

    /**
     * 聊天请求构建器
     */
    public static class Builder {
        private List<ChatMessage> messages;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private List<String> stop;
        private Boolean stream;
        private Map<String, Object> options;
        private List<ToolDefinition> tools;
        private String toolChoice;

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder addMessage(ChatMessage message) {
            if (this.messages == null) {
                this.messages = new java.util.ArrayList<>();
            }
            this.messages.add(message);
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        public Builder tools(List<ToolDefinition> tools) {
            this.tools = tools;
            return this;
        }

        public Builder addTool(ToolDefinition tool) {
            if (this.tools == null) {
                this.tools = new java.util.ArrayList<>();
            }
            this.tools.add(tool);
            return this;
        }

        public Builder toolChoice(String toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(messages, model, temperature, maxTokens, topP,
                                 frequencyPenalty, presencePenalty, stop, stream, options,
                                 tools, toolChoice);
        }
    }
}
