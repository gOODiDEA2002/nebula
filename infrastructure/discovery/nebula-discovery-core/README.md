# Nebula Discovery Core

> Nebula 框架的服务发现核心抽象层，提供统一的服务注册与发现接口，支持多种服务发现实现

## 模块概述

`nebula-discovery-core` 是 Nebula 框架服务发现功能的核心抽象层，定义了服务注册发现订阅等标准接口该模块不依赖于特定的服务发现实现（如 NacosConsulEureka 等），为微服务架构提供了统一的服务治理能力

## 核心特性

- 统一的服务注册和发现接口
- 多种负载均衡策略（轮询随机加权等）
- 服务健康检查
- 服务变更监听和订阅
- 服务分组和集群管理
- 异步操作支持
- 扩展性强，易于集成不同的服务发现中间件

## 核心组件

### 1. ServiceInstance 服务实例

服务实例信息的统一模型：

```java
@Data
@Builder
public class ServiceInstance {
    private String serviceName;      // 服务名称
    private String instanceId;       // 实例ID
    private String ip;               // IP地址
    private int port;                // 端口号
    private double weight;           // 权重
    private boolean healthy;         // 是否健康
    private boolean enabled;         // 是否启用
    private String clusterName;      // 集群名称
    private String groupName;        // 分组名称
    private Map<String, String> metadata;  // 元数据
    private long registerTime;       // 注册时间
    private long lastHeartbeat;      // 最后心跳时间
    private String version;          // 服务版本
    private String protocol;         // 服务协议 (http, https, grpc等)
    
    // 工具方法
    public String getAddress() { ... }     // 获取完整地址: protocol://ip:port
    public String getUri() { ... }         // 获取URI: ip:port
    public boolean isAvailable() { ... }   // 检查是否可用
    public String getMetadata(String key) { ... }  // 获取元数据
}
```

### 2. ServiceDiscovery 服务发现接口

核心服务发现接口，定义了所有服务治理功能：

```java
public interface ServiceDiscovery {
    
    // 注册和注销
    void register(ServiceInstance instance) throws ServiceDiscoveryException;
    CompletableFuture<Void> registerAsync(ServiceInstance instance);
    void deregister(String serviceName, String instanceId) throws ServiceDiscoveryException;
    
    // 查询服务实例
    List<ServiceInstance> getInstances(String serviceName) throws ServiceDiscoveryException;
    List<ServiceInstance> getInstances(String serviceName, boolean healthyOnly) throws ServiceDiscoveryException;
    List<ServiceInstance> getInstances(String serviceName, String groupName) throws ServiceDiscoveryException;
    List<ServiceInstance> getInstances(String serviceName, List<String> clusters) throws ServiceDiscoveryException;
    
    // 服务订阅
    void subscribe(String serviceName, ServiceChangeListener listener) throws ServiceDiscoveryException;
    void subscribe(String serviceName, String groupName, ServiceChangeListener listener) throws ServiceDiscoveryException;
    void unsubscribe(String serviceName) throws ServiceDiscoveryException;
    
    // 服务列表
    List<String> getServices() throws ServiceDiscoveryException;
    List<String> getServices(int pageNo, int pageSize) throws ServiceDiscoveryException;
    boolean existsService(String serviceName) throws ServiceDiscoveryException;
    
    // 实例管理
    void updateInstance(ServiceInstance instance) throws ServiceDiscoveryException;
    void heartbeat(String serviceName, String instanceId) throws ServiceDiscoveryException;
    void shutdown();
}
```

### 3. LoadBalancer 负载均衡器

选择服务实例的策略接口：

```java
public interface LoadBalancer {
    
    /**
     * 从服务实例列表中选择一个实例
     */
    ServiceInstance choose(List<ServiceInstance> instances);
    
    /**
     * 从服务实例列表中选择一个实例（带上下文）
     */
    ServiceInstance choose(List<ServiceInstance> instances, LoadBalanceContext context);
}
```

### 4. 内置负载均衡策略

#### 轮询负载均衡（RoundRobin）

```java
public class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        // 过滤出可用实例
        List<ServiceInstance> availableInstances = instances.stream()
            .filter(ServiceInstance::isAvailable)
            .toList();
        
        if (availableInstances.isEmpty()) {
            return null;
        }
        
        // 轮询选择
        int index = Math.abs(counter.getAndIncrement()) % availableInstances.size();
        return availableInstances.get(index);
    }
}
```

#### 随机负载均衡（Random）

