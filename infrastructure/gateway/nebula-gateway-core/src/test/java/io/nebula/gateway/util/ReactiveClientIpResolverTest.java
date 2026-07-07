package io.nebula.gateway.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ReactiveClientIpResolver 测试
 */
class ReactiveClientIpResolverTest {

    @Test
    void defaultNoTrust_ignoresXff() {
        ReactiveClientIpResolver resolver = new ReactiveClientIpResolver(List.of());
        ServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "1.2.3.4, 10.0.0.1")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.100", 12345))
                .build();

        assertEquals("192.168.1.100", resolver.resolve(request));
    }

    @Test
    void trustedProxy_parsesXffRightToLeft() {
        ReactiveClientIpResolver resolver = new ReactiveClientIpResolver(List.of("10.0.0.1", "10.0.0.2"));
        ServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "1.2.3.4, 10.0.0.2, 10.0.0.1")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 12345))
                .build();

        assertEquals("1.2.3.4", resolver.resolve(request));
    }

    @Test
    void trustedProxyCidr_matchesSubnet() {
        ReactiveClientIpResolver resolver = new ReactiveClientIpResolver(List.of("10.0.0.0/8"));
        ServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "203.0.113.50, 10.1.2.3")
                .remoteAddress(new java.net.InetSocketAddress("10.1.2.3", 12345))
                .build();

        assertEquals("203.0.113.50", resolver.resolve(request));
    }

    @Test
    void untrustedRemoteAddr_ignoresXff() {
        ReactiveClientIpResolver resolver = new ReactiveClientIpResolver(List.of("10.0.0.0/8"));
        ServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "spoofed-ip")
                .remoteAddress(new java.net.InetSocketAddress("203.0.113.1", 12345))
                .build();

        assertEquals("203.0.113.1", resolver.resolve(request));
    }

    @Test
    void noXff_fallsBackToXRealIp() {
        ReactiveClientIpResolver resolver = new ReactiveClientIpResolver(List.of("10.0.0.1"));
        ServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Real-IP", "100.64.0.1")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 12345))
                .build();

        assertEquals("100.64.0.1", resolver.resolve(request));
    }

    @Test
    void noHeaders_usesRemoteAddr() {
        ReactiveClientIpResolver resolver = new ReactiveClientIpResolver(List.of("10.0.0.1"));
        ServerHttpRequest request = MockServerHttpRequest.get("/")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 12345))
                .build();

        assertEquals("10.0.0.1", resolver.resolve(request));
    }

    @Test
    void allXffTrusted_returnsRemoteAddr() {
        ReactiveClientIpResolver resolver = new ReactiveClientIpResolver(List.of("10.0.0.0/8"));
        ServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "10.1.1.1, 10.2.2.2")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 12345))
                .build();

        assertEquals("10.0.0.1", resolver.resolve(request));
    }
}
