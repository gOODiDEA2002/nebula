package io.nebula.ai.rag.eval;

import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 确定性词面检索器（评测专用，非交付物）
 * <p>
 * <b>为什么用它而不是真实 embedding：</b>单元测试里没有模型服务，也不能让评测结果
 * 依赖某个模型版本。而切分质量的作用机制与检索器类型无关 ——
 * 块边界决定「回答一个问题所需的信息是否共存于同一个块」，这一点对词面检索器与
 * 向量检索器方向一致。真实 embedding 的端到端评测由后续示例的全链路评测承担。
 * <p>
 * <b>打分公式：</b>查询与块内容的字符 2-gram 交集大小，除以块 2-gram 数量的平方根。
 * 分母是 Lucene 经典相似度的字段长度归一思路（{@code 1/sqrt(length)}）：
 * 不做归一会让长块仅因为词多就排前面，做线性归一又会过度偏袒极短块。
 * <p>
 * <b>零随机性：</b>同分时按块 ID 升序稳定排序，任何一次运行的结果完全可复现。
 */
class DeterministicLexicalRetriever implements RetrievalFunction {

    private final List<ScoredChunk> chunks;

    DeterministicLexicalRetriever(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("评测块集不能为空");
        }
        this.chunks = new ArrayList<>(chunks.size());
        for (DocumentChunk chunk : chunks) {
            if (chunk.getId() == null || chunk.getId().isBlank()) {
                throw new IllegalArgumentException("评测块缺少 ID: 前缀判中依赖确定性块 ID");
            }
            this.chunks.add(new ScoredChunk(chunk, bigrams(chunk.getContent())));
        }
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        Set<String> queryGrams = bigrams(query);
        if (queryGrams.isEmpty()) {
            return List.of();
        }

        List<Scored> scored = new ArrayList<>(chunks.size());
        for (ScoredChunk candidate : chunks) {
            int overlap = 0;
            for (String gram : queryGrams) {
                if (candidate.grams.contains(gram)) {
                    overlap++;
                }
            }
            if (overlap == 0) {
                continue;
            }
            double score = overlap / Math.sqrt(Math.max(1, candidate.grams.size()));
            scored.add(new Scored(candidate.chunk, score));
        }

        scored.sort(Comparator.comparingDouble((Scored s) -> s.score).reversed()
                .thenComparing(s -> s.chunk.getId()));

        List<RetrievalResult> results = new ArrayList<>(Math.min(topK, scored.size()));
        for (int i = 0; i < scored.size() && i < topK; i++) {
            Scored item = scored.get(i);
            results.add(RetrievalResult.builder()
                    .id(item.chunk.getId())
                    .content(item.chunk.getContent())
                    .metadata(item.chunk.getMetadata())
                    .score(item.score)
                    .source("lexical")
                    .build());
        }
        return results;
    }

    /**
     * 字符 2-gram 集合：小写化、连续空白折叠成单个空格后取全部相邻字符对
     */
    static Set<String> bigrams(String text) {
        Set<String> grams = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return grams;
        }
        String normalized = text.toLowerCase().replaceAll("\\s+", " ").trim();
        for (int i = 0; i + 1 < normalized.length(); i++) {
            grams.add(normalized.substring(i, i + 2));
        }
        return grams;
    }

    private record ScoredChunk(DocumentChunk chunk, Set<String> grams) {
    }

    private record Scored(DocumentChunk chunk, double score) {
    }
}
