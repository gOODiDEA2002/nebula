# nebula-discovery-core 模块单元测试清单

## 模块说明

服务发现核心抽象层，定义了服务注册、发现、订阅、健康检查和负载均衡的标准接口。

## 核心功能

1. 服务实例模型（ServiceInstance）
2. 服务发现接口（ServiceDiscovery）
3. 负载均衡策略（LoadBalancer）
4. 服务变更监听（ServiceChangeListener）

## 测试类清单

### 1. ServiceInstanceTest

**测试类路径**: `io.nebula.discovery.core.ServiceInstance`  
**测试目的**: 验证服务实例模型的构建和辅助方法

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testBuilder() | builder() | 验证Builder模式构建 |
| testGetAddress() | getAddress() | 验证地址拼接 |
| testMetadata() | getMetadata() | 验证元数据操作 |

### 2. LoadBalancerTest

**测试类路径**: `io.nebula.discovery.core.loadbalance` 包下的实现类  
**测试目的**: 验证负载均衡算法

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testRoundRobin() | RoundRobinLoadBalancer.choose() | 验证轮询算法 |
| testRandom() | RandomLoadBalancer.choose() | 验证随机算法 |
| testWeightedRandom() | WeightedRandomLoadBalancer.choose() | 验证加权随机算法 |
| testEmptyList() | choose() | 验证空列表处理 |

### 3. ServiceDiscoveryInterfaceTest

**测试类路径**: `io.nebula.discovery.core.ServiceDiscovery`  
**测试目的**: 验证接口定义的完整性（通常结合Mock实现测试）

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testRegister() | register() | 验证注册流程 | ServiceDiscovery实现类 |
| testGetInstances() | getInstances() | 验证实例获取 | ServiceDiscovery实现类 |

## 测试执行

```bash
mvn test -pl nebula/infrastructure/discovery/nebula-discovery-core
```

## 验收标准

- 服务实例模型测试通过
- 核心负载均衡算法测试通过
- 边界条件（如空列表、单实例）测试覆盖

