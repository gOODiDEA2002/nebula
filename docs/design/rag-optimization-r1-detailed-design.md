# RAG 优化 R1 详细设计（P1 评测 + P3 切分 + 兼容门禁）

> 状态：详细设计稿（v1，2026-08-30，待评审；评审通过前不改代码）
> 上位设计：`docs/design/rag-framework-optimization-design.md` v2（已批准，DS1-DS7 取推荐值）
> 范围：`nebula-ai-rag` 新增 `eval` / `chunking.parse` / `chunking.pack` 三个包 + 测试与评测语料；**不新增自动配置 Bean、不改配置键、既有主代码文件零修改**
> 兼容口径：上位 Y1（只允许兼容增量）/ Y2（默认行为不变，稳定字段特征测试口径）

## 1. 兼容红线：SIA API 消费面基线（J6 已核实）

以下成员的存在性与签名是本期兼容门禁的守护对象（2026-08-30 经 SIA 源码检索得出）：

| 类别 | 成员 | SIA 使用方式 |
|---|---|---|
| 接口 | `Retriever`（retrieve/getName/getWeight/timeoutMillis）、`Reranker`、`AnswerGenerator`、`RagPromptRenderer` | 业务类实现 |
| 具体类继承 | `VectorStoreRetriever` 的 4 参构造器 `(VectorStoreService, String, double, double)` 与可覆盖的 `retrieve/getName/getWeight` | `VectorRetriever extends` 并调 `super(...)` |
| Bean 类型 | `RagPipeline`、`DefaultRagPipeline`、`HybridRetrievalEngine`、`RrfFusionStrategy`（含 `getRrfK/getSourcePriority`）、`TextChunker` | `getBean` / 构造注入 / `instanceof` / cast |
| 常量 | `DefaultRagPipeline.REASON_NO_DOCUMENT` / `REASON_GENERATION_TIMEOUT` | 判等 |
| 模型 | `RetrievalResult`（builder + 全 getter）、`RagQuery`、`RagAnswer`（builder，含 degradeReason） | 构造与读取 |
| 构造器 | `TextChunker(int, int)`、`DefaultRagPipeline` 6 参构造器 | Bean 定义与测试 |
| 配置类 | `io.nebula.ai.rag.config.RagProperties` 及其嵌套结构 | 装配测试引用 |

**门禁实现：`ApiBaselineTest`（反射断言）**。不引入 japicmp 插件（对连续重发布的 SNAPSHOT 无稳定基线可比）；改为测试内硬编码上表成员清单，用反射逐项断言「类存在、成员存在、参数与返回类型精确匹配」。任何人改动红线成员会在本模块测试期立刻爆红，错误信息指向本节。清单更新须在测试注释里注明对应的 SIA 侧协调变更。

## 2. P1 评测库详细设计（包 `io.nebula.ai.rag.eval`，纯库、无 Spring 依赖）

### 2.1 类型与签名

```java
/** 金标条目 */
public class GoldenSetEntry {
    private String query;
    private List<String> expectedIdPrefixes;   // 命中判据：结果 ID 以任一前缀开头
    private String subset;                     // 所属子集（用于分子集聚合）
}

public class GoldenSet {
    private List<GoldenSetEntry> entries;
    public static GoldenSet fromJson(InputStream in);   // Jackson 3, 失败抛明确异常
}

/** 被评测的检索函数：引擎、整条管线或任何检索器均可适配 */
@FunctionalInterface
public interface RetrievalFunction {
    List<RetrievalResult> retrieve(String query, int topK);
}

public class RetrievalEvaluator {
    public RetrievalEvaluator(int k);                    // 评测深度，默认 5
    public EvalReport evaluate(GoldenSet goldenSet, RetrievalFunction fn,
                               Map<String, String> configSnapshot);
}

public class EvalReport {
    private double recallAtK;      // 命中条目数 / 总条目数
    private double mrr;            // Σ(1/首个命中名次) / 总条目数，未命中计 0
    private double ndcgAtK;        // 二值相关 nDCG：DCG = Σ hit_i/log2(i+1)，IDCG 按理想排列
    private int k;
    private int total;
    private Map<String, SubsetMetrics> perSubset;   // 子集 → 同三指标
    private List<QueryOutcome> perQuery;            // {query, subset, hitRank(-1 未中), topIds}
    private Map<String, String> configSnapshot;     // 调用方传入的配置快照，报告对比的锚
    public String toComparableSummary();            // 单行文本：三指标 + 各子集 recall，供日志与断言
}
```

