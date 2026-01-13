package io.nebula.crawler.core.cookie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Cookie数据类
 * <p>
 * 表示HTTP Cookie的基本信息
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cookie {
    
    /**
     * Cookie名称
     */
    private String name;
    
    /**
     * Cookie值
     */
    private String value;
    
    /**
     * 域名
     */
    private String domain;
    
    /**
     * 路径
     */
    @Builder.Default
    private String path = "/";
    
    /**
     * 过期时间
     */
    private Instant expiresAt;
    
    /**
     * 是否仅HTTPS
     */
    @Builder.Default
    private boolean secure = false;
    
    /**
     * 是否HttpOnly
     */
    @Builder.Default
    private boolean httpOnly = false;
    
    /**
     * SameSite属性
     */
    private SameSite sameSite;
    
    /**
     * 创建时间
     */
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    /**
     * SameSite枚举
     */
    public enum SameSite {
        STRICT,
        LAX,
        NONE
    }
    
    /**
     * 检查Cookie是否过期
     *
     * @return true表示已过期
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // Session cookie, 不自动过期
        }
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * 检查Cookie是否适用于指定域名
     *
     * @param targetDomain 目标域名
     * @return true表示适用
     */
    public boolean matchesDomain(String targetDomain) {
        if (domain == null || targetDomain == null) {
            return false;
        }
        
        String normalizedDomain = domain.startsWith(".") ? domain.substring(1) : domain;
        String normalizedTarget = targetDomain.toLowerCase();
        
        return normalizedTarget.equals(normalizedDomain) ||
               normalizedTarget.endsWith("." + normalizedDomain);
    }
    
    /**
     * 检查Cookie是否适用于指定路径
     *
     * @param targetPath 目标路径
     * @return true表示适用
     */
    public boolean matchesPath(String targetPath) {
        if (path == null || targetPath == null) {
            return true;
        }
        return targetPath.startsWith(path);
    }
    
    /**
     * 转换为Set-Cookie头格式
     *
     * @return Cookie字符串
     */
    public String toSetCookieHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        
        if (domain != null) {
            sb.append("; Domain=").append(domain);
        }
        if (path != null) {
            sb.append("; Path=").append(path);
        }
        if (expiresAt != null) {
            sb.append("; Expires=").append(expiresAt.toString());
        }
        if (secure) {
            sb.append("; Secure");
        }
        if (httpOnly) {
            sb.append("; HttpOnly");
        }
        if (sameSite != null) {
            sb.append("; SameSite=").append(sameSite.name());
        }
        
        return sb.toString();
    }
    
    /**
     * 转换为请求Cookie格式
     *
     * @return name=value
     */
    public String toRequestFormat() {
        return name + "=" + value;
    }
    
    /**
     * 从Set-Cookie头解析Cookie
     *
     * @param setCookieHeader Set-Cookie头内容
     * @param defaultDomain   默认域名
     * @return Cookie对象
     */
    public static Cookie parse(String setCookieHeader, String defaultDomain) {
        if (setCookieHeader == null || setCookieHeader.isEmpty()) {
            return null;
        }
        
        String[] parts = setCookieHeader.split(";");
        if (parts.length == 0) {
            return null;
        }
        
        // 解析name=value
        String[] nameValue = parts[0].trim().split("=", 2);
        if (nameValue.length < 2) {
            return null;
        }
        
        Cookie.CookieBuilder builder = Cookie.builder()
            .name(nameValue[0].trim())
            .value(nameValue[1].trim())
            .domain(defaultDomain);
        
        // 解析其他属性
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            String[] attrParts = part.split("=", 2);
            String attrName = attrParts[0].toLowerCase();
            String attrValue = attrParts.length > 1 ? attrParts[1] : "";
            
            switch (attrName) {
                case "domain":
                    builder.domain(attrValue);
                    break;
                case "path":
                    builder.path(attrValue);
                    break;
                case "expires":
                    try {
                        builder.expiresAt(Instant.parse(attrValue));
                    } catch (Exception ignored) {
                        // 忽略解析错误
                    }
                    break;
                case "max-age":
                    try {
                        long maxAge = Long.parseLong(attrValue);
                        builder.expiresAt(Instant.now().plusSeconds(maxAge));
                    } catch (Exception ignored) {
                        // 忽略解析错误
                    }
                    break;
                case "secure":
                    builder.secure(true);
                    break;
                case "httponly":
                    builder.httpOnly(true);
                    break;
                case "samesite":
                    try {
                        builder.sameSite(SameSite.valueOf(attrValue.toUpperCase()));
                    } catch (Exception ignored) {
                        // 忽略解析错误
                    }
                    break;
            }
        }
        
        return builder.build();
    }
}

