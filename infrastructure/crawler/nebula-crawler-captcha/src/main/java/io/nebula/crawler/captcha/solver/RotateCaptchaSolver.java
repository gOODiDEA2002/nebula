package io.nebula.crawler.captcha.solver;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.cv.OpenCvService;
import io.nebula.crawler.captcha.exception.CaptchaException;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 旋转验证码解决器
 * 支持本地OpenCV服务检测和第三方平台识别
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class RotateCaptchaSolver implements CaptchaSolver {

    private final List<CaptchaServiceProvider> providers;
    private final boolean localEnabled;
    private final OpenCvService openCvService;

    /**
     * 构造函数
     *
     * @param providers     第三方平台列表
     * @param localEnabled  是否启用本地识别
     * @param openCvService OpenCV服务（可选）
     */
    public RotateCaptchaSolver(List<CaptchaServiceProvider> providers,
                               boolean localEnabled,
                               OpenCvService openCvService) {
        this.providers = providers;
        this.localEnabled = localEnabled;
        this.openCvService = openCvService;
    }

    @Override
    public String getName() {
        return "RotateCaptchaSolver";
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.ROTATE;
    }

    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();

        try {
            // 尝试本地OpenCV服务检测旋转角度
            if (localEnabled && openCvService != null) {
                Integer angle = detectRotateAngle(request.getImageBase64());
                if (angle != null) {
                    return CaptchaResult.builder()
                            .success(true)
                            .type(CaptchaType.ROTATE)
                            .rotateAngle(angle)
                            .costTime(System.currentTimeMillis() - startTime)
                            .solverName("LocalOpenCV")
                            .confidence(0.8)
                            .build();
                }
            }

            // 尝试第三方平台
            for (CaptchaServiceProvider provider : providers) {
                if (!provider.supportsType(CaptchaType.ROTATE)) {
                    continue;
                }
                if (!provider.isAvailable()) {
                    continue;
                }

                try {
                    // 使用通用图像识别接口，提示为旋转类型
                    CaptchaResult result = solveWithProvider(provider, request);
                    if (result.isSuccess()) {
                        result.setCostTime(System.currentTimeMillis() - startTime);
                        result.setSolverName(provider.getName());
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("打码平台 {} 识别旋转验证码失败: {}", provider.getName(), e.getMessage());
                }
            }

            throw new CaptchaException("旋转验证码识别失败");
        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("旋转验证码识别异常", e);
        }
    }

    /**
     * 使用OpenCV服务检测旋转角度
     *
     * @param imageBase64 图片Base64
     * @return 旋转角度（0-360），null表示检测失败
     */
    private Integer detectRotateAngle(String imageBase64) {
        if (openCvService == null) {
            log.debug("OpenCV服务未配置");
            return null;
        }

        if (imageBase64 == null || imageBase64.isEmpty()) {
            log.debug("缺少图片数据");
            return null;
        }

        if (!openCvService.isAvailable()) {
            log.debug("OpenCV服务不可用");
            return null;
        }

        OpenCvService.RotateDetectResult result = openCvService.detectRotateAngle(imageBase64);
        if (result != null) {
            log.info("OpenCV检测旋转角度成功: angle={}, confidence={}",
                    result.angle(), result.confidence());
            return result.angle();
        }

        return null;
    }

    /**
     * 使用第三方平台识别
     *
     * @param provider 打码平台
     * @param request  请求
     * @return 识别结果
     */
    private CaptchaResult solveWithProvider(CaptchaServiceProvider provider, CaptchaRequest request)
            throws CaptchaException {
        // 将旋转验证码作为特殊图像验证码处理
        // 返回的text字段应该包含角度数值
        try {
            java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
            byte[] imageData = decoder.decode(request.getImageBase64());

            CaptchaResult result = provider.solveImage(imageData, request.getTimeout());
            if (result.isSuccess() && result.getText() != null) {
                try {
                    int angle = Integer.parseInt(result.getText().trim());
                    result.setRotateAngle(angle);
                    result.setType(CaptchaType.ROTATE);
                    return result;
                } catch (NumberFormatException e) {
                    log.warn("打码平台返回的角度无法解析: {}", result.getText());
                }
            }
            return result;
        } catch (Exception e) {
            throw new CaptchaException("调用打码平台失败", e);
        }
    }

    @Override
    public boolean isAvailable() {
        // 本地OpenCV服务可用 或 有可用的第三方平台
        boolean localAvailable = localEnabled &&
                openCvService != null &&
                openCvService.isAvailable();

        boolean providerAvailable = providers.stream()
                .anyMatch(p -> p.supportsType(CaptchaType.ROTATE) && p.isAvailable());

        return localAvailable || providerAvailable;
    }

    @Override
    public int getPriority() {
        return 40;
    }
}
