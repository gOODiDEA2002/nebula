package io.nebula.ai.core.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * tool use 字段的 JSON 往返测试
 * <p>
 * 这些模型会跨进程传递（RPC/消息/落库），字段加进来却序列化不回来等于没加，
 * 所以每个新字段都要在往返后逐个断言，而不是只看对象能不能构造。
 * 同时钉住「无 tool 字段的旧报文仍可反序列化」，保证 2.1.0 的存量消息不被新版本读崩。
 */
class ChatToolModelSerializationTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void chatRequestWithTools_roundTripsEveryToolField() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(ChatMessage.user("北京今天天气怎么样")))
                .model("qwen3-4b")
                .addTool(ToolDefinition.builder()
                        .name("get_weather")
                        .description("查询指定城市的天气")
                        .parametersSchema(Map.of(
                                "type", "object",
                                "properties", Map.of("city", Map.of("type", "string"))))
                        .build())
                .toolChoice("auto")
                .build();

        ChatRequest restored = jsonMapper.readValue(
                jsonMapper.writeValueAsString(request), ChatRequest.class);

        assertThat(restored.getToolChoice()).isEqualTo("auto");
        assertThat(restored.getTools()).hasSize(1);
        ToolDefinition tool = restored.getTools().get(0);
        assertThat(tool.getName()).isEqualTo("get_weather");
        assertThat(tool.getDescription()).isEqualTo("查询指定城市的天气");
        assertThat(tool.getParametersSchema())
                .containsEntry("type", "object")
                .containsKey("properties");
        // 既有字段不能因为新增构造函数而丢失
        assertThat(restored.getModel()).isEqualTo("qwen3-4b");
        assertThat(restored.getMessages()).hasSize(1);
        assertThat(restored.getMessages().get(0).getContent()).isEqualTo("北京今天天气怎么样");
    }

    @Test
    void chatRequestWithoutTools_keepsToolFieldsNull() {
        ChatRequest request = ChatRequest.simple("你好");

        ChatRequest restored = jsonMapper.readValue(
                jsonMapper.writeValueAsString(request), ChatRequest.class);

        assertThat(restored.getTools()).isNull();
        assertThat(restored.getToolChoice()).isNull();
        assertThat(restored).isEqualTo(request);
    }

    @Test
    void legacyChatRequestJson_withoutToolFields_stillDeserializes() {
        String legacyJson = "{\"messages\":[{\"role\":\"USER\",\"content\":\"你好\"}],\"model\":\"gpt-4o\"}";

        ChatRequest restored = jsonMapper.readValue(legacyJson, ChatRequest.class);

        assertThat(restored.getModel()).isEqualTo("gpt-4o");
        assertThat(restored.getTools()).isNull();
        assertThat(restored.getToolChoice()).isNull();
    }

    @Test
    void chatResponseWithToolCalls_roundTripsArgumentsJsonVerbatim() {
        ChatResponse response = ChatResponse.builder()
                .content("")
                .finishReason("tool_calls")
                .toolCalls(List.of(new ToolCall("call_1", "get_weather", "{\"city\":\"北京\"}")))
                .build();

        ChatResponse restored = jsonMapper.readValue(
                jsonMapper.writeValueAsString(response), ChatResponse.class);

        assertThat(restored.getFinishReason()).isEqualTo("tool_calls");
        assertThat(restored.getToolCalls()).hasSize(1);
        ToolCall toolCall = restored.getToolCalls().get(0);
        assertThat(toolCall.getId()).isEqualTo("call_1");
        assertThat(toolCall.getName()).isEqualTo("get_weather");
        // 参数原文必须逐字保留，解析责任在应用侧
        assertThat(toolCall.getArgumentsJson()).isEqualTo("{\"city\":\"北京\"}");
    }

    @Test
    void chatMessageToolResponse_roundTripsToolCallIdAndName() {
        ChatMessage message = ChatMessage.toolResponse("call_1", "get_weather", "晴, 26 度");

        ChatMessage restored = jsonMapper.readValue(
                jsonMapper.writeValueAsString(message), ChatMessage.class);

        assertThat(restored.getRole()).isEqualTo(ChatMessage.MessageRole.TOOL_RESPONSE);
        assertThat(restored.getToolCallId()).isEqualTo("call_1");
        assertThat(restored.getToolName()).isEqualTo("get_weather");
        assertThat(restored.getContent()).isEqualTo("晴, 26 度");
        assertThat(restored.getToolCalls()).isNull();
    }

    @Test
    void chatMessageAssistantToolCalls_roundTripsToolCallList() {
        ChatMessage message = ChatMessage.assistantToolCalls("",
                List.of(new ToolCall("call_1", "get_weather", "{\"city\":\"北京\"}")));

        ChatMessage restored = jsonMapper.readValue(
                jsonMapper.writeValueAsString(message), ChatMessage.class);

        assertThat(restored.getRole()).isEqualTo(ChatMessage.MessageRole.ASSISTANT);
        assertThat(restored.getToolCalls())
                .containsExactly(new ToolCall("call_1", "get_weather", "{\"city\":\"北京\"}"));
        assertThat(restored.getToolCallId()).isNull();
    }

    @Test
    void legacyConstructors_remainSourceCompatibleAndToolFree() {
        ChatMessage message = new ChatMessage(null, ChatMessage.MessageRole.USER, "你好", null, null);
        ChatResponse response = new ChatResponse(null, "回答", null, "gpt-4o", null, null, "stop", null);
        ChatRequest request = new ChatRequest(List.of(message), "gpt-4o",
                null, null, null, null, null, null, null, null);

        assertThat(message.getToolCalls()).isNull();
        assertThat(message.getToolCallId()).isNull();
        assertThat(response.getToolCalls()).isNull();
        assertThat(request.getTools()).isNull();
        assertThat(request.getToolChoice()).isNull();
    }
}
