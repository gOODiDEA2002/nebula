package io.nebula.discovery.nacos.util;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

/**
 * 网络工具类
 * 用于获取本机IP地址,支持网络地址过滤
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class NetworkUtils {

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String EMPTY_IP = "0.0.0.0";

    /**
     * 获取本机IP地址
     *
     * @return IP地址
     */
    public static String getLocalHost() {
        return getLocalHost(null, null);
    }

    /**
     * 获取本机IP地址(支持首选网络和忽略接口)
     *
     * @param preferredNetworks 首选网络前缀列表
     * @param ignoredInterfaces 忽略的网络接口列表
     * @return IP地址
     */
    public static String getLocalHost(List<String> preferredNetworks, List<String> ignoredInterfaces) {
        InetAddress localAddress = null;
        InetAddress preferredAddress = null;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                log.warn("无法获取网络接口列表");
                return getLocalHostFallback();
            }

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // 跳过被忽略的网络接口
                if (shouldIgnoreInterface(networkInterface, ignoredInterfaces)) {
                    continue;
                }

                // 跳过回环接口、虚拟接口和未启用的接口
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    // 只处理IPv4地址
                    if (!(address instanceof Inet4Address)) {
                        continue;
                    }

                    // 跳过回环地址和链路本地地址
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                        continue;
                    }

                    String hostAddress = address.getHostAddress();

                    // 检查是否匹配首选网络
                    if (preferredNetworks != null && !preferredNetworks.isEmpty()) {
                        if (matchesPreferredNetwork(hostAddress, preferredNetworks)) {
                            preferredAddress = address;
                            log.debug("找到首选网络地址: {} (网络接口: {})", hostAddress, networkInterface.getName());
                            break; // 找到首选地址后立即返回
                        }
                    }

                    // 保存第一个可用地址作为备选
                    if (localAddress == null) {
                        localAddress = address;
                    }
                }

                // 如果已经找到首选地址,停止搜索
                if (preferredAddress != null) {
                    break;
                }
            }
        } catch (SocketException e) {
            log.error("获取本机IP地址失败", e);
            return getLocalHostFallback();
        }

        // 优先返回首选地址
        if (preferredAddress != null) {
            String preferredHost = preferredAddress.getHostAddress();
            log.info("使用首选网络地址: {}", preferredHost);
            return preferredHost;
        }

        // 其次返回第一个可用地址
        if (localAddress != null) {
            String localHost = localAddress.getHostAddress();
            log.info("使用本机IP地址: {}", localHost);
            return localHost;
        }

        // 兜底方案
        return getLocalHostFallback();
    }

    /**
     * 检查是否应该忽略该网络接口
     *
     * @param networkInterface 网络接口
     * @param ignoredInterfaces 忽略的接口列表
     * @return 是否忽略
     */
    private static boolean shouldIgnoreInterface(NetworkInterface networkInterface, List<String> ignoredInterfaces) {
        if (ignoredInterfaces == null || ignoredInterfaces.isEmpty()) {
            return false;
        }

        String interfaceName = networkInterface.getName();
        for (String ignoredInterface : ignoredInterfaces) {
            if (interfaceName.startsWith(ignoredInterface)) {
                log.debug("忽略网络接口: {}", interfaceName);
                return true;
            }
        }
        return false;
    }

    /**
     * 检查IP地址是否匹配首选网络
     *
     * @param hostAddress IP地址
     * @param preferredNetworks 首选网络前缀列表
     * @return 是否匹配
     */
    private static boolean matchesPreferredNetwork(String hostAddress, List<String> preferredNetworks) {
        for (String preferredNetwork : preferredNetworks) {
            if (hostAddress.startsWith(preferredNetwork)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 兜底方案:尝试使用 InetAddress.getLocalHost()
     *
     * @return IP地址
     */
    private static String getLocalHostFallback() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            String hostAddress = address.getHostAddress();
            if (!LOCALHOST_IP.equals(hostAddress) && !EMPTY_IP.equals(hostAddress)) {
                log.info("使用兜底方案获取IP地址: {}", hostAddress);
                return hostAddress;
            }
        } catch (Exception e) {
            log.error("兜底方案获取IP地址失败", e);
        }

        log.warn("无法获取本机IP地址,使用localhost");
        return LOCALHOST_IP;
    }
}

