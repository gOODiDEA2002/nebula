# RAG 优化 R2 详细设计（最小索引治理 + BM25 路 + 查询改写）

> 状态：详细设计稿（v1，2026-09-01，待评审；评审通过前不改代码）
> 上位设计：`docs/design/rag-framework-optimization-design.md` v2（已批准）§5 P2/P4/P5、§8 R2
> 范围：`nebula-ai-rag` 新增 `index` / `transform` 包与检索器、`nebula-search-elasticsearch` 的 mapping 下发实现、`nebula-ai-spring` 的可选改写器适配、`RagAutoConfiguration` 增量装配；另含 R1 遗留的「超限代码块签名摘要」小项
> 兼容口径：Y1 兼容增量（`ApiBaselineTest` 门禁已在岗）/ Y2 新键默认关、默认行为不变

## 1. 现状事实与影响面（2026-09-01 核实）

| 事实 | 出处 | 含义 |
|---|---|---|
| `IndexMapping` 就是 `properties` + `settings` 两个 `Map<String,Object>` | `IndexMapping.java` | mapping 下发可走「Map → JSON → ES 客户端 withJson」的通用转换，任意嵌套（含 analyzer）无需逐类型建模 |
| `createIndex` 忽略 mapping 的现有调用方**仅 fullstack-example 一处**；SIA 的 `KnowledgeEsIndexInitializer` 绕过 nebula 直用 ES client（自带 IK mapping） | 全仓 grep；SIA `KnowledgeEsIndexInitializer.java:62` | 实现下发是「补齐接口既有承诺」的行为变更，影响面一处，实施时必须核对该 example 传入的 mapping 可被真实 ES 接受 |
| 查询构建器已有 `Bool/Match/MatchAll/Term/Range` | `search-core/query/builder/` | BM25 检索与等值/范围过滤可表达，无需扩查询模型 |
| SIA 生产 ES 用 `ik_max_word`/`ik_smart` | 同上 SIA 文件 | 默认 mapping 的 analyzer 必须可配（默认 `standard`，IK 不保证安装） |
| 引擎现签名 `retrieve(String, int, Map)`；`orderedStream` 中无序 Bean 排最后 | `HybridRetrievalEngine.java:82`；R1b 教训 | 变体重载为新增方法；框架默认向量检索器需补 `@Order(10)` 防「检索器列表首位漂移」旧坑复发 |

## 2. P2-min：索引治理骨架（包 `io.nebula.ai.rag.index`）

### 2.1 契约

```java
/** 源文档：完整快照语义（J7）——快照中不存在即视为已删除，无独立 tombstone */
public class SourceDocument {
    private String id;            // 业务 docId，块 ID 前缀
    private String content;
    private String format;        // 对应 StructureParser.format()
    private String contentHash;   // 由 DocumentSource 提供；框架提供 Sha256ContentHash 工具
    private Map<String, Object> metadata;
}

public interface DocumentSource {
    String name();                          // 状态库的分区键
    List<SourceDocument> snapshot();        // 每次调用返回完整一致快照
}

/** 写目标。幂等契约（J8，逐实现测试钉住）：
    upsert 同 ID 重复调用 = 覆盖；delete 不存在的 ID = 静默成功；批内部分失败必须抛出（含已成功 ID 清单） */
public interface IndexSink {
    String name();
    void upsert(String docId, List<DocumentChunk> chunks);
    void delete(String docId, List<String> chunkIds);
}

public interface IndexStateRepository {
    Map<String, DocIndexState> load(String sourceName);
    void save(String sourceName, DocIndexState state);       // 逐文档保存, 不做批量事务假设
    void remove(String sourceName, String docId);
}

/** 文档 × sink 状态：全部必需 sink 为 DONE 才算该文档完成（上位 §4 决策 4） */
public class DocIndexState {
    private String docId;
    private String contentHash;
    private List<String> chunkIds;
    private Map<String, SinkStatus> sinkStatus;   // sinkName → DONE / PENDING
    private long generation;
    private int schemaVersion;                    // 首版 = 1
}
```

### 2.2 计划与执行

