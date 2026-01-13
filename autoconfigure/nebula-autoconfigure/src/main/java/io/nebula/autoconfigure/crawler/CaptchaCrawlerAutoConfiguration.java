package io.nebula.autoconfigure.crawler;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.config.CaptchaProperties;
import io.nebula.crawler.captcha.cv.OpenCvService;
import io.nebula.crawler.captcha.detector.CaptchaDetector;
import io.nebula.crawler.captcha.detector.DefaultCaptchaDetector;
import io.nebula.crawler.captcha.ocr.DdddOcrEngine;
import io.nebula.crawler.captcha.ocr.OcrEngine;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import io.nebula.crawler.captcha.provider.TwoCaptchaProvider;
import io.nebula.crawler.captcha.solver.ClickCaptchaSolver;
import io.nebula.crawler.captcha.solver.GestureCaptchaSolver;
import io.nebula.crawler.captcha.solver.ImageCaptchaSolver;
import io.nebula.crawler.captcha.solver.RotateCaptchaSolver;
import io.nebula.crawler.captcha.solver.SliderCaptchaSolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 验证码模块自动配置
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
@Configuration
@ConditionalOnClass(CaptchaManager.class)
@ConditionalOnProperty(prefix = "nebula.crawler.captcha", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaCrawlerAutoConfiguration {

    /**
     * 验证码类型检测器
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaDetector captchaDetector() {
        log.info("初始化默认验证码检测器");
        return new DefaultCaptchaDetector();
    }

    /**
     * OCR引擎
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nebula.crawler.captcha", name = "local-ocr-enabled", havingValue = "true", matchIfMissing = true)
    public OcrEngine ocrEngine(CaptchaProperties properties) {
        String engineType = properties.getOcrEngine();
        if ("ddddocr".equalsIgnoreCase(engineType)) {
            log.info("初始化ddddocr引擎，服务地址: {}", properties.getDdddocrUrl());
            return new DdddOcrEngine(properties.getDdddocrUrl());
        }
        // 可以扩展其他OCR引擎
        log.warn("未知的OCR引擎类型: {}，使用ddddocr作为默认", engineType);
        return new DdddOcrEngine(properties.getDdddocrUrl());
    }

    /**
     * OpenCV服务客户端
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nebula.crawler.captcha", name = "local-slider-enabled", havingValue = "true", matchIfMissing = true)
    public OpenCvService openCvService(CaptchaProperties properties) {
        log.info("初始化OpenCV服务客户端，服务地址: {}", properties.getOpencvUrl());
        return new OpenCvService(properties.getOpencvUrl());
    }

    /**
     * 第三方打码平台提供者列表
     */
    @Bean
    @ConditionalOnMissingBean
    public List<CaptchaServiceProvider> captchaServiceProviders(CaptchaProperties properties) {
        List<CaptchaServiceProvider> providers = new ArrayList<>();

        if (properties.getProviders() != null) {
            for (CaptchaProperties.ProviderConfig config : properties.getProviders()) {
                if (!config.isEnabled()) {
                    continue;
                }
                
                switch (config.getName().toLowerCase()) {
                    case "2captcha":
                        providers.add(new TwoCaptchaProvider(config.getApiKey()));
                        log.info("注册2Captcha打码平台");
                        break;
                    // 可以扩展其他打码平台
                    default:
                        log.warn("未知的打码平台: {}", config.getName());
                }
            }
        }

        return providers;
    }

    /**
     * 图形验证码解决器
     */
    @Bean
    @ConditionalOnMissingBean
    public ImageCaptchaSolver imageCaptchaSolver(
            CaptchaProperties properties,
            OcrEngine ocrEngine,
            List<CaptchaServiceProvider> providers) {
        log.info("初始化图形验证码解决器");
        return new ImageCaptchaSolver(properties, ocrEngine, providers);
    }

    /**
     * 滑块验证码解决器
     */
    @Bean
    @ConditionalOnMissingBean
    public SliderCaptchaSolver sliderCaptchaSolver(
            CaptchaProperties properties,
            List<CaptchaServiceProvider> providers,
            Optional<OpenCvService> openCvService) {
        log.info("初始化滑块验证码解决器，OpenCV服务: {}",
                openCvService.isPresent() ? "可用" : "不可用");
        return new SliderCaptchaSolver(properties, providers, openCvService.orElse(null));
    }

    /**
     * 手势验证码解决器
     */
    @Bean
    @ConditionalOnMissingBean
    public GestureCaptchaSolver gestureCaptchaSolver(List<CaptchaServiceProvider> providers) {
        log.info("初始化手势验证码解决器");
        return new GestureCaptchaSolver(providers);
    }

    /**
     * 旋转验证码解决器
     */
    @Bean
    @ConditionalOnMissingBean
    public RotateCaptchaSolver rotateCaptchaSolver(
            CaptchaProperties properties,
            List<CaptchaServiceProvider> providers,
            Optional<OpenCvService> openCvService) {
        log.info("初始化旋转验证码解决器，OpenCV服务: {}",
                openCvService.isPresent() ? "可用" : "不可用");
        return new RotateCaptchaSolver(providers, properties.isLocalRotateEnabled(),
                openCvService.orElse(null));
    }

    /**
     * 点击验证码解决器
     */
    @Bean
    @ConditionalOnMissingBean
    public ClickCaptchaSolver clickCaptchaSolver(
            CaptchaProperties properties,
            List<CaptchaServiceProvider> providers) {
        log.info("初始化点击验证码解决器");
        return new ClickCaptchaSolver(providers, properties.isLocalClickEnabled());
    }

    /**
     * 验证码管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaManager captchaManager(
            List<CaptchaSolver> solvers,
            CaptchaDetector detector) {
        log.info("初始化验证码管理器，可用解决器: {}",
                solvers.stream().map(CaptchaSolver::getName).toList());
        return new CaptchaManager(solvers, detector);
    }
}
