# Nebula AI Spring - 使用示例

> Spring AI集成完整使用指南，以票务系统智能推荐为例

## 目录

- [快速开始](#快速开始)
- [聊天对话](#聊天对话)
- [文本嵌入](#文本嵌入)
- [向量存储](#向量存储)
- [RAG检索增强](#rag检索增强)
- [智能推荐](#智能推荐)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-ai-spring</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  ai:
    enabled: true
    
    # 聊天服务配置
    chat:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: gpt-4
          temperature: 0.7
          max-tokens: 2000
    
    # 嵌入服务配置
    embedding:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: text-embedding-3-small
    
    # 向量存储配置
    vector-store:
      default-provider: chroma
      chroma:
        host: localhost
        port: 8000
        collection-name: ticket-knowledge
```

---

## 聊天对话

### 1. 基础对话

```java
/**
 * AI聊天服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatService {
    
    private final ChatService chatService;
    
    /**
     * 简单对话
     */
    public String chat(String userMessage) {
        ChatResponse response = chatService.chat(userMessage);
        
        log.info("AI回复：{}", response.getContent());
        
        return response.getContent();
    }
    
    /**
     * 带上下文的对话
     */
    public String chatWithContext(List<ChatMessage> history, String userMessage) {
        // 添加用户消息
        history.add(ChatMessage.user(userMessage));
        
        ChatResponse response = chatService.chat(history);
        
        // 添加AI回复到历史
        history.add(ChatMessage.assistant(response.getContent()));
        
        return response.getContent();
    }
}
```

### 2. 票务咨询助手

```java
/**
 * 票务咨询AI助手
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingAssistantService {
    
    private final ChatService chatService;
    private final ShowtimeService showtimeService;
    
    /**
     * 处理用户咨询
     */
    public String handleUserQuery(String query, Long userId) {
        // 1. 构建系统提示
        String systemPrompt = """
                你是一个专业的票务咨询助手。
                请根据用户的问题，提供准确、友好的回答。
                如果涉及具体演出信息，请基于提供的数据回答。
                如果不确定，请诚实告知用户。
                """;
        
        // 2. 获取相关演出信息
        List<Showtime> relevantShowtimes = findRelevantShowtimes(query);
        
        // 3. 构建上下文
        String context = buildShowtimeContext(relevantShowtimes);
        
        // 4. 构建完整提示
        String fullPrompt = String.format("""
                %s
                
                当前可用的演出信息：
                %s
                
                用户问题：%s
                """, systemPrompt, context, query);
        
        // 5. 调用AI
        ChatResponse response = chatService.chat(fullPrompt);
        
        log.info("AI助手回复用户{}：{}", userId, response.getContent());
        
        return response.getContent();
    }
    
    private List<Showtime> findRelevantShowtimes(String query) {
        // 从数据库或搜索引擎获取相关演出
        return showtimeService.searchShowtimes(query, 5);
    }
    
    private String buildShowtimeContext(List<Showtime> showtimes) {
        return showtimes.stream()
                .map(s -> String.format("- %s，时间：%s，价格：%s元，地点：%s",
                        s.getTitle(), s.getShowTime(), s.getPrice(), s.getVenue()))
                .collect(Collectors.joining("\n"));
    }
}
```

---

## 文本嵌入

### 1. 基础嵌入

```java
/**
 * 文本嵌入服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    
    private final io.nebula.ai.core.embedding.EmbeddingService embeddingService;
    
    /**
     * 生成文本嵌入向量
     */
    public float[] embed(String text) {
        float[] embedding = embeddingService.embed(text);
        
        log.info("生成嵌入向量：文本长度={}, 向量维度={}", 
                text.length(), embedding.length);
        
        return embedding;
    }
    
    /**
     * 批量生成嵌入向量
     */
    public List<float[]> batchEmbed(List<String> texts) {
        List<float[]> embeddings = embeddingService.batchEmbed(texts);
        
        log.info("批量生成嵌入向量：文本数={}", texts.size());
        
        return embeddings;
    }
    
    /**
     * 计算文本相似度
     */
    public double similarity(String text1, String text2) {
        float[] embedding1 = embed(text1);
        float[] embedding2 = embed(text2);
        
        return cosineSimilarity(embedding1, embedding2);
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

### 2. 演出相似度计算

```java
/**
 * 演出相似度服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeSimilarityService {
    
    private final EmbeddingService embeddingService;
    
    /**
     * 计算演出相似度
     */
    public double calculateSimilarity(Showtime showtime1, Showtime showtime2) {
        // 构建演出描述
        String desc1 = buildShowtimeDescription(showtime1);
        String desc2 = buildShowtimeDescription(showtime2);
        
        // 计算相似度
        return embeddingService.similarity(desc1, desc2);
    }
    
    /**
     * 查找相似演出
     */
    public List<Showtime> findSimilarShowtimes(Showtime targetShowtime, 
                                               List<Showtime> candidates, 
                                               int topK) {
        String targetDesc = buildShowtimeDescription(targetShowtime);
        
        // 计算所有候选演出的相似度
        List<SimilarityScore> scores = candidates.stream()
                .map(candidate -> {
                    String candidateDesc = buildShowtimeDescription(candidate);
                    double similarity = embeddingService.similarity(targetDesc, candidateDesc);
                    return new SimilarityScore(candidate, similarity);
                })
                .sorted(Comparator.comparingDouble(SimilarityScore::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        
        return scores.stream()
                .map(SimilarityScore::getShowtime)
                .collect(Collectors.toList());
    }
    
    private String buildShowtimeDescription(Showtime showtime) {
        return String.format("%s %s %s %s %s",
                showtime.getTitle(),
                showtime.getPerformer(),
                showtime.getCategory(),
                showtime.getVenue(),
                showtime.getDescription());
    }
}

@Data
@AllArgsConstructor
class SimilarityScore {
    private Showtime showtime;
    private double score;
}
```

---

## 向量存储

### 1. 存储和检索

```java
/**
 * 向量存储服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {
    
    private final io.nebula.ai.core.vectorstore.VectorStoreService vectorStoreService;
    private final EmbeddingService embeddingService;
    
    /**
     * 存储文档
     */
    public void storeDocument(String id, String content, Map<String, Object> metadata) {
        // 生成嵌入向量
        float[] embedding = embeddingService.embed(content);
        
        // 创建文档
        Document document = Document.builder()
                .id(id)
                .content(content)
                .embedding(embedding)
                .metadata(metadata)
                .build();
        
        // 存储到向量数据库
        vectorStoreService.add(document);
        
        log.info("文档已存储：id={}", id);
    }
    
    /**
     * 批量存储文档
     */
    public void batchStoreDocuments(List<Document> documents) {
        // 批量生成嵌入向量
        List<String> contents = documents.stream()
                .map(Document::getContent)
                .collect(Collectors.toList());
        
        List<float[]> embeddings = embeddingService.batchEmbed(contents);
        
        // 设置嵌入向量
        for (int i = 0; i < documents.size(); i++) {
            documents.get(i).setEmbedding(embeddings.get(i));
        }
        
        // 批量存储
        vectorStoreService.addAll(documents);
        
        log.info("批量存储文档：共{}条", documents.size());
    }
    
    /**
     * 相似度检索
     */
    public List<Document> search(String query, int topK) {
        // 生成查询向量
        float[] queryEmbedding = embeddingService.embed(query);
        
        // 向量检索
        List<Document> results = vectorStoreService.similaritySearch(queryEmbedding, topK);
        
        log.info("相似度检索：query={}, 结果数={}", query, results.size());
        
        return results;
    }
}
```

### 2. 演出知识库

```java
/**
 * 演出知识库服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeKnowledgeBaseService {
    
    private final VectorStoreService vectorStoreService;
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * 构建演出知识库
     */
    @PostConstruct
    public void buildKnowledgeBase() {
        log.info("开始构建演出知识库");
        
        // 1. 从数据库获取所有演出
        List<Showtime> showtimes = showtimeMapper.selectAll();
        
        // 2. 转换为文档
        List<Document> documents = showtimes.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
        
        // 3. 批量存储到向量数据库
        vectorStoreService.batchStoreDocuments(documents);
        
        log.info("演出知识库构建完成：共{}个演出", showtimes.size());
    }
    
    /**
     * 搜索相关演出
     */
    public List<Showtime> searchRelevantShowtimes(String query, int topK) {
        // 1. 向量检索
        List<Document> documents = vectorStoreService.search(query, topK);
        
        // 2. 提取演出ID
        List<Long> showtimeIds = documents.stream()
                .map(doc -> (Long) doc.getMetadata().get("showtimeId"))
                .collect(Collectors.toList());
        
        // 3. 从数据库获取完整信息
        return showtimeMapper.selectBatchIds(showtimeIds);
    }
    
    private Document convertToDocument(Showtime showtime) {
        // 构建文档内容
        String content = String.format("""
                标题：%s
                表演者：%s
                分类：%s
                地点：%s
                时间：%s
                价格：%s元
                描述：%s
                标签：%s
                """,
                showtime.getTitle(),
                showtime.getPerformer(),
                showtime.getCategory(),
                showtime.getVenue(),
                showtime.getShowTime(),
                showtime.getPrice(),
                showtime.getDescription(),
                showtime.getTags());
        
        // 元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("showtimeId", showtime.getId());
        metadata.put("category", showtime.getCategory());
        metadata.put("price", showtime.getPrice());
        metadata.put("showTime", showtime.getShowTime());
        
        return Document.builder()
                .id(showtime.getId().toString())
                .content(content)
                .metadata(metadata)
                .build();
    }
}
```

---

## RAG检索增强

### 1. 基础RAG

```java
/**
 * RAG检索增强服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {
    
    private final VectorStoreService vectorStoreService;
    private final ChatService chatService;
    
    /**
     * RAG问答
     */
    public String ragQuery(String question) {
        // 1. 检索相关文档
        List<Document> relevantDocs = vectorStoreService.search(question, 5);
        
        // 2. 构建上下文
        String context = relevantDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));
        
        // 3. 构建提示
        String prompt = String.format("""
                基于以下上下文信息回答问题。
                如果上下文中没有相关信息，请诚实告知。
                
                上下文：
                %s
                
                问题：%s
                
                请提供准确、简洁的回答：
                """, context, question);
        
        // 4. 调用LLM
        ChatResponse response = chatService.chat(prompt);
        
        log.info("RAG问答：question={}, answer={}", question, response.getContent());
        
        return response.getContent();
    }
}
```

### 2. 票务智能问答

```java
/**
 * 票务智能问答服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingQAService {
    
    private final ShowtimeKnowledgeBaseService knowledgeBaseService;
    private final ChatService chatService;
    
    /**
     * 智能问答
     */
    public TicketingQAResponse answer(String question, Long userId) {
        // 1. 检索相关演出
        List<Showtime> relevantShowtimes = knowledgeBaseService.searchRelevantShowtimes(question, 5);
        
        // 2. 构建上下文
        String context = buildContext(relevantShowtimes);
        
        // 3. 生成回答
        String answer = generateAnswer(question, context);
        
        // 4. 构建响应
        TicketingQAResponse response = new TicketingQAResponse();
        response.setQuestion(question);
        response.setAnswer(answer);
        response.setRelevantShowtimes(relevantShowtimes);
        
        log.info("智能问答：userId={}, question={}", userId, question);
        
        return response;
    }
    
    private String buildContext(List<Showtime> showtimes) {
        return showtimes.stream()
                .map(s -> String.format("""
                        【%s】
                        表演者：%s
                        时间：%s
                        地点：%s
                        价格：%s元
                        描述：%s
                        """,
                        s.getTitle(),
                        s.getPerformer(),
                        s.getShowTime(),
                        s.getVenue(),
                        s.getPrice(),
                        s.getDescription()))
                .collect(Collectors.joining("\n"));
    }
    
    private String generateAnswer(String question, String context) {
        String prompt = String.format("""
                你是一个专业的票务咨询助手。
                请基于以下演出信息，回答用户的问题。
                
                演出信息：
                %s
                
                用户问题：%s
                
                请提供专业、友好的回答，如果需要推荐演出，请说明推荐理由：
                """, context, question);
        
        ChatResponse response = chatService.chat(prompt);
        
        return response.getContent();
    }
}

@Data
public class TicketingQAResponse {
    private String question;
    private String answer;
    private List<Showtime> relevantShowtimes;
}
```

---

## 智能推荐

### 1. 基于内容的推荐

```java
/**
 * 基于内容的推荐服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentBasedRecommendationService {
    
    private final ShowtimeSimilarityService similarityService;
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * 推荐相似演出
     */
    public List<Showtime> recommendSimilar(Long showtimeId, int topK) {
        // 1. 获取目标演出
        Showtime targetShowtime = showtimeMapper.selectById(showtimeId);
        
        // 2. 获取候选演出
        List<Showtime> candidates = showtimeMapper.selectUpcoming();
        
        // 3. 过滤掉目标演出本身
        candidates = candidates.stream()
                .filter(s -> !s.getId().equals(showtimeId))
                .collect(Collectors.toList());
        
        // 4. 查找相似演出
        List<Showtime> similar = similarityService.findSimilarShowtimes(
                targetShowtime, candidates, topK);
        
        log.info("推荐相似演出：showtimeId={}, 推荐数={}", showtimeId, similar.size());
        
        return similar;
    }
}
```

### 2. 基于用户的个性化推荐

```java
/**
 * 个性化推荐服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizedRecommendationService {
    
    private final VectorStoreService vectorStoreService;
    private final UserPreferenceService userPreferenceService;
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * 个性化推荐
     */
    public List<Showtime> recommendForUser(Long userId, int topK) {
        // 1. 获取用户偏好
        UserPreference preference = userPreferenceService.getUserPreference(userId);
        
        // 2. 构建用户画像查询
        String userProfileQuery = buildUserProfileQuery(preference);
        
        // 3. 向量检索
        List<Document> documents = vectorStoreService.search(userProfileQuery, topK * 2);
        
        // 4. 提取演出ID
        List<Long> showtimeIds = documents.stream()
                .map(doc -> (Long) doc.getMetadata().get("showtimeId"))
                .limit(topK)
                .collect(Collectors.toList());
        
        // 5. 获取演出详情
        List<Showtime> recommendations = showtimeMapper.selectBatchIds(showtimeIds);
        
        log.info("个性化推荐：userId={}, 推荐数={}", userId, recommendations.size());
        
        return recommendations;
    }
    
    private String buildUserProfileQuery(UserPreference preference) {
        return String.format("""
                用户偏好：
                - 喜欢的分类：%s
                - 喜欢的表演者：%s
                - 价格范围：%s-%s元
                - 地区偏好：%s
                """,
                String.join("、", preference.getFavoriteCategories()),
                String.join("、", preference.getFavoritePerformers()),
                preference.getMinPrice(),
                preference.getMaxPrice(),
                String.join("、", preference.getFavoriteLocations()));
    }
}
```

---

## 票务系统完整示例

### 完整的AI驱动票务服务

```java
/**
 * AI驱动的票务服务（完整示例）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AITicketingService {
    
    private final TicketingAssistantService assistantService;
    private final TicketingQAService qaService;
    private final PersonalizedRecommendationService recommendationService;
    private final ChatService chatService;
    
    /**
     * 1. 智能客服对话
     */
    public ConversationResponse handleConversation(Long userId, String message, 
                                                   List<ChatMessage> history) {
        // 1. 调用AI助手
        String response = assistantService.handleUserQuery(message, userId);
        
        // 2. 更新对话历史
        history.add(ChatMessage.user(message));
        history.add(ChatMessage.assistant(response));
        
        // 3. 构建响应
        ConversationResponse result = new ConversationResponse();
        result.setResponse(response);
        result.setHistory(history);
        
        return result;
    }
    
    /**
     * 2. 智能搜索和推荐
     */
    public SmartSearchResponse smartSearch(String query, Long userId) {
        // 2.1 智能问答（理解用户意图）
        TicketingQAResponse qaResponse = qaService.answer(query, userId);
        
        // 2.2 个性化推荐
        List<Showtime> personalizedRecommendations = 
                recommendationService.recommendForUser(userId, 5);
        
        // 2.3 组合结果
        SmartSearchResponse response = new SmartSearchResponse();
        response.setAnswer(qaResponse.getAnswer());
        response.setRelevantShowtimes(qaResponse.getRelevantShowtimes());
        response.setPersonalizedRecommendations(personalizedRecommendations);
        
        return response;
    }
    
    /**
     * 3. 自动生成演出介绍
     */
    public String generateShowtimeIntroduction(Showtime showtime) {
        String prompt = String.format("""
                请为以下演出生成一段吸引人的介绍文案（200字左右）：
                
                标题：%s
                表演者：%s
                分类：%s
                时间：%s
                地点：%s
                价格：%s元
                
                要求：
                1. 突出演出特色和亮点
                2. 语言生动、有感染力
                3. 适合在票务平台展示
                """,
                showtime.getTitle(),
                showtime.getPerformer(),
                showtime.getCategory(),
                showtime.getShowTime(),
                showtime.getVenue(),
                showtime.getPrice());
        
        ChatResponse response = chatService.chat(prompt);
        
        log.info("生成演出介绍：showtimeId={}", showtime.getId());
        
        return response.getContent();
    }
    
    /**
     * 4. 智能分类和标签提取
     */
    public ShowtimeCategorizationResult categorizeShowtime(String title, String description) {
        String prompt = String.format("""
                请分析以下演出信息，提取分类和标签：
                
                标题：%s
                描述：%s
                
                请以JSON格式返回：
                {
                  "category": "分类名称（如：音乐会、话剧、体育赛事等）",
                  "tags": ["标签1", "标签2", "标签3"],
                  "targetAudience": "目标受众描述"
                }
                """,
                title,
                description);
        
        ChatResponse response = chatService.chat(prompt);
        
        // 解析JSON响应
        return parseCategorizationResult(response.getContent());
    }
    
    /**
     * 5. 用户意图识别
     */
    public UserIntent recognizeIntent(String userMessage) {
        String prompt = String.format("""
                请识别用户消息的意图：
                
                用户消息：%s
                
                可能的意图包括：
                - SEARCH_SHOWTIME（搜索演出）
                - BUY_TICKET（购买门票）
                - REFUND_TICKET（退票）
                - ASK_QUESTION（咨询问题）
                - RECOMMEND（请求推荐）
                
                请返回JSON格式：
                {
                  "intent": "意图类型",
                  "confidence": 0.95,
                  "entities": {
                    "category": "分类",
                    "performer": "表演者",
                    "date": "日期"
                  }
                }
                """,
                userMessage);
        
        ChatResponse response = chatService.chat(prompt);
        
        return parseUserIntent(response.getContent());
    }
    
    // 辅助方法
    
    private ShowtimeCategorizationResult parseCategorizationResult(String jsonResponse) {
        // JSON解析逻辑
        return new ShowtimeCategorizationResult();
    }
    
    private UserIntent parseUserIntent(String jsonResponse) {
        // JSON解析逻辑
        return new UserIntent();
    }
}

/**
 * 对话响应
 */
@Data
public class ConversationResponse {
    private String response;
    private List<ChatMessage> history;
}

/**
 * 智能搜索响应
 */
@Data
public class SmartSearchResponse {
    private String answer;
    private List<Showtime> relevantShowtimes;
    private List<Showtime> personalizedRecommendations;
}

/**
 * 演出分类结果
 */
@Data
public class ShowtimeCategorizationResult {
    private String category;
    private List<String> tags;
    private String targetAudience;
}

/**
 * 用户意图
 */
@Data
public class UserIntent {
    private String intent;
    private double confidence;
    private Map<String, String> entities;
}
```

---

## 最佳实践

### 1. 提示工程

- **明确指令**：清晰描述任务和期望输出
- **提供上下文**：包含必要的背景信息
- **结构化输出**：要求JSON等结构化格式
- **few-shot示例**：提供示例输入输出

### 2. 成本优化

- **模型选择**：根据任务复杂度选择合适模型
- **缓存结果**：缓存常见查询结果
- **批量处理**：合并多个请求
- **流式输出**：长文本使用流式输出

### 3. 性能优化

- **异步调用**：非关键路径使用异步
- **超时控制**：设置合理超时时间
- **重试机制**：API调用失败时重试
- **并发控制**：限制并发请求数

### 4. 数据安全

- **API密钥管理**：使用环境变量
- **数据脱敏**：敏感信息脱敏处理
- **访问控制**：限制API访问权限
- **审计日志**：记录所有AI调用

### 5. 质量保障

- **输出验证**：验证AI输出格式
- **fallback机制**：AI失败时的降级方案
- **人工审核**：关键内容人工审核
- **A/B测试**：对比不同提示效果

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0
