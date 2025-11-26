# Nebula Search Elasticsearch 配置指南

> Elasticsearch全文搜索配置说明

## 概述

`nebula-search-elasticsearch` 提供 Elasticsearch 全文搜索支持。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-search-elasticsearch</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### Elasticsearch配置

```yaml
nebula:
  search:
    elasticsearch:
      enabled: true
      uris: http://elasticsearch:9200
      username: ${ES_USERNAME}
      password: ${ES_PASSWORD}
      connection-timeout: 10s
      socket-timeout: 60s
```

## 票务系统场景

### 电影搜索

```yaml
nebula:
  search:
    elasticsearch:
      enabled: true
      uris: http://es-node1:9200,http://es-node2:9200
      # 索引配置
      indexes:
        movies:
          number-of-shards: 3
          number-of-replicas: 2
```

### 使用示例

```java
@Document(indexName = "movies")
@Data
public class MovieDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String director;
    
    @Field(type = FieldType.Date)
    private LocalDate releaseDate;
}

@Repository
public interface MovieSearchRepository extends ElasticsearchRepository<MovieDocument, String> {
    List<MovieDocument> findByTitleContaining(String keyword);
}
```

---

**最后更新**: 2025-11-20

