# RAG 优化 R3 详细设计（版本化重灌与蓝绿切换）

> 状态：详细设计稿（v1，2026-09-01，待评审；评审通过前不改代码）
> 上位设计：`docs/design/rag-framework-optimization-design.md` v2（已批准）§5 P2、§8 R3
> 前序：`docs/design/rag-optimization-r2-detailed-design.md`（已交付，提交 `1db8acb5`）
> 范围：`nebula-ai-rag` 新增 `CollectionSwitcher` 中立端口与重灌管线、`nebula-ai-spring` 的 Qdrant alias 适配、`nebula-search-core`/`nebula-search-elasticsearch` 的索引别名补齐、装配层快速失败补强
> 兼容口径：Y1 兼容增量（`ApiBaselineTest` 门禁在岗）/ Y2 新键默认关、默认行为不变

## 1. 现状事实与范围重定（2026-09-01 核实）

### 1.1 R3 范围已被 R2 提前消化一半

上位设计 §8 把「差分计划、断点续跑、`CollectionSwitcher` + Qdrant alias 适配、持久化缺席快速失败」四项都列在 R3。R2 实施时**差分计划与断点续跑已经落地并验收**：

| 上位列入 R3 的项 | 实际状态 | 出处 |
|---|---|---|
| 差分计划 | 已交付：`IndexPlanner.plan` 覆盖增/改/删/hash 未变跳过/单 sink PENDING 重入 | `index/IndexPlanner.java`；`IndexPlannerTest` 6 例 |
| 断点续跑 | 已交付：`IndexingPipeline` 逐 sink 保存状态、失败按文档记账、重跑自然续跑 | `index/IndexingPipeline.java`；`IndexingPipelineTest.interruptedRun_isResumedOnReentry` |
| `CollectionSwitcher` + Qdrant alias | **未开始**，本期本体 | — |
| 持久化缺席快速失败 | **弱化处置**：R2 用 `@ConditionalOnBean` 静默不装配，不是启动失败 | `RagAutoConfiguration.java:379` |

因此 R3 实际内容收窄为「蓝绿切换 + 版本化重灌 + 快速失败补强」，估算相应从 1.5 人周下调至**约 1 人周**。

### 1.2 与设计强相关的现状约束

| 事实 | 出处 | 对设计的含义 |
|---|---|---|
| `QdrantClient` 1.17.0 有完整 alias API：`createAliasAsync`/`deleteAliasAsync`/`renameAliasAsync`/`updateAliasesAsync(List<AliasOperations>)`/`listAliasesAsync` | `javap io.qdrant.client.QdrantClient`（本地 m2 1.17.0） | **J2 成立**。`updateAliasesAsync` 接受操作列表，可在一次提交内「删旧别名 + 建新别名」，Qdrant 侧切换原子 |
| ES 客户端 8.15.5 有 `updateAliases`（一次请求多操作） | `javap ElasticsearchIndicesClient` | ES 侧切换同样可原子 |
| `SearchService` **没有任何别名能力**，只有 `createIndex`/`deleteIndex`/`indexExists`/`getIndexInfo`/`refresh` | `SearchService.java:22,30,38,45,196` | **J9 的现状答案是「当前做不到」**。BM25 侧要参与蓝绿，必须先给 `SearchService` 补别名端口（default 方法保 Y1） |
| Qdrant 的 `VectorStore` Bean 在装配期绑定 `collectionName`，运行期不可改 | `AIAutoConfiguration.java:226-229` | 切换只能走「客户端连别名、别名改指向」，不能走「重建 Bean 指向新集合」 |
| `SpringAIVectorStoreService.collectionName` 硬编码 `"nebula-documents"`，与真实 Qdrant 集合名（`nebula.ai.vector-store.qdrant.collection-name`）无关 | `SpringAIVectorStoreService.java:50,442` | `getCollectionName()` 返回值不可信，切换器**不得**依赖它推导物理名；逻辑名必须由 R3 自己的配置键显式给出 |
| `SpringAIVectorStoreService` 的 `createCollection`/`deleteCollection` 是空实现与「不支持」 | 同文件 447-458 | 集合的建与删不能走 `VectorStoreService` 抽象，只能由供应商适配层直接用 `QdrantClient`（Y5 归位 `nebula-ai-spring`） |
| `nebula-ai-spring` 已含 `spring-ai-starter-vector-store-qdrant`（optional），传递 `io.qdrant:client` | `nebula-ai-spring/pom.xml:62` | Qdrant 适配落 `nebula-ai-spring` 无需新增依赖坐标，与 R2 的改写器适配同模块，守 Y5 |
| `QdrantIdMappingVectorStore` 是包在 Qdrant `VectorStore` 外的装饰器，映射全量对称 | `QdrantIdMappingVectorStore.java:37,80` | 重灌写新集合时若走 `VectorStore`，必须同样带上 id-mapping，否则新旧两代 ID 形态不一致 |

