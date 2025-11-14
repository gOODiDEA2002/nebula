# nebula-ai-spring 模块单元测试清单

## 模块说明

Spring AI集成模块，提供统一的AI服务接口，支持对话（Chat）、嵌入（Embeddings）、向量存储（Vector Store）和RAG（检索增强生成）等功能。

## 核心功能

1. 对话服务（ChatService）
2. 嵌入服务（EmbeddingService）
3. 向量存储服务（VectorStoreService）
4. RAG服务（检索增强生成）

## 测试类清单

### 1. ChatServiceTest

**测试类路径**: `io.nebula.ai.spring.ChatService`  
**测试目的**: 验证AI对话功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testChat() | chat(String) | 测试简单对话 | ChatClient |
| testChatWithOptions() | chat(String, ChatOptions) | 测试带选项的对话 | ChatClient |
| testStreamChat() | streamChat(String) | 测试流式对话 | ChatClient |
| testChatWithHistory() | chat(List&lt;Message&gt;) | 测试带历史记录的对话 | ChatClient |

**测试数据准备**:
- Mock ChatClient
- 准备测试问题和回答

**验证要点**:
- 对话请求正确发送
- 响应正确解析
- 流式响应正确处理
- 历史记录正确传递

**Mock示例**:
```java
@Mock
private ChatClient chatClient;

@InjectMocks
private ChatService chatService;

@Test
void testChat() {
    String question = "什么是Spring AI?";
    String expectedAnswer = "Spring AI是一个AI应用框架...";
    
    ChatResponse mockResponse = new ChatResponse(
        List.of(new Generation(expectedAnswer))
    );
    
    when(chatClient.call(any(Prompt.class)))
        .thenReturn(mockResponse);
    
    String answer = chatService.chat(question);
    
    assertThat(answer).isEqualTo(expectedAnswer);
    verify(chatClient).call(any(Prompt.class));
}
```

---

### 2. EmbeddingServiceTest

**测试类路径**: `io.nebula.ai.spring.EmbeddingService`  
**测试目的**: 验证文本嵌入向量生成功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testEmbed() | embed(String) | 测试单文本嵌入 | EmbeddingClient |
| testEmbedList() | embed(List&lt;String&gt;) | 测试批量文本嵌入 | EmbeddingClient |
| testEmbedWithOptions() | embed(String, EmbeddingOptions) | 测试带选项的嵌入 | EmbeddingClient |

**测试数据准备**:
- Mock EmbeddingClient
- 准备测试文本和向量

**验证要点**:
- 嵌入向量正确生成
- 向量维度正确
- 批量嵌入正确

**Mock示例**:
```java
@Mock
private EmbeddingClient embeddingClient;

@InjectMocks
private EmbeddingService embeddingService;

@Test
void testEmbed() {
    String text = "测试文本";
    float[] expectedVector = new float[]{0.1f, 0.2f, 0.3f};
    
    EmbeddingResponse mockResponse = new EmbeddingResponse(
        List.of(new Embedding(expectedVector))
    );
    
    when(embeddingClient.call(any(EmbeddingRequest.class)))
        .thenReturn(mockResponse);
    
    float[] vector = embeddingService.embed(text);
    
    assertThat(vector).hasSize(3);
    assertThat(vector[0]).isEqualTo(0.1f);
}
```

---

### 3. VectorStoreServiceTest

**测试类路径**: `io.nebula.ai.spring.VectorStoreService`  
**测试目的**: 验证向量存储和检索功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testAdd() | add(Document) | 测试添加文档 | VectorStore |
| testAddBatch() | add(List&lt;Document&gt;) | 测试批量添加文档 | VectorStore |
| testSearch() | search(String) | 测试向量搜索 | VectorStore |
| testSearchWithLimit() | search(String, int) | 测试限制结果数量搜索 | VectorStore |
| testDelete() | delete(String) | 测试删除文档 | VectorStore |

**测试数据准备**:
- Mock VectorStore
- 准备测试Document对象

**验证要点**:
- 文档正确添加
- 向量搜索正确
- 相似度计算正确
- 删除操作生效

**Mock示例**:
```java
@Mock
private VectorStore vectorStore;

@InjectMocks
private VectorStoreService vectorStoreService;

@Test
void testAdd() {
    Document document = new Document(
        "doc-1",
        "测试文档内容",
        Map.of("source", "test")
    );
    
    doNothing().when(vectorStore).add(anyList());
    
    vectorStoreService.add(document);
    
    verify(vectorStore).add(argThat(docs -> 
        docs.size() == 1 &&
        docs.get(0).getId().equals("doc-1")
    ));
}
```

---

### 4. RAGServiceTest

**测试类路径**: `io.nebula.ai.spring.RAGService`  
**测试目的**: 验证检索增强生成（RAG）功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testQuery() | query(String) | 测试RAG查询 | VectorStore, ChatClient |
| testQueryWithTopK() | query(String, int) | 测试指定检索数量 | VectorStore, ChatClient |
| testQueryWithPromptTemplate() | - | 测试自定义提示词模板 | VectorStore, ChatClient |

**测试数据准备**:
- Mock VectorStore和ChatClient
- 准备测试查询和相关文档

**验证要点**:
- 检索相关文档
- 构建增强提示词
- 生成正确答案

**Mock示例**:
```java
@Mock
private VectorStore vectorStore;

@Mock
private ChatClient chatClient;

@InjectMocks
private RAGService ragService;

@Test
void testQuery() {
    String question = "Spring AI是什么?";
    
    // Mock检索结果
    Document doc1 = new Document("doc-1", "Spring AI是一个AI框架...");
    when(vectorStore.similaritySearch(any(SearchRequest.class)))
        .thenReturn(List.of(doc1));
    
    // Mock对话结果
    String expectedAnswer = "根据文档，Spring AI是...";
    ChatResponse mockResponse = new ChatResponse(
        List.of(new Generation(expectedAnswer))
    );
    when(chatClient.call(any(Prompt.class)))
        .thenReturn(mockResponse);
    
    String answer = ragService.query(question);
    
    assertThat(answer).contains("Spring AI");
    verify(vectorStore).similaritySearch(any(SearchRequest.class));
    verify(chatClient).call(any(Prompt.class));
}
```

---

### 5. DocumentLoaderTest

**测试类路径**: `io.nebula.ai.spring.DocumentLoader`  
**测试目的**: 验证文档加载和分割功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testLoadText() | load(String) | 测试加载文本文档 | 无 |
| testLoadPdf() | load(File) | 测试加载PDF文档 | PdfReader |
| testSplitDocument() | split(Document) | 测试文档分割 | TextSplitter |

**测试数据准备**:
- 准备测试文件
- Mock文件读取器

**验证要点**:
- 文档正确加载
- 分割策略正确
- 元数据正确设置

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| ChatClient | 对话服务 | Mock call() |
| EmbeddingClient | 嵌入服务 | Mock call() |
| VectorStore | 向量存储 | Mock add(), similaritySearch() |
| PdfReader | PDF加载 | Mock read() |

### 不需要真实AI服务
**所有测试都应该Mock AI客户端，不需要调用真实的AI API**。

---

## 测试依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

```bash
mvn test -pl nebula/infrastructure/ai/nebula-ai-spring
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- Chat、Embedding、VectorStore测试通过
- RAG功能测试通过

