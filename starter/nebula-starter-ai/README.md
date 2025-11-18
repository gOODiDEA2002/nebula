# Nebula Starter AI

AI应用专用Starter，集成Spring AI和LangChain4j，支持RAG、LLM、向量检索等AI功能。

## 适用场景

- 🤖 LLM集成应用
- 🔍 RAG (检索增强生成) 系统
- 📚 向量检索服务
- 💬 智能对话系统
- 🧠 知识库问答
- 📊 AI数据分析

## 包含模块

| 模块 | 描述 |
|------|------|
| `nebula-foundation` | 基础工具类 (继承自minimal) |
| `nebula-ai-core` | AI核心抽象接口 |
| `nebula-ai-spring` | Spring AI集成 |
| `nebula-ai-langchain4j` | LangChain4j集成 (可选) |
| `nebula-data-cache` | 多级缓存 (用于缓存embedding) |
| Spring Boot Web | Web支持 (可选) |
| Spring Boot Actuator | 监控支持 (可选) |

## 功能特性

### AI核心功能
- ✅ **聊天服务** (`ChatService`)
  - 多种LLM模型支持 (OpenAI, DeepSeek, Azure, etc.)
  - 流式响应
  - 函数调用 (Function Calling)
  
- ✅ **Embedding服务** (`EmbeddingService`)
  - 文本向量化
  - 批量embedding
  - 相似度计算

- ✅ **向量存储** (`VectorStoreService`)
  - Chroma, Pinecone, Milvus等
  - 向量索引和检索
  - 元数据过滤

- ✅ **RAG支持**
  - 文档加载和解析
  - 文档分块
  - 向量检索 + LLM生成

### 缓存优化
- ✅ Embedding结果缓存 (提升性能)
- ✅ 多级缓存 (Caffeine + Redis)

### Web API (可选)
- ✅ REST API支持
- ✅ 监控端点 (Actuator)

## 内存占用

**~500MB** (AI核心 + 缓存)

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>

<!-- 如果需要Web API，显式声明 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring AI依赖 (根据需要选择) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-chroma-store-spring-boot-starter</artifactId>
</dependency>
```

### 2. 配置AI服务

`application.yml`:

```yaml
nebula:
  ai:
    enabled: true
    
    # OpenAI配置
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com
      chat:
        options:
          model: gpt-4
          temperature: 0.7
      embedding:
        model: text-embedding-3-small
    
    # Chroma向量数据库配置
    vector-store:
      chroma:
        host: localhost
        port: 8000
        collection-name: my_docs
        initialize-schema: true
    
  # 缓存配置
  data:
    cache:
      enabled: true
      type: multi-level
      default-ttl: 3600s
      
      redis:
        host: localhost
        port: 6379
        database: 0
```

### 3. 使用AI服务

#### 聊天功能

```java
import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.ChatMessage;
import io.nebula.ai.core.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MyAIService {
    
    @Autowired
    private ChatService chatService;
    
    public String chat(String userMessage) {
        // 简单聊天
        String response = chatService.chat(userMessage);
        return response;
    }
    
    public String chatWithHistory(List<ChatMessage> messages) {
        // 带历史记录的聊天
        ChatResponse response = chatService.chat(messages);
        return response.getContent();
    }
}
```

#### 向量检索 (RAG)

```java
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.core.model.Document;
import io.nebula.ai.core.model.SearchResult;

@Service
public class RAGService {
    
    @Autowired
    private VectorStoreService vectorStore;
    
    @Autowired
    private ChatService chatService;
    
    public void indexDocuments(List<String> texts) {
        // 索引文档
        List<Document> docs = texts.stream()
            .map(text -> Document.builder()
                .content(text)
                .build())
            .toList();
        
        vectorStore.addAll(docs);
    }
    
