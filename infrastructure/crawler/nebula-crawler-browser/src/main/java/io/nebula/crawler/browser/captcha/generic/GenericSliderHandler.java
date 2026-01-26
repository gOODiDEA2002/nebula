package io.nebula.crawler.browser.captcha.generic;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import io.nebula.crawler.browser.captcha.BrowserCaptchaHandler;
import io.nebula.crawler.captcha.CaptchaManager;
import io.nebula.crawler.captcha.CaptchaRequest;
import io.nebula.crawler.captcha.CaptchaResult;
import io.nebula.crawler.captcha.CaptchaType;
import io.nebula.crawler.captcha.CaptchaVendor;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * 通用滑块验证码处理器
 * 
 * 适用于大多数常见的滑块验证码，特点：
 * 1. 验证码图片可以直接截图获取
 * 2. 滑块可以简单拖动
 * 3. 不需要特殊的人类轨迹模拟
 *
 * @author Nebula Team
 * @since 2.0.2
 */
@Slf4j
public class GenericSliderHandler implements BrowserCaptchaHandler {

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 5;

    /**
     * 常见滑块验证码选择器
     */
    private static final String[] CAPTCHA_SELECTORS = {
            ".slide-verify",
            ".slider-captcha",
            ".verify-wrap",
            ".captcha-container",
            "[class*='slider'][class*='verify']"
    };

    /**
     * 常见背景图选择器
     */
    private static final String[] BACKGROUND_SELECTORS = {
            ".captcha-bg img",
            ".slide-verify-image img",
            ".verify-bg img",
            "canvas.captcha-canvas",
            "[class*='captcha'][class*='bg'] img"
    };

    /**
     * 常见滑块按钮选择器
     */
    private static final String[] SLIDER_BUTTON_SELECTORS = {
            ".slider-btn",
            ".slide-verify-slider",
            ".verify-move-block",
            "[class*='slider'][class*='btn']",
            "[class*='drag'][class*='btn']"
    };

    private final CaptchaManager captchaManager;
    private String customCaptchaSelector;
    private String customBackgroundSelector;
    private String customSliderButtonSelector;

    /**
     * 构造函数
     *
     * @param captchaManager 验证码管理器
     */
    public GenericSliderHandler(CaptchaManager captchaManager) {
        this.captchaManager = captchaManager;
    }

    /**
     * 设置自定义选择器
     */
    public GenericSliderHandler withSelectors(String captchaSelector,
                                               String backgroundSelector,
                                               String sliderButtonSelector) {
        this.customCaptchaSelector = captchaSelector;
        this.customBackgroundSelector = backgroundSelector;
        this.customSliderButtonSelector = sliderButtonSelector;
        return this;
    }

    @Override
    public CaptchaVendor getVendor() {
        return CaptchaVendor.GENERIC_SLIDER;
    }

