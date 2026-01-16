# Nebula Crawler Browser - 测试指南

## 测试前提

- Playwright Server 可用（REMOTE 模式）
- 本地浏览器可启动（LOCAL 模式）

## 建议测试

1. 页面渲染与 JS 执行是否正常。
2. 连接超时与重试行为。
3. 异常场景截图是否生成。

## 集成测试建议

使用固定的测试页面，验证 DOM 渲染与元素提取逻辑。
