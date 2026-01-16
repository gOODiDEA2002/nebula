# Nebula Crawler HTTP

> 基于OkHttp的高性能HTTP爬虫实现

## 模块概述

`nebula-crawler-http` 提供基于OkHttp 4.x的HTTP爬虫引擎，特性包括：

- 高性能HTTP客户端
- 连接池复用
- 自动重试机制
- User-Agent轮换
- 代理支持
- 限流控制

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-crawler-http</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 配置

```yaml
nebula:
  crawler:
    enabled: true
    http:
      enabled: true
      connect-timeout: 30000
      read-timeout: 60000
      max-connections: 200
      retry-count: 3
      use-proxy: true
      default-qps: 5.0
      user-agents:
        - "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."
```

### 3. 使用

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final HttpCrawlerEngine httpCrawlerEngine;
    
    public void crawl() {
        // 简单爬取
        CrawlerRequest request = CrawlerRequest.get("https://example.com");
        CrawlerResponse response = httpCrawlerEngine.crawl(request);
        
        if (response.isSuccess()) {
            String content = response.getContent();
            // 处理内容...
        }
    }
    
    public void crawlAsync() {
        // 异步爬取
        CompletableFuture<CrawlerResponse> future = httpCrawlerEngine
            .crawlAsync(CrawlerRequest.get("https://example.com"));
        
        future.thenAccept(response -> {
            // 处理响应...
        });
    }
}
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `true` | 是否启用 |
| `connect-timeout` | `30000` | 连接超时(ms) |
| `read-timeout` | `60000` | 读取超时(ms) |
| `write-timeout` | `60000` | 写入超时(ms) |
| `max-connections` | `200` | 最大连接数 |
| `max-connections-per-host` | `20` | 每主机最大连接数 |
| `retry-count` | `3` | 重试次数 |
| `retry-interval` | `1000` | 重试间隔(ms) |
| `use-proxy` | `false` | 是否使用代理 |
| `default-qps` | `5.0` | 默认QPS限制 |
| `user-agents` | `[]` | User-Agent池 |

## 拦截器

内置拦截器：

- **UserAgentInterceptor**：自动轮换User-Agent
- **RetryInterceptor**：失败自动重试
- **ProxyInterceptor**：代理切换

## 依赖

- OkHttp 4.12.0
- Jsoup 1.17.2

## 许可证

MIT License

