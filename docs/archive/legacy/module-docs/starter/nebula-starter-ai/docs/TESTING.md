# Nebula Starter AI - 测试指南

> AI应用专用Starter的完整测试指南，包括LLM测试、Embedding测试、RAG测试等。

## 测试概览

- [测试环境准备](#测试环境准备)
- [LLM测试](#llm测试)
- [Embedding测试](#embedding测试)
- [向量存储测试](#向量存储测试)
- [RAG测试](#rag测试)
- [集成测试](#集成测试)
- [性能测试](#性能测试)
- [票务系统测试示例](#票务系统测试示例)

---

## 测试环境准备

### Maven依赖

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-ai</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- WireMock（Mock AI API） -->
    <dependency>
        <groupId>com.github.tomakehurst</groupId>
        <artifactId>wiremock-jre8</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 测试配置

`src/test/resources/application-test.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: test-api-key
      base-url: http://localhost:8089  # WireMock地址
    vectorstore:
      chroma:
        client:
          host: localhost
          port: 8000

logging:
  level:
    org.springframework.ai: DEBUG
```

---

## LLM测试

### 单元测试

```java
@SpringBootTest
@DisplayName("ChatService测试")
class ChatServiceTest {
    
    @Autowired
    private ChatService chatService;
    
    private WireMockServer wireMockServer;
    
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    @DisplayName("测试简单对话")
    void testChat() {
        // Mock OpenAI API响应
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "id": "chatcmpl-123",
                        "object": "chat.completion",
                        "created": 1677652288,
                        "choices": [{
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "你好！我是AI助手。"
                            },
                            "finish_reason": "stop"
                        }]
                    }
                    """)));
        
        // 执行测试
        String response = chatService.chat("你好");
        
        // 验证结果
        assertThat(response).isEqualTo("你好！我是AI助手。");
        
        // 验证API调用
        verify(postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }
    
    @Test
    @DisplayName("测试带参数的对话")
    void testChatWithOptions() {
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(getSuccessResponse("这是温度为0.3的回答"))));
        
        String response = chatService.chatWithOptions("测试", 0.3);
        
        assertThat(response).isNotNull();
    }
    
    @Test
    @DisplayName("测试API错误处理")
    void testChatError() {
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(429)
                .withBody("{\"error\": {\"message\": \"Rate limit exceeded\"}}")));
        
        assertThatThrownBy(() -> chatService.chat("测试"))
            .isInstanceOf(AIException.class)
            .hasMessageContaining("Rate limit");
    }
}
```

---

## Embedding测试

```java
@SpringBootTest
@DisplayName("EmbeddingService测试")
class EmbeddingServiceTest {
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Test
    @DisplayName("测试文本Embedding")
    void testEmbed() {
        String text = "这是一段测试文本";
        
        List<Double> vector = embeddingService.embed(text);
        
        assertThat(vector).isNotNull();
        assertThat(vector).hasSize(1536);  // text-embedding-3-small维度
        assertThat(vector).allMatch(v -> v >= -1.0 && v <= 1.0);
    }
    
    @Test
    @DisplayName("测试批量Embedding")
    void testEmbedBatch() {
        List<String> texts = Arrays.asList("文本1", "文本2", "文本3");
        
        List<List<Double>> vectors = embeddingService.embedBatch(texts);
        
        assertThat(vectors).hasSize(3);
        assertThat(vectors.get(0)).hasSize(1536);
    }
    
    @Test
    @DisplayName("测试相似度计算")
    void testSimilarity() {
        String text1 = "苹果手机";
        String text2 = "iPhone";
        String text3 = "香蕉水果";
        
        double sim12 = embeddingService.textSimilarity(text1, text2);
        double sim13 = embeddingService.textSimilarity(text1, text3);
        
        // text1和text2应该更相似
        assertThat(sim12).isGreaterThan(sim13);
        assertThat(sim12).isGreaterThan(0.7);
    }
    
    @Test
    @DisplayName("测试Embedding缓存")
    void testEmbeddingCache() {
        String text = "缓存测试文本";
        
        // 第一次调用
        long start1 = System.currentTimeMillis();
        embeddingService.embed(text);
        long time1 = System.currentTimeMillis() - start1;
        
        // 第二次调用（应该从缓存读取）
        long start2 = System.currentTimeMillis();
        embeddingService.embed(text);
        long time2 = System.currentTimeMillis() - start2;
        
        // 第二次应该明显更快
        assertThat(time2).isLessThan(time1 / 10);
    }
}
```

---

## 向量存储测试

```java
@SpringBootTest
@Testcontainers
@DisplayName("VectorStore测试")
class VectorStoreServiceTest {
    
    @Container
    static GenericContainer<?> chroma = new GenericContainer<>("chromadb/chroma:latest")
        .withExposedPorts(8000);
    
    @DynamicPropertySource
    static void chromaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.vectorstore.chroma.client.host", chroma::getHost);
        registry.add("spring.ai.vectorstore.chroma.client.port", chroma::getFirstMappedPort);
    }
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Test
    @DisplayName("测试添加文档")
    void testAddDocument() {
        String content = "这是一个测试文档";
        Map<String, Object> metadata = Map.of("source", "test", "type", "document");
        
        vectorStoreService.addDocument(content, metadata);
        
        // 验证文档已添加
        List<Document> results = vectorStoreService.search(content, 1);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).isEqualTo(content);
    }
    
    @Test
    @DisplayName("测试批量添加文档")
    void testAddDocuments() {
        List<String> contents = Arrays.asList("文档1", "文档2", "文档3");
        
        vectorStoreService.addDocuments(contents);
        
        // 验证文档数量
        List<Document> results = vectorStoreService.search("文档", 10);
        assertThat(results).hasSizeGreaterThanOrEqualTo(3);
    }
    
    @Test
    @DisplayName("测试相似度检索")
    void testSearch() {
        // 添加测试文档
        vectorStoreService.addDocument("苹果是一种水果", Map.of());
        vectorStoreService.addDocument("iPhone是苹果公司的手机", Map.of());
        vectorStoreService.addDocument("香蕉也是水果", Map.of());
        
        // 搜索
        List<Document> results = vectorStoreService.search("苹果手机", 2);
        
        // 验证结果
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getContent()).contains("iPhone");
    }
    
    @Test
    @DisplayName("测试带阈值的检索")
    void testSearchWithThreshold() {
        vectorStoreService.addDocument("高相关性文档", Map.of());
        vectorStoreService.addDocument("完全不相关的内容", Map.of());
        
        List<Document> results = vectorStoreService.searchWithThreshold(
            "高相关性", 10, 0.7
        );
        
        // 只返回相似度>0.7的文档
        assertThat(results).hasSizeLessThanOrEqualTo(1);
    }
}
```

---

## RAG测试

```java
@SpringBootTest
@DisplayName("RAG服务测试")
class RAGServiceTest {
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        List<String> documents = Arrays.asList(
            "阿凡达2是一部由詹姆斯·卡梅隆执导的科幻电影，于2022年上映。",
            "流浪地球3讲述了人类带着地球逃离太阳系寻找新家园的故事。",
            "阿凡达2的票房超过20亿美元，成为全球票房最高的电影之一。"
        );
        
        ragService.indexDocuments(documents);
    }
    
    @Test
    @DisplayName("测试RAG问答")
    void testQuery() {
        String question = "阿凡达2是谁导演的？";
        
        String answer = ragService.query(question);
        
        assertThat(answer).isNotNull();
        assertThat(answer).containsIgnoringCase("詹姆斯·卡梅隆");
    }
    
    @Test
    @DisplayName("测试无相关信息的问答")
    void testQueryNoContext() {
        String question = "今天天气怎么样？";
        
        String answer = ragService.query(question);
        
        assertThat(answer).contains("没有找到相关信息");
    }
    
    @Test
    @DisplayName("测试文档分块")
    void testChunkDocument() {
        String longDocument = "长文档".repeat(200);
        
        ragService.indexDocuments(List.of(longDocument));
        
        // 验证文档被分块
        List<Document> chunks = vectorStoreService.search(longDocument.substring(0, 50), 10);
        assertThat(chunks).hasSizeGreaterThan(1);
    }
}
```

---

## 集成测试

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@DisplayName("AI集成测试")
class AIIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("测试聊天API")
    void testChatAPI() {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        
        ResponseEntity<Result> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/ai/chat",
            request,
            Result.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo("0");
    }
    
    @Test
    @DisplayName("测试RAG API")
    void testRAGAPI() {
        // 先索引文档
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.setDocuments(List.of("测试文档内容"));
        
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/ai/index",
            indexRequest,
            Result.class
        );
        
        // 再查询
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setQuestion("测试问题");
        
        ResponseEntity<Result> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/ai/query",
            queryRequest,
            Result.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## 性能测试

```java
@SpringBootTest
@DisplayName("AI性能测试")
class AIPerformanceTest {
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Test
    @DisplayName("测试Embedding性能")
    void testEmbeddingPerformance() {
        int count = 100;
        List<String> texts = IntStream.range(0, count)
            .mapToObj(i -> "测试文本" + i)
            .toList();
        
        long start = System.currentTimeMillis();
        embeddingService.embedBatch(texts);
        long time = System.currentTimeMillis() - start;
        
        double avgTime = (double) time / count;
        System.out.println("平均Embedding时间: " + avgTime + "ms");
        
        assertThat(avgTime).isLessThan(100);  // 平均<100ms
    }
    
    @Test
    @DisplayName("测试向量检索性能")
    void testSearchPerformance() {
        // 添加1000个文档
        List<String> documents = IntStream.range(0, 1000)
            .mapToObj(i -> "文档内容" + i)
            .toList();
        
        vectorStoreService.addDocuments(documents);
        
        // 测试检索性能
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            vectorStoreService.search("文档", 10);
        }
        long time = System.currentTimeMillis() - start;
        
        double avgTime = (double) time / 100;
        System.out.println("平均检索时间: " + avgTime + "ms");
        
        assertThat(avgTime).isLessThan(50);  // 平均<50ms
    }
}
```

---

## 票务系统测试示例

### 智能客服测试

```java
@SpringBootTest
@DisplayName("智能客服测试")
class CustomerServiceAITest {
    
    @Autowired
    private CustomerServiceAI customerServiceAI;
    
    @Test
    @DisplayName("测试电影咨询")
    void testMovieInquiry() {
        String response = customerServiceAI.handleInquiry(
            "阿凡达2什么时候上映的？",
            "user123"
        );
        
        assertThat(response).isNotNull();
        assertThat(response).containsIgnoringCase("2022");
    }
    
    @Test
    @DisplayName("测试订单咨询")
    void testOrderInquiry() {
        String response = customerServiceAI.handleInquiry(
            "我的订单什么时候能退票？",
            "user123"
        );
        
        assertThat(response).contains("订单");
    }
    
    @Test
    @DisplayName("测试意图识别")
    void testIntentRecognition() {
        Map<String, String> testCases = Map.of(
            "阿凡达2好看吗？", "MOVIE_INQUIRY",
            "我的订单在哪里？", "ORDER_INQUIRY",
            "我要退票", "REFUND_REQUEST",
            "你们的服务太差了", "COMPLAINT"
        );
        
        testCases.forEach((message, expectedIntent) -> {
            String response = customerServiceAI.handleInquiry(message, "user123");
            // 验证响应内容符合意图
            assertThat(response).isNotNull();
        });
    }
}
```

### 电影推荐测试

```java
@SpringBootTest
@DisplayName("电影推荐测试")
class MovieRecommendationServiceTest {
    
    @Autowired
    private MovieRecommendationService recommendationService;
    
    @Test
    @DisplayName("测试基于历史的推荐")
    void testRecommendByHistory() {
        List<Movie> recommendations = recommendationService.recommendByHistory("user123");
        
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).hasSizeLessThanOrEqualTo(5);
    }
    
    @Test
    @DisplayName("测试相似电影推荐")
    void testRecommendSimilar() {
        List<Movie> similar = recommendationService.recommendSimilar("movie123");
        
        assertThat(similar).isNotEmpty();
        assertThat(similar).hasSizeLessThanOrEqualTo(5);
    }
}
```

### 评论分析测试

```java
@SpringBootTest
@DisplayName("评论分析测试")
class ReviewAnalysisServiceTest {
    
    @Autowired
    private ReviewAnalysisService reviewAnalysisService;
    
    @Test
    @DisplayName("测试情感分析-积极")
    void testSentimentPositive() {
        String review = "这部电影太棒了！强烈推荐！";
        
        SentimentResult result = reviewAnalysisService.analyzeSentiment(review);
        
        assertThat(result.getSentiment()).isEqualTo("positive");
        assertThat(result.getScore()).isGreaterThan(0.7);
    }
    
    @Test
    @DisplayName("测试情感分析-消极")
    void testSentimentNegative() {
        String review = "太无聊了，浪费时间和金钱。";
        
        SentimentResult result = reviewAnalysisService.analyzeSentiment(review);
        
        assertThat(result.getSentiment()).isEqualTo("negative");
        assertThat(result.getScore()).isLessThan(0.3);
    }
    
    @Test
    @DisplayName("测试评论摘要生成")
    void testSummarizeReviews() {
        List<String> reviews = Arrays.asList(
            "特效很棒，故事情节也不错。",
            "演员表演很到位，值得一看。",
            "有点长，但总体还可以。"
        );
        
        String summary = reviewAnalysisService.summarizeReviews(reviews);
        
        assertThat(summary).isNotNull();
        assertThat(summary).contains("特效");
    }
}
```

---

## 测试覆盖率

### 配置JaCoCo

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <excludes>
            <exclude>**/dto/**</exclude>
            <exclude>**/config/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

运行测试：

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

---

## 最佳实践

1. **Mock AI API**：使用WireMock避免实际调用API
2. **使用Testcontainers**：确保向量数据库测试环境一致
3. **测试缓存**：验证Embedding缓存效果
4. **性能基准**：建立性能基准测试
5. **集成测试**：测试完整AI流程

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置参考
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

