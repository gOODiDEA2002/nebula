package io.nebula.crawler.captcha;

import io.nebula.crawler.captcha.exception.CaptchaException;

import java.util.concurrent.CompletableFuture;

/**
 * 验证码解决器接口
 * 定义验证码识别的通用能力
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface CaptchaSolver {

    /**
     * 获取解决器名称
     *
     * @return 解决器名称
     */
    String getName();

    /**
     * 获取支持的验证码类型
     *
     * @return 验证码类型
     */
    CaptchaType getSupportedType();

    /**
     * 解决验证码
     *
     * @param request 验证码请求
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    CaptchaResult solve(CaptchaRequest request) throws CaptchaException;

    /**
     * 异步解决验证码
     *
     * @param request 验证码请求
     * @return 识别结果的Future
     */
    default CompletableFuture<CaptchaResult> solveAsync(CaptchaRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return solve(request);
            } catch (CaptchaException e) {
                return CaptchaResult.fail(e.getMessage());
            }
        });
    }

    /**
     * 报告识别结果（用于反馈优化）
     *
     * @param taskId  任务ID
     * @param success 是否成功
     */
    default void reportResult(String taskId, boolean success) {
        // 默认空实现
    }

    /**
     * 检查服务是否可用
     *
     * @return true如果可用
     */
    boolean isAvailable();

    /**
     * 获取优先级（数字越小优先级越高）
     *
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
}
