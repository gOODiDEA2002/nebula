# Nebula Fullstack Example

`fullstack-example` 是 Nebula 2.1 的综合示例，覆盖数据访问、多级缓存、HTTP/gRPC 客户端、gRPC
服务端、Nacos、RabbitMQ、Elasticsearch、MinIO、任务、支付、通知、AI、向量存储和 MCP。

## 端口与依赖

| 项目 | 默认值 | 配置方式 |
| --- | --- | --- |
| HTTP | 1000 | `FULLSTACK_PORT` |
| gRPC Server | 2000 | `FULLSTACK_GRPC_PORT` |
| MySQL | 127.0.0.1:3306 | `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` |
| Redis | 127.0.0.1:6379，DB 1 | `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`FULLSTACK_REDIS_DATABASE` |
| Nacos | 127.0.0.1:8848 | `NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD` |
| RabbitMQ | 127.0.0.1:5672 | `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD` |
| Elasticsearch | 127.0.0.1:9200 | `ELASTICSEARCH_HOST`、`ELASTICSEARCH_PORT` |
| MinIO | 127.0.0.1:9000 | `MINIO_HOST`、`MINIO_PORT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY` |
| Chroma | 127.0.0.1:9002 | `CHROMA_HOST`、`CHROMA_PORT`、`CHROMA_COLLECTION` |
| Hy3 兼容 API | `https://tokenhub.tencentmaas.com/v1` | `vocoor_hy3_token`、`OPENAI_BASE_URL`、聊天与嵌入模型变量 |

完整模式还需要 Redis、RabbitMQ、Nacos、MinIO 和 Chroma。E2E 会使用
[`docker/verification/docker-compose.yml`](../../docker/verification/docker-compose.yml)启动隔离的 MySQL 8.3
和 Elasticsearch 9.4.2，不会复用现有数据卷。

## 数据模式

| Profile | 能力 | 配置文件 |
| --- | --- | --- |
| `dev` | 默认单数据源 | `application.yml` |
| `readwrite` | 主从读写分离 | `application-readwrite.yml` |
| `sharding` | 订单分库分表 | `application-sharding.yml` |
| `combined` | 读写分离与分片组合 | `application-combined.yml` |

## 启动

先安装当前框架快照：

```bash
mvn install -DskipTests
```

默认模式：

```bash
mvn -q -f examples/fullstack-example spring-boot:run
```

指定数据模式：

```bash
SPRING_PROFILES_ACTIVE=readwrite mvn -q -f examples/fullstack-example spring-boot:run
SPRING_PROFILES_ACTIVE=sharding mvn -q -f examples/fullstack-example spring-boot:run
SPRING_PROFILES_ACTIVE=combined mvn -q -f examples/fullstack-example spring-boot:run
```

外部能力可通过对应 `NEBULA_*_ENABLED` 环境变量关闭。HTTP RPC Server 默认关闭；本示例作为 User
RPC 消费方，通过 Nacos 调用下游，并在 `FULLSTACK_GRPC_PORT` 提供独立 Echo 服务。

## 代表性端点

| 能力 | 端点前缀 |
| --- | --- |
| 商品与数据模式 | `/data`、`/readwrite`、`/sharding` |
| 缓存 | `/cache` |
| 消息 | `/messaging` |
| 搜索 | `/search` |
| 存储 | `/storage` |
| 任务 | `/task` |
| 支付与通知 | `/payment`、`/notification` |
| 下游 RPC | `/rpc-client/users` |
| AI 与向量存储 | `/ai` |
| MCP | `/api/mcp` |
| Web、认证与监控 | `/hello`、`/auth`、`/health`、`/performance` |

## 完整验证

```bash
E2E_MODE=full examples/fullstack-example/e2e-test.sh
```

验证脚本覆盖四种数据模式、真实 SQL 路由、缓存一致性、RabbitMQ、Elasticsearch、MinIO、任务、支付、
通知、Web 功能、RPC、gRPC、AI、Chroma 和 MCP，并负责清理 namespaced 数据、受管进程、容器与卷。
真实 AI 流程从 `vocoor_hy3_token` 读取密钥，默认使用 `hy3` 聊天和
`kinfra-text-embedding-0.6b` 嵌入模型。`OPENAI_*` 变量仅作为 OpenAI 兼容协议的覆盖入口。

原先分散的 15 份功能测试手册已归档到
[`docs/archive/examples/fullstack-example`](../../docs/archive/examples/fullstack-example/)，其命令和配置仅供历史排查，
不再作为 Nebula 2.1 的运行依据。
