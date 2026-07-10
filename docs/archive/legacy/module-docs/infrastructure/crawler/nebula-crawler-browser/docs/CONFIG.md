# Nebula Crawler Browser - 配置参考

## 配置前缀

- `nebula.crawler.browser`

## 基础配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.crawler.browser.enabled` | boolean | true | 是否启用浏览器爬虫 |
| `nebula.crawler.browser.mode` | string | `REMOTE` | 运行模式（LOCAL/REMOTE） |
| `nebula.crawler.browser.browser-type` | string | `chromium` | 浏览器类型 |
| `nebula.crawler.browser.headless` | boolean | true | 无头模式 |
| `nebula.crawler.browser.pool-size` | int | 5 | 浏览器实例池大小 |
| `nebula.crawler.browser.page-timeout` | int | 30000 | 页面加载超时（ms） |
| `nebula.crawler.browser.navigation-timeout` | int | 30000 | 导航超时（ms） |
| `nebula.crawler.browser.connect-timeout` | int | 30000 | 连接超时（ms） |
| `nebula.crawler.browser.screenshot-on-error` | boolean | true | 错误截图 |
| `nebula.crawler.browser.use-proxy` | boolean | false | 是否使用代理 |
| `nebula.crawler.browser.viewport-width` | int | 1920 | 视口宽度 |
| `nebula.crawler.browser.viewport-height` | int | 1080 | 视口高度 |
| `nebula.crawler.browser.disable-images` | boolean | false | 是否禁用图片 |
| `nebula.crawler.browser.disable-css` | boolean | false | 是否禁用 CSS |
| `nebula.crawler.browser.user-agent` | string | - | 默认 User-Agent |
| `nebula.crawler.browser.slow-mo` | int | 0 | 慢速模式（ms） |

## 远程连接配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.crawler.browser.remote.endpoints` | list | `ws://localhost:9222` | Playwright 端点 |
| `nebula.crawler.browser.remote.load-balance-strategy` | string | `ROUND_ROBIN` | 负载均衡策略 |
| `nebula.crawler.browser.remote.health-check-interval` | int | 30000 | 健康检查间隔（ms） |
| `nebula.crawler.browser.remote.max-retries` | int | 3 | 连接重试次数 |
| `nebula.crawler.browser.remote.retry-interval` | int | 1000 | 重试间隔（ms） |

## 配置示例

```yaml
nebula:
  crawler:
    browser:
      enabled: true
      mode: REMOTE
      browser-type: chromium
      headless: true
      pool-size: 5
      remote:
        endpoints:
          - ws://localhost:9222
```
