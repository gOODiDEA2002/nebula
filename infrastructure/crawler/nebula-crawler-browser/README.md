# Nebula Crawler Browser

> 基于 Playwright 的浏览器爬虫实现，支持 JavaScript 渲染

## 模块概述

`nebula-crawler-browser` 提供基于 Playwright 的浏览器爬虫引擎，适用于：

- 需要 JavaScript 渲染的页面
- 动态加载内容的网站
- 需要模拟用户交互的场景
- 需要截图的场景

## 特性

- 支持 Chromium/Firefox/WebKit
- 浏览器实例池化管理
- **支持本地和远程两种运行模式**
- **支持多端点负载均衡（适用于 K8s 部署）**
- 页面等待机制
- 错误时自动截图
- 代理支持
- 无头模式
- 反检测脚本注入

## 运行模式

### LOCAL 模式（默认）

在本地启动浏览器实例，适用于开发和单机部署。

```yaml
nebula:
  crawler:
    browser:
      enabled: true
      mode: LOCAL
      headless: true
      pool-size: 5
```

### REMOTE 模式

连接到远程 Playwright Server，适用于 Docker/K8s 部署。

```yaml
nebula:
  crawler:
    browser:
      enabled: true
      mode: REMOTE
      pool-size: 10
      remote:
        endpoints:
          - ws://playwright-1:9222
          - ws://playwright-2:9222
          - ws://playwright-3:9222
        load-balance-strategy: ROUND_ROBIN  # 或 RANDOM, LEAST_CONNECTIONS
        health-check-interval: 30000
        max-retries: 3
```

## 架构图

### 本地模式

```
┌────────────────────────────────────────┐
│  Application                           │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │  BrowserCrawlerEngine            │  │
│  │         │                        │  │
│  │         ▼                        │  │
│  │  ┌──────────────┐                │  │
│  │  │  BrowserPool │ ─────┐         │  │
│  │  └──────────────┘      │         │  │
│  │         │              ▼         │  │
│  │         ▼        ┌──────────┐   │  │
│  │  ┌──────────┐    │ Chromium │   │  │
│  │  │ Contexts │───►│ (local)  │   │  │
│  │  └──────────┘    └──────────┘   │  │
│  └──────────────────────────────────┘  │
└────────────────────────────────────────┘
```

### 远程模式（Docker/K8s）

```
┌────────────────────────────────────────────────────────────┐
│  Kubernetes Cluster                                        │
│                                                            │
│  ┌─────────────────────┐     ┌──────────────────────────┐  │
│  │  Application Pod    │     │  Playwright Server Pods  │  │
│  │                     │ WS  │                          │  │
│  │  BrowserCrawlerEngine│────►│  ┌──────────────────┐   │  │
│  │         │           │     │  │ Pod 1 (Chromium) │   │  │
│  │         ▼           │     │  └──────────────────┘   │  │
│  │  ┌──────────────┐   │     │  ┌──────────────────┐   │  │
│  │  │  BrowserPool │   │────►│  │ Pod 2 (Chromium) │   │  │
│  │  │  (负载均衡)   │   │     │  └──────────────────┘   │  │
│  │  └──────────────┘   │     │  ┌──────────────────┐   │  │
│  │                     │────►│  │ Pod N (Chromium) │   │  │
│  └─────────────────────┘     │  └──────────────────┘   │  │
│                              └──────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-crawler-browser</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 安装浏览器（仅 LOCAL 模式）

首次使用需要安装浏览器：

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

### 3. 配置

```yaml
nebula:
  crawler:
    enabled: true
    browser:
      enabled: true
      mode: LOCAL           # 或 REMOTE
      browser-type: chromium
      headless: true
      pool-size: 5
      page-timeout: 30000
      screenshot-on-error: true
      use-proxy: false
