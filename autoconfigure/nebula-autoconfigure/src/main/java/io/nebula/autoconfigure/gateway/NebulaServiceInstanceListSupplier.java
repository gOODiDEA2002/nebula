package io.nebula.autoconfigure.gateway;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nebula ServiceDiscovery 到 Spring Cloud LoadBalancer 的适配器
 * 
 * 将 Nebula 的 ServiceDiscovery 包装为 Spring Cloud LoadBalancer 可以使用的
 * ServiceInstanceListSupplier，使得 Gateway 可以使用 lb:// URI 进行负载均衡。
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class NebulaServiceInstanceListSupplier implements ServiceInstanceListSupplier {
    
    private final String serviceId;
    private final ServiceDiscovery serviceDiscovery;
    
    public NebulaServiceInstanceListSupplier(String serviceId, ServiceDiscovery serviceDiscovery) {
        this.serviceId = serviceId;
        this.serviceDiscovery = serviceDiscovery;
    }
    
    @Override
    public String getServiceId() {
        return serviceId;
    }
    
    @Override
    public Flux<List<org.springframework.cloud.client.ServiceInstance>> get() {
        return Flux.defer(() -> {
            try {
                List<ServiceInstance> nebulaInstances = serviceDiscovery.getInstances(serviceId);
                
                if (nebulaInstances == null || nebulaInstances.isEmpty()) {
                    log.debug("服务 {} 无可用实例", serviceId);
                    return Flux.just(Collections.emptyList());
                }
                
                // 转换 Nebula ServiceInstance 到 Spring Cloud ServiceInstance
                List<org.springframework.cloud.client.ServiceInstance> springInstances = nebulaInstances.stream()
                        .filter(ServiceInstance::isHealthy)
                        .map(this::convertToSpringServiceInstance)
                        .collect(Collectors.toList());
                
                log.debug("服务 {} 获取到 {} 个实例", serviceId, springInstances.size());
                return Flux.just(springInstances);
                
            } catch (Exception e) {
                log.warn("获取服务 {} 实例列表失败: {}", serviceId, e.getMessage());
                return Flux.just(Collections.emptyList());
            }
        });
    }
    
    /**
     * 将 Nebula ServiceInstance 转换为 Spring Cloud ServiceInstance
     */
    private org.springframework.cloud.client.ServiceInstance convertToSpringServiceInstance(ServiceInstance instance) {
        return new DefaultServiceInstance(
                instance.getInstanceId(),
                instance.getServiceName(),
                instance.getIp(),
                instance.getPort(),
                false,  // secure - 默认非 HTTPS
                instance.getMetadata()
        );
    }
}

