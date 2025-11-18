# Nebula AI 应用示例

本示例演示如何使用 `nebula-starter-ai` 创建一个基于 RAG (Retrieval Augmented Generation) 的文档问答系统。

## 项目介绍

**项目名称**: 智能文档问答助手  
**核心功能**: 
- 文档上传与索引
- 向量化存储（Chroma）
- 智能问答（RAG）
- 对话历史管理

## 项目结构

```
doc-qa-system/
├── pom.xml
└── src/main/java/com/example/docqa/
    ├── DocQAApplication.java
    ├── config/
    │   └── AIConfig.java
    ├── controller/
    │   ├── DocumentController.java
    │   └── ChatController.java
    ├── service/
    │   ├── DocumentService.java
    │   └── ChatService.java
    └── model/
        ├── Document.java
        └── ChatMessage.java
```

## 1. 项目配置

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>doc-qa-system</artifactId>
    <version>1.0.0</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.12</version>
    </parent>

    <properties>
        <java.version>21</java.version>
        <nebula.version>2.0.1-SNAPSHOT</nebula.version>
    </properties>

    <dependencies>
        <!-- Nebula AI Starter -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-ai</artifactId>
            <version>${nebula.version}</version>
        </dependency>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### application.yml

```yaml
spring:
  application:
    name: doc-qa-system

server:
  port: 8080

# Nebula AI 配置
nebula:
  ai:
    # OpenAI 配置
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_API_BASE_URL:https://api.openai.com}
      chat:
        model: gpt-4
        temperature: 0.7
        max-tokens: 2000
      embedding:
        model: text-embedding-3-small
        dimensions: 1536

    # 向量数据库配置（Chroma）
    vector-store:
      chroma:
        host: localhost
        port: 8000
        collection-name: documents

  # 缓存配置
  data:
    cache:
      enabled: true
      caffeine:
        enabled: true
        max-size: 1000
      redis:
        enabled: true
        host: localhost
        port: 6379
        key-prefix: "docqa:"

  # Web 配置
  web:
    response:
      unified-result: true
    cors:
      enabled: true
      allowed-origins: "http://localhost:3000"

logging:
  level:
    com.example.docqa: DEBUG
    io.nebula.ai: INFO
```

## 2. AI 配置类（可选）

### AIConfig.java

```java
package com.example.docqa.config;

import io.nebula.ai.core.chat.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public ChatOptions defaultChatOptions() {
        return ChatOptions.builder()
                .temperature(0.7)
                .maxTokens(2000)
                .topP(1.0)
                .build();
    }
}
```

## 3. 实体类

### DocumentChunk.java

```java
package com.example.docqa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    
    private String id;
    
    private String documentId;
    
    private String content;
    
    private String fileName;
    
    private Integer chunkIndex;
    
    private String category;
    
    private LocalDateTime indexedAt;
}
```

### ChatMessage.java

```java
package com.example.docqa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    private String role; // user, assistant
    
    private String content;
    
    private LocalDateTime timestamp;
    
    private String conversationId;
}
```

## 4. Service 层

### DocumentService.java

