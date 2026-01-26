package io.nebula.crawler.browser.captcha;

import io.nebula.crawler.browser.captcha.generic.GenericSliderHandler;
import io.nebula.crawler.browser.captcha.tencent.TencentCaptchaHandler;
import io.nebula.crawler.captcha.CaptchaManager;
import io.nebula.crawler.captcha.cv.OpenCvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 浏览器验证码自动配置
 * 
 * 自动注册各类验证码处理器到 BrowserCaptchaManager
 *
 * @author Nebula Team
 * @since 2.0.2
 */
@Slf4j
@Configuration
public class BrowserCaptchaAutoConfiguration {

    @Autowired(required = false)
    private CaptchaManager captchaManager;

    @Autowired(required = false)
    private OpenCvService openCvService;

    /**
     * 创建腾讯验证码处理器
     */
    @Bean
    @ConditionalOnMissingBean(TencentCaptchaHandler.class)
    public TencentCaptchaHandler tencentCaptchaHandler() {
        if (captchaManager != null) {
            log.info("创建腾讯验证码处理器（使用 CaptchaManager）");
            return new TencentCaptchaHandler(captchaManager, openCvService);
        } else if (openCvService != null) {
            log.info("创建腾讯验证码处理器（使用 OpenCvService）");
            return new TencentCaptchaHandler(openCvService);
        } else {
            String opencvUrl = System.getenv().getOrDefault("OPENCV_URL", "http://localhost:8867");
            log.info("创建腾讯验证码处理器（使用地址: {}）", opencvUrl);
            return new TencentCaptchaHandler(opencvUrl);
        }
    }

    /**
     * 创建通用滑块处理器
     */
    @Bean
    @ConditionalOnBean(CaptchaManager.class)
    @ConditionalOnMissingBean(GenericSliderHandler.class)
    public GenericSliderHandler genericSliderHandler() {
        log.info("创建通用滑块验证码处理器");
        return new GenericSliderHandler(captchaManager);
    }

    /**
     * 创建浏览器验证码管理器
     */
    @Bean
    @ConditionalOnMissingBean(BrowserCaptchaManager.class)
    public BrowserCaptchaManager browserCaptchaManager(List<BrowserCaptchaHandler> handlers) {
        log.info("创建浏览器验证码管理器，注册 {} 个处理器", handlers.size());
        for (BrowserCaptchaHandler handler : handlers) {
            log.info("  - {} (优先级: {}, 可用: {})",
                    handler.getVendor().getDescription(),
                    handler.getPriority(),
                    handler.isAvailable());
        }
        return new BrowserCaptchaManager(handlers);
    }
}
