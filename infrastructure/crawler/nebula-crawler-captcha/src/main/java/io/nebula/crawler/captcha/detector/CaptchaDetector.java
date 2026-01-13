package io.nebula.crawler.captcha.detector;

import io.nebula.crawler.captcha.CaptchaType;

/**
 * 验证码检测器接口
 * 用于自动检测验证码类型
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface CaptchaDetector {

    /**
     * 检测验证码类型
     *
     * @param imageBase64 图片的Base64编码
     * @return 验证码类型
     */
    CaptchaType detect(String imageBase64);

    /**
     * 检测验证码类型
     *
     * @param imageData 图片字节数据
     * @return 验证码类型
     */
    CaptchaType detect(byte[] imageData);
}
