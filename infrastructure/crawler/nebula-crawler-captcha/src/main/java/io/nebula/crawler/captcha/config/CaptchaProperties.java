package io.nebula.crawler.captcha.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 验证码配置属性
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@ConfigurationProperties(prefix = "nebula.crawler.captcha")
public class CaptchaProperties {

    /**
     * 是否启用验证码模块
     */
    private boolean enabled = true;

    /**
     * 是否启用本地OCR
     */
    private boolean localOcrEnabled = true;

    /**
     * 本地OCR引擎类型（tesseract/ddddocr）
     */
    private String ocrEngine = "ddddocr";

    /**
     * ddddocr服务地址
     */
    private String ddddocrUrl = "http://localhost:8866";

    /**
     * OpenCV服务地址
     */
    private String opencvUrl = "http://localhost:8867";

    /**
     * 是否启用本地滑块检测
     */
    private boolean localSliderEnabled = true;

    /**
     * 是否启用本地旋转检测
     */
    private boolean localRotateEnabled = true;

    /**
     * 是否启用本地点击检测
     */
    private boolean localClickEnabled = true;

    /**
     * 验证码最小长度
     */
    private int minLength = 4;

    /**
     * 验证码最大长度
     */
    private int maxLength = 6;

    /**
     * 默认超时时间（毫秒）
     */
    private int defaultTimeout = 60000;

    /**
     * 第三方平台配置
     */
    private List<ProviderConfig> providers;

    /**
     * 第三方平台配置
     */
    @Data
    public static class ProviderConfig {
        /**
         * 平台名称
         */
        private String name;

        /**
         * API Key
         */
        private String apiKey;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 优先级（数字越小优先级越高）
         */
        private int priority = 1;
    }
}
