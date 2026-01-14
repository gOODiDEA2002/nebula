package io.nebula.crawler.browser.captcha;

import lombok.Builder;
import lombok.Data;

/**
 * 验证码检测结果
 * 包含验证码类型、是否可自动处理、建议的处理策略等信息
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@Builder
public class CaptchaDetectionResult {

    /**
     * 验证码类型
     */
    private CaptchaVendor vendor;

    /**
     * 是否检测到验证码
     */
    private boolean detected;

    /**
     * 是否可以自动处理
     */
    private boolean canAutoHandle;

    /**
     * 建议的处理策略
     */
    private HandleStrategy suggestedStrategy;

    /**
     * 不可处理的原因
     */
    private String reason;

    /**
     * 额外信息
     */
    private String extraInfo;

    /**
     * 验证码供应商
     */
    public enum CaptchaVendor {
        /**
         * 未知类型
         */
        UNKNOWN("未知验证码"),

        /**
         * 简单滑块验证码（可自动处理）
         */
        SIMPLE_SLIDER("简单滑块验证码"),

        /**
         * 简单图片验证码（可自动处理）
         */
        SIMPLE_IMAGE("简单图片验证码"),

        /**
         * 腾讯验证码（TCaptcha）
         */
        TENCENT_TCAPTCHA("腾讯验证码"),

        /**
         * 极验验证码（GeeTest）
         */
        GEETEST("极验验证码"),

        /**
         * 网易易盾
         */
        NETEASE_YIDUN("网易易盾"),

        /**
         * 阿里云验证码
         */
        ALIYUN("阿里云验证码"),

        /**
         * Google reCAPTCHA
         */
        RECAPTCHA("Google reCAPTCHA"),

        /**
         * hCaptcha
         */
        HCAPTCHA("hCaptcha"),

        /**
         * 其他商业验证码
         */
        OTHER_COMMERCIAL("其他商业验证码");

        private final String description;

        CaptchaVendor(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 处理策略
     */
    public enum HandleStrategy {
        /**
         * 自动处理 - 使用本地算法
         */
        AUTO_LOCAL("自动处理（本地算法）"),

        /**
         * 第三方平台 - 使用打码平台
         */
        THIRD_PARTY_PLATFORM("第三方打码平台"),

        /**
         * 人工介入 - 需要人工处理
         */
        MANUAL_INTERVENTION("需要人工介入"),

        /**
         * 重试 - 刷新验证码重试
         */
        RETRY("刷新重试"),

        /**
         * 跳过 - 使用备用方案（如手机验证码）
         */
        SKIP_USE_ALTERNATIVE("跳过，使用备用方案"),

        /**
         * 延迟 - 等待一段时间后重试
         */
        DELAY_AND_RETRY("延迟后重试"),

        /**
         * 放弃 - 标记为失败
         */
        GIVE_UP("放弃处理");

        private final String description;

        HandleStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 无验证码
     */
    public static CaptchaDetectionResult none() {
        return CaptchaDetectionResult.builder()
                .detected(false)
                .canAutoHandle(true)
                .build();
    }

    /**
     * 可自动处理的验证码
     */
    public static CaptchaDetectionResult autoHandleable(CaptchaVendor vendor) {
        return CaptchaDetectionResult.builder()
                .vendor(vendor)
                .detected(true)
                .canAutoHandle(true)
                .suggestedStrategy(HandleStrategy.AUTO_LOCAL)
                .build();
    }

    /**
     * 需要第三方平台处理的验证码
     */
    public static CaptchaDetectionResult needThirdParty(CaptchaVendor vendor, String reason) {
        return CaptchaDetectionResult.builder()
                .vendor(vendor)
                .detected(true)
                .canAutoHandle(false)
                .suggestedStrategy(HandleStrategy.THIRD_PARTY_PLATFORM)
                .reason(reason)
                .build();
    }

    /**
     * 需要人工介入的验证码
     */
    public static CaptchaDetectionResult needManual(CaptchaVendor vendor, String reason) {
        return CaptchaDetectionResult.builder()
                .vendor(vendor)
                .detected(true)
                .canAutoHandle(false)
                .suggestedStrategy(HandleStrategy.MANUAL_INTERVENTION)
                .reason(reason)
                .build();
    }
}
