# Nebula Starter AI - 使用示例

> AI应用专用Starter的完整使用示例，涵盖LLM对话、RAG检索、向量存储、Embedding等典型AI应用场景。

## 示例概览

本文档包含以下示例：

- [示例1：简单LLM对话](#示例1简单llm对话)
- [示例2：流式对话响应](#示例2流式对话响应)
- [示例3：Function Calling](#示例3function-calling)
- [示例4：文本Embedding](#示例4文本embedding)
- [示例5：向量检索](#示例5向量检索)
- [示例6：RAG问答系统](#示例6rag问答系统)
- [示例7：多轮对话管理](#示例7多轮对话管理)
- [示例8：Prompt模板](#示例8prompt模板)
- [票务系统AI应用场景](#票务系统ai应用场景)

## 前提条件

### 环境要求

- **Java**：21+
- **Maven**：3.8+
- **Spring Boot**：3.2+
- **OpenAI API Key**（或其他LLM服务）
- **Chroma**：向量数据库（可选）
- **Redis**：缓存（可选）

### 依赖配置

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>

<!-- 选择LLM提供商 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- 选择向量数据库 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-chroma-store-spring-boot-starter</artifactId>
</dependency>
```

---

## 示例1：简单LLM对话

### 场景说明

实现最基本的LLM对话功能。

### 实现步骤

#### 步骤1：配置OpenAI

`application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.7
          max-tokens: 2000
```

#### 步骤2：创建对话服务

```java
package com.example.ai.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI对话服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatClient chatClient;
    
    /**
     * 简单对话
     */
    public String chat(String userMessage) {
        log.info("用户输入: {}", userMessage);
        
        String response = chatClient.call(userMessage);
        
        log.info("AI回复: {}", response);
        return response;
    }
    
    /**
     * 带参数的对话
     */
    public String chatWithOptions(String userMessage, double temperature) {
        ChatResponse response = chatClient.call(
            new Prompt(
                new UserMessage(userMessage),
                OpenAiChatOptions.builder()
                    .withTemperature(temperature)
                    .withMaxTokens(1000)
                    .build()
            )
        );
        
        return response.getResult().getOutput().getContent();
    }
}
```

#### 步骤3：创建REST API

```java
package com.example.ai.controller;

import io.nebula.web.controller.BaseController;
import io.nebula.core.model.Result;
import com.example.ai.service.ChatService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.Data;

/**
 * AI对话API
 */
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class ChatController extends BaseController {
    
    private final ChatService chatService;
    
    /**
     * 对话接口
     */
    @PostMapping
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        String response = chatService.chat(request.getMessage());
        
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setMessage(response);
        
        return success(chatResponse);
    }
}

@Data
class ChatRequest {
    private String message;
}

@Data
class ChatResponse {
    private String message;
}
```

#### 步骤4：测试

```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"你好，介绍一下你自己"}'
```

### 运行结果

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "message": "你好！我是一个AI助手，基于大型语言模型开发..."
  },
  "timestamp": "2025-11-20T10:00:00"
}
```

---

## 示例2：流式对话响应

### 场景说明

实现流式响应，逐字输出AI回复。

### 实现代码

```java
package com.example.ai.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.StreamingChatClient;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * 流式对话服务
 */
@Service
@RequiredArgsConstructor
public class StreamingChatService {
    
    private final StreamingChatClient streamingChatClient;
    
    /**
     * 流式对话
     */
    public Flux<String> streamChat(String userMessage) {
        return streamingChatClient.stream(userMessage)
            .map(chatResponse -> chatResponse.getResult().getOutput().getContent());
    }
}
```

**Controller**:

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestParam String message) {
    return streamingChatService.streamChat(message);
}
```

**前端调用**:

```javascript
const eventSource = new EventSource('/api/ai/chat/stream?message=你好');

eventSource.onmessage = (event) => {
    console.log(event.data);
    // 逐字显示
};
```

---

## 示例3：Function Calling

### 场景说明

让LLM调用本地函数获取实时数据。

### 实现代码

```java
package com.example.ai.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Function Calling服务
 */
@Service
@RequiredArgsConstructor
public class FunctionCallingService {
    
    private final ChatClient chatClient;
    
    /**
     * 带函数调用的对话
     */
    public String chatWithFunctions(String userMessage) {
        // 定义可调用的函数
        FunctionCallback weatherFunction = FunctionCallback.builder()
            .function("getWeather", this::getWeather)
            .description("获取指定城市的天气信息")
            .inputType(WeatherRequest.class)
            .build();
        
        FunctionCallback movieFunction = FunctionCallback.builder()
            .function("getMovies", this::getMovies)
            .description("获取正在上映的电影列表")
            .inputType(MovieRequest.class)
            .build();
        
        // 执行对话
        ChatResponse response = chatClient.call(
            new Prompt(
                new UserMessage(userMessage),
                OpenAiChatOptions.builder()
                    .withFunctionCallbacks(List.of(weatherFunction, movieFunction))
                    .build()
            )
        );
        
        return response.getResult().getOutput().getContent();
    }
    
    /**
     * 获取天气（模拟）
     */
    private WeatherResponse getWeather(WeatherRequest request) {
        WeatherResponse response = new WeatherResponse();
        response.setCity(request.getCity());
        response.setTemperature(25);
        response.setWeather("晴天");
        return response;
    }
    
    /**
     * 获取电影列表（模拟）
     */
    private MovieListResponse getMovies(MovieRequest request) {
        MovieListResponse response = new MovieListResponse();
        response.setMovies(List.of(
            new Movie("阿凡达2", "科幻"),
            new Movie("流浪地球3", "科幻")
        ));
        return response;
    }
}

@Data
class WeatherRequest {
    private String city;
}

@Data
class WeatherResponse {
    private String city;
    private int temperature;
    private String weather;
}

@Data
class MovieRequest {
    private String city;
}

@Data
class MovieListResponse {
    private List<Movie> movies;
}

@Data
@AllArgsConstructor
class Movie {
    private String name;
    private String genre;
}
```

**测试**:

```java
String response = functionCallingService.chatWithFunctions(
    "北京今天天气怎么样？有什么好看的电影推荐吗？"
);
// AI会自动调用 getWeather 和 getMovies 函数
```

---

## 示例4：文本Embedding

### 场景说明

将文本转换为向量表示。

### 实现代码

```java
package com.example.ai.service;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Embedding服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    
    private final EmbeddingClient embeddingClient;
    
    /**
     * 文本转向量
     */
    public List<Double> embed(String text) {
        log.info("生成Embedding: {}", text);
        
        EmbeddingResponse response = embeddingClient.embedForResponse(List.of(text));
        return response.getResult().getOutput();
    }
    
    /**
     * 批量转换
     */
    public List<List<Double>> embedBatch(List<String> texts) {
        log.info("批量生成Embedding: {} 条", texts.size());
        
        EmbeddingResponse response = embeddingClient.embedForResponse(texts);
        return response.getResults().stream()
            .map(embedding -> embedding.getOutput())
            .toList();
    }
    
    /**
     * 计算相似度（余弦相似度）
     */
    public double similarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 文本相似度
     */
    public double textSimilarity(String text1, String text2) {
        List<Double> vec1 = embed(text1);
        List<Double> vec2 = embed(text2);
        return similarity(vec1, vec2);
    }
}
```

**使用示例**:

```java
// 单个文本
List<Double> vector = embeddingService.embed("这是一段测试文本");

// 批量处理
List<String> texts = Arrays.asList("文本1", "文本2", "文本3");
List<List<Double>> vectors = embeddingService.embedBatch(texts);

// 计算相似度
double similarity = embeddingService.textSimilarity("苹果手机", "iPhone");
```

---

## 示例5：向量检索

### 场景说明

使用向量数据库存储和检索文档。

### 实现代码

```java
package com.example.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 向量存储服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {
    
    private final VectorStore vectorStore;
    
    /**
     * 添加文档
     */
    public void addDocument(String content, Map<String, Object> metadata) {
        Document document = new Document(content, metadata);
        vectorStore.add(List.of(document));
        
        log.info("文档已添加: {}", content.substring(0, Math.min(50, content.length())));
    }
    
    /**
     * 批量添加文档
     */
    public void addDocuments(List<String> contents) {
        List<Document> documents = contents.stream()
            .map(content -> new Document(content))
            .toList();
        
        vectorStore.add(documents);
        
        log.info("批量添加文档: {} 条", contents.size());
    }
    
    /**
     * 相似度检索
     */
    public List<Document> search(String query, int topK) {
        log.info("检索查询: {}, topK: {}", query, topK);
        
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );
        
        log.info("检索到 {} 条相关文档", results.size());
        return results;
    }
    
    /**
     * 带阈值的检索
     */
    public List<Document> searchWithThreshold(String query, int topK, double threshold) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold)
        );
        
        return results;
    }
    
    /**
     * 带元数据过滤的检索
     */
    public List<Document> searchWithFilter(String query, int topK, Map<String, Object> filter) {
        SearchRequest request = SearchRequest.query(query)
            .withTopK(topK)
            .withFilterExpression(buildFilterExpression(filter));
        
        return vectorStore.similaritySearch(request);
    }
    
    private String buildFilterExpression(Map<String, Object> filter) {
        // 构建过滤表达式
        // 例如: "category == 'movie' && year >= 2020"
        return filter.entrySet().stream()
            .map(entry -> String.format("%s == '%s'", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(" && "));
    }
}
```

**配置**:

```yaml
spring:
  ai:
    vectorstore:
      chroma:
        client:
          host: localhost
          port: 8000
        collection-name: my_documents
        initialize-schema: true
```

---

## 示例6：RAG问答系统

### 场景说明

实现完整的RAG（检索增强生成）问答系统。

### 实现代码

```java
package com.example.ai.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG问答服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {
    
    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;
    
    /**
     * RAG问答
     */
    public String query(String question) {
        log.info("RAG查询: {}", question);
        
        // 1. 检索相关文档
        List<Document> relevantDocs = vectorStoreService.search(question, 5);
        
        if (relevantDocs.isEmpty()) {
            return "抱歉，我没有找到相关信息。";
        }
        
        // 2. 构建上下文
        String context = relevantDocs.stream()
            .map(doc -> doc.getContent())
            .collect(Collectors.joining("\n\n"));
        
        log.info("检索到上下文，共 {} 个字符", context.length());
        
        // 3. 构建提示词
        String prompt = buildPrompt(question, context);
        
        // 4. LLM生成答案
        String answer = chatClient.call(prompt);
        
        log.info("RAG回答: {}", answer.substring(0, Math.min(100, answer.length())));
        
        return answer;
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String question, String context) {
        return String.format("""
            你是一个专业的问答助手。请基于以下提供的上下文信息回答用户的问题。
            
            上下文信息：
            %s
            
            用户问题：%s
            
            请注意：
            1. 只使用上下文中的信息回答
            2. 如果上下文中没有相关信息，请明确告知用户
            3. 回答要准确、简洁
            4. 如果可能，请引用上下文中的具体内容
            
            回答：
            """, context, question);
    }
    
    /**
     * 索引文档
     */
    public void indexDocuments(List<String> documents) {
        log.info("开始索引文档: {} 条", documents.size());
        
        // 文档分块
        List<String> chunks = documents.stream()
            .flatMap(doc -> chunkDocument(doc).stream())
            .toList();
        
        // 添加到向量库
        vectorStoreService.addDocuments(chunks);
        
        log.info("文档索引完成: {} 个分块", chunks.size());
    }
    
    /**
     * 文档分块
     */
    private List<String> chunkDocument(String document) {
        int chunkSize = 500; // 每块500字符
        int overlap = 50;    // 重叠50字符
        
        List<String> chunks = new ArrayList<>();
        
        for (int i = 0; i < document.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, document.length());
            chunks.add(document.substring(i, end));
            
            if (end == document.length()) {
                break;
            }
        }
        
        return chunks;
    }
}
```

**使用示例**:

```java
// 1. 索引文档
List<String> documents = Arrays.asList(
    "阿凡达2是一部科幻电影，由詹姆斯·卡梅隆执导...",
    "流浪地球3讲述了人类带着地球逃离太阳系的故事..."
);
ragService.indexDocuments(documents);

