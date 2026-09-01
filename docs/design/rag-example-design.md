# rag-example 示例模块设计

> 状态：设计稿（v2，2026-08-29，待终审；评审通过前不创建模块、不改代码）
> v2 变更：定位从「装配演示」升级为「RAG 调优方法演示」——新增内置金标集与评测端点、切块/权重调优演示、README 生产就绪差距清单；新增附录 A 框架 RAG 优化路线图（含切分改进的业界对标结论）
> 范围：`examples/rag-example` 新模块 + `examples/pom.xml` 注册 + `examples/README.md` 索引一行；附录 A 仅为路线图存档，不属本次实施范围
> 关联：`infrastructure/ai/nebula-ai-rag`（2.1.1-SNAPSHOT，已上线 SIA 生产）、`examples/starter-ai-example`（形态参照）、`examples/e2e-common.sh`（验证框架）

## 1. 背景与目标

`nebula-ai-rag` 当前只有单元与条件测试，缺扩展规约要求的「真实调用链测试」；示例目录中也没有 RAG 用法演示——此前 fullstack-example 的 `documentQA` 是手写「检索 + 拼 prompt」，恰是该模块要取代的写法。

本示例一份代码承担三个职责：

1. **使用文档**：回答业务开发者「引入 `nebula-ai-rag` 后，从灌库到问答要写哪几个类、配哪几个键」。
2. **调优方法演示**（v2 核心）：内置金标集与 `/rag/eval` 评测端点，用户改一个环境变量、跑两次评测，就能亲眼看到切块长度或检索权重对召回指标的影响——传达「先度量、再调参」的方法，而不是只给一组默认值。
3. **回归资产**：以真实中间件（Chroma + Hy3 兼容端点）跑通「解析 → 切块 → 灌库 → 多路检索 → RRF 融合 → 生成」全链路，纳入示例 E2E 门禁；金标召回断言同时充当框架检索质量的首个回归卡点。

演示重点：多路检索 RRF 融合、`Retriever` 扩展点（自写一路即插即融合）、检索器排序与降级路径控制、切块与权重调优对召回的可测影响、`RagAnswer.degraded` 降级语义、`enabled=false` 零侵扰。

## 2. 非目标

- 不演示 Qdrant / Elasticsearch 检索路（Qdrant 已有 SIA 生产背书与金标测试；不为示例新增中间件）。
- 不演示重排（默认 `NoopReranker`；`LlmScoringReranker` 仅在 README 提配置方式）。
- 不做前端、不改任何 Starter、不动其他示例。
- **不在示例里补框架能力缺口**（HTML/XML/JSON 切分、查询改写、引用标记等见附录 A）；实现中发现框架缺陷，停下记录、单独评审，不在示例里绕。

## 3. 模块结构

```text
examples/rag-example/
  pom.xml                         nebula-starter-ai + nebula-ai-rag
  README.md                       运行说明 + 端点示例 + 调优对照表 + 生产就绪差距清单
  e2e-test.sh                     接入 e2e-common 框架
  src/main/java/io/nebula/examples/rag/
    RagExampleApplication.java    启动类
    config/RagDemoConfiguration.java   两个 Retriever Bean（见 §5）
    store/DemoDocumentStore.java  内存文档块存储（关键词路数据源 + 索引状态）
    retriever/InMemoryKeywordRetriever.java   演示用第二路检索器
    service/RagDemoService.java   灌库/清理逻辑（解析 → 切块 → 双写）
    service/RagEvalService.java   金标集评测（recall@5 / MRR，见 §6.2）
    controller/RagDemoController.java   六个 REST 端点
  src/main/resources/
    application.yml
    docs/nebula-faq-*.md          内置知识文档 4 篇（见 §6.1）
    eval/golden-set.json          金标问答对 10 条（见 §6.2）
```