### 1.3 必须如实说明的前提：本期能力当前没有真实消费方

- SIA 的 ES 索引由自建 `KnowledgeEsIndexInitializer` 管理，不经框架索引管线；SIA 的向量侧也未使用 R2 的 `IndexingPipeline`。
- `rag-example` 尚未实施，框架内没有任何调用方。
- 结论：R3 是**为「换 embedding 模型需要整库重灌」这一必然会来的场景预置机制**，而非当下有人等着用。这一点影响优先级判断，作为待拍板项 R3-D6 交由用户决定（见 §9）。

## 2. 目标与非目标

### 2.1 目标

1. 提供后端中立的 `CollectionSwitcher` 端口与 Qdrant/ES 两个实现，使「向量集合」与「BM25 索引」都能以别名承载读流量、以物理名承载写入。
2. 提供 `ReindexPipeline`：按新代际建目标 → 全量灌入 → 两侧切换 → 按保留策略清理，全过程可重入、失败可回滚。
3. 把 R2 的「状态库缺席则静默不装配」补强为「显式启用增量却缺持久化状态库时启动快速失败」。

### 2.2 非目标

- 不做重灌期间的读写并发协调（重灌是停写窗口操作，见 §6 红队第 5 条）。
- 不做跨后端的分布式事务；跨后端切换是「各自原子 + 顺序执行 + 失败回滚」（见 §4.3，J9 的诚实答案）。
- 不做持久化 `IndexStateRepository` 实现（上位 §5 P2 已定：持久化归业务）。
- 不改 `VectorStoreService` 契约（上位 §1.2 非目标）；集合的建删只在供应商适配层做。

## 3. P2-rest：中立端口（包 `io.nebula.ai.rag.index`）

### 3.1 `CollectionSwitcher`

```java
/** 后端中立的「别名 → 物理目标」切换端口（Y6：nebula-ai-rag 只持端口，实现在适配层） */
public interface CollectionSwitcher {

    /** 与 IndexSink.name() 对齐，用于把切换器与写目标配对 */
    String name();

    /** 物理名推导；默认 <逻辑名>-g<代际>，实现可覆盖以适配既有命名 */
    default String physicalName(String logicalName, long generation) {
        return logicalName + "-g" + generation;
    }

    /** 幂等准备物理目标（已存在即返回，不重建、不清空） */
    void prepare(String physicalName);

    boolean exists(String physicalName);

    /** 把逻辑名（别名）指向物理名；同一后端内必须原子完成，别名不存在时等同于首次建立 */
    void switchTo(String logicalName, String physicalName);

    /** 当前逻辑名指向的物理名；不存在返回 null（回滚与清理的安全校验依据） */
    String resolveCurrent(String logicalName);

    /** 永久删除物理目标；实现必须拒绝删除仍被任何别名指向的目标 */
    void drop(String physicalName);
}
```

设计取舍：`resolveCurrent` 是回滚与「不误删活动代际」两件事的共同前提，不设它就只能靠状态库单方面记账，一旦状态库与后端实际不一致就会删错集合，故列为端口必需方法而非可选。

### 3.2 `IndexTargetFactory`：往非活动目标写入

重灌要写的是**尚未接管读流量的新物理目标**，而 R2 的两个 sink 分别绑定「容器里的 `VectorStoreService`」与「配置里的索引名」，都指向活动目标。故新增中立工厂端口：

```java
/** 按物理名产出写目标；重灌管线用它拿到「指向新代际」的 sink */
public interface IndexTargetFactory {
    String name();                          // 与 CollectionSwitcher.name() 对齐
    IndexSink sinkFor(String physicalName);
}
```