// 2. 问答
String answer = ragService.query("阿凡达2是谁导演的？");
// 答案：阿凡达2由詹姆斯·卡梅隆执导。
```

---

## 示例7：多轮对话管理

### 场景说明

维护对话历史，实现多轮对话。

### 实现代码

```java
package com.example.ai.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话服务
 */
@Service
@RequiredArgsConstructor
public class ConversationService {
    
    private final ChatClient chatClient;
    
    // 存储对话历史（实际应该使用Redis）
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();
    
    /**
     * 多轮对话
     */
    public String chat(String sessionId, String userMessage) {
        // 1. 获取对话历史
        List<Message> history = conversations.computeIfAbsent(
            sessionId, 
            k -> new ArrayList<>()
        );
        
        // 2. 添加用户消息
        history.add(new UserMessage(userMessage));
        
        // 3. 调用LLM
        ChatResponse response = chatClient.call(new Prompt(history));
        String assistantMessage = response.getResult().getOutput().getContent();
        
        // 4. 添加助手消息
        history.add(new AssistantMessage(assistantMessage));
        
        // 5. 限制历史长度（保留最近10轮对话）
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
        
        return assistantMessage;
    }
    
    /**
     * 清除对话历史
     */
    public void clearHistory(String sessionId) {
        conversations.remove(sessionId);
    }
    
