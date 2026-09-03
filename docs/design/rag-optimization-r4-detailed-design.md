# RAG 优化 R4 详细设计（HTTP 交叉编码重排、可信上下文、流式生成与指标）

> 状态：详细设计稿（v1，2026-09-03，已批准，进入实施）
> 上位设计：`docs/design/rag-framework-optimization-design.md` v2（已批准）§5 P6、P7，§8 R4
> 前序：`docs/design/rag-optimization-r3-detailed-design.md`（已交付，提交 `8f13d909`，Nexus 2.1.1-SNAPSHOT buildNumber 4）
> 范围：`nebula-ai-rag` 新增 `rerank.http` 包（HTTP 交叉编码重排）、`guard` 包（检索内容清洗端口与正则实现）、`ContextAssembly` 结构与引用后处理端口、流式生成端口与事件契约、指标内部端口与 Micrometer 适配；`nebula-autoconfigure` 的条件装配；`DefaultRagPipeline` 新增完整构造器
> 兼容口径：Y1 兼容增量（`ApiBaselineTest` 门禁在岗，仅追加 `r4Additions`）/ Y2 新键默认关、默认行为不变 / Y5 供应商 SDK 不进 `nebula-ai-rag`

## 1. 现状事实与范围重定（2026-09-03 核实）

### 1.1 与设计强相关的现状事实

| 事实 | 出处 | 对设计的含义 |
|---|---|---|
| `RagPipeline` 只有 `RagAnswer query(RagQuery)` 一个抽象方法，`ApiBaselineTest.ragPipeline_keepsQueryMethod` 锁定 | `pipeline/RagPipeline.java`；`ApiBaselineTest.java:117-122` | 流式入口只能以 **default 方法**追加，不能新增抽象方法 |
| `DefaultRagPipeline` 有 6 参与 7 参（含 `QueryTransformer`）两个公开构造器，`ApiBaselineTest` 锁定 6 参 | `DefaultRagPipeline.java:60-90`；`ApiBaselineTest.defaultRagPipeline_sixArgConstructor` | R4 新增完整构造器，6/7 参原样保留并委托、注入 Noop 默认 |
| `query()` 流程：改写 → 检索 → 空则 `no-document` 降级 → `applyRerank` → 组装 → 渲染 → 虚拟线程内生成并按 `generation.timeoutMs` 超时降级为检索摘要 | `DefaultRagPipeline.java:93-135,166-204` | 清洗、指标、引用后处理都是在既有流程上**插入钩子**，不改动顺序与降级语义 |
| `applyRerank`：`request.enableRerank` 优先于 `rerank.enabled`；`topK` 来自 `rerank.topK`；关闭时按融合顺序截断 | `DefaultRagPipeline.java:152-161` | HTTP 重排失败直通时的「融合原序截断」与关闭重排时的行为**同一条代码路径** |
| `RagProperties.Rerank.timeoutMs`（默认 3000）**当前无任何消费者** | `RagProperties.java:221-260`；全仓 grep `getRerank().getTimeoutMs` 为空 | R4 赋予其含义：HTTP 重排的独立超时，不新增键（R4-D2） |
| `Reranker` 只有 `rerank(query, results, topK)` 与 `getName()`，`ApiBaselineTest.reranker_keepsTwoMembers` 锁定 | `ApiBaselineTest.java:90-96` | HTTP 实现只实现接口，接口零改动；SIA 的 `BgeReranker` 不受影响 |
| `Reranker` 单槽：`noopReranker` 带 `@ConditionalOnMissingBean(Reranker.class)`；SIA 以自定义 Bean 占槽 | `RagAutoConfiguration.java`；SIA `BgeReranker` | HTTP 重排必须**先于** `noopReranker` 注册且同样让位于用户 Bean：落在嵌套配置类（成员类先于外层 `@Bean` 方法处理，R3 已用此手法） |
| `ContextAssembler.assemble(List)` 用 `String.format(documentTemplate, i+1, content)` 拼接，按字符预算在首个溢出处停止 | `ContextAssembler.java` | 序号 `i+1` 就是「入选序号」，天然是引用映射的键；`ContextAssembly` 只是把已有的中间态暴露出来 |
| `RagAnswer`、`RetrievalResult` 均为 `@Data @Builder @NoArgsConstructor @AllArgsConstructor` | `RagAnswer.java:22-26`；`RetrievalResult.java:19-23` | **给这两个类加字段会改变公开全参构造器签名，违反 Y1**。R4 不给二者加任何字段（R4-D5） |
| `ChatService.chatStream(String\|List\|ChatRequest, ChatStreamCallback)` 回调式流；`ChatStreamCallback{onChunk(String), onComplete(ChatResponse), onError(Throwable)}` | `ChatService.java:70-86,112-133` | 任何 `ChatService` 实现（含 SIA 的 `ChatServiceWrapper`）都能桥接成 `Flux` |
| `ReactiveChatService.chatStream(ChatRequest) -> Flux<ChatStreamChunk>`；`SpringAIChatService` 同时实现两者；但 `AIAutoConfiguration.chatService` 工厂方法返回类型是 `ChatService` | `ReactiveChatService.java:27`；`AIAutoConfiguration.java:242-245` | 容器在实例化前按工厂方法返回类型预测 Bean 类型，`@ConditionalOnBean(ReactiveChatService.class)` **装配期不可靠**。默认适配器只依赖 `ChatService` 回调桥接（R4-D6） |
| `reactor-core` 是 `nebula-ai-core` 的 compile 依赖，传递到 `nebula-ai-rag` | `nebula-ai-core/pom.xml` | `Flux` 可直接出现在 `nebula-ai-rag` 公开签名中，无新增依赖 |
| 全仓无 `MeterRegistry` 使用；根 POM 由 Boot 4.1 BOM 管 Micrometer 版本；本地 m2 有 micrometer-core 1.9.17 | grep 全仓；`pom.xml` 注释 | `nebula-ai-rag` 新增 `micrometer-core` optional（总纲允许的两个新依赖之一） |
| `nebula-ai-rag` 无 `spring-web`/`RestClient` 依赖；Jackson 3 经 `nebula-foundation` 的 `JsonUtils` 可用 | `nebula-ai-rag/pom.xml:22-72` | HTTP 客户端用 JDK 21 `java.net.http.HttpClient`（零新依赖，R4-D1） |
| TEI `POST /rerank`：请求 `{query, texts[], truncate?, raw_scores?}`，响应数组 `[{index, score, text?}]`，服务端已按 score 降序 | context7 `/huggingface/text-embeddings-inference`（`router/src/http/types.rs`） | `tei` wire 格式的编解码依据 |
| Cohere `POST /v2/rerank`：请求 `{model, query, documents[], top_n?}`，响应 `{results:[{index, relevance_score}]}` | context7 Cohere OpenAPI | `cohere` wire 格式的编解码依据。Jina / vLLM / Xinference 的 `/v1/rerank` 与之同形（**推断**，本期以契约测试覆盖 nebula 侧编解码，不对第三方实测） |
| SIA 生产 `rerank.enabled=false`，dev `top-k 5 / timeout-ms 3000`；`RagPipelineImpl` 把 `RagAnswer.references` 逐条映射为 DTO 返回前端 | SIA `application*.yml`；`RagPipelineImpl` | `references` 语义若改为「入选引用」，SIA 前端引用条数会变少。必须走新键默认 `all` |
| SIA `AgentSafetyPatterns.PROMPT_INJECTION` 正则（中英文指令覆盖、角色扮演、`system:` 前缀、`<system>` 标签、jailbreak/DAN）；`CloudFieldSanitizer` 命中即**整值替换**为固定占位文本 | SIA `AgentSafetyPatterns`、`CloudFieldSanitizer` | `PatternSanitizer` 内置默认正则移植该表达式，默认模式为整值替换 |
| Y8 重排延迟教训：批量、独立超时、失败直通融合原序，三条缺一不可 | 交接文件硬约束；SIA 记忆（MATCHING_RERANK 8s 预算、超时回退 `rerankApplied=false`） | `HttpCrossEncoderReranker` 三条同时满足，任一失败都不得抛到管线 |