设计要点：判中口径为**前缀匹配**（依赖确定性块 ID，见 §3.4）；指标全部纯函数、无外部依赖；`configSnapshot` 由调用方组装（评测库不反读 Spring 配置，保持纯库）。

### 2.2 评测语料与金标（`src/test/resources/eval/`）

| 子集 | 语料 | 金标条数 | 度量目标 |
|---|---|---|---|
| plain | 4 篇纯文本 MD | 6 | 基线参照（改进不应回退） |
| table | 3 篇含大表格 MD | 6 | 表格原子化 + 表头重复的收益 |
| code | 3 篇含长代码块 MD | 5 | 代码块原子化的收益 |
| breadcrumb | 4 篇深层级标题 MD（关键信息在 H3/H4 短节） | 7 | 面包屑注入的收益 |
| json | 3 个 JSON/JSONL（嵌套配置 + 记录行） | 6 | 键路径切分的收益 |

合计约 17 份语料、30 条金标。语料内容取材 nebula 自身文档改写（中文为主，混少量英文），关键答案刻意放在「定长切分必然切坏、结构切分必然保全」的位置（如表格末行、深层小节、代码块中段）——这是 J3 区分度的构造性保证。

### 2.3 单元级评测检索器（test 目录，非交付物）

`DeterministicLexicalRetriever`：对给定块列表按「查询与块内容的中文 2-gram 重合率」打分取 topK，零随机性。**用固定检索器度量切分质量的相对差异**——单元测试无法用真实 embedding，而切分改进对词面检索器与向量检索器的作用方向一致（块边界决定信息是否共存于同一块）。真实 embedding 端到端评测由后续示例的 Full E2E 承担，本期不做。

### 2.4 对比 harness 与阈值

`ChunkingEvalComparisonTest`：同一语料 × 两种切分（A=现 `TextChunker` 定长 500/100；B=新结构化管线同预算）× 同一检索器 → 两份 `EvalReport`，断言：

- table / code / breadcrumb / json 四个目标子集：B 的 recall@5 − A ≥ **0.10**；
- plain 子集：B 的 recall@5 − A ≥ **−0.05**（允许小幅波动不允许实质回退）。

阈值为初值；实施第一步先跑 A 基线确认区分度（J3），若需调整只允许**调整一次**并把两份报告与理由写进提交说明。

## 3. P3 切分详细设计（包 `chunking.parse` / `chunking.pack`）

### 3.1 总决策：旧类冻结、新包平行

`TextChunker` / `DocumentChunker` / `MarkdownDocumentParser` / `ParsedDocument` **一行不改**（比「门面委托」更强的 Y2 保证：连委托风险都没有）。新能力全部在平行包中，R2 的索引管线与后续示例显式选用新管线；旧类在未来主版本再议弃用。`DocumentChunk` / `ChunkType` 复用为公共输出模型（`DocumentChunk` 有全套 setter，确定性 ID 由装箱器 `setId` 写入，**该类零修改**）。

### 3.2 解析层（`chunking.parse`）

```java
public enum DocElementType { HEADING, PARAGRAPH, CODE, TABLE, LIST_ITEM, CONFIG, RECORD }

public class DocElement {
    private DocElementType type;
    private String text;                 // 元素原文（表格含表头行）
    private List<String> breadcrumb;     // 标题路径 / 元素路径 / JSON 键路径
    private Map<String, Object> attrs;   // 如 table 的 headerRowCount、code 的 language
}

public interface StructureParser {
    String format();                                       // "markdown" / "html" / "xml" / "json" / "jsonl"
    List<DocElement> parse(String content, ParseOptions options) throws ParseLimitExceededException;
}

public class ParseOptions {   // J15 安全上限，全部有默认值
    private int maxInputChars = 2_000_000;
    private int maxDepth = 64;
    private int maxElements = 100_000;
}
```

