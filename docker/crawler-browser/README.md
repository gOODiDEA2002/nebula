# Crawler Browser 服务

本目录提供与 `nebula-crawler-browser` 配套的远程 Playwright Browser Server。Java 客户端和容器均使用
Playwright `1.41.0`，通过 Playwright WebSocket 协议连接，不是 Chrome DevTools Protocol（CDP）服务。

## 环境要求

- Docker 20.10+
- Docker Compose v2
- 每个实例至少 2 GB 共享内存
- 默认宿主端口：主实例 `9222`，备用实例 `9223`

## 启停与验证

默认只启动主实例：

```bash
./start.sh
curl --noproxy '*' http://localhost:9222/
docker compose logs crawler-browser-01
./stop.sh
```

HTTP 存活入口应严格返回 `Running`，容器日志会记录实际监听地址
`Listening on ws://0.0.0.0:9222/`。应用连接地址为 `ws://localhost:9222`。

宿主端口冲突时可覆盖映射端口：

```bash
CRAWLER_BROWSER_PORT=19222 ./start.sh
curl --noproxy '*' http://localhost:19222/
```

需要两个实例时直接使用 Compose，并按需覆盖两个宿主端口：

```bash
CRAWLER_BROWSER_PORT=19222 \
CRAWLER_BROWSER_SECONDARY_PORT=19223 \
docker compose up -d
```

## 应用配置

```yaml
nebula:
  crawler:
    enabled: true
    browser:
      enabled: true
      mode: REMOTE
      pool-size: 2
      remote:
        endpoints:
          - ws://localhost:9222
        use-cdp: false
        connections-per-endpoint: 1
```

`use-cdp` 必须保持为 `false`。只有目标服务是以远程调试端口启动的 Chrome 时，才使用 CDP 模式。

## 版本核对

服务端与 Java 客户端的 Playwright 版本必须一致：

```bash
docker exec crawler-browser-01 npx playwright --version
mvn -q -f ../../examples/crawler-example dependency:tree \
  -Dincludes=com.microsoft.playwright:playwright
```

当前两端应均为 `1.41.0`。镜像标签为
`harbor.vocoor.com/ci/browser-playwright:latest`，镜像元数据版本为 `1.0.0`。

## 资源与故障排查

- 浏览器崩溃：提高 `shm_size`，并检查容器内存限制。
- 连接超时：核对宿主端口、容器日志和应用的 WebSocket 地址。
- 版本不匹配：同步 Dockerfile 中的 npm Playwright 与 Maven 客户端版本。
- 测试结束：执行 `docker compose down`，确认容器、网络和宿主端口均已释放。

完整的 HTTP 与 Browser 引擎验证见
[Crawler 示例](../../examples/crawler-example/README.md)和 `examples/crawler-example/e2e-test.sh`。