### 1.2 范围重定

上位 §8 给 R4 的估算是约 2 人周（P6 + P7）。本期范围与上位一致，做如下三处澄清：

1. **引用后处理只做端口与 Noop**。目前没有消费方要求具体的引用改写规则（角标、脚注、去重），做实现是纯猜测；端口冻结后消费方可以 lambda 提供。
2. **流式做到「可用」而不是「可选装饰」**：端口 + 默认适配器 + `DefaultRagPipeline` 的真实实现 + 事件契约冻结（J14）。`RagPipeline.queryStream` 的 default 实现只在端口缺席或第三方 `RagPipeline` 实现未覆盖时返回「暂不支持」事件。
3. **J5 三家 wire 实测降级为契约测试**：本期以 JDK 内置 `HttpServer` 模拟 TEI 与 Cohere 两种 wire 格式做契约测试；真实 TEI 容器实测列为可选手工项；Cohere 需付费 key，不做真实实测（R4-D9）。

### 1.3 必须如实说明的前提

- SIA 生产关闭重排，dev 用 LLM 打分重排；SIA 不会在 R4 交付后立即切换到 HTTP 重排。
- SIA 是否需要框架流式：未核实（SIA 有自己的会话与输出通道，本期不做假设）。
- 因此 R4 的验收以框架内测试与条件矩阵为主，真实后端 E2E 与 R3 一样留待 rag-example。

## 2. 目标与非目标

### 2.1 目标

1. 提供可直接对接 TEI / Cohere 风格 `/rerank` 服务的 `HttpCrossEncoderReranker`，满足 Y8 三条，显式配 URL 才装配，与既有 `Reranker` 同槽互斥。
2. 在上下文组装前提供检索内容清洗端口，默认 Noop；提供移植 SIA 手法的 `PatternSanitizer`。
3. 把上下文组装的中间态以 `ContextAssembly` 暴露，供引用后处理与「入选引用」语义使用；旧 `assemble(List)` 行为逐字节不变。
4. 冻结流式事件契约（顺序、终态、取消、背压、中途失败、超时），提供端口、默认适配器与管线实现。
5. 提供指标内部端口与 Micrometer 适配，缺席即 Noop，验收从 `MeterRegistry` 读取。

### 2.2 非目标

- 不改 `Reranker`、`AnswerGenerator`、`RagPromptRenderer`、`RagPipeline.query` 签名；不给 `RagAnswer`、`RetrievalResult`、`RagQuery` 加字段。
- 不做 SSE/WebSocket 传输层；框架只产出 `Flux<RagStreamEvent>`，传输由应用层负责。
- 不做 LLM 侧输出的安全审查（只做检索内容进入上下文前的清洗）。
- 不暴露指标公开端口，不动 `/performance`（J16）。
- 不在流式路径上对 DELTA 做引用改写；`CitationPostProcessor` 只在终态处理完整答案文本。

