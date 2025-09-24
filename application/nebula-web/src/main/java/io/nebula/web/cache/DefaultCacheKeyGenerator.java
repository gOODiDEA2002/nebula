package io.nebula.web.cache;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.TreeMap;

/**
 * 默认缓存键生成器
 * 基于请求的方法、URI、查询参数和特定请求头生成唯一键
 */
public class DefaultCacheKeyGenerator implements CacheKeyGenerator {
    
    private final String keyPrefix;
    private final String[] includeHeaders;
    
    public DefaultCacheKeyGenerator(String keyPrefix) {
        this(keyPrefix, new String[]{"Accept", "Accept-Language", "Accept-Encoding"});
    }
    
    public DefaultCacheKeyGenerator(String keyPrefix, String[] includeHeaders) {
        this.keyPrefix = keyPrefix != null ? keyPrefix : "cache:";
        this.includeHeaders = includeHeaders != null ? includeHeaders : new String[0];
    }
    
    @Override
    public String generateKey(HttpServletRequest request) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 添加前缀
        keyBuilder.append(keyPrefix);
        
        // 添加请求方法和URI
        keyBuilder.append(request.getMethod()).append(":");
        keyBuilder.append(request.getRequestURI());
        
        // 添加查询参数（按字母顺序排序以确保一致性）
        String queryString = buildSortedQueryString(request);
        if (StringUtils.hasText(queryString)) {
            keyBuilder.append("?").append(queryString);
        }
        
        // 添加特定的请求头
        if (includeHeaders.length > 0) {
            TreeMap<String, String> headers = new TreeMap<>();
            for (String headerName : includeHeaders) {
                String headerValue = request.getHeader(headerName);
                if (StringUtils.hasText(headerValue)) {
                    headers.put(headerName.toLowerCase(), headerValue);
                }
            }
            
            if (!headers.isEmpty()) {
                keyBuilder.append("#headers:");
                headers.forEach((name, value) -> 
                    keyBuilder.append(name).append("=").append(value).append(";"));
            }
        }
        
        // 生成哈希值以保持键的合理长度
        String fullKey = keyBuilder.toString();
        return keyPrefix + hashString(fullKey);
    }
    
    /**
     * 构建排序后的查询字符串
     */
    private String buildSortedQueryString(HttpServletRequest request) {
        TreeMap<String, String[]> sortedParams = new TreeMap<>();
        
        // 收集所有参数
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues != null && paramValues.length > 0) {
                sortedParams.put(paramName, paramValues);
            }
        }
        
        if (sortedParams.isEmpty()) {
            return "";
        }
        
        StringBuilder queryBuilder = new StringBuilder();
        sortedParams.forEach((name, values) -> {
            for (String value : values) {
                if (queryBuilder.length() > 0) {
                    queryBuilder.append("&");
                }
                queryBuilder.append(name).append("=").append(value != null ? value : "");
            }
        });
        
        return queryBuilder.toString();
    }
    
    /**
     * 生成字符串的哈希值
     */
    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果MD5不可用，使用hashCode作为fallback
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }
}
