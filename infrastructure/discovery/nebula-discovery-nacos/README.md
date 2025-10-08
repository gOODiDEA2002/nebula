# Nebula Discovery Nacos 模块

## 模块简介

`nebula-discovery-nacos` 是 Nebula 框架的 Nacos 服务发现实现模块，基于阿里云 Nacos 提供完整的服务注册与发现功能。该模块实现了 `nebula-discovery-core` 的核心接口，提供统一的服务发现抽象。

## 功能特性

### 核心功能
- **服务注册**: 支持服务实例的注册和注销
- **服务发现**: 支持根据服务名称查询健康的服务实例
- **健康检查**: 自动心跳保持服务实例健康状态
- **服务订阅**: 支持订阅服务变化事件，实时感知服务实例变更
- **分组与集群**: 支持 Nacos 的分组和集群功能
- **负载均衡**: 集成多种负载均衡策略（轮询、随机、加权随机等）

### 增强特性
- **自动配置**: Spring Boot 自动配置，零配置启动
- **多租户支持**: 支持 Nacos 命名空间，实现多环境隔离
- **安全认证**: 支持用户名密码认证和 AccessKey/SecretKey 认证
- **元数据管理**: 支持为服务实例添加自定义元数据
- **异步操作**: 提供异步注册和注销接口

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 基础配置

在 `application.yml` 中配置 Nacos 服务发现：

```yaml
nebula:
  discovery:
    nacos:
      enabled: true                    # 启用 Nacos 服务发现
      server-addr: localhost:8848      # Nacos 服务器地址
      namespace: nebula-dev            # 命名空间ID
      group-name: DEFAULT_GROUP        # 分组名称
      cluster-name: DEFAULT            # 集群名称
      
      # 可选的认证配置
      username: nacos                  # Nacos 用户名
      password: nacos                  # Nacos 密码
      
      # 实例配置
      weight: 1.0                      # 服务权重（用于负载均衡）
      healthy: true                    # 是否健康
      instance-enabled: true           # 是否启用实例
      
      # 心跳配置
      heartbeat-interval: 5000         # 心跳间隔（毫秒）
      heartbeat-timeout: 15000         # 心跳超时时间（毫秒）
      
      # 元数据
      metadata:
        version: 1.0.0
        env: dev
```

## 基础使用

### 1. 注册服务实例

```java
@Service
public class MyServiceRegistry {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    public void registerService() throws ServiceDiscoveryException {
        ServiceInstance instance = ServiceInstance.builder()
                .serviceName("user-service")
                .instanceId("user-service-001")
                .ip("192.168.1.100")
                .port(8080)
                .weight(1.0)
                .healthy(true)
                .enabled(true)
                .clusterName("DEFAULT")
                .groupName("DEFAULT_GROUP")
                .protocol("http")
                .metadata(Map.of(
                    "version", "1.0.0",
                    "region", "cn-hangzhou"
                ))
                .build();
        
        serviceDiscovery.register(instance);
    }
}
```

### 2. 查询服务实例

```java
@Service
public class MyServiceConsumer {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    public void queryService() throws ServiceDiscoveryException {
        // 获取所有健康实例
        List<ServiceInstance> instances = serviceDiscovery.getInstances("user-service");
        
        // 获取指定分组的实例
        List<ServiceInstance> groupInstances = 
            serviceDiscovery.getInstances("user-service", "DEFAULT_GROUP");
        
        // 获取指定集群的实例
        List<ServiceInstance> clusterInstances = 
            serviceDiscovery.getInstances("user-service", List.of("cluster1", "cluster2"));
        
        // 遍历实例
        for (ServiceInstance instance : instances) {
            System.out.println("Instance: " + instance.getAddress());
        }
    }
}
```

### 3. 订阅服务变化

```java
@Service
public class ServiceWatcher {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @PostConstruct
    public void watchService() throws ServiceDiscoveryException {
        // 订阅服务变化
        serviceDiscovery.subscribe("user-service", (serviceName, instances) -> {
            System.out.println("服务 " + serviceName + " 发生变化");
            System.out.println("当前实例数: " + instances.size());
            
            for (ServiceInstance instance : instances) {
                System.out.println("  - " + instance.getAddress() + 
                                 " (健康: " + instance.isHealthy() + ")");
            }
        });
    }
}
```

### 4. 注销服务实例

