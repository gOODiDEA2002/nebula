package io.nebula.example.modules.discovery.service.impl;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceDiscoveryException;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.example.modules.discovery.entity.dto.*;
import io.nebula.example.modules.discovery.entity.vo.ServiceInstanceVo;
import io.nebula.example.modules.discovery.service.DiscoveryDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务发现演示Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ServiceDiscovery.class)
public class DiscoveryDemoServiceImpl implements DiscoveryDemoService {
    
    private final ServiceDiscovery serviceDiscovery;
    
    /** 订阅的服务监听器缓存 */
    private final Map<String, Object> subscriptionCache = new ConcurrentHashMap<>();
    
    @Override
    public RegisterServiceDto.Response registerService(RegisterServiceDto.Request request) {
        RegisterServiceDto.Response response = new RegisterServiceDto.Response();
        
        try {
            // 构建服务实例
            ServiceInstance instance = ServiceInstance.builder()
                    .serviceName(request.getServiceName())
                    .instanceId(request.getInstanceId())
                    .ip(request.getIp())
                    .port(request.getPort())
                    .weight(request.getWeight() != null ? request.getWeight() : 1.0)
                    .healthy(true)
                    .enabled(true)
                    .clusterName(request.getClusterName())
                    .groupName(request.getGroupName())
                    .protocol(request.getProtocol())
                    .metadata(request.getMetadata())
                    .registerTime(System.currentTimeMillis())
                    .build();
            
            // 注册服务
            serviceDiscovery.register(instance);
            
            response.setSuccess(true);
            response.setAddress(instance.getAddress());
            response.setMessage("服务注册成功");
            
            log.info("服务注册成功: serviceName={}, instanceId={}, address={}", 
                    request.getServiceName(), request.getInstanceId(), instance.getAddress());
            
        } catch (ServiceDiscoveryException e) {
            log.error("服务注册失败", e);
            response.setSuccess(false);
            response.setMessage("服务注册失败: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public DeregisterServiceDto.Response deregisterService(DeregisterServiceDto.Request request) {
        DeregisterServiceDto.Response response = new DeregisterServiceDto.Response();
        
        try {
            // 注销服务
            serviceDiscovery.deregister(request.getServiceName(), request.getInstanceId());
            
            response.setSuccess(true);
            response.setMessage("服务注销成功");
            
            log.info("服务注销成功: serviceName={}, instanceId={}", 
                    request.getServiceName(), request.getInstanceId());
            
        } catch (ServiceDiscoveryException e) {
            log.error("服务注销失败", e);
            response.setSuccess(false);
            response.setMessage("服务注销失败: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public GetServiceInstancesDto.Response getServiceInstances(GetServiceInstancesDto.Request request) {
        GetServiceInstancesDto.Response response = new GetServiceInstancesDto.Response();
        
        try {
            List<ServiceInstance> instances;
            
            // 根据参数选择查询方式
            if (request.getGroupName() != null && !request.getGroupName().isEmpty()) {
                instances = serviceDiscovery.getInstances(
                        request.getServiceName(), 
                        request.getGroupName(), 
                        request.getHealthyOnly()
                );
            } else {
                instances = serviceDiscovery.getInstances(
                        request.getServiceName(), 
                        request.getHealthyOnly()
                );
            }
            
            // 转换为VO
            List<ServiceInstanceVo> instanceVos = instances.stream()
                    .map(this::convertToVo)
                    .collect(Collectors.toList());
            
            response.setInstances(instanceVos);
            response.setTotal(instanceVos.size());
            
            log.info("查询服务实例成功: serviceName={}, count={}", 
                    request.getServiceName(), instanceVos.size());
            
        } catch (ServiceDiscoveryException e) {
            log.error("查询服务实例失败", e);
            response.setInstances(List.of());
            response.setTotal(0);
        }
        
        return response;
    }
    
    @Override
    public GetAllServicesDto.Response getAllServices(GetAllServicesDto.Request request) {
        GetAllServicesDto.Response response = new GetAllServicesDto.Response();
        
        try {
            List<String> services;
            
            // 根据参数选择查询方式
            if (request.getGroupName() != null && !request.getGroupName().isEmpty()) {
                services = serviceDiscovery.getServices(request.getGroupName());
            } else if (request.getPageNo() != null && request.getPageSize() != null) {
                services = serviceDiscovery.getServices(request.getPageNo(), request.getPageSize());
            } else {
                services = serviceDiscovery.getServices();
            }
            
            response.setServices(services);
            response.setTotal(services.size());
            
            log.info("查询所有服务成功: count={}", services.size());
            
        } catch (ServiceDiscoveryException e) {
            log.error("查询所有服务失败", e);
            response.setServices(List.of());
            response.setTotal(0);
        }
        
        return response;
    }
    
    @Override
    public SubscribeServiceDto.Response subscribeService(SubscribeServiceDto.Request request) {
        SubscribeServiceDto.Response response = new SubscribeServiceDto.Response();
        
        try {
            String key = request.getServiceName() + "#" + 
                        (request.getGroupName() != null ? request.getGroupName() : "DEFAULT");
            
            // 检查是否已订阅
            if (subscriptionCache.containsKey(key)) {
                response.setSuccess(true);
                response.setMessage("已经订阅过该服务");
                return response;
            }
            
            // 订阅服务变化
            if (request.getGroupName() != null && !request.getGroupName().isEmpty()) {
                serviceDiscovery.subscribe(
                        request.getServiceName(), 
                        request.getGroupName(),
                        (serviceName, instances) -> {
                            log.info("服务变化通知: serviceName={}, instanceCount={}", 
                                    serviceName, instances.size());
                            // 这里可以添加更多的处理逻辑
                        }
                );
            } else {
                serviceDiscovery.subscribe(
                        request.getServiceName(),
                        (serviceName, instances) -> {
                            log.info("服务变化通知: serviceName={}, instanceCount={}", 
                                    serviceName, instances.size());
                            // 这里可以添加更多的处理逻辑
                        }
                );
            }
            
            // 记录订阅
            subscriptionCache.put(key, Boolean.TRUE);
            
            response.setSuccess(true);
            response.setMessage("订阅服务变化成功");
            
            log.info("订阅服务变化成功: serviceName={}, groupName={}", 
                    request.getServiceName(), request.getGroupName());
            
        } catch (ServiceDiscoveryException e) {
            log.error("订阅服务变化失败", e);
            response.setSuccess(false);
            response.setMessage("订阅服务变化失败: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public SubscribeServiceDto.Response unsubscribeService(SubscribeServiceDto.Request request) {
        SubscribeServiceDto.Response response = new SubscribeServiceDto.Response();
        
        try {
            String key = request.getServiceName() + "#" + 
                        (request.getGroupName() != null ? request.getGroupName() : "DEFAULT");
            
            // 取消订阅
            if (request.getGroupName() != null && !request.getGroupName().isEmpty()) {
                serviceDiscovery.unsubscribe(request.getServiceName(), request.getGroupName());
            } else {
                serviceDiscovery.unsubscribe(request.getServiceName());
            }
            
            // 移除订阅记录
            subscriptionCache.remove(key);
            
            response.setSuccess(true);
            response.setMessage("取消订阅服务变化成功");
            
            log.info("取消订阅服务变化成功: serviceName={}, groupName={}", 
                    request.getServiceName(), request.getGroupName());
            
        } catch (ServiceDiscoveryException e) {
            log.error("取消订阅服务变化失败", e);
            response.setSuccess(false);
            response.setMessage("取消订阅服务变化失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 转换ServiceInstance为VO
     */
    private ServiceInstanceVo convertToVo(ServiceInstance instance) {
        ServiceInstanceVo vo = new ServiceInstanceVo();
        BeanUtils.copyProperties(instance, vo);
        vo.setAddress(instance.getAddress());
        vo.setAvailable(instance.isAvailable());
        return vo;
    }
}


