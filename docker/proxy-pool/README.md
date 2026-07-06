# proxy-pool 公共代理池服务

基于开源项目 [jhao104/proxy_pool](https://github.com/jhao104/proxy_pool) 的二次定制版本，用于 SIA (source-insight-ai) 等爬虫项目的可用代理供给。

## 与上游差异

本版本相对 jhao104/proxy_pool 做了以下调整，目的是降低对下游网络的压力（详见 `setting.py` 和 `helper/scheduler.py` 顶部注释）：

| 项 | 上游默认 | 本版本 | 说明 |
|---|---|---|---|
| 基础镜像 | python:3.6-alpine | python:3.11-slim | Python 3.6 已 EOL |
| `PROXY_FETCHER` | 12 个免费源 | 3 个精选源 | 减少抓取量 |
| `VERIFY_TIMEOUT` | 10 秒 | 3 秒 | 失败连接更快释放 fd |
| `MAX_FAIL_COUNT` | 0 | 3 | 避免单次失败立刻剔除引发的池子大幅 churn |
| `POOL_SIZE_MIN` | 20 | 10 | 触发抓取的水位 |
| scheduler `proxy_fetch` 间隔 | 4 分钟 | 30 分钟 | 拉新代理频率 |
| scheduler `proxy_check` 间隔 | 2 分钟 | 10 分钟 | 验证现有代理频率 |
| `threadpool max_workers` | 20 | 5 | 并发降低 |
| `job max_instances` | 10 | 1 | 同一时刻只允许 1 个实例 |
| `coalesce` | False | True | 错过的轮次合并而非累加 |
| Dockerfile | 无 HEALTHCHECK / STOPSIGNAL | 都有 | 容器健康检查 + 优雅停机 |

## 已知架构限制

`proxy_pool` 的核心工作是验证公网免费代理（绝大多数代理在境外）。**容器必须有直连境外网络的能力**，否则验证会全部失败，池子会一直为空。

**当前 Mac 本地部署存在架构问题**：Mac 在国内，直连国外被墙；如果容器流量走 mihomo/vocoor 代理出去，会让境外的代理服务器变成跳板，自身资源被吞没。详见 `gost-incident-2026-05-13.md` 第九章。

推荐部署位置：**境外 VPS**（如 bwg），让容器直接出网访问公共代理。

## 快速开始

### 1. 启动服务

```bash
cd nebula/docker/proxy-pool
chmod +x start.sh stop.sh
./start.sh
```

或使用 docker compose：

```bash
docker compose up -d
```

### 2. 健康检查

```bash
curl http://localhost:5010/
# 返回 API 列表 / 状态
```

### 3. 获取一个可用代理

```bash
curl http://localhost:5010/get/
# {"https": false, "proxy": "1.2.3.4:8080", ...}
```

### 4. 停止服务

```bash
./stop.sh
```

## API 接口

| Method | URL | 说明 |
|---|---|---|
| GET | `/` | API 列表 / 状态 |
| GET | `/get/` | 随机返回一个代理 |
| GET | `/pop/` | 取出并删除一个代理 |
| GET | `/all/` | 返回所有代理 |
| GET | `/count/` | 池子数量统计 |
| GET | `/delete/?proxy=ip:port` | 删除一个代理 |
| GET | `/get_status/` | 池子状态 |

## 依赖

- **Redis**：通过 `DB_CONN` 环境变量配置。在 SIA 项目部署里通常连 `sia-net` 网络上的 `sia-redis`。

## 资源需求

- CPU: 0.5 ~ 1 核
- 内存: 256MB 以上
- 网络: 容器需直连境外（验证国外代理）

## 参考

- 上游项目：<https://github.com/jhao104/proxy_pool>
- 故障复盘：参见 `gost-incident-2026-05-13.md`
