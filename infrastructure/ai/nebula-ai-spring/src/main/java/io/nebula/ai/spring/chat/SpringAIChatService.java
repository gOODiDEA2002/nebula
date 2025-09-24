package io.nebula.ai.spring.chat;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.exception.ChatException;
import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatRequest;
import io.nebula.ai.core.model.ChatResponse;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 基于Spring AI的聊天服务实现
 */
@Service
public class SpringAIChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(SpringAIChatService.class);

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    @Autowired
    public SpringAIChatService(ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
        this.chatClient = chatClientBuilder.build();
        this.chatModel = chatModel;
    }

    @Override
    public ChatResponse chat(String message) {
        try {
            log.debug("发送聊天消息: {}", message);

            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            return ChatResponse.builder()
                    .content(response)
                    .timestamp(LocalDateTime.now())
                    .model(getCurrentModel())
                    .build();

        } catch (Exception e) {
            log.error("聊天调用失败: {}", e.getMessage(), e);
            throw new ChatException("聊天调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        try {
            log.debug("发送聊天消息列表: {} 条消息", messages.size());

            // 使用ChatModel API直接处理消息列表
            List<org.springframework.ai.chat.messages.Message> springAiMessages = messages.stream()
                    .map(this::convertToSpringAiMessage)
                    .collect(java.util.stream.Collectors.toList());

            Prompt prompt = new Prompt(springAiMessages);
            org.springframework.ai.chat.model.ChatResponse springResponse = chatModel.call(prompt);

            String responseContent = springResponse.getResult().getOutput().getText();

            return ChatResponse.builder()
                    .content(responseContent)
                    .timestamp(LocalDateTime.now())
                    .model(getCurrentModel())
                    .build();

        } catch (Exception e) {
            log.error("聊天调用失败: {}", e.getMessage(), e);
            throw new ChatException("聊天调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            log.debug("发送聊天请求: {} 条消息", request.getMessages().size());

            // 使用更低级的ChatModel API来支持更多配置选项
            List<org.springframework.ai.chat.messages.Message> springAiMessages = request.getMessages()
                    .stream()
                    .map(this::convertToSpringAiMessage)
                    .collect(Collectors.toList());

            Prompt prompt = new Prompt(springAiMessages);
            org.springframework.ai.chat.model.ChatResponse springResponse = chatModel.call(prompt);

            String responseContent = springResponse.getResult().getOutput().getText();

            return ChatResponse.builder()
                    .content(responseContent)
                    .timestamp(LocalDateTime.now())
                    .model(getCurrentModel())
                    .usage(new ChatResponse.Usage(
                            springResponse.getMetadata().getUsage().getPromptTokens(),
                            springResponse.getMetadata().getUsage().getPromptTokens(), // 临时使用相同值
                            springResponse.getMetadata().getUsage().getTotalTokens()
                    ))
                    .finishReason(springResponse.getResult().getMetadata().getFinishReason())
                    .build();

        } catch (Exception e) {
            log.error("聊天调用失败: {}", e.getMessage(), e);
            throw new ChatException("聊天调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<ChatResponse> chatAsync(String message) {
        return CompletableFuture.supplyAsync(() -> chat(message));
    }

    @Override
    public CompletableFuture<ChatResponse> chatAsync(List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> chat(messages));
    }

    @Override
    public CompletableFuture<ChatResponse> chatAsync(ChatRequest request) {
        return CompletableFuture.supplyAsync(() -> chat(request));
    }

    @Override
    public void chatStream(String message, ChatStreamCallback callback) {
        try {
            log.debug("开始流式聊天: {}", message);

            chatClient.prompt()
                    .user(message)
                    .stream()
                    .content()
                    .doOnNext(callback::onChunk)
                    .reduce("", (accumulator, chunk) -> accumulator + chunk)
                    .doOnSuccess(fullResponse -> {
                        ChatResponse response = ChatResponse.builder()
                                .content(fullResponse)
                                .timestamp(LocalDateTime.now())
                                .model(getCurrentModel())
                                .build();
                        callback.onComplete(response);
                    })
                    .doOnError(callback::onError)
                    .subscribe();

        } catch (Exception e) {
            log.error("流式聊天调用失败: {}", e.getMessage(), e);
            callback.onError(new ChatException("流式聊天调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public void chatStream(List<ChatMessage> messages, ChatStreamCallback callback) {
        // 简化实现：转换为字符串形式
        String combinedMessage = messages.stream()
                .filter(msg -> msg.getRole() == ChatMessage.MessageRole.USER)
                .map(ChatMessage::getContent)
                .collect(Collectors.joining("\n"));
        
        chatStream(combinedMessage, callback);
    }

    @Override
    public void chatStream(ChatRequest request, ChatStreamCallback callback) {
        // 简化实现：使用第一个用户消息
        String userMessage = request.getMessages().stream()
                .filter(msg -> msg.getRole() == ChatMessage.MessageRole.USER)
                .map(ChatMessage::getContent)
                .findFirst()
                .orElse("Hello");
        
        chatStream(userMessage, callback);
    }

    @Override
    public boolean isAvailable() {
        try {
            // 发送简单的测试消息
            chat("test");
            return true;
        } catch (Exception e) {
            log.warn("聊天服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getSupportedModels() {
        // Spring AI 目前没有标准的方法获取支持的模型列表
        // 这里返回常见的OpenAI模型
        return List.of(
                "gpt-3.5-turbo",
                "gpt-4",
                "gpt-4-turbo",
                "gpt-4o",
                "gpt-4o-mini"
        );
    }

    @Override
    public String getCurrentModel() {
        // 从Spring AI ChatModel获取当前模型
        // 这是一个简化的实现，实际可能需要根据具体的ChatModel实现来获取
        return "gpt-3.5-turbo"; // 默认值
    }

    /**
     * 将Nebula ChatMessage转换为Spring AI Message
     */
    private org.springframework.ai.chat.messages.Message convertToSpringAiMessage(ChatMessage message) {
        return switch (message.getRole()) {
            case USER -> new org.springframework.ai.chat.messages.UserMessage(message.getContent());
            case SYSTEM -> new org.springframework.ai.chat.messages.SystemMessage(message.getContent());
            case ASSISTANT -> new org.springframework.ai.chat.messages.AssistantMessage(message.getContent());
            default -> throw new ChatException("不支持的消息角色: " + message.getRole());
        };
    }
}
