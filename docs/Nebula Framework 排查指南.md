# Nebula Framework 排查指南

本文档提供 Nebula Framework 常见问题的排查方法和解决方案。

## 目录

- [诊断工具](#诊断工具)
- [服务发现问题](#服务发现问题)
- [RPC 问题](#rpc-问题)
- [配置问题](#配置问题)
- [启动问题](#启动问题)

---

## 诊断工具

### 1. 启动摘要日志

Nebula Framework 会在应用启动完成后自动输出配置摘要：

```
======================================================================
                    NEBULA FRAMEWORK STARTUP SUMMARY                   
======================================================================
  [Framework Info]
    Version              : 2.0.1-SNAPSHOT
    Profile              : default

  [Service Discovery (Nacos)]
    Status               : ENABLED
    Server Address       : localhost:8848
    ...
======================================================================
```

### 2. 诊断端点

访问 `/actuator/nebula-diagnostic` 获取详细诊断信息：

```bash
curl http://localhost:8080/actuator/nebula-diagnostic | jq
```

响应示例：
```json
{
  "timestamp": "2024-01-21 21:30:00",
  "framework": {
    "name": "Nebula Framework",
    "version": "2.0.1-SNAPSHOT",
    "javaVersion": "21",
    "springBootVersion": "3.5.x"
  },
  "discovery": {
    "enabled": true,
    "status": "CONNECTED",
    "serverAddr": "localhost:8848"
  },
  "rpc": {
    "http": { "enabled": true, "port": 8080 }
  }
}
```

需要在配置中暴露端点：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,nebula-diagnostic
```

---

## 服务发现问题

### 问题：Nacos 连接失败

**错误信息：**
```
Client not connected, current status:STARTING
```

**排查步骤：**

1. 检查 Nacos 服务是否运行：
   ```bash
   curl http://localhost:8848/nacos/v1/ns/service/list
   ```

2. 检查网络连通性：
   ```bash
   telnet localhost 8848
   ```

3. 验证认证信息：
   ```bash
   curl -X POST "http://localhost:8848/nacos/v1/auth/login" \
     -d "username=nacos&password=nacos"
   ```

**解决方案：**

- 确保 Nacos 服务已启动
- 检查 `nebula.discovery.nacos.server-addr` 配置
- 确认用户名密码正确（默认 nacos/nacos）

### 问题：服务注册失败

**错误信息：**
```
ServiceDiscoveryException: 注册服务实例失败
```

**排查步骤：**

1. 检查 `spring.application.name` 是否配置
2. 检查服务端口是否正确
3. 检查 Nacos 命名空间是否存在

**解决方案：**

```yaml
spring:
  application:
    name: my-service  # 必须配置

nebula:
  discovery:
    nacos:
      namespace: ""   # 空字符串表示 public，不要写 "public"
```

---

## RPC 问题

### 问题：RPC 客户端 Bean 未找到

**错误信息：**
```
required a bean of type 'XxxRpcClient' that could not be found
```

**解决方案：**

**方式一（推荐）：API 模块自动配置**

在 API 模块中创建自动配置：

```java
// src/main/java/xxx/api/XxxApiAutoConfiguration.java
@AutoConfiguration
@EnableRpcClients(basePackages = "xxx.api")
public class XxxApiAutoConfiguration {}
```

并注册到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：
```
xxx.api.XxxApiAutoConfiguration
```

**方式二：手动启用**

在主应用类添加：
```java
@SpringBootApplication
@EnableRpcClients(basePackages = "xxx.api")
public class MyApplication {}
```

### 问题：RPC 调用超时

**错误信息：**
```
Read timed out
```

**解决方案：**

调整超时配置：
```yaml
nebula:
  rpc:
    http:
      client:
        connect-timeout: 30000   # 连接超时（毫秒）
        read-timeout: 60000      # 读取超时（毫秒）
        retry-count: 3           # 重试次数
```

### 问题：服务发现集成不生效

**症状：** RPC 调用直接使用 baseUrl，没有通过服务发现

**排查步骤：**

1. 检查诊断端点中 `discovery.beanPresent` 是否为 true
2. 检查 `rpc.discoveryIntegration.enabled` 是否为 true

**解决方案：**

确保以下条件满足：
- 引入了 `nebula-discovery-nacos` 依赖
- 引入了 `nebula-rpc-http` 依赖
- 没有显式配置 `nebula.rpc.discovery.enabled=false`

---

## 配置问题

### 问题：配置校验失败

**错误信息：**
```
Could not bind properties to 'XxxProperties'
```

**常见原因：**

1. 端口超出范围（1-65535）
2. 超时时间配置错误
3. 必需字段为空

**解决方案：**

参考配置校验规则：

| 属性 | 有效范围 |
|------|---------|
| port | 1 - 65535 |
| heartbeatInterval | 1000 - 60000 ms |
| connectTimeout | 1000 - 120000 ms |
| readTimeout | 1000 - 600000 ms |
| retryCount | 0 - 10 |

### 问题：namespace 配置错误

**症状：** 连接 Nacos 失败，提示命名空间不存在

**原因：** Nacos 的 public 命名空间 ID 是空字符串

**错误配置：**
```yaml
nebula:
  discovery:
    nacos:
      namespace: public  # 错误！
```

**正确配置：**
```yaml
nebula:
  discovery:
    nacos:
      namespace: ""      # 空字符串表示 public
      # 或者不配置，使用默认值
```

---

## 启动问题

### 问题：循环依赖

**错误信息：**
```
The dependencies of some of the beans in the application context form a cycle
```

**解决方案：**

检查是否在配置类中存在循环引用，使用 `@Lazy` 注解延迟加载：

```java
@Bean
public MyBean myBean(@Lazy OtherBean otherBean) {
    return new MyBean(otherBean);
}
```

### 问题：Bean 创建顺序错误

**错误信息：**
```
Bean 'xxx' is not eligible for getting processed by all BeanPostProcessors
```

**解决方案：**

这通常是警告而非错误。如果影响功能，可以使用 `@AutoConfigureAfter` 或 `@AutoConfigureBefore` 调整配置顺序。

---

## 最小化配置示例

以下是使用智能默认值的最小配置：

```yaml
server:
  port: 8080

spring:
  application:
    name: my-service

# Nebula 配置 - 使用默认值即可
nebula:
  rpc:
    http:
      server:
        port: ${server.port}

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,nebula-diagnostic
```

此配置将自动启用：
- Nacos 服务发现（localhost:8848）
- HTTP RPC
- RPC 服务发现集成
- 异步 RPC

---

## 获取帮助

如果以上方法无法解决问题：

1. 查看完整启动日志
2. 访问 `/actuator/nebula-diagnostic` 获取诊断信息
3. 检查 Nacos 控制台（http://localhost:8848/nacos）
4. 提交 Issue 并附上诊断信息
