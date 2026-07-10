# Nebula Crawler - 使用示例

## 组合示例

```yaml
nebula:
  crawler:
    enabled: true
    http:
      enabled: true
    browser:
      enabled: true
    proxy:
      enabled: true
```

## 说明

HTTP 抓取负责基础请求，浏览器模块负责 JS 渲染，代理与验证码模块用于反爬场景。
