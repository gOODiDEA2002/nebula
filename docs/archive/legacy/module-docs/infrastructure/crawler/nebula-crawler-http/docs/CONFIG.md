# Nebula Crawler HTTP - 配置参考

## 配置前缀

- `nebula.crawler.http`

## 配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.crawler.http.enabled` | boolean | true | 是否启用 HTTP 爬虫 |
| `nebula.crawler.http.connect-timeout` | int | 30000 | 连接超时（ms） |
| `nebula.crawler.http.read-timeout` | int | 60000 | 读取超时（ms） |
| `nebula.crawler.http.write-timeout` | int | 60000 | 写入超时（ms） |
| `nebula.crawler.http.max-connections` | int | 200 | 最大连接数 |
| `nebula.crawler.http.max-connections-per-host` | int | 20 | 每主机最大连接数 |
| `nebula.crawler.http.keep-alive-time` | long | 300000 | 连接保活时间（ms） |
| `nebula.crawler.http.retry-count` | int | 3 | 重试次数 |
| `nebula.crawler.http.retry-interval` | int | 1000 | 重试间隔（ms） |
| `nebula.crawler.http.use-proxy` | boolean | false | 是否使用代理 |
| `nebula.crawler.http.default-qps` | double | 5.0 | 默认 QPS |
| `nebula.crawler.http.follow-redirects` | boolean | true | 是否跟随重定向 |
| `nebula.crawler.http.trust-all-certs` | boolean | false | 是否信任所有证书 |
| `nebula.crawler.http.user-agents` | list | - | User-Agent 池 |

## 配置示例

```yaml
nebula:
  crawler:
    http:
      enabled: true
      connect-timeout: 30000
      read-timeout: 60000
      retry-count: 3
      default-qps: 5.0
      follow-redirects: true
      user-agents:
        - "Mozilla/5.0 ..."
```