依赖声明（版本随 examples 父 pom 的 `${project.version}`）：`nebula-starter-ai`、`nebula-ai-rag`。显式引入 `nebula-ai-rag` 正是「依赖决定能力」的演示点。

## 4. 接口设计

统一包 `Result<T>` 外壳。RAG 关闭时（`AI_ENABLED` 缺省），全部端点返回禁用提示不抛错，Controller 用 `ObjectProvider` 注入。

| 端点 | 请求 | 响应要点 |
|---|---|---|
| `GET /rag/status` | — | `enabled`、检索器清单 `[{name, weight, order}]`、`rrfK`、`sourcePriority`、当前切块参数、`indexedChunks` 计数 |
| `POST /rag/index` | 空体 | 解析 4 篇内置 Markdown → 切块 → 双写（向量库 + 内存存储）；返回 `{documents, chunks, chunkSize, overlap}`；幂等（块 ID 确定性，§7） |
| `POST /rag/search` | `{"query": "...", "topK": 5}` | 管线 `generateAnswer=false`；返回融合结果 `[{id, content, score, source}]`，`source` 恒为 `hybrid` |
| `POST /rag/query` | `{"query": "...", "topK": 5}` | 完整管线；返回 `{answer, references, degraded, degradeReason, costMs}` |
| `GET /rag/eval` | — | 对金标集逐条检索，返回 `{recallAt5, mrr, total, perQuery: [{query, expected, hitRank}]}`（只测检索，不调生成，秒级完成） |
| `DELETE /rag/documents` | — | 按确定性块 ID 删除向量库条目并清空内存存储；返回删除计数 |

`/rag/eval` 是 v2 的灵魂端点：README 的调优教程即「index → eval → 改 `CHUNK_SIZE` 重启 → index → eval → 对比 recall」。

## 5. 检索路与装配设计

容器内两路检索器，均由示例显式声明（不依赖框架默认 Bean），以演示排序、权重与降级路径的控制方式：

```java
/** 向量路：显式声明使框架默认 vectorStoreRetriever 让位（@ConditionalOnMissingBean 回退），
    @Order(10) 保证它是引擎降级路径（整体失败时退回首个检索器）；权重经环境变量可调（§7） */
@Bean @Order(10)
public VectorStoreRetriever vectorRetriever(VectorStoreService vss, RagDemoProperties props) {
    return new VectorStoreRetriever(vss, "VectorRetriever", props.getVectorWeight(), 0.0);
}

/** 关键词路：读 DemoDocumentStore，2-gram 重合计分 */
@Bean @Order(20)
public InMemoryKeywordRetriever keywordRetriever(DemoDocumentStore store, RagDemoProperties props) { ... }
```

README 逐条解读的教学点：

- **为什么显式声明向量路**：框架默认 `VectorStoreRetriever` Bean 无 `@Order`，`orderedStream()` 会把它排到有序 Bean 之后，降级路径会错落到关键词路；显式声明 + `@Order(10)` 才能钉住「整体失败降级为向量检索」。SIA 生产同款做法。
- **`InMemoryKeywordRetriever` 算法**：查询与块内容做中文 2-gram 重合计分（`score = 重合 2-gram 数 / 查询 2-gram 数`），排序取 topK。约 60 行，无第三方依赖；实现 `Retriever` 接口即被引擎自动纳入融合——这一行为本身就是演示目标。
- 生成路走框架默认 `ChatServiceAnswerGenerator`（Hy3 兼容端点），不覆盖——与 SIA 覆盖场景路由（`AnswerGenerator` 端口）的做法形成对照，README 两种都提。

## 6. 内置数据设计

### 6.1 知识文档（4 篇 Markdown）

