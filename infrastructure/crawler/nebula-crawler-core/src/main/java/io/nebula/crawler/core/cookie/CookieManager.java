package io.nebula.crawler.core.cookie;

import java.util.List;
import java.util.Map;

/**
 * Cookie管理器接口
 * <p>
 * 负责Cookie的生命周期管理，包括从响应中提取Cookie并应用到请求
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface CookieManager {

    /**
     * 从响应头中提取并存储Cookie
     *
     * @param domain  请求域名
     * @param headers 响应头
     */
    void saveCookies(String domain, Map<String, List<String>> headers);

    /**
     * 获取请求头中的Cookie字符串
     *
     * @param domain 目标域名
     * @param path   目标路径
     * @return Cookie头值，格式: name1=value1; name2=value2
     */
    String getCookieHeader(String domain, String path);

    /**
     * 获取请求头中的Cookie字符串
     *
     * @param url 完整URL
     * @return Cookie头值
     */
    String getCookieHeaderForUrl(String url);

    /**
     * 手动设置Cookie
     *
     * @param cookie Cookie对象
     */
    void setCookie(Cookie cookie);

    /**
     * 手动设置Cookie
     *
     * @param domain 域名
     * @param name   Cookie名称
     * @param value  Cookie值
     */
    void setCookie(String domain, String name, String value);

    /**
     * 获取Cookie值
     *
     * @param domain 域名
     * @param name   Cookie名称
     * @return Cookie值，不存在返回null
     */
    String getCookieValue(String domain, String name);

    /**
     * 删除Cookie
     *
     * @param domain 域名
     * @param name   Cookie名称
     */
    void removeCookie(String domain, String name);

    /**
     * 清空指定域名的Cookie
     *
     * @param domain 域名
     */
    void clearDomain(String domain);

    /**
     * 清空所有Cookie
     */
    void clearAll();

    /**
     * 获取底层Cookie存储
     *
     * @return CookieStore实例
     */
    CookieStore getCookieStore();
}