```java
public class RandomLoadBalancer implements LoadBalancer {
    
    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        List<ServiceInstance> availableInstances = instances.stream()
            .filter(ServiceInstance::isAvailable)
            .toList();
        
        if (availableInstances.isEmpty()) {
            return null;
        }
        
        int index = ThreadLocalRandom.current().nextInt(availableInstances.size());
        return availableInstances.get(index);
    }
}
```

#### 加权随机负载均衡（WeightedRandom）

```java
public class WeightedRandomLoadBalancer implements LoadBalancer {
    
    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        List<ServiceInstance> availableInstances = instances.stream()
            .filter(ServiceInstance::isAvailable)
            .toList();
        
        if (availableInstances.isEmpty()) {
            return null;
        }
        
        // 计算总权重
        double totalWeight = availableInstances.stream()
            .mapToDouble(ServiceInstance::getWeight)
            .sum();
        
        // 加权随机选择
        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        double currentWeight = 0;
        
        for (ServiceInstance instance : availableInstances) {
            currentWeight += instance.getWeight();
            if (random <= currentWeight) {
                return instance;
            }
        }
        
        return availableInstances.get(availableInstances.size() - 1);
    }
}
```

### 5. ServiceChangeListener 服务变更监听器

```java
public interface ServiceChangeListener {
    
    /**
     * 服务实例变化时的回调
     * 
     * @param serviceName 服务名称
     * @param instances 当前服务实例列表
     */
    void onServiceChange(String serviceName, List<ServiceInstance> instances);
}
```

### 6. HealthChecker 健康检查器

```java
public interface HealthChecker {
    
    /**
     * 检查服务实例是否健康
     * 
     * @param instance 服务实例
     * @return 是否健康
     */
    boolean check(ServiceInstance instance);
    
    /**
     * 批量检查服务实例健康状态
     * 
     * @param instances 服务实例列表
     * @return 健康的实例列表
     */
    default List<ServiceInstance> checkAll(List<ServiceInstance> instances) {
        return instances.stream()
            .filter(this::check)
            .collect(Collectors.toList());
    }
}
```

## 使用示例

### 1. 注册服务

```java
@Service
public class ServiceRegistrationService {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @PostConstruct
    public void registerService() {
        // 构建服务实例
        ServiceInstance instance = ServiceInstance.builder()
            .serviceName("user-service")
            .instanceId(UUID.randomUUID().toString())
            .ip("192.168.1.100")
            .port(8080)
            .weight(1.0)
            .healthy(true)
            .enabled(true)
            .clusterName("default")
            .groupName("production")
            .protocol("http")
            .version("1.0.0")
            .metadata(Map.of(
                "region", "cn-hangzhou",
                "zone", "zone-a",
                "environment", "prod"
            ))
            .registerTime(System.currentTimeMillis())
            .build();
        
        try {
            // 同步注册
            serviceDiscovery.register(instance);
            log.info("服务注册成功: {}", instance.getServiceName());
            
            // 或者异步注册
            serviceDiscovery.registerAsync(instance)
                .thenRun(() -> log.info("异步注册成功"))
                .exceptionally(ex -> {
                    log.error("异步注册失败", ex);
                    return null;
                });
        } catch (ServiceDiscoveryException e) {
            log.error("服务注册失败", e);
        }
    }
    
    @PreDestroy
    public void deregisterService() {
        try {
            serviceDiscovery.deregister("user-service", instanceId);
            log.info("服务注销成功");
        } catch (ServiceDiscoveryException e) {
            log.error("服务注销失败", e);
        }
    }
}
```

### 2. 发现服务

```java
@Service
public class OrderService {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @Autowired
    private LoadBalancer loadBalancer;
    
    public void callUserService(Long userId) {
        try {
            // 获取所有健康的服务实例
            List<ServiceInstance> instances = serviceDiscovery.getInstances("user-service");
            
            if (instances.isEmpty()) {
                throw new BusinessException("用户服务不可用");
            }
            
            // 使用负载均衡选择一个实例
            ServiceInstance instance = loadBalancer.choose(instances);
            
            if (instance == null) {
                throw new BusinessException("没有可用的用户服务实例");
            }
            
            // 调用服务
            String url = instance.getAddress() + "/api/users/" + userId;
            log.info("调用用户服务: {}", url);
            
            // 发起HTTP请求...
            
        } catch (ServiceDiscoveryException e) {
            log.error("服务发现失败", e);
            throw new BusinessException("服务调用失败");
        }
    }
    
    // 获取指定分组的服务实例
    public void callUserServiceByGroup(String groupName) {
        try {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(
                "user-service", 
                groupName
            );
            
            // 选择实例并调用...
            
        } catch (ServiceDiscoveryException e) {
            log.error("服务发现失败", e);
        }
    }
    
    // 获取指定集群的服务实例
    public void callUserServiceByClusters() {
        try {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(
                "user-service", 
                Arrays.asList("cluster-a", "cluster-b")
            );
            
            // 选择实例并调用...
            
        } catch (ServiceDiscoveryException e) {
            log.error("服务发现失败", e);
        }
    }
}
```

