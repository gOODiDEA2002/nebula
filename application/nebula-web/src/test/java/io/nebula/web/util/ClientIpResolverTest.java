package io.nebula.web.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户端 IP 解析器测试（CWT-28: XFF 伪造防护语义）
 */
@ExtendWith(MockitoExtension.class)
class ClientIpResolverTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("默认未配置可信代理: 直接用 remoteAddr, 完全不读转发头")
    void noTrustedProxiesUsesRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(new String[0]);
        when(request.getRemoteAddr()).thenReturn("198.51.100.9");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.9");
        verify(request, never()).getHeader("X-Forwarded-For");
        verify(request, never()).getHeader("X-Real-IP");
    }

    @Test
    @DisplayName("remoteAddr 不是可信代理: 忽略伪造的 XFF")
    void untrustedRemoteAddrIgnoresForwardedHeaders() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"10.0.0.0/8"});
        when(request.getRemoteAddr()).thenReturn("198.51.100.9");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.9");
        verify(request, never()).getHeader("X-Forwarded-For");
    }

    @Test
    @DisplayName("来自可信代理: XFF 从右向左取第一个不可信地址(跳过代理链)")
    void trustedProxyResolvesRightmostUntrustedHop() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"127.0.0.1", "10.0.0.0/8"});
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        // 客户端伪造了首段 1.2.3.4; 真实客户端 203.0.113.7; 10.0.0.5 是内层可信代理
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 203.0.113.7, 10.0.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("XFF 整条链路可信(内网互调): 取最左原始地址")
    void allHopsTrustedFallsBackToLeftmost() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"127.0.0.1", "10.0.0.0/8"});
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.3, 10.0.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.3");
    }

    @Test
    @DisplayName("可信代理但无 XFF: 回退 X-Real-IP, 再回退 remoteAddr")
    void trustedProxyFallsBackToRealIpThenRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"127.0.0.1"});
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.8");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.8");

        when(request.getHeader("X-Real-IP")).thenReturn(null);
        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("CIDR 匹配: 边界内命中, 边界外不命中")
    void cidrRangeMatching() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"192.168.1.0/24"});

        when(request.getRemoteAddr()).thenReturn("192.168.1.254");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.9");
        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");

        when(request.getRemoteAddr()).thenReturn("192.168.2.1");
        assertThat(resolver.resolve(request)).isEqualTo("192.168.2.1");
    }

    @Test
    @DisplayName("非法配置条目被忽略, 不影响其余条目")
    void invalidEntriesIgnored() {
        ClientIpResolver resolver = new ClientIpResolver(
                new String[]{"", "  ", "bad/cidr", "300.1.1.1/8", "127.0.0.1"});
        assertThat(resolver.hasTrustedProxies()).isTrue();

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.1");
    }
}
