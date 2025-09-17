package io.nebula.discovery.nacos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Nacos配置属性
 */
@Data
@Component
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
}
