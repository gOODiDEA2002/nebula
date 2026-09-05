# RAG 后续小修详细设计（F-1 / F-4 / F-5）

- 版本：v1（2026-09-05）
- 上游：`docs/design/rag-example-design.md` §12 验收记录（F-2/F-3 已修，F-1/F-4/F-5 记录未改）
- 状态：已批准，已实现（待验收）

## 1. 目标与范围

修复 rag-example 验收阶段暴露的三处框架缺陷，全部满足 Y1（二进制向后兼容）与 Y2（默认行为不变）。

| 编号 | 缺陷 | 修复位置 | 默认行为是否变化 |
|------|------|---------|----------------|
| F-1 | `nebula.ai.rag.rerank.http.url` 配为空串时仍装配 HTTP 重排 | `RagAutoConfiguration.HttpRerankConfiguration` | 否（空串场景本身即缺陷） |
| F-4 | 生成器返回空正文（如推理模型 `finish_reason=length`）不判降级 | `DefaultRagPipeline`、`RagProperties.Degrade` | 否（新键默认关） |
| F-5 | `vectorStoreIndexSink` / `searchServiceIndexSink` 无 `@Order`，写目标顺序不确定 | `RagAutoConfiguration.IndexingConfiguration` | 否（仅固定顺序） |

不在范围：SIA 回流接入 R4 新键；重排/生成其它行为调整。

## 2. 事实依据

- `HttpRerankConfiguration` 现用 `@ConditionalOnProperty(prefix = "nebula.ai.rag.rerank.http", name = "url")`，Spring 对空串判为「已设置」；同文件 `SearchIndexNamePresentCondition` 已有非空自定义 Condition 先例。
- `DefaultRagPipeline.generate()` 仅在 `TimeoutException` / `InterruptedException` / 其它异常时置 `degradeReason`，正常返回的 `raw` 不做空白检查；`streamDeltas()` 完成时无条件按 `accumulator` 发 `COMPLETE(degraded=false)`，`anyDelta` 只要收到片段（含空串）即置真。
- `ChatServiceAnswerGenerator.generate()` 返回 `response.getContent()`，可能为 `null` 或空串；端口 `AnswerGenerator` 返回 `String`，管线看不到 `finishReason`。
- `vectorStoreIndexSink`（`@ConditionalOnBean(VectorStoreService)`）与 `searchServiceIndexSink` 均无 `@Order`；`indexingPipeline` 通过 `sinks.orderedStream().toList()` 取序。检索侧先例：`vectorStoreRetriever` `@Order(10)`，`OrderedSearchServiceRetriever` 默认 20。
- SIA 影响核对：SIA `application.yml` 的 `rerank:` 块只有 `enabled/top-k/timeout-ms`，无 `http.url`；`application-prod.yml` 为 `rerank.enabled=false`。SIA 自定义 `AnswerGenerator`（`LlmRagConfiguration.ragAnswerGenerator`），未实现 `IndexSink`。三处修复对 SIA 现有配置零影响。

## 3. 设计

### 3.1 F-1：HTTP 重排 url 非空才装配

- 在 `RagAutoConfiguration` 新增 `static class RerankHttpUrlPresentCondition implements Condition`：读 `nebula.ai.rag.rerank.http.url`，`null` 或空白返回 `false`。写法与 `SearchIndexNamePresentCondition` 一致。
- `HttpRerankConfiguration` 的 `@ConditionalOnProperty(...)` 改为 `@Conditional(RerankHttpUrlPresentCondition.class)`；类注释补一句「空串视为未配置，走 `noopReranker`」。
- 测试：`RagR4ConditionTest` 新增 `rerankHttpUrlBlank_usesNoopReranker`（`nebula.ai.rag.rerank.http.url=` 空值，断言 `Reranker` 为 `NoopReranker` 且无 `HttpCrossEncoderReranker`）。既有 `rerankHttpUrlAbsent_*`、`rerankHttpUrlSet_*`、`userRerankerBean_*` 保持通过。

### 3.2 F-4：空正文降级（新键默认关）

配置：
- `RagProperties.Degrade` 新增 `private boolean onEmptyAnswer = false;`，键 `nebula.ai.rag.degrade.on-empty-answer`，注释说明适用场景（推理模型思考耗尽 `max-tokens` 返回空正文）。Lombok 生成 `isOnEmptyAnswer()/setOnEmptyAnswer()`，属于新增方法，满足 Y1。

常量：
- `DefaultRagPipeline` 新增 `public static final String REASON_EMPTY_ANSWER = "empty-answer";`。

阻塞式 `generate()`：
- `raw = future.get(...)` 之后：若 `properties.getDegrade().isOnEmptyAnswer()` 且 `raw == null || raw.isBlank()`，则 `answer = buildFallbackAnswer(summaryRefs)`、`degradeReason = REASON_EMPTY_ANSWER`、`outcome = "empty"`，并 `log.warn`；否则维持 `citationPostProcessor.process(raw, assembly)`。
- 开关关闭时代码路径与现状完全一致（`raw` 为 `null` 时 `citationPostProcessor.process(null, ...)` 的现有行为不改）。

