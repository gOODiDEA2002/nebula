package io.nebula.crawler.core.cookie;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存Cookie存储实现
 * <p>
 * 使用ConcurrentHashMap存储Cookie，线程安全
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class InMemoryCookieStore implements CookieStore {

    /**
     * 存储结构: domain -> (name -> cookie)
     */
    private final ConcurrentHashMap<String, Map<String, Cookie>> cookies = new ConcurrentHashMap<>();

    @Override
    public void add(Cookie cookie) {
        if (cookie == null || cookie.getDomain() == null || cookie.getName() == null) {
            return;
        }

        String domain = normalizeDomain(cookie.getDomain());
        cookies.computeIfAbsent(domain, k -> new ConcurrentHashMap<>())
               .put(cookie.getName(), cookie);
        
        log.debug("添加Cookie: domain={}, name={}", domain, cookie.getName());
    }

    @Override
    public void addAll(List<Cookie> cookieList) {
        if (cookieList == null) {
            return;
        }
        cookieList.forEach(this::add);
    }

    @Override
    public List<Cookie> get(String domain) {
        if (domain == null) {
            return Collections.emptyList();
        }

        List<Cookie> result = new ArrayList<>();
        String normalizedDomain = normalizeDomain(domain);

        // 精确匹配
        Map<String, Cookie> domainCookies = cookies.get(normalizedDomain);
        if (domainCookies != null) {
            result.addAll(domainCookies.values());
        }

        // 检查父域名Cookie
        cookies.forEach((storedDomain, cookieMap) -> {
            if (!storedDomain.equals(normalizedDomain)) {
                for (Cookie cookie : cookieMap.values()) {
                    if (cookie.matchesDomain(domain)) {
                        result.add(cookie);
                    }
                }
            }
        });

        // 过滤过期Cookie
        return result.stream()
            .filter(c -> !c.isExpired())
            .collect(Collectors.toList());
    }

    @Override
    public List<Cookie> get(String domain, String path) {
        return get(domain).stream()
            .filter(c -> c.matchesPath(path))
            .collect(Collectors.toList());
    }

    @Override
    public Cookie getCookie(String domain, String name) {
        if (domain == null || name == null) {
            return null;
        }

        String normalizedDomain = normalizeDomain(domain);
        Map<String, Cookie> domainCookies = cookies.get(normalizedDomain);
        if (domainCookies != null) {
            Cookie cookie = domainCookies.get(name);
            if (cookie != null && !cookie.isExpired()) {
                return cookie;
            }
        }

        // 检查父域名
        for (Map.Entry<String, Map<String, Cookie>> entry : cookies.entrySet()) {
            if (!entry.getKey().equals(normalizedDomain)) {
                Cookie cookie = entry.getValue().get(name);
                if (cookie != null && cookie.matchesDomain(domain) && !cookie.isExpired()) {
                    return cookie;
                }
            }
        }

        return null;
    }

    @Override
    public boolean remove(String domain, String name) {
        if (domain == null || name == null) {
            return false;
        }

        String normalizedDomain = normalizeDomain(domain);
        Map<String, Cookie> domainCookies = cookies.get(normalizedDomain);
        if (domainCookies != null) {
            Cookie removed = domainCookies.remove(name);
            if (removed != null) {
                log.debug("删除Cookie: domain={}, name={}", domain, name);
                return true;
            }
        }
        return false;
    }

    @Override
    public int remove(String domain) {
        if (domain == null) {
            return 0;
        }

        String normalizedDomain = normalizeDomain(domain);
        Map<String, Cookie> domainCookies = cookies.remove(normalizedDomain);
        int count = domainCookies != null ? domainCookies.size() : 0;
        
        if (count > 0) {
            log.debug("删除域名Cookie: domain={}, count={}", domain, count);
        }
        return count;
    }

    @Override
    public void clear() {
        int count = size();
        cookies.clear();
        log.debug("清空所有Cookie, count={}", count);
    }

    @Override
    public int cleanExpired() {
        int cleanedCount = 0;

        for (Map.Entry<String, Map<String, Cookie>> entry : cookies.entrySet()) {
            Map<String, Cookie> domainCookies = entry.getValue();
            Iterator<Map.Entry<String, Cookie>> iterator = domainCookies.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Cookie cookie = iterator.next().getValue();
                if (cookie.isExpired()) {
                    iterator.remove();
                    cleanedCount++;
                }
            }

            // 如果域名下没有Cookie了，移除整个域名
            if (domainCookies.isEmpty()) {
                cookies.remove(entry.getKey());
            }
        }

        if (cleanedCount > 0) {
            log.debug("清理过期Cookie, count={}", cleanedCount);
        }
        return cleanedCount;
    }

    @Override
    public int size() {
        return cookies.values().stream()
            .mapToInt(Map::size)
            .sum();
    }

    @Override
    public List<String> getDomains() {
        return new ArrayList<>(cookies.keySet());
    }

    /**
     * 规范化域名
     */
    private String normalizeDomain(String domain) {
        if (domain == null) {
            return "";
        }
        String normalized = domain.toLowerCase();
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}

