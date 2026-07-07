package io.nebula.web.interceptor;

/**
 * Nebula Web 拦截器执行顺序常量
 *
 * <p>此前各拦截器顺序未显式定义(Auth=-1、Performance=0、其余默认 0 按装配顺序),
 * 导致限流排在认证之后(无法挡匿名洪水冲击认证逻辑)、限流与缓存相对顺序不确定
 * (缓存命中可能绕过限流计数)。统一为:
 * Logging &lt; RateLimit &lt; Auth &lt; Cache &lt; Performance。</p>
 *
 * <ul>
 *   <li>日志最先: 任何请求(含被限流/拒绝的)都留痕</li>
 *   <li>限流先于认证: 匿名洪水在进入认证逻辑前被挡住</li>
 *   <li>缓存后于认证: 未授权请求不消费/填充缓存</li>
 *   <li>性能最后: 统计的是通过全部前置检查的真实业务请求</li>
 * </ul>
 *
 * <p>取值间隔 100, 便于应用在框架拦截器之间插入自定义拦截器。</p>
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
public final class InterceptorOrders {

    /** 请求日志 */
    public static final int REQUEST_LOGGING = 100;

    /** 限流 */
    public static final int RATE_LIMIT = 200;

    /** 认证 */
    public static final int AUTH = 300;

    /** 响应缓存 */
    public static final int RESPONSE_CACHE = 400;

    /** 性能监控 */
    public static final int PERFORMANCE_MONITOR = 500;

    private InterceptorOrders() {
    }
}
