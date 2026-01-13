package io.nebula.crawler.core.cookie;

import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 默认Cookie管理器实现
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class DefaultCookieManager implements CookieManager {

    private static final String SET_COOKIE_HEADER = "Set-Cookie";
    private static final String SET_COOKIE_HEADER_LOWER = "set-cookie";

    private final CookieStore cookieStore;

    public DefaultCookieManager() {
        this(new InMemoryCookieStore());
    }

    public DefaultCookieManager(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    @Override
    public void saveCookies(String domain, Map<String, List<String>> headers) {
        if (headers == null || domain == null) {
            return;
        }

        // 查找Set-Cookie头（不区分大小写）
        List<String> setCookieHeaders = headers.entrySet().stream()
            .filter(e -> SET_COOKIE_HEADER.equalsIgnoreCase(e.getKey()))
            .flatMap(e -> e.getValue().stream())
            .collect(Collectors.toList());

        for (String setCookieValue : setCookieHeaders) {
            Cookie cookie = Cookie.parse(setCookieValue, domain);
            if (cookie != null) {
                cookieStore.add(cookie);
            }
        }

        log.debug("从响应保存Cookie: domain={}, count={}", domain, setCookieHeaders.size());
    }

    @Override
    public String getCookieHeader(String domain, String path) {
        List<Cookie> cookies = cookieStore.get(domain, path);
        
        if (cookies.isEmpty()) {
            return "";
        }

        return cookies.stream()
            .map(Cookie::toRequestFormat)
            .collect(Collectors.joining("; "));
    }

    @Override
    public String getCookieHeaderForUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String domain = parsedUrl.getHost();
            String path = parsedUrl.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            return getCookieHeader(domain, path);
        } catch (MalformedURLException e) {
            log.warn("URL解析失败: {}", url);
            return "";
        }
    }

    @Override
    public void setCookie(Cookie cookie) {
        cookieStore.add(cookie);
    }

    @Override
    public void setCookie(String domain, String name, String value) {
        Cookie cookie = Cookie.builder()
            .domain(domain)
            .name(name)
            .value(value)
            .path("/")
            .build();
        cookieStore.add(cookie);
    }

    @Override
    public String getCookieValue(String domain, String name) {
        Cookie cookie = cookieStore.getCookie(domain, name);
        return cookie != null ? cookie.getValue() : null;
    }

    @Override
    public void removeCookie(String domain, String name) {
        cookieStore.remove(domain, name);
    }

    @Override
    public void clearDomain(String domain) {
        cookieStore.remove(domain);
    }

    @Override
    public void clearAll() {
        cookieStore.clear();
    }

    @Override
    public CookieStore getCookieStore() {
        return cookieStore;
    }
}

