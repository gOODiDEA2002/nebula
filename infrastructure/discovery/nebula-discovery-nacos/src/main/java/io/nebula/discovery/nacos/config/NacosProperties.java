package io.nebula.discovery.nacos.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nacos 配置属性
 * 
 * 智能默认值设计：
 * - 支持环境变量覆盖
 * - 提供开箱即用的默认值
 * - 最小化必需配置项
 * - 配置校验确保参数有效
 * 
 * 环境变量优先级：
 * 1. 显式配置的值（application.yml）
 * 2. 环境变量（NACOS_ADDR 等）
 * 3. 默认值（localhost:8848 等）
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "nebula.discovery.nacos")
public class NacosProperties {
    
    /**
     * Nacos 服务器地址
     * 格式: host:port 或 host1:port1,host2:port2（集群模式）
     * 默认值: localhost:8848
     * 可通过环境变量 NACOS_ADDR 覆盖（在 application.yml 中配置）
     */
    @NotBlank(message = "Nacos 服务器地址不能为空，请配置 nebula.discovery.nacos.server-addr")
    private String serverAddr = "localhost:8848";
    
    /**
     * 命名空间
     * 默认值: 空字符串（表示 public 命名空间）
     * 
     * 注意：Nacos 的 public 命名空间 ID 是空字符串，不是 "public"
     */
    private String namespace = "";
    
    /**
     * 集群名称
     * 默认值: DEFAULT
     */
    @NotBlank(message = "集群名称不能为空")
    private String clusterName = "DEFAULT";
    
    /**
     * 分组名称
     * 默认值: DEFAULT_GROUP
     */
    @NotBlank(message = "分组名称不能为空")
    private String groupName = "DEFAULT_GROUP";
    
    /**
     * 用户名
     * 默认值: 空（不发送认证信息）。开启了鉴权的 Nacos 集群须显式配置，
     * 不再默认使用出厂账号 nacos/nacos——避免生产误用默认弱凭据
     */
    private String username = "";
    
    /**
     * 密码
     * 默认值: 空（不发送认证信息），须与 username 一起显式配置
     */
    private String password = "";
    
    /**
     * 访问密钥
     */
    private String accessKey;
    
    /**
     * 密钥
     */
    private String secretKey;
    
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
