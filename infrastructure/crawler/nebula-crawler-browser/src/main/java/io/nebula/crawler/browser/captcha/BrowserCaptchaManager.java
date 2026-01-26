package io.nebula.crawler.browser.captcha;

import com.microsoft.playwright.Page;
import io.nebula.crawler.captcha.CaptchaVendor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 浏览器验证码管理器
 * 
 * 管理多个验证码处理器，自动检测并处理页面上的验证码
 *
 * @author Nebula Team
 * @since 2.0.2
 */
@Slf4j
public class BrowserCaptchaManager {

    private final List<BrowserCaptchaHandler> handlers;

    public BrowserCaptchaManager() {
        this.handlers = new CopyOnWriteArrayList<>();
    }

    public BrowserCaptchaManager(List<BrowserCaptchaHandler> handlers) {
        this.handlers = new CopyOnWriteArrayList<>(handlers);
        // 按优先级排序
        this.handlers.sort(Comparator.comparingInt(BrowserCaptchaHandler::getPriority));
    }

    /**
     * 注册验证码处理器
     *
     * @param handler 处理器实例
     */
    public void registerHandler(BrowserCaptchaHandler handler) {
        handlers.add(handler);
        handlers.sort(Comparator.comparingInt(BrowserCaptchaHandler::getPriority));
        log.info("注册验证码处理器: vendor={}, priority={}",
                handler.getVendor().getDescription(), handler.getPriority());
    }

    /**
     * 检测页面是否存在验证码
     *
     * @param page Playwright Page 对象
     * @return 检测到的验证码供应商类型，未检测到返回 UNKNOWN
     */
    public CaptchaVendor detectVendor(Page page) {
        for (BrowserCaptchaHandler handler : handlers) {
            if (handler.isAvailable() && handler.detect(page)) {
                log.debug("检测到验证码: {}", handler.getVendor().getDescription());
                return handler.getVendor();
            }
        }
        return CaptchaVendor.UNKNOWN;
    }

    /**
     * 检测页面是否存在验证码
     *
     * @param page Playwright Page 对象
     * @return true 如果检测到验证码
     */
    public boolean hasCaptcha(Page page) {
        return detectVendor(page) != CaptchaVendor.UNKNOWN;
    }

    /**
     * 自动检测并处理验证码
     * 
     * 遍历所有注册的处理器，找到能处理的处理器并执行
     *
     * @param page Playwright Page 对象
     * @return true 如果处理成功或不存在验证码
     */
    public boolean handleCaptcha(Page page) {
        return handleCaptcha(page, 5);
    }

    /**
     * 自动检测并处理验证码（支持重试）
     *
     * @param page       Playwright Page 对象
     * @param maxRetries 最大重试次数
     * @return true 如果处理成功或不存在验证码
     */
    public boolean handleCaptcha(Page page, int maxRetries) {
        Optional<BrowserCaptchaHandler> handlerOpt = findHandler(page);

        if (handlerOpt.isEmpty()) {
            log.debug("未检测到验证码或没有可用的处理器");
            return true; // 没有验证码，视为成功
        }

        BrowserCaptchaHandler handler = handlerOpt.get();
        log.info("使用 {} 处理验证码", handler.getVendor().getDescription());

        boolean result = handler.handle(page, maxRetries);
        if (result) {
            log.info("验证码处理成功: {}", handler.getVendor().getDescription());
        } else {
            log.warn("验证码处理失败: {}", handler.getVendor().getDescription());
        }

        return result;
    }

    /**
     * 使用指定类型的处理器处理验证码
     *
     * @param page   Playwright Page 对象
     * @param vendor 验证码供应商类型
     * @return true 如果处理成功
     */
    public boolean handleCaptcha(Page page, CaptchaVendor vendor) {
        return handleCaptcha(page, vendor, 5);
    }

    /**
     * 使用指定类型的处理器处理验证码（支持重试）
     *
     * @param page       Playwright Page 对象
     * @param vendor     验证码供应商类型
     * @param maxRetries 最大重试次数
     * @return true 如果处理成功
     */
    public boolean handleCaptcha(Page page, CaptchaVendor vendor, int maxRetries) {
        Optional<BrowserCaptchaHandler> handlerOpt = handlers.stream()
                .filter(h -> h.getVendor() == vendor && h.isAvailable())
                .findFirst();

        if (handlerOpt.isEmpty()) {
            log.warn("没有找到 {} 的处理器", vendor.getDescription());
            return false;
        }

        return handlerOpt.get().handle(page, maxRetries);
    }

    /**
     * 查找能处理当前页面验证码的处理器
     */
    private Optional<BrowserCaptchaHandler> findHandler(Page page) {
        return handlers.stream()
                .filter(h -> h.isAvailable() && h.detect(page))
                .findFirst();
    }

    /**
     * 获取已注册的处理器数量
     */
    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * 获取可用的处理器数量
     */
    public int getAvailableHandlerCount() {
        return (int) handlers.stream().filter(BrowserCaptchaHandler::isAvailable).count();
    }
}
