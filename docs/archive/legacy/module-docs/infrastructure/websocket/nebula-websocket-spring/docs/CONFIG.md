# Nebula WebSocket Spring - 配置参考

## 配置前缀

- `nebula.websocket`

## 基础配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.websocket.enabled` | boolean | true | 是否启用 WebSocket |
| `nebula.websocket.endpoint` | string | `/ws` | WebSocket 端点 |
| `nebula.websocket.allowed-origins` | list | `*` | 允许的来源 |
| `nebula.websocket.sock-js-enabled` | boolean | false | 是否启用 SockJS |

## 心跳配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.websocket.heartbeat.enabled` | boolean | true | 是否启用心跳 |
| `nebula.websocket.heartbeat.interval-seconds` | int | 30 | 心跳间隔（秒） |
| `nebula.websocket.heartbeat.timeout-seconds` | int | 60 | 超时阈值（秒） |

## 集群配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.websocket.cluster.enabled` | boolean | false | 是否启用集群 |
| `nebula.websocket.cluster.channel-prefix` | string | `websocket:cluster:` | 集群频道前缀 |

## 缓冲区配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.websocket.buffer.send-buffer-size-limit` | int | 524288 | 发送缓冲区大小 |
| `nebula.websocket.buffer.send-time-limit` | int | 10000 | 发送超时（毫秒） |
| `nebula.websocket.buffer.message-size-limit` | int | 65536 | 消息大小限制 |

## 配置示例

```yaml
nebula:
  websocket:
    enabled: true
    endpoint: /ws
    allowed-origins:
      - "*"
    sock-js-enabled: false
    heartbeat:
      enabled: true
      interval-seconds: 30
      timeout-seconds: 60
    cluster:
      enabled: false
      channel-prefix: "websocket:cluster:"
    buffer:
      send-buffer-size-limit: 524288
      send-time-limit: 10000
      message-size-limit: 65536
```
