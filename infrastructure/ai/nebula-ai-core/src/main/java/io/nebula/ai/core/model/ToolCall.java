package io.nebula.ai.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 模型返回的工具调用
 * <p>
 * {@link #getArgumentsJson()} 原样保留模型输出的 JSON 字符串：框架不做解析也不做校验，
 * 参数结构由应用按自己的工具契约解释；提前解析成 Map 会在模型输出非法 JSON 时
 * 把「工具参数不对」变成「整轮对话失败」，也丢掉了原文这一排障依据。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ToolCall {

    private final String id;
    private final String name;
    private final String argumentsJson;

    @JsonCreator
    public ToolCall(@JsonProperty("id") String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("argumentsJson") String argumentsJson) {
        this.id = id;
        this.name = name;
        this.argumentsJson = argumentsJson;
    }

    /**
     * 创建工具调用构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolCall toolCall = (ToolCall) o;
        return Objects.equals(id, toolCall.id) &&
               Objects.equals(name, toolCall.name) &&
               Objects.equals(argumentsJson, toolCall.argumentsJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, argumentsJson);
    }

    @Override
    public String toString() {
        return "ToolCall{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", argumentsJson='" + argumentsJson + '\'' +
               '}';
    }

    /**
     * 工具调用构建器
     */
    public static class Builder {
        private String id;
        private String name;
        private String argumentsJson;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder argumentsJson(String argumentsJson) {
            this.argumentsJson = argumentsJson;
            return this;
        }

        public ToolCall build() {
            return new ToolCall(id, name, argumentsJson);
        }
    }
}
