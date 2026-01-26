package io.nebula.crawler.captcha;

/**
 * 验证码供应商类型
 * 
 * 不同供应商的验证码需要不同的处理策略
 *
 * @author Nebula Team
 * @since 2.0.2
 */
public enum CaptchaVendor {

    /**
     * 腾讯验证码 (TCaptcha)
     * 特点：iframe 嵌入，需要从 style 提取图片 URL，需要人类轨迹模拟
     */
    TENCENT("tencent", "腾讯验证码"),

    /**
     * 阿里验证码 (阿里云人机验证)
     */
    ALIBABA("alibaba", "阿里验证码"),

    /**
     * 极验验证码 (GeeTest)
     */
    GEETEST("geetest", "极验验证码"),

    /**
     * 网易易盾验证码
     */
    NETEASE("netease", "网易易盾"),

    /**
     * 顶象验证码
     */
    DINGXIANG("dingxiang", "顶象验证码"),

    /**
     * 通用滑块验证码
     * 特点：简单的滑块拖动，可直接截图获取图片
     */
    GENERIC_SLIDER("generic_slider", "通用滑块验证码"),

    /**
     * 通用图形验证码
     */
    GENERIC_IMAGE("generic_image", "通用图形验证码"),

    /**
     * 未知类型
     */
    UNKNOWN("unknown", "未知验证码");

    private final String code;
    private final String description;

    CaptchaVendor(String code, String description) {
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
     * 根据代码获取枚举
     */
    public static CaptchaVendor fromCode(String code) {
        for (CaptchaVendor vendor : values()) {
            if (vendor.code.equals(code)) {
                return vendor;
            }
        }
        return UNKNOWN;
    }
}