- `IndexPlanner.plan(snapshot, states)` → `IndexPlan{toAdd, toUpdate, toDelete}`：hash 相同且全 sink DONE 的跳过；hash 变了或任一 sink PENDING 的进 toUpdate；状态库有而快照没有的进 toDelete。
- `IndexingPipeline.run(source)` 每文档五步：解析（按 `format` 选 `StructureParser`）→ 装箱（**强制 `ChunkIdStrategy.deterministic()`**，忽略调用方传入的随机策略并 warn）→ 逐 sink：先 `delete(旧 chunkIds)` 再 `upsert(新块)`，成功即置该 sink DONE 并 `save` → 全 sink DONE 后更新 hash 与 chunkIds → 失败文档记入 `IndexRunReport` 继续下一文档（可重入：重跑时 PENDING 文档自然落入 toUpdate）。
- `IndexRunReport`：added/updated/deleted/failed 计数 + 逐失败文档的 sink 与异常摘要；`toComparableSummary()` 风格与 `EvalReport` 一致。
- **J10 处置（如实）**：框架经 `VectorStoreService` 抽象无法探测后端是否 Qdrant、id-mapping 是否开启，无法做启动强校验。落地为三道软防线：`VectorStoreIndexSink` 的 javadoc 醒目声明；`RagProperties.indexing` 配置注释声明；rag-example（R1 后示例）以正确配置作可运行样例。残余风险记入验收说明。

### 2.3 两个内置 Sink 与内存状态库

- `VectorStoreIndexSink`：`DocumentChunk` → `io.nebula.ai.core.model.Document`（content=块内容；metadata 并入 breadcrumb/title/docId/chunkType）；delete 走 `VectorStoreService.deleteAll(ids)`。
- `SearchServiceIndexSink`：写 §3.2 的 `RagSearchDocument`；索引不存在时用默认 mapping 建索引（经 §3.1 的下发能力）。
- `InMemoryIndexStateRepository`：**仅测试与单次任务用，不进任何自动装配**（上位 DS6）；javadoc 声明重启即失忆、不能支撑删除对齐。

### 2.4 装配

`RagAutoConfiguration` 新增嵌套 `IndexingConfiguration`，条件 `nebula.ai.rag.indexing.enabled=true`（默认 false）：`IndexingPipeline`/`IndexPlanner` Bean 需容器内已有 `DocumentSource` 与 `IndexStateRepository`（`@ConditionalOnBean`，缺任一不装配并在 debug 日志说明）；`vectorStoreIndexSink` 需 `VectorStoreService`；`searchServiceIndexSink` 需 `SearchService` 类与 Bean 存在且 `indexing.search-index-name` 非空。持续增量场景缺持久化状态库即无 Bean，等价「启动快速失败」由 `@ConditionalOnBean` 的缺席 + 业务侧注入点编译期暴露承担。

## 3. P4：ES mapping 下发 + 通用 BM25 检索路

### 3.1 `nebula-search-elasticsearch` 的 mapping 下发（既有模块修改，Y7 补齐）

- `ElasticsearchSearchService.createIndex`：`mapping.properties` 非空时经 JSON 序列化 + ES 客户端 `withJson` 装入 `mappings`；`mapping.settings` 同法装入 `settings` 并与默认分片/副本合并（用户键覆盖默认）；两者皆空时行为与现状完全一致。
- 行为变更声明：唯一既有调用方 fullstack-example 的 mapping 从「被忽略」变为「被应用」，实施时跑该示例 E2E（search 组）确认真实 ES 接受；release note 记录。
- 测试：单元级断言构建出的 `CreateIndexRequest` 含 mapping/settings（客户端请求对象可检视）；含 IK analyzer 的 settings 样例走「构造正确性」断言（真实 IK 环境验证归 SIA 侧后续采用时）。

### 3.2 `RagSearchDocument` 与默认 mapping（J12 的答案）

```java
public class RagSearchDocument {      // io.nebula.ai.rag.retriever
    private String id;                // = 块 ID, keyword
    private String docId;             // keyword
    private String content;           // text, analyzer 可配(默认 standard)
    private String title;             // text + keyword 子字段
    private List<String> breadcrumb;  // keyword
    private String chunkType;         // keyword
    private Map<String, Object> metadata;   // object, enabled=false(只存不检索)
}
```

默认 mapping 常量由该类给出（`defaultMapping(analyzer, searchAnalyzer)`），analyzer 经配置注入。

### 3.3 `SearchServiceRetriever implements Retriever`

