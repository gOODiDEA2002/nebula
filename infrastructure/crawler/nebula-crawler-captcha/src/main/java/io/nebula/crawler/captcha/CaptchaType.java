package io.nebula.crawler.captcha;

/**
 * 验证码类型枚举
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public enum CaptchaType {

    /**
     * 图形验证码 - 字符识别（数字、字母、算术）
     */
    IMAGE("image", "图形验证码"),

    /**
     * 滑块验证码 - 拖动滑块至缺口位置
     */
    SLIDER("slider", "滑块验证码"),

    /**
     * 点击验证码 - 点选特定文字/图片
     */
    CLICK("click", "点击验证码"),

    /**
     * 手势验证码 - 九宫格轨迹绘制
     */
    GESTURE("gesture", "手势验证码"),

    /**
     * 旋转验证码 - 旋转图片至正确角度
     */
    ROTATE("rotate", "旋转验证码"),

    /**
     * 短信验证码 - 手机短信验证
     */
    SMS("sms", "短信验证码"),

    /**
     * Google reCAPTCHA
     */
    RECAPTCHA("recaptcha", "Google reCAPTCHA"),

    /**
     * hCaptcha
     */
    HCAPTCHA("hcaptcha", "hCaptcha"),

    /**
     * 未知验证码类型
     */
    UNKNOWN("unknown", "未知验证码");

    private final String code;
    private final String description;

    CaptchaType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取验证码类型
     */
    public static CaptchaType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (CaptchaType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
