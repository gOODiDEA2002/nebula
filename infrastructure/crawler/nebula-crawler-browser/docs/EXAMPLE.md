# Nebula Crawler Browser - 使用示例

## 示例：页面渲染抓取

```java
@Service
@RequiredArgsConstructor
public class BrowserCrawlerService {

    private final BrowserCrawlerEngine crawlerEngine;

    public String fetchRendered(String url) {
        CrawlerRequest request = CrawlerRequest.renderPage(url);
        CrawlerResponse response = crawlerEngine.crawl(request);
        return response.getContent();
    }
}
```

## 示例：自定义视口与超时

```java
CrawlerRequest request = CrawlerRequest.builder()
    .url("https://example.com")
    .renderPage(true)
    .connectTimeout(30000)
    .readTimeout(60000)
    .build();
CrawlerResponse response = crawlerEngine.crawl(request);
```
