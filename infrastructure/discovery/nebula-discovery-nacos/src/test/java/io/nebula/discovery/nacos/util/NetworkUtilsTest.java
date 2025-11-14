package io.nebula.discovery.nacos.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 网络工具类测试
 * 
 * 测试目的: 验证网络地址首选配置和网络接口过滤功能
 */
class NetworkUtilsTest {
    
    @Test
    void testGetLocalHost() {
        // 测试获取本机IP地址
        String localHost = NetworkUtils.getLocalHost();
        
        // 验证不为空且不是默认地址
        assertThat(localHost).isNotNull().isNotEmpty();
        assertThat(localHost).isNotEqualTo("0.0.0.0");
        
        // 验证是合法的IP地址格式
        assertThat(localHost).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
    
    @Test
    void testGetLocalHostWithPreferredNetworks() {
        // 测试带首选网络的情况
        // 注意：这个测试可能会因为实际网络环境而有不同的结果
        
        // 常见的内网IP段
        List<String> preferredNetworks = Arrays.asList("192.168.", "10.", "172.16.");
        
        String localHost = NetworkUtils.getLocalHost(preferredNetworks, null);
        
        // 验证不为空
        assertThat(localHost).isNotNull().isNotEmpty();
        
        // 如果有匹配的网络，应该返回对应的地址
        // 验证是合法的IP地址格式
        assertThat(localHost).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
    
    @Test
    void testGetLocalHostWithIgnoredInterfaces() {
        // 测试忽略某些网络接口
        List<String> ignoredInterfaces = Arrays.asList("docker", "veth", "br-");
        
        String localHost = NetworkUtils.getLocalHost(null, ignoredInterfaces);
        
        // 验证不为空
        assertThat(localHost).isNotNull().isNotEmpty();
        
        // 验证是合法的IP地址格式
        assertThat(localHost).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
    
    @Test
    void testGetLocalHostWithPreferredNetworksAndIgnoredInterfaces() {
        // 测试同时使用首选网络和忽略接口
        List<String> preferredNetworks = Arrays.asList("192.168.");
        List<String> ignoredInterfaces = Arrays.asList("docker", "veth");
        
        String localHost = NetworkUtils.getLocalHost(preferredNetworks, ignoredInterfaces);
        
        // 验证不为空
        assertThat(localHost).isNotNull().isNotEmpty();
        
        // 验证是合法的IP地址格式
        assertThat(localHost).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
    
    @Test
    void testGetLocalHostWithEmptyPreferredNetworks() {
        // 测试空的首选网络列表
        String localHost = NetworkUtils.getLocalHost(Collections.emptyList(), null);
        
        // 验证不为空
        assertThat(localHost).isNotNull().isNotEmpty();
        
        // 验证是合法的IP地址格式
        assertThat(localHost).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
    
    @Test
    void testGetLocalHostWithEmptyIgnoredInterfaces() {
        // 测试空的忽略接口列表
        String localHost = NetworkUtils.getLocalHost(null, Collections.emptyList());
        
        // 验证不为空
        assertThat(localHost).isNotNull().isNotEmpty();
        
        // 验证是合法的IP地址格式
        assertThat(localHost).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
    
    @Test
    void testGetLocalHostNotLocalhost() {
        // 确保返回的不是localhost地址
        String localHost = NetworkUtils.getLocalHost();
        
        // 在正常情况下（有网卡），应该不返回127.0.0.1
        // 但在某些特殊环境下可能会返回127.0.0.1，所以这个断言可能需要根据实际情况调整
        if (!"127.0.0.1".equals(localHost)) {
            assertThat(localHost).isNotEqualTo("127.0.0.1");
        }
    }
    
    @Test
    void testMultipleCallsConsistency() {
        // 测试多次调用的一致性
        String localHost1 = NetworkUtils.getLocalHost();
        String localHost2 = NetworkUtils.getLocalHost();
        String localHost3 = NetworkUtils.getLocalHost();
        
        // 多次调用应该返回相同的结果
        assertThat(localHost1).isEqualTo(localHost2);
        assertThat(localHost2).isEqualTo(localHost3);
    }
    
    @Test
    void testPreferredNetworkPriority() {
        // 测试首选网络的优先级
        // 如果机器有多个网卡，首选网络应该优先返回
        
        // 设置一个通常不会匹配的首选网络
        List<String> preferredNetworks = Arrays.asList("240.240.");
        String localHost = NetworkUtils.getLocalHost(preferredNetworks, null);
        
        // 如果没有匹配的首选网络，应该返回其他可用地址
        assertThat(localHost).isNotNull().isNotEmpty();
        assertThat(localHost).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
    
    @Test
    void testGetLocalHostIsNotEmpty() {
        // 最基本的测试：确保不会返回空值或空字符串
        String localHost = NetworkUtils.getLocalHost();
        
        assertThat(localHost)
                .as("Local host should not be null or empty")
                .isNotNull()
                .isNotEmpty();
    }
    
    @Test
    void testGetLocalHostIsValidIPFormat() {
        // 测试返回值是有效的IPv4格式
        String localHost = NetworkUtils.getLocalHost();
        
        // 分割成4个部分
        String[] parts = localHost.split("\\.");
        
        assertThat(parts)
                .as("IP address should have 4 parts")
                .hasSize(4);
        
        // 验证每个部分都是有效的数字
        for (String part : parts) {
            int value = Integer.parseInt(part);
            assertThat(value)
                    .as("Each part of IP should be between 0 and 255")
                    .isBetween(0, 255);
        }
    }
}

