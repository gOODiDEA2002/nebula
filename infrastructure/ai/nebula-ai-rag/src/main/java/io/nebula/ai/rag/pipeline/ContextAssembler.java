package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上下文拼接器
 * <p>
 * 把检索结果拼成带编号的文档列表，并控制总长度不超过预算。
 * 预算是按「加上这一篇会不会超」判断后整篇跳过，而不是截断到一半 ——
 * 半截文档喂给模型比少一篇更容易生成错误结论。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ContextAssembler {

    private final int maxLength;
    private final String documentTemplate;

    /**
     * @param maxLength        上下文最大字符数
     * @param documentTemplate 单篇模板，两个占位符依次为序号与内容
     */
    public ContextAssembler(int maxLength, String documentTemplate) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength 必须为正数");
        }
        if (documentTemplate == null || documentTemplate.isBlank()) {
            throw new IllegalArgumentException("documentTemplate 不能为空");
        }
        this.maxLength = maxLength;
        this.documentTemplate = documentTemplate;
    }

    /**
     * 拼接上下文
     *
     * @param results 已排序的检索结果
     * @return 拼接后的上下文文本；无结果时返回空串
     */
    public String assemble(List<RetrievalResult> results) {
        return assembleDetailed(results).getContext();
    }

    /**
     * 拼接上下文并给出入选引用与序号映射（R4 §4.2）
     * <p>
     * 与 {@link #assemble(List)} 共用同一套遍历与预算判断，因此正文<b>逐字相同</b>；
     * 额外记录实际入选的引用（序号从 1 起）与因预算跳过的条数，供 {@code references-mode=included}
     * 与 {@code CitationPostProcessor} 使用。
     *
     * @param results 已排序的检索结果
     * @return 组装结果；无结果时正文为空串、引用为空
     */
    public ContextAssembly assembleDetailed(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return new ContextAssembly("", List.of(), Map.of(), 0);
        }

        StringBuilder sb = new StringBuilder();
        int currentLength = 0;
        List<RetrievalResult> included = new ArrayList<>();
        Map<Integer, RetrievalResult> citationMap = new LinkedHashMap<>();
        for (int i = 0; i < results.size(); i++) {
            String docContent = String.format(documentTemplate, i + 1, results.get(i).getContent());
            if (currentLength + docContent.length() > maxLength) {
                break;
            }
            sb.append(docContent);
            currentLength += docContent.length();
            included.add(results.get(i));
            citationMap.put(i + 1, results.get(i));
        }
        return new ContextAssembly(sb.toString(), included, citationMap, results.size() - included.size());
    }

    public int getMaxLength() {
        return maxLength;
    }
}
