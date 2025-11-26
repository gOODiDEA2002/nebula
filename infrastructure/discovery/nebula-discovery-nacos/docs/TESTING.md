# nebula-discovery-nacos 模块单元测试清单

## 模块说明

Nacos服务发现实现模块，提供服务注册、发现、健康检查、服务订阅等功能。

## 核心功能

1. 服务注册（自动注册）
2. 服务发现（获取服务实例列表）
3. 服务订阅（监听服务变化）
4. 健康检查
5. 网络地址首选配置（preferred-networks）
6. 网络接口过滤（ignored-interfaces）

## 测试类清单

### 1. NacosServiceRegistryTest

**测试类路径**: `io.nebula.discovery.nacos.NacosServiceRegistry`  
**测试目的**: 验证服务注册到Nacos的功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testRegister() | register(ServiceInstance) | 测试服务注册 | NamingService |
| testRegisterWithMetadata() | register() | 测试带元数据的注册 | NamingService |
| testDeregister() | deregister(ServiceInstance) | 测试服务注销 | NamingService |
| testRegisterWithGroupName() | - | 测试指定分组注册 | NamingService |
| testRegisterWithNamespace() | - | 测试指定命名空间注册 | NamingService |

**测试数据准备**:
- Mock NamingService
- 准备ServiceInstance测试对象

**验证要点**:
- 服务名正确
- IP和端口正确
- 元数据正确传递
- 分组和命名空间正确

**Mock示例**:
```java
@Mock
private NamingService namingService;

@InjectMocks
private NacosServiceRegistry serviceRegistry;

@Test
void testRegister() throws Exception {
    ServiceInstance instance = ServiceInstance.builder()
        .serviceName("test-service")
        .ip("192.168.1.100")
        .port(8080)
        .metadata(Map.of("version", "1.0.0"))
        .build();
    
    doNothing().when(namingService).registerInstance(
        eq("test-service"),
        eq("DEFAULT_GROUP"),
        any(Instance.class)
    );
    
    serviceRegistry.register(instance);
    
    verify(namingService).registerInstance(
        eq("test-service"),
        eq("DEFAULT_GROUP"),
        argThat(inst -> 
            inst.getIp().equals("192.168.1.100") &&
            inst.getPort() == 8080
        )
    );
}
```

---

### 2. NacosServiceDiscoveryTest

**测试类路径**: `io.nebula.discovery.nacos.NacosServiceDiscovery`  
**测试目的**: 验证服务发现功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGetInstances() | getInstances(String) | 测试获取健康实例 | NamingService |
| testGetAllInstances() | getInstances(String, boolean) | 测试获取所有实例 | NamingService |
| testGetInstancesWithGroup() | getInstances(String, String) | 测试获取指定分组实例 | NamingService |
| testGetInstancesEmpty() | getInstances() | 测试没有实例的情况 | NamingService |

**测试数据准备**:
- Mock NamingService
- 准备模拟的Instance列表

**验证要点**:
- 获取的实例列表正确
- 健康实例过滤正确
- 分组参数正确传递

**Mock示例**:
```java
@Test
void testGetInstances() throws Exception {
    Instance instance1 = new Instance();
    instance1.setIp("192.168.1.100");
    instance1.setPort(8080);
    instance1.setHealthy(true);
    
    Instance instance2 = new Instance();
    instance2.setIp("192.168.1.101");
    instance2.setPort(8080);
    instance2.setHealthy(true);
    
    when(namingService.selectInstances(
        eq("test-service"),
        eq("DEFAULT_GROUP"),
        eq(true)
    )).thenReturn(List.of(instance1, instance2));
    
    List<ServiceInstance> instances = serviceDiscovery.getInstances("test-service");
    
    assertThat(instances).hasSize(2);
    assertThat(instances.get(0).getIp()).isEqualTo("192.168.1.100");
}
```

---

### 3. NacosServiceSubscriptionTest

