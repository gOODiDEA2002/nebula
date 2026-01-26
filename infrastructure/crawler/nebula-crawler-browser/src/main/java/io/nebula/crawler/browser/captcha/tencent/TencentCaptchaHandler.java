package io.nebula.crawler.browser.captcha.tencent;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BoundingBox;
import io.nebula.crawler.browser.captcha.BrowserCaptchaHandler;
import io.nebula.crawler.captcha.CaptchaManager;
import io.nebula.crawler.captcha.CaptchaRequest;
import io.nebula.crawler.captcha.CaptchaResult;
import io.nebula.crawler.captcha.CaptchaType;
import io.nebula.crawler.captcha.CaptchaVendor;
import io.nebula.crawler.captcha.cv.OpenCvService;
import io.nebula.crawler.captcha.tencent.HumanTrajectoryGenerator;
import io.nebula.crawler.captcha.tencent.TencentCaptchaResult;
import io.nebula.crawler.captcha.tencent.TencentSliderDetector;
import io.nebula.crawler.browser.util.StealthHelper;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 腾讯验证码处理器
 * 
 * 提供完整的腾讯滑块验证码处理能力：
 * 1. 检测验证码是否存在
 * 2. 提取背景图并通过 CaptchaManager 或 OpenCV 服务检测缺口位置
 * 3. 计算滑块位置
 * 4. 使用人类轨迹执行滑动
 *
 * @author Nebula Team
 * @since 2.0.2
 */
@Slf4j
public class TencentCaptchaHandler implements BrowserCaptchaHandler {

    /**
     * 腾讯验证码 iframe 选择器
     */
    private static final String CAPTCHA_IFRAME_SELECTOR = "#tcaptcha_iframe_dy, iframe[src*='captcha']";

    /**
     * 默认滑块中心位置（当无法检测时使用）
     */
    private static final int DEFAULT_SLIDER_CENTER = 40;

    /**
     * 腾讯验证码目标显示宽度
     */
    private static final int TENCENT_CAPTCHA_TARGET_WIDTH = 340;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRIES = 5;

    private final CaptchaManager captchaManager;
    private final TencentSliderDetector detector;
    private final HumanTrajectoryGenerator trajectoryGenerator;

    /**
     * 从 API 响应中解析出的滑块初始 X 位置
     * 使用 AtomicInteger 支持在回调中更新
     */
    private final AtomicInteger apiSliderInitX = new AtomicInteger(-1);

    /**
     * 精灵图信息（从 getsig 响应中解析）
     */
    private volatile SpriteInfo spriteInfo;

    /**
     * 当前会话 ID（用于关联截图和日志）
     */
    private volatile String sessionId;

    /**
     * 当前尝试次数（用于文件命名）
     */
    private final AtomicInteger currentAttempt = new AtomicInteger(0);

    /**
     * 验证码是否已成功（从网络响应中解析 errorCode=0）
     */
    private volatile boolean captchaVerified = false;

    /**
     * 精灵图信息
     */
    private record SpriteInfo(
            String spriteUrl,      // 精灵图 URL
            int sliderSpriteX,     // 滑块在精灵图中的 X 坐标
            int sliderSpriteY,     // 滑块在精灵图中的 Y 坐标
            int sliderWidth,       // 滑块宽度
            int sliderHeight       // 滑块高度
    ) {}

    /**
     * 构造函数（推荐：使用 CaptchaManager）
     * 通过 CaptchaManager 进行缺口检测，可以复用第三方打码平台作为备选
     *
     * @param captchaManager 验证码管理器
     */
    public TencentCaptchaHandler(CaptchaManager captchaManager) {
        this.captchaManager = captchaManager;
        this.detector = null;
        this.trajectoryGenerator = new HumanTrajectoryGenerator();
    }

    /**
     * 构造函数（使用 CaptchaManager + OpenCvService）
     *
     * @param captchaManager 验证码管理器
     * @param openCvService  OpenCV 服务（用于提取图片）
     */
    public TencentCaptchaHandler(CaptchaManager captchaManager, OpenCvService openCvService) {
        this.captchaManager = captchaManager;
        this.detector = openCvService != null ? new TencentSliderDetector(openCvService) : null;
        this.trajectoryGenerator = new HumanTrajectoryGenerator();
    }

    /**
     * 构造函数（直接使用 OpenCvService）
     *
     * @param openCvService OpenCV 服务
     */
    public TencentCaptchaHandler(OpenCvService openCvService) {
        this.captchaManager = null;
        this.detector = new TencentSliderDetector(openCvService);
        this.trajectoryGenerator = new HumanTrajectoryGenerator();
    }

