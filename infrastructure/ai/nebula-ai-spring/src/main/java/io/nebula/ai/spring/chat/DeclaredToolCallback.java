package io.nebula.ai.spring.chat;

import io.nebula.ai.core.model.ToolDefinition;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * 只声明、不执行的 {@link ToolCallback}
 * <p>
 * Spring AI 2.0 把工具声明下发给模型的唯一入口是
 * {@code ToolCallingManager#resolveToolDefinitions(ToolCallingChatOptions)}，
 * 它从 options 的 toolCallbacks 里取 {@code getToolDefinition()}。因此想只下发声明、
 * 不交出执行权，就得提供一个「有定义、无实现」的 callback。
 * <p>
 * {@link #call(String)} 恒抛异常而不是静默返回空串：框架层禁止自动工具循环
 * （流式并行多工具与取消语义在 2.0 上已证明不可靠），一旦有人绕开手动执行路径把它调起来，
 * 必须当场炸出来而不是把一个假结果喂回模型。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
class DeclaredToolCallback implements ToolCallback {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    /** 无入参时下发的最小合法 JSON Schema */
    private static final String EMPTY_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";

    private final org.springframework.ai.tool.definition.ToolDefinition delegate;

    DeclaredToolCallback(ToolDefinition definition) {
        this.delegate = org.springframework.ai.tool.definition.ToolDefinition.builder()
                .name(definition.getName())
                .description(definition.getDescription() != null ? definition.getDescription() : "")
                .inputSchema(toInputSchema(definition.getParametersSchema()))
                .build();
    }

    @Override
    public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
        return delegate;
    }

    @Override
    public String call(String toolInput) {
        throw new UnsupportedOperationException(
                "Nebula 不执行框架侧自动工具调用, 工具 " + delegate.name()
                        + " 需由应用取回 ChatResponse.toolCalls 后自行执行并回填 TOOL_RESPONSE 消息");
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }

    private static String toInputSchema(Map<String, Object> parametersSchema) {
        if (parametersSchema == null || parametersSchema.isEmpty()) {
            return EMPTY_SCHEMA;
        }
        return JSON_MAPPER.writeValueAsString(parametersSchema);
    }
}
