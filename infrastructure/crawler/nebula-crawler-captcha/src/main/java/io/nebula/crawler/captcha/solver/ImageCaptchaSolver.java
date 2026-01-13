package io.nebula.crawler.captcha.solver;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.config.CaptchaProperties;
import io.nebula.crawler.captcha.exception.CaptchaException;
import io.nebula.crawler.captcha.ocr.OcrEngine;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;

/**
 * 图形验证码解决器
 * 支持本地OCR和第三方打码平台
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class ImageCaptchaSolver implements CaptchaSolver {

    private final CaptchaProperties properties;
    private final OcrEngine ocrEngine;
    private final List<CaptchaServiceProvider> providers;

    public ImageCaptchaSolver(CaptchaProperties properties,
                              OcrEngine ocrEngine,
                              List<CaptchaServiceProvider> providers) {
        this.properties = properties;
        this.ocrEngine = ocrEngine;
        this.providers = providers != null ? providers : List.of();
    }

    @Override
    public String getName() {
        return "ImageCaptchaSolver";
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.IMAGE;
    }

    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();

        try {
            byte[] imageData = getImageData(request);

            // 1. 优先使用本地OCR
            if (properties.isLocalOcrEnabled() && ocrEngine != null) {
                try {
                    log.debug("使用本地OCR引擎识别验证码");
                    String text = ocrEngine.recognize(imageData);
                    if (isValidResult(text)) {
                        return CaptchaResult.builder()
                                .success(true)
                                .type(CaptchaType.IMAGE)
                                .text(text)
                                .costTime(System.currentTimeMillis() - startTime)
                                .confidence(0.8)
                                .solverName("LocalOCR-" + ocrEngine.getName())
                                .build();
                    }
                    log.debug("本地OCR识别结果无效: {}", text);
                } catch (Exception e) {
                    log.warn("本地OCR识别失败: {}", e.getMessage());
                }
            }

            // 2. 使用第三方打码平台
            for (CaptchaServiceProvider provider : providers) {
                if (!provider.isAvailable()) {
                    log.debug("打码平台 {} 不可用，跳过", provider.getName());
                    continue;
                }

                try {
                    log.debug("使用打码平台 {} 识别验证码", provider.getName());
                    CaptchaResult result = provider.solveImage(imageData, request.getTimeout());
                    if (result.isSuccess() && isValidResult(result.getText())) {
                        result.setCostTime(System.currentTimeMillis() - startTime);
                        result.setSolverName("Provider-" + provider.getName());
                        return result;
                    }
                    log.debug("打码平台 {} 识别失败: {}", provider.getName(),
                            result.isSuccess() ? "结果无效" : result.getErrorMessage());
                } catch (Exception e) {
                    log.warn("打码平台 {} 识别异常: {}", provider.getName(), e.getMessage());
                }
            }

            throw new CaptchaException("所有识别方式均失败");

        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("验证码识别异常: " + e.getMessage(), e);
        }
    }

    /**
     * 获取图片数据
     */
    private byte[] getImageData(CaptchaRequest request) throws CaptchaException {
        if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
            String base64 = request.getImageBase64();
            // 移除Base64前缀
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }
            return Base64.getDecoder().decode(base64);
        }
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            // 下载图片（需要HTTP客户端支持）
            throw new CaptchaException("暂不支持通过URL获取验证码图片");
        }
        throw new CaptchaException("未提供验证码图片");
    }

    /**
     * 验证结果是否有效
     */
    private boolean isValidResult(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        int length = text.trim().length();
        return length >= properties.getMinLength() && length <= properties.getMaxLength();
    }

    @Override
    public boolean isAvailable() {
        // 只要有任一识别方式可用即可
        if (properties.isLocalOcrEnabled() && ocrEngine != null && ocrEngine.isAvailable()) {
            return true;
        }
        return providers.stream().anyMatch(CaptchaServiceProvider::isAvailable);
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
