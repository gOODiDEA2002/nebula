# Nebula AI Spring 模块

##  模块简介

`nebula-ai-spring` 是 Nebula 框架的 AI 功能模块，基于 Spring AI 实现，提供了统一的 AI 能力抽象和强大的 AI 应用开发支持该模块集成了聊天嵌入向量存储等企业级 AI 特性

##  功能特性

###  核心功能
- **智能聊天**: 基于大语言模型的对话能力，支持同步异步和流式响应
- **文本嵌入**: 文本向量化服务，支持批量处理和相似度计算
- **向量存储**: 文档向量化存储和语义搜索，支持 RAG（检索增强生成）
- **多模型支持**: 支持 OpenAIAnthropic 等多种 AI 服务提供商

###  增强特性
- **自动配置**: Spring Boot 自动配置，零配置启动
- **类型安全**: 完整的泛型支持和类型安全
- **异步支持**: 支持异步和流式处理
- **统一抽象**: 基于 Nebula AI Core 的统一接口，易于切换不同实现

##  快速开始

### 添加依赖

```xml
<!-- Nebula AI Spring 模块 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-ai-spring</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>

<!-- Spring AI OpenAI Starter（根据需要选择） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>

<!-- Spring AI Chroma Vector Store（可选，用于向量存储） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-chroma</artifactId>
</dependency>

<!-- Spring AI MCP Server（可选，用于构建 MCP 服务器） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

> **注意**：从版本 2.0.1 开始，nebula-ai-spring 使用 Spring AI 1.1.0，支持标准的 MCP (Model Context Protocol) Server。

### 基础配置

在 `application.yml` 中配置 AI 服务：

```yaml
# 启用 Nebula AI 模块
nebula:
  ai:
    enabled: true

# Spring AI 配置
spring:
  ai:
    # OpenAI 配置
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-3.5-turbo
          temperature: 0.7
          max-tokens: 1000
      embedding:
        options:
          model: text-embedding-ada-002
    
    # Chroma 向量存储配置（可选）
    vectorstore:
      chroma:
        initialize-schema: true
        client:
          host: localhost
          port: 8000
          key-token: ${CHROMA_API_KEY:}
```

##  核心功能使用

### 1. 智能聊天功能

#### 1.1 简单聊天

```java
@Service
public class ChatDemoService {
    
    private final ChatService chatService;
    
    @Autowired
    public ChatDemoService(ChatService chatService) {
        this.chatService = chatService;
    }
    
    public String chat(String message) {
        ChatResponse response = chatService.chat(message);
        return response.getContent();
    }
}
```

#### 1.2 多轮对话

```java
public String multiRoundChat() {
    List<ChatMessage> messages = List.of(
        ChatMessage.system("你是一个友好的助手"),
        ChatMessage.user("你好，我想了解一下Java"),
        ChatMessage.assistant("你好！我很乐意帮你了解JavaJava是什么方面的内容你想了解呢？"),
        ChatMessage.user("Java的集合框架")
    );
    
    ChatResponse response = chatService.chat(messages);
    return response.getContent();
}
```

#### 1.3 流式聊天

```java
public void streamChat(String message) {
    chatService.chatStream(message, new ChatService.ChatStreamCallback() {
        @Override
        public void onChunk(String chunk) {
            System.out.print(chunk); // 实时输出文本片段
        }
        
        @Override
        public void onComplete(ChatResponse response) {
            System.out.println("\n完成，总tokens: " + response.getUsage().getTotalTokens());
        }
        
        @Override
        public void onError(Throwable error) {
            System.err.println("错误: " + error.getMessage());
        }
    });
}
```

#### 1.4 异步聊天

```java
public CompletableFuture<String> chatAsync(String message) {
    return chatService.chatAsync(message)
            .thenApply(ChatResponse::getContent);
}
```

### 2. 文本嵌入功能

#### 2.1 单文本向量化

```java
@Service
public class EmbeddingDemoService {
    
    private final EmbeddingService embeddingService;
    
    @Autowired
    public EmbeddingDemoService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }
    
    public List<Double> embedText(String text) {
        EmbeddingResponse response = embeddingService.embed(text);
        return response.getFirstVector();
    }
}
```

#### 2.2 批量文本向量化

```java
public List<List<Double>> embedBatch(List<String> texts) {
    EmbeddingResponse response = embeddingService.embed(texts);
    return response.getAllVectors();
}
```

#### 2.3 计算文本相似度

```java
public double calculateSimilarity(String text1, String text2) {
    List<Double> vector1 = embedText(text1);
    List<Double> vector2 = embedText(text2);
    
    return embeddingService.similarity(vector1, vector2);
}
```

### 3. 向量存储与搜索

#### 3.1 添加文档

```java
@Service
public class VectorStoreDemoService {
    
