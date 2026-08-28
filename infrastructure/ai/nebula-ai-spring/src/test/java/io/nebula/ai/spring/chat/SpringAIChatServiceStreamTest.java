package io.nebula.ai.spring.chat;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatRequest;
import io.nebula.ai.core.model.ChatResponse;
import io.nebula.ai.core.model.ChatStreamChunk;
import io.nebula.ai.spring.config.AIProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 流式出口一致性：callback 形态必须与 Flux 形态同源
 * <p>
 * 两条流式路径一旦各自实现，迟早会在分片切分、结束时机、异常传播上分叉，
 * 这里把「同一份脚本响应，两种消费方式结果一致」钉死。
 */
class SpringAIChatServiceStreamTest {

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
    void fluxChunks_carryContentDeltasAndFinishReasonOnLastChunk() {
        chatModel.scriptStream(List.of(
                textChunk("你", null), textChunk("好", null), textChunk("!", "stop")));

        List<ChatStreamChunk> chunks = chatService
                .chatStream(ChatRequest.simple("hi")).collectList().block();

        assertThat(chunks).hasSize(3);
        assertThat(chunks).extracting(ChatStreamChunk::getContentDelta)
                .containsExactly("你", "好", "!");
        assertThat(chunks.get(0).getFinishReason()).isNull();
        assertThat(chunks.get(2).getFinishReason()).isEqualTo("stop");
    }

    @Test
    void toolCallChunk_isEmittedAsSeparateChunk() {
        AssistantMessage output = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call_1", "function", "get_weather", "{\"city\":\"北京\"}")))
                .build();
        chatModel.scriptStream(List.of(new org.springframework.ai.chat.model.ChatResponse(
                List.of(new Generation(output,
                        ChatGenerationMetadata.builder().finishReason("tool_calls").build())))));

        List<ChatStreamChunk> chunks = chatService
                .chatStream(ChatRequest.simple("北京天气")).collectList().block();

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContentDelta()).isEmpty();
        assertThat(chunks.get(0).getToolCallDelta()).isNull();
        assertThat(chunks.get(1).getContentDelta()).isNull();
        assertThat(chunks.get(1).getToolCallDelta().getName()).isEqualTo("get_weather");
        assertThat(chunks.get(1).getToolCallDelta().getArgumentsJson()).isEqualTo("{\"city\":\"北京\"}");
        assertThat(chunks.get(1).getFinishReason()).isEqualTo("tool_calls");
    }

    @Test
    void callbackAndFlux_produceIdenticalContentSequence() {
        List<org.springframework.ai.chat.model.ChatResponse> script = List.of(
                textChunk("Java ", null), textChunk("是一门", null), textChunk("语言", "stop"));

        chatModel.scriptStream(script);
        List<String> fromFlux = chatService.chatStream(ChatRequest.simple("Java 是什么"))
                .map(ChatStreamChunk::getContentDelta)
                .collectList().block();

        chatModel.scriptStream(script);
        CollectingCallback callback = new CollectingCallback();
        chatService.chatStream("Java 是什么", callback);

        assertThat(callback.chunks).isEqualTo(fromFlux);
        assertThat(callback.completed.get()).isNotNull();
        assertThat(callback.completed.get().getContent()).isEqualTo("Java 是一门语言");
        assertThat(callback.error.get()).isNull();
    }

    @Test
    void messageListCallback_streamsThroughTheSameFluxSource() {
        chatModel.scriptStream(List.of(textChunk("答", null), textChunk("案", "stop")));

        CollectingCallback callback = new CollectingCallback();
        chatService.chatStream(List.of(ChatMessage.system("你是助手"), ChatMessage.user("问题")), callback);

        // 消息上下文没有被回调路径丢掉
        assertThat(chatModel.lastStream().getInstructions()).hasSize(2);
        assertThat(callback.chunks).containsExactly("答", "案");
        assertThat(callback.completed.get().getContent()).isEqualTo("答案");
    }

    @Test
    void streamError_reachesCallbackAndSkipsCompletion() {
        RecordingChatModel failing = new RecordingChatModel() {
            @Override
            public reactor.core.publisher.Flux<org.springframework.ai.chat.model.ChatResponse> stream(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                return reactor.core.publisher.Flux.error(new IllegalStateException("上游断流"));
            }
        };
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        ChatService service = new SpringAIChatService(builder, failing, new AIProperties());

        CollectingCallback callback = new CollectingCallback();
        service.chatStream(ChatRequest.simple("hi"), callback);

        assertThat(callback.error.get()).hasMessageContaining("上游断流");
        assertThat(callback.completed.get()).isNull();
    }

    private static org.springframework.ai.chat.model.ChatResponse textChunk(String text, String finishReason) {
        ChatGenerationMetadata metadata = finishReason != null
                ? ChatGenerationMetadata.builder().finishReason(finishReason).build()
                : ChatGenerationMetadata.NULL;
        return new org.springframework.ai.chat.model.ChatResponse(
                List.of(new Generation(new AssistantMessage(text), metadata)));
    }

    /**
     * 收集回调结果的替身：如实记录每一次 onChunk/onComplete/onError，不做任何合并或忽略
     */
    private static final class CollectingCallback implements ChatService.ChatStreamCallback {

        private final List<String> chunks = new ArrayList<>();
        private final AtomicReference<ChatResponse> completed = new AtomicReference<>();
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        @Override
        public void onChunk(String chunk) {
            chunks.add(chunk);
        }

        @Override
        public void onComplete(ChatResponse response) {
            completed.set(response);
        }

        @Override
        public void onError(Throwable throwable) {
            error.set(throwable);
        }
    }
}
