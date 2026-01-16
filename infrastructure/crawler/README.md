# Nebula Crawler

Nebula 爬虫模块组入口，包含核心抽象与多种实现。

## 模块构成

- `nebula-crawler-core`：统一接口与请求/响应模型
- `nebula-crawler-http`：HTTP 抓取实现
- `nebula-crawler-browser`：浏览器渲染抓取
- `nebula-crawler-proxy`：代理池与健康检测
- `nebula-crawler-captcha`：验证码检测与识别

## 使用建议

1. 先启用 `nebula-crawler-core` 与 `nebula-crawler-http` 完成基础抓取。
2. 需要 JS 渲染页面时启用 `nebula-crawler-browser`。
3. 复杂反爬场景引入 `proxy` 与 `captcha` 模块。

## 许可证

MIT License
