package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * 工具定义：下发给模型的 function 声明
 * <p>
 * 只描述工具「长什么样」，不携带任何可执行体。框架不代替应用执行工具调用，
 * 模型返回 {@link ToolCall} 后由应用自行执行并把结果作为
 * {@link ChatMessage.MessageRole#TOOL_RESPONSE} 消息回填到下一轮请求。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> parametersSchema;

    @JsonCreator
    public ToolDefinition(@JsonProperty("name") String name,
                          @JsonProperty("description") String description,
                          @JsonProperty("parametersSchema") Map<String, Object> parametersSchema) {
        this.name = Objects.requireNonNull(name, "Tool name cannot be null");
        this.description = description;
        this.parametersSchema = parametersSchema;
    }

    /**
     * 创建工具定义构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 工具入参的 JSON Schema，键值结构由调用方自行组织
     */
    public Map<String, Object> getParametersSchema() {
        return parametersSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolDefinition that = (ToolDefinition) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(parametersSchema, that.parametersSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, parametersSchema);
    }

    @Override
    public String toString() {
        return "ToolDefinition{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", parametersSchema=" + parametersSchema +
               '}';
    }

    /**
     * 工具定义构建器
     */
    public static class Builder {
        private String name;
        private String description;
        private Map<String, Object> parametersSchema;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parametersSchema(Map<String, Object> parametersSchema) {
            this.parametersSchema = parametersSchema;
            return this;
        }

        public ToolDefinition build() {
            return new ToolDefinition(name, description, parametersSchema);
        }
    }
}
