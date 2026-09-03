# rag-example

`nebula-ai-rag` 的端到端示例：演示从「加载文档 → 索引 → 混合检索 → 生成回答」的完整链路，同时作为该模块的用法文档、调优教学与 E2E 回归资产。

- 端口：`8087`（可用 `SERVER_PORT` 覆盖）
- 默认关闭：不配置 `AI_ENABLED=true` 时，所有端点返回禁用提示、HTTP 200、不抛错，可零依赖启动。
- 启用依赖：Chroma 向量库（默认 `localhost:9002`）与 OpenAI 兼容端点（聊天 + 嵌入）。

## 三步运行

```bash
# 1. 安装框架构件到本地仓库（首次）
mvn install -DskipTests

# 2. 关闭态启动（零外部依赖，用于查看禁用提示与端点契约）
mvn -q -f examples/rag-example spring-boot:run

# 3. 启用态启动（需 Chroma + AI 兼容端点）
export AI_ENABLED=true
export vocoor_hy3_token=<你的-API-Key>   # 也可用 OPENAI_API_KEY
export CHROMA_HOST=localhost CHROMA_PORT=9002
mvn -q -f examples/rag-example spring-boot:run
```

> API Key 只经环境变量注入，绝不写入代码、日志、响应或本文示例值。

## 七个端点

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/rag/status` | 运行态快照：是否启用、检索器与权重、分块/RRF/topK 参数、已索引文档与分块数、清洗/流式/指标开关 |
| POST | `/rag/index` | 加载 classpath 文档并增量索引，返回 added/updated/deleted/failed |
| POST | `/rag/search` | 仅检索（不生成），返回命中分块（id、内容、score、来源），按 score 降序 |
| POST | `/rag/query` | 检索 + 生成，返回 answer、references、degraded 标记 |
| POST | `/rag/query/stream` | 流式问答（SSE）：先 REFERENCES，再 DELTA，末 COMPLETE；禁用态返回 JSON 提示 |
| GET | `/rag/eval` | 用内置黄金集只做检索评测（不生成），返回逐条命中与 recall@K、配置快照 |
| DELETE | `/rag/documents` | 清空索引（双写目标一并删除），返回 deleted |

`/search`、`/query`、`/query/stream` 请求体：`{"query":"...","topK":5}`（`topK` 缺省走 `nebula.ai.rag.retrieval.top-k`）。

## 示例写了什么 / 框架给了什么

| 关注点 | 框架（nebula-ai-rag）提供 | 本示例负责组装 |
| --- | --- | --- |
| 索引流水线 | `IndexingPipeline`、结构化解析、确定性分块、双写与状态仓储抽象 | `ClasspathDocumentSource`（读 `docs/*.md`、内容哈希）、`FileIndexStateRepository`（单 JSON 落盘） |
| 向量检索 | `VectorStoreRetriever`、`VectorStoreService`、Chroma 接入 | 以 `@Order(10)` 注册向量检索器并注入权重 |
| 关键词检索 | `Retriever`/`IndexSink` 接口、混合检索引擎、RRF 融合 | `InMemoryKeywordIndex`（内存倒排）、`InMemoryKeywordRetriever`（中文 2-gram 重合打分，`@Order(20)`） |
| 生成与流式 | `RagPipeline`、`DefaultAnswerGenerator`、`StreamingAnswerGenerator`、注入清洗 | 直接复用，仅暴露 REST |
| 评测 | `RetrievalEvaluator`、`GoldenSet` | 提供 `eval/golden-set.json`（10 条，关键词 3 / 语义 7） |
| 指标 | `nebula.rag.query.duration`（Micrometer） | 经 actuator 暴露 |

## 调优教学：症状 → 旋钮

所有旋钮均为环境变量（见 `application.yml`），改后重启生效。

| 症状 | 可调旋钮 | 方向 |
| --- | --- | --- |
| 召回偏低、答案漏信息 | `CHUNK_SIZE` / `CHUNK_OVERLAP` | 减小分块、增大重叠，提高召回粒度 |
| 精确编码/术语查不到（如 NBX-2077） | `rag-demo.keyword-weight` | 提高关键词路权重，强化精确匹配 |
| 语义相近问句召回差 | `rag-demo.vector-weight` | 提高向量路权重 |
| 多路结果融合不稳 | `RRF_K` | 增大更平滑，减小更偏重高排名 |
| 上下文过长/成本高 | `nebula.ai.rag.retrieval.top-k` | 减小 topK |

调优验证：改旋钮后先 `POST /rag/index` 重建，再 `GET /rag/eval` 看 `recallAtK` 与 `configSnapshot`，用同一黄金集横向对比。

## 落地状态（R1–R4）

| 阶段 | 能力 | 本示例覆盖 |
| --- | --- | --- |
| R1 | 结构化切分、评测库 | `docs/*.md` 结构化解析 + `golden-set.json` 评测 |
| R2 | 索引治理骨架、BM25 检索路、查询改写 | 关键词内存检索路 + 混合检索组装（BM25/ES 与查询改写未演示） |
| R3 | 索引代际/重建流水线 | 增量索引、幂等重跑与删除对齐（`/rag/index` 计数）；版本化重灌与别名切换未演示（需 Qdrant/ES） |
| R4 | 混合检索、RRF 融合、注入清洗、流式、指标 | 全部经 REST 暴露与 E2E 校验 |

## 生产就绪差距（本示例为演示，非生产实现）

- 关键词索引为进程内存态，重启丢失、不可横向扩展；生产应使用 Elasticsearch/BM25 等持久检索。
- 状态仓储为单文件 JSON + 进程内写锁，无并发多实例协调；生产应使用数据库或分布式 KV。
- 文档源为 classpath 静态文件；生产需接入真实文档管道与增量变更捕获。
- 无鉴权与配额控制，端点公开；生产需接入认证与限流。

## E2E 测试

```bash
# Smoke：仅禁用态 S1-S3，零外部依赖
E2E_MODE=smoke examples/rag-example/e2e-test.sh

# Full：追加启用态 F1-F13，需 Chroma 与 AI 兼容端点凭据
E2E_MODE=full CHROMA_HOST=localhost CHROMA_PORT=9002 \
  vocoor_hy3_token=<你的-API-Key> examples/rag-example/e2e-test.sh
```

> 性能预期：Hy3 为推理模型，思考 token 计入 `max-tokens`，示例默认 `OPENAI_MAX_TOKENS=2048`
> （设为 256 会被思考耗尽，`finish_reason=length` 且正文为空）；单次生成（`/rag/query`、
> `/rag/query/stream`）约 10–30 秒，启用态用例已把 HTTP 超时放宽到 120 秒（`E2E_HTTP_MAX_TIME=120`）。
> Full 模式含两次应用重启与多次真实生成，总时长通常在数分钟量级，请预留足够超时。嵌入接口较快（亚秒级）。
