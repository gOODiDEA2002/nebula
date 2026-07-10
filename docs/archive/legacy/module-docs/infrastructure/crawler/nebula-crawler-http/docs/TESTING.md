# Nebula Crawler HTTP - 测试指南

## 测试前提

- 本地可访问的测试站点或 Mock Server

## 建议测试

1. 正常 HTML 抓取与解析。
2. 超时与重试策略是否生效。
3. 代理开关与 QPS 限制是否符合预期。

## 集成测试建议

使用 WireMock 或 MockServer 模拟站点，验证请求头、重试与重定向逻辑。
