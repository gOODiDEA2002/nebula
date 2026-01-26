package io.nebula.crawler.browser.captcha;

import com.microsoft.playwright.Page;
import io.nebula.crawler.captcha.CaptchaVendor;

/**
 * 浏览器验证码处理器接口
 * 
 * 负责完整的验证码处理流程：
 * 1. detect - 检测页面是否存在此类型验证码
 * 2. handle - 完整处理流程（提取图片 -> 识别缺口 -> 执行滑动）
 *
 * @author Nebula Team
 * @since 2.0.2
 */
public interface BrowserCaptchaHandler {

    /**
     * 获取支持的验证码供应商类型
     *
     * @return 验证码供应商类型
     */
    CaptchaVendor getVendor();

    /**
     * 检测页面是否存在此类型验证码
     *
     * @param page Playwright Page 对象
     * @return true 如果检测到此类型验证码
     */
    boolean detect(Page page);

    /**
     * 处理验证码（完整流程）
     * 
     * 内部执行步骤：
     * 1. 提取验证码图片
     * 2. 调用识别服务检测缺口位置
     * 3. 执行滑动/点击等操作
     *
     * @param page Playwright Page 对象
     * @return true 如果处理成功
     */
    boolean handle(Page page);

    /**
     * 处理验证码（支持重试）
     *
     * @param page       Playwright Page 对象
     * @param maxRetries 最大重试次数
     * @return true 如果处理成功
     */
    default boolean handle(Page page, int maxRetries) {
        return handle(page);
    }

    /**
     * 检查处理器是否可用
     *
     * @return true 如果后端服务可用
     */
    boolean isAvailable();

    /**
     * 获取处理器优先级
     * 数值越小优先级越高
     *
     * @return 优先级（默认 100）
     */
    default int getPriority() {
        return 100;
    }
}
