# Nebula WebSocket Netty

Nebula 框架基于 Netty 的高性能 WebSocket 实现模块。

## 特性

- 基于 Netty 实现，支持百万级连接
- 独立服务端口，不依赖 Servlet 容器
- 高性能、低延迟
- 支持 Redis 集群消息同步

## 适用场景

- IOT 设备大规模连接
- 实时通信应用
- 游戏服务器
- 需要极高并发的场景

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-websocket-netty</artifactId>
    <version>${nebula.version}</version>
</dependency>

<!-- 集群模式需要添加 Redis 消息模块 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-messaging-redis</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 2. 配置

```yaml
nebula:
  websocket:
    netty:
      enabled: true
      port: 9000                    # WebSocket 服务端口
      path: /ws                     # WebSocket 路径
      boss-threads: 1               # 接收连接线程数
      worker-threads: 0             # 处理线程数（0=CPU核心*2）
      max-content-length: 65536     # 最大消息长度
      backlog: 1024                 # 积压连接数
      reader-idle-time: 60          # 读空闲超时（秒）
      cluster:
        enabled: false              # 是否启用集群模式
        channel-prefix: "websocket:cluster:"
```

### 3. 使用

与 Spring 版本使用方式完全一致：

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final WebSocketMessageService messageService;
    
    public void sendToUser(String userId, Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.of("notification", data);
        messageService.sendToUser(userId, message);
    }
}
```

### 4. 消息处理器

```java
@Component
public class DataHandler implements WebSocketMessageHandler<DataRequest> {
    
    @Override
    public void handle(WebSocketSession session, WebSocketMessage<DataRequest> message) {
        // 处理消息
    }
    
    @Override
    public String getType() {
        return "data_request";
    }
    
    @Override
    public Class<DataRequest> getPayloadType() {
        return DataRequest.class;
    }
}
```

## 性能调优

### 线程配置

```yaml
nebula:
  websocket:
    netty:
      boss-threads: 1          # 通常 1 个足够
      worker-threads: 16       # 根据 CPU 核心数调整
```

### 连接数优化

```bash
# Linux 系统优化
# /etc/sysctl.conf
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535

# /etc/security/limits.conf
* soft nofile 1000000
* hard nofile 1000000
```

### JVM 参数

```bash
-Xms2g -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=50
-Dio.netty.leakDetection.level=disabled
```

## Spring vs Netty 对比

| 特性 | Spring WebSocket | Netty |
|------|------------------|-------|
| 性能 | 万级连接 | 百万级连接 |
| 部署方式 | 与应用共用端口 | 独立端口 |
| 开发成本 | 低 | 中等 |
| Spring 集成 | 原生支持 | 需要适配 |
| 适用场景 | 管理后台、一般业务 | IOT、高并发实时应用 |

## 模块结构

```
nebula-websocket-netty/
├── session/
│   └── NettyWebSocketSession.java     # Netty 会话实现
├── handler/
│   ├── HttpRequestHandler.java        # HTTP/握手处理
│   └── WebSocketFrameHandler.java     # WebSocket 帧处理
├── server/
│   └── NettyWebSocketServer.java      # Netty 服务器
├── config/
│   ├── NettyWebSocketProperties.java  # 配置属性
│   └── NettyWebSocketAutoConfiguration.java  # 自动配置类
└── NettyWebSocketMessageService.java  # 消息服务实现
```

> **注意**: 自动配置由 `nebula-autoconfigure` 模块统一管理，注册在其 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中。

## 许可证

Apache License 2.0

