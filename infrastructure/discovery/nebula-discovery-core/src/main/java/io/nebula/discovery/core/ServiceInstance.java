package io.nebula.discovery.core;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 服务实例信息
 * 统一的服务实例数据模型
 */
@Data
@Builder
public class ServiceInstance implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 实例ID
     */
    private String instanceId;
    
    /**
     * IP地址
     */
    private String ip;
    
    /**
     * 端口号
     */
    private int port;
    
    /**
     * 权重 (用于负载均衡)
     */
    private double weight;
    
    /**
     * 是否健康
     */
    private boolean healthy;
    
    /**
     * 是否启用
     */
    private boolean enabled;
    
    /**
     * 集群名称
     */
    private String clusterName;
    
    /**
     * 分组名称
     */
    private String groupName;
    
    /**
     * 元数据
     */
    private Map<String, String> metadata;
    
    /**
     * 注册时间戳
     */
    private long registerTime;
    
    /**
     * 最后心跳时间
     */
    private long lastHeartbeat;
    
    /**
     * 服务版本
     */
    private String version;
    
    /**
     * 服务协议 (http, https, tcp, grpc等)
     */
    private String protocol;
    
    /**
     * 获取完整的服务地址
     * 
     * @return 格式: protocol://ip:port
     */
    public String getAddress() {
        if (protocol != null && !protocol.isEmpty()) {
            return protocol + "://" + ip + ":" + port;
        }
        return ip + ":" + port;
    }
    
    /**
     * 获取服务URI
     * 
     * @return 格式: ip:port
     */
    public String getUri() {
        return ip + ":" + port;
    }
    
    /**
     * 检查实例是否可用
     * 
     * @return 健康且启用的实例才可用
     */
    public boolean isAvailable() {
        return healthy && enabled;
    }
    
    /**
     * 获取元数据值
     * 
     * @param key 元数据键
     * @return 元数据值，不存在则返回null
     */
    public String getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 获取元数据值，带默认值
     * 
     * @param key 元数据键
     * @param defaultValue 默认值
     * @return 元数据值，不存在则返回默认值
     */
    public String getMetadata(String key, String defaultValue) {
        String value = getMetadata(key);
        return value != null ? value : defaultValue;
    }
}
