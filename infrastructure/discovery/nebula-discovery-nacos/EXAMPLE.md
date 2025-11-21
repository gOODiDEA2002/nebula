# Nebula Discovery Nacos - 使用示例

> Nacos服务注册发现完整使用指南，以票务微服务为例

## 目录

- [快速开始](#快速开始)
- [服务注册](#服务注册)
- [服务发现](#服务发现)
- [服务订阅](#服务订阅)
- [健康检查](#健康检查)
- [元数据管理](#元数据管理)
- [多环境配置](#多环境配置)
- [与RPC集成](#与rpc集成)
- [负载均衡](#负载均衡)
- [票务微服务完整示例](#票务微服务完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
spring:
  application:
    name: user-service  # 服务名称

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848  # Nacos服务器地址
      namespace: ticket-system     # 命名空间
      group-name: DEFAULT_GROUP    # 分组
      cluster-name: DEFAULT        # 集群名称
      
      # 自动注册
      auto-register: true
      weight: 1.0
```

### 启动应用

```java
@SpringBootApplication
@EnableDiscoveryClient  // 启用服务发现
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

---

## 服务注册

### 1. 自动注册

```java
/**
 * 服务提供者（自动注册）
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    
    public static void main(String[] args) {
        // 应用启动时自动注册到Nacos
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### 2. 手动注册

```java
/**
 * 手动注册服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRegistrationService {
    
    private final ServiceRegistry serviceRegistry;
    
    /**
     * 注册服务实例
     */
    public void registerService() {
        ServiceInstance instance = ServiceInstance.builder()
                .serviceName("user-service")
                .ip("192.168.1.100")
                .port(8080)
                .weight(1.0)
                .healthy(true)
                .metadata(Map.of(
                        "version", "1.0.0",
                        "region", "us-east",
                        "env", "prod"
                ))
                .build();
        
        try {
            serviceRegistry.register(instance);
            log.info("服务注册成功：{}", instance);
        } catch (Exception e) {
            log.error("服务注册失败", e);
        }
    }
    
    /**
     * 注销服务实例
     */
    public void deregisterService() {
        ServiceInstance instance = ServiceInstance.builder()
                .serviceName("user-service")
                .ip("192.168.1.100")
                .port(8080)
                .build();
        
        try {
            serviceRegistry.deregister(instance);
            log.info("服务注销成功：{}", instance);
        } catch (Exception e) {
            log.error("服务注销失败", e);
        }
    }
}
```

### 3. 注册事件监听

```java
/**
 * 服务注册事件监听器
 */
@Component
@Slf4j
public class ServiceRegistrationListener {
    
    /**
     * 监听服务注册成功事件
     */
    @EventListener
    public void handleServiceRegistered(ServiceRegisteredEvent event) {
        ServiceInstance instance = event.getServiceInstance();
        
        log.info("服务注册成功：serviceName={}, ip={}, port={}", 
                instance.getServiceName(), instance.getIp(), instance.getPort());
        
        // 执行注册后的初始化操作
        // 如：预热缓存、初始化连接池等
    }
    
    /**
     * 监听服务注销事件
     */
    @EventListener
    public void handleServiceDeregistered(ServiceDeregisteredEvent event) {
        ServiceInstance instance = event.getServiceInstance();
        
        log.info("服务注销：serviceName={}, ip={}, port={}", 
                instance.getServiceName(), instance.getIp(), instance.getPort());
        
        // 执行注销前的清理操作
        // 如：关闭连接、释放资源等
    }
}
```

---

## 服务发现

### 1. 基础服务发现

```java
/**
 * 服务发现基础示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BasicServiceDiscoveryService {
    
    private final ServiceDiscovery serviceDiscovery;
    private final RestTemplate restTemplate;
    
    /**
     * 获取服务的所有实例
     */
    public List<ServiceInstance> getAllInstances(String serviceName) {
        try {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
            
            log.info("发现{}个{}服务实例", instances.size(), serviceName);
            
            return instances;
        } catch (Exception e) {
            log.error("服务发现失败：serviceName={}", serviceName, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取健康的服务实例
     */
    public List<ServiceInstance> getHealthyInstances(String serviceName) {
        List<ServiceInstance> allInstances = serviceDiscovery.getInstances(serviceName);
        
        return allInstances.stream()
                .filter(ServiceInstance::isHealthy)
                .collect(Collectors.toList());
    }
    
    /**
     * 调用远程服务（手动负载均衡）
     */
    public String callService(String serviceName, String path) {
        // 1. 获取健康的服务实例
        List<ServiceInstance> instances = getHealthyInstances(serviceName);
        
        if (instances.isEmpty()) {
            throw new BusinessException("没有可用的服务实例：" + serviceName);
        }
        
        // 2. 简单轮询负载均衡
        ServiceInstance instance = instances.get(
                ThreadLocalRandom.current().nextInt(instances.size()));
        
        // 3. 构建URL并调用
        String url = String.format("http://%s:%d%s", 
                instance.getIp(), instance.getPort(), path);
        
        log.info("调用远程服务：url={}", url);
        
        return restTemplate.getForObject(url, String.class);
    }
}
```

### 2. 按条件筛选服务

```java
/**
 * 服务筛选示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilteredServiceDiscoveryService {
    
    private final ServiceDiscovery serviceDiscovery;
    
    /**
     * 获取指定版本的服务实例
     */
    public List<ServiceInstance> getInstancesByVersion(String serviceName, String version) {
        List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
        
        return instances.stream()
                .filter(instance -> version.equals(instance.getMetadata().get("version")))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定区域的服务实例
     */
    public List<ServiceInstance> getInstancesByRegion(String serviceName, String region) {
        List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
        
        return instances.stream()
                .filter(instance -> region.equals(instance.getMetadata().get("region")))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定环境的服务实例
     */
    public List<ServiceInstance> getInstancesByEnv(String serviceName, String env) {
        List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
        
        return instances.stream()
                .filter(instance -> env.equals(instance.getMetadata().get("env")))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取权重最高的服务实例
     */
    public ServiceInstance getHighestWeightInstance(String serviceName) {
        List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
        
        return instances.stream()
                .max(Comparator.comparingDouble(ServiceInstance::getWeight))
                .orElse(null);
    }
}
```

### 3. 获取所有服务列表

```java
/**
 * 获取所有服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceListService {
    
    private final ServiceDiscovery serviceDiscovery;
    
    /**
     * 获取所有已注册的服务名称
     */
    public List<String> getAllServiceNames() {
        try {
            List<String> serviceNames = serviceDiscovery.getServices();
            
            log.info("发现{}个服务", serviceNames.size());
            
            return serviceNames;
        } catch (Exception e) {
            log.error("获取服务列表失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取所有服务及其实例信息
     */
    public Map<String, List<ServiceInstance>> getAllServicesWithInstances() {
        List<String> serviceNames = getAllServiceNames();
        
        Map<String, List<ServiceInstance>> result = new HashMap<>();
        
        for (String serviceName : serviceNames) {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
            result.put(serviceName, instances);
        }
        
        log.info("获取到{}个服务的实例信息", result.size());
        
        return result;
    }
}
```

---

## 服务订阅

### 1. 订阅服务变化

```java
/**
 * 服务订阅示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceSubscriptionService {
    
    private final ServiceDiscovery serviceDiscovery;
    
    /**
     * 订阅服务变化
     */
    public void subscribeService(String serviceName) {
        serviceDiscovery.subscribe(serviceName, event -> {
            log.info("服务{}发生变化：", serviceName);
            
            // 获取最新的服务实例列表
            List<ServiceInstance> instances = event.getInstances();
            
            log.info("当前服务实例数：{}", instances.size());
            
            for (ServiceInstance instance : instances) {
                log.info("  - {}:{}, 健康状态={}, 权重={}", 
                        instance.getIp(), instance.getPort(), 
                        instance.isHealthy(), instance.getWeight());
            }
            
            // 根据服务变化执行相应的操作
            handleServiceChange(serviceName, instances);
        });
        
        log.info("已订阅服务：{}", serviceName);
    }
    
    /**
     * 取消订阅
     */
    public void unsubscribeService(String serviceName) {
        serviceDiscovery.unsubscribe(serviceName);
        
        log.info("已取消订阅服务：{}", serviceName);
    }
    
    /**
     * 处理服务变化
     */
    private void handleServiceChange(String serviceName, List<ServiceInstance> instances) {
        // 1. 更新本地服务实例缓存
        updateLocalCache(serviceName, instances);
        
        // 2. 更新负载均衡器
        updateLoadBalancer(serviceName, instances);
        
        // 3. 发送服务变化事件
        publishServiceChangeEvent(serviceName, instances);
    }
    
    private void updateLocalCache(String serviceName, List<ServiceInstance> instances) {
        // 更新缓存逻辑
    }
    
    private void updateLoadBalancer(String serviceName, List<ServiceInstance> instances) {
        // 更新负载均衡器逻辑
    }
    
    private void publishServiceChangeEvent(String serviceName, List<ServiceInstance> instances) {
        // 发布事件逻辑
    }
}
```

### 2. 服务变化监听器

```java
/**
 * 服务变化监听器
 */
@Component
@Slf4j
public class ServiceChangeListener implements ApplicationListener<ServiceChangeEvent> {
    
    @Override
    public void onApplicationEvent(ServiceChangeEvent event) {
        String serviceName = event.getServiceName();
        List<ServiceInstance> instances = event.getInstances();
        ServiceChangeType changeType = event.getChangeType();
        
        log.info("服务变化通知：serviceName={}, changeType={}, instanceCount={}", 
                serviceName, changeType, instances.size());
        
        switch (changeType) {
            case ADDED:
                handleServiceAdded(serviceName, instances);
                break;
            case REMOVED:
                handleServiceRemoved(serviceName, instances);
                break;
            case MODIFIED:
                handleServiceModified(serviceName, instances);
                break;
        }
    }
    
    private void handleServiceAdded(String serviceName, List<ServiceInstance> instances) {
        log.info("新服务上线：{}", serviceName);
        // 处理新服务上线
    }
    
    private void handleServiceRemoved(String serviceName, List<ServiceInstance> instances) {
        log.info("服务下线：{}", serviceName);
        // 处理服务下线
    }
    
    private void handleServiceModified(String serviceName, List<ServiceInstance> instances) {
        log.info("服务信息变更：{}", serviceName);
        // 处理服务信息变更
    }
}
```

---

## 健康检查

### 1. 配置健康检查

```yaml
nebula:
  discovery:
    nacos:
      health-check:
        enabled: true
        type: http           # http | tcp
        path: /actuator/health
        interval: 5000       # 健康检查间隔（毫秒）
        timeout: 3000        # 超时时间（毫秒）
```

### 2. 自定义健康检查

```java
/**
 * 自定义健康检查
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 检查数据库连接
        if (!checkDatabaseConnection()) {
            return Health.down()
                    .withDetail("error", "数据库连接失败")
                    .build();
        }
        
        // 检查Redis连接
        if (!checkRedisConnection()) {
            return Health.down()
                    .withDetail("error", "Redis连接失败")
                    .build();
        }
        
        // 检查依赖服务
        if (!checkDependentServices()) {
            return Health.down()
                    .withDetail("error", "依赖服务不可用")
                    .build();
        }
        
        return Health.up()
                .withDetail("status", "所有检查通过")
                .build();
    }
    
    private boolean checkDatabaseConnection() {
        // 数据库连接检查逻辑
        return true;
    }
    
    private boolean checkRedisConnection() {
        // Redis连接检查逻辑
        return true;
    }
    
    private boolean checkDependentServices() {
        // 依赖服务检查逻辑
        return true;
    }
}
```

---

## 元数据管理

### 1. 配置元数据

```yaml
nebula:
  discovery:
    nacos:
      metadata:
        version: "1.0.0"       # 服务版本
        region: "us-east"      # 部署区域
        zone: "zone-a"         # 可用区
        env: "prod"            # 环境（dev/test/prod）
        weight: "100"          # 权重
        protocol: "http"       # 协议
        secure: "false"        # 是否HTTPS
```

### 2. 动态更新元数据

```java
/**
 * 元数据管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataManagementService {
    
    private final ServiceRegistry serviceRegistry;
    
    /**
     * 更新服务元数据
     */
    public void updateMetadata(String serviceName, Map<String, String> newMetadata) {
        ServiceInstance instance = getCurrentServiceInstance();
        
        // 更新元数据
        instance.setMetadata(newMetadata);
        
        try {
            // 重新注册服务（Nacos会更新元数据）
            serviceRegistry.register(instance);
            
            log.info("服务元数据已更新：serviceName={}, metadata={}", 
                    serviceName, newMetadata);
        } catch (Exception e) {
            log.error("更新服务元数据失败", e);
        }
    }
    
    /**
     * 动态调整服务权重
     */
    public void updateWeight(double newWeight) {
        ServiceInstance instance = getCurrentServiceInstance();
        
        instance.setWeight(newWeight);
        
        try {
            serviceRegistry.register(instance);
            
            log.info("服务权重已更新：weight={}", newWeight);
        } catch (Exception e) {
            log.error("更新服务权重失败", e);
        }
    }
    
    /**
     * 设置服务为下线状态（不注销，但标记为不健康）
     */
    public void markAsUnhealthy() {
        ServiceInstance instance = getCurrentServiceInstance();
        
        instance.setHealthy(false);
        
        try {
            serviceRegistry.register(instance);
            
            log.info("服务已标记为不健康");
        } catch (Exception e) {
            log.error("标记服务状态失败", e);
        }
    }
    
    private ServiceInstance getCurrentServiceInstance() {
        // 获取当前服务实例信息
        // 这里简化处理，实际应从配置或上下文获取
        return ServiceInstance.builder()
                .serviceName("user-service")
                .ip("192.168.1.100")
                .port(8080)
                .build();
    }
}
```

---

## 多环境配置

### 1. 开发环境

```yaml
# application-dev.yml
spring:
  application:
    name: user-service
  profiles:
    active: dev

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: dev           # 开发环境命名空间
      group-name: DEV_GROUP
      metadata:
        env: dev
        version: "1.0.0-SNAPSHOT"
```

### 2. 测试环境

```yaml
# application-test.yml
spring:
  application:
    name: user-service
  profiles:
    active: test

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: test-nacos.company.com:8848
      namespace: test          # 测试环境命名空间
      group-name: TEST_GROUP
      metadata:
        env: test
        version: "1.0.0-RC1"
```

### 3. 生产环境

```yaml
# application-prod.yml
spring:
  application:
    name: user-service
  profiles:
    active: prod

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: nacos-1.prod.company.com:8848,nacos-2.prod.company.com:8848  # 集群地址
      namespace: prod          # 生产环境命名空间
      group-name: PROD_GROUP
      cluster-name: PROD_CLUSTER
      username: ${NACOS_USERNAME}
      password: ${NACOS_PASSWORD}
      metadata:
        env: prod
        version: "1.0.0"
        region: "us-east"
```

---

## 与RPC集成

### 1. 配置RPC使用服务发现

```yaml
nebula:
  rpc:
    http:
      client:
        discovery:
          enabled: true
          type: nacos  # 使用Nacos服务发现
```

### 2. 使用服务名调用RPC

```java
/**
 * RPC客户端使用服务发现
 */
@RpcClient(name = "user-service")  // 使用服务名而非IP:PORT
public interface UserServiceClient extends UserService {
    // RPC客户端会自动从Nacos获取user-service的可用实例
}

/**
 * 订单服务调用用户服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final UserServiceClient userServiceClient;
    
    /**
     * 创建订单时调用用户服务
     */
    public String createOrder(CreateOrderRequest request) {
        // RPC调用会自动通过Nacos发现user-service的实例
        // 并使用负载均衡选择一个实例
        UserDTO user = userServiceClient.getUserById(request.getUserId());
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 订单创建逻辑
        // ...
        
        return "ORDER-" + System.currentTimeMillis();
    }
}
```

---

## 负载均衡

### 1. 内置负载均衡策略

```yaml
nebula:
  discovery:
    nacos:
      load-balancer:
        strategy: weighted-random  # 加权随机（默认）| round-robin | random
```

### 2. 自定义负载均衡器

```java
/**
 * 自定义负载均衡器
 */
@Component
public class CustomLoadBalancer implements LoadBalancer {
    
    /**
     * 基于响应时间的负载均衡
     */
    @Override
    public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }
        
        // 根据实例的平均响应时间选择（需要事先记录）
        return instances.stream()
                .min(Comparator.comparingLong(instance -> 
                        getAverageResponseTime(instance)))
                .orElse(instances.get(0));
    }
    
    private long getAverageResponseTime(ServiceInstance instance) {
        // 从监控系统获取实例的平均响应时间
        // 这里简化处理
        String responseTimeStr = instance.getMetadata().get("avgResponseTime");
        return responseTimeStr != null ? Long.parseLong(responseTimeStr) : Long.MAX_VALUE;
    }
}
```

---

## 票务微服务完整示例

### 架构图

```
[用户服务] ←→ [订单服务] ←→ [票务服务]
    ↓            ↓             ↓
       [Nacos服务注册中心]
```

### 1. 用户服务（user-service）

```yaml
# application.yml
spring:
  application:
    name: user-service
  
server:
  port: 8081

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: ticket-system
      metadata:
        version: "1.0.0"
        service-type: "user"
```

```java
/**
 * 用户服务应用
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

/**
 * 用户服务实现
 */
@Service
@RpcService
@Slf4j
public class UserServiceImpl implements UserService {
    
    @Override
    public UserDTO getUserById(Long userId) {
        log.info("用户服务：获取用户信息，userId={}", userId);
        // 业务逻辑
        return new UserDTO();
    }
}
```

### 2. 订单服务（order-service）

```yaml
# application.yml
spring:
  application:
    name: order-service
  
server:
  port: 8082

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: ticket-system
      metadata:
        version: "1.0.0"
        service-type: "order"
```

```java
/**
 * 订单服务应用
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableRpcClients  // 启用RPC客户端
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

/**
 * 订单服务（消费用户服务和票务服务）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final UserServiceClient userServiceClient;    // 通过Nacos发现
    private final TicketServiceClient ticketServiceClient; // 通过Nacos发现
    
    /**
     * 创建订单（跨服务调用）
     */
    public String createOrder(CreateOrderRequest request) {
        log.info("创建订单：userId={}, showtimeId={}", 
                request.getUserId(), request.getShowtimeId());
        
        // 1. 调用用户服务（自动通过Nacos发现）
        UserDTO user = userServiceClient.getUserById(request.getUserId());
        
        // 2. 调用票务服务（自动通过Nacos发现）
        ShowtimeDTO showtime = ticketServiceClient.getShowtimeById(request.getShowtimeId());
        
        // 3. 创建订单
        // ...
        
        return "ORDER-123";
    }
}
```

### 3. 票务服务（ticket-service）

```yaml
# application.yml
spring:
  application:
    name: ticket-service
  
server:
  port: 8083

nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: ticket-system
      metadata:
        version: "1.0.0"
        service-type: "ticket"
```

```java
/**
 * 票务服务应用
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TicketServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}

/**
 * 票务服务实现
 */
@Service
@RpcService
@Slf4j
public class TicketServiceImpl implements TicketService {
    
    @Override
    public ShowtimeDTO getShowtimeById(Long showtimeId) {
        log.info("票务服务：获取演出信息，showtimeId={}", showtimeId);
        // 业务逻辑
        return new ShowtimeDTO();
    }
}
```

### 4. 服务调用流程

```
1. 用户服务、订单服务、票务服务启动时，自动注册到Nacos
2. 订单服务通过 @RpcClient 注解声明对用户服务和票务服务的依赖
3. RPC框架自动从Nacos获取用户服务和票务服务的实例列表
4. 调用时使用负载均衡策略选择一个实例
5. 发起HTTP RPC调用
6. 如果服务实例发生变化，Nacos会推送变更通知，RPC框架自动更新实例列表
```

---

## 最佳实践

### 1. 服务命名规范

- **格式**：`{业务域}-service`
- **示例**：`user-service`、`order-service`、`ticket-service`
- **避免**：过长的名称、特殊字符

### 2. 命名空间隔离

- **环境隔离**：每个环境（dev/test/prod）使用独立的namespace
- **业务隔离**：不同业务系统使用不同的namespace
- **多租户隔离**：多租户场景使用不同的namespace

### 3. 分组管理

- **业务分组**：按业务域划分group（如 USER_GROUP、ORDER_GROUP）
- **版本分组**：按版本划分group（如 V1_GROUP、V2_GROUP）
- **环境分组**：按环境划分group（如 DEV_GROUP、PROD_GROUP）

### 4. 元数据设计

**必选元数据**：
- `version`: 服务版本
- `env`: 环境（dev/test/prod）

**可选元数据**：
- `region`: 部署区域
- `zone`: 可用区
- `protocol`: 协议类型
- `weight`: 权重

### 5. 健康检查

- **启用健康检查**：确保服务健康状态准确
- **合理的检查间隔**：5-10秒
- **超时设置**：不超过检查间隔的一半
- **多维度检查**：数据库、Redis、依赖服务

### 6. 服务下线

- **优雅下线**：先标记为不健康，等待请求处理完成后再注销
- **注销确认**：确保服务实例已从Nacos注销
- **清理资源**：关闭连接、释放资源

### 7. 监控和告警

- **服务注册监控**：监控服务注册成功率
- **服务可用性监控**：监控服务健康实例数
- **服务变更告警**：服务实例数变化时告警
- **心跳监控**：监控心跳失败率

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0