    private final VectorStoreService vectorStoreService;
    
    @Autowired
    public VectorStoreDemoService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }
    
    public void addDocument(String content, Map<String, Object> metadata) {
        Document document = Document.builder()
                .content(content)
                .metadata(metadata)
                .build();
        
        vectorStoreService.add(document);
    }
    
    public void addDocuments(List<String> contents) {
        List<Document> documents = contents.stream()
                .map(content -> Document.simple(content))
                .toList();
        
        vectorStoreService.addAll(documents);
    }
}
```

#### 3.2 语义搜索

```java
public List<String> search(String query, int topK) {
    SearchResult result = vectorStoreService.search(query, topK);
    
    return result.getDocuments().stream()
            .map(SearchResult.DocumentResult::getContent)
            .toList();
}
```

#### 3.3 带过滤条件的搜索

```java
public SearchResult searchWithFilter(String query, int topK, Map<String, Object> filter) {
    return vectorStoreService.search(query, topK, filter);
}
```

### 4. 文档智能问答（RAG）

```java
@Service
public class RAGService {
    
    private final ChatService chatService;
    private final VectorStoreService vectorStoreService;
    
    @Autowired
    public RAGService(ChatService chatService, VectorStoreService vectorStoreService) {
        this.chatService = chatService;
        this.vectorStoreService = vectorStoreService;
    }
    
    /**
     * 基于文档的智能问答
     */
    public String ask(String question) {
        // 1. 搜索相关文档
        SearchResult searchResult = vectorStoreService.search(question, 3);
        
        // 2. 构建上下文
        String context = searchResult.getContents()
                .stream()
                .collect(Collectors.joining("\n\n"));
        
        // 3. 构建提示消息
        List<ChatMessage> messages = List.of(
            ChatMessage.system("你是一个专业的助手，根据以下上下文回答用户问题\n\n上下文:\n" + context),
            ChatMessage.user(question)
        );
        
        // 4. 获取回答
        ChatResponse response = chatService.chat(messages);
        return response.getContent();
    }
    
    /**
     * 批量添加知识库文档
     */
    public void addKnowledgeBase(List<String> documents) {
        List<Document> docs = documents.stream()
                .map(content -> Document.builder()
                        .content(content)
                        .addMetadata("source", "knowledge_base")
                        .addMetadata("timestamp", LocalDateTime.now().toString())
                        .build())
                .toList();
        
        vectorStoreService.addAll(docs);
    }
}
```

##  高级特性

### 自定义聊天配置

```java
@Service
public class AdvancedChatService {
    
    private final ChatService chatService;
    
    public String chatWithOptions(String message) {
        ChatRequest request = ChatRequest.builder()
                .addMessage(ChatMessage.user(message))
                .model("gpt-4")
                .temperature(0.5)
                .maxTokens(2000)
                .build();
        
        ChatResponse response = chatService.chat(request);
        return response.getContent();
    }
}
```

### 自定义嵌入配置

```java
public List<Double> embedWithOptions(String text) {
    EmbeddingRequest request = EmbeddingRequest.builder()
            .addText(text)
            .model("text-embedding-3-large")
            .build();
    
    EmbeddingResponse response = embeddingService.embed(request);
    return response.getFirstVector();
}
```

### 高级向量搜索

```java
public SearchResult advancedSearch(String query) {
    SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(10)
            .similarityThreshold(0.7)  // 相似度阈值
            .addFilter("category", "技术")
            .addFilter("status", "published")
            .build();
    
    return vectorStoreService.search(request);
}
```

##  配置选项说明

### Nebula AI 配置

```yaml
nebula:
  ai:
    # 是否启用AI功能
    enabled: true
    
    # 聊天配置
    chat:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          base-url: https://api.openai.com  # 可选，自定义API端点
          model: gpt-3.5-turbo
          options:
            temperature: 0.7
            max-tokens: 1000
    
    # 嵌入配置
    embedding:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: text-embedding-ada-002
    
    # 向量存储配置
    vector-store:
      default-provider: chroma
      providers:
        chroma:
          host: localhost
          port: 8000
          collection-name: nebula-documents
```

## ️ 自定义扩展

### 自定义聊天服务

```java
@Service
@Primary
public class CustomChatService implements ChatService {
    
    private final SpringAIChatService delegateService;
    
    @Override
    public ChatResponse chat(String message) {
        // 添加自定义逻辑（如日志监控限流等）
        log.info("接收聊天请求: {}", message);
        
        // 调用原始服务
        ChatResponse response = delegateService.chat(message);
        
        // 后处理
        log.info("聊天响应完成，tokens: {}", response.getUsage().getTotalTokens());
        
        return response;
    }
    
    // 实现其他方法...
}
```

### 自定义向量存储策略

```java
@Configuration
public class VectorStoreConfig {
    