    /**
     * 获取对话历史
     */
    public List<Message> getHistory(String sessionId) {
        return conversations.getOrDefault(sessionId, new ArrayList<>());
    }
}
```

**Controller**:

```java
@PostMapping("/conversation")
public Result<ChatResponse> conversation(
    @RequestHeader("Session-Id") String sessionId,
    @RequestBody ChatRequest request) {
    
    String response = conversationService.chat(sessionId, request.getMessage());
    
    ChatResponse chatResponse = new ChatResponse();
    chatResponse.setMessage(response);
    chatResponse.setSessionId(sessionId);
    
    return success(chatResponse);
}
```

---

## 示例8：Prompt模板

### 场景说明

使用模板管理复杂的提示词。

### 实现代码

```java
package com.example.ai.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Prompt模板服务
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateService {
    
    private final ChatClient chatClient;
    
    /**
     * 使用模板生成回答
     */
    public String generateWithTemplate(String templateName, Map<String, Object> variables) {
        // 获取模板
        String template = getTemplate(templateName);
        
        // 填充变量
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(variables);
        
        // 调用LLM
        return chatClient.call(prompt).getResult().getOutput().getContent();
    }
    
    /**
     * 电影推荐模板
     */
    public String recommendMovies(String genre, String mood) {
        String template = """
            你是一个专业的电影推荐助手。
            
            用户喜好：
            - 类型：{genre}
            - 心情：{mood}
            
            请推荐3部适合的电影，并简要说明推荐理由。
            
            回答格式：
            1. 电影名称 - 推荐理由
            2. 电影名称 - 推荐理由
            3. 电影名称 - 推荐理由
            """;
        
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of(
            "genre", genre,
            "mood", mood
        ));
        
        return chatClient.call(prompt).getResult().getOutput().getContent();
    }
    
    /**
     * 获取模板
     */
    private String getTemplate(String templateName) {
        // 实际应该从配置文件或数据库加载
        Map<String, String> templates = Map.of(
            "summarize", "请总结以下内容：\n\n{content}\n\n要求：简洁、准确、突出重点。",
            "translate", "请将以下{source_lang}翻译成{target_lang}：\n\n{content}",
            "analyze", "请分析以下{type}：\n\n{content}\n\n分析维度：{dimensions}"
        );
        
        return templates.get(templateName);
    }
}
```

---

## 票务系统AI应用场景

### 场景1：智能客服

```java
package com.ticketsystem.ai.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 智能客服服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceAI {
    
    private final ChatClient chatClient;
    private final RAGService ragService;
    
    /**
     * 处理用户咨询
     */
    public String handleInquiry(String userMessage, String userId) {
        log.info("用户 {} 咨询: {}", userId, userMessage);
        
        // 1. 意图识别
        String intent = recognizeIntent(userMessage);
        
        // 2. 根据意图处理
        return switch (intent) {
            case "MOVIE_INQUIRY" -> handleMovieInquiry(userMessage);
            case "ORDER_INQUIRY" -> handleOrderInquiry(userMessage, userId);
            case "REFUND_REQUEST" -> handleRefundRequest(userMessage, userId);
            case "COMPLAINT" -> handleComplaint(userMessage, userId);
            default -> handleGeneralInquiry(userMessage);
        };
    }
    
    /**
     * 意图识别
     */
    private String recognizeIntent(String message) {
        String prompt = String.format("""
            分析以下用户消息的意图，从以下类别中选择一个：
            - MOVIE_INQUIRY: 询问电影信息
            - ORDER_INQUIRY: 询问订单信息
            - REFUND_REQUEST: 退票退款请求
            - COMPLAINT: 投诉建议
            - GENERAL: 一般咨询
            
            用户消息：%s
            
            只返回意图类别，不要其他内容。
            """, message);
        
        return chatClient.call(prompt).trim();
    }
    
    /**
     * 处理电影咨询
     */
    private String handleMovieInquiry(String message) {
        // 使用RAG检索电影信息
        return ragService.query(message);
    }
    
    /**
     * 处理订单咨询
     */
    private String handleOrderInquiry(String message, String userId) {
        // 查询用户订单
        List<Order> orders = orderService.findByUserId(userId);
        
        String orderInfo = orders.stream()
            .map(order -> String.format(
                "订单号：%s，电影：%s，座位：%s，状态：%s",
                order.getId(),
                order.getMovieName(),
                String.join(",", order.getSeatNos()),
                order.getStatus()
            ))
            .collect(Collectors.joining("\n"));
        
        String prompt = String.format("""
            用户订单信息：
            %s
            
            用户问题：%s
            
            请根据订单信息回答用户问题。
            """, orderInfo, message);
        
        return chatClient.call(prompt);
    }
    
    /**
     * 处理退票请求
     */
    private String handleRefundRequest(String message, String userId) {
        String prompt = String.format("""
            用户申请退票。
            
            退票规则：
            - 开场前2小时可免费退票
            - 开场前2小时内退票扣除10%%手续费
            - 开场后不可退票
            
            用户消息：%s
            
            请解释退票规则，并引导用户操作。
            """, message);
        
        return chatClient.call(prompt);
    }
    
    /**
     * 处理投诉
     */
    private String handleComplaint(String message, String userId) {
        // 记录投诉
        complaintService.record(userId, message);
        
        return "您的投诉已记录，我们会在24小时内联系您。感谢您的反馈！";
    }
    
    /**
     * 处理一般咨询
     */
    private String handleGeneralInquiry(String message) {
        return chatClient.call(message);
    }
}
```

### 场景2：电影推荐

```java
package com.ticketsystem.ai.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * 电影推荐服务
 */