### 3. 订阅服务变更

```java
@Component
public class ServiceSubscriber {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @PostConstruct
    public void subscribeServices() {
        try {
            // 订阅用户服务变更
            serviceDiscovery.subscribe("user-service", new ServiceChangeListener() {
                @Override
                public void onServiceChange(String serviceName, List<ServiceInstance> instances) {
                    log.info("服务 {} 实例变更，当前实例数: {}", serviceName, instances.size());
                    
                    // 更新本地缓存
                    updateLocalCache(serviceName, instances);
                    
                    // 通知其他组件
                    notifyServiceChange(serviceName, instances);
                }
            });
            
            // 订阅指定分组的服务变更
            serviceDiscovery.subscribe("product-service", "production", (serviceName, instances) -> {
                log.info("生产环境的产品服务变更: {} 个实例", instances.size());
            });
            
        } catch (ServiceDiscoveryException e) {
            log.error("订阅服务失败", e);
        }
    }
    
    @PreDestroy
    public void unsubscribeServices() {
        try {
            serviceDiscovery.unsubscribe("user-service");
            serviceDiscovery.unsubscribe("product-service");
        } catch (ServiceDiscoveryException e) {
            log.error("取消订阅失败", e);
        }
    }
    
    private void updateLocalCache(String serviceName, List<ServiceInstance> instances) {
        // 更新本地服务实例缓存
    }
    
    private void notifyServiceChange(String serviceName, List<ServiceInstance> instances) {
        // 通知其他组件服务变更
    }
}
```

### 4. 健康检查

```java
@Component
public class CustomHealthChecker implements HealthChecker {
    
    private final RestTemplate restTemplate;
    
    @Override
    public boolean check(ServiceInstance instance) {
        try {
            String healthUrl = instance.getAddress() + "/actuator/health";
            
            // 发送健康检查请求
            ResponseEntity<Map> response = restTemplate.getForEntity(
                healthUrl, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                return "UP".equals(body.get("status"));
            }
            
            return false;
        } catch (Exception e) {
            log.warn("健康检查失败: {}:{}", instance.getIp(), instance.getPort());
            return false;
        }
    }
}

// 使用健康检查器
@Service
public class ServiceMonitor {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @Autowired
    private HealthChecker healthChecker;
    
    @Scheduled(fixedRate = 30000) // 每30秒检查一次
    public void checkServiceHealth() {
        try {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(
                "user-service", 
                false  // 获取所有实例，包括不健康的
            );
            
            // 执行健康检查
            List<ServiceInstance> healthyInstances = healthChecker.checkAll(instances);
            
            log.info("用户服务健康实例数: {}/{}", 
                healthyInstances.size(), 
                instances.size()
            );
            
        } catch (ServiceDiscoveryException e) {
            log.error("健康检查失败", e);
        }
    }
}
```

### 5. 负载均衡策略

```java
@Configuration
public class LoadBalancerConfig {
    
    // 配置轮询负载均衡
    @Bean
    @ConditionalOnProperty(name = "nebula.loadbalancer.strategy", havingValue = "round-robin")
    public LoadBalancer roundRobinLoadBalancer() {
        return new RoundRobinLoadBalancer();
    }
    
    // 配置随机负载均衡
    @Bean
    @ConditionalOnProperty(name = "nebula.loadbalancer.strategy", havingValue = "random")
    public LoadBalancer randomLoadBalancer() {
        return new RandomLoadBalancer();
    }
    
    // 配置加权随机负载均衡
    @Bean
    @ConditionalOnProperty(name = "nebula.loadbalancer.strategy", havingValue = "weighted-random")
    public LoadBalancer weightedRandomLoadBalancer() {
        return new WeightedRandomLoadBalancer();
    }
    
    // 默认策略
    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer defaultLoadBalancer() {
        return new RoundRobinLoadBalancer();
    }
}

// 使用负载均衡
@Service
public class RpcClient {
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @Autowired
    private LoadBalancer loadBalancer;
    
    public <T> T call(String serviceName, Function<String, T> invoker) {
        try {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
            ServiceInstance instance = loadBalancer.choose(instances);
            
            if (instance == null) {
                throw new BusinessException("没有可用的服务实例");
            }
            
            String url = instance.getAddress();
            return invoker.apply(url);
            
        } catch (ServiceDiscoveryException e) {
            throw new BusinessException("服务调用失败", e);
        }
    }
}
```

