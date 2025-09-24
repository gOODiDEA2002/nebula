package io.nebula.discovery.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 服务发现核心接口
 * 定义服务注册、发现、订阅等核心功能
 */
public interface ServiceDiscovery {
    
    /**
     * 注册服务实例
     * 
     * @param instance 服务实例
     * @throws ServiceDiscoveryException 注册失败时抛出异常
     */
    void register(ServiceInstance instance) throws ServiceDiscoveryException;
    
    /**
     * 异步注册服务实例
     * 
     * @param instance 服务实例
     * @return CompletableFuture
     */
    default CompletableFuture<Void> registerAsync(ServiceInstance instance) {
        return CompletableFuture.runAsync(() -> {
            try {
                register(instance);
            } catch (ServiceDiscoveryException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 注销服务实例
     * 
     * @param serviceName 服务名称
     * @param instanceId 实例ID
     * @throws ServiceDiscoveryException 注销失败时抛出异常
     */
    void deregister(String serviceName, String instanceId) throws ServiceDiscoveryException;
    
    /**
     * 异步注销服务实例
     * 
     * @param serviceName 服务名称
     * @param instanceId 实例ID
     * @return CompletableFuture
     */
    default CompletableFuture<Void> deregisterAsync(String serviceName, String instanceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                deregister(serviceName, instanceId);
            } catch (ServiceDiscoveryException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 获取所有健康的服务实例
     * 
     * @param serviceName 服务名称
     * @return 服务实例列表
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    List<ServiceInstance> getInstances(String serviceName) throws ServiceDiscoveryException;
    
    /**
     * 获取服务实例（可指定是否只获取健康实例）
     * 
     * @param serviceName 服务名称
     * @param healthyOnly 是否只获取健康实例
     * @return 服务实例列表
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    List<ServiceInstance> getInstances(String serviceName, boolean healthyOnly) throws ServiceDiscoveryException;
    
    /**
     * 获取指定分组的服务实例
     * 
     * @param serviceName 服务名称
     * @param groupName 分组名称
     * @return 服务实例列表
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    default List<ServiceInstance> getInstances(String serviceName, String groupName) throws ServiceDiscoveryException {
        return getInstances(serviceName, groupName, true);
    }
    
    /**
     * 获取指定分组的服务实例
     * 
     * @param serviceName 服务名称
     * @param groupName 分组名称
     * @param healthyOnly 是否只获取健康实例
     * @return 服务实例列表
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    List<ServiceInstance> getInstances(String serviceName, String groupName, boolean healthyOnly) throws ServiceDiscoveryException;
    
    /**
     * 获取指定集群的服务实例
     * 
     * @param serviceName 服务名称
     * @param clusters 集群名称列表
     * @return 服务实例列表
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    List<ServiceInstance> getInstances(String serviceName, List<String> clusters) throws ServiceDiscoveryException;
    
    /**
     * 订阅服务变化
     * 
     * @param serviceName 服务名称
     * @param listener 变化监听器
     * @throws ServiceDiscoveryException 订阅失败时抛出异常
     */
    void subscribe(String serviceName, ServiceChangeListener listener) throws ServiceDiscoveryException;
    
    /**
     * 订阅指定分组的服务变化
     * 
     * @param serviceName 服务名称
     * @param groupName 分组名称
     * @param listener 变化监听器
     * @throws ServiceDiscoveryException 订阅失败时抛出异常
     */
    default void subscribe(String serviceName, String groupName, ServiceChangeListener listener) throws ServiceDiscoveryException {
        subscribe(serviceName, listener);
    }
    
    /**
     * 取消订阅服务变化
     * 
     * @param serviceName 服务名称
     * @throws ServiceDiscoveryException 取消订阅失败时抛出异常
     */
    void unsubscribe(String serviceName) throws ServiceDiscoveryException;
    
    /**
     * 取消订阅指定分组的服务变化
     * 
     * @param serviceName 服务名称
     * @param groupName 分组名称
     * @throws ServiceDiscoveryException 取消订阅失败时抛出异常
     */
    default void unsubscribe(String serviceName, String groupName) throws ServiceDiscoveryException {
        unsubscribe(serviceName);
    }
    
    /**
     * 获取所有服务名称
     * 
     * @return 服务名称列表
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    List<String> getServices() throws ServiceDiscoveryException;
    
    /**
     * 获取所有服务名称（分页）
     * 
     * @param pageNo 页码（从1开始）
     * @param pageSize 页大小
     * @return 服务名称列表
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    List<String> getServices(int pageNo, int pageSize) throws ServiceDiscoveryException;
    
    /**
     * 获取指定分组的所有服务名称
     * 
     * @param groupName 分组名称
     * @return 服务名称列表
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    default List<String> getServices(String groupName) throws ServiceDiscoveryException {
        return getServices();
    }
    
    /**
     * 检查服务是否存在
     * 
     * @param serviceName 服务名称
     * @return 是否存在
     * @throws ServiceDiscoveryException 查询失败时抛出异常
     */
    default boolean existsService(String serviceName) throws ServiceDiscoveryException {
        List<ServiceInstance> instances = getInstances(serviceName, false);
        return instances != null && !instances.isEmpty();
    }
    
    /**
     * 更新服务实例信息
     * 
     * @param instance 服务实例
     * @throws ServiceDiscoveryException 更新失败时抛出异常
     */
    default void updateInstance(ServiceInstance instance) throws ServiceDiscoveryException {
        // 默认实现：先注销再注册
        deregister(instance.getServiceName(), instance.getInstanceId());
        register(instance);
    }
    
    /**
     * 发送心跳
     * 
     * @param serviceName 服务名称
     * @param instanceId 实例ID
     * @throws ServiceDiscoveryException 心跳失败时抛出异常
     */
    default void heartbeat(String serviceName, String instanceId) throws ServiceDiscoveryException {
        // 默认空实现，子类可以覆盖
    }
    
    /**
     * 关闭服务发现客户端
     * 释放资源，取消所有订阅
     */
    default void shutdown() {
        // 默认空实现，子类可以覆盖
    }
}

