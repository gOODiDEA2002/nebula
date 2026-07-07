package io.nebula.web.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 拦截器顺序契约测试
 *
 * <p>固化 CWT-18 修复的顺序语义, 防止后续无意改乱:
 * Logging &lt; RateLimit &lt; Auth &lt; Cache &lt; Performance。</p>
 */
class InterceptorOrdersTest {

    @Test
    @DisplayName("限流必须先于认证: 匿名洪水在认证之前被挡")
    void rateLimitBeforeAuth() {
        assertTrue(InterceptorOrders.RATE_LIMIT < InterceptorOrders.AUTH,
                "限流拦截器必须先于认证拦截器执行");
    }

    @Test
    @DisplayName("缓存必须后于认证且先于性能: 未授权请求不进缓存, 缓存命中仍计入限流")
    void cacheBetweenAuthAndPerformance() {
        assertTrue(InterceptorOrders.AUTH < InterceptorOrders.RESPONSE_CACHE,
                "响应缓存必须后于认证拦截器");
        assertTrue(InterceptorOrders.RESPONSE_CACHE < InterceptorOrders.PERFORMANCE_MONITOR,
                "性能监控必须最后执行");
        assertTrue(InterceptorOrders.RATE_LIMIT < InterceptorOrders.RESPONSE_CACHE,
                "缓存命中不得绕过限流计数");
    }

    @Test
    @DisplayName("日志最先: 被限流/拒绝的请求也要留痕")
    void loggingFirst() {
        assertTrue(InterceptorOrders.REQUEST_LOGGING < InterceptorOrders.RATE_LIMIT,
                "请求日志拦截器必须最先执行");
    }
}