```java
package com.example.docqa.service;

import com.example.docqa.model.DocumentChunk;
import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.model.EmbeddingRequest;
import io.nebula.ai.core.model.EmbeddingResponse;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.data.cache.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final CacheManager cacheManager;

    private static final int CHUNK_SIZE = 500; // 每个分块的字符数
    private static final int CHUNK_OVERLAP = 50; // 分块重叠

    /**
     * 上传并索引文档
     */
    public String uploadDocument(MultipartFile file, String category) throws IOException {
        log.info("Uploading document: {}", file.getOriginalFilename());
        
        // 读取文件内容
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        
        // 分块
        List<DocumentChunk> chunks = chunkDocument(content, file.getOriginalFilename(), category);
        
        // 向量化并存储
        indexChunks(chunks);
        
        String documentId = UUID.randomUUID().toString();
        log.info("Document uploaded successfully: {}", documentId);
        
        return documentId;
    }

    /**
     * 文档分块
     */
    private List<DocumentChunk> chunkDocument(String content, String fileName, String category) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String documentId = UUID.randomUUID().toString();
        
        int start = 0;
        int chunkIndex = 0;
        
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
            String chunkContent = content.substring(start, end);
            
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(UUID.randomUUID().toString())
                    .documentId(documentId)
                    .content(chunkContent)
                    .fileName(fileName)
                    .chunkIndex(chunkIndex++)
                    .category(category)
                    .indexedAt(LocalDateTime.now())
                    .build();
            
            chunks.add(chunk);
            
            start += (CHUNK_SIZE - CHUNK_OVERLAP);
        }
        
        log.info("Document chunked into {} pieces", chunks.size());
        return chunks;
    }

    /**
     * 向量化并索引分块
     */
    private void indexChunks(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            // 生成 embedding
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .input(chunk.getContent())
                    .build();
            
            EmbeddingResponse response = embeddingService.embed(request);
            
            // 存储到向量数据库
            vectorStoreService.add(chunk.getId(), response.getEmbedding(), chunk);
        }
        
        log.info("Indexed {} chunks", chunks.size());
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String documentId) {
        log.info("Deleting document: {}", documentId);
        
        // 从向量数据库删除
        vectorStoreService.deleteByFilter("documentId", documentId);
        
        // 清除缓存
        cacheManager.delete("doc:" + documentId);
        
        return true;
    }

    /**
     * 搜索相关文档片段
     */
    public List<DocumentChunk> searchRelevantChunks(String query, int topK) {
        // 生成查询的 embedding
        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(query)
                .build();
        
        EmbeddingResponse response = embeddingService.embed(request);
        
        // 向量搜索
        var searchResults = vectorStoreService.search(response.getEmbedding(), topK);
        
        // 转换为 DocumentChunk
        List<DocumentChunk> chunks = new ArrayList<>();
        for (var result : searchResults.getResults()) {
            DocumentChunk chunk = (DocumentChunk) result.getMetadata().get("chunk");
            chunks.add(chunk);
        }
        
        return chunks;
    }
}
```

### ChatService.java

```java
package com.example.docqa.service;

import com.example.docqa.model.ChatMessage;
import com.example.docqa.model.DocumentChunk;
import io.nebula.ai.core.chat.ChatRequest;
import io.nebula.ai.core.chat.ChatResponse;
import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.Message;
import io.nebula.data.cache.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatService aiChatService;
    private final DocumentService documentService;
    private final CacheManager cacheManager;

    /**
     * RAG 问答
     */
    public ChatMessage chat(String question, String conversationId) {
        log.info("Processing question: {}", question);
        
        // 1. 检索相关文档片段
        List<DocumentChunk> relevantChunks = documentService.searchRelevantChunks(question, 3);
        
        // 2. 构建上下文
        String context = buildContext(relevantChunks);
        
        // 3. 构建提示词
        String prompt = buildPrompt(context, question);
        
        // 4. 获取对话历史
        List<ChatMessage> history = getConversationHistory(conversationId);
        
        // 5. 构建消息列表
        List<Message> messages = new ArrayList<>();
        
        // 系统提示
        messages.add(Message.builder()
                .role("system")
                .content("你是一个专业的文档问答助手。请基于提供的文档内容回答用户问题。如果文档中没有相关信息，请明确告知用户。")
                .build());
        
        // 历史消息
        for (ChatMessage msg : history) {
            messages.add(Message.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .build());
        }
        
        // 当前问题（带上下文）
        messages.add(Message.builder()
                .role("user")
                .content(prompt)
                .build());
        
        // 6. 调用 LLM
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();
        
        ChatResponse response = aiChatService.chat(request);
        String answer = response.getContent();
        
        // 7. 保存对话历史
        ChatMessage userMessage = ChatMessage.builder()
                .role("user")
                .content(question)
                .timestamp(LocalDateTime.now())
                .conversationId(conversationId)
                .build();
        
        ChatMessage assistantMessage = ChatMessage.builder()
                .role("assistant")
                .content(answer)
                .timestamp(LocalDateTime.now())
                .conversationId(conversationId)
                .build();
        
        saveConversationHistory(conversationId, userMessage);
        saveConversationHistory(conversationId, assistantMessage);
        
        log.info("Answer generated successfully");
        return assistantMessage;
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<DocumentChunk> chunks) {
        return chunks.stream()
                .map(chunk -> String.format("文档片段 %d:\n%s\n", chunk.getChunkIndex(), chunk.getContent()))
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String context, String question) {
        return String.format("""
                请基于以下文档内容回答问题：
                
                %s
                
                问题：%s
                
                请给出简洁准确的答案。如果文档中没有相关信息，请明确说明。
                """, context, question);
    }

    /**
     * 获取对话历史
     */
    private List<ChatMessage> getConversationHistory(String conversationId) {
        String cacheKey = "conversation:" + conversationId;
        return cacheManager.get(cacheKey, List.class).orElse(new ArrayList<>());
    }

    /**
     * 保存对话历史
     */
    private void saveConversationHistory(String conversationId, ChatMessage message) {
        String cacheKey = "conversation:" + conversationId;
        List<ChatMessage> history = getConversationHistory(conversationId);
        history.add(message);
        
        // 只保留最近 10 条消息
        if (history.size() > 10) {
            history = history.subList(history.size() - 10, history.size());
        }
        
        cacheManager.set(cacheKey, history, 3600);
    }

    /**
     * 清除对话历史
     */
    public void clearConversation(String conversationId) {
        String cacheKey = "conversation:" + conversationId;
        cacheManager.delete(cacheKey);
    }
}
```