## 3. P6：HTTP 交叉编码重排（包 `io.nebula.ai.rag.rerank.http`）

### 3.1 类与职责

```java
/** wire 格式编解码：请求体编码与响应解码分离，便于加第三种格式而不改重排器 */
public interface RerankWireCodec {
    /** 编码一批候选为请求体 JSON */
    String encode(String query, List<String> texts, String model);
    /** 解码响应为 (index, score) 列表；条数、索引范围由调用方校验 */
    List<ScoredIndex> decode(String responseBody);
    record ScoredIndex(int index, double score) {}
}

/** TEI: {query, texts, truncate:true} -> [{index, score}] */
public class TeiRerankWireCodec implements RerankWireCodec { ... }

/** Cohere 风格: {model, query, documents, top_n} -> {results:[{index, relevance_score}]} */
public class CohereRerankWireCodec implements RerankWireCodec { ... }

/** 通过 HTTP 调用交叉编码服务打分的重排器（Y8：批量、独立超时、失败直通） */
public class HttpCrossEncoderReranker implements Reranker {
    public static final String NAME = "http-cross-encoder";

    public HttpCrossEncoderReranker(HttpClient httpClient, URI url, RerankWireCodec codec,
                                    String model, String apiKey, long timeoutMillis,
                                    int batchSize, int maxCharsPerDoc, RagMetrics metrics);

    @Override public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK);
    @Override public String getName() { return NAME; }
}
```

- `RerankWireCodec` 公开且非 final，第三种 wire 格式由使用方实现后以自定义 `Reranker` Bean 组装，无需改 nebula。
- JSON 编解码统一走 `io.nebula.core.util.JsonUtils`（Jackson 3），不引入新序列化器。
- `HttpClient` 由构造器注入（装配层创建，`connectTimeout` 取 `timeoutMillis`），测试可替换。

### 3.2 算法与 Y8 三条

```
rerank(query, results, topK):
  1. results 空或 size <= 1 -> 直接截断返回（不发请求）
  2. texts = results.content（maxCharsPerDoc > 0 时按字符截断）
  3. 按 batchSize 切批；每批一个 HttpRequest（POST url, Content-Type application/json,
     apiKey 非空时 Authorization: Bearer）, 请求级 timeout = timeoutMillis
  4. 所有批 sendAsync 并发发出；CompletableFuture.allOf(...).get(timeoutMillis)
     -> 任一批超时、非 2xx、解码失败、返回条数与批大小不符、index 越界：
        取消全部 future，metrics.recordRerankPassthrough(reason)，warn 一条，
        返回 results.stream().limit(topK)（融合原序，与 rerank.enabled=false 同一路径）
  5. 全部成功：按批内 index 回填全局下标，score 写入 RetrievalResult 副本
     （builder 复制 id/content/metadata/source，score 替换为交叉编码分），
     按 score 降序稳定排序，取 topK
```

- **批量**：一批一个请求，多批并发，总耗时受单个 `timeoutMillis` 约束而不是批数乘超时。
- **独立超时**：`rerank.timeout-ms` 只约束重排，与 `retrieval.timeout-seconds`、`generation.timeout-ms` 互不影响。
- **失败直通**：任何异常都在重排器内吞掉，管线看不到差异；不做「部分批成功就混排」（未打分候选的相对位置无定义，会产生不可解释的排序）。
- 分数语义：以交叉编码分替换 `score`，不与融合分加权；候选来源 `source` 保留，融合分不保留（`metadata` 是消费方的 Map，不注入新键，避免污染）。

### 3.3 装配（`RagAutoConfiguration` 嵌套配置类 `HttpRerankConfiguration`）

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "nebula.ai.rag.rerank.http", name = "url")
static class HttpRerankConfiguration {
    @Bean
    @ConditionalOnMissingBean(Reranker.class)
    public Reranker httpCrossEncoderReranker(RagProperties properties, ObjectProvider<RagMetrics> metrics) {
        // wire-format 解析 tei|cohere，非法值抛 IllegalArgumentException 启动失败
        // cohere 且 model 为空 -> 启动失败（Cohere 请求体 model 必填）
    }
}
```

- 显式配 `url` 才装配，缺 `url` 时既有 `noopReranker` 路径零变化（Y2）。
- 用户自定义 `Reranker` Bean（SIA `BgeReranker`）先于自动配置注册，`@ConditionalOnMissingBean` 让位；嵌套配置类先于外层 `noopReranker` 处理，因此 `url` 配置时 HTTP 实现胜出。装配顺序由条件测试固化（§9）。

## 4. P7-a：可信上下文与引用（包 `io.nebula.ai.rag.guard`、`io.nebula.ai.rag.pipeline`）

### 4.1 `RetrievedContentSanitizer` 端口与 `PatternSanitizer`

```java
/** 检索内容进入重排与上下文前的清洗端口；返回 null 表示剔除该条 */
@FunctionalInterface
public interface RetrievedContentSanitizer {
    RetrievalResult sanitize(RetrievalResult result);

    default List<RetrievalResult> sanitizeAll(List<RetrievalResult> results) {
        // 逐条 sanitize，过滤 null，保持原顺序
    }
}

public class NoopRetrievedContentSanitizer implements RetrievedContentSanitizer { /* 原样返回 */ }

