# Nebula AI 功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula AI 模块的各种功能，包括智能聊天文本嵌入文档向量存储和智能问答（RAG）等

## 启动应用

```bash
cd nebula-example
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## 前置准备

### 1. 配置 API Key

在 `application.yml` 或环境变量中配置 OpenAI API Key：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

或者设置环境变量：

```bash
export OPENAI_API_KEY=your-api-key-here
```

### 2. 启动向量存储服务（可选）

如果要使用文档向量存储功能，需要启动 Chroma 服务：

```bash
docker run -d -p 8000:8000 chromadb/chroma
```

或者使用已有的 docker-compose：

```bash
cd nebula-middleware
docker-compose up -d chroma
```

### 3. 添加 AI 模块依赖

确保 `nebula-example/pom.xml` 中包含以下依赖：

```xml
<!-- Nebula AI Spring 模块 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-ai-spring</artifactId>
</dependency>

<!-- Spring AI OpenAI Starter -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Spring AI Chroma Vector Store -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-chroma-store-spring-boot-starter</artifactId>
</dependency>
```

## API 接口测试

### 1. 智能聊天功能

#### 1.1 基础聊天

```bash
curl -X POST http://localhost:8000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请用一句话介绍一下Java语言"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "聊天成功",
  "data": {
    "id": "chatcmpl-xxxxx",
    "content": "Java是一种面向对象的跨平台的编程语言，以其"一次编写，到处运行"的特性而闻名",
    "model": "gpt-3.5-turbo",
    "usage": {
      "promptTokens": 25,
      "completionTokens": 42,
      "totalTokens": 67
    },
    "finishReason": "stop",
    "timestamp": "2024-01-15T10:30:00"
  },
  "success": true
}
```

#### 1.2 带参数的聊天

```bash
curl -X POST http://localhost:8000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "写一个冒泡排序算法的Java实现",
    "model": "gpt-4",
    "temperature": 0.7,
    "maxTokens": 500
  }'
```

#### 1.3 创意写作测试

```bash
curl -X POST http://localhost:8000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "写一首关于春天的五言绝句",
    "temperature": 1.0
  }'
```

### 2. 文本嵌入功能

#### 2.1 单文本向量化

```bash
curl -X POST http://localhost:8000/ai/embed \
  -H "Content-Type: application/json" \
  -d '{
    "texts": ["人工智能正在改变世界"]
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "文本嵌入成功",
  "data": {
    "id": "emb-xxxxx",
    "embeddings": [
      {
        "index": 0,
        "vector": [0.123, -0.456, 0.789, ...],
        "text": "人工智能正在改变世界"
      }
    ],
    "model": "text-embedding-ada-002",
    "dimension": 1536,
    "usage": {
      "promptTokens": 12,
      "totalTokens": 12
    },
    "timestamp": "2024-01-15T10:35:00"
  },
  "success": true
}
```

#### 2.2 批量文本向量化

```bash
curl -X POST http://localhost:8000/ai/embed \
  -H "Content-Type: application/json" \
  -d '{
    "texts": [
      "人工智能是计算机科学的一个分支",
      "机器学习是实现人工智能的重要方法",
      "深度学习是机器学习的一个子领域"
    ]
  }'
```

#### 2.3 指定嵌入模型

```bash
curl -X POST http://localhost:8000/ai/embed \
  -H "Content-Type: application/json" \
  -d '{
    "texts": ["测试文本"],
    "model": "text-embedding-3-large"
  }'
```

### 3. 文本相似度计算

#### 3.1 计算两个文本的相似度

```bash
curl -X POST http://localhost:8000/ai/similarity \
  -H "Content-Type: application/json" \
  -d '{
    "text1": "我喜欢吃苹果",
    "text2": "我爱吃水果"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "相似度计算成功",
  "data": {
    "similarity": 0.8523,
    "text1": "我喜欢吃苹果",
    "text2": "我爱吃水果",
    "timestamp": "2024-01-15T10:40:00"
  },
  "success": true
}
```

#### 3.2 相似文本对比

```bash
curl -X POST http://localhost:8000/ai/similarity \
  -H "Content-Type: application/json" \
  -d '{
    "text1": "Java是一种编程语言",
    "text2": "Python也是一种编程语言"
  }'
```

#### 3.3 不相似文本对比

```bash
curl -X POST http://localhost:8000/ai/similarity \
  -H "Content-Type: application/json" \
  -d '{
    "text1": "我喜欢编程",
    "text2": "今天天气真好"
  }'
