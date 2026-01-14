package io.nebula.crawler.browser.captcha;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.nebula.crawler.browser.captcha.CaptchaDetectionResult.CaptchaVendor;
import io.nebula.crawler.browser.captcha.CaptchaDetectionResult.HandleStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * 浏览器验证码检测器
 * 自动检测页面中的验证码类型，并返回处理建议
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class BrowserCaptchaDetector {

    /**
     * 检测页面中的验证码类型
     *
     * @param page 浏览器页面
     * @return 检测结果
     */
    public CaptchaDetectionResult detect(Page page) {
        try {
            String pageContent = page.content().toLowerCase();
            String pageUrl = page.url().toLowerCase();

            // 1. 检测腾讯验证码 (TCaptcha)
            if (isTencentCaptcha(page, pageContent)) {
                log.info("检测到腾讯验证码 (TCaptcha)");
                return CaptchaDetectionResult.builder()
                        .vendor(CaptchaVendor.TENCENT_TCAPTCHA)
                        .detected(true)
                        .canAutoHandle(false)
                        .suggestedStrategy(HandleStrategy.THIRD_PARTY_PLATFORM)
                        .reason("腾讯验证码使用 canvas 渲染和轨迹检测，本地算法难以处理")
                        .extraInfo("建议使用第三方打码平台或手动登录保存 Cookie")
                        .build();
            }

            // 2. 检测极验验证码 (GeeTest)
            if (isGeeTestCaptcha(page, pageContent)) {
                log.info("检测到极验验证码 (GeeTest)");
                return CaptchaDetectionResult.builder()
                        .vendor(CaptchaVendor.GEETEST)
                        .detected(true)
                        .canAutoHandle(false)
                        .suggestedStrategy(HandleStrategy.THIRD_PARTY_PLATFORM)
                        .reason("极验验证码有复杂的行为检测机制")
                        .build();
            }

            // 3. 检测网易易盾
            if (isNeteaseCaptcha(page, pageContent)) {
                log.info("检测到网易易盾验证码");
                return CaptchaDetectionResult.builder()
                        .vendor(CaptchaVendor.NETEASE_YIDUN)
                        .detected(true)
                        .canAutoHandle(false)
                        .suggestedStrategy(HandleStrategy.THIRD_PARTY_PLATFORM)
                        .reason("网易易盾验证码有复杂的行为检测机制")
                        .build();
            }

            // 4. 检测 Google reCAPTCHA
            if (isRecaptcha(page, pageContent)) {
                log.info("检测到 Google reCAPTCHA");
                return CaptchaDetectionResult.builder()
                        .vendor(CaptchaVendor.RECAPTCHA)
                        .detected(true)
                        .canAutoHandle(false)
                        .suggestedStrategy(HandleStrategy.THIRD_PARTY_PLATFORM)
                        .reason("reCAPTCHA 需要专业的打码服务")
                        .build();
            }

            // 5. 检测阿里云验证码
            if (isAliyunCaptcha(page, pageContent)) {
                log.info("检测到阿里云验证码");
                return CaptchaDetectionResult.builder()
                        .vendor(CaptchaVendor.ALIYUN)
                        .detected(true)
                        .canAutoHandle(false)
                        .suggestedStrategy(HandleStrategy.THIRD_PARTY_PLATFORM)
                        .reason("阿里云验证码有复杂的行为检测机制")
                        .build();
            }

            // 6. 检测简单滑块验证码
            if (isSimpleSliderCaptcha(page)) {
                log.info("检测到简单滑块验证码");
                return CaptchaDetectionResult.autoHandleable(CaptchaVendor.SIMPLE_SLIDER);
            }

            // 7. 检测简单图片验证码
            if (isSimpleImageCaptcha(page)) {
                log.info("检测到简单图片验证码");
                return CaptchaDetectionResult.autoHandleable(CaptchaVendor.SIMPLE_IMAGE);
            }

            // 8. 检测未知类型的验证码（通过通用特征）
            if (hasGenericCaptchaFeatures(page, pageContent)) {
                log.info("检测到未知类型验证码");
                return CaptchaDetectionResult.builder()
                        .vendor(CaptchaVendor.UNKNOWN)
                        .detected(true)
                        .canAutoHandle(false)
                        .suggestedStrategy(HandleStrategy.RETRY)
                        .reason("未知验证码类型，建议刷新重试")
                        .build();
            }

            // 未检测到验证码
            return CaptchaDetectionResult.none();

        } catch (Exception e) {
            log.error("验证码检测异常: {}", e.getMessage());
            return CaptchaDetectionResult.none();
        }
    }

    /**
     * 检测腾讯验证码
     */
    private boolean isTencentCaptcha(Page page, String content) {
        // 特征1: 包含 TCaptcha 相关脚本
        if (content.contains("tcaptcha") || content.contains("t.captcha.qq.com")) {
            return true;
        }

        // 特征2: iframe src 包含腾讯验证码域名
        try {
            Locator iframes = page.locator("iframe");
            for (int i = 0; i < iframes.count(); i++) {
                String src = iframes.nth(i).getAttribute("src");
                if (src != null && (src.contains("captcha.qq.com") || src.contains("t.captcha"))) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        // 特征3: 特定的 class 名称
        return page.locator(".tc-fg-item, .tc-action-icon, #tcaptcha_iframe").count() > 0;
    }

    /**
     * 检测极验验证码
     */
    private boolean isGeeTestCaptcha(Page page, String content) {
        return content.contains("geetest") ||
                content.contains("gt_slider") ||
                page.locator(".geetest_slider, .geetest_btn, .geetest_holder").count() > 0;
    }

    /**
     * 检测网易易盾
     */
    private boolean isNeteaseCaptcha(Page page, String content) {
        return content.contains("yidun") ||
                content.contains("yd-captcha") ||
                page.locator(".yidun_slider, .yidun_jigsaw, .yidun_modal").count() > 0;
    }

    /**
     * 检测 Google reCAPTCHA
     */
    private boolean isRecaptcha(Page page, String content) {
        return content.contains("recaptcha") ||
                content.contains("g-recaptcha") ||
                page.locator(".g-recaptcha, .grecaptcha-badge, #recaptcha").count() > 0;
    }

    /**
     * 检测阿里云验证码
     */
    private boolean isAliyunCaptcha(Page page, String content) {
        return content.contains("alicdn") && content.contains("captcha") ||
                page.locator(".ali-captcha, .aliyun-captcha, .nc_wrapper").count() > 0;
    }

    /**
     * 检测简单滑块验证码（可自动处理）
     */
    private boolean isSimpleSliderCaptcha(Page page) {
        // 简单滑块：有 slider 相关元素，但不是商业验证码
        Locator sliders = page.locator(".slider, .slide, [class*='slider']:not(.geetest):not(.yidun)");
        if (sliders.count() > 0) {
            // 检查是否有简单的背景图+滑块图结构
            Locator imgs = page.locator("img[src*='slider'], img[src*='captcha']");
            return imgs.count() >= 2;
        }
        return false;
    }

    /**
     * 检测简单图片验证码
     */
    private boolean isSimpleImageCaptcha(Page page) {
        // 简单图片验证码：有验证码图片和输入框
        Locator captchaImg = page.locator("img[src*='captcha'], img[src*='code'], img[src*='verify']");
        Locator captchaInput = page.locator("input[name*='captcha'], input[name*='code'], input[placeholder*='验证码']");
        return captchaImg.count() > 0 && captchaInput.count() > 0;
    }

    /**
     * 检测通用验证码特征
     */
    private boolean hasGenericCaptchaFeatures(Page page, String content) {
        // 通用验证码关键词
        boolean hasKeyword = content.contains("验证码") ||
                content.contains("captcha") ||
                content.contains("verify") ||
                content.contains("安全验证");

        // 验证码相关元素
        boolean hasElement = page.locator("iframe, .captcha, .verify, [class*='captcha']").count() > 0;

        return hasKeyword && hasElement;
    }

    /**
     * 获取处理建议描述
     */
    public static String getHandleSuggestion(CaptchaDetectionResult result) {
        if (!result.isDetected()) {
            return "未检测到验证码";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("验证码类型: ").append(result.getVendor().getDescription()).append("\n");
        sb.append("可自动处理: ").append(result.isCanAutoHandle() ? "是" : "否").append("\n");
        sb.append("建议策略: ").append(result.getSuggestedStrategy().getDescription());

        if (result.getReason() != null) {
            sb.append("\n原因: ").append(result.getReason());
        }
        if (result.getExtraInfo() != null) {
            sb.append("\n备注: ").append(result.getExtraInfo());
        }

        return sb.toString();
    }
}
