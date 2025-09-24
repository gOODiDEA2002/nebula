package io.nebula.rpc.core.discovery;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.discovery.core.LoadBalancer;
import io.nebula.discovery.core.LoadBalancerFactory;
import io.nebula.discovery.core.LoadBalanceStrategy;
import io.nebula.discovery.core.ServiceDiscoveryException;
import io.nebula.rpc.core.client.RpcClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集成服务发现的 RPC 客户端
 * 自动从服务发现中获取服务实例，并进行负载均衡
 */
@Slf4j
public class ServiceDiscoveryRpcClient implements RpcClient {
    
    private final ServiceDiscovery serviceDiscovery;
    private final LoadBalancer loadBalancer;
    private final RpcClient delegateClient;
    private final ConcurrentHashMap<String, List<ServiceInstance>> serviceCache = new ConcurrentHashMap<>();
    
    public ServiceDiscoveryRpcClient(ServiceDiscovery serviceDiscovery, 
                                   LoadBalancer loadBalancer, 
                                   RpcClient delegateClient) {
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalancer = loadBalancer;
        this.delegateClient = delegateClient;
        
        log.info("ServiceDiscoveryRpcClient 初始化完成，负载均衡策略: {}", 
                loadBalancer.getClass().getSimpleName());
    }
    
    public ServiceDiscoveryRpcClient(ServiceDiscovery serviceDiscovery, 
                                   LoadBalanceStrategy strategy, 
                                   RpcClient delegateClient) {
        this(serviceDiscovery, LoadBalancerFactory.getLoadBalancer(strategy), delegateClient);
    }
    
    public ServiceDiscoveryRpcClient(ServiceDiscovery serviceDiscovery, 
                                   RpcClient delegateClient) {
        this(serviceDiscovery, LoadBalancerFactory.getDefaultLoadBalancer(), delegateClient);
    }
    
    @Override
    public <T> T call(Class<T> serviceClass, String methodName, Object... args) {
        String serviceName = serviceClass.getName();
        ServiceInstance instance = selectServiceInstance(serviceName);
        
        if (instance == null) {
            throw new RuntimeException("没有可用的服务实例: " + serviceName);
        }
        
        try {
            // 设置目标服务地址
            setTargetAddress(delegateClient, instance);
            
            // 执行RPC调用
            return delegateClient.call(serviceClass, methodName, args);
            
        } catch (Exception e) {
            log.error("RPC调用失败: serviceName={}, instance={}, method={}", 
                    serviceName, instance.getAddress(), methodName, e);
            throw new RuntimeException("RPC调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public <T> CompletableFuture<T> callAsync(Class<T> serviceClass, String methodName, Object... args) {
        return CompletableFuture.supplyAsync(() -> call(serviceClass, methodName, args));
    }
    
    @Override
    public <T> T createProxy(Class<T> serviceClass) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                (proxy, method, methodArgs) -> {
                    // 对于Object类的方法，直接调用
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(this, methodArgs);
                    }
                    
                    // RPC调用
                    return call(serviceClass, method.getName(), methodArgs);
                }
        );
    }
    
    @Override
    public String getServiceAddress(String serviceName) {
        ServiceInstance instance = selectServiceInstance(serviceName);
        return instance != null ? instance.getAddress() : null;
    }
    
    @Override
    public void close() {
        try {
            if (delegateClient != null) {
                delegateClient.close();
            }
            
            // 清理缓存
            serviceCache.clear();
            
            log.info("ServiceDiscoveryRpcClient 已关闭");
        } catch (Exception e) {
            log.error("关闭 ServiceDiscoveryRpcClient 时发生异常", e);
        }
    }
    
    /**
     * 选择服务实例
     * 
     * @param serviceName 服务名称
     * @return 选中的服务实例，如果没有可用实例返回null
     */
    private ServiceInstance selectServiceInstance(String serviceName) {
        try {
            // 从服务发现获取服务实例
            List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName, true);
            
            if (instances == null || instances.isEmpty()) {
                log.warn("没有找到可用的服务实例: {}", serviceName);
                return null;
            }
            
            // 更新缓存
            serviceCache.put(serviceName, instances);
            
            // 使用负载均衡器选择实例
            ServiceInstance selectedInstance = loadBalancer.choose(instances);
            
            if (selectedInstance != null) {
                log.debug("选择服务实例: serviceName={}, instance={}", 
                        serviceName, selectedInstance.getAddress());
            } else {
                log.warn("负载均衡器未能选择到服务实例: {}", serviceName);
            }
            
            return selectedInstance;
            
        } catch (ServiceDiscoveryException e) {
            log.error("获取服务实例失败: serviceName={}", serviceName, e);
            
            // 尝试使用缓存
            List<ServiceInstance> cachedInstances = serviceCache.get(serviceName);
            if (cachedInstances != null && !cachedInstances.isEmpty()) {
                log.info("使用缓存的服务实例: serviceName={}, count={}", 
                        serviceName, cachedInstances.size());
                return loadBalancer.choose(cachedInstances);
            }
            
            return null;
        }
    }
    
    /**
     * 设置目标地址到委托客户端
     * 这里需要根据具体的RpcClient实现来设置目标地址
     * 
     * @param client RPC客户端
     * @param instance 服务实例
     */
    private void setTargetAddress(RpcClient client, ServiceInstance instance) {
        // 这是一个抽象方法，具体实现需要根据RpcClient的类型来决定
        // 对于HttpRpcClient，可能需要设置baseUrl
        // 对于其他类型的客户端，可能需要设置不同的连接参数
        
        if (client instanceof ConfigurableRpcClient) {
            ((ConfigurableRpcClient) client).setTargetAddress(instance.getAddress());
        }
        
        // TODO: 这里可以扩展支持更多类型的RpcClient
    }
    
    /**
     * 可配置的RPC客户端接口
     * 用于设置目标地址
     */
    public interface ConfigurableRpcClient extends RpcClient {
        /**
         * 设置目标地址
         * 
         * @param address 目标地址
         */
        void setTargetAddress(String address);
    }
    
    /**
     * 获取服务实例缓存
     * 
     * @return 服务实例缓存
     */
    public ConcurrentHashMap<String, List<ServiceInstance>> getServiceCache() {
        return serviceCache;
    }
    
    /**
     * 刷新服务实例缓存
     * 
     * @param serviceName 服务名称
     */
    public void refreshServiceCache(String serviceName) {
        try {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName, true);
            if (instances != null) {
                serviceCache.put(serviceName, instances);
                log.info("刷新服务实例缓存: serviceName={}, count={}", serviceName, instances.size());
            }
        } catch (ServiceDiscoveryException e) {
            log.error("刷新服务实例缓存失败: serviceName={}", serviceName, e);
        }
    }
    
    /**
     * 清理服务实例缓存
     * 
     * @param serviceName 服务名称
     */
    public void clearServiceCache(String serviceName) {
        serviceCache.remove(serviceName);
        log.info("清理服务实例缓存: serviceName={}", serviceName);
    }
    
    /**
     * 获取服务发现客户端
     * 
     * @return 服务发现客户端
     */
    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }
    
    /**
     * 获取负载均衡器
     * 
     * @return 负载均衡器
     */
    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }
}
