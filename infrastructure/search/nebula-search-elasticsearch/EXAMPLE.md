# nebula-search-elasticsearch 模块示例

## 模块简介

`nebula-search-elasticsearch` 是 Nebula 框架基于 Elasticsearch (7.x/8.x) 的搜索实现模块。它实现了 `nebula-search-core` 的标准接口，提供了高性能的全文检索、结构化查询和聚合分析能力。

## 核心功能示例

### 1. 配置 Elasticsearch

在 `application.yml` 中配置 ES 连接信息。

**`application.yml`**:

```yaml
nebula:
  search:
    elasticsearch:
      enabled: true
      uris: 
        - http://localhost:9200
      username: ${ES_USERNAME:elastic}
      password: ${ES_PASSWORD:changeme}
      
      # 连接池配置
      connection-timeout: 5s
      read-timeout: 30s
      max-connections: 100
      max-connections-per-route: 20
      
      # 索引配置
      index-prefix: "nebula-"  # 索引前缀，防止命名冲突
      default-shards: 1
      default-replicas: 0
```

### 2. 启动应用

引入模块后，`SearchService` 会自动注入 Elasticsearch 实现。

**`io.nebula.example.es.SearchApplication`**:

```java
package io.nebula.example.es;

import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.SearchResult;
import io.nebula.search.core.query.SearchQuery;
import io.nebula.search.core.query.builder.MatchQueryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class SearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }

    @Bean
    public CommandLineRunner testSearch(SearchService searchService) {
        return args -> {
            String indexName = "logs";
            
            // 1. 索引文档
            LogDoc doc = new LogDoc("1", "Error occurred in service", "ERROR");
            searchService.indexDocument(indexName, doc.getId(), doc);
            
            // 等待刷新 (ES 是近实时的)
            Thread.sleep(1000);
            
            // 2. 搜索文档
            SearchQuery query = SearchQuery.builder()
                    .indexName(indexName)
                    .query(new MatchQueryBuilder("message", "Error"))
                    .build();
                    
            SearchResult<LogDoc> result = searchService.search(query, LogDoc.class);
            log.info("搜索到 {} 条日志", result.getTotal());
            result.getHits().forEach(hit -> log.info("日志内容: {}", hit.getMessage()));
        };
    }

    @Data
    public static class LogDoc {
        private String id;
        private String message;
        private String level;

        public LogDoc(String id, String message, String level) {
            this.id = id;
            this.message = message;
            this.level = level;
        }
    }
}
```

## 进阶特性

### 1. 索引前缀隔离

`nebula-search-elasticsearch` 支持配置 `index-prefix`。
例如配置为 `nebula-`，当你调用 `createIndex("users")` 时，实际在 ES 中创建的索引名为 `nebula-users`。这在多应用共享 ES 集群时非常有用。

### 2. SSL/TLS 支持

支持配置 SSL 证书路径，用于连接开启了安全认证的 ES 集群（如 Elastic Cloud 或自建 HTTPS 集群）。

```yaml
nebula:
  search:
    elasticsearch:
      ssl-enabled: true
      ssl-ca-path: /path/to/ca.crt
```

### 3. 原生客户端访问

如果需要使用 `nebula-search-core` 未覆盖的 ES 高级特性（如 Percolate Query, ML 等），可以通过 Spring 容器获取底层的 `RestHighLevelClient` (或新版 `ElasticsearchClient`) Bean。

## 总结

`nebula-search-elasticsearch` 将复杂的 ES 查询 DSL 封装为类型安全的 Java API，极大地降低了搜索引擎的开发门槛，同时保留了 ES 强大的搜索和聚合能力。

