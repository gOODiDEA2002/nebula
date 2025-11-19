# Nebula AI Core 模块

## 模块简介

`nebula-ai-core` 是 Nebula 框架的人工智能核心抽象层。它定义了与大语言模型（LLM）交互、向量存储、检索增强生成（RAG）以及 MCP（Model Context Protocol）集成的统一接口和模型。

该模块旨在屏蔽不同 AI 模型提供商（如 OpenAI、Anthropic、Google Gemini、阿里云通义千问等）的差异，为上层应用提供一致的编程体验。

## 核心组件

### 1. ChatService - 聊天服务接口

定义了与 LLM 进行对话的核心方法，支持同步和流式响应。

```java
public interface ChatService {
    ChatResponse chat(ChatRequest request);
    Flux<ChatResponse> stream(ChatRequest request);
    // ... 其他相关方法
}
```

### 2. EmbeddingService - 向量化服务接口

定义了将文本转换为向量（Embedding）的接口。

```java
public interface EmbeddingService {
    EmbeddingResponse embed(EmbeddingRequest request);
    List<Double> embed(String text);
}
```

### 3. VectorStoreService - 向量存储服务接口

定义了向量数据库的增删改查操作，支持相似度搜索。

```java
public interface VectorStoreService {
    void add(List<Document> documents);
    SearchResult search(SearchRequest request);
    void delete(List<String> ids);
}
```

### 4. Model Context Protocol (MCP) 支持

提供了 MCP 协议的核心抽象，包括工具（Tools）、资源（Resources）和服务的定义。

- **McpTool**: 定义 AI 可调用的工具。
- **McpResource**: 定义 AI 可访问的资源。
- **McpClientService**: MCP 客户端服务接口。
- **McpServerService**: MCP 服务端服务接口。

## 扩展性

`nebula-ai-core` 仅包含接口定义和核心模型。具体的实现由 `nebula-ai-spring` 或其他适配模块提供。

## 依赖说明

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-ai-core</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

## 版本要求

- Java 21+