五个实现的规格：

| 实现 | 要点 |
|---|---|
| `MarkdownStructureParser` | 正则行扫描：标题（维护层级栈 → 面包屑）、围栏代码块（含语言标记，栏内内容不参与任何其他识别）、表格（`\|` 行 + 分隔行判定，`headerRowCount` 记入 attrs）、列表项（`-`/`*`/数字，连续项各自成元素）、其余为段落。不引入 flexmark——正则覆盖本设计所需的行级结构已足够，AST 解析器留到出现嵌套语法需求时再议 |
| `HtmlStructureParser` | jsoup 解析（**依赖 optional**，本类是模块内唯一 import jsoup 的类，消费方需类存在检查）；剔除 `script/style/nav/footer`；`h1-h6` 维护面包屑；`p/li/pre/table/section/div(直接文本)` 映射元素；表格转 Markdown 管道表示（与 MD 路统一下游处理） |
| `XmlStructureParser` | `XMLInputFactory` 显式关闭 DTD 与外部实体（`SUPPORT_DTD=false`、`IS_SUPPORTING_EXTERNAL_ENTITIES=false`，J15）；叶子元素文本聚为 RECORD/PARAGRAPH，面包屑=元素路径+识别性属性（id/name）；重复兄弟元素各自成元素 |
| `JsonStructureParser` | Jackson 3 树遍历：递归下潜到「子树序列化长度 ≤ maxChunkSize」即封为 RECORD 元素（文本=合法 JSON 子树，面包屑=键路径，数组转下标键）；超深/超大守 ParseOptions |
| `JsonlStructureParser` | 按行切 RECORD；空行跳过；单行超限交装箱层按 JSON 递归降级 |

### 3.3 装箱层（`chunking.pack`）

```java
public class ChunkPacker {
    public ChunkPacker(PackOptions options);
    public List<DocumentChunk> pack(String docId, List<DocElement> elements);
}

public class PackOptions {
    private int maxChunkSize = 500;
    private int overlap = 100;
    private Set<DocElementType> preserveTypes = Set.of(TABLE, CODE);   // 原子单元
    private SeparatorHierarchy separators = SeparatorHierarchy.chineseDefault();
                                            // ["\n\n", "\n", 句末标点集, " ", ""]
    private LengthMeasure lengthMeasure = LengthMeasure.chars();        // token 计数端口留待后续
    private ChunkIdStrategy idStrategy = ChunkIdStrategy.random();      // 现状语义为默认
    private boolean breadcrumbToContent = false;    // true 时块首注入「A > B > C」行
    private static final String META_BREADCRUMB = "breadcrumb";          // metadata 恒写入
}
```

装箱算法（详细设计冻结，实施不得偏离）：

1. 顺序扫描元素流；HEADING 只更新面包屑不单独成块。
2. 同面包屑下的相邻非原子元素贪心并箱；放不下则封箱开新箱。
3. 原子元素（preserveTypes）独立成块，**超限不切**；唯一例外：TABLE 超限按数据行切分，每块重复表头行（headerRowCount 取自 attrs），attrs 记 `tablePart=i/n`。
4. 单个非原子元素超限：按 `SeparatorHierarchy` 递归降级切分（段落 → 换行 → 句 → 空格 → 字符），overlap 取整句/整行边界回退，禁止裸字符 overlap。
5. 每块产出：`chunkType` 按主导元素类型、`title` 取面包屑末级、`metadata["breadcrumb"]` 恒写、ID 经 `idStrategy`。

```java
public interface ChunkIdStrategy {
    String chunkId(String docId, int index, DocumentChunk chunk);
    static ChunkIdStrategy random();          // "chunk-" + UUID（现状语义）
    static ChunkIdStrategy deterministic();   // docId + "#" + index（评测与 R2 索引管线用）
}
```

### 3.4 与 P1 的接线

评测语料经「解析 → deterministic ID 装箱」产出块集，金标 `expectedIdPrefixes` 即写 `<语料文件名>#`——上位设计「确定性块 ID 是评测前置」在此闭合。

