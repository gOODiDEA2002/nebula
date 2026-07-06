package io.nebula.security.authentication;

import io.nebula.security.config.SecurityProperties;
import io.nebula.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link JwtAuthenticationFilter} 补齐了 RBAC 链路的填充环节：
 * 有效 token 期间填充 {@link SecurityContext}、请求结束清理、只填充不拦截。
 */
class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final SecurityProperties.Jwt jwtProperties = new SecurityProperties.Jwt();
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, jwtProperties);

    @AfterEach
    void tearDown() {
        SecurityContext.clearAuthentication();
    }

    @Test
    void populatesContextDuringRequestAndClearsAfter() throws Exception {
        when(jwtService.validateAccessToken("valid")).thenReturn("42");
        when(jwtService.getClaim("valid", "username", String.class)).thenReturn("alice");
        when(jwtService.getClaim(eq("valid"), eq("roles"), eq(Object.class))).thenReturn(List.of("ADMIN"));
        when(jwtService.getClaim(eq("valid"), eq("permissions"), eq(Object.class))).thenReturn(List.of("order:create"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");

        AtomicReference<Authentication> during = new AtomicReference<>();
        FilterChain chain = (req, res) -> during.set(SecurityContext.getAuthentication());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // 处理期间已填充
        Authentication auth = during.get();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(42L);
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getRoles()).contains("ADMIN");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("order:create");

        // 请求结束后必须清理，避免线程复用串号
        assertThat(SecurityContext.getAuthentication()).isNull();
    }

    @Test
    void invalidTokenDoesNotPopulateButStillPassesThrough() throws Exception {
        when(jwtService.validateAccessToken(anyString())).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");

        AtomicReference<Authentication> during = new AtomicReference<>();
        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> {
            during.set(SecurityContext.getAuthentication());
            chainCalled.set(true);
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // 只填充不拦截: 未填充上下文, 但请求照常放行
        assertThat(during.get()).isNull();
        assertThat(chainCalled.get()).isTrue();
    }

    @Test
    void noTokenPassesThrough() throws Exception {
        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(SecurityContext.getAuthentication()).isNull();
    }
}
