# Nebula Crawler Captcha - 测试指南

## 测试前提

- OCR 服务可用（ddddocr/opencv）
- 已配置 `nebula.crawler.captcha.enabled=true`

## 建议测试

1. 文本验证码识别准确率。
2. 滑块/点击/旋转验证码识别流程。
3. 失败重试与超时行为。