- `SearchServiceIndexSink` 侧实现平凡：`SearchServiceIndexTargetFactory` 换索引名 new 一个即可（R2 的 sink 本就按 indexName 写）。
- 向量侧实现落 `nebula-ai-spring`（见 §4.2），因为按集合名构造 `VectorStore` 需要 Qdrant 供应商知识与 id-mapping 装饰（Y5）。

### 3.3 `ReindexPipeline` 与代际状态

```java
public class ReindexPlan {        // 由 ReindexPipeline 内部产出，也可供业务预览
    private long fromGeneration;
    private long toGeneration;
    private Map<String, String> targets;   // switcher name → 新物理名
}

public class ReindexReport {      // 风格对齐 IndexRunReport / EvalReport
    private long generation;
    private IndexRunReport indexRun;       // 灌入阶段的逐文档结果
    private List<String> switched;         // 已成功切换的 switcher 名
    private List<String> rolledBack;
    private String failureStage;           // PREPARE / INDEX / SWITCH / ROLLBACK
    private String manualInterventionHint; // 回滚亦失败时给出人工干预指引
    public String toComparableSummary() { ... }
}
```

代际状态**不复用** `DocIndexState.generation`：后者在 R2 的语义是「该文档写入完成次数」（`IndexingPipeline` 每次全 sink 完成 +1），与「索引代际」是两个概念，混用会让 R2 既有语义漂移（违 Y2）。R3 另立源级别状态：

```java
public class IndexGenerationState {
    private String sourceName;
    private long activeGeneration;       // 别名当前指向的代际
    private long buildingGeneration;     // 正在灌但未切换的代际, 0 = 无
    private int schemaVersion;           // 首版 = 1
}
```

存放位置：复用现有 `IndexStateRepository`，以保留分区键 `<sourceName>#generation` 承载（其 `load/save/remove` 的键都是 `String`，无需改接口即可容纳，守 Y1）。重灌期间逐文档状态写入分区 `<sourceName>@g<代际>`，与活动代际的记账天然隔离，重灌失败不污染活动代际的差分基线。

执行步骤（每步失败即停并进入 §4.3 的回滚判定）：

1. 读 `IndexGenerationState`，`toGeneration = max(active, building) + 1`；若 `building != 0` 说明上次重灌未完成，**沿用该代际续跑**而不是再开新代际（可重入）。
2. 逐 switcher `prepare(physicalName)`；写 `buildingGeneration`。
3. 用 `IndexTargetFactory` 产出的 sink 组装一条临时 `IndexingPipeline`，以分区键 `<source>@g<代际>` 跑全量（复用 R2 全部差分/续跑/幂等逻辑，不复制实现）。
4. 灌入零失败才进入切换；有失败则停在 building 态，报告如实列出，下次重跑续跑。
5. 按 `switch-order` 顺序逐 switcher `switchTo`。
6. 全切成功 → `activeGeneration = toGeneration`、`buildingGeneration = 0`；按 `keep-generations` 清理更旧代际（`drop` 前用 `resolveCurrent` 校验目标未被任何别名指向）。

## 4. 适配层实现

### 4.1 `SearchService` 别名补齐（`nebula-search-core` + `nebula-search-elasticsearch`）

`SearchService` 新增两个 **default 方法**（Y1：接口新增 default 不破坏既有实现与二进制兼容）：

```java
/** 把别名原子指向目标索引；默认不支持，由具体实现覆盖 */
default IndexResult switchAlias(String aliasName, String targetIndexName) {
    throw new UnsupportedOperationException(
            getClass().getSimpleName() + " 不支持索引别名操作");
}

/** 别名当前指向的索引名；无别名或不支持时返回空列表 */
default List<String> resolveAlias(String aliasName) {
    return List.of();
}
```

ES 实现：
- `switchAlias`：先 `getAlias` 取现有指向，再用 `updateAliases` 在**一次请求**内提交「remove 旧索引上的该别名（逐个）+ add 新索引的该别名」；别名此前不存在时操作列表只含 add。
- `resolveAlias`：`getAlias(aliasName)`，别名不存在时 ES 抛 404 → 捕获返回空列表（不把「没有别名」当错误）。
- 与 R2 一致，两个方法的单元测试走「构建出的请求对象可检视」路线，不依赖真实 ES。

