package io.nebula.crawler.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码请求
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaRequest {

    /**
     * 任务ID（用于追踪）
     */
    private String taskId;

    /**
     * 验证码类型
     */
    private CaptchaType type;

    /**
     * 验证码图片（Base64编码）
     */
    private String imageBase64;

    /**
     * 验证码图片URL
     */
    private String imageUrl;

    /**
     * 目标网站URL（用于reCAPTCHA等）
     */
    private String siteUrl;

    /**
     * 站点密钥（用于reCAPTCHA等）
     */
    private String siteKey;

    /**
     * 滑块验证码的背景图（Base64）
     */
    private String backgroundImage;

    /**
     * 滑块验证码的滑块图（Base64）
     */
    private String sliderImage;

    /**
     * 手势验证码的轨迹提示
     */
    private String gestureHint;

    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private int timeout = 60000;

    /**
     * 扩展参数
     */
    @Builder.Default
    private Map<String, Object> extras = new HashMap<>();

    /**
     * 创建图形验证码请求
     */
    public static CaptchaRequest image(String imageBase64) {
        return CaptchaRequest.builder()
                .type(CaptchaType.IMAGE)
                .imageBase64(imageBase64)
                .build();
    }

    /**
     * 创建图形验证码请求（通过URL）
     */
    public static CaptchaRequest imageUrl(String imageUrl) {
        return CaptchaRequest.builder()
                .type(CaptchaType.IMAGE)
                .imageUrl(imageUrl)
                .build();
    }

    /**
     * 创建滑块验证码请求
     */
    public static CaptchaRequest slider(String backgroundImage, String sliderImage) {
        return CaptchaRequest.builder()
                .type(CaptchaType.SLIDER)
                .backgroundImage(backgroundImage)
                .sliderImage(sliderImage)
                .build();
    }

    /**
     * 创建reCAPTCHA请求
     */
    public static CaptchaRequest recaptcha(String siteUrl, String siteKey) {
        return CaptchaRequest.builder()
                .type(CaptchaType.RECAPTCHA)
                .siteUrl(siteUrl)
                .siteKey(siteKey)
                .build();
    }

    /**
     * 创建hCaptcha请求
     */
    public static CaptchaRequest hcaptcha(String siteUrl, String siteKey) {
        return CaptchaRequest.builder()
                .type(CaptchaType.HCAPTCHA)
                .siteUrl(siteUrl)
                .siteKey(siteKey)
                .build();
    }

    /**
     * 创建手势验证码请求
     */
    public static CaptchaRequest gesture(String imageBase64, String hint) {
        return CaptchaRequest.builder()
                .type(CaptchaType.GESTURE)
                .imageBase64(imageBase64)
                .gestureHint(hint)
                .build();
    }

    /**
     * 创建旋转验证码请求
     */
    public static CaptchaRequest rotate(String imageBase64) {
        return CaptchaRequest.builder()
                .type(CaptchaType.ROTATE)
                .imageBase64(imageBase64)
                .build();
    }

    /**
     * 添加扩展参数
     */
    public CaptchaRequest addExtra(String key, Object value) {
        if (this.extras == null) {
            this.extras = new HashMap<>();
        }
        this.extras.put(key, value);
        return this;
    }

    /**
     * 获取扩展参数
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        if (this.extras == null) {
            return null;
        }
        return (T) this.extras.get(key);
    }
}
