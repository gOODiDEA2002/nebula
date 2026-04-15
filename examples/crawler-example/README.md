# Nebula Crawler Example

基于 `nebula-crawler-http` 和 `nebula-crawler-browser` 模块的网页爬虫示例，演示 HTTP 和浏览器双引擎爬虫的核心用法。

## 功能概览

| 功能 | 端点 | 引擎 | 说明 |
|------|------|------|------|
| 单页爬取 | `POST /crawler/crawl` | HTTP | 爬取指定 URL 返回内容 |
| 批量爬取 | `POST /crawler/batch` | HTTP | 并行爬取多个 URL |
| 爬取解析 | `GET /crawler/parse?url=` | HTTP | 爬取 HTML 并提取标题、链接 |
| 浏览器爬取 | `POST /crawler/browser` | Browser | 通过 Playwright 渲染 JS 动态页面 |
| 健康检查 | `GET /crawler/health` | ALL | 检查所有爬虫引擎状态 |

## 技术要点

- **双引擎架构** - HTTP 引擎处理静态页面，Browser 引擎处理 JS 渲染的动态页面
- **`CrawlerEngine`** - 统一爬虫引擎接口，支持同步/异步/批量/回调四种模式
- **`CrawlerRequest`** - Builder 模式构建请求，支持自定义 Header、超时、重试
- **`CrawlerResponse`** - 响应封装，内置 Jsoup 解析、JSON 解析等便捷方法
- **远程 Playwright** - Browser 引擎连接远程 Docker 容器中的 Playwright Server，无需本地安装
- **Stealth 反检测** - 内置 Stealth4j，隐藏自动化特征绕过反爬检测
- **优雅降级** - Browser 引擎不可用时，应用正常启动，HTTP 引擎不受影响

## 快速启动

```bash
# 1. 安装框架到本地仓库
mvn install -DskipTests -f ../../pom.xml

# 2. 启动爬虫示例（仅 HTTP 引擎）
mvn spring-boot:run -f pom.xml

# 3. 启动远程 Playwright Server（需要 Docker）
docker run -d --name crawler-browser -p 9222:9222 --shm-size=2g harbor.vocoor.com/ci/browser-playwright:latest

# 4. 启动爬虫示例（同时启用浏览器引擎）
BROWSER_CRAWLER_ENABLED=true mvn spring-boot:run -f pom.xml
# 或者连接远程服务器
BROWSER_CRAWLER_ENABLED=true PLAYWRIGHT_SERVER=your-server-ip mvn spring-boot:run -f pom.xml
```

## 使用示例

### 单页爬取（HTTP）

```bash
curl -X POST http://localhost:8085/crawler/crawl \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/get"}'
```

### 批量爬取（HTTP）

```bash
curl -X POST http://localhost:8085/crawler/batch \
  -H "Content-Type: application/json" \
  -d '{
    "urls": [
      "https://httpbin.org/get",
      "https://httpbin.org/ip"
    ],
    "timeout": 10000
  }'
```

### 爬取并解析 HTML

```bash
curl "http://localhost:8085/crawler/parse?url=https://example.com"
```

### 浏览器引擎爬取（需启用 Browser 引擎）

```bash
curl -X POST http://localhost:8085/crawler/browser \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com",
    "waitUntil": "domcontentloaded",
    "waitSelector": "h1",
    "screenshot": false
  }'
```

### 引擎健康检查

```bash
curl http://localhost:8085/crawler/health
```

## 配置说明

```yaml
nebula:
  crawler:
    enabled: true                    # 启用爬虫模块
    http:
      enabled: true                  # 启用 HTTP 爬虫引擎
      connect-timeout: 30000        # 连接超时 (ms)
      read-timeout: 60000           # 读取超时 (ms)
      max-connections: 50           # 最大连接数
      retry-count: 2                # 重试次数
      default-qps: 5.0              # QPS 限制
      follow-redirects: true        # 跟随重定向
    browser:
      enabled: false                 # 启用浏览器引擎（需要远程 Playwright Server）
      mode: REMOTE                   # REMOTE=连接远程服务器, LOCAL=本地启动浏览器
      pool-size: 3                   # 浏览器上下文池大小
      stealth:
        enabled: true                # 启用 Stealth 反检测
      remote:
        endpoints:                   # 远程 Playwright Server 端点列表
          - ws://localhost:9222
        use-cdp: true                # 使用 Chrome DevTools Protocol 连接
```

## 相关文档

- [Nebula 框架使用指南](../../docs/Nebula框架使用指南.md)
- [Nebula 框架审查报告](../../docs/nebula-framework-review.md)