## 5. Controller 层

### DocumentController.java

```java
package com.example.docqa.controller;

import com.example.docqa.service.DocumentService;
import io.nebula.foundation.common.Result;
import io.nebula.web.controller.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController extends BaseController {

    private final DocumentService documentService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public Result<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category) throws IOException {
        
        String documentId = documentService.uploadDocument(file, category);
        return Result.success(documentId);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public Result<Void> deleteDocument(@PathVariable String documentId) {
        boolean success = documentService.deleteDocument(documentId);
        return success ? Result.success() : Result.error("删除失败");
    }
}
```

### ChatController.java

```java
package com.example.docqa.controller;

import com.example.docqa.model.ChatMessage;
import com.example.docqa.service.ChatService;
import io.nebula.foundation.common.Result;
import io.nebula.web.controller.BaseController;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController extends BaseController {

    private final ChatService chatService;

    /**
     * 发送消息
     */
    @PostMapping
    public Result<ChatMessage> chat(@RequestBody ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }
        
        ChatMessage response = chatService.chat(request.getQuestion(), conversationId);
        return Result.success(response);
    }

    /**
     * 清除对话
     */
    @DeleteMapping("/{conversationId}")
    public Result<Void> clearConversation(@PathVariable String conversationId) {
        chatService.clearConversation(conversationId);
        return Result.success();
    }

    @Data
    public static class ChatRequest {
        @NotBlank(message = "问题不能为空")
        private String question;
        
        private String conversationId;
    }
}
```

## 6. 运行应用

### 启动 Chroma

```bash
docker run -d --name chroma \
  -p 8000:8000 \
  chromadb/chroma:latest
```

### 启动 Redis

```bash
docker run -d --name redis \
  -p 6379:6379 \
  redis:latest
```

### 启动应用

```bash
# 设置环境变量
export OPENAI_API_KEY=your_api_key

# 运行应用
mvn spring-boot:run
```

## 7. 测试 API

### 上传文档

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document.txt" \
  -F "category=技术文档"
```

### 发起问答

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "如何配置缓存？",
    "conversationId": "conv-123"
  }'
```

### 清除对话

```bash
curl -X DELETE http://localhost:8080/api/chat/conv-123
```

## 8. 核心特性

### ✅ 文档向量化
- 自动分块（Chunking）
- Embedding 生成
- 向量存储（Chroma）

### ✅ RAG 问答
- 语义搜索
- 上下文构建
- LLM 生成答案

### ✅ 对话历史
- 多轮对话支持
- 上下文记忆
- 历史管理

### ✅ 缓存优化
- Caffeine 本地缓存
- Redis 分布式缓存
- 对话历史缓存

## 9. 进阶功能

### 添加流式响应

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestParam String question) {
    return chatService.chatStream(question);
}
```

### 添加多模态支持

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vision</artifactId>
</dependency>
```

---

**完整代码**: 参见 `nebula-doc-mcp-server` 项目  
**相关文档**:
- [Nebula AI 模块文档](../nebula-ai-core/README.md)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)