```

### 4. 文档向量存储

#### 4.1 添加单个文档

```bash
curl -X POST http://localhost:8000/ai/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Java是由Sun Microsystems在1995年推出的一种编程语言，以其跨平台特性而著称",
    "metadata": {
      "source": "Java入门教程",
      "category": "编程语言",
      "author": "技术团队"
    }
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "文档添加成功",
  "data": {
    "documentId": "doc-xxxxx-xxxx-xxxx",
    "success": true,
    "timestamp": "2024-01-15T10:45:00"
  },
  "success": true
}
```

#### 4.2 批量添加知识库文档

添加关于Java的文档：

```bash
curl -X POST http://localhost:8000/ai/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Java的核心特性包括面向对象平台无关性健壮性安全性和多线程支持",
    "metadata": {"category": "Java核心特性"}
  }'

curl -X POST http://localhost:8000/ai/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Java虚拟机（JVM）是Java实现跨平台的关键，它可以在不同操作系统上运行相同的Java字节码",
    "metadata": {"category": "JVM"}
  }'

curl -X POST http://localhost:8000/ai/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring Framework是Java生态系统中最流行的应用开发框架，提供了依赖注入AOP等核心功能",
    "metadata": {"category": "Spring框架"}
  }'

curl -X POST http://localhost:8000/ai/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Java集合框架包括ListSetMap等接口，以及ArrayListHashSetHashMap等常用实现类",
    "metadata": {"category": "Java集合"}
  }'
```

### 5. 文档语义搜索

#### 5.1 基础搜索

```bash
curl -X POST http://localhost:8000/ai/documents/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是JVM",
    "topK": 3
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "文档搜索成功",
  "data": {
    "documents": [
      {
        "id": "doc-xxxxx-1",
        "content": "Java虚拟机（JVM）是Java实现跨平台的关键，它可以在不同操作系统上运行相同的Java字节码",
        "score": 0.9245,
        "metadata": {"category": "JVM"}
      },
      {
        "id": "doc-xxxxx-2",
        "content": "Java是由Sun Microsystems在1995年推出的一种编程语言，以其跨平台特性而著称",
        "score": 0.7823,
        "metadata": {"category": "编程语言"}
      }
    ],
    "query": "什么是JVM",
    "totalFound": 2,
    "maxScore": 0.9245,
    "minScore": 0.7823,
    "timestamp": "2024-01-15T10:50:00"
  },
  "success": true
}
```

#### 5.2 带相似度阈值的搜索

```bash
curl -X POST http://localhost:8000/ai/documents/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Java的主要特点",
    "topK": 5,
    "similarityThreshold": 0.7
  }'
```

#### 5.3 带过滤条件的搜索

```bash
curl -X POST http://localhost:8000/ai/documents/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "框架",
    "topK": 3,
    "filter": {
      "category": "Spring框架"
    }
  }'
```

### 6. 文档智能问答（RAG）

#### 6.1 基础问答

首先添加一些知识库文档，然后进行问答：

```bash
curl -X POST http://localhost:8000/ai/qa \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Java虚拟机的作用是什么？",
    "contextSize": 3
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "问答成功",
  "data": {
    "answer": "Java虚拟机（JVM）的主要作用是实现Java的跨平台特性它能够在不同的操作系统上运行相同的Java字节码，使得Java程序可以\"一次编写，到处运行\"JVM负责将Java字节码解释或编译成特定平台的机器码来执行",
    "question": "Java虚拟机的作用是什么？",
    "contextDocuments": [
      {
        "id": "doc-xxxxx-1",
        "content": "Java虚拟机（JVM）是Java实现跨平台的关键...",
        "score": 0.9245
      },
      {
        "id": "doc-xxxxx-2",
        "content": "Java是由Sun Microsystems在1995年推出...",
        "score": 0.8123
      }
    ],
    "model": "gpt-3.5-turbo",
    "usage": {
      "promptTokens": 156,
      "completionTokens": 89,
      "totalTokens": 245
    },
    "timestamp": "2024-01-15T10:55:00"
  },
  "success": true
}
```

#### 6.2 带参数的问答

```bash
curl -X POST http://localhost:8000/ai/qa \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Spring框架有哪些核心功能？",
    "contextSize": 2,
    "similarityThreshold": 0.7,
    "temperature": 0.3
  }'
```

#### 6.3 复杂问题问答

```bash
curl -X POST http://localhost:8000/ai/qa \
  -H "Content-Type: application/json" \
  -d '{
    "question": "比较一下Java集合框架中List和Set的区别",
    "contextSize": 5
  }'
```

#### 6.4 多轮对话场景

先建立知识库：

```bash
# 添加关于Nebula框架的文档
curl -X POST http://localhost:8000/ai/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Nebula是一个现代化的企业级Java后端框架，基于Spring Boot 3.x和Java 21构建",
    "metadata": {"topic": "Nebula简介"}
  }'

curl -X POST http://localhost:8000/ai/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Nebula框架提供了数据访问消息传递服务发现对象存储等完整的基础设施支持",
    "metadata": {"topic": "Nebula功能"}
  }'
```

然后进行问答：

```bash
# 第一个问题
curl -X POST http://localhost:8000/ai/qa \
  -H "Content-Type: application/json" \
  -d '{
    "question": "什么是Nebula框架？"
  }'

