package io.nebula.crawler.captcha.tencent;

import io.nebula.crawler.captcha.cv.OpenCvService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 腾讯验证码缺口检测器
 * 
 * 负责从腾讯验证码中提取背景图并检测缺口位置
 *
 * @author Nebula Team
 * @since 2.0.2
 */
@Slf4j
public class TencentSliderDetector {

    /**
     * 腾讯验证码的目标显示宽度（页面上的实际显示宽度）
     */
    public static final int TENCENT_CAPTCHA_TARGET_WIDTH = 340;

    private final OpenCvService openCvService;
    private final OkHttpClient httpClient;

    /**
     * 检测结果
     */
    public record DetectionResult(
            boolean success,
            int offset,           // 缺口左边缘（缩放后）
            int gapCenter,        // 缺口中心（缩放后）
            int gapWidth,         // 缺口宽度
            double confidence,    // 置信度
            String method,        // 检测方法
            String errorMessage   // 错误信息
    ) {
        public static DetectionResult success(int offset, int gapCenter, int gapWidth,
                                               double confidence, String method) {
            return new DetectionResult(true, offset, gapCenter, gapWidth, confidence, method, null);
        }

        public static DetectionResult fail(String errorMessage) {
            return new DetectionResult(false, 0, 0, 0, 0, null, errorMessage);
        }
    }

