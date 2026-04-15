package io.nebula.example.modules.discovery.service;

import io.nebula.example.modules.discovery.entity.dto.*;

/**
 * 服务发现演示Service接口
 */
public interface DiscoveryDemoService {
    
    /**
     * 注册服务实例
     */
    RegisterServiceDto.Response registerService(RegisterServiceDto.Request request);
    
    /**
     * 注销服务实例
     */
    DeregisterServiceDto.Response deregisterService(DeregisterServiceDto.Request request);
    
    /**
     * 获取服务实例列表
     */
    GetServiceInstancesDto.Response getServiceInstances(GetServiceInstancesDto.Request request);
    
    /**
     * 获取所有服务列表
     */
    GetAllServicesDto.Response getAllServices(GetAllServicesDto.Request request);
    
    /**
     * 订阅服务变化
     */
    SubscribeServiceDto.Response subscribeService(SubscribeServiceDto.Request request);
    
    /**
     * 取消订阅服务变化
     */
    SubscribeServiceDto.Response unsubscribeService(SubscribeServiceDto.Request request);
}


