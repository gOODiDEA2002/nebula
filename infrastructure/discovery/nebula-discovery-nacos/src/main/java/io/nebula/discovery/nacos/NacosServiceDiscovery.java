package io.nebula.discovery.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceDiscoveryException;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.discovery.core.ServiceChangeListener;
import io.nebula.discovery.nacos.config.NacosProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Nacos服务发现实现
 */
@Slf4j
public class NacosServiceDiscovery implements ServiceDiscovery, InitializingBean, DisposableBean {
    
    private final NacosProperties nacosProperties;
    private NamingService namingService;
    private final ConcurrentHashMap<String, List<ServiceInstance>> serviceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EventListener> listenerCache = new ConcurrentHashMap<>();
    
    public NacosServiceDiscovery(NacosProperties nacosProperties) {
        this.nacosProperties = nacosProperties;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        initNamingService();
    }
    
    @Override
    public void destroy() throws Exception {
        shutdown();
    }
    
    @Override
    public void shutdown() {
        try {
            if (namingService != null) {
                namingService.shutDown();
            }
        } catch (Exception e) {
            log.error("关闭 Nacos 服务发现失败", e);
        }
    }
    
    @Override
    public void register(ServiceInstance instance) throws ServiceDiscoveryException {
        try {
            Instance nacosInstance = convertToNacosInstance(instance);
            namingService.registerInstance(instance.getServiceName(), 
                    instance.getGroupName() != null ? instance.getGroupName() : nacosProperties.getGroupName(), 
                    nacosInstance);
            log.info("注册服务实例: serviceName={}, instanceId={}, address={}", 
                    instance.getServiceName(), instance.getInstanceId(), instance.getAddress());
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("注册服务实例失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void deregister(String serviceName, String instanceId) throws ServiceDiscoveryException {
        try {
            // 从缓存中查找实例信息
            List<ServiceInstance> instances = serviceCache.get(serviceName);
            if (instances != null) {
                for (ServiceInstance instance : instances) {
                    if (instanceId.equals(instance.getInstanceId())) {
                        namingService.deregisterInstance(serviceName, 
                                instance.getGroupName() != null ? instance.getGroupName() : nacosProperties.getGroupName(),
                                instance.getIp(), instance.getPort());
                        log.info("注销服务实例: serviceName={}, instanceId={}", serviceName, instanceId);
                        return;
                    }
                }
            }
            throw new ServiceDiscoveryException("未找到要注销的服务实例: " + serviceName + "#" + instanceId);
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("注销服务实例失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 注册服务实例（兼容方法）
     */
    public void registerInstance(String serviceName, String ip, int port) throws NacosException {
        registerInstance(serviceName, ip, port, nacosProperties.getClusterName());
    }
    
    /**
     * 注册服务实例
     */
    public void registerInstance(String serviceName, String ip, int port, String clusterName) throws NacosException {
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        instance.setWeight(nacosProperties.getWeight());
        instance.setClusterName(clusterName);
        instance.setEnabled(nacosProperties.isInstanceEnabled());
        instance.setHealthy(nacosProperties.isHealthy());
        instance.setMetadata(nacosProperties.getMetadata());
        
        namingService.registerInstance(serviceName, nacosProperties.getGroupName(), instance);
        log.info("注册服务实例: serviceName={}, ip={}, port={}", serviceName, ip, port);
    }
    
    /**
     * 注销服务实例
     */
    public void deregisterInstance(String serviceName, String ip, int port) throws NacosException {
        namingService.deregisterInstance(serviceName, nacosProperties.getGroupName(), ip, port);
        log.info("注销服务实例: serviceName={}, ip={}, port={}", serviceName, ip, port);
    }
    
    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws ServiceDiscoveryException {
        return getInstances(serviceName, true);
    }
    
    @Override
    public List<ServiceInstance> getInstances(String serviceName, boolean healthyOnly) throws ServiceDiscoveryException {
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, nacosProperties.getGroupName(), healthyOnly);
            List<ServiceInstance> result = instances.stream()
                    .map(this::convertToServiceInstance)
                    .collect(Collectors.toList());
            
            // 更新缓存
            serviceCache.put(serviceName, result);
            return result;
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("获取服务实例失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ServiceInstance> getInstances(String serviceName, String groupName, boolean healthyOnly) throws ServiceDiscoveryException {
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, groupName, healthyOnly);
            return instances.stream()
                    .map(this::convertToServiceInstance)
                    .collect(Collectors.toList());
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("获取服务实例失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ServiceInstance> getInstances(String serviceName, List<String> clusters) throws ServiceDiscoveryException {
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, nacosProperties.getGroupName(), clusters, true);
            return instances.stream()
                    .map(this::convertToServiceInstance)
                    .collect(Collectors.toList());
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("获取指定集群的服务实例失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) throws ServiceDiscoveryException {
        try {
            subscribeInternal(serviceName, nacosProperties.getGroupName(), listener);
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("订阅服务变化失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void subscribe(String serviceName, String groupName, ServiceChangeListener listener) throws ServiceDiscoveryException {
        try {
            subscribeInternal(serviceName, groupName, listener);
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("订阅服务变化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 内部订阅方法
     */
    private void subscribeInternal(String serviceName, String groupName, ServiceChangeListener listener) throws NacosException {
        EventListener eventListener = new EventListener() {
            @Override
            public void onEvent(Event event) {
                if (event instanceof NamingEvent) {
                    NamingEvent namingEvent = (NamingEvent) event;
                    List<ServiceInstance> instances = namingEvent.getInstances().stream()
                            .map(NacosServiceDiscovery.this::convertToServiceInstance)
                            .collect(Collectors.toList());
                    
                    // 更新缓存
                    serviceCache.put(serviceName, instances);
                    
                    // 通知监听器
                    listener.onServiceChange(serviceName, instances);
                }
            }
        };
        
        namingService.subscribe(serviceName, groupName, eventListener);
        listenerCache.put(serviceName + "#" + groupName, eventListener);
        log.info("订阅服务变化: serviceName={}, groupName={}", serviceName, groupName);
    }
    
    @Override
    public void unsubscribe(String serviceName) throws ServiceDiscoveryException {
        try {
            unsubscribe(serviceName, nacosProperties.getGroupName());
        } catch (Exception e) {
            throw new ServiceDiscoveryException("取消订阅服务变化失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void unsubscribe(String serviceName, String groupName) throws ServiceDiscoveryException {
        try {
            String key = serviceName + "#" + groupName;
            EventListener listener = listenerCache.remove(key);
            if (listener != null) {
                namingService.unsubscribe(serviceName, groupName, listener);
                serviceCache.remove(serviceName);
                log.info("取消订阅服务变化: serviceName={}, groupName={}", serviceName, groupName);
            }
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("取消订阅服务变化失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> getServices() throws ServiceDiscoveryException {
        try {
            return namingService.getServicesOfServer(1, Integer.MAX_VALUE, nacosProperties.getGroupName()).getData();
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("获取服务列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> getServices(int pageNo, int pageSize) throws ServiceDiscoveryException {
        try {
            return namingService.getServicesOfServer(pageNo, pageSize, nacosProperties.getGroupName()).getData();
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("获取服务列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> getServices(String groupName) throws ServiceDiscoveryException {
        try {
            return namingService.getServicesOfServer(1, Integer.MAX_VALUE, groupName).getData();
        } catch (NacosException e) {
            throw new ServiceDiscoveryException("获取服务列表失败: " + e.getMessage(), e);
        }
    }
    
    private void initNamingService() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", nacosProperties.getServerAddr());
        properties.setProperty("namespace", nacosProperties.getNamespace());
        
        // 设置认证信息(空字符串也会被设置,确保 Nacos 客户端能正确处理认证)
        if (nacosProperties.getUsername() != null && !nacosProperties.getUsername().isEmpty()) {
            properties.setProperty("username", nacosProperties.getUsername());
            log.info("Nacos 认证已启用: username={}", nacosProperties.getUsername());
        }
        if (nacosProperties.getPassword() != null && !nacosProperties.getPassword().isEmpty()) {
            properties.setProperty("password", nacosProperties.getPassword());
        }
        if (nacosProperties.getAccessKey() != null && !nacosProperties.getAccessKey().isEmpty()) {
            properties.setProperty("accessKey", nacosProperties.getAccessKey());
        }
        if (nacosProperties.getSecretKey() != null && !nacosProperties.getSecretKey().isEmpty()) {
            properties.setProperty("secretKey", nacosProperties.getSecretKey());
        }
        
        namingService = NamingFactory.createNamingService(properties);
        log.info("Nacos NamingService 初始化完成");
    }
    
    private ServiceInstance convertToServiceInstance(Instance instance) {
        return ServiceInstance.builder()
                .serviceName(instance.getServiceName())
                .instanceId(instance.getInstanceId())
                .ip(instance.getIp())
                .port(instance.getPort())
                .weight(instance.getWeight())
                .healthy(instance.isHealthy())
                .enabled(instance.isEnabled())
                .clusterName(instance.getClusterName())
                .metadata(instance.getMetadata())
                .registerTime(System.currentTimeMillis())
                .lastHeartbeat(System.currentTimeMillis())
                .protocol("http") // 默认协议
                .build();
    }
    
    private Instance convertToNacosInstance(ServiceInstance serviceInstance) {
        Instance instance = new Instance();
        instance.setServiceName(serviceInstance.getServiceName());
        instance.setInstanceId(serviceInstance.getInstanceId());
        instance.setIp(serviceInstance.getIp());
        instance.setPort(serviceInstance.getPort());
        instance.setWeight(serviceInstance.getWeight());
        instance.setHealthy(serviceInstance.isHealthy());
        instance.setEnabled(serviceInstance.isEnabled());
        instance.setClusterName(serviceInstance.getClusterName() != null ? 
                               serviceInstance.getClusterName() : nacosProperties.getClusterName());
        instance.setMetadata(serviceInstance.getMetadata());
        return instance;
    }
}