## 4. 文件清单

新增主代码 18 个（eval 6 + parse 8 + pack 4），新增测试约 14 个（`ApiBaselineTest`、评测库 3、各解析器 5、装箱器 3、对比 harness 1、语料与金标资源），既有主代码文件改动 **0 个**；`nebula-ai-rag/pom.xml` 增 jsoup（optional）与 test 域 Jackson（已有）。

## 5. 测试计划

| 层 | 内容 |
|---|---|
| 兼容门禁 | `ApiBaselineTest` 反射断言 §1 全表；构建期常跑 |
| 解析器单元 | 每实现覆盖:空输入/仅标题/嵌套/畸形输入(未闭合围栏、断表格、坏 JSON)/上限触发(深度、元素数、输入长)/安全用例(XML 实体注入样本必须拒绝) |
| 装箱单元 | 原子保护(超限表格行切+表头重复、代码块不切)/递归降级边界/overlap 整句回退/面包屑 metadata/两种 ID 策略 |
| 评测库单元 | 三指标各自的手算金标用例(含未命中、并列、k 截断) |
| 对比 harness | §2.4 阈值断言 |
| 回归 | 模块全量 + nebula 全仓 1040 基线 + SIA 347 联动(本机 install 后) |

R1 无新增自动配置 Bean，四类条件测试不适用（在验收记录中说明而非补形式测试）。

## 6. 红队自检

| 失败路径 | 应对 |
|---|---|
| jsoup 缺席时加载 `HtmlStructureParser` 抛 NoClassDefFoundError | 该类为模块内唯一 jsoup import；javadoc 与 R2 装配层用类存在检查；本期无自动装配，测试里显式声明依赖存在 |
| MD 表格正则误报（代码块内的管道行） | 围栏内内容先行摘除再识别表格；用例覆盖 |
| JSON 深度炸弹 / 巨数组 | ParseOptions 三上限 + 超限抛 `ParseLimitExceededException`（不静默截断） |
| 阈值定不出区分度（J3 失败） | 实施第一步先跑基线；语料构造已按「定长必坏、结构必全」设计，仍不达则调语料而非放水阈值 |
| 评测库被误当通用协议 | `GoldenSet` javadoc 明示口径（前缀判中、二值相关）与适用范围 |
| 反射门禁漏项 | 门禁表与 §1 同源；SIA 侧新增消费面时须同步补表（写入测试类注释的维护规则） |

## 7. 实施步骤（小步 + 当场验证）

1. `ApiBaselineTest` 先行落地（红线先立，后续每步受其保护）→ 模块测试绿。
2. 评测库 + 指标手算用例 → 绿。
3. 评测语料 + 金标 + `DeterministicLexicalRetriever` + 基线报告（A 侧）→ 区分度确认（J3），必要时修语料。
4. `chunking.parse` 五解析器（MD → JSON/JSONL → HTML → XML 顺序，先易后难）逐个带用例 → 各自绿。
5. `chunkPacker` + ID 策略 → 绿。
6. 对比 harness（B 侧）→ 阈值断言过；产出前后 `EvalReport` 对比留档。
7. nebula 全仓 `mvn test` + `install` + SIA 347 联动 → 全绿后请批提交（`feat(ai-rag)` 单 commit 或按 eval/chunking 两 commit，提交时定）。

估算：约 3.5 人周（上位 §8 口径）；派 opus 子代理一轮实现，总控按第 5 节测试计划独立复核。

## 8. 待拍板决策（可只答「是 / 否」）

| ID | 决策 | 推荐 |
|---|---|---|
| R1-D1 | 旧切分类完全冻结（非门面委托），新包平行演进 | 是 |
| R1-D2 | 兼容门禁用反射基线测试，不引入 japicmp 插件 | 是 |
| R1-D3 | Markdown 解析继续用正则行扫描，不引入 flexmark | 是 |
| R1-D4 | 对比阈值初值 +0.10 / −0.05，允许凭两份报告修一次 | 是 |
| R1-D5 | 批准后派 opus 子代理按 §7 实施 | 是 |
