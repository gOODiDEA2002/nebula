package io.nebula.examples.rag.retriever;

import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内存关键词检索器：查询与块内容做中文 2-gram 重合计分。
 * <p>
 * 打分口径：{@code score = 命中的查询 2-gram 数 / 查询 2-gram 总数}，取值 [0,1]，
 * 排序后取 topK。无第三方依赖，约 60 行，专供演示「关键词路补齐向量路对专名 / 代号的漏召」。
 * 实现 {@link Retriever} 即被 {@code hybridRetrievalEngine} 收集；{@code source = keyword}。
 * 生产实现应换成 Elasticsearch BM25。
 *
 * @author Nebula Framework
 */
public class InMemoryKeywordRetriever implements Retriever {

    /** 检索器名称与结果 source 标注 */
    public static final String NAME = "keyword";

    private final InMemoryKeywordIndex index;
    private final double weight;

    public InMemoryKeywordRetriever(InMemoryKeywordIndex index, double weight) {
        this.index = index;
        this.weight = weight;
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
        Set<String> queryGrams = bigrams(query);
        if (queryGrams.isEmpty()) {
            return List.of();
        }
        List<RetrievalResult> scored = new ArrayList<>();
        for (InMemoryKeywordIndex.StoredChunk chunk : index.allChunks()) {
            Set<String> contentGrams = bigrams(chunk.content());
            int hit = 0;
            for (String g : queryGrams) {
                if (contentGrams.contains(g)) {
                    hit++;
                }
            }
            if (hit == 0) {
                continue;
            }
            double score = (double) hit / queryGrams.size();
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("docId", chunk.docId());
            metadata.put("title", chunk.title());
            scored.add(RetrievalResult.builder()
                    .id(chunk.chunkId())
                    .content(chunk.content())
                    .metadata(metadata)
                    .score(score)
                    .source(NAME)
                    .build());
        }
        scored.sort(Comparator.comparingDouble(RetrievalResult::getScore).reversed());
        if (scored.size() > topK) {
            return new ArrayList<>(scored.subList(0, topK));
        }
        return scored;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    /** 生成字符 2-gram 集合：去空白、转小写后取相邻双字，长度为 1 时退化为单字 */
    private static Set<String> bigrams(String text) {
        Set<String> grams = new HashSet<>();
        if (text == null) {
            return grams;
        }
        String normalized = text.replaceAll("\\s+", "").toLowerCase();
        if (normalized.isEmpty()) {
            return grams;
        }
        if (normalized.length() == 1) {
            grams.add(normalized);
            return grams;
        }
        for (int i = 0; i + 1 < normalized.length(); i++) {
            grams.add(normalized.substring(i, i + 2));
        }
        return grams;
    }
}
