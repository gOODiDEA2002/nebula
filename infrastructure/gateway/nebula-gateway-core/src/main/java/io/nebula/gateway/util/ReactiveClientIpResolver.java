package io.nebula.gateway.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * WebFlux 环境下的客户端 IP 解析器
 * <p>
 * 安全语义与 nebula-web 的 ClientIpResolver 一致：
 * 默认不信任 XFF（直接用 remoteAddr）；仅当 remoteAddr 命中可信代理列表时才解析 XFF，
 * 并从右向左取第一个不可信地址（右侧由代理写入不可伪造，左侧可被客户端伪造）。
 *
 * @since 2.0.1
 */
@Slf4j
public class ReactiveClientIpResolver {

    private final List<String> trustedProxies;

    public ReactiveClientIpResolver(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies != null ? trustedProxies : Collections.emptyList();
    }

    /**
     * 解析客户端真实 IP
     */
    public String resolve(ServerHttpRequest request) {
        String remoteAddr = extractRemoteAddr(request);

        if (trustedProxies.isEmpty() || !isTrusted(remoteAddr)) {
            return remoteAddr;
        }

        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return parseXff(xff, remoteAddr);
        }

        String xri = request.getHeaders().getFirst("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri.trim();
        }

        return remoteAddr;
    }

    /**
     * 从 XFF 头中从右向左取第一个不可信地址
     */
    private String parseXff(String xff, String remoteAddr) {
        String[] parts = xff.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String addr = parts[i].trim();
            if (!addr.isEmpty() && !isTrusted(addr)) {
                return addr;
            }
        }
        return remoteAddr;
    }

    private String extractRemoteAddr(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    /**
     * 判断地址是否为可信代理
     * <p>
     * 支持精确 IP 匹配和 IPv4 CIDR 匹配
     */
    private boolean isTrusted(String addr) {
        if ("unknown".equals(addr)) {
            return false;
        }
        for (String proxy : trustedProxies) {
            if (proxy.contains("/")) {
                if (matchesCidr(addr, proxy)) {
                    return true;
                }
            } else if (proxy.equals(addr)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCidr(String addr, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            byte[] target = InetAddress.getByName(addr).getAddress();
            if (network.length != target.length) {
                return false;
            }
            int prefixLen = Integer.parseInt(parts[1]);
            int fullBytes = prefixLen / 8;
            int remainBits = prefixLen % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (network[i] != target[i]) {
                    return false;
                }
            }
            if (remainBits > 0 && fullBytes < network.length) {
                int mask = 0xFF << (8 - remainBits);
                return (network[fullBytes] & mask) == (target[fullBytes] & mask);
            }
            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("CIDR 匹配失败: addr={}, cidr={}", addr, cidr);
            return false;
        }
    }
}