- 查询：`MatchQueryBuilder(content)`，`title` 以 `BoolQueryBuilder` should 子句加权；`size = topK`；`_score` 直接作 `RetrievalResult.score`（RRF 只用名次，量纲无关）。
- **filter 语义（审查发现 9 落地）**：允许键 = `docId`、`chunkType`、`breadcrumb`（等值，Term/Terms）；出现许可集之外的键抛 `IllegalArgumentException` 快速失败，不静默忽略。范围过滤本期不开（无消费方，C15）。
- 结果：`SearchDocument.source` 映射回 `RagSearchDocument` → `RetrievalResult{id, content, metadata(含 docId/breadcrumb/title), score, source="keyword"}`。
- 装配（嵌套 `SearchRetrieverConfiguration`）：`@ConditionalOnClass(SearchService)` + `@ConditionalOnBean(SearchService)` + `nebula.ai.rag.search.index-name` 非空才创建；`@Order` 取 `nebula.ai.rag.search.order`（默认 20）；权重 `search.weight`（默认 0.4）。**同时给框架默认 `vectorStoreRetriever` Bean 补 `@Order(10)`**——防止有序的 search 检索器把无序的向量检索器挤出列表首位、降级路径漂移（R1b 已踩过同型坑；对既有单检索器应用无行为影响）。
- 条件测试补「缺 SearchService 类」矩阵（`FilteredClassLoader`）。

## 4. P5：查询改写与变体检索

### 4.1 契约与引擎重载

```java
public class QueryVariant {
    private String text;
    private double weight;          // 变体权重，默认 1.0
}

public interface QueryTransformer {
    List<QueryVariant> transform(String rawQuery);   // 返回空列表视为配置错误, 抛异常
}
```

- 默认实现 `TrimQueryTransformer`：返回 `[QueryVariant(rawQuery.trim(), 1.0)]`（现状语义）。
- 引擎**新增**重载 `retrieve(List<QueryVariant> variants, int topK, Map filter)`：任务集 = 变体 × 检索器（并行，沿用现有单路超时与空表收敛）；融合输入每任务一个列表，权重 = 检索器权重 × 变体权重；跨变体同 ID 的去重由 RRF `scoreMap.merge` 天然完成（分数累加，与多路命中同语义）。变体数上限 `transform.max-variants`（默认 4，超出截断并 warn）；总并发不额外设池（现实现本就 per-future，任务数上限 = 4 × 检索器数，可控）。旧单查询方法改为委托单元素变体——**行为逐路径等价**（一个变体 × N 检索器 = 现状的 N 列表 N 权重），以既有 `HybridRetrievalEngineTest` 全绿为证。
- **三文本契约（审查发现 5 落地）**：`DefaultRagPipeline` 固定——提示词用 `RagQuery.query` 原文；检索用 transformer 产出的变体；重排用首个变体文本（默认即 trim 后文本，现状）。新增「前后带空白的查询」逐字段回归测试钉住三者。

### 4.2 LLM 改写器与 Spring AI 适配（附录 B 决策点落地）

- `nebula-ai-rag` 内不放 LLM 改写实现（避免对 ChatService 的又一默认依赖）。
- `nebula-ai-spring` 新增 **optional** 依赖 `spring-ai-rag`，提供 `SpringAiQueryTransformerAdapter`：按 `nebula.ai.rag.transform.mode` 包装 `RewriteQueryTransformer`（mode=rewrite）或 `MultiQueryExpander`（mode=multi-query，`includeOriginal=true` 强制保底）为本框架 `QueryTransformer`；LLM 失败或超时直通原查询（warn 一次）。
- 装配：`transform.mode=none`（默认，装 `TrimQueryTransformer`）；`rewrite`/`multi-query` 需 `@ConditionalOnClass` spring-ai-rag 类 + `ChatClient.Builder` Bean，缺任一启动快速失败（显式配了模式却不可用不能静默降级）。

## 5. R1 遗留小项：超限代码块签名摘要

`PackOptions.codeSummaryToContent`（默认 false，Y2）：开启时，CODE 原子块在块首附加一行注释形式的摘要（取代码块前 N 行中的签名样行：含 `class/def/function/public/fn` 等模式或首个非空行，上限 120 字符）。评测语料 code 子集在开启后应有可测提升（纳入对比 harness 的第三配置 C 侧，非门禁、仅记录）。

## 6. 配置键增量（全部新增，默认值保持现行为）

```yaml
nebula:
  ai:
    rag:
      indexing:
        enabled: false
        search-index-name: ""        # 空 = 不装配 SearchServiceIndexSink
      search:
        index-name: ""               # 空 = 不装配 SearchServiceRetriever
        weight: 0.4
        order: 20
        analyzer: standard
        search-analyzer: standard
      transform:
        mode: none                   # none | rewrite | multi-query
        max-variants: 4
      chunking:
        code-summary: false
```

`RagProperties` 增量嵌套类，`ApiBaselineTest` 相应补表（新增成员不属红线，但 RagProperties 既有嵌套结构在守护清单内不得变形）。

## 7. 测试计划