```

### 4. 使用

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final BrowserCrawlerEngine browserCrawlerEngine;
    
    public void crawlDynamicPage() {
        CrawlerRequest request = CrawlerRequest.builder()
            .url("https://example.com")
            .renderJs(true)
            .waitSelector(".content-loaded")
            .waitTimeout(10000)
            .build();
        
        CrawlerResponse response = browserCrawlerEngine.crawl(request);
        
        if (response.isSuccess()) {
            Document doc = response.asDocument();
            // 处理渲染后的 DOM...
        }
    }
    
    public void crawlWithScreenshot() {
        CrawlerRequest request = CrawlerRequest.builder()
            .url("https://example.com")
            .renderJs(true)
            .screenshot(true)
            .build();
        
        CrawlerResponse response = browserCrawlerEngine.crawl(request);
        byte[] screenshot = response.getScreenshot();
    }
}
```

## 配置说明

### 基础配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `true` | 是否启用 |
| `mode` | `LOCAL` | 运行模式：LOCAL 或 REMOTE |
| `browser-type` | `chromium` | 浏览器类型 |
| `headless` | `true` | 无头模式（仅 LOCAL） |
| `pool-size` | `5` | 上下文池大小 |
| `page-timeout` | `30000` | 页面超时(ms) |
| `navigation-timeout` | `30000` | 导航超时(ms) |
| `connect-timeout` | `30000` | 连接超时(ms)（仅 REMOTE） |
| `screenshot-on-error` | `true` | 错误时截图 |
| `use-proxy` | `false` | 使用代理 |
| `viewport-width` | `1920` | 视口宽度 |
| `viewport-height` | `1080` | 视口高度 |
| `disable-images` | `false` | 禁用图片 |
| `disable-css` | `false` | 禁用 CSS |
| `slow-mo` | `0` | 慢速模式延迟(ms)（仅 LOCAL） |

### 远程配置（REMOTE 模式）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `remote.endpoints` | `[]` | Playwright Server 端点列表 |
| `remote.load-balance-strategy` | `ROUND_ROBIN` | 负载均衡策略 |
| `remote.health-check-interval` | `30000` | 健康检查间隔(ms) |
| `remote.max-retries` | `3` | 连接失败重试次数 |
| `remote.retry-interval` | `1000` | 重试间隔(ms) |

### 负载均衡策略

| 策略 | 说明 |
|------|------|
| `ROUND_ROBIN` | 轮询（默认） |
| `RANDOM` | 随机选择 |
| `LEAST_CONNECTIONS` | 最少连接数 |

## Docker 部署

### 启动 Playwright Server

```yaml
# docker-compose.yml
version: '3.8'

services:
  playwright:
    image: node:18-slim
    command: npx playwright@1.41.0 run-server --port 9222 --host 0.0.0.0
    ports:
      - "9222:9222"
    environment:
      - PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright
    shm_size: '2gb'
```

### 应用配置

```yaml
nebula:
  crawler:
    browser:
      mode: REMOTE
      remote:
        endpoints:
          - ws://playwright:9222
```

## Kubernetes 部署

### Playwright Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: playwright-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: playwright
  template:
    metadata:
      labels:
        app: playwright
    spec:
      containers:
      - name: playwright
        image: mcr.microsoft.com/playwright:v1.41.0-jammy
        command: ["npx", "playwright", "run-server", "--port", "9222", "--host", "0.0.0.0"]
        ports:
        - containerPort: 9222
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: playwright
spec:
  selector:
    app: playwright
  ports:
  - port: 9222
    targetPort: 9222
```

### 应用配置（K8s）

```yaml
nebula:
  crawler:
    browser:
      mode: REMOTE
      pool-size: 20
      remote:
        endpoints:
          - ws://playwright:9222
        load-balance-strategy: LEAST_CONNECTIONS
        health-check-interval: 10000
```

## 浏览器池

`BrowserPool` 管理浏览器实例的生命周期：

- 预创建指定数量的浏览器上下文
- 自动回收和复用
- 支持并发请求
- 自动健康检查和重连（REMOTE 模式）
- 多端点负载均衡（REMOTE 模式）

## 性能优化

1. **禁用图片/CSS**：提高加载速度
2. **合理设置池大小**：根据并发需求调整
3. **使用等待选择器**：避免不必要的等待
4. **使用 REMOTE 模式**：资源隔离，便于扩展

## 依赖

- Playwright 1.41.0

## 许可证

MIT License