    /**
     * 构造函数
     *
     * @param openCvService OpenCV 服务（可选，为 null 时使用独立 HTTP 调用）
     */
    public TencentSliderDetector(OpenCvService openCvService) {
        this.openCvService = openCvService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 构造函数（使用指定的 OpenCV 服务地址）
     *
     * @param openCvServerUrl OpenCV 服务地址
     */
    public TencentSliderDetector(String openCvServerUrl) {
        this.openCvService = new OpenCvService(openCvServerUrl);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 检测缺口位置（不使用滑块图）
     *
     * @param backgroundBase64 背景图 Base64
     * @param sliderCenterPos  滑块中心位置（页面坐标）
     * @return 检测结果（包含滑动距离）
     */
    public TencentCaptchaResult detect(String backgroundBase64, int sliderCenterPos) {
        return detect(backgroundBase64, null, sliderCenterPos, TENCENT_CAPTCHA_TARGET_WIDTH);
    }

    /**
     * 检测缺口位置（使用滑块图进行模板匹配）
     *
     * @param backgroundBase64 背景图 Base64
     * @param sliderBase64     滑块图 Base64（可选，用于模板匹配）
     * @param sliderCenterPos  滑块中心位置（页面坐标）
     * @return 检测结果（包含滑动距离）
     */
    public TencentCaptchaResult detect(String backgroundBase64, String sliderBase64, int sliderCenterPos) {
        return detect(backgroundBase64, sliderBase64, sliderCenterPos, TENCENT_CAPTCHA_TARGET_WIDTH);
    }

    /**
     * 检测缺口位置（完整参数）
     *
     * @param backgroundBase64 背景图 Base64
     * @param sliderBase64     滑块图 Base64（可选，用于模板匹配）
     * @param sliderCenterPos  滑块中心位置（页面坐标）
     * @param targetWidth      目标显示宽度
     * @return 检测结果
     */
    public TencentCaptchaResult detect(String backgroundBase64, String sliderBase64,
                                        int sliderCenterPos, int targetWidth) {
        long startTime = System.currentTimeMillis();

        if (backgroundBase64 == null || backgroundBase64.isEmpty()) {
            return TencentCaptchaResult.fail("背景图为空");
        }

        try {
            // 调用 OpenCV 服务检测缺口
            DetectionResult detection = callOpenCvService(backgroundBase64, sliderBase64, targetWidth);

            if (!detection.success()) {
                return TencentCaptchaResult.builder()
                        .success(false)
                        .errorMessage(detection.errorMessage())
                        .costTime(System.currentTimeMillis() - startTime)
                        .build();
            }

            // 计算滑动距离
            // 使用缺口中心与滑块中心对齐的方式
            int targetPos = detection.gapCenter() > 0 ? detection.gapCenter() : detection.offset();
            int slideDistance = targetPos - sliderCenterPos;

            log.info("缺口检测成功: offset={}, gapCenter={}, sliderCenter={}, slideDistance={}, confidence={:.3f}, method={}",
                    detection.offset(), detection.gapCenter(), sliderCenterPos,
                    slideDistance, detection.confidence(), detection.method());

            return TencentCaptchaResult.builder()
                    .success(true)
                    .offset(detection.offset())
                    .gapCenter(detection.gapCenter())
                    .gapWidth(detection.gapWidth())
                    .sliderCenter(sliderCenterPos)
                    .slideDistance(slideDistance > 0 ? slideDistance : detection.offset())
                    .confidence(detection.confidence())
                    .method(detection.method())
                    .costTime(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("缺口检测异常: {}", e.getMessage(), e);
            return TencentCaptchaResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .costTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 调用 OpenCV 服务检测缺口
     *
     * @param backgroundBase64 背景图 Base64
     * @param sliderBase64     滑块图 Base64（可选，用于模板匹配）
     * @param targetWidth      目标显示宽度
     */
    private DetectionResult callOpenCvService(String backgroundBase64, String sliderBase64, int targetWidth) {
        // 优先使用注入的 OpenCvService
        if (openCvService != null && openCvService.isAvailable()) {
            // 如果提供了滑块图，记录日志
            if (sliderBase64 != null && !sliderBase64.isEmpty()) {
                log.info("使用滑块图进行模板匹配检测");
            }

            OpenCvService.SliderDetectResult result = openCvService.detectSliderGap(
                    backgroundBase64, sliderBase64, targetWidth);
            if (result != null) {
                // 使用服务返回的缺口中心，如果没有则估算
                int gapCenter = (result.gapCenter() != null && result.gapCenter() > 0)
                        ? result.gapCenter()
                        : result.offset() + 25; // 估算中心位置
                int gapWidth = (result.gapWidth() != null && result.gapWidth() > 0)
                        ? result.gapWidth()
                        : 50; // 估算宽度

                String method = (sliderBase64 != null && !sliderBase64.isEmpty())
                        ? "opencv_template_matching"
                        : "opencv_service";

                log.debug("OpenCV 检测结果: offset={}, gapCenter={}, gapWidth={}, confidence={}, method={}",
                        result.offset(), gapCenter, gapWidth, result.confidence(), method);

                return DetectionResult.success(
                        result.offset(),
                        gapCenter,
                        gapWidth,
                        result.confidence(),
                        method
                );
            }
        }

        return DetectionResult.fail("OpenCV 服务不可用");
    }

    /**
     * 从样式字符串提取图片 URL 并下载为 Base64
     *
     * @param styleString CSS 样式字符串（包含 background-image）
     * @return Base64 编码的图片数据，失败返回 null
     */
    public String extractImageFromStyle(String styleString) {
        if (styleString == null || !styleString.contains("background-image")) {
            return null;
        }

        try {
            // 解析 URL: background-image: url("...")
            Pattern pattern = Pattern.compile("background-image:\\s*url\\([\"']?([^\"')]+)[\"']?\\)");
            Matcher matcher = pattern.matcher(styleString);
            if (!matcher.find()) {
                return null;
            }

            String imageUrl = matcher.group(1);
            // 处理 HTML 实体
            imageUrl = imageUrl.replace("&amp;", "&");

            log.debug("提取到图片 URL: {}...", imageUrl.substring(0, Math.min(80, imageUrl.length())));

            return downloadImageAsBase64(imageUrl);

        } catch (Exception e) {
            log.warn("提取图片失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 下载图片并转换为 Base64
     */
    public String downloadImageAsBase64(String imageUrl) {
        try {
            Request request = new Request.Builder()
                    .url(imageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "image/*")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("图片下载失败: HTTP {}", response.code());
                    return null;
                }

                byte[] imageBytes = response.body().bytes();
                log.debug("图片下载成功, 大小: {} bytes", imageBytes.length);

                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            log.warn("图片下载异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查 OpenCV 服务是否可用
     */
    public boolean isAvailable() {
        return openCvService != null && openCvService.isAvailable();
    }
}