| 文件 | 主题 | 特殊设计 |
|---|---|---|
| `nebula-faq-starter.md` | Starter 选择与默认值机制 | 含表格与代码块（覆盖 `ChunkType.CODE/CONFIG` 路径；README 顺带示范表格被切断的现象，呼应附录 A 切分改进项） |
| `nebula-faq-config.md` | 三级启用策略与配置覆盖 | — |
| `nebula-faq-vector.md` | 向量库选型（Chroma/Qdrant）与 ID 映射 | — |
| `nebula-faq-codes.md` | 虚构故障码对照表（如 `NBX-2077 表示向量库连接超时`） | 埋入生僻代号，专供证明关键词路贡献（向量召回大概率漏、2-gram 必中） |

### 6.2 金标集（golden-set.json，10 条）

```json
[{"query": "Nebula 的模块启用开关怎么配", "expectedDocPrefix": "nebula-faq-config"},
 {"query": "NBX-2077 是什么故障",        "expectedDocPrefix": "nebula-faq-codes"}, ...]
```

- 判中口径：融合结果前 5 条中出现 `expectedDocPrefix` 开头的块 ID 即命中（块级太脆，文档级足够演示指标含义）。
- 10 条覆盖：4 篇文档各至少 2 条；含 1 条故障码类（关键词路强项）与 1 条近义改述类（向量路强项），使权重调整对指标的影响双向可见。
- 指标：`recall@5`（命中率）与 `MRR`（首个命中排名倒数均值）。口径写死在 README，防止误当成通用评测协议。

## 7. 配置设计（application.yml 草案）

```yaml
server:
  port: 8087

spring:
  application:
    name: rag-example

nebula:
  ai:
    enabled: ${AI_ENABLED:false}
    openai:
      api-key: ${AI_API_KEY:}
      base-url: ${AI_BASE_URL:https://tokenhub.tencentmaas.com/v1}
      chat.options.model: ${AI_CHAT_MODEL:hy3}
      embedding.options.model: ${AI_EMBED_MODEL:kinfra-text-embedding-0.6b}
    vector-store:
      default-provider: chroma
      chroma:
        host: ${CHROMA_HOST:localhost}
        port: ${CHROMA_PORT:9002}
        collection-name: ${CHROMA_COLLECTION:rag-example}
        initialize-schema: true
    rag:
      enabled: ${AI_ENABLED:false}
      retrieval:
        top-k: 5
      fusion:
        rrf-k: ${RRF_K:60}
      chunking:
        size: ${CHUNK_SIZE:500}
        overlap: ${CHUNK_OVERLAP:100}
      generation:
        timeout-ms: 60000

# 示例自有调优旋钮（RagDemoProperties, 前缀 rag-demo）
rag-demo:
  vector-weight: ${VECTOR_WEIGHT:0.6}
  keyword-weight: ${KEYWORD_WEIGHT:0.4}
```

- 双开关同绑 `AI_ENABLED`：缺省 false，应用可启动、端点报禁用。
- **调优旋钮全部环境变量化**（`CHUNK_SIZE` / `CHUNK_OVERLAP` / `RRF_K` / `VECTOR_WEIGHT` / `KEYWORD_WEIGHT`）：这是「跑两次 eval 对比」教程的前提。
- collection 名经 `CHROMA_COLLECTION` 注入，E2E 用 `nebula_e2e_rag_${E2E_RUN_ID}` 隔离并收尾删除。
- 块 ID 确定性：`<文件名去后缀>#<块序号>`，重灌幂等、删除精确。注意：改 `CHUNK_SIZE` 后块数变化，README 要求「换参数前先 DELETE 再 index」。

README 附「症状 → 旋钮」调优对照表（v2 新增，示例核心交付物之一）：

| 症状 | 先看的旋钮 |
|---|---|
| 召回的块内容太碎、答案缺上下文 | `CHUNK_SIZE` 调大、`CHUNK_OVERLAP` 调大 |
| 召回混入大段无关内容 | `CHUNK_SIZE` 调小、`top-k` 调小 |
| 专有名词/代号查不到 | 关键词路权重调大；确认该路数据源覆盖 |
| 近义改述查不到 | 向量路权重调大；检查 embedding 模型 |
| 多路结果排序不稳 | `RRF_K` 调大（压平名次差异） |

