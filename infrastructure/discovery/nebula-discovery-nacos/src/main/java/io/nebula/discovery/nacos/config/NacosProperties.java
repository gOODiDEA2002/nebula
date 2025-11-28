package io.nebula.discovery.nacos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos配置属性
 */
@Data
@ConfigurationProperties(prefix = "nebula.discovery.nacos")
public class NacosProperties {
    
    /**
     * Nacos服务器地址
     */
    private String serverAddr = "localhost:8848";
    
    /**
     * 命名空间
     */
    private String namespace = "";
    
    /**
     * 集群名称
     */
    private String clusterName = "DEFAULT";
    
    /**
     * 分组名称
     */
    private String groupName = "DEFAULT_GROUP";
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 访问密钥
     */
    private String accessKey;
    
    /**
     * 密钥
     */
    private String secretKey;
    
    /**
     * 心跳间隔（毫秒）
     */
    private long heartbeatInterval = 5000;
    
    /**
     * 心跳超时时间（毫秒）
     */
    private long heartbeatTimeout = 15000;
    
    /**
     * IP删除超时时间（毫秒）
     */
    private long ipDeleteTimeout = 30000;
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
    
    /**
     * 是否自动注册
     */
    private boolean autoRegister = true;
    
    /**
     * 指定注册的 IP 地址
     * 如果不指定，将自动检测本机 IP（根据 preferredNetworks 和 ignoredInterfaces 配置）
     */
    private String ip;
    
    /**
     * 服务权重
     */
    private double weight = 1.0;
    
    /**
     * 是否健康
     */
    private boolean healthy = true;
    
    /**
     * 是否启用
     */
    private boolean instanceEnabled = true;
    
    /**
     * 元数据
     */
    private java.util.Map<String, String> metadata = new java.util.HashMap<>();
    
    /**
     * 首选网络地址列表
     * 用于过滤本机IP地址,优先选择匹配的网段
     * 例如: ["192.168.2", "10.0"]
     * 应用端根据实际网络环境配置
     */
    private java.util.List<String> preferredNetworks = new java.util.ArrayList<>();
    
    /**
     * 忽略的网络接口
     * 框架默认忽略常见的虚拟网卡/不需要的接口
     * 应用端通常不需要修改此配置
     */
    private java.util.List<String> ignoredInterfaces = java.util.Arrays.asList(
            "docker0",     // Docker 默认网桥
            "veth",        // Docker 容器虚拟网卡
            "feth",        // macOS 虚拟网卡
            "utun",        // VPN 隧道
            "awdl",        // Apple Wireless Direct Link
            "llw",         // Low Latency WLAN
            "bridge",      // 网桥接口
            "vmnet",       // VMware 虚拟网卡
            "vboxnet",     // VirtualBox 虚拟网卡
            "virbr"        // Linux 虚拟网桥
    );
}
