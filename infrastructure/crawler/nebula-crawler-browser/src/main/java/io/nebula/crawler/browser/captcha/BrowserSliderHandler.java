package io.nebula.crawler.browser.captcha;

import io.nebula.crawler.browser.captcha.CaptchaDetectionResult.CaptchaVendor;
import io.nebula.crawler.browser.captcha.CaptchaDetectionResult.HandleStrategy;
import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.exception.CaptchaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BoundingBox;

import java.util.Random;

/**
 * 浏览器滑块验证码处理器
 * 封装滑块验证码的完整处理流程：检测、识别、拖动
 * 支持验证码类型自动检测，并根据类型采取不同处理策略
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class BrowserSliderHandler {

    private final CaptchaManager captchaManager;
    private final BrowserCaptchaDetector captchaDetector;
    private final Random random = new Random();

    /**
     * 仅使用 CaptchaManager 创建处理器
     */
    public BrowserSliderHandler(CaptchaManager captchaManager) {
        this.captchaManager = captchaManager;
        this.captchaDetector = new BrowserCaptchaDetector();
    }

    /**
     * 默认的验证码 iframe 选择器
     */
    private static final String[] DEFAULT_IFRAME_SELECTORS = {
            "iframe[src*='captcha']",
            "iframe[src*='slider']",
            "iframe",
    };

    /**
     * 默认的背景图选择器
     */
    private static final String[] DEFAULT_BACKGROUND_SELECTORS = {
            "img.bg-img",
            "img[data-type='bg']",
            "img",
    };

    /**
     * 默认的滑块按钮选择器
     */
    private static final String[] DEFAULT_SLIDER_BTN_SELECTORS = {
            "img[cursor=pointer]",
            ".slider-btn",
            "[class*='slider']",
            "img:last-child",
    };

    /**
     * 处理滑块验证码
     * 自动识别验证码元素、检测偏移量、执行拖动
     *
     * @param page 浏览器页面
     * @return 处理结果
     */
    public SliderHandleResult handle(Page page) {
        return handle(page, SliderHandleOptions.defaults());
    }

    /**
     * 处理滑块验证码（带选项）
     * 处理流程:
     * 1. 检测验证码类型
     * 2. 根据类型判断是否可自动处理
     * 3. 提取图片并识别偏移量
     * 4. 执行拖动操作
     *
     * @param page    浏览器页面
     * @param options 处理选项
     * @return 处理结果
     */
    public SliderHandleResult handle(Page page, SliderHandleOptions options) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 检测验证码类型
            CaptchaDetectionResult detection = captchaDetector.detect(page);
            log.info("验证码检测结果: {}", BrowserCaptchaDetector.getHandleSuggestion(detection));

            // 2. 判断是否可自动处理
            if (detection.isDetected() && !detection.isCanAutoHandle()) {
                // 商业验证码，无法自动处理
                return SliderHandleResult.builder()
                        .success(false)
                        .detectionResult(detection)
                        .error("检测到" + detection.getVendor().getDescription() + "，无法自动处理")
                        .suggestedAction(detection.getSuggestedStrategy())
                        .build();
            }

            // 3. 查找验证码容器（可能在 iframe 中）
            FrameLocator captchaFrame = findCaptchaFrame(page, options);

            // 4. 提取背景图和滑块图的 Base64
            ImagePair images = extractImages(captchaFrame, page, options);
            if (images == null) {
                log.warn("无法提取验证码图片");
                return SliderHandleResult.failed("无法提取验证码图片", detection);
            }

            // 5. 使用 CaptchaManager 检测偏移量
            Integer offset = detectOffset(images.backgroundBase64, images.sliderBase64);
            if (offset == null) {
                log.warn("无法检测滑块偏移量，使用降级策略");
                offset = options.getFallbackOffset();
            }

            // 6. 查找滑块按钮
            Locator sliderBtn = findSliderButton(captchaFrame, page, options);
            if (sliderBtn == null || sliderBtn.count() == 0) {
                log.warn("无法找到滑块按钮");
                return SliderHandleResult.failed("无法找到滑块按钮", detection);
            }

            // 7. 执行拖动
            BoundingBox box = sliderBtn.boundingBox();
            if (box == null) {
                return SliderHandleResult.failed("无法获取滑块位置", detection);
            }

            performDrag(page, box, offset, options);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("滑块验证码处理完成: offset={}, costTime={}ms", offset, costTime);

            return SliderHandleResult.builder()
                    .success(true)
                    .offset(offset)
                    .costTime(costTime)
                    .detectionResult(detection)
                    .build();

        } catch (Exception e) {
            log.error("滑块验证码处理失败: {}", e.getMessage());
            return SliderHandleResult.failed(e.getMessage(), null);
        }
    }

    /**
     * 查找验证码 iframe
     */
    private FrameLocator findCaptchaFrame(Page page, SliderHandleOptions options) {
        String[] selectors = options.getIframeSelectors() != null ?
                options.getIframeSelectors() : DEFAULT_IFRAME_SELECTORS;

        for (String selector : selectors) {
            try {
                if (page.locator(selector).count() > 0) {
                    return page.frameLocator(selector);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * 提取验证码图片
     */
    private ImagePair extractImages(FrameLocator frame, Page page, SliderHandleOptions options) {
        try {
            Locator imgLocator = frame != null ?
                    frame.locator("img") :
                    page.locator(options.getContainerSelector() + " img");

            int imgCount = imgLocator.count();
            if (imgCount < 2) {
                return null;
            }

            // 通常第一张是背景图，第二张是滑块图
            String backgroundSrc = imgLocator.first().getAttribute("src");
            String sliderSrc = imgLocator.nth(1).getAttribute("src");

            String bgBase64 = extractBase64FromSrc(backgroundSrc);
            String sliderBase64 = extractBase64FromSrc(sliderSrc);

            if (bgBase64 != null && sliderBase64 != null) {
                return new ImagePair(bgBase64, sliderBase64);
            }

            return null;
        } catch (Exception e) {
            log.debug("提取图片失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 src 属性提取 Base64
     */
    private String extractBase64FromSrc(String src) {
        if (src == null) {
            return null;
        }
        if (src.startsWith("data:image")) {
            String[] parts = src.split(",");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        // TODO: 支持从 URL 下载图片
        return null;
    }

    /**
     * 使用 CaptchaManager 检测偏移量
     */
    private Integer detectOffset(String backgroundBase64, String sliderBase64) {
        if (captchaManager == null || !captchaManager.isAvailable(CaptchaType.SLIDER)) {
            return null;
        }

        try {
            CaptchaRequest request = CaptchaRequest.builder()
                    .type(CaptchaType.SLIDER)
                    .backgroundImage(backgroundBase64)
                    .sliderImage(sliderBase64)
                    .timeout(30000)
                    .build();

            CaptchaResult result = captchaManager.solve(request);
            if (result.isSuccess() && result.getSliderOffset() != null) {
                return result.getSliderOffset();
            }
        } catch (CaptchaException e) {
            log.warn("验证码识别失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 查找滑块按钮
     */
    private Locator findSliderButton(FrameLocator frame, Page page, SliderHandleOptions options) {
        String[] selectors = options.getSliderBtnSelectors() != null ?
                options.getSliderBtnSelectors() : DEFAULT_SLIDER_BTN_SELECTORS;

        for (String selector : selectors) {
            try {
                Locator btn = frame != null ?
                        frame.locator(selector) :
                        page.locator(selector);

                if (btn.count() > 0) {
                    return btn.first();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * 执行滑块拖动（模拟人类轨迹）
     */
    private void performDrag(Page page, BoundingBox box, int distance, SliderHandleOptions options) {
        double startX = box.x + box.width / 2;
        double startY = box.y + box.height / 2;

        // 移动到滑块位置
        page.mouse().move(startX, startY);
        page.waitForTimeout(50 + random.nextInt(50));

        // 按下鼠标
        page.mouse().down();

        // 分段移动，模拟人类轨迹
        int steps = options.getDragSteps();
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / steps;
            // 使用 easeOutQuad 缓动函数：先快后慢
            double eased = 1 - (1 - progress) * (1 - progress);
            double currentX = startX + distance * eased;
            // 添加随机 Y 偏移模拟人类抖动
            double offsetY = (random.nextDouble() - 0.5) * 2;
            page.mouse().move(currentX, startY + offsetY);
            page.waitForTimeout(5 + random.nextInt(10));
        }

        // 松开鼠标
        page.mouse().up();

        log.debug("滑块拖动完成: distance={}", distance);
    }

    /**
     * 图片对
     */
    private record ImagePair(String backgroundBase64, String sliderBase64) {
    }

    /**
     * 滑块处理结果
     */
    @lombok.Data
    @lombok.Builder
    public static class SliderHandleResult {
        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 检测到的偏移量
         */
        private Integer offset;

        /**
         * 处理耗时（毫秒）
         */
        private long costTime;

        /**
         * 错误信息
         */
        private String error;

        /**
         * 验证码检测结果
         */
        private CaptchaDetectionResult detectionResult;

        /**
         * 建议的处理策略
         */
        private HandleStrategy suggestedAction;

        public static SliderHandleResult success(int offset, long costTime) {
            return SliderHandleResult.builder()
                    .success(true)
                    .offset(offset)
                    .costTime(costTime)
                    .build();
        }

        public static SliderHandleResult failed(String error, CaptchaDetectionResult detection) {
            return SliderHandleResult.builder()
                    .success(false)
                    .error(error)
                    .detectionResult(detection)
                    .build();
        }

        /**
         * 是否为不可自动处理的商业验证码
         */
        public boolean isCommercialCaptcha() {
            return detectionResult != null &&
                    detectionResult.isDetected() &&
                    !detectionResult.isCanAutoHandle();
        }

        /**
         * 获取验证码类型描述
         */
        public String getCaptchaVendorDescription() {
            if (detectionResult != null && detectionResult.getVendor() != null) {
                return detectionResult.getVendor().getDescription();
            }
            return "未知";
        }
    }

    /**
     * 滑块处理选项
     */
    @lombok.Data
    @lombok.Builder
    public static class SliderHandleOptions {
        /**
         * iframe 选择器列表
         */
        private String[] iframeSelectors;

        /**
         * 验证码容器选择器
         */
        @lombok.Builder.Default
        private String containerSelector = "body";

        /**
         * 滑块按钮选择器列表
         */
        private String[] sliderBtnSelectors;

        /**
         * 降级偏移量（无法检测时使用）
         */
        @lombok.Builder.Default
        private int fallbackOffset = 280;

        /**
         * 拖动步数
         */
        @lombok.Builder.Default
        private int dragSteps = 30;

        public static SliderHandleOptions defaults() {
            return SliderHandleOptions.builder().build();
        }
    }
}