## 实现指南

### 实现自定义服务发现

要为 Nebula 添加新的服务发现实现（如 ConsulEurekaEtcd 等），需要实现 `ServiceDiscovery` 接口：

```java
public class ConsulServiceDiscovery implements ServiceDiscovery {
    
    private final ConsulClient consulClient;
    
    @Override
    public void register(ServiceInstance instance) throws ServiceDiscoveryException {
        try {
            // 转换为 Consul 的服务注册对象
            NewService service = new NewService();
            service.setId(instance.getInstanceId());
            service.setName(instance.getServiceName());
            service.setAddress(instance.getIp());
            service.setPort(instance.getPort());
            service.setMeta(instance.getMetadata());
            
            // 添加健康检查
            NewService.Check check = new NewService.Check();
            check.setHttp(instance.getAddress() + "/actuator/health");
            check.setInterval("10s");
            service.setCheck(check);
            
            // 注册到 Consul
            consulClient.agentServiceRegister(service);
            
        } catch (Exception e) {
            throw new ServiceDiscoveryException("注册服务失败", e);
        }
    }
    
    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws ServiceDiscoveryException {
        try {
            Response<List<HealthService>> response = consulClient.getHealthServices(
                serviceName, 
                true,  // 只获取健康实例
                QueryParams.DEFAULT
            );
            
            return response.getValue().stream()
                .map(this::convertToServiceInstance)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            throw new ServiceDiscoveryException("获取服务实例失败", e);
        }
    }
    
    private ServiceInstance convertToServiceInstance(HealthService healthService) {
        Service service = healthService.getService();
        return ServiceInstance.builder()
            .serviceName(service.getService())
            .instanceId(service.getId())
            .ip(service.getAddress())
            .port(service.getPort())
            .metadata(service.getMeta())
            .healthy(true)
            .enabled(true)
            .build();
    }
    
    // 实现其他方法...
}

// 自动配置
@Configuration
@ConditionalOnClass(ConsulClient.class)
@EnableConfigurationProperties(ConsulProperties.class)
public class ConsulDiscoveryAutoConfiguration {
    
    @Bean
    public ConsulClient consulClient(ConsulProperties properties) {
        return new ConsulClient(properties.getHost(), properties.getPort());
    }
    
    @Bean
    public ServiceDiscovery consulServiceDiscovery(ConsulClient consulClient) {
        return new ConsulServiceDiscovery(consulClient);
    }
}
```

## 最佳实践

### 1. 服务注册

- 使用有意义的服务名称和实例ID
- 合理设置服务权重，实现流量控制
- 及时更新服务健康状态
- 在服务关闭时注销实例

### 2. 服务发现

- 缓存服务实例列表，减少注册中心压力
- 使用服务订阅机制，及时感知变更
- 实现故障转移和重试机制
- 合理选择负载均衡策略

### 3. 健康检查

- 提供轻量级的健康检查端点
- 设置合理的检查间隔
- 区分服务启动和服务健康
- 支持优雅下线

### 4. 元数据使用

- 使用元数据传递版本信息
- 使用元数据实现灰度发布
- 使用元数据进行流量染色
- 避免在元数据中存储敏感信息

## 依赖说明

```xml
<dependencies>
    <!-- Nebula Foundation -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-foundation</artifactId>
    </dependency>
</dependencies>
```

## 相关模块

- [nebula-discovery-nacos](../nebula-discovery-nacos/README.md) - Nacos 实现
- [nebula-rpc-core](../../rpc/nebula-rpc-core/README.md) - RPC 核心
- [nebula-foundation](../../../core/nebula-foundation/README.md) - 基础工具

## 版本要求

- Java 21+
- Spring Boot 3.x
- Maven 3.6+

## 许可证

Apache License 2.0

---

**Nebula Discovery Core** - 构建可靠微服务治理的基石