/** 正则命中即整值替换或剔除（移植 SIA CloudFieldSanitizer 手法） */
public class PatternSanitizer implements RetrievedContentSanitizer {
    /** 移植 SIA AgentSafetyPatterns.PROMPT_INJECTION，CASE_INSENSITIVE */
    public static final String DEFAULT_PATTERN = "忽略(以上|之前|前面|上述)(的)?(指令|提示|要求|设定)|(你现在是|你是一个|假装你|扮演)|(system|assistant|user)\\s*[:：]|</?(system|prompt|instruction)>|ignore\\s+(all\\s+)?(previous|above|prior)\\s+(instructions|prompts)|jailbreak|dan\\s+mode";
    public static final String DEFAULT_REPLACEMENT = "[内容因安全策略未进入上下文]";
    public enum Mode { REPLACE, DROP }

    public PatternSanitizer(List<Pattern> patterns, Mode mode, String replacement);
    // REPLACE：content 整值替换为 replacement，其余字段保留（引用仍可定位到原文档）
    // DROP：返回 null，该条从候选中剔除
}
```

**清洗位置：融合之后、重排之前**（R4-D4）。理由：SIA 的 `BgeReranker` 与框架 `LlmScoringReranker` 都会把候选内容送进 LLM 打分，注入文本若在重排后才清洗，已经进过一次模型。代价是清洗量从 `topK` 变为 `topK * candidateMultiplier`，正则清洗量级为微秒，可接受。

清洗后的列表同时用于重排、上下文与 `references`（保持一致，避免前端展示未清洗文本又被复制回提示词）。

### 4.2 `ContextAssembly` 与 `ContextAssembler.assembleDetailed`

```java
/** 上下文组装结果：正文 + 入选引用 + 序号映射（序号与 documentTemplate 的 %d 一致，从 1 起） */
public final class ContextAssembly {
    public String getContext();
    public List<RetrievalResult> getIncludedReferences();   // 与 context 中出现顺序一致
    public Map<Integer, RetrievalResult> getCitationMap();   // 1 -> 第一篇
    public int getOmittedCount();                            // 因预算未入选的条数
}

public class ContextAssembler {
    // 既有
    public String assemble(List<RetrievalResult> results) { return assembleDetailed(results).getContext(); }
    // 新增
    public ContextAssembly assembleDetailed(List<RetrievalResult> results);
}
```

行为保持证明：`assemble` 改为委托后，测试对多组输入断言新旧输出字符串逐字相同（含预算刚好溢出、空列表、单条超预算）。

### 4.3 `CitationPostProcessor` 端口

```java
/** 对完整答案文本做引用改写（角标、脚注、去重等）；只消费 ContextAssembly */
@FunctionalInterface
public interface CitationPostProcessor {
    String process(String answer, ContextAssembly assembly);
}
public class NoopCitationPostProcessor implements CitationPostProcessor { /* 原样返回 */ }
```

- 管线只在**生成成功**（非降级摘要）时调用；流式路径只在 COMPLETE 前对拼接后的完整文本调用，DELTA 不改写。
- 无配置键：默认 Noop Bean，用户提供 Bean 即生效（Y2 默认行为不变，因 Noop 零效果）。

### 4.4 `references` 语义：`context.references-mode`

| 值 | `RagAnswer.references` 内容 | 说明 |
|---|---|---|
| `all`（默认） | 重排后的全部 topK（现状） | SIA 零变化 |
| `included` | `ContextAssembly.includedReferences` | 第 n 条即 `[文档n]`，引用映射不需要新字段 |

不给 `RagAnswer` 加 `citations` 字段（R4-D5）：`@AllArgsConstructor` 使加字段等于改公开构造器签名，违反 Y1；`included` 模式下顺序即映射，已满足引用定位需要。

## 5. P7-b：流式生成（包 `io.nebula.ai.rag.pipeline`）

### 5.1 端口与默认适配器

```java
/** 流式答案生成端口：只产出文本增量；超时、降级、终态由管线统一负责 */
@FunctionalInterface
public interface StreamingAnswerGenerator {
    Flux<String> generateStream(String prompt);
}

/** 用 ChatService.chatStream(String, ChatStreamCallback) 桥接为 Flux（单路径，R4-D6） */
public class ChatServiceStreamingAnswerGenerator implements StreamingAnswerGenerator {
    public ChatServiceStreamingAnswerGenerator(ChatService chatService);
    // Flux.create(sink -> chatService.chatStream(prompt, callback{
    //     onChunk -> sink.next; onComplete -> sink.complete; onError -> sink.error }),
    //   FluxSink.OverflowStrategy.BUFFER)
    // sink.onCancel: 置取消标记，后续 onChunk 丢弃（回调式 API 无法中止上游请求）
}
```

单抽象方法，SIA 可用 lambda 提供（与 `AnswerGenerator` 惯例一致）。

### 5.2 `RagStreamEvent`（新类，Lombok 风格与 `RagAnswer` 一致）

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RagStreamEvent {
    public enum Type { REFERENCES, DELTA, COMPLETE, ERROR }
    private Type type;
    private List<RetrievalResult> references;   // REFERENCES
    private String delta;                       // DELTA
    private RagAnswer answer;                   // COMPLETE：answer 为拼接后的完整文本（已过 CitationPostProcessor）
    private String errorReason;                 // ERROR：与 DefaultRagPipeline.REASON_* 同一套常量
    private String errorMessage;                // ERROR
    // 静态工厂 references(...) / delta(...) / complete(...) / error(reason, message)
}
```

