package io.nebula.ai.spring.chat;

import io.nebula.ai.core.exception.ChatException;
import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatRequest;
import io.nebula.ai.core.model.ChatResponse;
import io.nebula.ai.core.model.ToolCall;
import io.nebula.ai.core.model.ToolDefinition;
import io.nebula.ai.spring.config.AIProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * tool use 的请求侧下发与响应侧回填
 * <p>
 * 关注三件事：工具声明确实进了发给模型的 Prompt；工具执行权没有被交给框架；
 * TOOL / 携带 toolCalls 的 ASSISTANT 消息能正确映射回 Spring AI 的消息类型。
 */
class SpringAIChatServiceToolTest {

    private RecordingChatModel chatModel;
    private SpringAIChatService chatService;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        chatModel = new RecordingChatModel();
        chatService = new SpringAIChatService(builder, chatModel, new AIProperties());
    }

    @Test
    void toolsInRequest_areSentAsToolDefinitionsWithSchema() {
        chatModel.scriptCall(textResponse("好的"));

        chatService.chat(requestWithTools("auto"));

        OpenAiChatOptions options = (OpenAiChatOptions) chatModel.lastCall().getOptions();
        assertThat(options).isNotNull();
        assertThat(options.getToolCallbacks()).hasSize(1);

        org.springframework.ai.tool.definition.ToolDefinition definition =
                options.getToolCallbacks().get(0).getToolDefinition();
        assertThat(definition.name()).isEqualTo("get_weather");
        assertThat(definition.description()).isEqualTo("查询指定城市的天气");
        assertThat(definition.inputSchema()).contains("\"city\"");
        assertThat(options.getToolChoice()).isEqualTo("auto");
    }

    /**
     * 工具执行权必须留在应用侧：下发的 callback 只有定义没有实现，
     * 任何人（含框架自身）把它调起来都要当场失败，而不是把假结果喂回模型
     */
    @Test
    void declaredToolCallback_refusesExecutionInsteadOfReturningFakeResult() {
        chatModel.scriptCall(textResponse("好的"));

        chatService.chat(requestWithTools(null));

        OpenAiChatOptions options = (OpenAiChatOptions) chatModel.lastCall().getOptions();
        ToolCallback callback = options.getToolCallbacks().get(0);
        assertThatThrownBy(() -> callback.call("{\"city\":\"北京\"}"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("get_weather");
    }

    @Test
    void noTools_keepsPromptOptionsUnchanged() {
        chatModel.scriptCall(textResponse("你好"));

        chatService.chat(ChatRequest.simple("你好"));

        // 无模型覆写也无工具时不附带 options，行为与引入 tool 能力之前一致
        assertThat(chatModel.lastCall().getOptions()).isNull();
    }

    @Test
    void modelOnlyRequest_stillCarriesModelOverrideWithoutTools() {
        chatModel.scriptCall(textResponse("你好"));

        chatService.chat(ChatRequest.simple("你好", "qwen3-4b"));

        OpenAiChatOptions options = (OpenAiChatOptions) chatModel.lastCall().getOptions();
        assertThat(options.getModel()).isEqualTo("qwen3-4b");
        assertThat(options.getToolCallbacks()).isNullOrEmpty();
    }

    @Test
    void toolCallsInResponse_areMappedBackWithArgumentsVerbatim() {
        chatModel.scriptCall(toolCallResponse());

        ChatResponse response = chatService.chat(ChatRequest.simple("北京天气"));

        assertThat(response.getFinishReason()).isEqualTo("tool_calls");
        assertThat(response.getToolCalls()).hasSize(1);
        ToolCall toolCall = response.getToolCalls().get(0);
        assertThat(toolCall.getId()).isEqualTo("call_1");
        assertThat(toolCall.getName()).isEqualTo("get_weather");
        assertThat(toolCall.getArgumentsJson()).isEqualTo("{\"city\":\"北京\"}");
    }

    @Test
    void toolResponseMessage_isConvertedInsteadOfThrowing() {
        chatModel.scriptCall(textResponse("北京今天晴"));

        chatService.chat(ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system("你是天气助手"),
                        ChatMessage.user("北京天气"),
                        ChatMessage.assistantToolCalls("",
                                List.of(new ToolCall("call_1", "get_weather", "{\"city\":\"北京\"}"))),
                        ChatMessage.toolResponse("call_1", "get_weather", "晴, 26 度")))
                .build());

        List<Message> sent = chatModel.lastCall().getInstructions();
        assertThat(sent).hasSize(4);
        assertThat(sent.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(sent.get(1)).isInstanceOf(UserMessage.class);

        AssistantMessage assistant = (AssistantMessage) sent.get(2);
        assertThat(assistant.hasToolCalls()).isTrue();
        assertThat(assistant.getToolCalls()).containsExactly(
                new AssistantMessage.ToolCall("call_1", "function", "get_weather", "{\"city\":\"北京\"}"));

        ToolResponseMessage toolResponse = (ToolResponseMessage) sent.get(3);
        assertThat(toolResponse.getResponses()).containsExactly(
                new ToolResponseMessage.ToolResponse("call_1", "get_weather", "晴, 26 度"));
    }

    @Test
    void toolRole_isAcceptedTheSameWayAsToolResponseRole() {
        chatModel.scriptCall(textResponse("好"));

        chatService.chat(ChatRequest.builder()
                .messages(List.of(new ChatMessage(null, ChatMessage.MessageRole.TOOL,
                        "晴, 26 度", null, null, "call_1", "get_weather", null)))
                .build());

        assertThat(chatModel.lastCall().getInstructions().get(0))
                .isInstanceOf(ToolResponseMessage.class);
    }

    /**
     * 缺 toolCallId 的 TOOL 消息一定要当场报错：模型无法把结果与调用配对，
     * 放行只会换来一次语义错乱的对话而不是一个明确的失败
     */
    @Test
    void toolMessageWithoutToolCallId_failsFast() {
        assertThatThrownBy(() -> chatService.chat(ChatRequest.builder()
                .messages(List.of(new ChatMessage(null, ChatMessage.MessageRole.TOOL_RESPONSE,
                        "晴", null, null, null, "get_weather", null)))
                .build()))
                .isInstanceOf(ChatException.class)
                .hasMessageContaining("toolCallId");
    }

    @Test
    void assistantMessageWithoutToolCalls_staysPlainAssistantMessage() {
        chatModel.scriptCall(textResponse("好"));

        chatService.chat(ChatRequest.builder()
                .messages(List.of(ChatMessage.assistant("上次我说过了")))
                .build());

        AssistantMessage assistant = (AssistantMessage) chatModel.lastCall().getInstructions().get(0);
        assertThat(assistant.hasToolCalls()).isFalse();
        assertThat(assistant.getText()).isEqualTo("上次我说过了");
    }

    private ChatRequest requestWithTools(String toolChoice) {
        return ChatRequest.builder()
                .messages(List.of(ChatMessage.user("北京天气")))
                .tools(List.of(ToolDefinition.builder()
                        .name("get_weather")
                        .description("查询指定城市的天气")
                        .parametersSchema(Map.of(
                                "type", "object",
                                "properties", Map.of("city", Map.of("type", "string"))))
                        .build()))
                .toolChoice(toolChoice)
                .build();
    }

    private static org.springframework.ai.chat.model.ChatResponse textResponse(String text) {
        return new org.springframework.ai.chat.model.ChatResponse(
                List.of(new Generation(new AssistantMessage(text),
                        ChatGenerationMetadata.builder().finishReason("stop").build())),
                ChatResponseMetadata.builder().usage(new DefaultUsage(1, 2, 3)).build());
    }

    private static org.springframework.ai.chat.model.ChatResponse toolCallResponse() {
        AssistantMessage output = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call_1", "function", "get_weather", "{\"city\":\"北京\"}")))
                .build();
        return new org.springframework.ai.chat.model.ChatResponse(
                List.of(new Generation(output,
                        ChatGenerationMetadata.builder().finishReason("tool_calls").build())),
                ChatResponseMetadata.builder().usage(new DefaultUsage(1, 2, 3)).build());
    }
}