README 同时附「生产就绪差距清单」：如实列出评测、索引治理、开箱 BM25、查询改写、重排、引用防注入、指标观测七项框架尚未覆盖（详见附录 A），防止用户误以为复制示例即生产。

## 8. E2E 验证设计（e2e-test.sh）

接入 `e2e-common.sh`，禁用 8087 / 启用 18087。密钥经 `vocoor_hy3_token` 注入，缺失时 Full 模式记 BLOCKED（照抄 starter-ai 的 `AI_LIVE_READY` 处理）。

| # | 模式 | 用例 | 断言 |
|---|---|---|---|
| S1 | Smoke/Full | 无密钥启动 | 应用起、`/rag/status` 报禁用 |
| S2 | Smoke/Full | 禁用态六端点 | 均返回禁用提示，HTTP 200，无异常栈 |
| S3 | Smoke/Full | 日志安全 | 日志无 API Key 泄漏 |
| F1 | Full | 启用态启动 | 日志含「Nebula AI RAG 模块自动配置已启用」与两路检索器名 |
| F2 | Full | `POST /rag/index` | chunks 大于文档数（切块生效），status 的 `indexedChunks` 一致 |
| F3 | Full | 重复 index | chunks 数不变（幂等） |
| F4 | Full | `POST /rag/search`（常规问句） | 非空、`source=hybrid`、按分数降序 |
| F5 | Full | `POST /rag/query`（常规问句） | `answer` 非空、`references` 与 F4 同源、`degraded=false` |
| F6 | Full | `search` 查询 `NBX-2077` | 结果含 `nebula-faq-codes` 的块（关键词路贡献证明） |
| F7 | Full | `GET /rag/eval` | 返回 10 条明细；`recallAt5 >= 0.7`（金标集与文档同仓固定，指标确定性可断言；阈值留余量防 embedding 模型端波动） |
| F8 | Full | `DELETE /rag/documents` | 删除计数与 F2 一致；再 search 返回空 |
| F9 | Full | 收尾 | 临时 collection 已删、进程清理、密钥不落日志 |

降级路径（生成超时 → `degraded=true`）不进 E2E（依赖外部时延不可靠），由 `nebula-ai-rag` 单元测试覆盖，README 说明语义。

## 9. 验收标准

- [ ] `mvn -q -f examples/rag-example spring-boot:run` 缺省可启动；README 三步内跑通问答，调优教程可复现（两组参数的 eval 指标确有差异并与预期方向一致）。
- [ ] `E2E_MODE=smoke` 与 `E2E_MODE=full` 全部 PASS（Full 需 Chroma 9002 + `vocoor_hy3_token`），0 FAIL / 0 SKIP；证据目录留存。
- [ ] `examples/pom.xml` 注册、`examples/README.md` 索引行更新；不改其余文件。
- [ ] README 含调优对照表与生产就绪差距清单。
- [ ] 代码风格：中文注释、无 emoji、4 空格缩进，对齐 starter-ai-example 写法。

## 10. 实施方式与工作量

评审通过后派 opus 子代理一轮实现（本设计 + starter-ai-example 为唯一依据），总控验收时实跑 Smoke 与 Full E2E 并核对全部断言，另亲手复现一次调优教程。估算 1 至 1.5 个工作日。提交：`feat(examples): 新增 rag-example` 单 commit，推送前单独请批。

## 11. 待拍板决策（可只答「是 / 否」）

| ID | 决策 | 推荐 |
|---|---|---|
| D1 | 位置命名 `examples/rag-example`（进阶类） | 是 |
| D2 | 仅 Chroma 后端，不为示例新增 Qdrant 中间件 | 是 |
| D3 | 双检索器融合演示（显式声明两 Bean + 故障码文档钉住关键词路贡献） | 是 |
| D4 | 生成走 Hy3 兼容端点 + `AI_ENABLED` 门控（对齐 starter-ai-example） | 是 |
| D5 | 内置金标集 + `/rag/eval` 评测端点与调优演示（v2 新增核心） | 是 |
| D6 | 附录 A 路线图仅存档待排期，不随本示例实施 | 是 |
| D7 | 评审通过后按 §10 方式实施 | 是 |