```java
@Service
public class ServiceDeregistration {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @PreDestroy
    public void deregisterService() throws ServiceDiscoveryException {
        serviceDiscovery.deregister("user-service", "user-service-001");
    }
}
```

## 高级特性

### 负载均衡

框架内置了多种负载均衡策略：

```java
@Service
public class LoadBalancedServiceCaller {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    private final LoadBalancer loadBalancer = LoadBalancerFactory.create(LoadBalanceStrategy.ROUND_ROBIN);
    
    public void callService() throws ServiceDiscoveryException {
        // 获取服务实例列表
        List<ServiceInstance> instances = serviceDiscovery.getInstances("user-service");
        
        // 使用负载均衡选择一个实例
        ServiceInstance instance = loadBalancer.choose(instances);
        
        if (instance != null) {
            // 调用选中的实例
            String url = instance.getAddress() + "/api/users";
            // ... HTTP 调用
        }
    }
}
```

支持的负载均衡策略：
- `ROUND_ROBIN`: 轮询
- `RANDOM`: 随机
- `WEIGHTED_RANDOM`: 加权随机
- `LEAST_CONNECTIONS`: 最少连接（需配合连接跟踪）
- `CONSISTENT_HASH`: 一致性哈希

### 健康检查

```java
@Service
public class HealthCheckExample {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @Autowired
    private HealthChecker healthChecker;
    
    @Scheduled(fixedRate = 5000)
    public void checkHealth() throws ServiceDiscoveryException {
        List<ServiceInstance> instances = serviceDiscovery.getInstances("user-service", false);
        
        for (ServiceInstance instance : instances) {
            boolean healthy = healthChecker.check(instance);
            System.out.println(instance.getInstanceId() + " 健康状态: " + healthy);
        }
    }
}
```

### 异步操作

```java
@Service
public class AsyncServiceDiscovery {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    public CompletableFuture<Void> registerAsync() {
        ServiceInstance instance = ServiceInstance.builder()
                .serviceName("async-service")
                .instanceId("async-001")
                .ip("localhost")
                .port(8080)
                .build();
        
        return serviceDiscovery.registerAsync(instance)
                .thenRun(() -> System.out.println("注册成功"))
                .exceptionally(ex -> {
                    System.err.println("注册失败: " + ex.getMessage());
                    return null;
                });
    }
}
```

## 配置参考

### 完整配置示例

```yaml
nebula:
  discovery:
    nacos:
      # 基础配置
      enabled: true
      server-addr: localhost:8848
      namespace: ${NACOS_NAMESPACE:}
      group-name: ${NACOS_GROUP:DEFAULT_GROUP}
      cluster-name: ${NACOS_CLUSTER:DEFAULT}
      
      # 认证配置
      username: ${NACOS_USERNAME:}
      password: ${NACOS_PASSWORD:}
      access-key: ${NACOS_ACCESS_KEY:}
      secret-key: ${NACOS_SECRET_KEY:}
      
      # 实例配置
      auto-register: true              # 是否自动注册
      weight: 1.0                      # 权重
      healthy: true                    # 是否健康
      instance-enabled: true           # 是否启用
      
      # 心跳配置
      heartbeat-interval: 5000         # 心跳间隔（毫秒）
      heartbeat-timeout: 15000         # 心跳超时（毫秒）
      ip-delete-timeout: 30000         # IP删除超时（毫秒）
      
      # 元数据
      metadata:
        version: ${APP_VERSION:1.0.0}
        env: ${SPRING_PROFILES_ACTIVE:dev}
        zone: ${ZONE:cn-hangzhou}
```

### 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| enabled | boolean | true | 是否启用 Nacos 服务发现 |
| server-addr | String | localhost:8848 | Nacos 服务器地址 |
| namespace | String | "" | 命名空间ID，用于环境隔离 |
| group-name | String | DEFAULT_GROUP | 分组名称 |
| cluster-name | String | DEFAULT | 集群名称 |
| username | String | - | Nacos 用户名 |
| password | String | - | Nacos 密码 |
| access-key | String | - | 访问密钥（阿里云） |
| secret-key | String | - | 密钥（阿里云） |
| auto-register | boolean | true | 是否自动注册当前应用 |
| weight | double | 1.0 | 服务权重，用于负载均衡 |
| healthy | boolean | true | 是否健康 |
| instance-enabled | boolean | true | 是否启用实例 |
| heartbeat-interval | long | 5000 | 心跳间隔（毫秒） |
| heartbeat-timeout | long | 15000 | 心跳超时时间（毫秒） |
| ip-delete-timeout | long | 30000 | IP删除超时时间（毫秒） |
| metadata | Map | {} | 服务实例元数据 |