| 层 | 内容 |
|---|---|
| 兼容门禁 | `ApiBaselineTest` 既有断言全绿；引擎旧签名/`RagProperties` 既有结构零变化 |
| P2 单元 | Planner 差分矩阵（增/改/删/hash 未变跳过/单 sink PENDING 重入）；Pipeline 逐文档失败继续与可重入；两 Sink 幂等契约（重复 upsert、删不存在、部分失败抛出——桩必须断言收到的参数与 ID 集） |
| P4 单元 | `createIndex` 请求对象含 mapping/settings 断言（含 IK 样例、空 mapping 走现状路径）；Retriever 查询构建、filter 许可集拒绝、命中映射；「缺 SearchService 类」矩阵 |
| P5 单元 | 变体重载等价性（单变体 = 旧路径逐字段一致）；权重乘积；max-variants 截断；三文本契约（带空白查询）；adapter 的失败直通与快速失败装配 |
| 行为等价 | 既有 `HybridRetrievalEngineTest`/`DefaultRagPipelineTest` 零修改全绿 |
| 评测 | 对比 harness 增 C 侧（code-summary 开）记录 code 子集变化，非门禁 |
| 条件矩阵 | indexing/search/transform 三组「默认关 / 显式开 / 显式关 / 缺类缺 Bean」 |
| 回归 | nebula 全仓（基线 1174/0/3）+ install + SIA 347 联动；fullstack-example search 组 E2E（mapping 下发行为变更的实证） |

## 8. 红队自检

| 失败路径 | 应对 |
|---|---|
| fullstack-example 的 mapping 被应用后真实 ES 拒绝 | 实施时先读该 example 的 mapping 内容再动手；E2E 实跑；不兼容则修 example 的 mapping（它本来就该是合法的） |
| 变体重载引入行为漂移 | 旧方法委托单变体 + 既有测试零修改全绿作等价证明 |
| search 检索器把降级路径挤到首位 | 框架默认向量检索器补 `@Order(10)`（§3.3），条件测试断言列表顺序 |
| 显式配 rewrite 模式但 spring-ai-rag 不在 classpath | 启动快速失败带明确信息，不静默退回 trim |
| IndexingPipeline 半途崩溃 | 逐文档逐 sink 保存状态；重入测试模拟中断点续跑 |
| 内存状态库被误用于生产增量 | 不自动装配 + javadoc；rag-example 后续演示正确形态 |
| `withJson` 转换对畸形 Map 的报错不可读 | 转换失败包装为含 indexName 与片段的明确异常 |

## 9. 实施步骤（小步 + 当场验证）

1. `RagProperties` 增量 + `ApiBaselineTest` 补表 → 模块绿。
2. P5：`QueryVariant`/`QueryTransformer`/引擎重载 + 等价性与三文本测试 → 绿（这是其余项的依赖面最小的一块，先落）。
3. P4a：`nebula-search-elasticsearch` mapping 下发 + 请求断言测试 → search 模块绿。
4. P4b：`RagSearchDocument` + `SearchServiceRetriever` + 装配与条件矩阵 → 绿。
5. P2：契约 + Planner + Pipeline + 两 Sink + 内存状态库 + 幂等与重入测试 → 绿。
6. §5 code-summary + C 侧评测记录 → 绿。
7. `nebula-ai-spring` 适配器（optional spring-ai-rag）+ 快速失败装配测试 → 绿。
8. 收尾：nebula 全仓 test（≥1174 非跳过只增）→ install → SIA 347 联动 → fullstack-example search 组 E2E → 请批提交。

估算：约 3 人周（上位 §8 口径）；派 opus 子代理一轮实现，总控独立复核。

## 10. 待拍板决策（可只答「是 / 否」）

| ID | 决策 | 推荐 |
|---|---|---|
| R2-D1 | mapping 下发经「Map → JSON → withJson」通用转换，不逐字段建模 | 是 |
| R2-D2 | filter 许可集首版仅 `docId`/`chunkType`/`breadcrumb` 等值，范围过滤不开 | 是 |
| R2-D3 | Spring AI 改写器适配纳入本期（optional spring-ai-rag 依赖落 nebula-ai-spring） | 是 |
| R2-D4 | 框架默认向量检索器补 `@Order(10)` | 是 |
| R2-D5 | J10（Qdrant id-mapping）以文档三道软防线处置，不做运行期探测 | 是 |
| R2-D6 | code-summary 纳入本期为默认关的可选项，评测仅记录不设门禁 | 是 |
| R2-D7 | 批准后派 opus 子代理按 §9 实施 | 是 |
