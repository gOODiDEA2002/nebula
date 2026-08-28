package io.nebula.ai.rag.pipeline;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.ChatResponse;

/**
 * 走 {@code ChatService} 的默认答案生成实现
 * <p>
 * 只做一件事：把提示词交给聊天服务并取回文本。超时、降级由管线统一负责，
 * 这里不重复实现一套，避免两处超时互相盖住导致排障时对不上时间线。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ChatServiceAnswerGenerator implements AnswerGenerator {

    private final ChatService chatService;

    public ChatServiceAnswerGenerator(ChatService chatService) {
        if (chatService == null) {
            throw new IllegalArgumentException("ChatService 不能为空");
        }
        this.chatService = chatService;
    }

    @Override
    public String generate(String prompt, long timeoutMillis) {
        ChatResponse response = chatService.chat(prompt);
        return response != null ? response.getContent() : null;
    }
}
