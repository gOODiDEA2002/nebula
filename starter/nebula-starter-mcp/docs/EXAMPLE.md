# Nebula Starter MCP - 使用示例

本文档展示 MCP Server 的基础用法。

## 示例 1：定义 MCP 工具

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
        return results.getDocuments()
            .stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));
    }
}
```

## 示例 2：票务场景（模块示例）

将票务知识库接入 MCP 工具，提供“场次/座位/订单”相关问答能力：

```java
@McpTool(description = "Ticketing knowledge assistant")
public String ticketingQA(TicketingQuery query) {
    return vectorStoreService.search(
        SearchRequest.builder()
            .query(query.getQuestion())
            .topK(3)
            .build()
    ).getDocuments().stream()
     .map(Document::getContent)
     .collect(Collectors.joining("\n"));
}
```
