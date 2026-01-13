package io.nebula.crawler.captcha.detector;

import io.nebula.crawler.captcha.CaptchaType;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * 默认验证码检测器实现
 * 基于图片特征进行简单的类型判断
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class DefaultCaptchaDetector implements CaptchaDetector {

    // 常见滑块验证码的尺寸特征
    private static final int SLIDER_MIN_WIDTH = 200;
    private static final int SLIDER_MAX_HEIGHT = 200;

    // 常见图形验证码的尺寸特征
    private static final int IMAGE_MIN_WIDTH = 60;
    private static final int IMAGE_MAX_WIDTH = 200;
    private static final int IMAGE_MIN_HEIGHT = 20;
    private static final int IMAGE_MAX_HEIGHT = 80;

    @Override
    public CaptchaType detect(String imageBase64) {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return CaptchaType.UNKNOWN;
        }

        try {
            // 移除Base64前缀（如果有）
            String base64Data = imageBase64;
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }

            byte[] imageData = Base64.getDecoder().decode(base64Data);
            return detect(imageData);
        } catch (Exception e) {
            log.warn("验证码类型检测失败: {}", e.getMessage());
            return CaptchaType.UNKNOWN;
        }
    }

    @Override
    public CaptchaType detect(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return CaptchaType.UNKNOWN;
        }

        try {
            // 解析图片尺寸
            int[] dimensions = getImageDimensions(imageData);
            if (dimensions == null) {
                return CaptchaType.IMAGE; // 默认返回图形验证码
            }

            int width = dimensions[0];
            int height = dimensions[1];

            log.debug("图片尺寸: {}x{}", width, height);

            // 根据尺寸判断类型
            // 滑块验证码通常较宽
            if (width >= SLIDER_MIN_WIDTH && height <= SLIDER_MAX_HEIGHT) {
                return CaptchaType.SLIDER;
            }

            // 图形验证码通常较小
            if (width >= IMAGE_MIN_WIDTH && width <= IMAGE_MAX_WIDTH
                    && height >= IMAGE_MIN_HEIGHT && height <= IMAGE_MAX_HEIGHT) {
                return CaptchaType.IMAGE;
            }

            // 默认返回图形验证码
            return CaptchaType.IMAGE;

        } catch (Exception e) {
            log.warn("验证码类型检测失败: {}", e.getMessage());
            return CaptchaType.UNKNOWN;
        }
    }

    /**
     * 获取图片尺寸
     * 支持PNG、JPEG、GIF格式
     */
    private int[] getImageDimensions(byte[] imageData) {
        if (imageData.length < 24) {
            return null;
        }

        // PNG格式
        if (imageData[0] == (byte) 0x89 && imageData[1] == 'P' && imageData[2] == 'N' && imageData[3] == 'G') {
            int width = ((imageData[16] & 0xff) << 24) | ((imageData[17] & 0xff) << 16)
                    | ((imageData[18] & 0xff) << 8) | (imageData[19] & 0xff);
            int height = ((imageData[20] & 0xff) << 24) | ((imageData[21] & 0xff) << 16)
                    | ((imageData[22] & 0xff) << 8) | (imageData[23] & 0xff);
            return new int[]{width, height};
        }

        // JPEG格式
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8) {
            return getJpegDimensions(imageData);
        }

        // GIF格式
        if (imageData[0] == 'G' && imageData[1] == 'I' && imageData[2] == 'F') {
            int width = (imageData[7] & 0xff) << 8 | (imageData[6] & 0xff);
            int height = (imageData[9] & 0xff) << 8 | (imageData[8] & 0xff);
            return new int[]{width, height};
        }

        return null;
    }

    /**
     * 获取JPEG图片尺寸
     */
    private int[] getJpegDimensions(byte[] imageData) {
        int offset = 2;
        while (offset < imageData.length) {
            if (imageData[offset] != (byte) 0xFF) {
                offset++;
                continue;
            }

            int marker = imageData[offset + 1] & 0xFF;

            // SOF0, SOF1, SOF2 标记
            if (marker >= 0xC0 && marker <= 0xC3) {
                int height = ((imageData[offset + 5] & 0xff) << 8) | (imageData[offset + 6] & 0xff);
                int width = ((imageData[offset + 7] & 0xff) << 8) | (imageData[offset + 8] & 0xff);
                return new int[]{width, height};
            }

            // 跳过该段
            if (offset + 3 >= imageData.length) break;
            int segmentLength = ((imageData[offset + 2] & 0xff) << 8) | (imageData[offset + 3] & 0xff);
            offset += segmentLength + 2;
        }

        return null;
    }
}