    public String ragQuery(String query) {
        // 1. 检索相关文档
        SearchResult searchResult = vectorStore.search(query, 5);
        
        // 2. 构建提示词
        String context = searchResult.getDocuments().stream()
            .map(doc -> doc.getContent())
            .collect(Collectors.joining("\n\n"));
        
        String prompt = String.format(
            "基于以下上下文回答问题:\n\n%s\n\n问题: %s",
            context, query
        );
        
        // 3. LLM生成答案
        return chatService.chat(prompt);
    }
}
```

#### Embedding服务

```java
import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.model.EmbeddingResponse;

@Service
public class MyEmbeddingService {
    
    @Autowired
    private EmbeddingService embeddingService;
    
    public List<Double> embed(String text) {
        EmbeddingResponse response = embeddingService.embed(text);
        return response.getFirstVector();
    }
    
    public double similarity(String text1, String text2) {
        List<Double> vec1 = embed(text1);
        List<Double> vec2 = embed(text2);
        return embeddingService.similarity(vec1, vec2);
    }
}
```

### 4. REST API示例

```java
import io.nebula.core.common.result.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private VectorStoreService vectorStore;
    
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        String response = chatService.chat(request.getMessage());
        return Result.success(response);
    }
    
    @PostMapping("/search")
    public Result<SearchResult> search(@RequestBody SearchRequest request) {
        SearchResult result = vectorStore.search(
            request.getQuery(), 
            request.getTopK()
        );
        return Result.success(result);
    }
}
```

## 完整示例项目

参考实现: 
- `nebula/example/nebula-doc-mcp-server` - MCP协议文档服务器
- `nebula/examples/nebula-example-rag` - RAG问答系统

## 支持的AI Provider

### LLM模型
- ✅ OpenAI (GPT-3.5, GPT-4)
- ✅ Azure OpenAI
- ✅ DeepSeek
- ✅ Claude (Anthropic)
- ✅ 本地模型 (Ollama)

### Embedding模型
- ✅ OpenAI Embedding
- ✅ Azure OpenAI Embedding
- ✅ 本地模型 (Ollama, nomic-embed-text)

### 向量数据库
- ✅ Chroma
- ✅ Pinecone
- ✅ Milvus
- ✅ Weaviate
- ✅ Qdrant

## 性能优化建议

1. **启用缓存**
```yaml
nebula:
  data:
    cache:
      enabled: true
      type: multi-level  # Caffeine + Redis
```

2. **批量处理**
```java
// 批量embedding
List<String> texts = Arrays.asList("text1", "text2", "text3");
EmbeddingResponse response = embeddingService.embed(texts);
```

3. **控制TopK数量**
```java
// 不要检索过多文档
SearchResult result = vectorStore.search(query, 5);  // 5-10个就够
```

## 常见问题

### Q: 为什么不包含数据库?
A: AI应用通常只需要向量存储，不需要传统关系型数据库。如果需要，可以单独引入`nebula-data-persistence`。

### Q: 如何使用本地模型?
A: 配置Ollama:
```yaml
nebula:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama2
      embedding:
        model: nomic-embed-text
```

### Q: 内存不够怎么办?
A: 
1. 使用更小的模型
2. 减少batch size
3. 启用缓存减少重复计算

## 升级到其他Starter

如果需要微服务能力，可以升级到`nebula-starter-service` + AI模块:

```xml
<dependency>
    <artifactId>nebula-starter-service</artifactId>
</dependency>
<dependency>
    <artifactId>nebula-ai-spring</artifactId>
</dependency>
```

## 不包含的功能

以下功能默认不包含：

- ❌ 传统数据库 (MySQL/PostgreSQL)
- ❌ RPC服务 (gRPC)
- ❌ 服务发现 (Nacos)
- ❌ 消息队列 (RabbitMQ)

如需这些功能，请单独引入或使用`nebula-starter-service`。

## 文档

- [Nebula AI模块文档](../../infrastructure/ai/nebula-ai-spring/README.md)
- [Spring AI文档](https://docs.spring.io/spring-ai/reference/)
- [RAG最佳实践](../../docs/best-practices/RAG.md)

---

**版本**: 2.0.1-SNAPSHOT  
**推荐场景**: AI应用、RAG系统、向量检索  
**维护**: Nebula Framework Team

