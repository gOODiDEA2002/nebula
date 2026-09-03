package io.nebula.ai.rag.pipeline;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatRequest;
import io.nebula.ai.core.model.ChatResponse;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code ChatServiceStreamingAnswerGenerator} 回调到 {@link reactor.core.publisher.Flux} 的信号映射（R4 §5.1、§10）
 * <p>
 * 覆盖：onChunk/onComplete/onError 到 Flux 信号的映射；下游取消后 onChunk 被丢弃。
 */
class ChatServiceStreamingAnswerGeneratorTest {

    @Test
    void constructor_rejectsNullChatService() {
        assertThatThrownBy(() -> new ChatServiceStreamingAnswerGenerator(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onChunkThenComplete_mapsToNextSignalsThenComplete() {
        DeferredChatService chat = new DeferredChatService();
        ChatServiceStreamingAnswerGenerator generator = new ChatServiceStreamingAnswerGenerator(chat);

        StepVerifier.create(generator.generateStream("prompt"))
                .then(() -> chat.emitChunk("Hello "))
                .expectNext("Hello ")
                .then(() -> chat.emitChunk("World"))
                .expectNext("World")
                .then(() -> chat.emitComplete())
                .verifyComplete();

        assertThat(chat.lastPrompt).isEqualTo("prompt");
    }

    @Test
    void onError_mapsToErrorSignal() {
        DeferredChatService chat = new DeferredChatService();
        ChatServiceStreamingAnswerGenerator generator = new ChatServiceStreamingAnswerGenerator(chat);

        RuntimeException boom = new RuntimeException("上游炸了");

        StepVerifier.create(generator.generateStream("prompt"))
                .then(() -> chat.emitChunk("partial"))
                .expectNext("partial")
                .then(() -> chat.emitError(boom))
                .expectErrorMatches(err -> err == boom)
                .verify();
    }

    @Test
    void cancel_discardsSubsequentChunks() {
        DeferredChatService chat = new DeferredChatService();
        ChatServiceStreamingAnswerGenerator generator = new ChatServiceStreamingAnswerGenerator(chat);

        // 取消订阅后，上游仍回调 onChunk：应被丢弃，不抛异常、不再有信号
        StepVerifier.create(generator.generateStream("prompt"))
                .then(() -> chat.emitChunk("a"))
                .expectNext("a")
                .thenCancel()
                .verify();

        // 取消后继续回调：不得抛出，也不应有任何观察副作用
        chat.emitChunk("b");
        chat.emitComplete();
        assertThat(chat.callbackInvoked.get()).isGreaterThanOrEqualTo(1);
    }

    /**
     * 延迟发射的 {@link ChatService} 测试替身：{@code chatStream} 只捕获回调，由测试驱动发射时机。
     */
    private static final class DeferredChatService implements ChatService {

        private volatile ChatStreamCallback callback;
        private volatile String lastPrompt;
        private final AtomicInteger callbackInvoked = new AtomicInteger();

        void emitChunk(String chunk) {
            callbackInvoked.incrementAndGet();
            callback.onChunk(chunk);
        }

        void emitComplete() {
            callback.onComplete(null);
        }

        void emitError(Throwable error) {
            callback.onError(error);
        }

        @Override
        public void chatStream(String message, ChatStreamCallback callback) {
            this.lastPrompt = message;
            this.callback = callback;
        }

        // ---- 未使用的接口方法 ----

        @Override
        public ChatResponse chat(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChatResponse chat(List<ChatMessage> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<ChatResponse> chatAsync(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<ChatResponse> chatAsync(List<ChatMessage> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<ChatResponse> chatAsync(ChatRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void chatStream(List<ChatMessage> messages, ChatStreamCallback callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void chatStream(ChatRequest request, ChatStreamCallback callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public List<String> getSupportedModels() {
            return List.of();
        }

        @Override
        public String getCurrentModel() {
            return "test";
        }
    }
}