@Service
@RequiredArgsConstructor
public class MovieRecommendationService {
    
    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;
    private final EmbeddingService embeddingService;
    
    /**
     * 基于用户历史的电影推荐
     */
    public List<Movie> recommendByHistory(String userId) {
        // 1. 获取用户观影历史
        List<Order> history = orderService.findByUserId(userId);
        
        // 2. 分析用户偏好
        String preferences = analyzePreferences(history);
        
        // 3. 生成推荐
        String prompt = String.format("""
            用户观影偏好：
            %s
            
            当前正在上映的电影：
            %s
            
            请推荐3部最适合该用户的电影，并说明推荐理由。
            """, preferences, getCurrentMovies());
        
        String recommendations = chatClient.call(prompt);
        
        return parseRecommendations(recommendations);
    }
    
    /**
     * 基于内容的电影推荐
     */
    public List<Movie> recommendSimilar(String movieId) {
        // 1. 获取电影信息
        Movie movie = movieService.getById(movieId);
        
        // 2. 生成电影描述的向量
        String description = movie.getName() + " " + movie.getGenre() + " " + movie.getDescription();
        List<Double> vector = embeddingService.embed(description);
        
        // 3. 检索相似电影
        List<Document> similarDocs = vectorStoreService.search(description, 10);
        
        // 4. 提取电影ID
        return similarDocs.stream()
            .map(doc -> doc.getMetadata().get("movieId").toString())
            .map(id -> movieService.getById(id))
            .limit(5)
            .toList();
    }
    
