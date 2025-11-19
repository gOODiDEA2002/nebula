# nebula-discovery-nacos 模块示例

## 模块简介

`nebula-discovery-nacos` 是 Nebula 框架基于 Alibaba Nacos 实现的服务发现模块。它实现了 `nebula-discovery-core` 的标准接口，支持服务的自动注册、心跳维持、实时订阅感知等功能。

## 核心功能示例

### 1. 配置 Nacos 服务发现

在 `application.yml` 中配置 Nacos 服务器地址和相关参数。

**`application.yml`**:

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: ${NACOS_SERVER:localhost:8848}
      namespace: "" # 命名空间ID，默认为空(public)
      group-name: DEFAULT_GROUP
      cluster-name: DEFAULT
      
      # 认证信息 (可选)
      username: ${NACOS_USERNAME:}
      password: ${NACOS_PASSWORD:}
      
      # 自动注册配置
      auto-register: true # 启动时自动注册当前服务
      weight: 1.0
      metadata:
        version: "1.0.0"
        region: "us-east"
      
      # 网络配置 (用于多网卡环境)
      preferred-networks:
        - "192.168."
        - "10.0."
      ignored-interfaces:
        - "docker0"
        - "veth.*"
```

### 2. 启动应用

引入模块后，Spring Boot 应用启动时会自动将自己注册到 Nacos。

**`io.nebula.example.nacos.ProviderApplication`**:

```java
package io.nebula.example.nacos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Nacos Provider!";
    }
}
```

### 3. 使用服务发现

可以直接注入 `ServiceDiscovery` 使用，或者配合 RPC 模块使用（RPC 模块会自动使用服务发现查找地址）。

**`io.nebula.example.nacos.ConsumerService`**:

```java
package io.nebula.example.nacos;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsumerService {

    private final ServiceDiscovery serviceDiscovery;
    private final RestTemplate restTemplate = new RestTemplate();

    public String callProvider() {
        // 手动查找服务
        List<ServiceInstance> instances = serviceDiscovery.getInstances("nacos-provider");
        if (instances.isEmpty()) {
            return "No provider available";
        }
        
        // 简单选择第一个实例
        ServiceInstance instance = instances.get(0);
        String url = "http://" + instance.getIp() + ":" + instance.getPort() + "/hello";
        
        return restTemplate.getForObject(url, String.class);
    }
}
```

## 进阶特性

### 1. 多网卡环境 IP 选择

在容器或多网卡环境中，正确上报 IP 至关重要。`nebula-discovery-nacos` 提供了灵活的 IP 选择策略：
- `preferred-networks`: 优先匹配指定网段的 IP。
- `ignored-interfaces`: 忽略指定名称的网卡（支持正则）。

### 2. NamingService 直接访问

如果需要使用 Nacos 原生 API 的某些高级特性，可以通过反射或 bean 获取底层的 `NamingService`，但在标准开发中建议使用 `ServiceDiscovery` 接口以保持抽象。

## 总结

`nebula-discovery-nacos` 提供了开箱即用的 Nacos 集成，使得微服务架构中的服务注册与发现变得简单且标准化。它处理了与 Nacos Server 的复杂交互，开发者只需关注业务逻辑。