    @Override
    public boolean detect(Page page) {
        // 自定义选择器优先
        if (customCaptchaSelector != null) {
            try {
                Locator element = page.locator(customCaptchaSelector);
                if (element.count() > 0 && element.first().isVisible()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }

        // 尝试常见选择器
        for (String selector : CAPTCHA_SELECTORS) {
            try {
                Locator element = page.locator(selector);
                if (element.count() > 0 && element.first().isVisible()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }

        return false;
    }

    @Override
    public boolean handle(Page page) {
        return handle(page, DEFAULT_MAX_RETRIES);
    }

    @Override
    public boolean handle(Page page, int maxRetries) {
        log.info("开始处理通用滑块验证码...");

        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                // 检查验证码是否存在
                if (!detect(page)) {
                    log.info("验证码已消失，处理成功");
                    return true;
                }

                // 1. 提取背景图
                String backgroundBase64 = extractBackgroundImage(page);
                if (backgroundBase64 == null) {
                    log.warn("无法提取背景图 (重试 {}/{})", retry + 1, maxRetries);
                    Thread.sleep(500);
                    continue;
                }

                // 2. 使用 CaptchaManager 识别缺口
                CaptchaRequest request = CaptchaRequest.builder()
                        .type(CaptchaType.SLIDER)
                        .backgroundImage(backgroundBase64)
                        .timeout(30000)
                        .build();

                CaptchaResult result = captchaManager.solve(request);
                if (!result.isSuccess() || result.getSliderOffset() == null) {
                    log.warn("缺口识别失败: {} (重试 {}/{})",
                            result.getErrorMessage(), retry + 1, maxRetries);
                    Thread.sleep(500);
                    continue;
                }

                int offset = result.getSliderOffset();
                log.info("缺口识别成功: offset={}, confidence={}",
                        offset, result.getConfidence());

                // 3. 执行滑动
                boolean slideSuccess = performSlide(page, offset);
                if (!slideSuccess) {
                    log.warn("滑动执行失败 (重试 {}/{})", retry + 1, maxRetries);
                    Thread.sleep(500);
                    continue;
                }

                // 4. 等待验证结果
                Thread.sleep(1000);

                // 5. 检查验证码是否消失
                if (!detect(page)) {
                    log.info("通用滑块验证码处理成功");
                    return true;
                }

                log.info("验证码仍然存在 (重试 {}/{})", retry + 1, maxRetries);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                log.error("处理验证码异常: {}", e.getMessage());
            }
        }

        log.error("通用滑块验证码处理失败，已达最大重试次数");
        return false;
    }

    @Override
    public boolean isAvailable() {
        return captchaManager != null && captchaManager.isAvailable(CaptchaType.SLIDER);
    }

    @Override
    public int getPriority() {
        return 100; // 较低优先级，作为通用备选方案
    }

    /**
     * 提取背景图 Base64
     */
    private String extractBackgroundImage(Page page) {
        // 自定义选择器优先
        if (customBackgroundSelector != null) {
            String base64 = tryExtractImage(page, customBackgroundSelector);
            if (base64 != null) return base64;
        }

        // 尝试常见选择器
        for (String selector : BACKGROUND_SELECTORS) {
            String base64 = tryExtractImage(page, selector);
            if (base64 != null) return base64;
        }

        return null;
    }

    /**
     * 尝试提取指定元素的图片
     */
    private String tryExtractImage(Page page, String selector) {
        try {
            Locator element = page.locator(selector);
            if (element.count() == 0) return null;

            Locator first = element.first();
            if (!first.isVisible()) return null;

            // 尝试截图
            byte[] screenshot = first.screenshot();
            if (screenshot != null && screenshot.length > 0) {
                return Base64.getEncoder().encodeToString(screenshot);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 执行滑动操作
     */
    private boolean performSlide(Page page, int offset) {
        try {
            BoundingBox sliderBox = findSliderButton(page);
            if (sliderBox == null) {
                log.warn("找不到滑块按钮");
                return false;
            }

            double startX = sliderBox.x + sliderBox.width / 2;
            double startY = sliderBox.y + sliderBox.height / 2;
            double endX = startX + offset;

            // 鼠标移动到滑块
            page.mouse().move(startX, startY);
            Thread.sleep(100);

            // 按下鼠标
            page.mouse().down();
            Thread.sleep(50);

            // 分步滑动（模拟人类行为）
            int steps = 20;
            for (int i = 1; i <= steps; i++) {
                double progress = (double) i / steps;
                double currentX = startX + offset * progress;
                // 添加少量随机偏移
                double currentY = startY + (Math.random() * 4 - 2);
                page.mouse().move(currentX, currentY);
                Thread.sleep(20 + (long)(Math.random() * 20));
            }

            // 释放鼠标
            page.mouse().up();

            return true;

        } catch (Exception e) {
            log.error("执行滑动失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 查找滑块按钮
     */
    private BoundingBox findSliderButton(Page page) {
        // 自定义选择器优先
        if (customSliderButtonSelector != null) {
            BoundingBox box = tryFindElement(page, customSliderButtonSelector);
            if (box != null) return box;
        }

        // 尝试常见选择器
        for (String selector : SLIDER_BUTTON_SELECTORS) {
            BoundingBox box = tryFindElement(page, selector);
            if (box != null) return box;
        }

        return null;
    }

    /**
     * 尝试查找元素并获取边界框
     */
    private BoundingBox tryFindElement(Page page, String selector) {
        try {
            Locator element = page.locator(selector);
            if (element.count() > 0 && element.first().isVisible()) {
                return element.first().boundingBox();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
