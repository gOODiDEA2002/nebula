# 设计文档：修复向量检索得分丢失回归（score 恒 0）

版本 D1（2026-07-19，待评审）。仓库：nebula（main，revision=2.1.0-SNAPSHOT）。

## 1. 缺陷陈述与生产影响

SB 4.1 全栈升级（78e595a4，07-06）把 Chroma 向量库 bean 从自研 `CustomChromaVectorStore` 换成 **Spring AI 2.0 官方 `ChromaVectorStore.builder`**（`AIAutoConfiguration:143` 注释明示）。但 `nebula-ai-spring` 的 `SpringAIVectorStoreService.extractScore` 仍按旧 Custom 契约**只读 `metadata["score"]`**——官方实现不写该键，于是**所有检索结果 score 恒 0.0**。

07-08/07-16 发布的 2.1.0-SNAPSHOT 均含此回归。下游 SIA 平台生产实证受灾面：语义召回恒空（0.3 阈值全灭）、物资编码映射检索恒空、智能体编码映射工具静默返回空。时间线与 SIA 侧观测完全吻合。

## 2. 根因（有码为证）

- 官方 SA 2.0 `ChromaVectorStore.doSimilaritySearch`（反编译确证）：分数经 **`Document.Builder.score(Double)`** 写入 Document 本体（语义 1-distance），同时 metadata 写 `DocumentMetadata.DISTANCE`；**不写 `metadata["score"]`**。
- `SpringAIVectorStoreService.extractScore`（`infrastructure/ai/nebula-ai-spring/.../SpringAIVectorStoreService.java:522-533`）：只读 `metadata["score"]`，读不到回退 0.0。
- 结论：换官方实现时，读分逻辑未同步——典型契约迁移遗漏。

## 3. 修复设计（最小面）

只改 `extractScore` 一个方法，读取顺序改为四级回退：

```java
private double extractScore(org.springframework.ai.document.Document doc) {
    // 1. 官方 SA 2.0 契约: 分数在 Document 本体
    if (doc.getScore() != null) {
        return doc.getScore();
    }
    Map<String, Object> md = doc.getMetadata();
    if (md != null) {
        // 2. 兼容旧 CustomChromaVectorStore 契约(metadata["score"])
        Object s = md.get("score");
        if (s instanceof Number n) {
            return n.doubleValue();
        }
        // 3. 兜底: 官方 metadata 距离字段(DocumentMetadata.DISTANCE, 语义 score = 1 - distance)
        Object d = md.get(org.springframework.ai.document.DocumentMetadata.DISTANCE.value());
        if (d instanceof Number n) {
            return 1.0 - n.doubleValue();
        }
    }
    // 4. 无法获取
    return 0.0;
}
```

不动：bean 装配（维持官方实现，方向正确）、`CustomChromaVectorStore`（保留不删，本次不扩范围）、阈值/过滤等其余逻辑。

## 4. 测试

- 单测 `SpringAIVectorStoreServiceTest`（或新建）三用例：Document.score 有值取之；仅 metadata["score"] 时取之（旧契约兼容）；仅 DISTANCE 时取 1-distance；全无回 0。
- 模块全量：`mvn test -pl infrastructure/ai/nebula-ai-spring -am` 绿。

## 5. 发布与下游消费

- main 合入 → CI publish 阶段自动 `mvn deploy` 发布新 2.1.0-SNAPSHOT。
- 下游 SIA 的 llm 服务 pin `2.1.0-SNAPSHOT`，重建即消费（其消费侧已有"score 全 0 跳过阈值"的咽喉点免疫，框架修复后真实分数回流、阈值语义自动还原；免疫层继续留作对未知丢分形态的兜底，两层不冲突）。
- 验证闭环（SIA 侧，修复发布后）：`similarityThreshold=0.3` 的编码检索返回非空且 score>0。

## 6. 风险

| 风险 | 评估 |
|------|------|
| doc.getScore() 在其它 VectorStore 实现下语义不一 | SA 官方约定 score 越大越相似(0-1)，各官方 store 一致；回退链保证读不到时不误判 |
| 旧 Custom 若被重新启用 | 回退级 2 已兼容其契约 |
| 影响其它下游 | 该方法只影响"得分读取"，此前恒 0 属最坏值，任何真实分数都是严格改善 |
