# Nebula Discovery Nacos 配置指南

> Nacos服务发现配置说明

## 概述

`nebula-discovery-nacos` 提供基于 Nacos 的服务注册与发现。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### Nacos配置

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: ${NACOS_ADDR:localhost:8848}
      namespace: ${NACOS_NAMESPACE:public}
      group: DEFAULT_GROUP
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:nacos}
```

## 票务系统场景

### 服务注册

```yaml
spring:
  application:
    name: order-service

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: nacos-server:8848
      namespace: ticket-prod
      # 服务权重
      weight: 1.0
      # 集群名称
      cluster-name: DEFAULT
      # 是否注册为临时实例
      ephemeral: true
```

### 服务发现

```java
@Service
public class OrderService {
    
    @Autowired
    private NacosDiscoveryClient discoveryClient;
    
    public List<ServiceInstance> getUserServiceInstances() {
        return discoveryClient.getInstances("user-service");
    }
}
```

### 配置中心

```yaml
nebula:
  discovery:
    nacos:
      config:
        enabled: true
        server-addr: nacos-server:8848
        namespace: ticket-prod
        # 配置文件ID
        data-id: order-service.yml
        # 配置组
        group: DEFAULT_GROUP
        # 配置格式
        file-extension: yml
        # 自动刷新
        refresh-enabled: true
```

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

