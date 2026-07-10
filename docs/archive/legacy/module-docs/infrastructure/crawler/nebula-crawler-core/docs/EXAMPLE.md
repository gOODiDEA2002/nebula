# Nebula Crawler Core - 使用示例

核心模块定义统一的请求与响应模型，具体执行由 HTTP/Browser 引擎实现。

## 示例：构建请求并执行

```java
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final CrawlerEngine crawlerEngine;

    public String fetchHtml(String url) {
        CrawlerRequest request = CrawlerRequest.get(url);
        CrawlerResponse response = crawlerEngine.crawl(request);
        return response.getContent();
    }
}
```

## 示例：解析响应

```java
CrawlerResponse response = crawlerEngine.crawl(request);
if (response.isSuccess()) {
    Document doc = response.asDocument();
    // 解析标题
    String title = doc.title();
}
```
