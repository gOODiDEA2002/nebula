# nebula-ai-core 模块示例

## 模块简介

`nebula-ai-core` 模块定义了 Nebula 框架的 AI 能力核心抽象。它提供了一套统一的接口，用于与 LLM（大语言模型）、向量数据库和 MCP（Model Context Protocol）工具进行交互。

核心组件包括：
- **ChatService**: 统一的聊天服务接口，支持同步、异步和流式对话。
- **EmbeddingService**: 文本嵌入服务接口。
- **VectorStoreService**: 向量数据库操作接口。
- **McpClientService**: MCP 客户端接口，用于调用外部工具和资源。

## 核心功能示例

### 1. 聊天对话 (Chat)

使用 `ChatService` 进行对话。

**`io.nebula.example.ai.service.AssistantService`**:

```java
package io.nebula.example.ai.service;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final ChatService chatService;

    public String askQuestion(String question) {
        // 1. 简单问答
        ChatResponse response = chatService.chat(question);
        return response.getContent();
    }
    
    public void askWithContext() {
        // 2. 带上下文的多轮对话
        List<ChatMessage> history = List.of(
            ChatMessage.system("你是一个专业的 Java 助手"),
            ChatMessage.user("什么是 Spring Boot?"),
            ChatMessage.assistant("Spring Boot 是基于 Spring 的快速开发框架..."),
            ChatMessage.user("它有什么优点？")
        );
        
        ChatResponse response = chatService.chat(history);
        log.info("AI 回答: {}", response.getContent());
    }
    
    public void streamAnswer(String question) {
        // 3. 流式响应 (打字机效果)
        chatService.chatStream(question, new ChatService.ChatStreamCallback() {
            @Override
            public void onChunk(String chunk) {
                System.out.print(chunk); // 实时输出
            }

            @Override
            public void onComplete(ChatResponse response) {
                System.out.println("\n--- 完成 ---");
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式对话出错", error);
            }
        });
    }
}
```

### 2. MCP 工具调用

使用 `McpClientService` 调用已连接的 MCP Server 提供的工具。

**`io.nebula.example.ai.service.ToolService`**:

```java
package io.nebula.example.ai.service;

import io.nebula.ai.core.mcp.McpClientService;
import io.nebula.ai.core.mcp.McpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolService {

    private final McpClientService mcpClientService;

    public void listAndCallTools() {
        if (!mcpClientService.isConnected()) {
            log.warn("MCP 客户端未连接");
            return;
        }

        // 1. 列出可用工具
        List<McpTool> tools = mcpClientService.listTools();
        tools.forEach(t -> log.info("工具: {}, 描述: {}", t.getName(), t.getDescription()));

        // 2. 调用工具 (例如一个计算器工具)
        // 假设工具名为 "calculator", 参数为 JSON 字符串
        String result = mcpClientService.callTool("calculator", "{\"expression\": \"1 + 1\"}");
        log.info("工具调用结果: {}", result);
    }
}
```

### 3. 向量检索 (RAG 基础)

结合 `EmbeddingService` 和 `VectorStoreService` 实现语义搜索。

```java
// 1. 将文本转换为向量
List<Double> vector = embeddingService.embed("如何配置 Nebula 框架?");

// 2. 在向量库中搜索相似文档
List<Document> docs = vectorStoreService.similaritySearch(vector, 3); // Top 3
```

## 总结

`nebula-ai-core` 定义了构建 AI 应用所需的基础能力。配合 `nebula-ai-spring` 模块，可以快速接入 OpenAI, Ollama 等主流模型提供商，并利用 MCP 协议扩展 AI 的能力边界。