`ElasticsearchCollectionSwitcher`（落 `nebula-ai-rag`？否——见下）：ES 侧切换只用到 `SearchService` 这一中立契约，不涉及供应商 SDK，故 `SearchServiceCollectionSwitcher` 可以落 `nebula-ai-rag`（`nebula-search-core` 已是其 optional 依赖，R2 引入），`prepare` 走 `createIndex(默认 mapping)`、`drop` 走 `deleteIndex`、`switchTo`/`resolveCurrent` 走上面两个新方法。这不违 Y5：`nebula-search-core` 是中立契约而非供应商 SDK。

### 4.2 Qdrant 适配（`nebula-ai-spring`）

`QdrantCollectionSwitcher implements CollectionSwitcher`：

- 依赖 `QdrantClient`（已有 `nebulaQdrantClient` Bean）与向量维度/距离配置。
- `prepare`：`collectionExistsAsync` 为假才 `createCollectionAsync(name, VectorParams{size, distance})`；维度取新增配置键（见 §5），**不从旧集合推断**——重灌的典型动因就是换模型换维度，从旧集合抄维度正好抄错。
- `switchTo`：`updateAliasesAsync` 一次提交 `[DeleteAlias(logical)?, CreateAlias(collection=physical, alias=logical)]`；先 `listAliasesAsync` 判断别名是否存在以决定是否带 DeleteAlias（对不存在的别名发 Delete 会报错）。
- `resolveCurrent`：`listAliasesAsync` 过滤 `aliasName == logical` 取其 `collectionName`。
- `drop`：先 `listCollectionAliasesAsync(physical)`，非空则拒绝删除并抛出含别名清单的异常；空才 `deleteCollectionAsync`。
- 所有调用 `.get(timeout)` 同步化并把 `ExecutionException` 拆包为带集合名的明确异常。

`QdrantIndexTargetFactory implements IndexTargetFactory`：按物理集合名 `QdrantVectorStore.builder(...).collectionName(physical).build()`，再按 `id-mapping.enabled` 决定是否包 `QdrantIdMappingVectorStore`（**与 `AIAutoConfiguration` 的装配逻辑保持同一套规则**，避免新旧代际 ID 形态不一致），最后包 `SpringAIVectorStoreService` 得到 `VectorStoreService`，交给 R2 的 `VectorStoreIndexSink`。

> 注：此处复用装配逻辑存在「两处规则需同步」的隐患。落地时把 id-mapping 装饰规则抽成 `nebula-ai-spring` 内的一个静态工厂方法，`AIAutoConfiguration` 与本工厂共同调用，消除重复分支。

### 4.3 跨后端切换协议（J9 的诚实结论）

**J9 不成立的部分必须写明：向量集合与 BM25 索引分属两个独立后端，无法做到真正的同 generation 原子切换。** 可做到的是：

1. 每个后端内部原子（Qdrant `updateAliases`、ES `updateAliases` 各自一次提交）。
2. 跨后端按 `switch-order` 顺序执行，默认 `search-first`：BM25 路权重低（R2 默认 0.4）且 RRF 只用名次，先切它对融合结果的扰动比先切向量小。
3. 任一步失败 → 立即把已切换的后端 `switchTo` 回旧物理名 → 报告 `failureStage=SWITCH`、`rolledBack` 列出回滚项、`activeGeneration` 不推进。
4. 回滚亦失败 → `manualInterventionHint` 给出「后端 X 别名 A 现指向 g<新>，应指回 g<旧>」的具体指令，并以 `error` 级日志输出；框架不再自动重试（重试只会放大不一致）。
5. **残余风险如实声明**：两次切换之间存在毫秒级到秒级的「跨代际混读」窗口，此窗口内一路命中新代际、另一路命中旧代际。RRF 融合对此是容忍的（分数按名次累加，不会因跨代际而抛错），但引用内容可能来自两代文本。无法消除，只能缩短。

## 5. 配置键增量（全部新增，默认关）

```yaml
nebula:
  ai:
    rag:
      indexing:
        fail-fast-without-state-repository: true   # 见 §7
        reindex:
          enabled: false                # 默认关: 不装配任何切换器与重灌管线
          vector-alias: ""              # 空 = 向量侧不参与切换
          vector-dimension: 0           # >0 才允许 prepare 建集合
          vector-distance: cosine       # cosine | dot | euclid
          search-alias: ""              # 空 = BM25 侧不参与切换
          switch-order: search-first    # search-first | vector-first
          keep-generations: 2           # 保留的历史代际数; 0 = 切换后立即清理
```