新类加字段不受 Y1 约束（本期首次出现），但一旦发布即冻结，后续加字段需走 builder 而不能依赖全参构造器。

### 5.3 事件契约（J14 冻结）

| 项 | 规则 |
|---|---|
| 顺序 | 恰好一个 `REFERENCES` 在最前；随后 0..n 个 `DELTA`；最后恰好一个终态（`COMPLETE` 或 `ERROR`）；终态后 Flux `onComplete` |
| Flux 错误信号 | 生成失败、超时**不**走 `onError`，一律以 `ERROR` 事件表达后正常完成，保证传输层能收到格式完整的结尾。`onError` 只用于编程错误（如取消后重复订阅） |
| 参数非法 | 与 `query()` 一致：`Flux.error(IllegalArgumentException)`（订阅期抛出，不发任何事件） |
| 无文档 | `REFERENCES([])` → `DELTA(degrade.no-document-answer)` → `COMPLETE(degraded, no-document)` |
| `generateAnswer=false` | `REFERENCES(refs)` → `COMPLETE(answer=null, references=refs)` |
| 端口缺席 | `ERROR(streaming-unsupported)` 后完成；不发 `REFERENCES`（不执行检索，避免白跑一次检索） |
| 首个 DELTA 之前失败或超时 | 与 `query()` 对齐：`DELTA(检索摘要降级答案)` → `COMPLETE(degraded, generation-failed / generation-timeout)` |
| 首个 DELTA 之后失败或超时 | `ERROR(generation-failed / generation-timeout)` 终止；**不**补摘要（客户端已有部分文本，再追加摘要会产生拼接错乱）（R4-D7） |
| 超时 | `generation.timeout-ms` 为从订阅到终态的**总时限**（`takeUntilOther(Mono.delay)` 实现），不是字间间隔 |
| 取消 | 下游 `cancel` 向上游 Flux 传播；不再发任何事件；上游 LLM 请求是否中止取决于适配器（回调桥接只能丢弃后续 chunk）。指标记 `outcome=cancelled` |
| 背压 | `BUFFER` 策略；LLM 出字速率远低于消费者，缓冲上界即单次答案长度。不做 `DROP`/`LATEST`（会丢字） |
| 线程 | 检索、清洗、重排、组装在订阅时于 `Schedulers.boundedElastic()` 执行（同步阻塞调用，不能占用调用方的事件循环线程）；DELTA 在适配器回调线程上发出 |
| 常量 | `DefaultRagPipeline` 新增 `public static final String REASON_STREAMING_UNSUPPORTED = "streaming-unsupported"` |

### 5.4 `RagPipeline.queryStream` 与 `DefaultRagPipeline` 实现

```java
public interface RagPipeline {
    RagAnswer query(RagQuery query);                       // 既有
    default Flux<RagStreamEvent> queryStream(RagQuery query) {
        return Flux.just(RagStreamEvent.error(DefaultRagPipeline.REASON_STREAMING_UNSUPPORTED,
                "当前 RagPipeline 实现不支持流式"));
    }
}
```

`DefaultRagPipeline.queryStream` 覆盖实现：`streamingGenerator == null` 时返回同样的「暂不支持」事件；否则按 §5.3 契约执行。**不得**从容器直接取 `ReactiveChatService`（总纲红线）。

### 5.5 装配

- `nebula.ai.rag.streaming.enabled=false`（默认）：不装配 `StreamingAnswerGenerator`，管线注入 null，`queryStream` 返回「暂不支持」。
- `enabled=true` 且容器有 `ChatService`：装配 `ChatServiceStreamingAnswerGenerator`（`@ConditionalOnMissingBean(StreamingAnswerGenerator.class)`）。
- `enabled=true` 但无 `ChatService`：不装配并 warn 一条（与 `defaultAnswerGenerator` 的 `@ConditionalOnBean(ChatService)` 处置一致）。

## 6. P7-c：指标（包 `io.nebula.ai.rag.metrics`）

### 6.1 内部端口

```java
/** 管线内部指标端口；不对外承诺稳定，消费方读 MeterRegistry 而不是实现本接口 */
public interface RagMetrics {
    void recordStage(String stage, long durationNanos, String outcome);   // stage: retrieval|rerank|assemble|generation
    void recordQuery(long durationNanos, boolean degraded, String reason); // reason 未降级时为 "none"
    void recordRerankPassthrough(String reason);                           // timeout|http-error|decode-error|mismatch
}
public class NoopRagMetrics implements RagMetrics { ... }
```

### 6.2 Micrometer 适配

```java
/** 只有本类引用 io.micrometer 类型；由 @ConditionalOnClass(MeterRegistry) 守护加载 */
public class MicrometerRagMetrics implements RagMetrics {
    public MicrometerRagMetrics(MeterRegistry registry);
}
```

| Meter 名 | 类型 | 标签 | 说明 |
|---|---|---|---|
| `nebula.rag.stage.duration` | Timer | `stage`, `outcome`（success/failure/timeout/cancelled/passthrough） | 各阶段耗时 |
| `nebula.rag.query.duration` | Timer | `degraded`（true/false）, `reason`（none/no-document/generation-timeout/generation-failed） | 整次查询耗时，含流式 |
| `nebula.rag.rerank.passthrough` | Counter | `reason` | HTTP 重排直通次数 |

标签取值全部是有限枚举，不带查询文本、URL 等无界值。

### 6.3 依赖与装配

