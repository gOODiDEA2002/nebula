package io.nebula.ai.spring.chat;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.exception.ChatException;
import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatRequest;
import io.nebula.ai.core.model.ChatResponse;

import io.nebula.ai.spring.config.AIProperties;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
/**
 * 基于Spring AI的聊天服务实现
 */
@Slf4j
public class SpringAIChatService implements ChatService {

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final AIProperties aiProperties;

    @Autowired
    public SpringAIChatService(ChatClient.Builder chatClientBuilder, ChatModel chatModel,
                                AIProperties aiProperties) {
        this.chatClient = chatClientBuilder.build();
        this.chatModel = chatModel;
        this.aiProperties = aiProperties;
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
            log.debug("发送聊天请求: {} 条消息, model={}", request.getMessages().size(), request.getModel());

            List<org.springframework.ai.chat.messages.Message> springAiMessages = request.getMessages()
                    .stream()
                    .map(this::convertToSpringAiMessage)
                    .collect(Collectors.toList());

            Prompt prompt;
            if (request.getModel() != null && !request.getModel().isBlank()) {
                prompt = new Prompt(springAiMessages,
                        OpenAiChatOptions.builder().model(request.getModel()).build());
            } else {
                prompt = new Prompt(springAiMessages);
            }

            org.springframework.ai.chat.model.ChatResponse springResponse = chatModel.call(prompt);

            String responseContent = springResponse.getResult().getOutput().getText();
            String usedModel = request.getModel() != null ? request.getModel() : getCurrentModel();

            return ChatResponse.builder()
                    .content(responseContent)
                    .timestamp(LocalDateTime.now())
                    .model(usedModel)
                    .usage(new ChatResponse.Usage(
                            springResponse.getMetadata().getUsage().getPromptTokens(),
                            springResponse.getMetadata().getUsage().getCompletionTokens(),
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
        try {
            log.debug("开始流式聊天(多消息): {} 条消息", messages.size());

            List<org.springframework.ai.chat.messages.Message> springAiMessages = messages.stream()
                    .map(this::convertToSpringAiMessage)
                    .collect(Collectors.toList());

            Prompt prompt = new Prompt(springAiMessages);
            streamWithPrompt(prompt, callback);

        } catch (Exception e) {
            log.error("流式聊天调用失败: {}", e.getMessage(), e);
            callback.onError(new ChatException("流式聊天调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public void chatStream(ChatRequest request, ChatStreamCallback callback) {
        try {
            log.debug("开始流式聊天(请求): {} 条消息, model={}", request.getMessages().size(), request.getModel());

            List<org.springframework.ai.chat.messages.Message> springAiMessages = request.getMessages().stream()
                    .map(this::convertToSpringAiMessage)
                    .collect(Collectors.toList());

            Prompt prompt;
            if (request.getModel() != null && !request.getModel().isBlank()) {
                prompt = new Prompt(springAiMessages,
                        OpenAiChatOptions.builder().model(request.getModel()).build());
            } else {
                prompt = new Prompt(springAiMessages);
            }
            streamWithPrompt(prompt, callback);

        } catch (Exception e) {
            log.error("流式聊天调用失败: {}", e.getMessage(), e);
            callback.onError(new ChatException("流式聊天调用失败: " + e.getMessage(), e));
        }
    }

    private void streamWithPrompt(Prompt prompt, ChatStreamCallback callback) {
        chatModel.stream(prompt)
                .doOnNext(response -> {
                    String chunk = response.getResult() != null && response.getResult().getOutput() != null
                            ? response.getResult().getOutput().getText() : "";
                    if (chunk != null) {
                        callback.onChunk(chunk);
                    }
                })
                .reduce("", (accumulator, response) -> {
                    String text = response.getResult() != null && response.getResult().getOutput() != null
                            ? response.getResult().getOutput().getText() : "";
                    return accumulator + (text != null ? text : "");
                })
                .doOnSuccess(fullResponse -> {
                    ChatResponse chatResponse = ChatResponse.builder()
                            .content(fullResponse)
                            .timestamp(LocalDateTime.now())
                            .model(getCurrentModel())
                            .build();
                    callback.onComplete(chatResponse);
                })
                .doOnError(callback::onError)
                .subscribe();
    }

    @Override
    public boolean isAvailable() {
        return chatModel != null && aiProperties.isEnabled();
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
        return aiProperties.getOpenai().getChat().getOptions().getModel();
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
