package io.nebula.discovery.core;

import java.util.List;

/**
 * 服务变化监听器
 * 用于监听服务实例的变化事件
 */
@FunctionalInterface
public interface ServiceChangeListener {
    
    /**
     * 服务变化回调方法
     * 
     * @param serviceName 服务名称
     * @param instances 当前服务实例列表
     */
    void onServiceChange(String serviceName, List<ServiceInstance> instances);
}

/**
 * 服务变化事件类型
 */
enum ServiceChangeType {
    /**
     * 实例注册
     */
    INSTANCE_REGISTERED,
    
    /**
     * 实例注销
     */
    INSTANCE_DEREGISTERED,
    
    /**
     * 实例状态变化 (健康状态、权重等)
     */
    INSTANCE_UPDATED,
    
    /**
     * 服务完全下线
     */
    SERVICE_REMOVED
}

/**
 * 详细的服务变化事件
 */
class ServiceChangeEvent {
    private final String serviceName;
    private final ServiceInstance instance;
    private final ServiceChangeType changeType;
    private final long timestamp;
    
    public ServiceChangeEvent(String serviceName, ServiceInstance instance, 
                            ServiceChangeType changeType) {
        this.serviceName = serviceName;
        this.instance = instance;
        this.changeType = changeType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getServiceName() { return serviceName; }
    public ServiceInstance getInstance() { return instance; }
    public ServiceChangeType getChangeType() { return changeType; }
    public long getTimestamp() { return timestamp; }
}

/**
 * 增强的服务变化监听器
 * 提供更详细的事件信息
 */
@FunctionalInterface
interface DetailedServiceChangeListener {
    
    /**
     * 服务变化回调方法（详细版本）
     * 
     * @param event 服务变化事件
     */
    void onServiceChange(ServiceChangeEvent event);
}
