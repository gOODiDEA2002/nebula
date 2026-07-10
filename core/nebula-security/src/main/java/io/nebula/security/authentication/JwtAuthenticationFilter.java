package io.nebula.security.authentication;

import io.nebula.security.config.SecurityProperties;
import io.nebula.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * JWT 认证过滤器。
 * <p>
 * 从请求头解析 JWT，校验通过后将认证信息写入 {@link SecurityContext}，请求结束后清理。
 * 填充链路的缺失正是"RBAC 授权注解事实上不可用"的根因：{@link SecurityContext} 此前无任何生产代码填充，
 * {@code SecurityAspect} 拿到的永远是 null。本过滤器补齐该链路。
 * <p>
 * 设计要点：
 * <ul>
 *   <li><b>只填充、不拦截</b>：无 token / token 无效时不返回 401，仅不填充上下文，请求照常放行；
 *       是否拒绝由 {@code SecurityAspect}(@RequiresAuthentication 等) 决定。这样不会破坏使用自有认证的应用。</li>
 *   <li><b>按需注册</b>：显式开启 {@code nebula.security.jwt.filter.enabled}，或启用
 *       {@code nebula.web.auth.enabled} 时由自动配置注册。</li>
 * </ul>
 * roles/permissions 的 claim 键沿用框架既有约定({@code roles} / {@code permissions})。
 *
 * @author Nebula Framework
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";

    private final JwtService jwtService;
    private final SecurityProperties.Jwt jwtProperties;

    public JwtAuthenticationFilter(JwtService jwtService, SecurityProperties.Jwt jwtProperties) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null) {
                Authentication authentication = buildAuthentication(token);
                if (authentication != null) {
                    SecurityContext.setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // 必须清理，避免线程复用导致的上下文串号
            SecurityContext.clearAuthentication();
        }
    }

    /**
     * 从请求头解析 token（去掉前缀）。
     */
    private String resolveToken(HttpServletRequest request) {
        String headerName = jwtProperties.getHeaderName();
        String header = request.getHeader(headerName == null ? "Authorization" : headerName);
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = jwtProperties.getTokenPrefix();
        if (prefix != null && !prefix.isEmpty() && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        return header.trim();
    }

    /**
     * 校验 token 并构建认证信息；token 无效返回 null（不抛异常，交由切面决定是否拒绝）。
     */
    private Authentication buildAuthentication(String token) {
        String subject;
        try {
            subject = jwtService.validateAccessToken(token);
        } catch (Exception e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return null;
        }
        if (subject == null) {
            return null;
        }

        List<String> permissions = readStringList(token, CLAIM_PERMISSIONS);
        Collection<GrantedAuthority> authorities = new ArrayList<>(permissions.size());
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        UserPrincipal principal = new UserPrincipal(parseUserId(subject), safeStringClaim(token, CLAIM_USERNAME), authorities);
        principal.setRoles(new LinkedHashSet<>(readStringList(token, CLAIM_ROLES)));

        return new JwtAuthenticationToken(token, principal, authorities);
    }

    private Long parseUserId(String subject) {
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            // subject 非数字时不作为 userId，保留 null（username 仍可从 claim 获取）
            return null;
        }
    }

    private String safeStringClaim(String token, String key) {
        try {
            return jwtService.getClaim(token, key, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取字符串集合类 claim，兼容 JSON 数组与逗号分隔字符串两种存储形式。
     */
    private List<String> readStringList(String token, String key) {
        Object raw;
        try {
            raw = jwtService.getClaim(token, key, Object.class);
        } catch (Exception e) {
            return List.of();
        }
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof Collection<?> collection) {
            List<String> result = new ArrayList<>(collection.size());
            for (Object item : collection) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        String text = raw.toString().trim();
        if (text.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