    /**
     * 构造函数（使用 OpenCV 服务地址）
     *
     * @param openCvServerUrl OpenCV 服务地址
     */
    public TencentCaptchaHandler(String openCvServerUrl) {
        this.captchaManager = null;
        this.detector = new TencentSliderDetector(openCvServerUrl);
        this.trajectoryGenerator = new HumanTrajectoryGenerator();
    }

    @Override
    public CaptchaVendor getVendor() {
        return CaptchaVendor.TENCENT;
    }

    @Override
    public boolean detect(Page page) {
        try {
            Locator captchaIframe = page.locator(CAPTCHA_IFRAME_SELECTOR);
            return captchaIframe.count() > 0 && captchaIframe.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测页面是否存在腾讯验证码（兼容旧接口）
     */
    public boolean hasCaptcha(Page page) {
        return detect(page);
    }

    @Override
    public boolean handle(Page page) {
        return handle(page, MAX_RETRIES);
    }

    @Override
    public boolean handle(Page page, int maxRetries) {
        // 生成会话 ID（用于关联本次验证码处理的所有文件）
        sessionId = String.format("%d", System.currentTimeMillis());
        currentAttempt.set(0);
        captchaVerified = false;  // 重置验证状态
        log.info("开始处理腾讯验证码，会话ID: {}", sessionId);

        // 确保截图目录存在
        if (SCREENSHOT_ENABLED) {
            try {
                java.nio.file.Path screenshotPath = java.nio.file.Paths.get(SCREENSHOT_DIR);
                if (!java.nio.file.Files.exists(screenshotPath)) {
                    java.nio.file.Files.createDirectories(screenshotPath);
                }
            } catch (Exception e) {
                log.warn("创建截图目录失败: {}", e.getMessage());
            }
        }

        // 添加网络请求监控
        setupNetworkMonitoring(page);

        try {
            for (int retry = 0; retry < maxRetries; retry++) {
                // 更新当前尝试次数（用于文件命名）
                currentAttempt.set(retry + 1);

                // 检查验证码是否存在
                if (!hasCaptcha(page)) {
                    log.info("验证码已消失，处理成功");
                    return true;
                }

                // 等待验证码加载和 getsig 响应
                page.waitForTimeout(1500);
                
                // 等待精灵图信息准备就绪（最多等待 3 秒）
                if (spriteInfo == null) {
                    log.debug("等待 getsig 响应解析精灵图信息...");
                    for (int wait = 0; wait < 6 && spriteInfo == null; wait++) {
                        page.waitForTimeout(500);
                    }
                    if (spriteInfo == null) {
                        log.warn("等待精灵图信息超时，将使用边缘检测");
                    }
                }

                // 尝试处理
                boolean success = attemptSlide(page, retry);
                if (success) {
                    // 等待验证结果（网络响应会触发 captchaVerified 标志）
                    page.waitForTimeout(1500);

                    // 优先检查网络响应中的 errorCode=0
                    if (captchaVerified) {
                        log.info("验证码处理成功（第 {} 次尝试，通过网络响应确认）", retry + 1);
                        return true;
                    }

                    // 回退检查：验证码 iframe 是否消失
                    if (!hasCaptcha(page)) {
                        log.info("验证码处理成功（第 {} 次尝试，iframe 已消失）", retry + 1);
                        return true;
                    }
                }

                log.info("第 {} 次尝试失败，等待验证码刷新...", retry + 1);
                page.waitForTimeout(2000);
            }

            log.warn("验证码处理失败，已达到最大重试次数: {}", maxRetries);
            return false;

        } catch (Exception e) {
            log.error("验证码处理异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 尝试一次滑动
     */
    private boolean attemptSlide(Page page, int retryCount) {
        try {
            // 获取 iframe 位置信息
            Locator iframeLocator = page.locator("#tcaptcha_iframe_dy").first();
            BoundingBox iframeBox = iframeLocator.count() > 0 ? iframeLocator.boundingBox() : null;

            if (iframeBox == null) {
                log.warn("无法获取验证码 iframe 位置");
                return false;
            }

            // 在验证码 frame 中查找元素
            for (Frame frame : page.frames()) {
                String url = frame.url();
                if (!url.contains("captcha") && !url.contains("drag_ele")) {
                    continue;
                }

                log.debug("找到验证码 frame: {}", url.substring(0, Math.min(60, url.length())));

                // 注意：不在 iframe 中注入额外的 stealth 脚本
                // GongchangLoginTest 成功的关键是只使用 Stealth4j.newStealthPage() 在页面级别注入
                // 向 iframe 额外注入脚本可能被腾讯检测为异常行为
                // injectStealthToFrame(frame);

                // 1. 检测滑块位置（优先使用 DOM 检测值，更准确）
                // API 返回的 init_x 是原始坐标，需要缩放且可能与实际 DOM 位置有偏差
                // DOM 检测直接获取滑块元素在图片中的实际位置，与缺口坐标系一致
                int detectedSliderPos = detectSliderPosition(frame, iframeBox);
                int apiSliderPos = getSliderInitX();
                int sliderCenterPos;

                if (detectedSliderPos > 0 && detectedSliderPos != DEFAULT_SLIDER_CENTER) {
                    // 优先使用 DOM 检测的位置（更准确，与 GongchangLoginTest 一致）
                    sliderCenterPos = detectedSliderPos;
                    
                    // 仅用于日志对比
                    if (apiSliderPos > 0) {
                        double scaleRatio = (double) TENCENT_CAPTCHA_TARGET_WIDTH / 672.0;
                        int apiSliderCenter = (int) Math.round((apiSliderPos + 60) * scaleRatio);
                        log.info("使用 DOM 检测滑块位置: center={} (API计算值={}, 差异={})",
                                sliderCenterPos, apiSliderCenter, sliderCenterPos - apiSliderCenter);
                    } else {
                        log.info("使用 DOM 检测滑块位置: center={}", sliderCenterPos);
                    }
                } else if (apiSliderPos > 0) {
                    // 回退到 API 计算的位置
                    double scaleRatio = (double) TENCENT_CAPTCHA_TARGET_WIDTH / 672.0;
                    int originalSliderCenter = apiSliderPos + 60;
                    sliderCenterPos = (int) Math.round(originalSliderCenter * scaleRatio);
                    log.info("使用 API 滑块位置: init_x={}, 原始中心={}, 缩放比例={}, 最终中心={}",
                            apiSliderPos, originalSliderCenter, String.format("%.3f", scaleRatio), sliderCenterPos);
                } else {
                    sliderCenterPos = DEFAULT_SLIDER_CENTER;
                    log.warn("无法检测滑块位置，使用默认值: {}", sliderCenterPos);
                }

                // 2. 提取背景图
                String backgroundBase64 = extractBackgroundImage(frame);
                if (backgroundBase64 == null) {
                    log.warn("无法提取背景图");
                    continue;
                }

                // 3. 检测缺口位置（优先使用 CaptchaManager）
                TencentCaptchaResult detection = detectGapPosition(backgroundBase64, sliderCenterPos);
                if (!detection.isSuccess()) {
                    log.warn("缺口检测失败: {}", detection.getErrorMessage());
                    continue;
                }

                log.info("检测结果: slideDistance={}, confidence={}, method={}",
                        detection.getSlideDistance(), 
                        String.format("%.3f", detection.getConfidence()), detection.getMethod());

                // 4. 执行滑动
                BoundingBox sliderBox = findSliderButton(frame);
                if (sliderBox == null) {
                    log.warn("未找到滑块按钮");
                    continue;
                }

                // 详细日志：对比坐标系
                log.info("坐标对比: iframeBox=({}, {}), sliderBox=({}, {}), sliderBoxSize={}x{}",
                        String.format("%.1f", iframeBox.x), String.format("%.1f", iframeBox.y),
                        String.format("%.1f", sliderBox.x), String.format("%.1f", sliderBox.y),
                        String.format("%.1f", sliderBox.width), String.format("%.1f", sliderBox.height));
                log.info("位置计算: 缺口中心(图片坐标)={}, 滑块中心(图片坐标)={}, 滑动距离={}, 滑块按钮中心X(页面坐标)={}",
                        detection.getGapCenter(), sliderCenterPos, detection.getSlideDistance(),
                        String.format("%.1f", sliderBox.x + sliderBox.width / 2));

                return executeSlide(page, sliderBox, detection.getSlideDistance());
            }

            return false;

        } catch (Exception e) {
            log.error("尝试滑动异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检测图片内滑块的中心位置
     */
    private int detectSliderPosition(Frame frame, BoundingBox iframeBox) {
        try {
            Locator fgItems = frame.locator(".tc-fg-item");
            int fgCount = fgItems.count();
            log.debug("检测滑块位置: tc-fg-item count={}, iframeBox=({}, {}), size={}x{}",
                    fgCount, String.format("%.1f", iframeBox.x), String.format("%.1f", iframeBox.y),
                    String.format("%.1f", iframeBox.width), String.format("%.1f", iframeBox.height));

            if (fgCount > 0) {
                for (int i = 0; i < fgCount; i++) {
                    BoundingBox itemBox = fgItems.nth(i).boundingBox();
                    if (itemBox == null) continue;

                    // 检查是否在图片区域内（Y 坐标在顶部 70%）
                    double relativeY = itemBox.y - iframeBox.y;
                    boolean isInImageArea = relativeY < iframeBox.height * 0.7;

                    log.debug("  item {}: box=({}, {}), size={}x{}, relY={}, isInImage={}",
                            i, String.format("%.1f", itemBox.x), String.format("%.1f", itemBox.y),
                            String.format("%.1f", itemBox.width), String.format("%.1f", itemBox.height),
                            String.format("%.1f", relativeY), isInImageArea);

                    if (isInImageArea && itemBox.width > 30 && itemBox.height > 30) {
                        // 计算滑块在图片中的中心位置
                        double sliderRelativeX = itemBox.x - iframeBox.x;
                        double imageAreaMargin = 10; // 图片区域左边距
                        double sliderInImageX = sliderRelativeX - imageAreaMargin;
                        double sliderCenter = sliderInImageX + itemBox.width / 2;

                        log.info("检测到图片内滑块: sliderRelativeX={}, imageMargin=10, sliderInImageX={}, width={}, center={}",
                                String.format("%.1f", sliderRelativeX), String.format("%.1f", sliderInImageX),
                                String.format("%.1f", itemBox.width), (int) Math.round(sliderCenter));

                        return (int) Math.round(sliderCenter);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("检测滑块位置失败: {}", e.getMessage());
        }

        log.warn("未检测到图片内滑块，返回默认值: {}", DEFAULT_SLIDER_CENTER);
        return DEFAULT_SLIDER_CENTER;
    }

    /**
     * 检测缺口位置
     * 优先使用 CaptchaManager，回退到 TencentSliderDetector
     *
     * @param backgroundBase64 背景图 Base64
     * @param sliderCenterPos  滑块中心位置
     * @return 检测结果
     */
    private TencentCaptchaResult detectGapPosition(String backgroundBase64, int sliderCenterPos) {
        // 尝试提取滑块图
        String sliderBase64 = extractSliderImageBase64();
        return detectGapPosition(backgroundBase64, sliderBase64, sliderCenterPos);
    }

    /**
     * 检测缺口位置（支持滑块图模板匹配）
     * 优先使用 CaptchaManager，回退到 TencentSliderDetector
     *
     * @param backgroundBase64 背景图 Base64
     * @param sliderBase64     滑块图 Base64（可选，用于模板匹配）
     * @param sliderCenterPos  滑块中心位置
     * @return 检测结果
     */
    private TencentCaptchaResult detectGapPosition(String backgroundBase64, String sliderBase64, int sliderCenterPos) {
        // 优先使用 CaptchaManager
        if (captchaManager != null && captchaManager.isAvailable(CaptchaType.SLIDER)) {
            try {
                CaptchaRequest.CaptchaRequestBuilder requestBuilder = CaptchaRequest.builder()
                        .type(CaptchaType.SLIDER)
                        .backgroundImage(backgroundBase64)
                        .targetWidth(TENCENT_CAPTCHA_TARGET_WIDTH)
                        .timeout(30000);

                // 如果有滑块图，添加到请求中
                if (sliderBase64 != null && !sliderBase64.isEmpty()) {
                    requestBuilder.sliderImage(sliderBase64);
                    log.info("使用滑块图进行模板匹配检测");
                }

                CaptchaRequest request = requestBuilder.build();

                CaptchaResult result = captchaManager.solve(request);
                if (result.isSuccess() && result.getSliderOffset() != null) {
                    int offset = result.getSliderOffset();
                    // 使用缺口中心计算滑动距离（如果有的话）
                    int gapCenter = (result.getSliderGapCenter() != null && result.getSliderGapCenter() > 0)
                            ? result.getSliderGapCenter()
                            : offset + 25; // 估算中心
                    int targetPos = gapCenter;
                    int slideDistance = targetPos - sliderCenterPos;

                    log.info("CaptchaManager 检测成功: offset={}, gapCenter={}, sliderCenter={}, slideDistance={}, confidence={}",
                            offset, gapCenter, sliderCenterPos, slideDistance, result.getConfidence());

                    return TencentCaptchaResult.builder()
                            .success(true)
                            .offset(offset)
                            .gapCenter(gapCenter)
                            .gapWidth(result.getSliderGapWidth())
                            .sliderCenter(sliderCenterPos)
                            .slideDistance(slideDistance > 0 ? slideDistance : offset)
                            .confidence(result.getConfidence())
                            .method(result.getSolverName())
                            .costTime(result.getCostTime())
                            .build();
                }
                log.debug("CaptchaManager 检测失败: {}", result.getErrorMessage());
            } catch (Exception e) {
                log.debug("CaptchaManager 检测异常: {}", e.getMessage());
            }
        }

        // 回退到 TencentSliderDetector
        if (detector != null) {
            return detector.detect(backgroundBase64, sliderBase64, sliderCenterPos);
        }

        return TencentCaptchaResult.fail("没有可用的检测器");
    }

    /**
     * 提取背景图 Base64
     */
    private String extractBackgroundImage(Frame frame) {
        try {
            Locator bgElement = frame.locator("#slideBg, .tc-bg-img");
            if (bgElement.count() == 0) {
                return null;
            }

            String style = bgElement.first().getAttribute("style");

            // 使用 detector 提取图片
            if (detector != null) {
                return detector.extractImageFromStyle(style);
            }

            // 没有 detector 时，直接截图
            byte[] screenshot = bgElement.first().screenshot();
            if (screenshot != null && screenshot.length > 0) {
                return java.util.Base64.getEncoder().encodeToString(screenshot);
            }

            return null;
        } catch (Exception e) {
            log.debug("提取背景图失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 查找滑块按钮
     */
    private BoundingBox findSliderButton(Frame frame) {
        String[] selectors = {".tc-slider-normal", "#tcSliderBlock", ".tc-fg-item"};

        for (String selector : selectors) {
            try {
                Locator locator = frame.locator(selector);
                if (locator.count() > 0) {
                    BoundingBox box = locator.first().boundingBox();
                    if (box != null) {
                        log.debug("找到滑块按钮: {}, box=({}, {}, {}x{})",
                                selector, box.x, box.y, box.width, box.height);
                        return box;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * 截图保存目录（可通过系统属性配置）
     */
    private static final String SCREENSHOT_DIR = System.getProperty("captcha.screenshot.dir", "/tmp/captcha-screenshots");

    /**
     * 是否启用截图（通过系统属性配置）
     */
    private static final boolean SCREENSHOT_ENABLED = Boolean.parseBoolean(
            System.getProperty("captcha.screenshot.enabled", "true"));

    /**
     * 执行滑动操作
     */
    private boolean executeSlide(Page page, BoundingBox sliderBox, int slideDistance) {
        try {
            double startX = sliderBox.x + sliderBox.width / 2;
            double startY = sliderBox.y + sliderBox.height / 2;

            log.info("执行滑动: startX={}, startY={}, distance={}", startX, startY, slideDistance);

            // 文件名前缀：会话ID_尝试次数
            String filePrefix = String.format("%s_attempt%d", sessionId, currentAttempt.get());

            // 1. 初始截图
            saveScreenshot(page, filePrefix, "1_initial");

            // 模拟人类行为：先随机移动几下
            for (int i = 0; i < 2; i++) {
                double randX = 200 + Math.random() * 400;
                double randY = 200 + Math.random() * 300;
                page.mouse().move(randX, randY);
                page.waitForTimeout(50 + (long) (Math.random() * 100));
            }

            // 移动到滑块位置
            page.mouse().move(startX - 50, startY + 10);
            page.waitForTimeout(100);
            page.mouse().move(startX - 20, startY);
            page.waitForTimeout(80);
            page.mouse().move(startX, startY);
            page.waitForTimeout(150 + (long) (Math.random() * 100));

            // 按下鼠标
            page.mouse().down();
            page.waitForTimeout(50);

            // 2. 滑动开始前（鼠标按下时）
            saveScreenshot(page, filePrefix, "2_mouse_down");

            // 生成人类轨迹并执行
            List<HumanTrajectoryGenerator.TrajectoryPoint> trajectory =
                    trajectoryGenerator.generate(slideDistance);

            log.debug("轨迹: {}", trajectoryGenerator.formatTrajectory(trajectory));

            // 计算截图位置
            int totalPoints = trajectory.size();
            int point20 = (int)(totalPoints * 0.2);
            int point50 = (int)(totalPoints * 0.5);
            int point70 = (int)(totalPoints * 0.7);

            double currentX = startX;
            double fixedY = startY; // Y 轴保持固定（参考 GongchangLoginTest）

            for (int i = 0; i < trajectory.size(); i++) {
                HumanTrajectoryGenerator.TrajectoryPoint point = trajectory.get(i);
                currentX += point.deltaX();

                // 关键：Y 轴保持固定，不使用抖动（参考原项目的成功实现）
                page.mouse().move(currentX, fixedY);
                // 不使用 setSteps，直接移动（参考原项目）

                // 3. 滑动过程中 20%
                if (i == point20) {
                    saveScreenshot(page, filePrefix, "3_progress_20");
                }
                // 4. 滑动过程中 50%
                if (i == point50) {
                    saveScreenshot(page, filePrefix, "4_progress_50");
                }
                // 5. 滑动过程中 70%
                if (i == point70) {
                    saveScreenshot(page, filePrefix, "5_progress_70");
                }
            }

            // 6. 滑动结束前（鼠标释放时）
            saveScreenshot(page, filePrefix, "6_before_release");

            // 释放鼠标
            page.mouse().up();
            // 等待验证结果（与 GongchangLoginTest 保持一致）
            // 腾讯验证码需要足够的时间来处理滑动并返回验证结果
            page.waitForTimeout(2000);

            log.info("滑动截图已保存: sessionId={}, attempt={}", sessionId, currentAttempt.get());

            return true;

        } catch (Exception e) {
            log.error("执行滑动失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 保存截图
     */
    private void saveScreenshot(Page page, String timestamp, String name) {
        if (!SCREENSHOT_ENABLED) return;

        try {
            String filename = String.format("%s_%s.png", timestamp, name);
            java.nio.file.Path filePath = java.nio.file.Paths.get(SCREENSHOT_DIR, filename);
            page.screenshot(new Page.ScreenshotOptions().setPath(filePath));
            log.debug("保存截图: {}", filePath);
        } catch (Exception e) {
            log.warn("保存截图失败: {}", e.getMessage());
        }
    }

    /**
     * 检查服务是否可用
     */
    @Override
    public boolean isAvailable() {
        // CaptchaManager 可用
        if (captchaManager != null && captchaManager.isAvailable(CaptchaType.SLIDER)) {
            return true;
        }
        // TencentSliderDetector 可用
        return detector != null && detector.isAvailable();
    }

    @Override
    public int getPriority() {
        return 10; // 腾讯验证码优先级较高
    }

    /**
     * 设置网络请求监控
     * 监控腾讯验证码的关键接口，并解析滑块初始位置
     */
    private void setupNetworkMonitoring(Page page) {
        // 重置滑块初始位置
        apiSliderInitX.set(-1);

        // 监控请求
        page.onRequest(request -> {
            String url = request.url();
            if (url.contains("cap_union_new_getsig") || url.contains("cap_union_new_verify")) {
                log.debug("验证码请求: {} {}", request.method(), url);
                // 保存请求信息到文件
                saveNetworkData("request", url, request.method(), request.postData());
            }
        });

        // 监控响应
        page.onResponse(response -> {
            String url = response.url();
            if (url.contains("cap_union_new_getsig")) {
                // 解析 getsig 响应，获取滑块初始位置
                try {
                    String body = response.text();
                    parseSliderInitPosition(body);
                    // 保存响应到文件
                    saveNetworkData("getsig_response", url, String.valueOf(response.status()), body);
                } catch (Exception e) {
                    log.debug("无法解析 getsig 响应: {}", e.getMessage());
                }
            } else if (url.contains("cap_union_new_verify")) {
                // 记录验证结果
                try {
                    String body = response.text();
                    log.info("验证码校验响应: {}", body.length() > 500 ? body.substring(0, 500) + "..." : body);
                    // 保存响应到文件
                    saveNetworkData("verify_response", url, String.valueOf(response.status()), body);
                    
                    // 解析 errorCode，如果为 0 表示验证码已成功
                    if (body.contains("\"errorCode\":\"0\"") || body.contains("\"errorCode\": \"0\"")) {
                        captchaVerified = true;
                        log.info("验证码校验成功（errorCode=0）");
                    }
                } catch (Exception e) {
                    log.debug("无法读取验证响应: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * 保存网络请求/响应数据到文件
     */
    private void saveNetworkData(String type, String url, String statusOrMethod, String body) {
        if (!SCREENSHOT_ENABLED || sessionId == null) return;

        try {
            int attempt = currentAttempt.get();
            String filename = String.format("%s_attempt%d_%s.txt", sessionId, attempt, type);
            java.nio.file.Path filePath = java.nio.file.Paths.get(SCREENSHOT_DIR, filename);

            StringBuilder content = new StringBuilder();
            content.append("URL: ").append(url).append("\n");
            content.append("Status/Method: ").append(statusOrMethod).append("\n");
            content.append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n");
            content.append("---\n");
            content.append(body != null ? body : "(empty)");

            java.nio.file.Files.writeString(filePath, content.toString());
            log.debug("保存网络数据: {}", filename);
        } catch (Exception e) {
            log.debug("保存网络数据失败: {}", e.getMessage());
        }
    }

    /**
     * 解析滑块初始位置和精灵图信息
     * 从 cap_union_new_getsig 响应中提取：
     * 1. fg_elem_list 中 id=1 的 init_pos（滑块初始位置）
     * 2. sprite_url（精灵图地址）
     * 3. sprite_pos 和 size_2d（滑块在精灵图中的位置和尺寸）
     */
    private void parseSliderInitPosition(String responseBody) {
        try {
            // 1. 解析 init_pos，格式如: "init_pos":[50,221]
            Pattern initPosPattern = Pattern.compile("\"id\"\\s*:\\s*1[^}]*\"init_pos\"\\s*:\\s*\\[(\\d+)\\s*,\\s*(\\d+)\\]");
            Matcher initPosMatcher = initPosPattern.matcher(responseBody);
            if (initPosMatcher.find()) {
                int initX = Integer.parseInt(initPosMatcher.group(1));
                int initY = Integer.parseInt(initPosMatcher.group(2));
                apiSliderInitX.set(initX);
                log.info("从 API 响应解析到滑块初始位置: x={}, y={}", initX, initY);
            } else {
                // 备用方案：直接查找第一个 init_pos
                Pattern fallbackPattern = Pattern.compile("\"init_pos\"\\s*:\\s*\\[(\\d+)\\s*,\\s*(\\d+)\\]");
                Matcher fallbackMatcher = fallbackPattern.matcher(responseBody);
                if (fallbackMatcher.find()) {
                    int initX = Integer.parseInt(fallbackMatcher.group(1));
                    apiSliderInitX.set(initX);
                    log.info("从 API 响应解析到滑块初始位置（备用）: x={}", initX);
                }
            }

            // 2. 解析精灵图 URL
            // 格式: "sprite_url":"https://..." 或 相对路径 "/cap_union_new_getcapbysig?..."
            Pattern spriteUrlPattern = Pattern.compile("\"sprite_url\"\\s*:\\s*\"([^\"]+)\"");
            Matcher spriteUrlMatcher = spriteUrlPattern.matcher(responseBody);
            String spriteUrl = null;
            if (spriteUrlMatcher.find()) {
                spriteUrl = spriteUrlMatcher.group(1);
                // 处理转义字符
                spriteUrl = spriteUrl.replace("\\/", "/");
                // 如果是相对路径，添加域名前缀
                if (spriteUrl.startsWith("/")) {
                    spriteUrl = "https://t.captcha.qq.com" + spriteUrl;
                }
                log.debug("解析到精灵图 URL: {}", spriteUrl.length() > 100 ? spriteUrl.substring(0, 100) + "..." : spriteUrl);
            }

            // 3. 解析精灵图中滑块的位置和尺寸
            // 格式: "id":1, ..., "sprite_pos":[140,490], "size_2d":[120,120]
            // 需要在 fg_elem_list 中找到 id=1 的元素
            int sliderSpriteX = 0, sliderSpriteY = 0, sliderWidth = 120, sliderHeight = 120;

            // 查找 id=1 对应的 sprite_pos
            Pattern spritePosPattern = Pattern.compile("\"id\"\\s*:\\s*1[^}]*\"sprite_pos\"\\s*:\\s*\\[(\\d+)\\s*,\\s*(\\d+)\\]");
            Matcher spritePosMatcher = spritePosPattern.matcher(responseBody);
            if (spritePosMatcher.find()) {
                sliderSpriteX = Integer.parseInt(spritePosMatcher.group(1));
                sliderSpriteY = Integer.parseInt(spritePosMatcher.group(2));
                log.debug("解析到滑块在精灵图中的位置: x={}, y={}", sliderSpriteX, sliderSpriteY);
            }

            // 查找 id=1 对应的 size_2d
            Pattern size2dPattern = Pattern.compile("\"id\"\\s*:\\s*1[^}]*\"size_2d\"\\s*:\\s*\\[(\\d+)\\s*,\\s*(\\d+)\\]");
            Matcher size2dMatcher = size2dPattern.matcher(responseBody);
            if (size2dMatcher.find()) {
                sliderWidth = Integer.parseInt(size2dMatcher.group(1));
                sliderHeight = Integer.parseInt(size2dMatcher.group(2));
                log.debug("解析到滑块尺寸: {}x{}", sliderWidth, sliderHeight);
            }

            // 保存精灵图信息
            if (spriteUrl != null && sliderSpriteX > 0) {
                spriteInfo = new SpriteInfo(spriteUrl, sliderSpriteX, sliderSpriteY, sliderWidth, sliderHeight);
                log.info("精灵图信息: url={}, 滑块位置=({},{}), 尺寸={}x{}",
                        spriteUrl.length() > 50 ? "..." + spriteUrl.substring(spriteUrl.length() - 50) : spriteUrl,
                        sliderSpriteX, sliderSpriteY, sliderWidth, sliderHeight);
            }

        } catch (Exception e) {
            log.debug("解析滑块初始位置/精灵图信息失败: {}", e.getMessage());
        }
    }

    /**
     * 获取滑块初始 X 位置
     * 优先使用 API 响应中的值，否则返回默认值
     */
    private int getSliderInitX() {
        int apiValue = apiSliderInitX.get();
        if (apiValue > 0) {
            return apiValue;
        }
        return DEFAULT_SLIDER_CENTER;
    }

    /**
     * 下载精灵图并裁剪出滑块图片
     *
     * @return 滑块图片的 Base64 编码，如果失败返回 null
     */
    private String extractSliderImageBase64() {
        SpriteInfo info = spriteInfo;
        if (info == null || info.spriteUrl() == null || info.spriteUrl().isEmpty()) {
            log.debug("精灵图信息不可用，无法提取滑块图片");
            return null;
        }

        try {
            log.debug("开始下载精灵图: {}", info.spriteUrl());

            // 下载精灵图
            URL url = new URL(info.spriteUrl());
            BufferedImage spriteImage;
            try (InputStream is = url.openStream()) {
                spriteImage = ImageIO.read(is);
            }

            if (spriteImage == null) {
                log.warn("无法解析精灵图");
                return null;
            }

            log.debug("精灵图尺寸: {}x{}", spriteImage.getWidth(), spriteImage.getHeight());

            // 裁剪出滑块图片
            int x = info.sliderSpriteX();
            int y = info.sliderSpriteY();
            int width = info.sliderWidth();
            int height = info.sliderHeight();

            // 边界检查
            if (x < 0 || y < 0 || x + width > spriteImage.getWidth() || y + height > spriteImage.getHeight()) {
                log.warn("滑块裁剪区域超出精灵图边界: sprite={}x{}, crop=({},{})~{}x{}",
                        spriteImage.getWidth(), spriteImage.getHeight(), x, y, width, height);
                // 调整为有效范围
                x = Math.max(0, Math.min(x, spriteImage.getWidth() - 1));
                y = Math.max(0, Math.min(y, spriteImage.getHeight() - 1));
                width = Math.min(width, spriteImage.getWidth() - x);
                height = Math.min(height, spriteImage.getHeight() - y);
            }

            BufferedImage sliderImage = spriteImage.getSubimage(x, y, width, height);
            log.info("成功裁剪滑块图片: {}x{}", sliderImage.getWidth(), sliderImage.getHeight());

            // 保存调试图片
            if (SCREENSHOT_ENABLED && sessionId != null) {
                try {
                    String filename = String.format("%s/%s_attempt%d_slider.png",
                            SCREENSHOT_DIR, sessionId, currentAttempt.get());
                    ImageIO.write(sliderImage, "PNG", new java.io.File(filename));
                    log.debug("滑块图片已保存: {}", filename);
                } catch (Exception e) {
                    log.debug("保存滑块调试图片失败: {}", e.getMessage());
                }
            }

            // 转换为 Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(sliderImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            log.debug("滑块图片 Base64 长度: {}", base64.length());
            return base64;

        } catch (Exception e) {
            log.warn("提取滑块图片失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取精灵图信息
     */
    public SpriteInfo getSpriteInfo() {
        return spriteInfo;
    }

    /**
     * 尝试在 iframe 中注入隐身脚本
     * 用于绕过跨域 iframe 中的机器人检测
     */
    private void injectStealthToFrame(Frame frame) {
        try {
            // 执行隐身脚本
            frame.evaluate(StealthHelper.getStealthScript());
            log.debug("iframe 隐身脚本注入成功");
        } catch (Exception e) {
            // 跨域 iframe 可能无法执行脚本，这是预期的
            log.debug("iframe 隐身脚本注入失败（可能是跨域限制）: {}", e.getMessage());
            
            // 尝试使用 addScriptTag 方式注入
            try {
                frame.addScriptTag(new Frame.AddScriptTagOptions()
                        .setContent(getMinimalStealthScript()));
                log.debug("iframe 使用 addScriptTag 注入成功");
            } catch (Exception e2) {
                log.debug("iframe addScriptTag 注入也失败: {}", e2.getMessage());
            }
        }
    }

    /**
     * 获取最小化的隐身脚本（用于 addScriptTag 注入）
     */
    private String getMinimalStealthScript() {
        return """
            (function() {
                // 移除 webdriver 标记
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                    configurable: true
                });
                
                // 删除自动化标记
                delete window.__playwright;
                delete window.__pw_manual;
                
                // 模拟 chrome 对象
                if (!window.chrome) {
                    window.chrome = {
                        runtime: { id: undefined }
                    };
                }
            })();
            """;
    }
}
