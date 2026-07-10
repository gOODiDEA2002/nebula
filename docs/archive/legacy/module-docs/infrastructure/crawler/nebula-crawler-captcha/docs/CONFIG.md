# Nebula Crawler Captcha - 配置参考

## 配置前缀

- `nebula.crawler.captcha`

## 基础配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.crawler.captcha.enabled` | boolean | true | 是否启用验证码模块 |
| `nebula.crawler.captcha.local-ocr-enabled` | boolean | true | 是否启用本地 OCR |
| `nebula.crawler.captcha.ocr-engine` | string | `ddddocr` | OCR 引擎类型 |
| `nebula.crawler.captcha.ddddocr-urls` | list | - | ddddocr 服务地址列表 |
| `nebula.crawler.captcha.opencv-urls` | list | - | OpenCV 服务地址列表 |
| `nebula.crawler.captcha.local-slider-enabled` | boolean | true | 本地滑块检测 |
| `nebula.crawler.captcha.local-rotate-enabled` | boolean | true | 本地旋转检测 |
| `nebula.crawler.captcha.local-click-enabled` | boolean | true | 本地点击检测 |
| `nebula.crawler.captcha.min-length` | int | 4 | 最小长度 |
| `nebula.crawler.captcha.max-length` | int | 6 | 最大长度 |
| `nebula.crawler.captcha.default-timeout` | int | 60000 | 默认超时（ms） |

## 配置示例

```yaml
nebula:
  crawler:
    captcha:
      enabled: true
      ocr-engine: ddddocr
      ddddocr-urls:
        - http://localhost:8866
      opencv-urls:
        - http://localhost:8867
```