`RagProperties` 增量嵌套 `Indexing.Reindex`，`ApiBaselineTest` 按 R2 的先例补表（新增成员不属红线，既有嵌套结构不得变形）。

## 6. 红队自检

| 失败路径 | 应对 |
|---|---|
| 别名与集合同名（Qdrant 禁止，ES 也禁止别名与索引同名） | `prepare`/`switchTo` 前校验 `logicalName != physicalName`；配置里 `vector-alias` 若等于现有集合名，启动即报错并提示需先改名迁移 |
| 首次启用：别名尚不存在，切换发出 DeleteAlias 报错 | 先 `listAliases`/`getAlias` 探测，据此决定操作列表是否含 Delete；两侧实现都覆盖「别名不存在」用例 |
| 既有部署直接把真实索引名配给 `search.index-name`（R2 语义），启用别名后名字冲突 | 不自动迁移。文档明确：启用 `reindex` 时 `search-alias` 必须是新名字，首次切换后再把 `search.index-name` 改为别名；两键同值时启动报错 |
| `keep-generations` 清理误删活动集合 | `drop` 前必查 `resolveCurrent`/`listCollectionAliasesAsync`，被任何别名指向即拒绝删除并抛异常 |
| 重灌期间业务继续增量写入活动代际 | 框架不做并发协调（非目标）。新代际由全量灌入自足；重灌窗口内活动代际的增量**不会**自动补进新代际，切换后需再跑一次增量对齐。此约束写入 javadoc 与配置注释，`ReindexReport` 摘要中固定提示 |
| 换模型换维度后误用旧维度建集合 | `vector-dimension` 必须显式配 `>0`，不从旧集合推断；为 0 时 `prepare` 直接抛配置错误 |
| 灌入阶段部分文档失败却仍切换 | 切换前置条件是 `IndexRunReport.failed == 0`；有失败即停在 building 态，不切换 |
| 重灌反复失败留下大量孤儿集合 | 可重入设计沿用同一 `buildingGeneration`，不会每次新开代际；孤儿仅在人为改配置时产生，`drop` 提供手动清理路径 |
| id-mapping 规则在装配与工厂两处漂移 | §4.2 注记：抽公共静态工厂，两处共用；补一个「两条路径产出的 VectorStore 装饰形态一致」的测试 |
| `SearchService` 新 default 方法被既有实现意外继承为「不支持」 | 这正是期望行为（ES 之外的实现本就不支持）；`ApiBaselineTest` 与 search 模块测试断言 ES 实现已覆盖、默认实现抛 `UnsupportedOperationException` |

## 7. 状态库缺席快速失败（补强 R2）

R2 现状：`indexingPipeline` 标 `@ConditionalOnBean({DocumentSource.class, IndexStateRepository.class})`，缺状态库时**静默不装配**——业务侧注入点在编译期或启动期报「找不到 Bean」，信息不指向真实原因。上位 §5 P2 与审查发现 12 要求的是「启动快速失败」。

处置：新增守卫 Bean，条件为 `indexing.enabled=true` + `fail-fast-without-state-repository=true` + 有 `DocumentSource` Bean + 无 `IndexStateRepository` Bean，其构造函数直接抛出：

> 已启用 `nebula.ai.rag.indexing` 并提供了 DocumentSource，但容器内没有 IndexStateRepository。持续增量与删除对齐依赖持久化状态；请提供实现，或仅用于一次性任务时显式声明 InMemoryIndexStateRepository Bean（重启即失忆），或置 `fail-fast-without-state-repository=false` 关闭本检查。

显式声明 `InMemoryIndexStateRepository` 为 Bean 的用户不触发（这是用户的明示选择），但装配时 `warn` 一次说明其局限。

## 8. 测试计划

