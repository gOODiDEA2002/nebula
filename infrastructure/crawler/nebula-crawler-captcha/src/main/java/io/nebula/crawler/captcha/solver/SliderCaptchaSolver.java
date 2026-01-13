package io.nebula.crawler.captcha.solver;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.config.CaptchaProperties;
import io.nebula.crawler.captcha.cv.OpenCvService;
import io.nebula.crawler.captcha.exception.CaptchaException;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 滑块验证码解决器
 * 支持本地OpenCV服务检测和第三方平台识别
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class SliderCaptchaSolver implements CaptchaSolver {

    private final CaptchaProperties properties;
    private final List<CaptchaServiceProvider> providers;
    private final OpenCvService openCvService;

    /**
     * 构造函数
     *
     * @param properties    配置属性
     * @param providers     第三方平台列表
     * @param openCvService OpenCV服务（可选）
     */
    public SliderCaptchaSolver(CaptchaProperties properties,
                               List<CaptchaServiceProvider> providers,
                               OpenCvService openCvService) {
        this.properties = properties;
        this.providers = providers;
        this.openCvService = openCvService;
    }

    @Override
    public String getName() {
        return "SliderCaptchaSolver";
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.SLIDER;
    }

    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();

        try {
            // 尝试本地OpenCV服务检测
            if (properties.isLocalSliderEnabled() && openCvService != null) {
                Integer offset = detectGapOffset(request.getBackgroundImage(), request.getSliderImage());
                if (offset != null) {
                    return CaptchaResult.builder()
                            .success(true)
                            .type(CaptchaType.SLIDER)
                            .sliderOffset(offset)
                            .costTime(System.currentTimeMillis() - startTime)
                            .solverName("LocalOpenCV")
                            .confidence(0.9)
                            .build();
                }
            }

            // 尝试第三方平台
            for (CaptchaServiceProvider provider : providers) {
                if (!provider.supportsType(CaptchaType.SLIDER)) {
                    continue;
                }
                if (!provider.isAvailable()) {
                    continue;
                }

                try {
                    CaptchaResult result = provider.solveSlider(
                            request.getBackgroundImage(),
                            request.getSliderImage(),
                            request.getTimeout()
                    );
                    if (result.isSuccess()) {
                        result.setCostTime(System.currentTimeMillis() - startTime);
                        result.setSolverName(provider.getName());
                        result.setType(CaptchaType.SLIDER);
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("打码平台 {} 识别滑块失败: {}", provider.getName(), e.getMessage());
                }
            }

            throw new CaptchaException("滑块验证码识别失败");
        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("滑块验证码识别异常", e);
        }
    }

    /**
     * 使用OpenCV服务检测缺口偏移量
     *
     * @param backgroundBase64 背景图Base64
     * @param sliderBase64     滑块图Base64
     * @return 偏移量，检测失败返回null
     */
    private Integer detectGapOffset(String backgroundBase64, String sliderBase64) {
        if (openCvService == null) {
            log.debug("OpenCV服务未配置");
            return null;
        }

        if (backgroundBase64 == null || sliderBase64 == null) {
            log.debug("缺少背景图或滑块图");
            return null;
        }

        if (!openCvService.isAvailable()) {
            log.debug("OpenCV服务不可用");
            return null;
        }

        OpenCvService.SliderDetectResult result = openCvService.detectSliderGap(backgroundBase64, sliderBase64);
        if (result != null) {
            log.info("OpenCV检测滑块缺口成功: offset={}, confidence={}",
                    result.offset(), result.confidence());
            return result.offset();
        }

        return null;
    }

    @Override
    public boolean isAvailable() {
        // 本地OpenCV服务可用 或 有可用的第三方平台
        boolean localAvailable = properties.isLocalSliderEnabled() &&
                openCvService != null &&
                openCvService.isAvailable();

        boolean providerAvailable = providers.stream()
                .anyMatch(p -> p.supportsType(CaptchaType.SLIDER) && p.isAvailable());

        return localAvailable || providerAvailable;
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
