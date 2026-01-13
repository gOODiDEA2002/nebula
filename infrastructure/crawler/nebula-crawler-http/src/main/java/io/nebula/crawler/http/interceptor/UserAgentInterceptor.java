package io.nebula.crawler.http.interceptor;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * User-Agent轮换拦截器
 * <p>
 * 为每个请求随机选择一个User-Agent，模拟不同浏览器访问
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public class UserAgentInterceptor implements Interceptor {

    private static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * 默认User-Agent列表
     */
    private static final List<String> DEFAULT_USER_AGENTS = List.of(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    );

    private final List<String> userAgents;
    private final Random random;

    /**
     * 使用默认User-Agent列表
     */
    public UserAgentInterceptor() {
        this(DEFAULT_USER_AGENTS);
    }

    /**
     * 使用自定义User-Agent列表
     *
     * @param userAgents User-Agent列表
     */
    public UserAgentInterceptor(List<String> userAgents) {
        this.userAgents = userAgents != null && !userAgents.isEmpty() ? 
                          userAgents : DEFAULT_USER_AGENTS;
        this.random = new Random();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // 如果请求已经有User-Agent，则不覆盖
        if (originalRequest.header(USER_AGENT_HEADER) != null) {
            return chain.proceed(originalRequest);
        }

        // 随机选择一个User-Agent
        String userAgent = getRandomUserAgent();
        Request newRequest = originalRequest.newBuilder()
            .header(USER_AGENT_HEADER, userAgent)
            .build();

        return chain.proceed(newRequest);
    }

    /**
     * 获取随机User-Agent
     *
     * @return User-Agent字符串
     */
    public String getRandomUserAgent() {
        return userAgents.get(random.nextInt(userAgents.size()));
    }

    /**
     * 获取User-Agent列表
     *
     * @return User-Agent列表（不可修改）
     */
    public List<String> getUserAgents() {
        return List.copyOf(userAgents);
    }
}

