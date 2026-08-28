package io.nebula.ai.rag.rerank;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.rag.retriever.RetrievalResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 用大模型给 (query, passage) 打相关性分的重排序器
 * <p>
 * 综合分 = 原始分 * (1 - llmWeight) + 模型分 * llmWeight，默认 0.7 偏向模型判断。
 * 单条评分失败按中等相关（0.5）处理而不是丢弃：一条打分打不出来不该让这条文档消失，
 * 整体失败则原样返回截断结果 —— 重排是锦上添花，不该成为检索链路的新故障点。
 * <p>
 * 候选数不超过 topK 时直接返回：排一个不会淘汰任何东西的序，只是白花一次模型调用。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class LlmScoringReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(LlmScoringReranker.class);

    /** 打分失败时的中等相关性默认值 */
    private static final double NEUTRAL_SCORE = 0.5;

    /** 送进 prompt 的段落截断长度，避免长文档把上下文撑爆 */
    private static final int PASSAGE_LIMIT = 500;

    private static final String PROMPT_TEMPLATE =
            "请评估以下查询与文档段落的相关性，只返回 0 到 1 之间的数字（保留两位小数），不要有其他文字。\n\n"
                    + "查询: %s\n\n文档: %s\n\n相关性评分:";

    private final ChatService chatService;
    private final double llmWeight;

    public LlmScoringReranker(ChatService chatService) {
        this(chatService, 0.7);
    }

    /**
     * @param llmWeight 模型分在综合分中的占比，取值 [0, 1]
     */
    public LlmScoringReranker(ChatService chatService, double llmWeight) {
        if (chatService == null) {
            throw new IllegalArgumentException("ChatService 不能为空");
        }
        if (llmWeight < 0 || llmWeight > 1) {
            throw new IllegalArgumentException("llmWeight 必须在 [0, 1] 区间内");
        }
        this.chatService = chatService;
        this.llmWeight = llmWeight;
    }

    @Override
    public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK) {
        if (results == null || results.isEmpty() || topK <= 0) {
            return List.of();
        }
        if (results.size() <= topK) {
            return List.copyOf(results);
        }

        try {
            List<RetrievalResult> scored = new ArrayList<>(results.size());
            for (RetrievalResult result : results) {
                double llmScore = scoreRelevance(query, result.getContent());
                scored.add(RetrievalResult.builder()
                        .id(result.getId())
                        .content(result.getContent())
                        .metadata(result.getMetadata())
                        .score(result.getScore() * (1 - llmWeight) + llmScore * llmWeight)
                        .source(result.getSource())
                        .build());
            }
            scored.sort(Comparator.comparingDouble(RetrievalResult::getScore).reversed());
            return scored.stream().limit(topK).toList();
        } catch (Exception e) {
            log.warn("重排序失败, 返回原始顺序: {}", e.getMessage());
            return results.stream().limit(topK).toList();
        }
    }

    @Override
    public String getName() {
        return "LlmScoringReranker";
    }

    private double scoreRelevance(String query, String passage) {
        if (passage == null || passage.isBlank()) {
            return NEUTRAL_SCORE;
        }
        try {
            String prompt = String.format(PROMPT_TEMPLATE, query,
                    passage.length() > PASSAGE_LIMIT ? passage.substring(0, PASSAGE_LIMIT) : passage);
            return parseScore(chatService.chat(prompt).getContent());
        } catch (Exception e) {
            log.debug("LLM 评分失败, 按中等相关处理: {}", e.getMessage());
            return NEUTRAL_SCORE;
        }
    }

    /**
     * 解析模型返回的评分
     * <p>
     * 模型常常在数字前后带解释文字，这里只抽数字部分并夹到 [0, 1]。
     */
    private double parseScore(String response) {
        if (response == null || response.isBlank()) {
            return NEUTRAL_SCORE;
        }
        try {
            String cleaned = response.trim().replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) {
                return NEUTRAL_SCORE;
            }
            double score = Double.parseDouble(cleaned);
            return Math.max(0, Math.min(1, score));
        } catch (NumberFormatException e) {
            return NEUTRAL_SCORE;
        }
    }
}
