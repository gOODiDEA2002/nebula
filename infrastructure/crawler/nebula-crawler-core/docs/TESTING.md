# Nebula Crawler Core - 测试指南

核心模块测试重点是请求/响应模型与引擎接口的契约。

## 建议测试

1. `CrawlerRequest` 构建器参数覆盖测试。
2. `CrawlerResponse` 解析方法（JSON/HTML）是否兼容。
3. 针对实现模块的引擎能力做集成测试。