    /**
     * 分析用户偏好
     */
    private String analyzePreferences(List<Order> history) {
        String historyText = history.stream()
            .map(order -> order.getMovieName() + "（" + order.getGenre() + "）")
            .collect(Collectors.joining("、"));
        
        String prompt = String.format("""
            用户观影历史：%s
            
            请分析用户的观影偏好，包括：
            1. 喜欢的电影类型
            2. 喜欢的电影风格
            3. 观影频率和时间偏好
            
            用一段话总结。
            """, historyText);
        
        return chatClient.call(prompt);
    }
}
```

### 场景3：评论分析

```java
package com.ticketsystem.ai.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * 评论分析服务
 */
@Service
@RequiredArgsConstructor
public class ReviewAnalysisService {
    
    private final ChatClient chatClient;
    
    /**
     * 情感分析
     */
    public SentimentResult analyzeSentiment(String review) {
        String prompt = String.format("""
            分析以下电影评论的情感倾向：
            
            评论：%s
            
            返回JSON格式：
            {
                "sentiment": "positive/negative/neutral",
                "score": 0.0-1.0,
                "keywords": ["关键词1", "关键词2"]
            }
            """, review);
        
        String result = chatClient.call(prompt);
        return JsonUtils.fromJson(result, SentimentResult.class);
    }
    
