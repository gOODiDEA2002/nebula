package io.nebula.ai.rag.fusion;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RRF（Reciprocal Rank Fusion）融合策略
 * <p>
 * 公式：{@code score(d) = SUM( weight_i / (rrfK + rank_i(d) + 1) )}，rank 从 0 起算。
 * 只看排名不看分数，因此不受各检索器分数尺度差异的影响 —— 向量的余弦相似度与
 * BM25 的打分本来就不在一个量纲上，直接加权求和会让分数大的那一路吃掉全部话语权。
 * <p>
 * <b>元数据保留偏置：</b>同一个 ID 在多路都命中时，默认保留最先遇到的那一份元数据；
 * {@code sourcePriority} 中列出的来源可以覆盖已保留的那份。典型用法是把图谱检索列进去 ——
 * 结构化关系匹配带回来的元数据业务价值更高。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class RrfFusionStrategy implements FusionStrategy {

    /** 融合结果的 source 标注 */
    public static final String SOURCE = "hybrid";

    /** RRF 常数的标准取值 */
    public static final int DEFAULT_RRF_K = 60;

    private final int rrfK;
    private final List<String> sourcePriority;

    public RrfFusionStrategy() {
        this(DEFAULT_RRF_K, List.of());
    }

    /**
     * @param rrfK           RRF 常数，越大则名次差异被压得越平
     * @param sourcePriority 元数据保留优先的来源列表，可为空
     */
    public RrfFusionStrategy(int rrfK, List<String> sourcePriority) {
        if (rrfK <= 0) {
            throw new IllegalArgumentException("rrfK 必须为正数");
        }
        this.rrfK = rrfK;
        this.sourcePriority = sourcePriority != null ? List.copyOf(sourcePriority) : List.of();
    }

    @Override
    public List<RetrievalResult> fuse(List<List<RetrievalResult>> resultLists,
                                      List<Double> weights, int topK) {
        if (resultLists == null || resultLists.isEmpty() || topK <= 0) {
            return List.of();
        }
        if (weights == null || weights.size() != resultLists.size()) {
            throw new IllegalArgumentException("weights 数量必须与 resultLists 一致");
        }

        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, RetrievalResult> resultMap = new HashMap<>();

        for (int i = 0; i < resultLists.size(); i++) {
            List<RetrievalResult> results = resultLists.get(i);
            if (results == null) {
                continue;
            }
            double weight = weights.get(i);

            for (int rank = 0; rank < results.size(); rank++) {
                RetrievalResult result = results.get(rank);
                String key = result.getId();
                if (key == null) {
                    continue;
                }

                double rrfScore = weight / (rrfK + rank + 1);
                scoreMap.merge(key, rrfScore, Double::sum);

                if (!resultMap.containsKey(key) || sourcePriority.contains(result.getSource())) {
                    resultMap.put(key, result);
                }
            }
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    RetrievalResult original = resultMap.get(entry.getKey());
                    return RetrievalResult.builder()
                            .id(original.getId())
                            .content(original.getContent())
                            .metadata(original.getMetadata())
                            .score(entry.getValue())
                            .source(SOURCE)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public int getRrfK() {
        return rrfK;
    }

    public List<String> getSourcePriority() {
        return sourcePriority;
    }
}
