# Nebula Crawler Proxy

> 代理IP池管理模块

## 模块概述

`nebula-crawler-proxy` 提供完整的代理IP池管理功能：

- 多代理源支持（API/文件）
- 代理可用性检测
- 智能轮换策略
- 失败自动下线
- 黑名单机制
- Redis缓存

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-crawler-proxy</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 配置

```yaml
nebula:
  crawler:
    enabled: true
    proxy:
      enabled: true
      check-interval: 300000       # 检测间隔5分钟
      check-url: "http://httpbin.org/ip"
      check-timeout: 10000
      min-available: 50            # 最小可用数
      max-fail-count: 3            # 最大失败次数
      blacklist-expire-hours: 24   # 黑名单过期时间
      providers:
        - type: api
          name: proxy-api
          url: "${PROXY_API_URL}"
          count: 100
          api-key: "${PROXY_API_KEY}"
        - type: file
          name: local-proxy
          url: "classpath:proxy-list.txt"
```

### 3. 使用

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final ProxyProvider proxyProvider;
    
    public void useProxy() {
        // 获取代理
        Proxy proxy = proxyProvider.getProxy();
        
        if (proxy != null) {
            // 使用代理...
            try {
                // 请求成功
                proxyProvider.reportSuccess(proxy);
            } catch (Exception e) {
                // 请求失败
                proxyProvider.reportFailure(proxy, e.getMessage());
            }
        }
        
        // 获取多个代理
        List<Proxy> proxies = proxyProvider.getProxies(10);
        
        // 查看可用数量
        int available = proxyProvider.getAvailableCount();
        
        // 刷新代理池
        proxyProvider.refresh();
    }
}
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `false` | 是否启用 |
| `check-interval` | `300000` | 检测间隔(ms) |
| `check-url` | `http://httpbin.org/ip` | 检测URL |
| `check-timeout` | `10000` | 检测超时(ms) |
| `min-available` | `50` | 最小可用代理数 |
| `max-fail-count` | `3` | 最大失败次数 |
| `blacklist-expire-hours` | `24` | 黑名单过期时间(小时) |

## 代理源

### API代理源

从HTTP API获取代理：

```yaml
providers:
  - type: api
    name: my-proxy-api
    url: "https://proxy-provider.com/api/get"
    count: 100
    api-key: "your-api-key"
```

### 文件代理源

从本地文件读取代理（每行一个，格式：`host:port`）：

```yaml
providers:
  - type: file
    name: local-proxy
    url: "classpath:proxy-list.txt"
```

文件格式示例：
```
192.168.1.1:8080
192.168.1.2:8080
# 带认证
192.168.1.3:8080:username:password
```

## 自定义代理源

实现`ProxySource`接口：

```java
@Component
public class CustomProxySource implements ProxySource {
    
    @Override
    public String getName() {
        return "custom-source";
    }
    
    @Override
    public List<Proxy> fetch() {
        List<Proxy> proxies = new ArrayList<>();
        // 从自定义来源获取代理...
        return proxies;
    }
}
```

## 工作流程

```
+------------------+     +------------------+     +------------------+
|   代理源(API)    |     |   代理源(文件)   |     |   自定义代理源   |
+------------------+     +------------------+     +------------------+
         |                       |                       |
         v                       v                       v
+----------------------------------------------------------------------+
|                           代理池 (ProxyPool)                          |
+----------------------------------------------------------------------+
|  - 代理获取                                                           |
|  - 可用性检测                                                         |
|  - 成功/失败统计                                                      |
|  - 黑名单管理                                                         |
+----------------------------------------------------------------------+
         |                       |                       |
         v                       v                       v
+------------------+     +------------------+     +------------------+
|   HTTP爬虫引擎   |     |  浏览器爬虫引擎  |     |   其他使用方     |
+------------------+     +------------------+     +------------------+
```

## Redis缓存

代理池使用Redis缓存代理列表和黑名单：

- `nebula:crawler:proxy:list` - 可用代理列表
- `nebula:crawler:proxy:blacklist` - 黑名单代理

## 依赖

- nebula-data-cache (Redis)
- OkHttp 4.12.0

## 许可证

Apache License 2.0

