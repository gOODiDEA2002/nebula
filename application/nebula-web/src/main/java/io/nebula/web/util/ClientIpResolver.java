package io.nebula.web.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 IP 解析器（可信代理感知）
 *
 * <p>此前限流键与请求日志直接信任 {@code X-Forwarded-For} 首段, 客户端每次请求伪造
 * 一个新 IP 即可绕过 IP 限流(CWT-28)。本解析器的语义:</p>
 * <ul>
 *   <li>未配置可信代理(默认): 一律使用 {@code remoteAddr}, 完全不读转发头——不可伪造</li>
 *   <li>配置了可信代理且 {@code remoteAddr} 命中: 解析 {@code X-Forwarded-For},
 *       从右向左取第一个不在可信列表中的地址(右侧是最近的代理写入的, 左侧可被客户端伪造);
 *       XFF 缺失时回退 {@code X-Real-IP}, 再回退 {@code remoteAddr}</li>
 *   <li>{@code remoteAddr} 不是可信代理: 使用 {@code remoteAddr}, 忽略转发头</li>
 * </ul>
 *
 * <p>可信代理支持精确 IP(v4/v6)与 IPv4 CIDR(如 {@code 10.0.0.0/8})。</p>
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
public class ClientIpResolver {

    private static final String UNKNOWN = "unknown";

    private final List<String> exactProxies = new ArrayList<>();
    private final List<long[]> cidrRanges = new ArrayList<>();

    public ClientIpResolver(String[] trustedProxies) {
        if (trustedProxies != null) {
            for (String entry : trustedProxies) {
                if (!StringUtils.hasText(entry)) {
                    continue;
                }
                String trimmed = entry.trim();
                if (trimmed.contains("/")) {
                    long[] range = parseCidr(trimmed);
                    if (range != null) {
                        cidrRanges.add(range);
                    }
                } else {
                    exactProxies.add(trimmed);
                }
            }
        }
    }

    /**
     * 解析客户端真实 IP
     */
    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!hasTrustedProxies() || !isTrustedProxy(remoteAddr)) {
            return StringUtils.hasText(remoteAddr) ? remoteAddr : UNKNOWN;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !UNKNOWN.equalsIgnoreCase(xForwardedFor)) {
            String[] hops = xForwardedFor.split(",");
            // 从右向左: 跳过可信代理自己追加的地址, 第一个不可信地址即客户端
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = hops[i].trim();
                if (StringUtils.hasText(hop) && !isTrustedProxy(hop)) {
                    return hop;
                }
            }
            // 整条链路都可信(内网互调): 取最左原始地址
            String first = hops[0].trim();
            if (StringUtils.hasText(first)) {
                return first;
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !UNKNOWN.equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return StringUtils.hasText(remoteAddr) ? remoteAddr : UNKNOWN;
    }

    /**
     * 是否配置了可信代理
     */
    public boolean hasTrustedProxies() {
        return !exactProxies.isEmpty() || !cidrRanges.isEmpty();
    }

    private boolean isTrustedProxy(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        if (exactProxies.contains(ip)) {
            return true;
        }
        if (!cidrRanges.isEmpty()) {
            long value = ipv4ToLong(ip);
            if (value >= 0) {
                for (long[] range : cidrRanges) {
                    if (value >= range[0] && value <= range[1]) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 解析 IPv4 CIDR 为 [起始, 结束] 数值区间, 非法输入返回 null
     */
    private static long[] parseCidr(String cidr) {
        String[] parts = cidr.split("/");
        if (parts.length != 2) {
            return null;
        }
        long base = ipv4ToLong(parts[0].trim());
        if (base < 0) {
            return null;
        }
        int prefix;
        try {
            prefix = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (prefix < 0 || prefix > 32) {
            return null;
        }
        long mask = prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        long start = base & mask;
        long end = start | (~mask & 0xFFFFFFFFL);
        return new long[]{start, end};
    }

    /**
     * IPv4 转数值, 非 IPv4 返回 -1
     */
    private static long ipv4ToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            return -1;
        }
        long value = 0;
        for (String octet : octets) {
            int part;
            try {
                part = Integer.parseInt(octet.trim());
            } catch (NumberFormatException e) {
                return -1;
            }
            if (part < 0 || part > 255) {
                return -1;
            }
            value = (value << 8) | part;
        }
        return value;
    }
}
