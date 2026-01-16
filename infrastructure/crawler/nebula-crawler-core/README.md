# Nebula Crawler Core

> Nebula框架爬虫核心抽象层，定义统一的爬虫接口和基础类

## 模块概述

`nebula-crawler-core` 是Nebula爬虫模块组的核心抽象层，提供：

- 统一的爬虫引擎接口（`CrawlerEngine`）
- 请求/响应封装（`CrawlerRequest`/`CrawlerResponse`）
- 代理管理抽象（`ProxyProvider`/`Proxy`）
- 限流器（`RateLimiter`）
- 基础异常类

## 核心接口

### CrawlerEngine

爬虫引擎统一接口：

```java
public interface CrawlerEngine {
    CrawlerEngineType getType();
    CrawlerResponse crawl(CrawlerRequest request);
    CompletableFuture<CrawlerResponse> crawlAsync(CrawlerRequest request);
    List<CrawlerResponse> crawlBatch(List<CrawlerRequest> requests);
    void shutdown();
    boolean isHealthy();
}
```

### CrawlerRequest

请求封装，支持多种构建方式：

```java
// 简单GET请求
CrawlerRequest request = CrawlerRequest.get("https://example.com");

// 带参数的GET请求
CrawlerRequest request = CrawlerRequest.get("https://example.com", Map.of("key", "value"));

// POST请求
CrawlerRequest request = CrawlerRequest.post("https://example.com", "{\"data\":\"value\"}");

// 需要JS渲染的请求
CrawlerRequest request = CrawlerRequest.renderPage("https://example.com");

// 完整Builder方式
CrawlerRequest request = CrawlerRequest.builder()
    .url("https://example.com")
    .method(HttpMethod.GET)
    .headers(Map.of("Authorization", "Bearer token"))
    .connectTimeout(30000)
    .readTimeout(60000)
    .retryCount(3)
    .build();
```

### CrawlerResponse

响应封装，提供便捷的解析方法：

```java
CrawlerResponse response = engine.crawl(request);

if (response.isSuccess()) {
    // 解析为Jsoup Document
    Document doc = response.asDocument();
    
    // 解析为JSON对象
    MyData data = response.asJson(MyData.class);
    
    // 解析为Map
    Map<String, Object> map = response.asMap();
    
    // 获取原始内容
    String content = response.getContent();
}
```

### ProxyProvider

代理提供者接口：

```java
public interface ProxyProvider {
    Proxy getProxy();
    List<Proxy> getProxies(int count);
    void reportSuccess(Proxy proxy);
    void reportFailure(Proxy proxy, String reason);
    int getAvailableCount();
    void refresh();
}
```

## 依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-crawler-core</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

## 相关模块

- [nebula-crawler-http](../nebula-crawler-http) - HTTP爬虫实现
- [nebula-crawler-browser](../nebula-crawler-browser) - 浏览器爬虫实现
- [nebula-crawler-proxy](../nebula-crawler-proxy) - 代理池管理

## 许可证

MIT License

