# nebula-discovery-core 模块示例

## 模块简介

`nebula-discovery-core` 模块定义了 Nebula 框架的服务发现核心抽象。它提供了一套独立于具体实现的 API，用于服务的注册、发现、订阅和健康检查。

核心组件包括：
- **ServiceDiscovery**: 服务发现核心接口，定义了注册、注销、查询、订阅等行为。
- **ServiceInstance**: 服务实例模型，包含 IP、端口、元数据等信息。
- **ServiceChangeListener**: 服务变化监听器接口。
- **LoadBalancer**: 负载均衡接口（配合服务发现使用）。

## 核心功能示例

### 1. 获取服务列表

在消费者端，通过注入 `ServiceDiscovery` 接口来查找服务。

**`io.nebula.example.discovery.service.GatewayService`**:

```java
package io.nebula.example.discovery.service;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayService {

    private final ServiceDiscovery serviceDiscovery;

    public String routeRequest(String serviceName) {
        try {
            // 1. 获取指定服务的所有健康实例
            List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName, true);
            
            if (instances.isEmpty()) {
                throw new RuntimeException("服务不可用: " + serviceName);
            }
            
            // 2. 简单的负载均衡 (随机)
            ServiceInstance instance = instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
            
            String url = String.format("%s://%s:%d", instance.getProtocol(), instance.getIp(), instance.getPort());
            log.info("请求路由至: {}", url);
            return url;
            
        } catch (Exception e) {
            log.error("服务发现失败", e);
            throw new RuntimeException("服务路由失败", e);
        }
    }
    
    public List<String> listAllServices() {
        // 获取所有已注册的服务名
        return serviceDiscovery.getServices();
    }
}
```

### 2. 订阅服务变化

可以监听服务实例的上线和下线事件，通常用于更新本地缓存或负载均衡器列表。

**`io.nebula.example.discovery.listener.ServiceMonitor`**:

```java
package io.nebula.example.discovery.listener;

import io.nebula.discovery.core.ServiceChangeListener;
import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceMonitor {

    private final ServiceDiscovery serviceDiscovery;

    @PostConstruct
    public void init() {
        // 订阅 "order-service" 的变化
        serviceDiscovery.subscribe("order-service", new ServiceChangeListener() {
            @Override
            public void onServiceChange(String serviceName, List<ServiceInstance> instances) {
                log.info("服务 {} 发生变化，当前实例数: {}", serviceName, instances.size());
                instances.forEach(instance -> 
                    log.info(" - 实例: {}:{} (Healthy: {})", 
                        instance.getIp(), instance.getPort(), instance.isHealthy())
                );
            }
        });
    }
}
```

### 3. 手动注册/注销 (非 Spring Boot 自动场景)

虽然 Nebula 框架通常自动处理注册，但在特殊场景下可以手动控制。

```java
ServiceInstance instance = ServiceInstance.builder()
    .serviceName("custom-service")
    .instanceId("custom-1")
    .ip("192.168.1.100")
    .port(8080)
    .build();

// 异步注册
serviceDiscovery.registerAsync(instance).thenRun(() -> {
    log.info("注册成功");
});
```

## 总结

`nebula-discovery-core` 提供了服务发现的标准接口。要使其工作，必须引入具体的实现模块（如 `nebula-discovery-nacos`）。

