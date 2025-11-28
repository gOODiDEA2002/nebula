# Nebula MCP Core

MCP (Model Context Protocol) Server 核心模块，提供构建 AI 工具服务的基础设施。

## 功能特性

- **MCP Server 支持** - 基于 Spring AI 的 MCP Server 实现
- **文档搜索** - 语义搜索服务接口
- **文档索引** - 文档索引器接口
- **可扩展设计** - 易于扩展和自定义

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-mcp-core</artifactId>
</dependency>
```

### 2. 配置

```yaml
nebula:
  mcp:
    enabled: true
    name: "my-mcp-server"
    version: "1.0.0"
    tool-packages:
      - "com.example.tools"
    search:
      cache-enabled: true
      cache-ttl-minutes: 60
      default-top-k: 5
    indexing:
      auto-index: false
      docs-path: "./docs"
      chunk-strategy: HYBRID
      max-chunk-size: 1000
```

### 3. 创建 MCP 工具

使用 `@McpTool` 注解创建工具：

```java
@Service
public class MyMcpTools {
    
    @McpTool(description = "Search documentation")
    public String searchDocs(SearchRequest request) {
        // 实现搜索逻辑
        return "搜索结果...";
    }
    
    @Data
    public static class SearchRequest {
        private String query;
        private String category;
    }
}
```

## 核心接口

### DocumentSearchService

文档搜索服务接口，用于实现语义搜索：

```java
public interface DocumentSearchService {
    List<SearchResult> search(String query, int topK);
    List<SearchResult> search(String query, int topK, Map<String, String> filters);
}
```

### DocumentIndexer

文档索引器接口，用于管理文档索引：

```java
public interface DocumentIndexer {
    IndexResult indexDocument(Path filePath);
    List<IndexResult> indexDirectory(Path directory);
    boolean deleteDocument(String documentId);
    void clearAll();
    IndexStats getStats();
}
```

## 与向量数据库集成

默认实现仅提供基础框架，实际项目中应该接入向量数据库：

### Chroma 集成示例

```java
@Service
public class ChromaDocumentSearchService implements DocumentSearchService {
    
    private final ChromaClient chromaClient;
    
    @Override
    public List<SearchResult> search(String query, int topK) {
        // 使用 Chroma 进行向量搜索
        var results = chromaClient.query(query, topK);
        return results.stream()
                .map(r -> new SearchResult(r.getContent(), r.getScore(), r.getMetadata()))
                .toList();
    }
}
```

## 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `nebula.mcp.enabled` | 是否启用 MCP Server | `true` |
| `nebula.mcp.name` | MCP Server 名称 | `nebula-mcp-server` |
| `nebula.mcp.tool-packages` | 工具扫描包路径 | `[]` |
| `nebula.mcp.search.cache-enabled` | 是否启用搜索缓存 | `true` |
| `nebula.mcp.search.default-top-k` | 默认返回结果数量 | `5` |
| `nebula.mcp.indexing.auto-index` | 是否启动时自动索引 | `false` |
| `nebula.mcp.indexing.chunk-strategy` | 分块策略 | `HYBRID` |

## 架构图

```mermaid
flowchart TB
    subgraph MCP Server
        Tools[@McpTool 工具]
        Search[DocumentSearchService]
        Index[DocumentIndexer]
    end
    
    subgraph AI Client
        Claude[Claude/GPT]
    end
    
    subgraph Storage
        Vector[向量数据库]
        Docs[文档源]
    end
    
    Claude -->|MCP Protocol| Tools
    Tools --> Search
    Search --> Vector
    Index --> Docs
    Index --> Vector
```

## 相关模块

- [nebula-starter-mcp](../../starter/nebula-starter-mcp) - MCP 启动器
- [nebula-ai-spring](../ai/nebula-ai-spring) - AI 集成模块

## 版本要求

- Java 21+
- Spring Boot 3.2+
- Spring AI 1.1.0+

