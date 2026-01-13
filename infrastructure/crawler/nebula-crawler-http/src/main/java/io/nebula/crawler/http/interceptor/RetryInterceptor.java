package io.nebula.crawler.http.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Set;

/**
 * 重试拦截器
 * <p>
 * 当请求失败时自动重试，支持指数退避策略
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class RetryInterceptor implements Interceptor {

    /**
     * 默认可重试的状态码
     */
    private static final Set<Integer> DEFAULT_RETRY_STATUS_CODES = Set.of(
        408, // Request Timeout
        429, // Too Many Requests
        500, // Internal Server Error
        502, // Bad Gateway
        503, // Service Unavailable
        504  // Gateway Timeout
    );

    private final int maxRetries;
    private final long baseDelayMs;
    private final double multiplier;
    private final long maxDelayMs;
    private final Set<Integer> retryStatusCodes;

    /**
     * 使用默认配置创建重试拦截器
     * 默认: 3次重试，1秒基础延迟，2倍指数退避，最大30秒
     */
    public RetryInterceptor() {
        this(3, 1000, 2.0, 30000);
    }

    /**
     * 创建重试拦截器
     *
     * @param maxRetries   最大重试次数
     * @param baseDelayMs  基础延迟时间(毫秒)
     * @param multiplier   延迟倍增系数
     * @param maxDelayMs   最大延迟时间(毫秒)
     */
    public RetryInterceptor(int maxRetries, long baseDelayMs, double multiplier, long maxDelayMs) {
        this(maxRetries, baseDelayMs, multiplier, maxDelayMs, DEFAULT_RETRY_STATUS_CODES);
    }

    /**
     * 创建重试拦截器（完整参数）
     *
     * @param maxRetries       最大重试次数
     * @param baseDelayMs      基础延迟时间(毫秒)
     * @param multiplier       延迟倍增系数
     * @param maxDelayMs       最大延迟时间(毫秒)
     * @param retryStatusCodes 需要重试的状态码
     */
    public RetryInterceptor(int maxRetries, long baseDelayMs, double multiplier, 
                           long maxDelayMs, Set<Integer> retryStatusCodes) {
        this.maxRetries = Math.max(0, maxRetries);
        this.baseDelayMs = Math.max(0, baseDelayMs);
        this.multiplier = Math.max(1.0, multiplier);
        this.maxDelayMs = Math.max(baseDelayMs, maxDelayMs);
        this.retryStatusCodes = retryStatusCodes != null ? retryStatusCodes : DEFAULT_RETRY_STATUS_CODES;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // 如果不是第一次尝试，先关闭之前的响应
                if (response != null) {
                    response.close();
                }

                response = chain.proceed(request);

                // 检查是否需要重试
                if (shouldRetry(response, attempt)) {
                    long delay = calculateDelay(attempt);
                    log.warn("请求需要重试: url={}, statusCode={}, attempt={}/{}, delay={}ms",
                        request.url(), response.code(), attempt + 1, maxRetries + 1, delay);
                    
                    sleep(delay);
                    continue;
                }

                return response;

            } catch (IOException e) {
                lastException = e;
                
                if (attempt < maxRetries) {
                    long delay = calculateDelay(attempt);
                    log.warn("请求异常，准备重试: url={}, error={}, attempt={}/{}, delay={}ms",
                        request.url(), e.getMessage(), attempt + 1, maxRetries + 1, delay);
                    
                    sleep(delay);
                } else {
                    log.error("请求失败（已用尽重试次数）: url={}, error={}", 
                        request.url(), e.getMessage());
                }
            }
        }

        // 如果所有重试都失败，抛出最后的异常
        if (lastException != null) {
            throw lastException;
        }

        // 这种情况不应该发生，但为了编译器满意
        return response;
    }

    /**
     * 判断是否应该重试
     *
     * @param response 响应
     * @param attempt  当前尝试次数
     * @return true表示应该重试
     */
    private boolean shouldRetry(Response response, int attempt) {
        if (attempt >= maxRetries) {
            return false;
        }
        return retryStatusCodes.contains(response.code());
    }

    /**
     * 计算延迟时间（指数退避）
     *
     * @param attempt 当前尝试次数
     * @return 延迟时间(毫秒)
     */
    private long calculateDelay(int attempt) {
        double delay = baseDelayMs * Math.pow(multiplier, attempt);
        // 添加随机抖动（最多20%）
        double jitter = delay * 0.2 * Math.random();
        long finalDelay = (long) (delay + jitter);
        return Math.min(finalDelay, maxDelayMs);
    }

    /**
     * 休眠
     *
     * @param millis 毫秒数
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 获取基础延迟
     */
    public long getBaseDelayMs() {
        return baseDelayMs;
    }
}

