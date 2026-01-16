# Nebula Crawler Captcha - 使用示例

## 示例：验证码识别

```java
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final CaptchaManager captchaManager;

    public CaptchaResult solve(String base64) {
        CaptchaRequest request = new CaptchaRequest();
        request.setImageBase64(base64);
        request.setType(CaptchaType.TEXT);
        return captchaManager.solve(request);
    }
}
```

## 示例：异步识别

```java
CaptchaRequest request = new CaptchaRequest();
request.setImageBase64(base64);
request.setType(CaptchaType.SLIDER);

captchaManager.solveAsync(request)
    .thenAccept(result -> {
        // 处理结果
    });
```
