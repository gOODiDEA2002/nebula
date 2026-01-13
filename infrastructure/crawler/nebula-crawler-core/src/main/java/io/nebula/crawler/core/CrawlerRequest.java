package io.nebula.crawler.core;

import io.nebula.crawler.core.proxy.Proxy;
import io.nebula.core.common.util.IdGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 爬虫请求封装
 * <p>
 * 统一封装各种爬取参数，支持HTTP和浏览器爬虫。
 * 使用Builder模式构建，支持链式调用。
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerRequest {
    
    /**
     * 请求ID（用于追踪和日志关联）
     */
    @Builder.Default
    private String requestId = IdGenerator.uuid();
    
    /**
     * 目标URL
     */
    private String url;
    
    /**
     * HTTP方法
     */
    @Builder.Default
    private HttpMethod method = HttpMethod.GET;
    
    /**
     * 请求头
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    
    /**
     * URL参数
     */
    @Builder.Default
    private Map<String, String> params = new HashMap<>();
    
    /**
     * 请求体（POST/PUT时使用）
     */
    private String body;
    
    /**
     * 请求体类型
     */
    @Builder.Default
    private String contentType = "application/json";
    
    /**
     * 指定使用的代理（为null则由引擎自动选择）
     */
    private Proxy proxy;
    
    /**
     * 连接超时(ms)
     */
    @Builder.Default
    private int connectTimeout = 30000;
    
    /**
     * 读取超时(ms)
     */
    @Builder.Default
    private int readTimeout = 60000;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private int retryCount = 3;
    
    /**
     * 重试间隔(ms)
     */
    @Builder.Default
    private int retryInterval = 1000;
    
    /**
     * 是否需要渲染JavaScript（浏览器爬虫使用）
     */
    @Builder.Default
    private boolean renderJs = false;
    
    /**
     * 等待选择器（浏览器爬虫使用，等待元素出现）
     */
    private String waitSelector;
    
    /**
     * 等待超时(ms)（浏览器爬虫使用）
     */
    @Builder.Default
    private int waitTimeout = 30000;
    
    /**
     * 页面加载等待状态（浏览器爬虫使用）
     * 可选值：commit, domcontentloaded, load, networkidle
     * 默认使用commit以获得最快响应
     */
    @Builder.Default
    private String waitUntil = "commit";
    
    /**
     * 是否截图（浏览器爬虫使用）
     */
    @Builder.Default
    private boolean screenshot = false;
    
    /**
     * 是否跟随重定向
     */
    @Builder.Default
    private boolean followRedirects = true;
    
    /**
     * 扩展属性（用于传递自定义参数）
     */
    @Builder.Default
    private Map<String, Object> extras = new HashMap<>();
    
    // ========== 静态工厂方法 ==========
    
    /**
     * 创建简单GET请求
     *
     * @param url 目标URL
     * @return 请求对象
     */
    public static CrawlerRequest get(String url) {
        return CrawlerRequest.builder()
            .url(url)
            .method(HttpMethod.GET)
            .build();
    }
    
    /**
     * 创建带参数的GET请求
     *
     * @param url    目标URL
     * @param params URL参数
     * @return 请求对象
     */
    public static CrawlerRequest get(String url, Map<String, String> params) {
        return CrawlerRequest.builder()
            .url(url)
            .method(HttpMethod.GET)
            .params(params != null ? params : new HashMap<>())
            .build();
    }
    
    /**
     * 创建POST请求
     *
     * @param url  目标URL
     * @param body 请求体
     * @return 请求对象
     */
    public static CrawlerRequest post(String url, String body) {
        return CrawlerRequest.builder()
            .url(url)
            .method(HttpMethod.POST)
            .body(body)
            .build();
    }
    
    /**
     * 创建POST JSON请求
     *
     * @param url  目标URL
     * @param body 请求体
     * @return 请求对象
     */
    public static CrawlerRequest postJson(String url, String body) {
        return CrawlerRequest.builder()
            .url(url)
            .method(HttpMethod.POST)
            .body(body)
            .contentType("application/json")
            .build();
    }
    
    /**
     * 创建需要JS渲染的请求
     *
     * @param url 目标URL
     * @return 请求对象
     */
    public static CrawlerRequest renderPage(String url) {
        return CrawlerRequest.builder()
            .url(url)
            .method(HttpMethod.GET)
            .renderJs(true)
            .build();
    }
    
    /**
     * 创建需要JS渲染且等待元素的请求
     *
     * @param url          目标URL
     * @param waitSelector 等待的CSS选择器
     * @return 请求对象
     */
    public static CrawlerRequest renderPage(String url, String waitSelector) {
        return CrawlerRequest.builder()
            .url(url)
            .method(HttpMethod.GET)
            .renderJs(true)
            .waitSelector(waitSelector)
            .build();
    }
    
    // ========== 便捷方法 ==========
    
    /**
     * 添加请求头
     *
     * @param name  头名称
     * @param value 头值
     * @return this
     */
    public CrawlerRequest addHeader(String name, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(name, value);
        return this;
    }
    
    /**
     * 添加多个请求头
     *
     * @param headers 请求头Map
     * @return this
     */
    public CrawlerRequest addHeaders(Map<String, String> headers) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.putAll(headers);
        return this;
    }
    
    /**
     * 添加URL参数
     *
     * @param name  参数名
     * @param value 参数值
     * @return this
     */
    public CrawlerRequest addParam(String name, String value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.put(name, value);
        return this;
    }
    
    /**
     * 添加多个URL参数
     *
     * @param params 参数Map
     * @return this
     */
    public CrawlerRequest addParams(Map<String, String> params) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.putAll(params);
        return this;
    }
    
    /**
     * 添加扩展属性
     *
     * @param key   属性键
     * @param value 属性值
     * @return this
     */
    public CrawlerRequest addExtra(String key, Object value) {
        if (this.extras == null) {
            this.extras = new HashMap<>();
        }
        this.extras.put(key, value);
        return this;
    }
    
    /**
     * 获取扩展属性
     *
     * @param key 属性键
     * @param <T> 值类型
     * @return 属性值，不存在时返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        return extras != null ? (T) extras.get(key) : null;
    }
    
    /**
     * 获取扩展属性（带默认值）
     *
     * @param key          属性键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 属性值，不存在时返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key, T defaultValue) {
        if (extras == null) {
            return defaultValue;
        }
        T value = (T) extras.get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 构建完整URL（包含参数）
     *
     * @return 完整URL
     */
    public String buildFullUrl() {
        if (params == null || params.isEmpty()) {
            return url;
        }
        
        StringBuilder sb = new StringBuilder(url);
        sb.append(url.contains("?") ? "&" : "?");
        
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        return sb.toString();
    }
}