---

## 附录 A：框架 RAG 优化路线图（存档待排期，非本示例范围）

背景：示例评审中确认「照示例能跑起可用的 RAG，做不出做好了的」。差距按优先级排序如下；每项启动前另立设计。

| 优先级 | 项 | 内容 | 业界锚点 |
|---|---|---|---|
| P1 | 评测模块 | 金标集格式 + recall@k / MRR / nDCG 计算器 + 配置对比报告，作为一切调优的度量前提（本示例的 `/rag/eval` 是其最小雏形） | Ragas / TruLens 的检索指标子集 |
| P2 | 索引治理 | 文档生命周期：增量 upsert、删除同步、embedding 换模型的全量重灌流程与版本化 collection；吸收 SIA `KnowledgeIndexer` 经验 | LlamaIndex ingestion pipeline |
| P3 | 切分改进（三合一） | 见下文详述 | LangChain / Unstructured |
| P4 | 通用 BM25 检索路 | 架在 `nebula-search-elasticsearch` 上的 `Retriever` 实现，分词器（IK 等）经 ES 索引配置暴露——「分词器配置」的正确落点 | — |
| P5 | 查询改写 | 管线预处理端口从 trim 升级为可插拔（改写/扩展/多查询），默认仍直通 | LangChain MultiQueryRetriever |
| P6 | 重排实用化 | cross-encoder / BGE-reranker HTTP 批量接入（逐条 LLM 打分在 SIA 生产因延迟被关停，教训在案） | — |
| P7 | 生成可信与观测 | 行内引用标记、检索内容防注入清洗（参照 SIA agent 层 `CloudFieldSanitizer`）、流式输出、分路延迟/命中率/降级率 Micrometer 指标 | — |

### 切分改进详述（P3）

业界对标结论（2026-08 经 context7 核对 LangChain 现版文档）：

1. **两段式是标准形态**：结构切分（带面包屑 metadata）与尺寸控制（递归降级）是两个可组合的独立阶段（LangChain `MarkdownHeaderTextSplitter` → `RecursiveCharacterTextSplitter` 官方流水线）。现有 `DocumentChunker` 把两者揉在一起，按此拆分。
2. **分隔符层级递归**：`TextChunker` 从「定长 + 句边界回退」升级为有序分隔符表递归降级（段落 → 换行 → 句 → 字符），一张表即可支持一种新格式（LangChain `from_language` 预置 16 种语言分隔符的做法）。
3. **原子单元保护**：Markdown 表格/列表识别为原子块（`ChunkType.TABLE` 补产出）+ 标题路径面包屑注入 metadata；原子超限时宁可超限不切（LangChain `HTMLSemanticPreservingSplitter` 的 `elements_to_preserve` 语义），表格按行切时重复表头。
4. **HTML/XML 结构解析器**：jsoup 系 DOM 解析，沿块级元素切；XML 沿元素子树切并保留父级上下文。
5. **JSON/JSONL**：JSONL 按行一记录一块；JSON 递归键路径切分器（合法 JSON 块 + 从根重建键路径 + 数组转下标字典，对齐 LangChain `RecursiveJsonSplitter`）。**记录型 JSON 的文本改写模板明确为业务责任**，框架只固化「文本供召回、metadata 供过滤」双表示约定（SIA 生产 462 万点即此做法）。
6. **overlap 按边界走**（整句/整行，非裸字符）；**长度度量支持 token 计数**（现为字符数）。
7. 语义切分、父子分层块（LlamaIndex `HierarchicalNodeParser`）各家均列实验位，不进本路线图。
