package io.nebula.crawler.core;

import io.nebula.crawler.core.proxy.Proxy;
import io.nebula.core.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 爬虫响应封装
 * <p>
 * 封装爬取结果，提供便捷的解析方法。
 * 支持HTML解析、JSON解析等多种方式。
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerResponse {
    
    /**
     * 请求ID（与请求关联）
     */
    private String requestId;
    
    /**
     * 请求URL
     */
    private String url;
    
    /**
     * 最终URL（可能经过重定向）
     */
    private String finalUrl;
    
    /**
     * HTTP状态码
     */
    private int statusCode;
    
    /**
     * 响应头
     */
    @Builder.Default
    private Map<String, List<String>> headers = new HashMap<>();
    
    /**
     * 响应内容（字符串形式）
     */
    private String content;
    
    /**
     * 响应字节（二进制形式）
     */
    private byte[] bytes;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 内容编码
     */
    @Builder.Default
    private String charset = "UTF-8";
    
    /**
     * 响应时间(ms)
     */
    private long responseTime;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误异常
     */
    private Throwable exception;
    
    /**
     * 使用的代理
     */
    private Proxy usedProxy;
    
    /**
     * 截图数据（浏览器爬虫）
     */
    private byte[] screenshot;
    
    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> extras = new HashMap<>();
    
    // ========== 静态工厂方法 ==========
    
    /**
     * 创建成功响应
     *
     * @param requestId    请求ID
     * @param url          请求URL
     * @param statusCode   状态码
     * @param content      响应内容
     * @param responseTime 响应时间
     * @return 响应对象
     */
    public static CrawlerResponse success(String requestId, String url,
                                          int statusCode, String content, long responseTime) {
        return CrawlerResponse.builder()
            .requestId(requestId)
            .url(url)
            .statusCode(statusCode)
            .content(content)
            .responseTime(responseTime)
            .success(true)
            .build();
    }
    
    /**
     * 创建失败响应
     *
     * @param requestId    请求ID
     * @param url          请求URL
     * @param errorMessage 错误信息
     * @param exception    异常
     * @return 响应对象
     */
    public static CrawlerResponse failure(String requestId, String url,
                                          String errorMessage, Throwable exception) {
        return CrawlerResponse.builder()
            .requestId(requestId)
            .url(url)
            .success(false)
            .errorMessage(errorMessage)
            .exception(exception)
            .build();
    }
    
    /**
     * 创建失败响应
     *
     * @param requestId    请求ID
     * @param url          请求URL
     * @param errorMessage 错误信息
     * @return 响应对象
     */
    public static CrawlerResponse failure(String requestId, String url, String errorMessage) {
        return failure(requestId, url, errorMessage, null);
    }
    
    // ========== 状态判断方法 ==========
    
    /**
     * 判断是否成功（综合状态码）
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return success && statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * 判断是否为重定向
     *
     * @return 是否重定向
     */
    public boolean isRedirect() {
        return statusCode >= 300 && statusCode < 400;
    }
    
    /**
     * 判断是否为客户端错误
     *
     * @return 是否客户端错误
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * 判断是否为服务端错误
     *
     * @return 是否服务端错误
     */
    public boolean isServerError() {
        return statusCode >= 500;
    }
    
    /**
     * 判断是否有内容
     *
     * @return 是否有内容
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
    
    // ========== 解析方法 ==========
    
    /**
     * 解析为Jsoup Document
     *
     * @return Jsoup Document对象
     * @throws IllegalStateException 如果响应内容为空
     */
    public Document asDocument() {
        if (content == null) {
            throw new IllegalStateException("响应内容为空");
        }
        return Jsoup.parse(content);
    }
    
    /**
     * 解析为指定类型的JSON对象
     *
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 解析后的对象
     * @throws IllegalStateException 如果响应内容为空
     */
    public <T> T asJson(Class<T> clazz) {
        if (content == null) {
            throw new IllegalStateException("响应内容为空");
        }
        return JsonUtils.fromJson(content, clazz);
    }
    
    /**
     * 解析为Map
     *
     * @return Map对象
     * @throws IllegalStateException 如果响应内容为空
     */
    public Map<String, Object> asMap() {
        if (content == null) {
            throw new IllegalStateException("响应内容为空");
        }
        return JsonUtils.toMap(content);
    }
    
    /**
     * 解析为List
     *
     * @param clazz 元素类型
     * @param <T>   泛型类型
     * @return List对象
     * @throws IllegalStateException 如果响应内容为空
     */
    public <T> List<T> asList(Class<T> clazz) {
        if (content == null) {
            throw new IllegalStateException("响应内容为空");
        }
        return JsonUtils.toList(content, clazz);
    }
    
    // ========== 便捷方法 ==========
    
    /**
     * 获取内容字节（如果没有直接存储则从字符串转换）
     *
     * @return 字节数组
     */
    public byte[] getBytes() {
        if (bytes != null) {
            return bytes;
        }
        if (content != null) {
            return content.getBytes(StandardCharsets.UTF_8);
        }
        return new byte[0];
    }
    
    /**
     * 获取响应头（单值）
     *
     * @param name 头名称
     * @return 头值，不存在时返回null
     */
    public String getHeader(String name) {
        if (headers == null) {
            return null;
        }
        List<String> values = headers.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }
    
    /**
     * 获取所有响应头值
     *
     * @param name 头名称
     * @return 头值列表
     */
    public List<String> getHeaders(String name) {
        if (headers == null) {
            return null;
        }
        return headers.get(name);
    }
    
    /**
     * 添加扩展属性
     *
     * @param key   属性键
     * @param value 属性值
     * @return this
     */
    public CrawlerResponse addExtra(String key, Object value) {
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
     * @return 属性值
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
     * @return 属性值
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
     * 获取内容长度
     *
     * @return 内容长度
     */
    public int getContentLength() {
        if (content != null) {
            return content.length();
        }
        if (bytes != null) {
            return bytes.length;
        }
        return 0;
    }
}