## 最佳实践

### 1. 命名规范

```yaml
# 服务命名建议
serviceName: user-service          # 小写，使用连字符
instanceId: user-service-001       # 服务名 + 实例编号

# 分组命名建议
groupName: PRODUCT_GROUP           # 大写，下划线分隔
clusterName: BEIJING_CLUSTER       # 大写，下划线分隔
```

### 2. 元数据使用

```java
// 推荐的元数据字段
Map<String, String> metadata = Map.of(
    "version", "1.0.0",              # 服务版本
    "env", "production",             # 环境
    "region", "cn-hangzhou",         # 地域
    "zone", "zone-a",                # 可用区
    "protocol", "http",              # 协议
    "warmup", "300"                  # 预热时间（秒）
);
```

### 3. 多环境配置

```yaml
# application-dev.yml
nebula:
  discovery:
    nacos:
      namespace: nebula-dev
      metadata:
        env: dev

# application-prod.yml
nebula:
  discovery:
    nacos:
      namespace: nebula-prod
      metadata:
        env: production
```

### 4. 错误处理

```java
@Service
public class RobustServiceDiscovery {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    public List<ServiceInstance> getInstancesWithRetry(String serviceName) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                return serviceDiscovery.getInstances(serviceName);
            } catch (ServiceDiscoveryException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("获取服务实例失败: {}", serviceName, e);
                    return Collections.emptyList();
                }
                
                try {
                    Thread.sleep(1000 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Collections.emptyList();
                }
            }
        }
        
        return Collections.emptyList();
    }
}
```

## 故障排查

### 常见问题

#### 1. 无法连接 Nacos 服务器

**现象**: 应用启动时报错 "Unable to connect to Nacos server"

**解决方案**:
- 检查 `server-addr` 配置是否正确
- 确认 Nacos 服务器是否启动
- 检查网络连接和防火墙设置
- 验证认证信息是否正确

#### 2. 服务注册失败

**现象**: 服务实例注册不成功

**解决方案**:
- 检查服务名称是否符合规范（不能包含特殊字符）
- 确认 IP 和端口是否正确
- 检查 namespace 和 groupName 配置
- 查看 Nacos 服务器日志

#### 3. 服务实例查询为空

**现象**: 查询服务实例返回空列表

**解决方案**:
- 确认服务确实已注册
- 检查 groupName 和 clusterName 是否一致
- 验证 namespace 配置
- 确认实例是否健康（healthyOnly 参数）

#### 4. 服务订阅不生效

**现象**: 服务变化监听器没有触发

**解决方案**:
- 确认订阅方法调用成功
- 检查监听器是否正确实现
- 验证服务名称是否正确
- 查看是否有异常日志

### 开启调试日志

```yaml
logging:
  level:
    io.nebula.discovery: DEBUG
    com.alibaba.nacos: DEBUG
```

## 性能优化

### 1. 本地缓存

```java
@Service
public class CachedServiceDiscovery {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    private final Cache<String, List<ServiceInstance>> cache = 
        CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();
    
    public List<ServiceInstance> getInstancesCached(String serviceName) {
        try {
            return cache.get(serviceName, () -> 
                serviceDiscovery.getInstances(serviceName)
            );
        } catch (ExecutionException e) {
            log.error("获取服务实例失败", e);
            return Collections.emptyList();
        }
    }
}
```

### 2. 批量查询

```java
// 使用并行流批量查询多个服务
List<String> serviceNames = Arrays.asList("user-service", "order-service", "payment-service");

Map<String, List<ServiceInstance>> allInstances = serviceNames.parallelStream()
    .collect(Collectors.toMap(
        name -> name,
        name -> {
            try {
                return serviceDiscovery.getInstances(name);
            } catch (ServiceDiscoveryException e) {
                return Collections.emptyList();
            }
        }
    ));
```

## 更多功能

- [Nebula Discovery Core 核心抽象](../nebula-discovery-core/README.md)
- [服务发现功能测试指南](../../../nebula-example/docs/nebula-discovery-test.md)
- [完整示例项目](../../../nebula-example)

## 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个模块。

## 许可证

本项目基于 Apache 2.0 许可证开源。


