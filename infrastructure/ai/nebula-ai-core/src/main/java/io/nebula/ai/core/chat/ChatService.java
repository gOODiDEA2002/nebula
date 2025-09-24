package io.nebula.ai.core.chat;

import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatResponse;
import io.nebula.ai.core.model.ChatRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天服务接口
 * 提供与AI聊天模型交互的统一抽象
 */
public interface ChatService {

    /**
     * 发送单条消息并获取响应
     *
     * @param message 用户消息
     * @return AI响应
     */
    ChatResponse chat(String message);

    /**
     * 发送消息列表并获取响应
     *
     * @param messages 消息列表
     * @return AI响应
     */
    ChatResponse chat(List<ChatMessage> messages);

    /**
     * 发送聊天请求并获取响应
     *
     * @param request 聊天请求
     * @return AI响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 异步发送消息并获取响应
     *
     * @param message 用户消息
     * @return 异步AI响应
     */
    CompletableFuture<ChatResponse> chatAsync(String message);

    /**
     * 异步发送消息列表并获取响应
     *
     * @param messages 消息列表
     * @return 异步AI响应
     */
    CompletableFuture<ChatResponse> chatAsync(List<ChatMessage> messages);

    /**
     * 异步发送聊天请求并获取响应
     *
     * @param request 聊天请求
     * @return 异步AI响应
     */
    CompletableFuture<ChatResponse> chatAsync(ChatRequest request);

    /**
     * 流式聊天响应
     *
     * @param message 用户消息
     * @param callback 流式响应回调
     */
    void chatStream(String message, ChatStreamCallback callback);

    /**
     * 流式聊天响应
     *
     * @param messages 消息列表
     * @param callback 流式响应回调
     */
    void chatStream(List<ChatMessage> messages, ChatStreamCallback callback);

    /**
     * 流式聊天响应
     *
     * @param request 聊天请求
     * @param callback 流式响应回调
     */
    void chatStream(ChatRequest request, ChatStreamCallback callback);

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取支持的模型列表
     *
     * @return 模型列表
     */
    List<String> getSupportedModels();

    /**
     * 获取当前使用的模型
     *
     * @return 当前模型
     */
    String getCurrentModel();

    /**
     * 流式聊天回调接口
     */
    interface ChatStreamCallback {
        
        /**
         * 接收到文本片段时调用
         *
         * @param chunk 文本片段
         */
        void onChunk(String chunk);

        /**
         * 流式响应完成时调用
         *
         * @param response 完整响应
         */
        void onComplete(ChatResponse response);

        /**
         * 发生错误时调用
         *
         * @param error 错误信息
         */
        void onError(Throwable error);
    }
}