- `nebula-ai-rag/pom.xml` 新增 `io.micrometer:micrometer-core`，`<optional>true</optional>`，版本由 Boot BOM 管理。
- `nebula.ai.rag.metrics.enabled=false`（默认）：装配 `NoopRagMetrics`。
- `enabled=true`、类路径有 `MeterRegistry` 且容器有 `MeterRegistry` Bean：装配 `MicrometerRagMetrics`；`enabled=true` 但缺类或缺 Bean：`NoopRagMetrics` 并 warn。
- 验收：测试用 `SimpleMeterRegistry` 从 registry 读 Timer/Counter；不建公开端口（J16）。

## 7. `DefaultRagPipeline` 改造与装配汇总

### 7.1 构造器

```java
// 既有（保留，委托到完整构造器，注入 Noop 默认）
public DefaultRagPipeline(HybridRetrievalEngine, Reranker, ContextAssembler, RagPromptRenderer,
                          AnswerGenerator, RagProperties)
public DefaultRagPipeline(..., RagProperties, QueryTransformer)
// R4 新增完整构造器
public DefaultRagPipeline(HybridRetrievalEngine, Reranker, ContextAssembler, RagPromptRenderer,
                          AnswerGenerator, RagProperties, QueryTransformer,
                          RetrievedContentSanitizer sanitizer, CitationPostProcessor citationPostProcessor,
                          StreamingAnswerGenerator streamingGenerator /* 可为 null */, RagMetrics metrics)
```

### 7.2 `query()` 带钩子的流程

```
resolveVariants
retrieve                       -> metrics.recordStage("retrieval", ...)
空 -> no-document 降级（不变）
sanitizer.sanitizeAll          （新增；清洗后为空同样走 no-document）
applyRerank                    -> metrics.recordStage("rerank", ...)
generateAnswer=false -> 返回（references 按 references-mode）
assembleDetailed               -> ContextAssembly
render
generate（虚拟线程 + 超时，不变） -> metrics.recordStage("generation", ...)
生成成功 -> citationPostProcessor.process(answer, assembly)
RagAnswer（references 按 references-mode）-> metrics.recordQuery(...)
```

### 7.3 `RagAutoConfiguration` 增量

| Bean | 条件 | 默认 |
|---|---|---|
| `httpCrossEncoderReranker`（嵌套 `HttpRerankConfiguration`） | `rerank.http.url` 存在 + `@ConditionalOnMissingBean(Reranker)` | 不装配 |
| `retrievedContentSanitizer` | `guard.sanitizer.enabled=true` → `PatternSanitizer`；否则 `Noop`；均 `@ConditionalOnMissingBean` | Noop |
| `citationPostProcessor` | `@ConditionalOnMissingBean` | Noop |
| `streamingAnswerGenerator` | `streaming.enabled=true` + `@ConditionalOnBean(ChatService)` + `@ConditionalOnMissingBean` | 不装配 |
| `ragMetrics`（嵌套 `MetricsConfiguration`） | `metrics.enabled=true` + `@ConditionalOnClass(MeterRegistry)` + `@ConditionalOnBean(MeterRegistry)` → Micrometer；否则 Noop | Noop |
| `ragPipeline` | 既有条件不变；改用完整构造器，`ObjectProvider<StreamingAnswerGenerator>.getIfAvailable()` | — |

## 8. 配置键增量

```yaml
nebula:
  ai:
    rag:
      rerank:
        timeout-ms: 3000               # 既有键；R4 起为 HTTP 重排的独立超时（含连接与读取）
        http:
          url:                         # 显式配置才装配 HttpCrossEncoderReranker，如 http://tei:8080/rerank
          wire-format: tei             # tei | cohere
          model:                       # cohere 格式必填（随请求发送）；tei 忽略
          api-key:                     # 可选；非空时发 Authorization: Bearer，建议经环境变量注入
          batch-size: 32               # 每个 HTTP 请求的候选数上限
          max-chars-per-doc: 0         # 0 = 不在客户端截断（TEI 走 truncate:true 服务端截断）
      guard:
        sanitizer:
          enabled: false               # 关闭 = NoopRetrievedContentSanitizer
          mode: replace                # replace（整值替换）| drop（剔除）
          replacement: "[内容因安全策略未进入上下文]"
          patterns: []                 # 空 = 内置 PatternSanitizer.DEFAULT_PATTERN；非空则完全覆盖
      context:
        references-mode: all           # all（现状）| included（只返回进入上下文的引用，序号即 [文档n]）
      streaming:
        enabled: false                 # 开启后在存在 ChatService 时装配默认流式适配器
      metrics:
        enabled: false                 # 开启后在存在 MeterRegistry 时装配 Micrometer 适配
```

Y2 核对：所有新键默认值下，装配结果与 R3 完全相同（Noop 清洗、Noop 引用、无流式、Noop 指标、`noopReranker`）；`rerank.timeout-ms` 在 SIA 的 yml 中已存在且此前无消费者，赋义后 SIA（LLM 重排）仍不消费。

## 9. 红队自检

