# Nebula Crawler Proxy - 使用示例

## 示例：获取代理并用于请求

```java
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final ProxyPool proxyPool;

    public Proxy getProxy() {
        return proxyPool.getProxy();
    }
}
```

## 示例：与 HTTP 爬虫结合

启用代理后，HTTP 爬虫会自动使用代理池：

```yaml
nebula:
  crawler:
    http:
      use-proxy: true
    proxy:
      enabled: true
```
