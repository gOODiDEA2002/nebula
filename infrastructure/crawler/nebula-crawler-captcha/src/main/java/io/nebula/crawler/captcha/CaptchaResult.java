package io.nebula.crawler.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 验证码识别结果
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResult {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 验证码类型
     */
    private CaptchaType type;

    /**
     * 识别结果文本（图形验证码）
     */
    private String text;

    /**
     * 滑块偏移量（滑块验证码）
     */
    private Integer sliderOffset;

    /**
     * 点击坐标列表（点击验证码）- [x, y]
     */
    private List<int[]> clickPoints;

    /**
     * 手势轨迹点（手势验证码）- [x, y]
     */
    private List<int[]> gestureTrack;

    /**
     * 旋转角度（旋转验证码）
     */
    private Integer rotateAngle;

    /**
     * reCAPTCHA Token
     */
    private String recaptchaToken;

    /**
     * hCaptcha Token
     */
    private String hcaptchaToken;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 识别耗时（毫秒）
     */
    private long costTime;

    /**
     * 置信度（0-1）
     */
    private double confidence;

    /**
     * 使用的识别方式
     */
    private String solverName;

    /**
     * 创建成功结果（图形验证码）
     */
    public static CaptchaResult successWithText(String text) {
        return CaptchaResult.builder()
                .success(true)
                .type(CaptchaType.IMAGE)
                .text(text)
                .build();
    }

    /**
     * 创建成功结果（滑块验证码）
     */
    public static CaptchaResult successWithOffset(int offset) {
        return CaptchaResult.builder()
                .success(true)
                .type(CaptchaType.SLIDER)
                .sliderOffset(offset)
                .build();
    }

    /**
     * 创建成功结果（reCAPTCHA）
     */
    public static CaptchaResult successWithRecaptchaToken(String token) {
        return CaptchaResult.builder()
                .success(true)
                .type(CaptchaType.RECAPTCHA)
                .recaptchaToken(token)
                .build();
    }

    /**
     * 创建成功结果（旋转验证码）
     */
    public static CaptchaResult successWithAngle(int angle) {
        return CaptchaResult.builder()
                .success(true)
                .type(CaptchaType.ROTATE)
                .rotateAngle(angle)
                .build();
    }

    /**
     * 创建成功结果（手势验证码）
     */
    public static CaptchaResult successWithGestureTrack(List<int[]> track) {
        return CaptchaResult.builder()
                .success(true)
                .type(CaptchaType.GESTURE)
                .gestureTrack(track)
                .build();
    }

    /**
     * 创建成功结果（点击验证码）
     */
    public static CaptchaResult successWithClickPoints(List<int[]> points) {
        return CaptchaResult.builder()
                .success(true)
                .type(CaptchaType.CLICK)
                .clickPoints(points)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static CaptchaResult fail(String errorMessage) {
        return CaptchaResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建失败结果（带类型）
     */
    public static CaptchaResult fail(CaptchaType type, String errorMessage) {
        return CaptchaResult.builder()
                .success(false)
                .type(type)
                .errorMessage(errorMessage)
                .build();
    }
}