**测试类路径**: `io.nebula.discovery.nacos.NacosServiceDiscovery`  
**测试目的**: 验证服务订阅和监听功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSubscribe() | subscribe(String, EventListener) | 测试订阅服务 | NamingService |
| testUnsubscribe() | unsubscribe(String, EventListener) | 测试取消订阅 | NamingService |
| testOnServiceChange() | - | 测试服务变化通知 | NamingService |

**测试数据准备**:
- Mock NamingService
- 创建EventListener

**验证要点**:
- 订阅正确添加
- 监听器被正确调用
- 取消订阅生效

**Mock示例**:
```java
@Test
void testSubscribe() throws Exception {
    AtomicBoolean called = new AtomicBoolean(false);
    
    EventListener listener = event -> {
        called.set(true);
    };
    
    doNothing().when(namingService).subscribe(
        eq("test-service"),
        eq("DEFAULT_GROUP"),
        any(EventListener.class)
    );
    
    serviceDiscovery.subscribe("test-service", listener);
    
    verify(namingService).subscribe(
        eq("test-service"),
        eq("DEFAULT_GROUP"),
        any(EventListener.class)
    );
}
```

---

### 4. NetworkAddressResolverTest

**测试类路径**: `io.nebula.discovery.nacos.NetworkAddressResolver`  
**测试目的**: 验证网络地址解析和过滤功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testResolveWithPreferredNetworks() | resolveIpAddress() | 测试首选网络匹配 | NetworkInterface |
| testResolveWithIgnoredInterfaces() | resolveIpAddress() | 测试忽略网络接口 | NetworkInterface |
| testResolveDefault() | resolveIpAddress() | 测试默认地址解析 | - |
| testFilterLoopback() | - | 测试过滤回环接口 | NetworkInterface |
| testFilterVirtual() | - | 测试过滤虚拟接口 | NetworkInterface |

**测试数据准备**:
- Mock NetworkInterface和InetAddress
- 准备多个网络地址测试

**验证要点**:
- 首选网络优先选择
- 忽略的接口被过滤
- 回环和虚拟接口被过滤
- IPv4地址优先

**Mock示例**:
```java
@Test
void testResolveWithPreferredNetworks() {
    List<String> preferredNetworks = List.of("192.168");
    List<String> ignoredInterfaces = List.of("docker0");
    
    NetworkAddressResolver resolver = new NetworkAddressResolver(
        preferredNetworks,
        ignoredInterfaces
    );
    
    // 测试逻辑，验证192.168网段被优先选择
    String ip = resolver.resolveIpAddress();
    
    assertThat(ip).startsWith("192.168");
}
```

---

### 5. AutoServiceRegistrationTest

**测试类路径**: 自动服务注册功能测试  
**测试目的**: 验证应用启动时自动注册到Nacos

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testAutoRegisterOnStartup() | - | 测试启动时自动注册 | NamingService |
| testAutoRegisterWithMetadata() | - | 测试带元数据的自动注册 | NamingService |
| testAutoRegisterDisabled() | - | 测试禁用自动注册 | NamingService |

**测试数据准备**:
- 配置auto-register=true
- Mock Spring ApplicationContext

**验证要点**:
- 启动后自动注册
- 元数据正确设置
- auto-register=false时不注册

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| NamingService | 服务注册/发现 | Mock registerInstance(), selectInstances() |
| NetworkInterface | 网络地址解析 | Mock getNetworkInterfaces() |
| InetAddress | IP地址获取 | Mock getHostAddress() |
| ApplicationContext | 自动注册 | Mock getBean() |

### 不需要真实Nacos
**所有测试都应该Mock NamingService，不需要启动真实的Nacos服务器**。

---

## 测试依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.alibaba.nacos</groupId>
        <artifactId>nacos-client</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

```bash
mvn test -pl nebula/infrastructure/discovery/nebula-discovery-nacos
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- 服务注册和发现测试通过
- 网络地址解析测试通过
- 服务订阅测试通过