流式 `streamDeltas()`：
- 开关开启时：`map` 阶段空白片段不置 `anyDelta`、不向下游发 `DELTA`（仍追加到 `accumulator`，无副作用）；完成阶段改为 `Flux.defer`：若 `accumulator` 内容空白，则发 `DELTA(fallback)` + `COMPLETE(degraded=true, reason=empty-answer)`，并 `recordStage("generation", ..., "empty")`、`recordQuery(..., true, REASON_EMPTY_ANSWER)`；否则维持现状。
- 开关关闭时：`map` 与完成逻辑与现状逐字一致（空片段照发、`anyDelta` 照置、`COMPLETE` 不降级）。
- `onErrorResume` 分支不改；开关开启时因 `anyDelta` 只计非空片段，「只收到空片段后出错」会走「首片段前失败」降级路径，这是期望行为。

指标：`generation` 阶段新增 outcome 取值 `empty`；`recordQuery` 新增 reason 取值 `empty-answer`。标签基数各加一，可接受。

示例：`examples/rag-example/src/main/resources/application.yml` 增加 `degrade.on-empty-answer: true`（示例即演示该能力）。

测试：
- `DefaultRagPipelineTest`：`emptyAnswer_degradesWhenEnabled`（生成器返回 `""` 与 `null` 两种，断言 `degraded=true`、`degradeReason=empty-answer`、答案含 `fallbackHeader`、指标记录 `empty`）；`emptyAnswer_notDegradedByDefault`（默认关，生成器返回 `""`，断言 `degraded=false`、答案为空串，锁定 Y2）。
- `DefaultRagPipelineStreamTest`：`blankDeltasOnly_degradesToSummaryWhenEnabled`（流发 `""`、`""` 后完成，断言事件恰为 `DELTA(fallback)`、`COMPLETE(degraded, empty-answer)`）；`blankDeltasOnly_completesNonDegradedByDefault`（默认关，断言两条空 `DELTA` + 非降级 `COMPLETE`）；`errorAfterBlankDeltas_degradesWhenEnabled`（开关开，空片段后出错，断言走摘要降级而非 `ERROR`）。

### 3.3 F-5：写目标固定顺序

- `vectorStoreIndexSink` 加 `@Order(10)`，`searchServiceIndexSink` 加 `@Order(20)`，与检索侧 10/20 对齐（向量先写，关键词后写）。`@Order` 标在 `@Bean` 方法上，`ObjectProvider.orderedStream()` 会读取。
- 不引入按 `search.order` 动态排序：写目标只需确定性，不需要与检索权重联动。
- 测试：`RagIndexingConditionTest` 新增 `indexSinks_orderedVectorThenSearch`（同时提供 `VectorStoreService`、`SearchService` 与 `search.index-name`，断言 `getBeanProvider(IndexSink.class).orderedStream()` 类型序为 `[VectorStoreIndexSink, SearchServiceIndexSink]`）。

### 3.4 文档

- `docs/design/rag-example-design.md` §12：F-1/F-4/F-5 状态改为「已修（本设计）」。
- `docs/framework/RAG_USAGE_GUIDE.md` §4.4 配置矩阵增加 `nebula.ai.rag.degrade.on-empty-answer`（默认 `false`）一行。

## 4. 兼容性核对

- Y1：只新增 Condition 类、常量、属性及其访问器、`@Order` 注解；无签名改动、无删除。`ApiBaselineTest` 必须原样通过。
- Y2：F-4 新键默认 `false`，关闭时代码路径逐字不变；F-1 只影响显式配空串的场景；F-5 只固定顺序。SIA 测试与生产 yml 零改动照跑。
- Y5：不引入任何供应商 SDK。

## 5. 实施与验证

- 派工：opus 一轮，范围为 §3 全部条目；总控独立验收。
- 验证顺序：
  1. `mvn -o test -pl autoconfigure/nebula-autoconfigure -Dtest='Rag*Test'`
  2. `mvn -o test -pl infrastructure/ai/nebula-ai-rag`（含 `ApiBaselineTest`）
  3. 全仓 `mvn -o test`，以 surefire XML 汇总，期望基线 1363 + 新增用例数，失败/错误为 0
  4. SIA `source-insight-llm` 离线联动（安装 nebula 到本地仓后跑 SIA llm 测试，基线 366/0/0/0，不改 SIA 任何文件）
  5. rag-example 独立脚本 Full E2E（`examples/rag-example/e2e-test.sh`），重点看流式用例在 `on-empty-answer=true` 下的降级事件
- 提交：一次 `fix(ai-rag): F-1/F-4/F-5 重排空 url、空正文降级与写目标顺序`，提交与推送前先问用户。
