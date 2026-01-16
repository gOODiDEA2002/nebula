# Nebula Crawler Proxy - 配置参考

## 配置前缀

- `nebula.crawler.proxy`

## 配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.crawler.proxy.enabled` | boolean | false | 是否启用代理池 |
| `nebula.crawler.proxy.min-available` | int | 10 | 最小可用代理数量 |
| `nebula.crawler.proxy.check-url` | string | `https://www.baidu.com` | 代理检测 URL |
| `nebula.crawler.proxy.check-timeout` | int | 5000 | 检测超时（ms） |
| `nebula.crawler.proxy.check-interval` | long | 300000 | 检测间隔（ms） |
| `nebula.crawler.proxy.max-fail-count` | int | 3 | 最大失败次数 |
| `nebula.crawler.proxy.blacklist-expire-hours` | int | 24 | 黑名单过期时间（小时） |
| `nebula.crawler.proxy.static-proxies` | list | - | 静态代理列表 |
| `nebula.crawler.proxy.api-sources` | list | - | API 代理源 |

## 配置示例

```yaml
nebula:
  crawler:
    proxy:
      enabled: true
      min-available: 10
      check-url: "https://www.baidu.com"
      static-proxies:
        - "http://user:pass@127.0.0.1:8080"
      api-sources:
        - name: provider-a
          url: "https://proxy.example.com/list"
          format: json
          host-field: host
          port-field: port
```
