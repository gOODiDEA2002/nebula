package io.nebula.crawler.captcha.solver;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.exception.CaptchaException;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 点击验证码解决器
 * 支持本地OCR识别和第三方平台识别
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class ClickCaptchaSolver implements CaptchaSolver {

    private final List<CaptchaServiceProvider> providers;
    private final boolean localEnabled;

    /**
     * 构造函数
     *
     * @param providers    第三方平台列表
     * @param localEnabled 是否启用本地识别
     */
    public ClickCaptchaSolver(List<CaptchaServiceProvider> providers, boolean localEnabled) {
        this.providers = providers;
        this.localEnabled = localEnabled;
    }

    @Override
    public String getName() {
        return "ClickCaptchaSolver";
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.CLICK;
    }

    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();

        try {
            String hint = request.getGestureHint(); // 复用gestureHint字段存储点击提示

            // 尝试本地点击位置识别
            if (localEnabled) {
                List<int[]> points = detectClickPoints(request.getImageBase64(), hint);
                if (points != null && !points.isEmpty()) {
                    return CaptchaResult.builder()
                            .success(true)
                            .type(CaptchaType.CLICK)
                            .clickPoints(points)
                            .costTime(System.currentTimeMillis() - startTime)
                            .solverName("LocalClickDetection")
                            .confidence(0.75)
                            .build();
                }
            }

            // 尝试第三方平台
            for (CaptchaServiceProvider provider : providers) {
                if (!provider.supportsType(CaptchaType.CLICK)) {
                    continue;
                }
                if (!provider.isAvailable()) {
                    continue;
                }

                try {
                    CaptchaResult result = provider.solveClick(
                            request.getImageBase64(),
                            hint,
                            request.getTimeout()
                    );
                    if (result.isSuccess()) {
                        result.setCostTime(System.currentTimeMillis() - startTime);
                        result.setSolverName(provider.getName());
                        result.setType(CaptchaType.CLICK);
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("打码平台 {} 识别点击验证码失败: {}", provider.getName(), e.getMessage());
                }
            }

            throw new CaptchaException("点击验证码识别失败");
        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("点击验证码识别异常", e);
        }
    }

    /**
     * 本地检测点击位置
     * 使用OCR识别文字位置或模板匹配
     *
     * @param imageBase64 图片Base64
     * @param hint        点击提示（如"请依次点击：春夏秋冬"）
     * @return 点击坐标列表，null表示检测失败
     */
    private List<int[]> detectClickPoints(String imageBase64, String hint) {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return null;
        }

        // TODO: 实现基于OCR的点击位置检测
        // 算法思路：
        // 1. 解析提示文字，提取需要点击的目标（如文字、图标）
        // 2. 使用OCR识别图片中的文字及其位置
        // 3. 或使用目标检测模型识别图标位置
        // 4. 按提示顺序返回坐标点

        log.debug("本地点击位置检测暂未实现，需要OCR/目标检测支持");
        return null;
    }

    /**
     * 解析点击提示，提取目标列表
     *
     * @param hint 点击提示
     * @return 目标列表
     */
    private List<String> parseClickTargets(String hint) {
        List<String> targets = new ArrayList<>();
        if (hint == null || hint.isEmpty()) {
            return targets;
        }

        // 常见提示格式：
        // "请依次点击：春夏秋冬"
        // "点击下图中的：猫、狗、鸟"
        // "请按顺序点击：1、2、3"

        // 提取冒号后的内容
        int colonIndex = hint.indexOf("：");
        if (colonIndex == -1) {
            colonIndex = hint.indexOf(":");
        }

        String content = colonIndex >= 0 ? hint.substring(colonIndex + 1).trim() : hint;

        // 按分隔符拆分
        String[] parts;
        if (content.contains("、")) {
            parts = content.split("、");
        } else if (content.contains(",")) {
            parts = content.split(",");
        } else if (content.contains(" ")) {
            parts = content.split("\\s+");
        } else {
            // 单字符拆分（如"春夏秋冬"）
            for (char c : content.toCharArray()) {
                targets.add(String.valueOf(c));
            }
            return targets;
        }

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                targets.add(trimmed);
            }
        }

        return targets;
    }

    @Override
    public boolean isAvailable() {
        return localEnabled || providers.stream()
                .anyMatch(p -> p.supportsType(CaptchaType.CLICK) && p.isAvailable());
    }

    @Override
    public int getPriority() {
        return 35;
    }
}