| # | 攻击面 | 处置 |
|---|---|---|
| 1 | `RagAnswer`/`RetrievalResult` 加字段改公开全参构造器（Y1） | 不加字段；`references-mode=included` 以顺序承载映射 |
| 2 | HTTP 重排部分批成功的混排 | 禁止混排：任一批失败整体直通 |
| 3 | 重排服务返回条数与请求不符或 index 越界 | 视为解码失败直通，计 `mismatch` |
| 4 | 重排服务慢导致管线整体超时 | 独立 `rerank.timeout-ms`，`allOf().get(timeout)` 后取消全部 future |
| 5 | `api-key` 进日志 | warn 日志只打 URL 与状态码，不打请求头与请求体 |
| 6 | `HttpRerankConfiguration` 与 `noopReranker` 注册顺序反转 | 嵌套配置类先处理（R3 已用同手法）；条件测试固化「配 url 得 HTTP 实现、不配得 Noop、用户 Bean 优先」 |
| 7 | 清洗在重排后，注入文本进入 LLM 打分 | 清洗放在融合后重排前 |
| 8 | `PatternSanitizer` 正则灾难性回溯 | 内置正则无嵌套量词；用户自定义 `patterns` 在装配期 `Pattern.compile` 失败即启动失败 |
| 9 | 清洗后全部剔除 | 与检索为空同路径 `no-document` 降级 |
| 10 | `assemble` 委托后行为漂移 | 逐字节等价测试（含预算边界） |
| 11 | 流式 `onError` 让 SSE 客户端收到半截流 | 生成失败一律以 `ERROR` 事件表达后 `onComplete` |
| 12 | 首 DELTA 后失败再补摘要造成拼接错乱 | 只发 `ERROR` 不补摘要（R4-D7） |
| 13 | 流式检索阶段在调用方线程阻塞 | `subscribeOn(boundedElastic)` |
| 14 | `Flux.create` BUFFER 无上界 | 上界即单次答案长度；文档化而非加 DROP（会丢字） |
| 15 | 取消后适配器继续 `sink.next` | `onCancel` 置标记后丢弃；`FluxSink` 取消后 `next` 本身也是 no-op |
| 16 | `@ConditionalOnBean(ReactiveChatService)` 装配期不可靠 | 不依赖它；默认适配器只用 `ChatService` 回调桥接 |
| 17 | `queryStream` default 方法在第三方 `RagPipeline` 实现上被误当作可用 | default 实现返回 `streaming-unsupported` 事件，不执行检索 |
| 18 | Micrometer 缺席时 `MicrometerRagMetrics` 类加载失败 | 只有该类引用 micrometer 类型；`@ConditionalOnClass` 守护；Noop 路径不触碰该类 |
| 19 | 指标标签无界（查询文本、URL） | 标签全部为有限枚举 |
| 20 | 新增 reactor-test/micrometer 测试依赖被误升为 compile | `reactor-test` test scope；`micrometer-core` optional（测试可直接用） |
| 21 | SIA 347 联动 | 推送前 `mvn -o install -DskipTests` 后跑 SIA llm 模块测试；SIA yml 零改动 |

## 10. 测试计划

| 测试类 | 覆盖 |
|---|---|
| `TeiRerankWireCodecTest` / `CohereRerankWireCodecTest` | 编码字段（tei 带 `truncate:true`；cohere 带 `model`、`top_n=texts.size()`）；解码 index/score；缺字段与非 JSON 抛解码异常 |
| `HttpCrossEncoderRerankerTest`（JDK `com.sun.net.httpserver.HttpServer` 模拟） | 单批打分排序取 topK；`batch-size=2` 且 5 候选 → 3 个请求、全局下标回填正确；超时直通原序；5xx 直通；非法 JSON 直通；条数不符/index 越界直通；空候选与单候选不发请求；`api-key` 非空时 Bearer 头；`max-chars-per-doc` 截断；直通时 `RagMetrics.recordRerankPassthrough` 被调用且 reason 正确 |
| `PatternSanitizerTest` | 内置正则命中「忽略以上指令」「ignore previous instructions」「</system>」「DAN mode」；REPLACE 整值替换且 id/metadata/source 保留；DROP 返回 null 且 `sanitizeAll` 过滤并保序；无命中原样返回（同一实例） |
| `ContextAssemblerTest`（追加） | `assemble` 与 `assembleDetailed().getContext()` 逐字节相同（空表、单条超预算、预算刚好溢出、全部入选）；`citationMap` 序号从 1 起与 `includedReferences` 一致；`omittedCount` |
| `DefaultRagPipelineTest`（追加） | 清洗在重排前（mock Reranker 断言收到已清洗内容）；清洗后为空走 `no-document`；`references-mode=all/included`；`CitationPostProcessor` 只在生成成功时调用、降级时不调用；6/7 参构造器与完整构造器行为等价；`RagMetrics` 各阶段被记录 |
| `DefaultRagPipelineStreamTest`（`reactor-test` `StepVerifier`） | 正常序列 REFERENCES → DELTA* → COMPLETE 且 answer 为拼接文本并经过后处理；无文档序列；`generateAnswer=false`；端口缺席 → 仅 `ERROR(streaming-unsupported)`；首 DELTA 前失败 → 降级 DELTA + COMPLETE(degraded)；DELTA 后失败 → ERROR；总时限超时（虚拟时间）；取消传播到上游；参数非法 `onError(IllegalArgumentException)` |
| `ChatServiceStreamingAnswerGeneratorTest` | `onChunk`/`onComplete`/`onError` 到 Flux 信号的映射；取消后 chunk 被丢弃 |
| `MicrometerRagMetricsTest`（`SimpleMeterRegistry`） | 三个 meter 名与标签；计时与计数可读出 |
| `RagAutoConfiguration` 条件测试（`ApplicationContextRunner`，沿用 R3 `RagReindexConditionTest` 手法） | `rerank.http.url` 缺席 → `NoopReranker`；配 url → `HttpCrossEncoderReranker`；用户 `Reranker` Bean 优先；`wire-format` 非法与 cohere 缺 model → 启动失败；sanitizer enabled 三态；streaming enabled 有/无 `ChatService`；metrics enabled 有/无 `MeterRegistry` Bean、缺 micrometer 类（`FilteredClassLoader`） |
| `ApiBaselineTest` | **既有用例零修改全绿**；追加 `r4Additions_areComplete`：`RagPipeline.queryStream` 为 default、`DefaultRagPipeline` 11 参构造器、`ContextAssembler.assembleDetailed`、`RetrievedContentSanitizer`/`CitationPostProcessor`/`StreamingAnswerGenerator` 单抽象方法、`REASON_STREAMING_UNSUPPORTED`、新公开类型非 final、包名稳定 |
| 回归 | 全仓 clean 全量：以 surefire XML 汇总为准，非跳过用例只增不减（R3 基线 1274/0/0/3）；`mvn -o install -DskipTests` 后 SIA `source-insight-llm` 347/0 |