# 第二个问题
curl -X POST http://localhost:8000/ai/qa \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Nebula框架提供了哪些功能？"
  }'
```

## 功能验证清单

###  智能聊天功能
- [x] 基础对话 - AI正确理解和回复用户消息
- [x] 参数控制 - temperaturemaxTokens等参数生效
- [x] 多种模型 - 支持切换不同的AI模型
- [x] Token统计 - 正确统计和返回token使用量

###  文本嵌入功能
- [x] 单文本向量化 - 成功将文本转换为向量
- [x] 批量向量化 - 支持批量处理多个文本
- [x] 向量维度 - 正确返回向量维度信息
- [x] 相似度计算 - 准确计算文本相似度

###  文档向量存储
- [x] 添加文档 - 成功添加文档到向量存储
- [x] 元数据支持 - 正确存储和检索元数据
- [x] 语义搜索 - 根据语义而非关键词搜索
- [x] 相似度阈值 - 过滤低相似度结果

###  智能问答（RAG）
- [x] 基于文档回答 - AI基于检索的文档回答问题
- [x] 上下文整合 - 正确整合多个相关文档
- [x] 相关度排序 - 返回最相关的文档
- [x] 准确性验证 - 答案与文档内容一致

## 性能测试

### 1. 聊天响应时间测试

```bash
time curl -X POST http://localhost:8000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "解释一下什么是微服务架构"
  }'
```

预期响应时间：1-5秒（取决于网络和AI服务响应）

### 2. 嵌入性能测试

```bash
# 测试批量嵌入性能
time curl -X POST http://localhost:8000/ai/embed \
  -H "Content-Type: application/json" \
  -d '{
    "texts": [
      "文本1...",
      "文本2...",
      "文本3...",
      "文本4...",
      "文本5..."
    ]
  }'
```

### 3. 文档搜索性能测试

```bash
# 添加100个文档后测试搜索性能
for i in {1..100}; do
  curl -X POST http://localhost:8000/ai/documents \
    -H "Content-Type: application/json" \
    -d "{
      \"content\": \"测试文档 $i 的内容...\",
      \"metadata\": {\"index\": $i}
    }" > /dev/null 2>&1
done

# 测试搜索性能
time curl -X POST http://localhost:8000/ai/documents/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "测试",
    "topK": 10
  }'
```

## 故障排查

### 1. API Key 相关

**问题**: 401 Unauthorized 或 API key 错误

**解决方案**:
```bash
# 检查环境变量
echo $OPENAI_API_KEY

# 或检查配置文件
grep -A 2 "openai" src/main/resources/application.yml
```

### 2. 向量存储连接失败

**问题**: Connection refused to localhost:8000

**解决方案**:
```bash
# 检查Chroma服务状态
docker ps | grep chroma

# 启动Chroma服务
docker-compose -f nebula-middleware/docker-compose.yml up -d chroma

# 检查端口占用
lsof -i :8000
```

### 3. 内存不足

**问题**: OutOfMemoryError

**解决方案**:
```bash
# 增加JVM内存
export MAVEN_OPTS="-Xmx2g -Xms1g"
mvn spring-boot:run
```

### 4. 请求超时

**问题**: Read timed out

**解决方案**:
```yaml
# 在application.yml中增加超时配置
spring:
  ai:
    openai:
      chat:
        options:
          timeout: 120  # 增加超时时间到120秒
```

## 开启调试日志

```yaml
logging:
  level:
    io.nebula.ai: DEBUG
    io.nebula.example.modules.ai: DEBUG
    org.springframework.ai: DEBUG
```

## 最佳实践

### 1. 合理使用模型

- **gpt-3.5-turbo**: 适合日常对话简单问答，速度快成本低
- **gpt-4**: 适合复杂推理代码生成专业领域问答
- **text-embedding-ada-002**: 通用嵌入模型，性价比高
- **text-embedding-3-large**: 更高质量的嵌入，适合对精度要求高的场景

### 2. 优化 Token 使用

```bash
# 设置合理的maxTokens避免浪费
curl -X POST http://localhost:8000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "简要说明...",
    "maxTokens": 200  # 限制输出长度
  }'
```

### 3. 文档分片策略

对于长文档，建议分片存储：

```python
# 示例：将长文档分片
long_document = "很长的文档内容..."
chunk_size = 500  # 每个分片500字符

for i, chunk in enumerate(split_into_chunks(long_document, chunk_size)):
    curl -X POST http://localhost:8000/ai/documents \
      -d '{
        "content": "' + chunk + '",
        "metadata": {"doc_id": "long-doc-1", "chunk_index": ' + i + '}
      }'
```

### 4. 缓存常用结果

对于频繁查询的问题，建议实现缓存机制

---

更多信息，请参考 [Nebula AI Spring 模块文档](../nebula/infrastructure/ai/nebula-ai-spring/README.md)

