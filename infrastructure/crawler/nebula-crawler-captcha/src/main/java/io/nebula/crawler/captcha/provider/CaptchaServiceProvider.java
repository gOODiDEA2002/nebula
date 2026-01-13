package io.nebula.crawler.captcha.provider;

import io.nebula.crawler.captcha.CaptchaResult;
import io.nebula.crawler.captcha.CaptchaType;
import io.nebula.crawler.captcha.exception.CaptchaException;

/**
 * 第三方打码平台接口
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface CaptchaServiceProvider {

    /**
     * 获取平台名称
     *
     * @return 平台名称
     */
    String getName();

    /**
     * 是否支持指定验证码类型
     *
     * @param type 验证码类型
     * @return true如果支持
     */
    boolean supportsType(CaptchaType type);

    /**
     * 识别图形验证码
     *
     * @param imageData 图片数据
     * @param timeout   超时时间（毫秒）
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    CaptchaResult solveImage(byte[] imageData, int timeout) throws CaptchaException;

    /**
     * 识别滑块验证码
     *
     * @param backgroundBase64 背景图Base64
     * @param sliderBase64     滑块图Base64
     * @param timeout          超时时间（毫秒）
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    CaptchaResult solveSlider(String backgroundBase64, String sliderBase64, int timeout) throws CaptchaException;

    /**
     * 识别点击验证码
     *
     * @param imageBase64 图片Base64
     * @param hint        提示信息
     * @param timeout     超时时间（毫秒）
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    CaptchaResult solveClick(String imageBase64, String hint, int timeout) throws CaptchaException;

    /**
     * 识别手势验证码
     *
     * @param imageBase64 图片Base64
     * @param hint        提示信息
     * @param timeout     超时时间（毫秒）
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    CaptchaResult solveGesture(String imageBase64, String hint, int timeout) throws CaptchaException;

    /**
     * 识别reCAPTCHA
     *
     * @param siteUrl 网站URL
     * @param siteKey 站点密钥
     * @param timeout 超时时间（毫秒）
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    CaptchaResult solveRecaptcha(String siteUrl, String siteKey, int timeout) throws CaptchaException;

    /**
     * 识别hCaptcha
     *
     * @param siteUrl 网站URL
     * @param siteKey 站点密钥
     * @param timeout 超时时间（毫秒）
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    default CaptchaResult solveHcaptcha(String siteUrl, String siteKey, int timeout) throws CaptchaException {
        throw new CaptchaException("不支持hCaptcha识别");
    }

    /**
     * 报告识别结果
     *
     * @param taskId  任务ID
     * @param success 是否成功
     */
    void reportResult(String taskId, boolean success);

    /**
     * 检查服务是否可用
     *
     * @return true如果可用
     */
    boolean isAvailable();

    /**
     * 获取账户余额
     *
     * @return 余额
     */
    double getBalance();
}
