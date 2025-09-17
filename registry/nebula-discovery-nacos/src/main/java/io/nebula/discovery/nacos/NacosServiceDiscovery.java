package io.nebula.discovery.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.nebula.discovery.nacos.config.NacosProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Nacos服务发现实现
 */
@Slf4j
@Component
public class NacosServiceDiscovery implements InitializingBean, DisposableBean {
    
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
        if (namingService != null) {
            namingService.shutDown();
        }
    }
    
    /**
     * 注册服务实例
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
    
    /**
     * 获取所有健康的服务实例
     */
    public List<ServiceInstance> getInstances(String serviceName) throws NacosException {
        return getInstances(serviceName, true);
    }
    
    /**
     * 获取服务实例
     */
    public List<ServiceInstance> getInstances(String serviceName, boolean healthy) throws NacosException {
        List<Instance> instances = namingService.selectInstances(serviceName, nacosProperties.getGroupName(), healthy);
        return instances.stream()
                .map(this::convertToServiceInstance)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定集群的服务实例
     */
    public List<ServiceInstance> getInstances(String serviceName, List<String> clusters) throws NacosException {
        List<Instance> instances = namingService.selectInstances(serviceName, nacosProperties.getGroupName(), clusters, true);
        return instances.stream()
                .map(this::convertToServiceInstance)
                .collect(Collectors.toList());
    }
    
    /**
     * 订阅服务变化
     */
    public void subscribe(String serviceName, ServiceChangeListener listener) throws NacosException {
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
        
        namingService.subscribe(serviceName, nacosProperties.getGroupName(), eventListener);
        listenerCache.put(serviceName, eventListener);
        log.info("订阅服务变化: serviceName={}", serviceName);
    }
    
    /**
     * 取消订阅服务变化
     */
    public void unsubscribe(String serviceName) throws NacosException {
        EventListener listener = listenerCache.remove(serviceName);
        if (listener != null) {
            namingService.unsubscribe(serviceName, nacosProperties.getGroupName(), listener);
            serviceCache.remove(serviceName);
            log.info("取消订阅服务变化: serviceName={}", serviceName);
        }
    }
    
    /**
     * 获取所有服务名
     */
    public List<String> getServices(int pageNo, int pageSize) throws NacosException {
        return namingService.getServicesOfServer(pageNo, pageSize, nacosProperties.getGroupName()).getData();
    }
    
    private void initNamingService() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", nacosProperties.getServerAddr());
        properties.setProperty("namespace", nacosProperties.getNamespace());
        
        if (nacosProperties.getUsername() != null) {
            properties.setProperty("username", nacosProperties.getUsername());
        }
        if (nacosProperties.getPassword() != null) {
            properties.setProperty("password", nacosProperties.getPassword());
        }
        if (nacosProperties.getAccessKey() != null) {
            properties.setProperty("accessKey", nacosProperties.getAccessKey());
        }
        if (nacosProperties.getSecretKey() != null) {
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
                .build();
    }
    
    /**
     * 服务实例信息
     */
    @lombok.Data
    @lombok.Builder
    public static class ServiceInstance {
        private String serviceName;
        private String instanceId;
        private String ip;
        private int port;
        private double weight;
        private boolean healthy;
        private boolean enabled;
        private String clusterName;
        private java.util.Map<String, String> metadata;
        
        public String getAddress() {
            return ip + ":" + port;
        }
    }
    
    /**
     * 服务变化监听器
     */
    @FunctionalInterface
    public interface ServiceChangeListener {
        void onServiceChange(String serviceName, List<ServiceInstance> instances);
    }
}
