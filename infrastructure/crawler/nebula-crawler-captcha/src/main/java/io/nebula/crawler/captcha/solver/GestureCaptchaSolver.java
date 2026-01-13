package io.nebula.crawler.captcha.solver;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.exception.CaptchaException;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 手势验证码解决器
 * 处理九宫格轨迹验证码
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class GestureCaptchaSolver implements CaptchaSolver {

    private final List<CaptchaServiceProvider> providers;

    // 九宫格坐标映射（以3x3为例，实际坐标根据图片大小调整）
    private static final int[][] GRID_POINTS = {
            {50, 50}, {150, 50}, {250, 50},   // 0, 1, 2
            {50, 150}, {150, 150}, {250, 150}, // 3, 4, 5
            {50, 250}, {150, 250}, {250, 250}  // 6, 7, 8
    };

    // 常见图案映射
    private static final Map<String, int[]> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put("Z", new int[]{0, 1, 2, 4, 6, 7, 8});
        PATTERNS.put("N", new int[]{6, 3, 0, 4, 8, 5, 2});
        PATTERNS.put("L", new int[]{0, 3, 6, 7, 8});
        PATTERNS.put("7", new int[]{0, 1, 2, 4, 6});
        PATTERNS.put("M", new int[]{6, 3, 0, 4, 2, 5, 8});
        PATTERNS.put("S", new int[]{2, 1, 0, 4, 8, 7, 6});
        PATTERNS.put("C", new int[]{2, 1, 0, 3, 6, 7, 8});
        PATTERNS.put("U", new int[]{0, 3, 6, 7, 8, 5, 2});
        PATTERNS.put("V", new int[]{0, 4, 8, 4, 2});
        PATTERNS.put("X", new int[]{0, 4, 8, 4, 2, 4, 6});
    }

    public GestureCaptchaSolver(List<CaptchaServiceProvider> providers) {
        this.providers = providers != null ? providers : List.of();
    }

    @Override
    public String getName() {
        return "GestureCaptchaSolver";
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.GESTURE;
    }

    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();

        try {
            String hint = request.getGestureHint();

            // 1. 根据提示匹配已知图案
            if (hint != null && !hint.isEmpty()) {
                List<int[]> track = recognizePattern(hint);
                if (track != null && !track.isEmpty()) {
                    log.info("手势图案匹配成功: hint={}", hint);
                    return CaptchaResult.builder()
                            .success(true)
                            .type(CaptchaType.GESTURE)
                            .gestureTrack(track)
                            .costTime(System.currentTimeMillis() - startTime)
                            .confidence(0.9)
                            .solverName("PatternMatch")
                            .build();
                }
            }

            // 2. 使用第三方平台
            for (CaptchaServiceProvider provider : providers) {
                if (!provider.supportsType(CaptchaType.GESTURE)) {
                    continue;
                }
                if (!provider.isAvailable()) {
                    log.debug("打码平台 {} 不可用，跳过", provider.getName());
                    continue;
                }

                try {
                    log.debug("使用打码平台 {} 识别手势验证码", provider.getName());
                    CaptchaResult result = provider.solveGesture(
                            request.getImageBase64(),
                            hint,
                            request.getTimeout()
                    );
                    if (result.isSuccess()) {
                        result.setCostTime(System.currentTimeMillis() - startTime);
                        result.setSolverName("Provider-" + provider.getName());
                        return result;
                    }
                    log.debug("打码平台 {} 识别失败: {}", provider.getName(), result.getErrorMessage());
                } catch (Exception e) {
                    log.warn("打码平台 {} 识别手势失败: {}", provider.getName(), e.getMessage());
                }
            }

            throw new CaptchaException("手势验证码识别失败");

        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("手势验证码识别异常: " + e.getMessage(), e);
        }
    }

    /**
     * 根据提示匹配图案并生成轨迹
     */
    private List<int[]> recognizePattern(String hint) {
        if (hint == null) {
            return null;
        }

        String upperHint = hint.toUpperCase();
        for (Map.Entry<String, int[]> entry : PATTERNS.entrySet()) {
            if (upperHint.contains(entry.getKey())) {
                return Arrays.stream(entry.getValue())
                        .mapToObj(i -> GRID_POINTS[i])
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    @Override
    public boolean isAvailable() {
        // 图案匹配始终可用
        return true;
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
