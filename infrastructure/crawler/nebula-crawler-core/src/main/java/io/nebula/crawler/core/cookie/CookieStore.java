package io.nebula.crawler.core.cookie;

import java.util.List;

/**
 * Cookie存储接口
 * <p>
 * 定义Cookie的存储和获取操作
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface CookieStore {

    /**
     * 存储Cookie
     *
     * @param cookie Cookie对象
     */
    void add(Cookie cookie);

    /**
     * 批量存储Cookie
     *
     * @param cookies Cookie列表
     */
    void addAll(List<Cookie> cookies);

    /**
     * 获取指定域名的所有Cookie
     *
     * @param domain 域名
     * @return Cookie列表
     */
    List<Cookie> get(String domain);

    /**
     * 获取指定域名和路径的Cookie
     *
     * @param domain 域名
     * @param path   路径
     * @return Cookie列表
     */
    List<Cookie> get(String domain, String path);

    /**
     * 获取指定Cookie
     *
     * @param domain 域名
     * @param name   Cookie名称
     * @return Cookie，不存在返回null
     */
    Cookie getCookie(String domain, String name);

    /**
     * 删除指定Cookie
     *
     * @param domain 域名
     * @param name   Cookie名称
     * @return true表示删除成功
     */
    boolean remove(String domain, String name);

    /**
     * 删除指定域名的所有Cookie
     *
     * @param domain 域名
     * @return 删除的Cookie数量
     */
    int remove(String domain);

    /**
     * 清空所有Cookie
     */
    void clear();

    /**
     * 清理过期Cookie
     *
     * @return 清理的Cookie数量
     */
    int cleanExpired();

    /**
     * 获取所有Cookie数量
     *
     * @return Cookie数量
     */
    int size();

    /**
     * 获取所有域名
     *
     * @return 域名列表
     */
    List<String> getDomains();
}