    /**
     * 生成评论摘要
     */
    public String summarizeReviews(List<String> reviews) {
        String allReviews = String.join("\n---\n", reviews);
        
        String prompt = String.format("""
            以下是一部电影的多条评论：
            
            %s
            
            请生成一个综合性的评论摘要，包括：
            1. 总体评价
            2. 主要优点
            3. 主要缺点
            4. 适合观众
            
            摘要：
            """, allReviews);
        
        return chatClient.call(prompt);
    }
    
    /**
     * 提取关键主题
     */
    public List<String> extractTopics(List<String> reviews) {
        String allReviews = String.join("\n", reviews);
        
        String prompt = String.format("""
            从以下电影评论中提取5个最主要的讨论主题：
            
            %s
            
            只返回主题列表，每行一个主题。
            """, allReviews);
        
        String result = chatClient.call(prompt);
        return Arrays.asList(result.split("\n"));
    }
}

@Data
class SentimentResult {
    private String sentiment;
    private double score;
    private List<String> keywords;
}
```

---

## 最佳实践

### 实践1：使用缓存

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 缓存1小时

# Embedding结果缓存
@Cacheable(value = "embeddings", key = "#text")
public List<Double> embed(String text) {
    return embeddingClient.embed(text);
}
```

### 实践2：错误处理

```java
public String chat(String message) {
    try {
        return chatClient.call(message);
    } catch (Exception e) {
        log.error("AI调用失败", e);
        return "抱歉，我暂时无法回答这个问题。请稍后再试。";
    }
}
```

### 实践3：Token控制

```java
ChatResponse response = chatClient.call(
    new Prompt(
        new UserMessage(message),
        OpenAiChatOptions.builder()
            .withMaxTokens(500)  // 限制Token数量
            .build()
    )
);
```

### 实践4：Prompt优化

- 提供清晰的指令
- 使用示例（Few-shot）
- 设置输出格式
- 限定回答范围

---

## 完整示例项目

参考示例项目：
- `nebula-dev-assistant` - MCP文档服务器
- `examples/rag-qa-system` - RAG问答系统

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划
- [Nebula AI文档](../../infrastructure/ai/nebula-ai-spring/README.md) - AI模块详细文档

---

> 如有问题或建议，欢迎提Issue。

