package io.nebula.ai.spring.chat;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.chat.ReactiveChatService;
import io.nebula.ai.core.exception.ChatException;
import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatRequest;
import io.nebula.ai.core.model.ChatResponse;
import io.nebula.ai.core.model.ChatStreamChunk;
import io.nebula.ai.core.model.ToolCall;
import io.nebula.ai.core.model.ToolDefinition;

import io.nebula.ai.spring.config.AIProperties;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;

import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
/**
 * 基于Spring AI的聊天服务实现
 */
@Slf4j
public class SpringAIChatService implements ChatService, ReactiveChatService {

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
                    .toolCalls(extractToolCalls(springResponse.getResult()))
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

            Prompt prompt = buildPrompt(request);

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
                    .toolCalls(extractToolCalls(springResponse.getResult()))
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

    /**
     * 流式聊天的唯一数据源
     * <p>
     * 三个回调形态的 {@code chatStream} 全部订阅本方法，避免两条流式路径各自演化。
     */
    @Override
    public Flux<ChatStreamChunk> chatStream(ChatRequest request) {
        Prompt prompt = buildPrompt(request);
        return chatModel.stream(prompt).concatMap(response -> Flux.fromIterable(toChunks(response)));
    }

    @Override
    public void chatStream(String message, ChatStreamCallback callback) {
        streamWithCallback(ChatRequest.simple(message), callback,
                () -> log.debug("开始流式聊天: {}", message));
    }

    @Override
    public void chatStream(List<ChatMessage> messages, ChatStreamCallback callback) {
        streamWithCallback(ChatRequest.builder().messages(messages).build(), callback,
                () -> log.debug("开始流式聊天(多消息): {} 条消息", messages.size()));
    }

    @Override
    public void chatStream(ChatRequest request, ChatStreamCallback callback) {
        streamWithCallback(request, callback,
                () -> log.debug("开始流式聊天(请求): {} 条消息, model={}",
                        request.getMessages().size(), request.getModel()));
    }

