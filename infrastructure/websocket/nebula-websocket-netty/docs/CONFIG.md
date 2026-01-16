# Nebula WebSocket Netty - 配置参考

## 配置前缀

- `nebula.websocket.netty`

## 基础配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.websocket.netty.enabled` | boolean | true | 是否启用 Netty WebSocket |
| `nebula.websocket.netty.port` | int | 9000 | 服务端口 |
| `nebula.websocket.netty.path` | string | `/ws` | WebSocket 路径 |
| `nebula.websocket.netty.boss-threads` | int | 1 | 接收连接线程数 |
| `nebula.websocket.netty.worker-threads` | int | 0 | 处理线程数（0=CPU*2） |
| `nebula.websocket.netty.max-content-length` | int | 65536 | 最大内容长度 |
| `nebula.websocket.netty.backlog` | int | 1024 | 积压连接数 |
| `nebula.websocket.netty.reader-idle-time` | int | 60 | 读空闲超时（秒） |
| `nebula.websocket.netty.writer-idle-time` | int | 0 | 写空闲超时（秒） |
| `nebula.websocket.netty.all-idle-time` | int | 0 | 读写空闲超时（秒） |

## 集群配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.websocket.netty.cluster.enabled` | boolean | false | 是否启用集群 |
| `nebula.websocket.netty.cluster.channel-prefix` | string | `websocket:cluster:` | 集群频道前缀 |

## 配置示例

```yaml
nebula:
  websocket:
    netty:
      enabled: true
      port: 9000
      path: /ws
      boss-threads: 1
      worker-threads: 0
      max-content-length: 65536
      backlog: 1024
      reader-idle-time: 60
      cluster:
        enabled: false
        channel-prefix: "websocket:cluster:"
```
