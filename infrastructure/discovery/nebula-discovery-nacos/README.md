# Nebula Discovery Nacos

Nacos 服务发现实现模块。

## 功能特性

- ✅ 服务注册与发现
- ✅ 自动服务注册
- ✅ 健康检查
- ✅ 服务订阅
- ✅ 多租户支持(命名空间、分组)
- ✅ 网络地址首选配置
- ✅ 网络接口过滤

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
</dependency>
```

### 2. 配置 Nacos

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      username: nacos
      password: nacos
      namespace:  # 默认命名空间
      group-name: DEFAULT_GROUP
      cluster-name: DEFAULT
      auto-register: true  # 启用自动服务注册
      weight: 1.0
      healthy: true
      instance-enabled: true
      
      # 首选网络地址配置(避免nacos取网卡出错)
      preferred-networks:
        - 192.168  # 优先选择 192.168 网段
        - 10.0     # 其次选择 10.0 网段
      
      # 忽略的网络接口
      ignored-interfaces:
        - docker0  # 忽略 Docker 网络接口
        - veth     # 忽略虚拟以太网接口
      
      metadata:
        version: 1.0.0
        env: dev
```

## 网络地址配置

### 问题场景

在多网卡环境下,Nacos可能会选择错误的网卡地址进行注册,导致服务无法正常访问。常见场景:

- Docker 环境中存在 `docker0` 网桥
- 虚拟机环境中存在多个虚拟网卡
- VPN连接创建的虚拟网卡

### 解决方案

使用 `preferred-networks` 和 `ignored-interfaces` 配置来精确控制注册的IP地址。

#### 配置项说明

| 配置项 | 类型 | 说明 | 示例 |
|-------|------|------|------|
| `preferred-networks` | List&lt;String&gt; | 首选网络前缀列表,优先选择匹配的网段 | `["192.168", "10.0"]` |
| `ignored-interfaces` | List&lt;String&gt; | 忽略的网络接口名称前缀列表 | `["docker0", "veth"]` |

#### 工作原理

1. **扫描网络接口**: 遍历所有网络接口
2. **过滤接口**: 跳过回环接口、虚拟接口、未启用接口和被忽略的接口
3. **匹配首选网络**: 检查IP地址是否匹配首选网络前缀
4. **选择IP地址**:
   - 如果找到匹配首选网络的地址,优先使用
   - 否则使用第一个可用的IPv4地址
   - 最后使用 `InetAddress.getLocalHost()` 作为兜底方案

### 配置示例

#### 示例1: 优先选择内网地址

```yaml
nebula:
  discovery:
    nacos:
      preferred-networks:
        - 192.168.1  # 优先选择 192.168.1.x 网段
        - 192.168.2  # 其次选择 192.168.2.x 网段
```

#### 示例2: 忽略 Docker 网络

```yaml
nebula:
  discovery:
    nacos:
      ignored-interfaces:
        - docker0
        - br-      # Docker 创建的网桥通常以 br- 开头
        - veth     # Docker 容器的虚拟以太网接口
```

#### 示例3: 组合使用

```yaml
nebula:
  discovery:
    nacos:
      # 只使用 10.0.x.x 网段的地址
      preferred-networks:
        - 10.0
      # 忽略所有Docker相关接口
      ignored-interfaces:
        - docker
        - veth
        - br-
```

## 自动服务注册

当配置 `auto-register: true` 时,应用启动后会自动注册到 Nacos。

### 注册信息

| 字段 | 来源 | 示例 |
|------|------|------|
| 服务名 | `spring.application.name` | `nebula-example` |
| IP地址 | 自动检测(支持首选网络配置) | `192.168.1.100` |
| 端口 | Web服务器端口 | `8000` |
| 实例ID | `{serviceName}:{ip}:{port}` | `nebula-example:192.168.1.100:8000` |
| 权重 | `nebula.discovery.nacos.weight` | `1.0` |
| 集群名 | `nebula.discovery.nacos.cluster-name` | `DEFAULT` |
| 分组名 | `nebula.discovery.nacos.group-name` | `DEFAULT_GROUP` |

### 元数据

