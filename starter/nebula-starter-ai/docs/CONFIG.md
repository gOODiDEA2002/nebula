# Nebula Starter AI - 配置参考

> AI应用专用Starter的完整配置说明，包括LLM、Embedding、向量数据库、缓存等配置。

## 配置概览

- [基础配置](#基础配置)
- [LLM配置](#llm配置)
- [Embedding配置](#embedding配置)
- [向量数据库配置](#向量数据库配置)
- [缓存配置](#缓存配置)
- [票务系统配置示例](#票务系统配置示例)

---

## 基础配置

### Maven依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>

<!-- LLM提供商（选择一个） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- 向量数据库（选择一个） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-chroma-store-spring-boot-starter</artifactId>
</dependency>
```

### 最小配置

`application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

---

## LLM配置

### OpenAI配置

`application.yml`:

```yaml
spring:
  ai:
    openai:
      # API密钥
      api-key: ${OPENAI_API_KEY}
      # 基础URL（可选，用于代理）
      base-url: https://api.openai.com
      # 组织ID（可选）
      organization-id: ${OPENAI_ORG_ID:}
      
      # 聊天模型配置
      chat:
        enabled: true
        options:
          # 模型名称
          model: gpt-4
          # 温度（0.0-2.0）
          temperature: 0.7
          # 最大Token数
          max-tokens: 2000
          # Top P采样
          top-p: 1.0
          # 频率惩罚
          frequency-penalty: 0.0
          # 存在惩罚
          presence-penalty: 0.0
          # 停止序列
          stop: []
          
      # Embedding模型配置
      embedding:
        enabled: true
        options:
          model: text-embedding-3-small
          # 维度（可选）
          dimensions: 1536
```

### Azure OpenAI配置

```yaml
spring:
  ai:
    azure:
      openai:
        # API密钥
        api-key: ${AZURE_OPENAI_API_KEY}
        # 终端点
        endpoint: https://your-resource.openai.azure.com
        
        # 聊天模型
        chat:
          options:
            deployment-name: gpt-4
            temperature: 0.7
            max-tokens: 2000
            
        # Embedding模型
        embedding:
          options:
            deployment-name: text-embedding-ada-002
```

### DeepSeek配置

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
          max-tokens: 2000
```

### Ollama配置（本地模型）

```yaml
spring:
  ai:
    ollama:
      # 基础URL
      base-url: http://localhost:11434
      
      # 聊天模型
      chat:
        enabled: true
        options:
          model: llama2
          # 或其他模型：mistral, codellama, etc.
          temperature: 0.7
          num-predict: 2000
          
      # Embedding模型
      embedding:
        enabled: true
        options:
          model: nomic-embed-text
```

---

## Embedding配置

### OpenAI Embedding

```yaml
spring:
  ai:
    openai:
      embedding:
        enabled: true
        options:
          # 模型选择
          model: text-embedding-3-small  # 或 text-embedding-3-large
          # 输出维度（仅3系列模型支持）
          dimensions: 1536
          # 批量大小
          batch-size: 16
```

### Ollama Embedding（本地）

```yaml
spring:
  ai:
    ollama:
      embedding:
        enabled: true
        options:
          model: nomic-embed-text
          # 可选模型：
          # - nomic-embed-text (维度: 768)
          # - mxbai-embed-large (维度: 1024)
```

### 自定义Embedding服务

```java
@Configuration
public class CustomEmbeddingConfig {
    
    @Bean
    @ConditionalOnProperty(prefix = "custom.embedding", name = "enabled", havingValue = "true")
    public EmbeddingClient customEmbeddingClient() {
        return new CustomEmbeddingClient();
    }
}
```

```yaml
custom:
  embedding:
    enabled: true
    endpoint: http://your-embedding-service:8080
    model: your-model-name
```

---

## 向量数据库配置

### Chroma配置

```yaml
spring:
  ai:
    vectorstore:
      chroma:
        # 客户端配置
        client:
          # Chroma服务地址
          host: localhost
          port: 8000
          # 密钥令牌（如果启用认证）
          key-token: ${CHROMA_API_KEY:}
          
        # 集合配置
        collection-name: my_documents
        # 是否初始化Schema
        initialize-schema: true
        # 距离函数: l2, ip, cosine
        distance-function: cosine
```

**启动Chroma**:

```bash
# Docker方式
docker run -d -p 8000:8000 chromadb/chroma:latest

# Python方式
pip install chromadb
chroma run --host localhost --port 8000
```

### Pinecone配置

```yaml
spring:
  ai:
    vectorstore:
      pinecone:
        # API密钥
        api-key: ${PINECONE_API_KEY}
        # 环境
        environment: us-east-1-aws
        # 项目ID
        project-id: ${PINECONE_PROJECT_ID}
        # 索引名称
        index-name: my-index
        # 命名空间
        namespace: default
```

### Milvus配置

```yaml
spring:
  ai:
    vectorstore:
      milvus:
        # 连接配置
        host: localhost
        port: 19530
        # 数据库名称
        database-name: default
        # 集合名称
        collection-name: my_collection
        # 索引类型
        index-type: IVF_FLAT
        # 度量类型
        metric-type: L2
```

### Qdrant配置

```yaml
spring:
  ai:
    vectorstore:
      qdrant:
        # 连接配置
        host: localhost
        port: 6333
        # API密钥（如果启用）
        api-key: ${QDRANT_API_KEY:}
        # 集合名称
        collection-name: my_collection
        # 是否使用gRPC
        use-grpc: false
```

---

## 缓存配置

### Redis缓存配置

```yaml
spring:
  # Redis配置
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          
  # 缓存配置
  cache:
    type: redis
    redis:
      # 缓存过期时间
      time-to-live: 3600000  # 1小时
      # 缓存键前缀
      key-prefix: "ai:"
      # 是否缓存null值
      cache-null-values: false
```

### 多级缓存配置

```yaml
nebula:
  cache:
    # 启用多级缓存
    multilevel:
      enabled: true
      # L1缓存（本地缓存）
      l1:
        # 最大条目数
        max-size: 1000
        # 写入后过期时间（秒）
        expire-after-write: 300
        # 访问后过期时间（秒）
        expire-after-access: 600
      # L2缓存（Redis）
      l2:
        enabled: true
        expire-after-write: 3600
```

### Embedding缓存配置

```java
@Configuration
@EnableCaching
public class EmbeddingCacheConfig {
    
    @Bean
    public CacheManager embeddingCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))  // Embedding缓存24小时
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

---

## 票务系统配置示例

### 智能客服配置

`application.yml`:

```yaml
spring:
  application:
    name: ticket-ai-service
  
  # OpenAI配置
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.3  # 客服回答要准确，降低温度
          max-tokens: 500
      embedding:
        options:
          model: text-embedding-3-small
    
    # Chroma向量数据库
    vectorstore:
      chroma:
        client:
          host: localhost
          port: 8000
        collection-name: ticket_knowledge_base
        initialize-schema: true
  
  # Redis缓存
  data:
    redis:
      host: localhost
      port: 6379
  
  cache:
    type: redis
    redis:
      time-to-live: 3600000

# 业务配置
ticket:
  ai:
    # 客服配置
    customer-service:
      # 是否启用AI客服
      enabled: true
      # 人工客服转接阈值（置信度）
      transfer-threshold: 0.6
      # 最大对话轮数
      max-conversation-rounds: 10
      
    # 推荐配置
    recommendation:
      # 是否启用智能推荐
      enabled: true
      # 推荐电影数量
      count: 5
      # 推荐策略
      strategy: hybrid  # content-based, collaborative, hybrid
      
    # 评论分析配置
    review-analysis:
      # 是否启用评论分析
      enabled: true
      # 情感分析阈值
      sentiment-threshold: 0.5
```

### RAG知识库配置

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.2  # RAG要准确，进一步降低温度
          max-tokens: 1000
      embedding:
        options:
          model: text-embedding-3-large  # 使用更大的模型提高准确性
          dimensions: 3072
    
    vectorstore:
      chroma:
        client:
          host: localhost
          port: 8000
        # 多个集合
        collections:
          - name: movie_info
            description: 电影信息库
          - name: faq
            description: 常见问题库
          - name: policy
            description: 政策规则库

# RAG配置
rag:
  # 检索配置
  retrieval:
    # TopK数量
    top-k: 5
    # 相似度阈值
    similarity-threshold: 0.7
    # 是否启用重排序
    rerank: true
    
  # 文档分块配置
  chunking:
    # 分块大小（字符数）
    chunk-size: 500
    # 重叠大小
    chunk-overlap: 50
    
  # Prompt配置
  prompt:
    # 系统提示词
    system: |
      你是票务系统的智能助手。
      请基于提供的上下文信息回答用户问题。
      如果信息不足，请明确告知用户。
    # 上下文模板
    context-template: |
      相关信息：
      {context}
      
      用户问题：{query}
```

### 电影推荐配置

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.8  # 推荐可以更有创意
          max-tokens: 800

# 推荐配置
recommendation:
  # 推荐算法配置
  algorithms:
    # 基于内容的推荐
    content-based:
      enabled: true
      weight: 0.4
      # 特征权重
      features:
        genre: 0.3
        director: 0.2
        actors: 0.2
        tags: 0.3
        
    # 协同过滤推荐
    collaborative:
      enabled: true
      weight: 0.3
      # 相似用户数量
      similar-users: 50
      
    # AI推荐
    ai-based:
      enabled: true
      weight: 0.3
      # Prompt模板
      prompt-template: |
        用户观影历史：{history}
        用户偏好：{preferences}
        当前上映电影：{movies}
        
        请推荐{count}部最适合的电影。
  
  # 缓存配置
  cache:
    # 用户推荐结果缓存时间（秒）
    user-recommendations-ttl: 3600
    # 相似电影缓存时间（秒）
    similar-movies-ttl: 7200
```

### 评论分析配置

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-3.5-turbo  # 评论分析用3.5够用且成本低
          temperature: 0.1  # 分析要客观
          max-tokens: 300

# 评论分析配置
review-analysis:
  # 情感分析
  sentiment:
    # 是否启用
    enabled: true
    # 批量分析大小
    batch-size: 10
    # 情感分类
    categories:
      - positive
      - negative
      - neutral
    
  # 主题提取
  topic-extraction:
    enabled: true
    # 最大主题数
    max-topics: 5
    # 最小文档数
    min-documents: 10
    
  # 摘要生成
  summarization:
    enabled: true
    # 最大评论数
    max-reviews: 50
    # 摘要长度
    summary-length: 200
```

### 完整配置文件

`application-prod.yml`（生产环境）:

```yaml
# ==================== 应用配置 ====================
spring:
  application:
    name: ticket-ai-service
  
  profiles:
    active: prod

# ==================== AI配置 ====================
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com
      chat:
        options:
          model: gpt-4
          temperature: 0.7
          max-tokens: 2000
      embedding:
        options:
          model: text-embedding-3-large
          dimensions: 3072
    
    vectorstore:
      chroma:
        client:
          host: chroma-prod
          port: 8000
        collection-name: ticket_prod
        initialize-schema: false  # 生产环境不自动初始化

# ==================== 缓存配置 ====================
spring:
  data:
    redis:
      host: redis-prod
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 1
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 10
  
  cache:
    type: redis
    redis:
      time-to-live: 7200000  # 2小时
      key-prefix: "ai:prod:"

# ==================== 业务配置 ====================
ticket:
  ai:
    customer-service:
      enabled: true
      transfer-threshold: 0.7
      max-conversation-rounds: 15
    recommendation:
      enabled: true
      count: 10
      strategy: hybrid
    review-analysis:
      enabled: true

# ==================== 监控配置 ====================
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
      environment: prod

# ==================== 日志配置 ====================
logging:
  level:
    root: INFO
    io.nebula: INFO
    com.ticketsystem: INFO
  file:
    name: /var/log/ticket-ai/app.log
    max-size: 100MB
    max-history: 30
```

---

## 配置最佳实践

### 实践1：敏感信息外部化

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}  # 通过环境变量提供

# 设置环境变量
export OPENAI_API_KEY=sk-...
```

### 实践2：成本控制

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          max-tokens: 1000  # 限制Token使用
          
# 使用更便宜的模型
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-3.5-turbo  # 而不是gpt-4
```

### 实践3：启用缓存

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 缓存Embedding和常见问题答案
```

### 实践4：配置超时

```yaml
spring:
  ai:
    openai:
      # 连接超时
      connect-timeout: 10000
      # 读取超时
      read-timeout: 60000
```

### 实践5：配置验证

```java
@Configuration
@ConfigurationProperties(prefix = "ticket.ai")
@Validated
public class AIConfig {
    
    @Valid
    private CustomerServiceConfig customerService;
    
    @Valid
    private RecommendationConfig recommendation;
    
    @Data
    public static class CustomerServiceConfig {
        @NotNull
        private Boolean enabled;
        
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private Double transferThreshold = 0.6;
        
        @Min(1)
        private Integer maxConversationRounds = 10;
    }
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

