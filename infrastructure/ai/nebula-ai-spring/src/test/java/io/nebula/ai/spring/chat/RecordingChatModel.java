package io.nebula.ai.spring.chat;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录收到的 Prompt 并返回脚本化响应的 {@link ChatModel} 测试替身
 * <p>
 * 替身只负责「原样记录 + 按脚本返回」，不对入参做任何宽松处理：
 * 调用方断言的是它记录下来的真实 Prompt（消息类型、options、tool 声明），
 * 而不是替身自己编造的东西。
 */
class RecordingChatModel implements ChatModel {

    private final List<Prompt> calls = new ArrayList<>();
    private final List<Prompt> streams = new ArrayList<>();

    private ChatResponse callResponse = new ChatResponse(List.of());
    private List<ChatResponse> streamResponses = List.of();

    void scriptCall(ChatResponse response) {
        this.callResponse = response;
    }

    void scriptStream(List<ChatResponse> responses) {
        this.streamResponses = responses;
    }

    List<Prompt> recordedCalls() {
        return calls;
    }

    Prompt lastCall() {
        if (calls.isEmpty()) {
            throw new IllegalStateException("ChatModel.call 从未被调用");
        }
        return calls.get(calls.size() - 1);
    }

    Prompt lastStream() {
        if (streams.isEmpty()) {
            throw new IllegalStateException("ChatModel.stream 从未被调用");
        }
        return streams.get(streams.size() - 1);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        calls.add(prompt);
        return callResponse;
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        streams.add(prompt);
        return Flux.fromIterable(streamResponses);
    }
}
