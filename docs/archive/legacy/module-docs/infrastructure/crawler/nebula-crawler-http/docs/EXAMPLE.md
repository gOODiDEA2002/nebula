# Nebula Crawler HTTP - 使用示例

## 示例：基础抓取

```java
@Service
@RequiredArgsConstructor
public class HttpCrawlerService {

    private final HttpCrawlerEngine crawlerEngine;

    public String fetch(String url) {
        CrawlerRequest request = CrawlerRequest.get(url);
        return crawlerEngine.crawl(request).getContent();
    }
}
```

## 示例：自定义请求头与重试

```java
CrawlerRequest request = CrawlerRequest.builder()
    .url("https://example.com")
    .headers(Map.of("Accept", "application/json"))
    .retryCount(2)
    .build();
CrawlerResponse response = crawlerEngine.crawl(request);
```