自动注册时会添加以下元数据:

| 元数据Key | 说明 | 示例 |
|----------|------|------|
| `version` | 应用版本 | `1.0.0` |
| `profile` | 激活的配置文件 | `dev` |
| `startTime` | 启动时间戳 | `1696838400000` |

## 服务发现

### 获取服务实例

```java
@Autowired
private ServiceDiscovery serviceDiscovery;

// 获取所有健康实例
List<ServiceInstance> instances = serviceDiscovery.getInstances("nebula-example");

// 获取所有实例(包括不健康的)
List<ServiceInstance> allInstances = serviceDiscovery.getInstances("nebula-example", false);

// 获取指定分组的实例
List<ServiceInstance> groupInstances = serviceDiscovery.getInstances("nebula-example", "DEFAULT_GROUP");
```

### 订阅服务变化

```java
serviceDiscovery.subscribe("nebula-example", (serviceName, instances) -> {
    System.out.println("服务实例发生变化: " + serviceName);
    System.out.println("当前实例数: " + instances.size());
});
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
      namespace: dev
      group-name: DEFAULT_GROUP
      cluster-name: DEFAULT
      
      # 认证配置
      username: nacos
      password: nacos
      # 或使用 AccessKey/SecretKey
      # access-key: your-access-key
      # secret-key: your-secret-key
      
      # 服务注册配置
      auto-register: true
      weight: 1.0
      healthy: true
      instance-enabled: true
      
      # 网络配置
      preferred-networks:
        - 192.168
      ignored-interfaces:
        - docker0
      
      # 心跳配置
      heartbeat-interval: 5000       # 心跳间隔(毫秒)
      heartbeat-timeout: 15000       # 心跳超时(毫秒)
      ip-delete-timeout: 30000       # IP删除超时(毫秒)
      
      # 元数据
      metadata:
        version: 1.0.0
        env: dev
        zone: cn-hangzhou
```

## 故障排查

### 1. 服务注册失败

**症状**: 应用启动后未在 Nacos 控制台看到服务

**可能原因**:
- Nacos 服务器地址配置错误
- 认证信息错误
- `auto-register` 设置为 `false`
- 网络不通

**排查步骤**:
```bash
# 检查 Nacos 连接
curl http://localhost:8848/nacos/v1/ns/operator/metrics

# 查看应用日志
tail -f logs/application.log | grep "Nacos"
```

### 2. IP 地址选择错误

**症状**: 服务注册成功但实例IP不正确,无法访问

**解决方案**: 使用首选网络配置

```yaml
nebula:
  discovery:
    nacos:
      preferred-networks:
        - 你期望的网段前缀
      ignored-interfaces:
        - 不想使用的网络接口
```

**验证**: 查看启动日志

```
使用首选网络地址: 192.168.1.100
或
使用本机IP地址: 10.0.0.50
```

### 3. Docker 环境问题

**症状**: Docker 容器中注册的是 `172.17.x.x` 地址

**解决方案**:

```yaml
nebula:
  discovery:
    nacos:
      preferred-networks:
        - 宿主机网段或容器网络网段
      ignored-interfaces:
        - docker0  # 忽略 Docker 默认网桥
```

## 高级特性

### 多租户隔离

使用命名空间和分组实现服务隔离:

```yaml
# 开发环境
nebula:
  discovery:
    nacos:
      namespace: dev
      group-name: DEV_GROUP

# 生产环境
nebula:
  discovery:
    nacos:
      namespace: prod
      group-name: PROD_GROUP
```

### 灰度发布

通过元数据和权重实现灰度发布:

```yaml
# 灰度实例
nebula:
  discovery:
    nacos:
      weight: 0.1  # 10% 流量
      metadata:
        version: 2.0.0-SNAPSHOT
        gray: true

# 稳定实例
nebula:
  discovery:
    nacos:
      weight: 0.9  # 90% 流量
      metadata:
        version: 1.0.0
        gray: false
```

## 参考资料

- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Spring Cloud Alibaba Nacos Discovery](https://github.com/alibaba/spring-cloud-alibaba/wiki/Nacos-discovery)
