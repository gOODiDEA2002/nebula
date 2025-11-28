# Nebula Starter MCP

MCP (Model Context Protocol) Server 启动器，快速构建 AI 工具服务。

## 功能特性

- **一站式依赖** - 包含构建 MCP Server 所需的所有组件
- **AI 能力集成** - 基于 `nebula-starter-ai` 的向量搜索和 RAG 能力
- **MCP 协议支持** - 完整实现 MCP Server 规范（基于 Spring AI）
- **自动配置** - Spring Boot 自动配置，开箱即用

## 设计理念

MCP (Model Context Protocol) 是一个**通信协议**，用于 AI 模型与工具/服务之间的交互。

本 Starter 的定位：
- **不重复造轮子** - 直接使用 Spring AI 的 MCP Server 实现
- **集成 Nebula AI** - 提供向量搜索、RAG、Embedding 等能力
- **便捷启动器** - 一个依赖搞定所有

## 包含的模块

| 模块 | 说明 |
|------|------|
| nebula-starter-ai | AI 能力（向量搜索、RAG、Embedding） |
| nebula-autoconfigure | 自动配置 |
| spring-ai-starter-mcp-server-webmvc | Spring AI MCP Server 实现 |
| spring-boot-starter-web | Web 服务支持 |

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-mcp</artifactId>
</dependency>
```

### 2. 配置应用

```yaml
nebula:
  # AI 配置
  ai:
    enabled: true
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      embedding:
        model: bge-m3
        enabled: true
    vector-store:
      chroma:
        host: localhost
        port: 9002
        collection-name: my_docs

# Spring AI MCP 配置
spring:
  ai:
    mcp:
      server:
        enabled: true
```

### 3. 创建 MCP 工具

使用 Spring AI 的 `@McpTool` 注解：

```java
@Service
@RequiredArgsConstructor
public class MyMcpTools {
    
    private final VectorStoreService vectorStoreService;
    
    @McpTool(description = "Search documentation for answers")
    public String searchDocs(SearchRequest request) {
        var results = vectorStoreService.search(
                SearchRequest.builder()
                        .query(request.getQuery())
                        .topK(5)
                        .build()
        );
        
        StringBuilder sb = new StringBuilder();
        sb.append("# Search Results\n\n");
        for (var doc : results.getDocuments()) {
            sb.append("## Score: ").append(doc.getScore()).append("\n");
            sb.append(doc.getContent()).append("\n\n");
        }
        return sb.toString();
    }
    
    @Data
    public static class SearchRequest {
        @JsonProperty(required = true)
        private String query;
    }
}
```

### 4. 启动应用

```java
@SpringBootApplication
public class MyMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyMcpServerApplication.class, args);
    }
}
```

## 与 Claude Desktop / Cursor 集成

### Claude Desktop 配置

```json
{
  "mcpServers": {
    "my-assistant": {
      "url": "http://localhost:3001/mcp"
    }
  }
}
```

### Cursor 配置

```json
{
  "mcpServers": {
    "my-assistant": {
      "url": "http://localhost:3001/mcp"
    }
  }
}
```

## 利用 Nebula AI 能力

### 向量搜索

```java
@Autowired
private VectorStoreService vectorStoreService;

// 搜索
SearchResult result = vectorStoreService.search(
    SearchRequest.builder()
        .query("如何使用分布式锁？")
        .topK(5)
        .build()
);

// 添加文档
vectorStoreService.add(List.of(
    Document.builder()
        .content("文档内容...")
        .metadata(Map.of("type", "doc"))
        .build()
));
```

### 文档处理（RAG）

```java
@Autowired
private DocumentChunker chunker;

@Autowired
private MarkdownDocumentParser parser;

// 解析 Markdown
ParsedDocument doc = parser.parse(markdownContent);

// 分块
List<DocumentChunk> chunks = chunker.chunk(doc);

// 索引到向量库
vectorStoreService.add(chunks.stream()
    .map(chunk -> Document.builder()
        .content(chunk.getContent())
        .metadata(chunk.getMetadata())
        .build())
    .toList()
);
```

## 架构图

```mermaid
flowchart TB
    subgraph Client
        Claude[Claude Desktop]
        Cursor[Cursor IDE]
    end
    
    subgraph MCP Server
        Tools[@McpTool 工具]
    end
    
    subgraph Nebula AI
        Vector[VectorStoreService]
        RAG[DocumentChunker]
        Embed[EmbeddingService]
    end
    
    subgraph Infrastructure
        Chroma[(Chroma)]
        Ollama[Ollama]
    end
    
    Claude -->|MCP| Tools
    Cursor -->|MCP| Tools
    Tools --> Vector
    Tools --> RAG
    Vector --> Chroma
    RAG --> Embed
    Embed --> Ollama
```

## 相关文档

- [MCP 协议规范](https://modelcontextprotocol.io/)
- [Spring AI MCP Server](https://docs.spring.io/spring-ai/reference/api/mcp-server.html)
- [Nebula AI Spring](../../infrastructure/ai/nebula-ai-spring/README.md)
- [Nebula Starter AI](../nebula-starter-ai/README.md)

## 版本要求

- Java 21+
- Spring Boot 3.2+
- Spring AI 1.1.0+

## 示例项目

参考 [nebula-dev-assistant](../../../nebula-dev-assistant) 项目，这是一个基于本 Starter 构建的完整 MCP Server 示例。
