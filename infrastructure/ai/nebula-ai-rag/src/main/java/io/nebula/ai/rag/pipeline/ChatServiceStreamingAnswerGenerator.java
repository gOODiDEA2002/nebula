package io.nebula.ai.rag.pipeline;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.chat.ChatService.ChatStreamCallback;
import io.nebula.ai.core.model.ChatResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用 {@link ChatService#chatStream} 桥接为 {@link Flux} 的默认流式适配器（R4 §5.1，单路径 R4-D6）
 * <p>
 * 只用 {@code ChatService.chatStream(String, ChatStreamCallback)} 一条路径，<b>不</b>依赖
 * {@code ReactiveChatService}（总纲红线）。回调式 API 无法真正中止上游请求，下游取消时置取消标记，
 * 后续 {@code onChunk} 一律丢弃。背压用 {@link FluxSink.OverflowStrategy#BUFFER}：LLM 出字速率远低于
 * 消费者，缓冲上界即单次答案长度，不用 {@code DROP}/{@code LATEST} 以免丢字。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ChatServiceStreamingAnswerGenerator implements StreamingAnswerGenerator {

    private final ChatService chatService;

    public ChatServiceStreamingAnswerGenerator(ChatService chatService) {
        if (chatService == null) {
            throw new IllegalArgumentException("ChatService 不能为空");
        }
        this.chatService = chatService;
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        return Flux.create(sink -> {
            AtomicBoolean cancelled = new AtomicBoolean(false);
            sink.onCancel(() -> cancelled.set(true));
            chatService.chatStream(prompt, new ChatStreamCallback() {
                @Override
                public void onChunk(String chunk) {
                    if (!cancelled.get()) {
                        sink.next(chunk);
                    }
                }

                @Override
                public void onComplete(ChatResponse response) {
                    if (!cancelled.get()) {
                        sink.complete();
                    }
                }

                @Override
                public void onError(Throwable error) {
                    if (!cancelled.get()) {
                        sink.error(error);
                    }
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}
