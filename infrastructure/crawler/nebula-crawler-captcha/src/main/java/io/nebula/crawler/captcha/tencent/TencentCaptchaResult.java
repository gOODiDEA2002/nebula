package io.nebula.crawler.captcha.tencent;

import lombok.Builder;
import lombok.Data;

/**
 * 腾讯验证码处理结果
 *
 * @author Nebula Team
 * @since 2.0.2
 */
@Data
@Builder
public class TencentCaptchaResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 检测到的偏移量（缩放后）
     */
    private int offset;

    /**
     * 缺口中心位置（缩放后）
     */
    private int gapCenter;

    /**
     * 缺口宽度
     */
    private int gapWidth;

    /**
     * 滑块中心位置
     */
    private int sliderCenter;

    /**
     * 计算得到的滑动距离
     */
    private int slideDistance;

    /**
     * 置信度 (0-1)
     */
    private double confidence;

    /**
     * 检测方法
     */
    private String method;

    /**
     * 处理耗时（毫秒）
     */
    private long costTime;

    /**
     * 创建成功结果
     */
    public static TencentCaptchaResult success(int slideDistance, double confidence, String method) {
        return TencentCaptchaResult.builder()
                .success(true)
                .slideDistance(slideDistance)
                .confidence(confidence)
                .method(method)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static TencentCaptchaResult fail(String errorMessage) {
        return TencentCaptchaResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
