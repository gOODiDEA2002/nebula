package io.nebula.ai.core.chat;

import io.nebula.ai.core.model.ChatRequest;
import io.nebula.ai.core.model.ChatStreamChunk;

import reactor.core.publisher.Flux;

/**
 * 响应式聊天服务接口
 * <p>
 * 与 {@link ChatService} 分开定义，既有实现不必被迫升级；
 * 同时实现两者的服务（如 {@code SpringAIChatService}）中，
 * {@link ChatService#chatStream} 的回调形态内部订阅本接口返回的 Flux，
 * 保证两条流式路径同源，不会各自演化出不同行为。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface ReactiveChatService {

    /**
     * 流式聊天
     *
     * @param request 聊天请求
     * @return 分片流；订阅时才真正发起调用（冷流）
     */
    Flux<ChatStreamChunk> chatStream(ChatRequest request);
}