| 层 | 内容 |
|---|---|
| 兼容门禁 | `ApiBaselineTest` 既有断言全绿 + R3 增量补表；R2 的 `index`/`transform`/`retriever` 公开面零变化 |
| 端口契约 | `CollectionSwitcher` 桩实现的契约测试：prepare 幂等、drop 拒删被别名指向者、resolveCurrent 空值语义 |
| Qdrant 适配 | mock `QdrantClient`：断言 `updateAliasesAsync` 收到的操作列表（别名存在 → Delete+Create；不存在 → 仅 Create）；`drop` 在 `listCollectionAliasesAsync` 非空时拒绝；维度为 0 时报配置错误 |
| ES 适配 | `switchAlias` 请求对象断言（remove 旧 + add 新一次提交、别名不存在时只 add）；`resolveAlias` 对 404 返回空列表；默认 default 方法抛 `UnsupportedOperationException` |
| 重灌管线 | 成功路径（prepare→灌→切→清理）；灌入有失败则不切换；切换中途失败触发回滚且 active 不推进；回滚失败给出人工干预提示；`building != 0` 时续跑同代际不新开 |
| 快速失败 | 四态矩阵：默认关 / 开且有状态库 / 开且缺状态库（应启动失败）/ 开且缺状态库但检查关闭 |
| 条件矩阵 | `reindex.enabled` 开关 × 两个别名键空/非空 × 缺 Qdrant 类 × 缺 `SearchService` 类 |
| 回归 | nebula 全仓（基线 1229/0/3，非跳过只增）+ `mvn -o install` + SIA llm 347 联动 |

真实后端的 alias 切换实测（J2/J9 的端到端证据）在本机无 Qdrant/ES 实例，与 R2 的 fullstack E2E 同样列为**未执行项**，待有环境补跑；单元层以请求对象断言覆盖构造正确性。

## 9. 实施步骤（小步 + 当场验证）

1. `RagProperties.Indexing.Reindex` 增量 + `ApiBaselineTest` 补表 → 模块绿。
2. §7 状态库快速失败守卫 + 四态矩阵测试 → autoconfigure 绿。
3. `nebula-search-core` 两个 default 方法 + ES 实现 + 请求对象断言测试 → search 模块绿。
4. `CollectionSwitcher`/`IndexTargetFactory` 端口 + `SearchServiceCollectionSwitcher`/`SearchServiceIndexTargetFactory` + 契约测试 → ai-rag 绿。
5. `IndexGenerationState` + `ReindexPlan`/`ReindexReport` + `ReindexPipeline` + 全路径测试（含回滚与续跑）→ ai-rag 绿。
6. `nebula-ai-spring`：id-mapping 公共工厂抽取（含两路径一致性测试）+ `QdrantCollectionSwitcher` + `QdrantIndexTargetFactory` + mock 测试 → ai-spring 绿。
7. 装配：`ReindexConfiguration` 嵌套（`reindex.enabled=true` + 别名键非空 + 对应 Bean/类存在）+ 条件矩阵 → autoconfigure 绿。
8. 收尾：全仓 test（≥1229 非跳过只增）→ install → SIA 347 联动 → 请批提交。

估算：约 1 人周（§1.1 重定后口径）。派 opus 子代理一轮实现，总控独立复核。

## 10. 决策记录（2026-09-01 用户拍板）

| ID | 决策 | 推荐 | 实际 |
|---|---|---|---|
| R3-D1 | 代际状态另立 `IndexGenerationState`，不复用 `DocIndexState.generation`（避免 R2 语义漂移） | 是 | **是** |
| R3-D2 | 重灌期间不做读写并发协调，作为停写窗口操作并在文档与报告中固定提示 | 是 | **是** |
| R3-D3 | 跨后端切换按 `search-first` 顺序执行 + 失败回滚，如实声明毫秒级混读窗口不可消除 | 是 | **是** |
| R3-D4 | 给 `SearchService` 增两个 default 别名方法（ES 实现，其余实现继承为「不支持」） | 是 | **是** |
| R3-D5 | 状态库缺席由「静默不装配」补强为「启动快速失败」，可经新键关闭 | 是 | **是** |
| R3-D6 | **顺序**：鉴于 §1.3（R3 当前无消费方）与 rag-example 启动门已达成，建议先做 rag-example，R3 紧随其后 | 是 | **否——直接实施 R3，rag-example 顺延至 R3 之后** |
| R3-D7 | 批准后按 §9 派 opus 子代理实施 | 是 | **是** |

§1.3 记录的「本期能力当前无真实消费方」仍然成立，用户已知悉并选择先建机制；rag-example 顺延不取消。