    @Bean
    @ConditionalOnProperty("custom.vector-store.enabled")
    public VectorStoreService customVectorStoreService(
            VectorStore vectorStore, 
            EmbeddingService embeddingService) {
        return new CustomVectorStoreService(vectorStore, embeddingService);
    }
}
```

##  故障排查

### 常见问题

1. **API Key 未配置**
   - 错误: `API key not configured`
   - 解决: 确保在环境变量或配置文件中设置了 `OPENAI_API_KEY`

2. **向量存储连接失败**
   - 错误: `Connection refused to localhost:8000`
   - 解决: 确认 Chroma 服务已启动，或调整配置中的主机和端口

3. **模型不支持**
   - 错误: `Model not found: xxx`
   - 解决: 检查配置的模型名称是否正确，确认 API Key 有权限访问该模型

### 开启调试日志

```yaml
logging:
  level:
    io.nebula.ai: DEBUG
    org.springframework.ai: DEBUG
```

##  支持的 AI 提供商

### OpenAI
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

### Anthropic Claude
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>
```

### Azure OpenAI
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-azure-openai-spring-boot-starter</artifactId>
</dependency>
```

##  更多功能

- [智能聊天功能演示](../../../nebula-example/docs/nebula-ai-test.md#智能聊天)
- [文本嵌入功能演示](../../../nebula-example/docs/nebula-ai-test.md#文本嵌入)
- [文档问答功能演示](../../../nebula-example/docs/nebula-ai-test.md#文档问答)
- [完整示例项目](../../../nebula-example)

##  贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个模块

##  许可证

本项目基于 Apache 2.0 许可证开源


## 🔌 MCP Server 支持

从 Spring AI 1.1.0 开始，nebula-ai-spring 支持构建标准的 MCP (Model Context Protocol) 服务器。

### 快速开始

#### 1. 添加依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

#### 2. 配置 MCP Server

**推荐使用 Nebula 配置前缀**（自动桥接到 Spring AI 配置）:

```yaml
nebula:
  ai:
    mcp:
      server:
        enabled: true                    # 启用 MCP Server
        protocol: STREAMABLE             # 使用 Streamable HTTP 协议
        name: my-mcp-server
        version: 1.0.0
        type: SYNC
        instructions: "AI 服务器描述"
        
        tool-callback-converter: true    # 自动检测 @Tool/@McpTool 注解
        
        capabilities:
          tool: true                     # 支持工具调用
          resource: true                 # 支持资源访问
          prompt: false
          completion: false
        
        tool-change-notification: false
        resource-change-notification: false
        
        streamable-http:
          mcp-endpoint: /mcp             # MCP 协议端点
          keep-alive-interval: 30s
```

**或使用 Spring AI 原生配置**:

```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        name: my-mcp-server
        version: 1.0.0
        type: SYNC
        instructions: "AI 服务器描述"
        capabilities:
          tool: true
          resource: true
        streamable-http:
          mcp-endpoint: /mcp
```

> **注意**: 两种配置方式二选一。使用 `nebula.ai.mcp` 时会自动桥接到 `spring.ai.mcp.server`。

#### 3. 定义 MCP 工具

```java
@Service
public class MyMcpTools {
    
    @McpTool(
        name = "search_docs",
        description = "搜索文档"
    )
    public String searchDocs(
            @McpToolParam(description = "搜索关键词", required = true) 
            String query) {
        // 实现搜索逻辑
        return "搜索结果...";
    }
}
```

#### 4. 定义 MCP 资源（可选）

```java
@Configuration
public class McpResourcesConfig {
    
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> myResources() {
        var resource = new McpSchema.Resource(
            "app://config",
            "应用配置",
            "应用配置信息",
            "application/json"
        );
        
        var spec = new McpServerFeatures.SyncResourceSpecification(
            resource,
            (exchange, request) -> {
                // 返回资源内容
                return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(
                        request.uri(), 
                        "application/json", 
                        "{...}"
                    ))
                );
            }
        );
        
        return List.of(spec);
    }
}
```

### MCP 功能特性

- **标准协议**: 符合 MCP Streamable HTTP 协议规范
- **工具注册**: 使用 `@McpTool` 注解轻松定义工具
- **资源管理**: 支持动态资源和变更通知
- **上下文支持**: 可通过 `McpSyncRequestContext` 访问请求上下文
- **进度报告**: 支持长时间运行任务的进度通知
- **日志集成**: 支持向客户端发送日志消息

### 更多信息

- [Spring AI MCP 文档](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-streamable-http-server-boot-starter-docs.html)
- [MCP 协议规范](https://modelcontextprotocol.io/)

---

## 🧪 测试

本模块提供完整的单元测试文档和示例，详见 [TESTING.md](./TESTING.md)

