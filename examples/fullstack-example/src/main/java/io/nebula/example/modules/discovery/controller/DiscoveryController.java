package io.nebula.example.modules.discovery.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.discovery.entity.dto.*;
import io.nebula.example.modules.discovery.service.DiscoveryDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.nebula.discovery.core.ServiceDiscovery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 服务发现演示控制器
 * 演示 Nebula 服务发现层的完整功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/discovery")
@RequiredArgsConstructor
@Validated
@Tag(name = "服务发现演示", description = "Nebula 服务发现功能演示API")
@ConditionalOnBean(ServiceDiscovery.class)
public class DiscoveryController {
    
    private final DiscoveryDemoService discoveryDemoService;
    
    @Operation(summary = "注册服务实例", description = "注册一个新的服务实例到注册中心")
    @PostMapping("/services/register")
    public Result<RegisterServiceDto.Response> registerService(
            @Valid @RequestBody RegisterServiceDto.Request request) {
        log.info("接收注册服务请求: serviceName={}, instanceId={}", 
                request.getServiceName(), request.getInstanceId());
        RegisterServiceDto.Response response = discoveryDemoService.registerService(request);
        return Result.success(response);
    }
    
    @Operation(summary = "注销服务实例", description = "从注册中心注销服务实例")
    @PostMapping("/services/deregister")
    public Result<DeregisterServiceDto.Response> deregisterService(
            @Valid @RequestBody DeregisterServiceDto.Request request) {
        log.info("接收注销服务请求: serviceName={}, instanceId={}", 
                request.getServiceName(), request.getInstanceId());
        DeregisterServiceDto.Response response = discoveryDemoService.deregisterService(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取服务实例列表", description = "根据服务名称获取所有服务实例")
    @PostMapping("/services/instances")
    public Result<GetServiceInstancesDto.Response> getServiceInstances(
            @Valid @RequestBody GetServiceInstancesDto.Request request) {
        log.info("接收查询服务实例请求: serviceName={}", request.getServiceName());
        GetServiceInstancesDto.Response response = discoveryDemoService.getServiceInstances(request);
        return Result.success(response, "查询服务实例成功");
    }
    
    @Operation(summary = "获取所有服务列表", description = "获取注册中心所有服务名称")
    @PostMapping("/services/all")
    public Result<GetAllServicesDto.Response> getAllServices(
            @Valid @RequestBody GetAllServicesDto.Request request) {
        log.info("接收查询所有服务请求");
        GetAllServicesDto.Response response = discoveryDemoService.getAllServices(request);
        return Result.success(response, "查询所有服务成功");
    }
    
    @Operation(summary = "订阅服务变化", description = "订阅服务实例变化事件")
    @PostMapping("/services/subscribe")
    public Result<SubscribeServiceDto.Response> subscribeService(
            @Valid @RequestBody SubscribeServiceDto.Request request) {
        log.info("接收订阅服务请求: serviceName={}", request.getServiceName());
        SubscribeServiceDto.Response response = discoveryDemoService.subscribeService(request);
        return Result.success(response);
    }
    
    @Operation(summary = "取消订阅服务变化", description = "取消订阅服务实例变化事件")
    @PostMapping("/services/unsubscribe")
    public Result<SubscribeServiceDto.Response> unsubscribeService(
            @Valid @RequestBody SubscribeServiceDto.Request request) {
        log.info("接收取消订阅服务请求: serviceName={}", request.getServiceName());
        SubscribeServiceDto.Response response = discoveryDemoService.unsubscribeService(request);
        return Result.success(response);
    }
}


