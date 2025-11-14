package io.nebula.ai.spring.chat;

import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SpringAIChatService 单元测试
 * 
 * 测试目标：验证 AI 对话服务的核心功能
 * - 简单聊天
 * - 多轮对话
 * 
 * 注意：由于 Spring AI 使用复杂的链式API和不可变对象，这里采用简化的测试策略
 * 主要验证服务层的业务逻辑正确性，而不是完全Mock Spring AI的所有内部细节
 * 
 * @author Nebula Framework
 */
@ExtendWith(MockitoExtension.class)
class SpringAIChatServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;
    
    @Mock
    private ChatClient.ChatClientRequestSpec userSpec;

    @Mock
    private ChatModel chatModel;

    private SpringAIChatService chatService;

    @BeforeEach
    void setUp() {
        // 配置 ChatClient.Builder 的链式调用
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        chatService = new SpringAIChatService(chatClientBuilder, chatModel);
    }

    /**
     * 测试简单聊天功能
     * 
     * 场景：用户发送单条消息，获取 AI 回复
     * 验证：返回 ChatResponse，内容正确，包含时间戳和模型信息
     */
    @Test
    void testSimpleChat() {
        // Given: 准备测试数据
        String userMessage = "Hello, how are you?";
        String expectedResponse = "I'm doing great, thank you for asking!";

        // Mock ChatClient 的链式调用
        // prompt() -> ChatClientRequestSpec
        // user() -> ChatClientRequestSpec
        // call() -> CallResponseSpec
        // content() -> String
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(userSpec);
        when(userSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(expectedResponse);

        // When: 执行聊天
        ChatResponse response = chatService.chat(userMessage);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo(expectedResponse);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(response.getModel()).isNotNull();

        // 验证关键方法被调用
        verify(chatClient).prompt();
        verify(callResponseSpec).content();
    }

    /**
     * 测试多轮对话功能
     * 
     * 场景：发送包含 SYSTEM, USER, ASSISTANT 消息的历史记录
     * 验证：消息正确转换为 Spring AI 格式，返回响应正确
     */
    @Test
    void testChatWithHistory() {
        // Given: 准备消息历史
        List<ChatMessage> messages = List.of(
            ChatMessage.system("You are a helpful assistant"),
            ChatMessage.user("What is Java?"),
            ChatMessage.assistant("Java is a programming language."),
            ChatMessage.user("Tell me more about it")
        );
        String expectedResponse = "Java is an object-oriented programming language...";

        // Mock ChatModel 的调用
        org.springframework.ai.chat.model.ChatResponse mockSpringResponse = mock(org.springframework.ai.chat.model.ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(mockSpringResponse);
        when(mockSpringResponse.getResult()).thenReturn(mockGeneration);
        when(mockGeneration.getOutput()).thenReturn(mock(org.springframework.ai.chat.messages.AssistantMessage.class));
        when(mockGeneration.getOutput().getText()).thenReturn(expectedResponse);

        // When: 执行多轮对话
        ChatResponse response = chatService.chat(messages);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo(expectedResponse);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getModel()).isNotNull();

        // 验证 ChatModel 被调用
        verify(chatModel).call(any(Prompt.class));
    }

    /**
     * 测试简单聊天 - 空响应场景
     * 
     * 场景：AI 返回空字符串
     * 验证：能正确处理空响应
     */
    @Test
    void testSimpleChatWithEmptyResponse() {
        // Given: AI 返回空响应
        String userMessage = "test";
        String expectedResponse = "";

        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(userSpec);
        when(userSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(expectedResponse);

        // When: 执行聊天
        ChatResponse response = chatService.chat(userMessage);

        // Then: 验证能处理空响应
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTimestamp()).isNotNull();
    }

    /**
     * 测试多轮对话 - 单条消息
     * 
     * 场景：历史记录只包含一条用户消息
     * 验证：能正确处理单条消息的场景
     */
    @Test
    void testChatWithSingleMessage() {
        // Given: 只有一条消息
        List<ChatMessage> messages = List.of(
            ChatMessage.user("Hello")
        );
        String expectedResponse = "Hi there!";

        // Mock ChatModel
        org.springframework.ai.chat.model.ChatResponse mockSpringResponse = mock(org.springframework.ai.chat.model.ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(mockSpringResponse);
        when(mockSpringResponse.getResult()).thenReturn(mockGeneration);
        when(mockGeneration.getOutput()).thenReturn(mock(org.springframework.ai.chat.messages.AssistantMessage.class));
        when(mockGeneration.getOutput().getText()).thenReturn(expectedResponse);

        // When: 执行聊天
        ChatResponse response = chatService.chat(messages);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo(expectedResponse);

        verify(chatModel).call(any(Prompt.class));
    }
}