    /**
     * 回调形态到 Flux 的桥接：只订阅 {@link #chatStream(ChatRequest)}，不另建流实现
     */
    private void streamWithCallback(ChatRequest request, ChatStreamCallback callback, Runnable logBefore) {
        try {
            logBefore.run();

            StringBuilder accumulator = new StringBuilder();
            chatStream(request).subscribe(
                    chunk -> {
                        if (chunk.getContentDelta() != null) {
                            accumulator.append(chunk.getContentDelta());
                            callback.onChunk(chunk.getContentDelta());
                        }
                    },
                    callback::onError,
                    () -> callback.onComplete(ChatResponse.builder()
                            .content(accumulator.toString())
                            .timestamp(LocalDateTime.now())
                            .model(getCurrentModel())
                            .build()));

        } catch (Exception e) {
            log.error("流式聊天调用失败: {}", e.getMessage(), e);
            callback.onError(new ChatException("流式聊天调用失败: " + e.getMessage(), e));
        }
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
     * 构建 Spring AI Prompt
     * <p>
     * 只有在需要覆写模型或下发工具声明时才附带 options，保持无 tool 请求与既有行为一致。
     */
    private Prompt buildPrompt(ChatRequest request) {
        List<org.springframework.ai.chat.messages.Message> springAiMessages = request.getMessages()
                .stream()
                .map(this::convertToSpringAiMessage)
                .collect(Collectors.toList());

        OpenAiChatOptions options = buildOptions(request);
        return options != null ? new Prompt(springAiMessages, options) : new Prompt(springAiMessages);
    }

    /**
     * 构建请求侧选项
     * <p>
     * 工具以「只声明、不执行」的 {@link DeclaredToolCallback} 下发（见该类说明），
     * 因此 Spring AI 拿不到任何可执行体，框架自动工具循环被显式关掉，
     * 工具调用一律交由应用手动执行。
     *
     * @return 无需覆写模型也无工具时返回 null，表示沿用 ChatModel 的默认选项
     */
    private OpenAiChatOptions buildOptions(ChatRequest request) {
        boolean hasModel = request.getModel() != null && !request.getModel().isBlank();
        List<ToolDefinition> tools = request.getTools();
        boolean hasTools = tools != null && !tools.isEmpty();

        if (!hasModel && !hasTools) {
            return null;
        }

        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (hasModel) {
            builder.model(request.getModel());
        }
        if (hasTools) {
            List<ToolCallback> callbacks = tools.stream()
                    .map(tool -> (ToolCallback) new DeclaredToolCallback(tool))
                    .collect(Collectors.toList());
            builder.toolCallbacks(callbacks);
            if (request.getToolChoice() != null && !request.getToolChoice().isBlank()) {
                builder.toolChoice(request.getToolChoice());
            }
        }
        return builder.build();
    }

    /**
     * 把一条 Spring AI 流式响应拆成 Nebula 分片
     * <p>
     * 文本增量在前、工具调用增量在后，finishReason 挂在本条响应产出的最后一个分片上；
     * 本条响应什么增量都没有时，仍为 finishReason 单独产出一个分片，避免结束信号丢失。
     */
    private List<ChatStreamChunk> toChunks(org.springframework.ai.chat.model.ChatResponse response) {
        Generation generation = response.getResult();
        if (generation == null) {
            return List.of();
        }

        String finishReason = generation.getMetadata() != null
                ? generation.getMetadata().getFinishReason() : null;

        List<ChatStreamChunk> chunks = new ArrayList<>();
        AssistantMessage output = generation.getOutput();
        String text = output != null ? output.getText() : null;
        if (text != null) {
            chunks.add(ChatStreamChunk.content(text, null));
        }
        for (ToolCall toolCall : extractToolCalls(generation)) {
            chunks.add(ChatStreamChunk.toolCall(toolCall, null));
        }

        if (chunks.isEmpty()) {
            return finishReason != null
                    ? List.of(ChatStreamChunk.content(null, finishReason)) : List.of();
        }
        if (finishReason != null) {
            ChatStreamChunk last = chunks.remove(chunks.size() - 1);
            chunks.add(new ChatStreamChunk(last.getContentDelta(), last.getToolCallDelta(), finishReason));
        }
        return chunks;
    }

    /**
     * 从 Spring AI 生成结果提取工具调用
     */
    private List<ToolCall> extractToolCalls(Generation generation) {
        if (generation == null || generation.getOutput() == null) {
            return List.of();
        }
        List<AssistantMessage.ToolCall> springToolCalls = generation.getOutput().getToolCalls();
        if (springToolCalls == null || springToolCalls.isEmpty()) {
            return List.of();
        }
        return springToolCalls.stream()
                .map(toolCall -> new ToolCall(toolCall.id(), toolCall.name(), toolCall.arguments()))
                .collect(Collectors.toList());
    }

    /**
     * 将Nebula ChatMessage转换为Spring AI Message
     */
    private org.springframework.ai.chat.messages.Message convertToSpringAiMessage(ChatMessage message) {
        return switch (message.getRole()) {
            case USER -> new org.springframework.ai.chat.messages.UserMessage(message.getContent());
            case SYSTEM -> new org.springframework.ai.chat.messages.SystemMessage(message.getContent());
            case ASSISTANT -> toAssistantMessage(message);
            case TOOL, TOOL_RESPONSE -> toToolResponseMessage(message);
        };
    }

    /**
     * ASSISTANT 消息：携带 toolCalls 时必须原样带回，模型据此识别后续 TOOL_RESPONSE 的归属
     */
    private org.springframework.ai.chat.messages.Message toAssistantMessage(ChatMessage message) {
        List<ToolCall> toolCalls = message.getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return new AssistantMessage(message.getContent());
        }
        List<AssistantMessage.ToolCall> springToolCalls = toolCalls.stream()
                .map(toolCall -> new AssistantMessage.ToolCall(
                        toolCall.getId(), "function", toolCall.getName(), toolCall.getArgumentsJson()))
                .collect(Collectors.toList());
        return AssistantMessage.builder()
                .content(message.getContent())
                .toolCalls(springToolCalls)
                .build();
    }

    /**
     * TOOL / TOOL_RESPONSE 消息：映射为 Spring AI 的 ToolResponseMessage
     */
    private org.springframework.ai.chat.messages.Message toToolResponseMessage(ChatMessage message) {
        if (message.getToolCallId() == null || message.getToolCallId().isBlank()) {
            throw new ChatException("TOOL 消息缺少 toolCallId, 模型无法把结果与调用配对");
        }
        ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(
                message.getToolCallId(), message.getToolName(), message.getContent());
        return ToolResponseMessage.builder()
                .responses(List.of(response))
                .build();
    }
}