新增测试依赖：`io.projectreactor:reactor-test`（test scope，Boot BOM 管版本）。

## 11. 实施步骤（每步可独立验证）

1. `nebula-ai-rag/pom.xml`：加 `micrometer-core`（optional）与 `reactor-test`（test）；`mvn -o -q -pl infrastructure/ai/nebula-ai-rag compile`。
2. `metrics` 包：`RagMetrics`、`NoopRagMetrics`、`MicrometerRagMetrics` + 测试。
3. `rerank.http` 包：`RerankWireCodec` 与两个实现 + 测试；`HttpCrossEncoderReranker` + HttpServer 测试。
4. `guard` 包：`RetrievedContentSanitizer`、`NoopRetrievedContentSanitizer`、`PatternSanitizer` + 测试。
5. `ContextAssembly`、`ContextAssembler.assembleDetailed`、`CitationPostProcessor`/Noop + 逐字节等价测试。
6. `RagStreamEvent`、`StreamingAnswerGenerator`、`ChatServiceStreamingAnswerGenerator`、`RagPipeline.queryStream` default + 测试。
7. `DefaultRagPipeline`：完整构造器、钩子、`queryStream` 实现、`REASON_STREAMING_UNSUPPORTED`；`RagProperties` 新分组；同步与流式测试。
8. `RagAutoConfiguration` 增量与条件测试；`ApiBaselineTest.r4Additions_areComplete`。
9. 模块级全量：`mvn -o test -pl infrastructure/ai/nebula-ai-rag,autoconfigure/nebula-autoconfigure`，输出重定向到日志文件、以 surefire XML 汇总。
10. 总控验收：全仓 clean 全量、`git diff` 审读、`mvn -o install -DskipTests`、SIA llm 347 联动；推送前按是/否请示。

## 12. 决策记录

| ID | 决策 | 推荐 | 实际 |
|---|---|---|---|
| R4-D1 | HTTP 重排客户端用 JDK 21 `java.net.http.HttpClient`，不给 `nebula-ai-rag` 引 `spring-web`/`RestClient` | 是：零新依赖；`sendAsync` 天然支持多批并发与统一超时 | 是（2026-09-03 批准）|
| R4-D2 | 复用既有 `rerank.timeout-ms`（当前无消费者）作为 HTTP 重排独立超时，不新增 `rerank.http.timeout-ms` | 是：少一个键，语义与键名一致；SIA yml 已有该键但其重排器不消费 | 是（2026-09-03 批准）|
| R4-D3 | 多批并发发送；任一批失败整体直通融合原序，不做部分打分混排 | 是：混排使未打分候选相对位置无定义；整体直通与 `rerank.enabled=false` 同一路径，可解释 | 是（2026-09-03 批准）|
| R4-D4 | 清洗放在融合后、重排前；清洗结果同时用于上下文与 `references` | 是：LLM 打分重排也会把内容送进模型；两处一致避免前端展示未清洗文本 | 是（2026-09-03 批准）|
| R4-D5 | 不给 `RagAnswer`/`RetrievalResult` 加字段；引用映射由 `references-mode=included` 的顺序承载 | 是：`@AllArgsConstructor` 使加字段等于改公开构造器签名（Y1） | 是（2026-09-03 批准）|
| R4-D6 | 默认流式适配器只走 `ChatService.chatStream(String, callback)` 回调桥接，不依赖 `ReactiveChatService` Bean | 是：工厂方法返回类型为 `ChatService`，装配期条件不可靠；回调桥接对 SIA 的 `ChatServiceWrapper` 同样成立 | 是（2026-09-03 批准）|
| R4-D7 | 流式中途失败：首 DELTA 前 → 降级摘要 + COMPLETE(degraded)；首 DELTA 后 → ERROR 终止不补摘要 | 是：前者与 `query()` 对齐，后者避免客户端拼接错乱 | 是（2026-09-03 批准）|
| R4-D8 | 指标只做内部端口 + Micrometer optional 适配，三个 meter 名固定，`metrics.enabled` 默认关 | 是：J16 不设公开端口；标签有限枚举 | 是（2026-09-03 批准）|
| R4-D9 | J5「三家 wire 实测」本期降为 nebula 侧契约测试（TEI/Cohere 两种格式的模拟服务）；真实 TEI 容器实测为可选手工项，Cohere 不做真实实测 | 是：真实实测依赖外部服务与付费 key，不阻塞交付；编解码契约已由官方文档核实 | 是（2026-09-03 批准）|
